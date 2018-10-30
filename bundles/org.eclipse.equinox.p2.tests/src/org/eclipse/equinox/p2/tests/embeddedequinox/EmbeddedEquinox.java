/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.embeddedequinox;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Map;
import org.osgi.framework.BundleContext;

/*
 * This class assumes the bundle's classloader only has access to org.osgi.framework
 * from the hosting framework.  Any more access could result in ClassVerifier errors.
 */
public class EmbeddedEquinox {
	private final Map<String, String> frameworkProperties;
	private final String[] frameworkArgs;
	private Class<?> eclipseStarterClazz;
	private BundleContext context;
	private URL[] frameworkClassPath;

	public EmbeddedEquinox(Map<String, String> frameworkProperties, String[] frameworkArgs, URL[] frameworkClassPath) {
		this.frameworkProperties = frameworkProperties;
		this.frameworkArgs = frameworkArgs;
		this.frameworkClassPath = frameworkClassPath;
	}

	public BundleContext startFramework() {
		System.setProperty("osgi.framework.useSystemProperties", "false");
		try {
			@SuppressWarnings("resource")
			// For some reason when you close the framework loader, tests fail!
			URLClassLoader frameworkLoader = new FrameworkClassLoader(frameworkClassPath, this.getClass().getClassLoader());
			eclipseStarterClazz = frameworkLoader.loadClass("org.eclipse.core.runtime.adaptor.EclipseStarter");

			Method setInitialProperties = eclipseStarterClazz.getMethod("setInitialProperties", Map.class); //$NON-NLS-1$
			setInitialProperties.invoke(null, frameworkProperties);

			Method runMethod = eclipseStarterClazz.getMethod("startup", String[].class, Runnable.class); //$NON-NLS-1$
			context = (BundleContext) runMethod.invoke(null, frameworkArgs, null);
		} catch (Throwable t) {
			if (t instanceof RuntimeException)
				throw (RuntimeException) t;
			throw new RuntimeException(t);
		}
		return context;
	}

	public void shutdown() {
		try {
			Method shutdownMethod = eclipseStarterClazz.getMethod("shutdown"); //$NON-NLS-1$
			shutdownMethod.invoke((Object[]) null);

		} catch (Throwable t) {
			if (t instanceof RuntimeException)
				throw (RuntimeException) t;
			throw new RuntimeException(t);
		}
	}

	public class FrameworkClassLoader extends URLClassLoader {
		ClassLoader embeddedBundleLoader;

		public FrameworkClassLoader(URL[] urls, ClassLoader parent) {
			super(urls, null);
			this.embeddedBundleLoader = parent;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			if (name.startsWith("org.osgi.framework.") || name.startsWith("org.osgi.resource."))
				// TODO this should call findClass; but then have to use reflection!!
				return embeddedBundleLoader.loadClass(name);
			return super.findClass(name);
		}

		@Override
		public URL findResource(String name) {
			if (name.startsWith("org/osgi/framework/") || name.startsWith("org/osgi/resource/"))
				// TODO this should call findResource; but then have to use reflection!!
				return embeddedBundleLoader.getResource(name);
			return super.findResource(name);
		}

		@Override
		public Enumeration<URL> findResources(String name) throws IOException {
			if (name.startsWith("org/osgi/framework/") || name.startsWith("org/osgi/resource/"))
				// TODO this should call findResources; but then have to use reflection!!
				return embeddedBundleLoader.getResources(name);
			return super.findResources(name);
		}

	}
}
