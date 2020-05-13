package com.github.rtwruck.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ContextListener implements ServletContextListener {
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		sce.getServletContext().setAttribute("WebContextListenerClassLoader", getClass().getClassLoader());
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}
}
