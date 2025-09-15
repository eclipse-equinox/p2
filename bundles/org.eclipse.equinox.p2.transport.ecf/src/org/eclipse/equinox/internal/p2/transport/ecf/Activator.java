/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc and others.
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

import java.util.*;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;
import org.eclipse.ecf.provider.filetransfer.IFileTransferProtocolToFactoryMapper;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle. This activator has
 * helper methods to get file transfer service tracker, and for making sure
 * required ECF bundles are started.
 */
@SuppressWarnings("restriction")
public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.equinox.p2.transport.ecf"; //$NON-NLS-1$
	private static final String HTTP = "http"; //$NON-NLS-1$
	private static final String HTTPS = "https"; //$NON-NLS-1$

	private static BundleContext context;
	// tracker for ECF service
	private ServiceTracker<IRetrieveFileTransferFactory, IRetrieveFileTransferFactory> retrievalFactoryTracker;

	// tracker for protocolToFactoryMapperTracker
	private ServiceTracker<IFileTransferProtocolToFactoryMapper, IFileTransferProtocolToFactoryMapper> protocolToFactoryMapperTracker = null;

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
		if (retrievalFactoryTracker != null) {
			retrievalFactoryTracker.close();
			retrievalFactoryTracker = null;
		}
		if (protocolToFactoryMapperTracker != null) {
			protocolToFactoryMapperTracker.close();
			protocolToFactoryMapperTracker = null;
		}

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
	 * Returns a {@link IRetrieveFileTransferFactory} using a {@link ServiceTracker}
	 * after having attempted to start the bundle
	 * "org.eclipse.ecf.provider.filetransfer". If something is wrong with the
	 * configuration this method returns null.
	 *
	 * @return a factory, or null, if configuration is incorrect
	 */
	public IRetrieveFileTransferFactory getRetrieveFileTransferFactory() {
		return getFileTransferServiceTracker().getService();
	}

	public synchronized void useJREHttpClient() {
		IFileTransferProtocolToFactoryMapper mapper = getProtocolToFactoryMapper();
		if (mapper != null) {
			// remove http
			// Remove browse provider
			String providerId = mapper.getBrowseFileTransferFactoryId(HTTP);
			if (providerId != null) {
				mapper.removeBrowseFileTransferFactory(providerId);
			}
			// Remove retrieve provider
			providerId = mapper.getRetrieveFileTransferFactoryId(HTTP);
			if (providerId != null) {
				mapper.removeRetrieveFileTransferFactory(providerId);
			}
			// Remove send provider
			providerId = mapper.getSendFileTransferFactoryId(HTTP);
			if (providerId != null) {
				mapper.removeSendFileTransferFactory(providerId);
			}
			// remove https
			// Remove browse provider
			providerId = mapper.getBrowseFileTransferFactoryId(HTTPS);
			if (providerId != null) {
				mapper.removeBrowseFileTransferFactory(providerId);
			}
			// Remove retrieve provider
			providerId = mapper.getRetrieveFileTransferFactoryId(HTTPS);
			if (providerId != null) {
				mapper.removeRetrieveFileTransferFactory(providerId);
			}
			// Remove send provider
			providerId = mapper.getSendFileTransferFactoryId(HTTPS);
			if (providerId != null) {
				mapper.removeSendFileTransferFactory(providerId);
			}
		}
	}

	/**
	 * Gets the singleton ServiceTracker for the IRetrieveFileTransferFactory and
	 * starts the bundles "org.eclipse.ecf" and
	 * "org.eclipse.ecf.provider.filetransfer" on first call.
	 *
	 * @return ServiceTracker
	 */
	private synchronized ServiceTracker<IRetrieveFileTransferFactory, IRetrieveFileTransferFactory> getFileTransferServiceTracker() {
		if (retrievalFactoryTracker == null) {
			BundleContext bundleContext = Activator.context;
			retrievalFactoryTracker = new ServiceTracker<>(bundleContext, IRetrieveFileTransferFactory.class, null);
			retrievalFactoryTracker.open();
			HashSet<String> toStart = new HashSet<>();
			toStart.add("org.eclipse.ecf"); //$NON-NLS-1$
			toStart.add("org.eclipse.ecf.provider.filetransfer"); //$NON-NLS-1$
			startBundle(toStart, bundleContext);
		}
		return retrievalFactoryTracker;
	}

	private IFileTransferProtocolToFactoryMapper getProtocolToFactoryMapper() {
		if (protocolToFactoryMapperTracker == null) {
			protocolToFactoryMapperTracker = new ServiceTracker<>(context, IFileTransferProtocolToFactoryMapper.class,
					null);
			protocolToFactoryMapperTracker.open();
		}
		return protocolToFactoryMapperTracker.getService();
	}

	private static void startBundle(Set<String> bundles, BundleContext bundleContext) {
		for (Bundle bundle : bundleContext.getBundles()) {
			String symbolicName = bundle.getSymbolicName();
			if (bundles.contains(symbolicName)) {
				try {
					if ((bundle.getState() & Bundle.INSTALLED) == 0) {
						bundle.start(Bundle.START_ACTIVATION_POLICY);
						bundle.start(Bundle.START_TRANSIENT);
						bundles.remove(symbolicName);
						if (bundles.isEmpty()) {
							// nothing more to do
							return;
						}
					}
				} catch (BundleException e) {
					// failed, try next bundle
				}
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
