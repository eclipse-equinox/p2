/****************************************************************************
 * Copyright (c) 2006, 2007 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.equinox.internal.p2.ecf.filetransfer.internal;

import java.util.*;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.util.LogHelper;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.httpclientjava.impl.HttpClientJavaPlugin;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.provider.IFileTransferProtocolToFactoryMapper;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.service.*;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The activator of the bundle. It keeps track of the file transfer providers
 * registered as {@link IFileTransferProvider} services and selects the one to
 * use for each protocol.
 */
public class Activator implements BundleActivator, IFileTransferProtocolToFactoryMapper {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.ecf.filetransfer"; //$NON-NLS-1$

	/**
	 * Name of a system property naming the id of the file transfer provider (e.g.
	 * <code>httpclient5</code>) that is to be used in preference to all other
	 * providers, regardless of their priority.
	 */
	public static final String PREFERRED_PROVIDER_SYS_PROP_NAME = PLUGIN_ID + ".httpProvider"; //$NON-NLS-1$

	private static final String[] jvmSchemes = new String[] {Messages.FileTransferNamespace_Http_Protocol, Messages.FileTransferNamespace_Ftp_Protocol, Messages.FileTransferNamespace_File_Protocol, Messages.FileTransferNamespace_Jar_Protocol, Messages.FileTransferNamespace_Https_Protocol, Messages.FileTransferNamespace_Mailto_Protocol, Messages.FileTransferNamespace_Gopher_Protocol};

	private static final String URL_HANDLER_PROTOCOL_NAME = "url.handler.protocol"; //$NON-NLS-1$

	private static final String URLSTREAM_HANDLER_SERVICE_NAME = "org.osgi.service.url.URLStreamHandlerService"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private BundleContext context = null;

	private ServiceTracker<LogService, LogService> logServiceTracker = null;

	private ServiceTracker<DebugOptions, DebugOptions> debugOptionsTracker = null;

	private ServiceTracker<IAdapterManager, IAdapterManager> adapterManagerTracker = null;

	private ServiceTracker<IProxyService, IProxyService> proxyServiceTracker = null;

	private ServiceTracker<IFileTransferProvider, IFileTransferProvider> providerTracker = null;

	private final Map<String, ProtocolFactory> retrieveFileTransferProtocolMap = new HashMap<>(3);

	private final Map<String, ProtocolFactory> browseFileTransferProtocolMap = new HashMap<>(3);

	private ServiceRegistration<IFileTransferProtocolToFactoryMapper> protocolMapperRegistration;

	private final String preferredProvider = System.getProperty(PREFERRED_PROVIDER_SYS_PROP_NAME);

	public Activator() {
		//
	}

	protected LogService getLogService() {
		synchronized (this) {
			if (this.context == null)
				return null;
			if (logServiceTracker == null) {
				logServiceTracker = new ServiceTracker<>(this.context, LogService.class, null);
				logServiceTracker.open();
			}
			return logServiceTracker.getService();
		}
	}

	public synchronized DebugOptions getDebugOptions() {
		if (context == null)
			return null;
		if (debugOptionsTracker == null) {
			debugOptionsTracker = new ServiceTracker<>(context, DebugOptions.class, null);
			debugOptionsTracker.open();
		}
		return debugOptionsTracker.getService();
	}

	public synchronized IAdapterManager getAdapterManager() {
		if (context == null)
			return null;
		if (adapterManagerTracker == null) {
			adapterManagerTracker = new ServiceTracker<>(context, IAdapterManager.class, null);
			adapterManagerTracker.open();
		}
		return adapterManagerTracker.getService();
	}

	public synchronized IProxyService getProxyService() {
		try {
			if (this.context == null)
				return null;
			if (proxyServiceTracker == null) {
				proxyServiceTracker = new ServiceTracker<>(this.context, IProxyService.class, null);
				proxyServiceTracker.open();
			}
			return proxyServiceTracker.getService();
		} catch (Exception | NoClassDefFoundError e) {
			logNoProxyWarning(e);
		}
		return null;
	}

	static boolean proxyWarningGiven = false;

	public static void logNoProxyWarning(Throwable e) {
		Activator a = getDefault();
		if (a != null) {
			if (!proxyWarningGiven) {
				a.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, IStatus.WARNING, "Warning: Platform proxy API not available", null)); //$NON-NLS-1$
				proxyWarningGiven = true;
			}
		}
	}

	public void log(IStatus status) {
		final LogService logService = getLogService();
		if (logService != null) {
			logService.log(LogHelper.getLogCode(status), LogHelper.getLogMessage(status), status.getException());
		}
	}

	public BundleContext getBundleContext() {
		return context;
	}

	public Bundle getBundle() {
		if (context == null)
			return null;
		return context.getBundle();
	}

	@Override
	public void start(BundleContext ctxt) throws Exception {
		plugin = this;
		this.context = ctxt;
		HttpClientJavaPlugin.getDefault().start(ctxt);
		this.providerTracker = new ServiceTracker<>(ctxt, IFileTransferProvider.class, new ServiceTrackerCustomizer<IFileTransferProvider, IFileTransferProvider>() {
			@Override
			public IFileTransferProvider addingService(ServiceReference<IFileTransferProvider> reference) {
				IFileTransferProvider provider = ctxt.getService(reference);
				if (provider != null)
					addProvider(provider);
				return provider;
			}

			@Override
			public void modifiedService(ServiceReference<IFileTransferProvider> reference, IFileTransferProvider provider) {
				// nothing to do
			}

			@Override
			public void removedService(ServiceReference<IFileTransferProvider> reference, IFileTransferProvider provider) {
				removeRetrieveFileTransferFactory(provider.getId());
				removeBrowseFileTransferFactory(provider.getId());
				ctxt.ungetService(reference);
				// Let the remaining providers take over the protocols of the removed one
				ServiceTracker<IFileTransferProvider, IFileTransferProvider> tracker = providerTracker;
				if (tracker != null) {
					for (IFileTransferProvider remaining : tracker.getTracked().values()) {
						if (remaining != provider)
							addProvider(remaining);
					}
				}
			}
		});
		this.providerTracker.open();
		protocolMapperRegistration = ctxt.registerService(IFileTransferProtocolToFactoryMapper.class, this, null);
	}

	@Override
	public void stop(BundleContext ctxt) throws Exception {
		if (this.protocolMapperRegistration != null) {
			this.protocolMapperRegistration.unregister();
			this.protocolMapperRegistration = null;
		}
		ServiceTracker<IFileTransferProvider, IFileTransferProvider> tracker = providerTracker;
		if (tracker != null) {
			// clear the field first so that removing the providers doesn't re-add the remaining ones
			providerTracker = null;
			tracker.close();
		}
		HttpClientJavaPlugin.getDefault().stop(ctxt);
		synchronized (this) {
			if (proxyServiceTracker != null) {
				proxyServiceTracker.close();
				proxyServiceTracker = null;
			}
			if (adapterManagerTracker != null) {
				adapterManagerTracker.close();
				adapterManagerTracker = null;
			}
			if (debugOptionsTracker != null) {
				debugOptionsTracker.close();
				debugOptionsTracker = null;
			}
			if (logServiceTracker != null) {
				logServiceTracker.close();
				logServiceTracker = null;
			}
			this.context = null;
		}
		synchronized (retrieveFileTransferProtocolMap) {
			retrieveFileTransferProtocolMap.clear();
		}
		synchronized (browseFileTransferProtocolMap) {
			browseFileTransferProtocolMap.clear();
		}
		plugin = null;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public synchronized static Activator getDefault() {
		if (plugin == null) {
			plugin = new Activator();
		}
		return plugin;
	}

	void addProvider(IFileTransferProvider provider) {
		String id = provider.getId();
		// The provider requested by the user wins over all others
		int priority = id.equals(preferredProvider) ? 0 : Math.max(provider.getPriority(), 1);
		for (String protocol : provider.getProtocols()) {
			setRetrieveFileTransferFactory(protocol, id, provider::newRetrieveFileTransfer, priority);
			setBrowseFileTransferFactory(protocol, id, provider::newRemoteFileSystemBrowser, priority);
		}
	}

	public String[] getPlatformSupportedSchemes() {
		final Set<String> protocols = new HashSet<>(Arrays.asList(jvmSchemes));
		BundleContext ctxt = context;
		if (ctxt == null)
			return protocols.toArray(new String[0]);
		final ServiceTracker<Object, Object> handlers = new ServiceTracker<>(ctxt, URLSTREAM_HANDLER_SERVICE_NAME, null);
		handlers.open();
		final ServiceReference<Object>[] refs = handlers.getServiceReferences();
		if (refs != null)
			for (ServiceReference<Object> ref : refs) {
				final Object protocol = ref.getProperty(URL_HANDLER_PROTOCOL_NAME);
				if (protocol instanceof String) {
					protocols.add((String) protocol);
				} else if (protocol instanceof String[]) {
					protocols.addAll(Arrays.asList((String[]) protocol));
				}
			}
		handlers.close();
		return protocols.toArray(new String[0]);
	}

	static class ProtocolFactory implements Comparable<ProtocolFactory> {
		final Object factory;
		final int priority;
		final String id;

		public ProtocolFactory(Object factory, int priority, String id) {
			this.factory = factory;
			this.priority = priority;
			this.id = id;
		}

		public Object getFactory() {
			return factory;
		}

		public String getID() {
			return id;
		}

		public int getPriority() {
			return priority;
		}

		@Override
		public int compareTo(ProtocolFactory other) {
			return Integer.compare(this.priority, other.priority);
		}
	}

	public IRetrieveFileTransfer getFileTransfer(String protocol) {
		ProtocolFactory protocolFactory = null;
		synchronized (retrieveFileTransferProtocolMap) {
			protocolFactory = retrieveFileTransferProtocolMap.get(protocol);
		}
		if (protocolFactory == null)
			return null;
		final IRetrieveFileTransferFactory factory = (IRetrieveFileTransferFactory) protocolFactory.getFactory();
		if (factory != null)
			return factory.newInstance();
		return null;
	}

	public IRemoteFileSystemBrowser getBrowseFileTransfer(String protocol) {
		ProtocolFactory protocolFactory = null;
		synchronized (browseFileTransferProtocolMap) {
			protocolFactory = browseFileTransferProtocolMap.get(protocol);
		}
		if (protocolFactory == null)
			return null;
		final IRemoteFileSystemBrowserFactory factory = (IRemoteFileSystemBrowserFactory) protocolFactory.getFactory();
		if (factory != null)
			return factory.newInstance();
		return null;
	}

	private boolean setFactory(Map<String, ProtocolFactory> map, String contribution, String protocol, String id, Object factory, int priority) {
		if (protocol == null || "".equals(protocol)) //$NON-NLS-1$
			return false;
		if (id == null)
			return false;
		if (factory == null)
			return false;
		// Now create new ProtocolFactory
		ProtocolFactory newProtocolFactory = new ProtocolFactory(factory, priority, id);
		synchronized (map) {
			ProtocolFactory oldProtocolFactory = map.get(protocol);
			// If found, choose between them based upon comparing their priority
			if (oldProtocolFactory != null) {
				// Now, compare priorities and pick winner
				int result = oldProtocolFactory.compareTo(newProtocolFactory);
				if (result < 0) {
					// Existing one has higher priority, so leave existing one as the handler
					return false;
				} else if (result == 0) {
					// Warn that we are using new one because they have the same priority.
					log(new Status(IStatus.WARNING, PLUGIN_ID, IStatus.WARNING, NLS.bind("{0} for protocol {1} from {2} will be used in preference to existing handler.  Both have same priority={3}.", new Object[] {contribution, protocol, id, Integer.valueOf(priority)}), null)); //$NON-NLS-1$
				}
			}
			// Finally, put protocol factory in map with protocol as key
			map.put(protocol, newProtocolFactory);
			return true;
		}
	}

	private String getFactoryId(Map<String, ProtocolFactory> map, String protocol) {
		if (protocol == null)
			return null;
		synchronized (map) {
			ProtocolFactory protocolFactory = map.get(protocol);
			return protocolFactory == null ? null : protocolFactory.getID();
		}
	}

	private int getFactoryPriority(Map<String, ProtocolFactory> map, String protocol) {
		if (protocol == null)
			return -1;
		synchronized (map) {
			ProtocolFactory protocolFactory = map.get(protocol);
			return protocolFactory == null ? -1 : protocolFactory.getPriority();
		}
	}

	private boolean removeFactory(Map<String, ProtocolFactory> map, String id) {
		if (id == null)
			return false;
		synchronized (map) {
			return map.values().removeIf(protocolFactory -> id.equals(protocolFactory.getID()));
		}
	}

	@Override
	public boolean setRetrieveFileTransferFactory(String protocol, String id, IRetrieveFileTransferFactory factory, int priority) {
		return setFactory(retrieveFileTransferProtocolMap, "File retrieve contribution", protocol, id, factory, priority); //$NON-NLS-1$
	}

	@Override
	public String getRetrieveFileTransferFactoryId(String protocol) {
		return getFactoryId(retrieveFileTransferProtocolMap, protocol);
	}

	@Override
	public int getRetrieveFileTransferPriority(String protocol) {
		return getFactoryPriority(retrieveFileTransferProtocolMap, protocol);
	}

	@Override
	public boolean removeRetrieveFileTransferFactory(String id) {
		return removeFactory(retrieveFileTransferProtocolMap, id);
	}

	@Override
	public boolean setBrowseFileTransferFactory(String protocol, String id, IRemoteFileSystemBrowserFactory factory, int priority) {
		return setFactory(browseFileTransferProtocolMap, "File browse contribution", protocol, id, factory, priority); //$NON-NLS-1$
	}

	@Override
	public String getBrowseFileTransferFactoryId(String protocol) {
		return getFactoryId(browseFileTransferProtocolMap, protocol);
	}

	@Override
	public int getBrowseFileTransferPriority(String protocol) {
		return getFactoryPriority(browseFileTransferProtocolMap, protocol);
	}

	@Override
	public boolean removeBrowseFileTransferFactory(String id) {
		return removeFactory(browseFileTransferProtocolMap, id);
	}

}
