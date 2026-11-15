/****************************************************************************
 * Copyright (c) 2021, 2022 IBM, Composent Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Chris Aniszczyk - initial API and implementation
 *    Yatta Solutions - HttpClient 4.5 implementation
 *    Christoph Läubrich - adapt for java http client
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.internal.provider.filetransfer.httpclientjava;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ecf.core.security.SSLContextFactory;
import org.eclipse.ecf.core.util.ECFRuntimeException;
import org.eclipse.ecf.core.util.LogHelper;
import org.eclipse.ecf.core.util.Trace;
import org.eclipse.ecf.filetransfer.service.IRemoteFileSystemBrowser;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransfer;
import org.eclipse.ecf.internal.provider.filetransfer.DebugOptions;
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.ECFHttpClientFactory.ModifierRunner;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The activator class controls the plug-in life cycle
 */
@SuppressWarnings("restriction")
public class Activator implements BundleActivator {

	private static final class ScopedHttpClientCustomizer implements ServiceTrackerCustomizer<HttpClient, HttpClient> {
		private final String neededScope;
		private final BundleContext context;

		public ScopedHttpClientCustomizer(BundleContext context, String neededScope) {
			this.context = context;
			this.neededScope = neededScope;
		}

		@Override
		public HttpClient addingService(ServiceReference<HttpClient> reference) {
			if (!hasScope(reference, neededScope)) {
				return null;
			}
			return context.getService(reference);
		}

		private boolean hasScope(ServiceReference<HttpClient> reference, String neededScope) {
			Object scopeProperty = reference.getProperty("http.client.scope"); //$NON-NLS-1$
			if (scopeProperty == null || !(scopeProperty instanceof String)) {
				return false;
			}
			String[] scopes = ((String) scopeProperty).split("\\s*,\\s*"); //$NON-NLS-1$
			for (String scope : scopes) {
				if (neededScope.equals(scope) || (scope.endsWith("*") && neededScope.startsWith(scope.substring(0, scope.length() - 1)))) { //$NON-NLS-1$
					return true;
				}
			}
			return false;
		}

		@Override
		public void modifiedService(ServiceReference<HttpClient> reference, HttpClient service) {
			if (!hasScope(reference, neededScope)) {
				context.ungetService(reference);
			}
		}

		@Override
		public void removedService(ServiceReference<HttpClient> reference, HttpClient service) {
			context.ungetService(reference);
		}
	}

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.ecf.provider.filetransfer.httpclientjava"; //$NON-NLS-1$

	public static final String USE_SHARED_CLIENT = PLUGIN_ID + ".sharedClient"; //$NON-NLS-1$

	private static final String USE_SHARED_CLIENT_DEFAULT = "true"; //$NON-NLS-1$

	/**
	 * @since 2.1
	 */
	public static final String USE_COOKIE_STORE = PLUGIN_ID + ".cookieStore"; //$NON-NLS-1$

	private static final String USE_COOKIE_STORE_DEFAULT = "true"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	private BundleContext context = null;

	private ServiceTracker<LogService, LogService> logServiceTracker = null;

	private ServiceTracker<SSLContextFactory, SSLContextFactory> sslContextFactoryTracker;

	private ServiceTracker<INTLMProxyHandler, INTLMProxyHandler> ntlmProxyHandlerTracker;

	private ServiceTracker<IHttpClientFactory, IHttpClientFactory> httpClientFactoryTracker;

	private ServiceTracker<HttpClient, HttpClient> browseClientTracker;

	private ServiceTracker<HttpClient, HttpClient> retrieveClientTracker;

	private boolean useSharedClient;

	private boolean useCookieStore;

	/**
	 * The constructor
	 */
	public Activator() {
		//
	}

	public BundleContext getContext() {
		return context;
	}

	@Override
	public void start(BundleContext ctxt) throws Exception {
		plugin = this;
		this.context = ctxt;
		useSharedClient = Boolean.parseBoolean(System.getProperty(USE_SHARED_CLIENT, USE_SHARED_CLIENT_DEFAULT));
		useCookieStore = Boolean.parseBoolean(System.getProperty(USE_COOKIE_STORE, USE_COOKIE_STORE_DEFAULT));
		applyDebugOptions(ctxt);
	}

	private void applyDebugOptions(BundleContext ctxt) {
		ServiceReference<org.eclipse.osgi.service.debug.DebugOptions> debugRef = ctxt.getServiceReference(org.eclipse.osgi.service.debug.DebugOptions.class);
		org.eclipse.osgi.service.debug.DebugOptions debugOptions = debugRef == null ? null : ctxt.getService(debugRef);
		if (debugOptions == null) {
			return;
		}
		try {
			if (!debugOptions.isDebugEnabled()) {
				return;
			}
			Map<String, String> options = debugOptions.getOptions();
			String ourDebugPrefix = PLUGIN_ID + "/";
			String ecfDebugPrefix = "org.eclipse.ecf.provider.filetransfer.httpclient4/";
			for (Map.Entry<String, String> entry : options.entrySet()) {
				if (entry.getKey() != null && entry.getKey().startsWith(ourDebugPrefix)) {
					String ecfOption = ecfDebugPrefix + entry.getKey().substring(ourDebugPrefix.length());
					String ecfValue = options.get(ecfOption);
					if (ecfValue == null) {
						debugOptions.setOption(ecfOption, entry.getValue());
					}
				}
			}
		} finally {
			ctxt.ungetService(debugRef);
		}
	}

	@Override
	public synchronized void stop(BundleContext ctxt) throws Exception {
		if (sslContextFactoryTracker != null) {
			sslContextFactoryTracker.close();
		}

		if (logServiceTracker != null) {
			logServiceTracker.close();
		}

		if (ntlmProxyHandlerTracker != null) {
			ntlmProxyHandlerTracker.close();
		}
		this.context = null;
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

	private synchronized LogService getLogService() {
		if (logServiceTracker == null) {
			logServiceTracker = new ServiceTracker<LogService, LogService>(this.context, LogService.class, null);
			logServiceTracker.open();
		}
		return logServiceTracker.getService();
	}

	public boolean isUseSharedClient() {
		return useSharedClient;
	}

	/**
	 * @since 2.1
	 */
	public boolean isUseCookieStore() {
		return useCookieStore;
	}

	public void log(IStatus status) {
		LogService logService = getLogService();
		if (logService != null) {
			Logger logger = logService.getLogger((String) null);
			int code = status.getSeverity();
			String message = LogHelper.getLogMessage(status);
			Throwable e = status.getException();
			switch (code) {
			case IStatus.ERROR:
				if (logger.isErrorEnabled()) logger.error(message,e);
				break;
			case IStatus.WARNING:
				if (logger.isWarnEnabled()) logger.warn(message,e);
				break;
			default:
				if (logger.isInfoEnabled()) logger.warn(message,e);
			}
		}
	}

	/**
	 * @since 2.1
	 */
	public synchronized SSLContextFactory getSSLContextFactory() {
		if (sslContextFactoryTracker == null) {
			sslContextFactoryTracker = new ServiceTracker<SSLContextFactory, SSLContextFactory>(this.context, SSLContextFactory.class, null);
			sslContextFactoryTracker.open();
		}
		SSLContextFactory service = sslContextFactoryTracker.getService();
		return service;
	}

	public synchronized INTLMProxyHandler getNTLMProxyHandler() {
		if (ntlmProxyHandlerTracker == null) {
			ntlmProxyHandlerTracker = new ServiceTracker<INTLMProxyHandler, INTLMProxyHandler>(this.context, INTLMProxyHandler.class, null);
			ntlmProxyHandlerTracker.open();
		}
		INTLMProxyHandler service = ntlmProxyHandlerTracker.getService();
		if (service == null) {
			service = new DefaultNTLMProxyHandler();
		}
		return service;
	}

	public synchronized IHttpClientFactory getHttpClientFactory() {
		if (httpClientFactoryTracker == null) {
			httpClientFactoryTracker = new ServiceTracker<IHttpClientFactory, IHttpClientFactory>(this.context, IHttpClientFactory.class, null);
			httpClientFactoryTracker.open();
		}
		IHttpClientFactory service = httpClientFactoryTracker.getService();
		if (service == null) {
			service = new ECFHttpClientFactory();
			Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
			serviceProperties.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
			context.registerService(IHttpClientFactory.class, service, serviceProperties);
		}
		return service;
	}

	public synchronized HttpClient getBrowseHttpClient() {
		HttpClient service;
		if (isUseSharedClient()) {
			if (browseClientTracker == null) {
				browseClientTracker = new ServiceTracker<HttpClient, HttpClient>(context, HttpClient.class,
						new ScopedHttpClientCustomizer(context, IRemoteFileSystemBrowser.class.getName()));
				browseClientTracker.open();
			}
			service = browseClientTracker.getService();
			if (service == null) {
				service = registerHttpClient();
			}
		} else {
			service = buildHttpClient();
		}
		return service;
	}

	public synchronized HttpClient getRetrieveHttpClient() {
		HttpClient service;
		if (isUseSharedClient()) {
			if (retrieveClientTracker == null) {
				retrieveClientTracker = new ServiceTracker<HttpClient, HttpClient>(context, HttpClient.class,
						new ScopedHttpClientCustomizer(context, IRetrieveFileTransfer.class.getName()));
				retrieveClientTracker.open();
			}
			service = retrieveClientTracker.getService();
			if (service == null) {
				service = registerHttpClient();
			}
		} else {
			service = buildHttpClient();
		}
		return service;
	}

	private HttpClient registerHttpClient() {
		HttpClient client = buildHttpClient();

		Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
		serviceProperties.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
		serviceProperties.put("http.client.scope", "org.eclipse.ecf.filetransfer.service.*");
		context.registerService(new String[] { HttpClient.class.getName() }, client, serviceProperties);

		return client;
	}

	private HttpClient buildHttpClient() {
		Builder builder = getHttpClientFactory().newClient();
		if (isUseCookieStore()) {
			CookieHandler cookieHandler = CookieHandler.getDefault();
			if (cookieHandler == null) {
				cookieHandler = new CookieManager();
			}
			builder.cookieHandler(cookieHandler);
		}
		return builder.build();
	}

	public static void logNoProxyWarning(Throwable e) {
		Activator a = getDefault();
		if (a != null) {
			a.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, IStatus.ERROR, "Warning: Platform proxy API not available", e)); //$NON-NLS-1$
		}
	}

	protected <T> T runModifiers(T value, ModifierRunner<T> modifierRunner) {
		T modifiedValue = value;
		List<ServiceReference<IHttpClientModifier>> orderedServices = getModifierReferences();
		for (ServiceReference<IHttpClientModifier> serviceReference : orderedServices) {
			IHttpClientModifier modifier = this.context.getService(serviceReference);
			try {
				if (modifier == null) {
					continue;
				}
				T newValue = modifierRunner.run(modifier, modifiedValue);
				if (newValue != null) {
					modifiedValue = newValue;
				}
			} finally {
				this.context.ungetService(serviceReference);
			}
		}
		return modifiedValue;
	}

	private List<ServiceReference<IHttpClientModifier>> getModifierReferences() {
		Collection<ServiceReference<IHttpClientModifier>> serviceReferences;
		try {
			serviceReferences = this.context.getServiceReferences(IHttpClientModifier.class, null);
		} catch (InvalidSyntaxException e) {
			// Can't happen
			throw new ECFRuntimeException(e);
		}
		List<ServiceReference<IHttpClientModifier>> orderedServices = new ArrayList<ServiceReference<IHttpClientModifier>>(serviceReferences);
		if (orderedServices.size() < 2) {
			return orderedServices;
		}
		Collections.sort(orderedServices, new Comparator<ServiceReference<?>>() {

			@Override
			public int compare(ServiceReference<?> o1, ServiceReference<?> o2) {
				if (o1 == o2) {
					return 0;
				}
				int ranking1 = getServiceRanking(o1);
				int ranking2 = getServiceRanking(o2);
				// Lowest ranking first
				return ranking1 - ranking2;
			}

			private int getServiceRanking(ServiceReference<?> reference) {
				Object rankingValue = reference.getProperty(Constants.SERVICE_RANKING);
				if (rankingValue instanceof Integer) {
					return (Integer) rankingValue;
				} else if (rankingValue instanceof String) {
					try {
						return Integer.parseInt((String) rankingValue);
					} catch (NumberFormatException e) {
						Trace.catching(Activator.PLUGIN_ID, DebugOptions.EXCEPTIONS_CATCHING, Activator.class, "getServiceRanking", e); //$NON-NLS-1$
					}

				}
				return 0;
			}

		});
		return orderedServices;
	}

}
