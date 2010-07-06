package com.sdstudio.iproxy;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.swing.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

import com.sdstudio.iproxy.actions.Actions;
import com.sdstudio.iproxy.event.MessageEvent;

@Component("SystrayMonitor")
public class SystrayMonitor implements MessageSourceAware,
		ApplicationListener<MessageEvent> {
	private static Logger logger = LoggerFactory
			.getLogger(SystrayMonitor.class);
	private SystemTray systray;
	private PopupMenu menu;
	private MessageSource messageSource;
	private TrayIcon trayIcon;
	private Actions actions;

	public Actions getActions() {
		return actions;
	}

	@Autowired
	public void setActions(Actions actions) {
		this.actions = actions;
	}

	@PostConstruct
	public void init() throws AWTException, NoSuchMessageException, IOException {
		if (SystemTray.isSupported()) {
			logger.info("Installing the system tray...");
			systray = SystemTray.getSystemTray();
			menu = new PopupMenu();
			MenuItem showMenuItem = new MenuItem((String) getActions()
					.getShowMainFrameAction().getValue(Action.NAME));
			showMenuItem.addActionListener(getActions()
					.getShowMainFrameAction());
			menu.add(showMenuItem);
			MenuItem closeMenuItem = new MenuItem((String) getActions()
					.getCloseAction().getValue(Action.NAME));
			closeMenuItem.addActionListener(getActions().getCloseAction());
			menu.add(closeMenuItem);
			trayIcon = new TrayIcon(Utils.getImage("iproxy_tray_icon.png"),
					messageSource.getMessage("iproxy.title", null,
							Utils.getLocale()), menu);
			systray.add(trayIcon);
		}
	}

	@PreDestroy
	public void dispose() {
		if (SystemTray.isSupported() && systray != null) {
			logger.info("Removing the system tray icon.");
			systray.remove(trayIcon);
		}
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void onApplicationEvent(MessageEvent event) {
		if (event.getChannel().equals("message") && systray != null) {
			trayIcon.displayMessage(event.getMessage().toString(),
					event.getTitle(), MessageType.INFO);
		}
	}
}
