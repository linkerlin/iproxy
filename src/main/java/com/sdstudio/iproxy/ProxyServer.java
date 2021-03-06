package com.sdstudio.iproxy;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.ethz.ssh2.Connection;

import com.sdstudio.iproxy.core.ModelBase;
import com.sdstudio.iproxy.event.LevelType;
import com.sdstudio.iproxy.event.MessageEvent;
import com.sdstudio.iproxy.ui.EditUserInformationDialog;

/**
 * The proxy server core for iproxy
 * 
 * @author Jack
 * @since Jun 23, 2010
 * @version 1.0
 */
@Component("ProxyServer")
public class ProxyServer extends ModelBase implements Runnable {
	private static Logger logger = LoggerFactory.getLogger(ProxyServer.class);
	private Connection connection;
	private boolean running;
	private Configuration configuration;
	private EditUserInformationDialog dialog;
	private ProxyPatternMatcher proxyPatternMatcher;

	public ProxyPatternMatcher getProxyPatternMatcher() {
		return proxyPatternMatcher;
	}

	@Autowired
	public void setProxyPatternMatcher(ProxyPatternMatcher proxyPatternMatcher) {
		this.proxyPatternMatcher = proxyPatternMatcher;
	}

	public EditUserInformationDialog getDialog() {
		return dialog;
	}

	@Autowired
	public void setDialog(EditUserInformationDialog dialog) {
		this.dialog = dialog;
	}

	public Connection getConnection() {
		return connection;
	}

	@Autowired
	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	@Autowired
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	protected synchronized void setRunning(boolean running) {
		if (this.running == running)
			return;
		this.running = running;
		firePropertyChange("running", !running, running);
		if (running)
			logger.info("Proxy server started!");
		else
			logger.info("Proxy server stoped!");
	}

	public synchronized boolean isRunning() {
		return running;
	}

	public void start() {
		if (isRunning())
			return;
		new Thread(this).start();

	}

	@PreDestroy
	public void stop() {
		releaseResources();
	}

	private void releaseResources() {
		if (connection != null)
			connection.close();
		setRunning(false);
	}

	public void run() {
		try {
			logger.info("Starting proxy server...");
			new MessageEvent(this, getMessageSupport().getMessage(
					"running.begin.title"), getMessageSupport().getMessage(
					"running.begin.message",
					getConfiguration().getString("ssh.host"),
					getConfiguration().getString("ssh.user"))).dispatch();
			getConnection().connect(null, 5000, 5000);
			if (getConfiguration().getString("ssh.user") == null
					|| getConfiguration().getString("ssh.password") == null)
				getDialog().setVisible(true);
			getConnection().authenticateWithPassword(
					getConfiguration().getString("ssh.user"),
					getConfiguration().getString("ssh.password"));
			setRunning(true);
			logger.info("Starting the socks proxy server...");
			getConnection().createDynamicPortForwarder(
					getConfiguration().getInteger("iproxy.port")).setMatcher(
					getProxyPatternMatcher());
			new MessageEvent(this, "running.title", "running.message",
					getMessageSupport()).dispatch();
			Utils.getMainFrame().setVisible(false);
		} catch (Exception e) {
			logger.error("Server start failed!", e);
			new MessageEvent(this, LevelType.Error, "proxy.start.failed.title",
					"proxy.start.failed.message", getMessageSupport())
					.dispatch();
			releaseResources();
		}
	}
}
