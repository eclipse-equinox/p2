/*******************************************************************************
 * Copyright (c) 2009, 2026 Cloudsmith Inc and others.
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
 *  Red Hat Inc. - Bug 460967
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.transport.ecf;

import java.util.Optional;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.FileTransfers;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.provider.IFileTransferProtocolToFactoryMapper;
import org.osgi.framework.*;

/**
 * The activator class controls the plug-in life cycle.
 */
@SuppressWarnings("restriction")
public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.equinox.p2.transport.ecf"; //$NON-NLS-1$
	private static final String HTTP = "http"; //$NON-NLS-1$
	private static final String HTTPS = "https"; //$NON-NLS-1$

	private static BundleContext context;

	// The shared instance
	private static Activator plugin;

	@Override
	public void start(BundleContext aContext) throws Exception {
		Activator.context = aContext;
		Activator.plugin = this;
	}

	@Override
	public void stop(BundleContext aContext) throws Exception {
		Activator.context = null;
		Activator.plugin = null;
	}

	/**
	 * Get singleton instance.
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Removes the http and https providers so that the URLConnection based
	 * transfer of the JRE is used.
	 */
	public synchronized void useJREHttpClient() {
		IFileTransferProtocolToFactoryMapper mapper = FileTransfers.getProtocolToFactoryMapper();
		for (String protocol : new String[] { HTTP, HTTPS }) {
			String providerId = mapper.getBrowseFileTransferFactoryId(protocol);
			if (providerId != null) {
				mapper.removeBrowseFileTransferFactory(providerId);
			}
			providerId = mapper.getRetrieveFileTransferFactoryId(protocol);
			if (providerId != null) {
				mapper.removeRetrieveFileTransferFactory(providerId);
			}
		}
	}

	public static String getProperty(String key) {
		if (context != null) {
			return context.getProperty(key);
		}
		return System.getProperty(key);
	}

	public static Optional<Version> getVersion() {
		return Optional.ofNullable(context) //
				.map(BundleContext::getBundle) //
				.or(() -> Optional.ofNullable(FrameworkUtil.getBundle(FileReader.class)))//
				.map(Bundle::getVersion);
	}

}
