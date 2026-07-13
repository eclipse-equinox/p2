/*******************************************************************************
 * Copyright (c) 2009, 2022 Cloudsmith Inc and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 * 	Cloudsmith Inc - initial API and implementation
 * 	IBM Corporation - ongoing development
 * 	Genuitec - Bug 291926
 *  Sonatype, Inc. - transport split
 *  Christoph Läubrich - Issue #20 - XMLParser should not require a bundle context but a Parser in the constructor
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.equinox.internal.p2.core.helpers.SecureXMLUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.xml.sax.SAXException;

/**
 * The activator class controls the plug-in life cycle. This activator has
 * helper methods to get SAXParserFactory, and for making sure required bundles
 * are started.
 */
public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.equinox.p2.repository"; //$NON-NLS-1$

	private static BundleContext context;

	@Override
	public void start(BundleContext aContext) throws Exception {
		synchronized (Activator.class) {
			Activator.context = aContext;
		}
		// Force the startup of the registry bundle to make sure that the preference
		// scope is registered
		IExtensionRegistry.class.getName();
	}

	@Override
	public void stop(BundleContext aContext) throws Exception {
		synchronized (Activator.class) {
			Activator.context = null;
		}
	}

	public static BundleContext getContext() {
		synchronized (Activator.class) {
			if (Activator.context == null) {
				throw new IllegalStateException("bundle " + ID + " is not started"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return Activator.context;
		}
	}

	public static SAXParserFactory getParserFactory() {
		try {
			return SecureXMLUtil.newSecureSAXParserFactory();
		} catch (SAXException | ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}

}
