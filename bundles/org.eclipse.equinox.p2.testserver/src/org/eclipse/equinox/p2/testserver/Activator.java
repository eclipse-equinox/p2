/*******************************************************************************
 * Copyright (c) 2009, 2018 Cloudsmith Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.testserver;

import java.net.URI;
import javax.servlet.ServletException;
import org.eclipse.equinox.p2.testserver.servlets.BasicResourceDelivery;
import org.eclipse.equinox.p2.testserver.servlets.ChopAndDelay;
import org.eclipse.equinox.p2.testserver.servlets.ContentLengthLier;
import org.eclipse.equinox.p2.testserver.servlets.FileMolester;
import org.eclipse.equinox.p2.testserver.servlets.IntermittentTimeout;
import org.eclipse.equinox.p2.testserver.servlets.LastModifiedLier;
import org.eclipse.equinox.p2.testserver.servlets.Redirector;
import org.eclipse.equinox.p2.testserver.servlets.Stats;
import org.eclipse.equinox.p2.testserver.servlets.StatusCodeResponse;
import org.eclipse.equinox.p2.testserver.servlets.TimeOut;
import org.eclipse.equinox.p2.testserver.servlets.Truncator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<HttpService, HttpService> {
	private static BundleContext context;
	private ServiceTracker<HttpService, ?> httpTracker;
	private SecureContext secureHttpContext;
	private SecuredArtifactsContext artifactSecuredHttpContext;
	private static Activator instance;
	private HttpService httpService;
	private AlwaysFailContext alwaysFail;
	private FlipFlopFailContext flipFlop;

	private static final String SITE = "http://download.eclipse.org/eclipse/updates/3.4"; //$NON-NLS-1$
	private static final String SITE2 = "http://www.eclipse.org/equinox/p2/testing/updateSite"; //$NON-NLS-1$
	private static final String SITE3 = "http://download.eclipse.org/eclipse/updates/3.5-I-builds/"; //$NON-NLS-1$

	@Override
	public void start(BundleContext aContext) throws Exception {
		context = aContext;

		httpTracker = new ServiceTracker<>(context, HttpService.class, this);
		httpTracker.open();
		instance = this;
	}

	@Override
	public void stop(BundleContext aContext) throws Exception {
		httpTracker.close();
		context = null;
	}

	@Override
	public HttpService addingService(ServiceReference<HttpService> reference) {
		httpService = context.getService(reference);
		secureHttpContext = new SecureContext(httpService.createDefaultHttpContext());
		artifactSecuredHttpContext = new SecuredArtifactsContext(httpService.createDefaultHttpContext());
		alwaysFail = new AlwaysFailContext(httpService.createDefaultHttpContext());
		flipFlop = new FlipFlopFailContext(httpService.createDefaultHttpContext());

		try {
			httpService.registerResources("/public", "/webfiles", null); //$NON-NLS-1$ //$NON-NLS-2$
			httpService.registerResources("/importexport", "/webfiles/importexport", null); //$NON-NLS-1$ //$NON-NLS-2$
			httpService.registerResources("/private", "/webfiles", secureHttpContext); //$NON-NLS-1$ //$NON-NLS-2$
			httpService.registerResources("/never", "/webfiles", alwaysFail); //$NON-NLS-1$ //$NON-NLS-2$
			httpService.registerResources("/flipflop", "/webfiles", flipFlop); //$NON-NLS-1$ //$NON-NLS-2$
			// httpService.registerResources("/mirrorrequest", "/webfiles/emptyJarRepo",
			// null); //$NON-NLS-1$ //$NON-NLS-2$

			httpService.registerServlet("/status", new StatusCodeResponse(), null, null); //$NON-NLS-1$
			httpService.registerServlet("/timeout", new TimeOut(), null, null); //$NON-NLS-1$
			httpService.registerServlet("/mirrorrequest", //$NON-NLS-1$
					new IntermittentTimeout("/mirrorrequest", URI.create("http://localhost:" //$NON-NLS-1$ //$NON-NLS-2$
							+ System.getProperty("org.osgi.service.http.port", "8080") + "/public/emptyJarRepo")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					null, null);
			httpService.registerServlet("/redirect", new Redirector(), null, null); //$NON-NLS-1$

			httpService.registerServlet("/truncated", new Truncator("/truncated", URI.create("/webfiles"), 50), null, //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					null);
			httpService.registerServlet("/molested", new FileMolester("/molested", URI.create("/webfiles"), 40), null, //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					null);
			// 8 bytes at a time, delay from 0 to 100 ms, in steps of 5
			httpService.registerServlet("/decelerate", //$NON-NLS-1$
					new ChopAndDelay("/decelerate", URI.create("/webfiles"), 3, 0, new LinearChange(0, 5, 100, 0)), //$NON-NLS-1$//$NON-NLS-2$
					null, null);

			addProxyServices(httpService, SITE, "/proxy/"); //$NON-NLS-1$
			addProxyServices(httpService, SITE2, "/proxy2/"); //$NON-NLS-1$

			httpService.registerServlet("/proxy3/aprivate", //$NON-NLS-1$
					new BasicResourceDelivery("/proxy3/aprivate", URI.create(SITE2)), null, artifactSecuredHttpContext); //$NON-NLS-1$
			httpService.registerServlet("/proxy4/aprivate", //$NON-NLS-1$
					new BasicResourceDelivery("/proxy4/aprivate", URI.create(SITE3)), null, artifactSecuredHttpContext); //$NON-NLS-1$
			httpService.registerServlet("/stats", new Stats(), null, null); //$NON-NLS-1$

		} catch (NamespaceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return httpService;
	}

	/**
	 * Adds services to a location
	 *
	 * @param site
	 * @param root the "mount dir" e.g. "/proxy/" "/proxy2/" etc.
	 * @throws NamespaceException
	 * @throws ServletException
	 */
	private void addProxyServices(HttpService service, String site, String root)
			throws ServletException, NamespaceException {
		service.registerServlet(root + "truncated", new Truncator(root + "truncated", URI.create(site), 50), null, //$NON-NLS-1$//$NON-NLS-2$
				null);
		service.registerServlet(root + "public", new BasicResourceDelivery(root + "public", URI.create(site)), null, //$NON-NLS-1$//$NON-NLS-2$
				null);
		service.registerServlet(root + "private", new BasicResourceDelivery(root + "private", URI.create(site)), //$NON-NLS-1$//$NON-NLS-2$
				null, secureHttpContext);
		service.registerServlet(root + "never", new BasicResourceDelivery(root + "private", URI.create(site)), null, //$NON-NLS-1$//$NON-NLS-2$
				alwaysFail);
		service.registerServlet(root + "flipFlop", new BasicResourceDelivery(root + "private", URI.create(site)), //$NON-NLS-1$//$NON-NLS-2$
				null, flipFlop);
		service.registerServlet(root + "molested", new FileMolester(root + "molested", URI.create(site), 40), null, //$NON-NLS-1$//$NON-NLS-2$
				null);
		service.registerServlet(root + "decelerate", //$NON-NLS-1$
				new ChopAndDelay(root + "decelerate", URI.create(site), 3, 0, new LinearChange(0, 5, 100, 0)), null, //$NON-NLS-1$
				null);
		service.registerServlet(root + "decelerate2", //$NON-NLS-1$
				new ChopAndDelay(root + "decelerate2", URI.create(site), 3, 80, new LinearChange(100, 5, 105, 0)), null, //$NON-NLS-1$
				null);
		service.registerServlet(root + "readtimeout", new ChopAndDelay(root + "readtimeout", URI.create(site), 3, //$NON-NLS-1$//$NON-NLS-2$
				10, new LinearChange(10 * 60 * 1000, 5, 5 + 10 * 60 * 1000, 0)), null, null);

		// lie about modified time
		service.registerServlet(root + "modified/zero", //$NON-NLS-1$
				new LastModifiedLier(root + "modified/zero", URI.create(site), LastModifiedLier.TYPE_ZERO), null, null); //$NON-NLS-1$
		service.registerServlet(root + "modified/old", //$NON-NLS-1$
				new LastModifiedLier(root + "modified/old", URI.create(site), LastModifiedLier.TYPE_OLD), null, null); //$NON-NLS-1$
		service.registerServlet(root + "modified/now", //$NON-NLS-1$
				new LastModifiedLier(root + "modified/now", URI.create(site), LastModifiedLier.TYPE_NOW), null, null); //$NON-NLS-1$
		service.registerServlet(root + "modified/future", //$NON-NLS-1$
				new LastModifiedLier(root + "modified/future", URI.create(site), LastModifiedLier.TYPE_FUTURE), null, //$NON-NLS-1$
				null);
		service.registerServlet(root + "modified/bad", //$NON-NLS-1$
				new LastModifiedLier(root + "modified/bad", URI.create(site), LastModifiedLier.TYPE_BAD), null, null); //$NON-NLS-1$

		// lie about length
		service.registerServlet(root + "length/zero", //$NON-NLS-1$
				new ContentLengthLier(root + "length/zero", URI.create(site), 0), null, null); //$NON-NLS-1$
		service.registerServlet(root + "length/less", //$NON-NLS-1$
				new ContentLengthLier(root + "length/less", URI.create(site), 90), null, null); //$NON-NLS-1$
		service.registerServlet(root + "length/more", //$NON-NLS-1$
				new ContentLengthLier(root + "length/more", URI.create(site), 200), null, null); //$NON-NLS-1$

	}

	public static Activator getInstance() {
		return instance;
	}

	public HttpService getHttp() {
		return httpService;
	}

	@Override
	public void modifiedService(ServiceReference<HttpService> reference, HttpService service) {
		// do nothing
	}

	@Override
	public void removedService(ServiceReference<HttpService> reference, HttpService service) {
		service.unregister("/public"); //$NON-NLS-1$
		service.unregister("/private"); //$NON-NLS-1$
		service.unregister("/stats"); //$NON-NLS-1$
	}

}
