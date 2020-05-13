package com.github.rtwruck.app;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ContextListener implements ServletContextListener {
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		sce.getServletContext().setAttribute("LibContextListenerClassLoader", getClass().getClassLoader());
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}
}
