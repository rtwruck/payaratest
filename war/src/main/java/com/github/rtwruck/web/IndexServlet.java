package com.github.rtwruck.web;

import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.Writer;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class IndexServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final Writer w = resp.getWriter();

		final String q = req.getParameter("q");
		final String path = (q == null) ? "com/github/rtwruck/common/ClassLoadedFromCommonLib.class" : q;

		printForm(w, path);

		findResourceInClassLoaderHierarchy(w,
				(ClassLoader) getServletContext().getAttribute("WebContextListenerClassLoader"), path,
				"WebContextListenerClassLoader");
		findResourceInClassLoaderHierarchy(w,
				(ClassLoader) getServletContext().getAttribute("LibContextListenerClassLoader"), path,
				"LibContextListenerClassLoader");

		w.write("<hr/>\n");

		printClassLoader(w, Thread.currentThread().getContextClassLoader(), "Context ClassLoader");
		printClassLoader(w, getClass().getClassLoader(), "Webapp ClassLoader");

		try {
			final Class<?> cls = Class.forName("com.github.rtwruck.app.ClassLoadedFromEarLib");
			printClassLoader(w, cls.getClassLoader(), "EAR ClassLoader");
		} catch (ClassNotFoundException e) {
			w.write("<h3>EAR ClassLoader is inaccessible</h3>");
		}

		try {
			final Class<?> cls = Class.forName("com.github.rtwruck.common.ClassLoadedFromCommonLib");
			printClassLoader(w, cls.getClassLoader(), "Common ClassLoader");
		} catch (ClassNotFoundException e) {
			w.write("<h3>Common ClassLoader is inaccessible</h3>");
		}

		printClassLoader(w, getClass().getClassLoader().getClass().getClassLoader(), "Module ClassLoader");
//		printClassLoader(w, Object.class.getClassLoader(), "Bootstrap ClassLoader");
	}

	private void findResourceInClassLoaderHierarchy(Writer w, ClassLoader cl, String path, String title)
			throws IOException {
		final String className;
		if (path.endsWith(".class"))
			className = path.replace(".class", "").replace("/", ".");
		else
			className = null;

		w.write("<h3>");
		w.write(title);
		w.write(" hierarchy</h3>\n<ol>");

		for (ClassLoader c = cl; c != null; c = c.getParent()) {
			w.write("<li>");
			printClassLoader(w, c);
			if (className != null) {
				try {
					final Class<?> clazz = c.loadClass(className);
					w.write("<div><b>Loaded ");
					printObjectID(w, clazz);
					printClassUID(w, clazz);
					w.write("</b> ");
					w.write(className);
					w.write(" from ClassLoader:</div>");
					printClassLoader(w, clazz.getClassLoader());
				} catch (ClassNotFoundException e) {
					w.write("<div>Could not load ");
					w.write(className);
					w.write("</div>");
					w.write("</div>");
				}
			}
			final Enumeration<URL> resources = c.getResources(path);
			printResources(w, resources);
			w.write("</li>\n");
		}

		w.write("</ol>\n");
	}

	private void printClassLoader(Writer w, ClassLoader cl, String title) throws IOException {
		w.write("<h3>");
		w.write(title);
		w.write("</h3>\n");

		printClassLoader(w, cl);
	}

	private void printClassLoader(Writer w, ClassLoader c) throws IOException {
		w.write("<div><b>");
		printObjectID(w, c);
		w.write("</b> ");
		w.write(c.toString());
		w.write("</div>");
	}

	private void printObjectID(Writer w, Object o) throws IOException {
		w.write("[ID ");
		w.write(Integer.toHexString(System.identityHashCode(o)));
		w.write("]");
	}

	private void printClassUID(Writer w, Class<?> c) throws IOException {
		final ObjectStreamClass o = ObjectStreamClass.lookup(c);
		if (o != null) {
			w.write("[UID ");
			w.write(Long.toHexString(o.getSerialVersionUID()));
			w.write("]");
		}
	}

	private void printResources(Writer w, Enumeration<URL> resources) throws IOException {
		w.write("<div>Matched resources:</div><ul>");

		while (resources.hasMoreElements()) {
			w.write("<li>");
			w.write(resources.nextElement().toString());
			w.write("</li>\n");
		}

		w.write("</ul>\n");
	}

	private void printForm(Writer w, String value) throws IOException {
		w.write("<form>Resource path: <input type=\"text\" name=\"q\" value=\"");
		w.write(value);
		w.write("\"/> <input type=\"submit\"/></form>\n\n");
	}
}
