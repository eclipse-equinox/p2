/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery.compatibility;

import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.compatibility.util.TransportUtil;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.osgi.util.NLS;

/**
 * Verifies if site URIs point to valid P2 repositories.
 * 
 * @author David Green
 * @author Steffen Pingel
 */
public class SiteVerifier {

	private final Catalog catalog;

	public SiteVerifier(Catalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Determine update site availability. This may be performed automatically as part of discovery when
	 * CatalogConfiguration#isVerifyUpdateSiteAvailability() is true, or it may be invoked later by calling this method.
	 */
	public void verifySiteAvailability(IProgressMonitor monitor) {
		// NOTE: we don't put java.net.URLs in the map since it involves DNS activity when
		//       computing the hash code.
		Map<String, Collection<CatalogItem>> urlToDescriptors = new HashMap<String, Collection<CatalogItem>>();

		for (CatalogItem descriptor : catalog.getItems()) {
			String url = descriptor.getSiteUrl();
			if (url == null) {
				continue;
			}
			if (!url.endsWith("/")) { //$NON-NLS-1$
				url += "/"; //$NON-NLS-1$
			}
			Collection<CatalogItem> collection = urlToDescriptors.get(url);
			if (collection == null) {
				collection = new ArrayList<CatalogItem>();
				urlToDescriptors.put(url, collection);
			}
			collection.add(descriptor);
		}
		final int totalTicks = urlToDescriptors.size();
		monitor.beginTask(Messages.SiteVerifier_Verify_Job_Label, totalTicks);
		try {
			if (!urlToDescriptors.isEmpty()) {
				ExecutorService executorService = Executors.newFixedThreadPool(Math.min(urlToDescriptors.size(), 4));
				try {
					List<Future<VerifyUpdateSiteJob>> futures = new ArrayList<Future<VerifyUpdateSiteJob>>(urlToDescriptors.size());
					for (String url : urlToDescriptors.keySet()) {
						futures.add(executorService.submit(new VerifyUpdateSiteJob(url)));
					}
					for (Future<VerifyUpdateSiteJob> jobFuture : futures) {
						try {
							for (;;) {
								try {
									VerifyUpdateSiteJob job = jobFuture.get(1L, TimeUnit.SECONDS);

									Collection<CatalogItem> descriptors = urlToDescriptors.get(job.url);
									for (CatalogItem descriptor : descriptors) {
										descriptor.setAvailable(job.ok);
									}
									break;
								} catch (TimeoutException e) {
									if (monitor.isCanceled()) {
										return;
									}
								}
							}
						} catch (InterruptedException e) {
							monitor.setCanceled(true);
							return;
						} catch (ExecutionException e) {
							if (e.getCause() instanceof OperationCanceledException) {
								monitor.setCanceled(true);
								return;
							}
							LogHelper.log(computeStatus(e, Messages.SiteVerifier_Unexpected_Error));
						}
						monitor.worked(1);
					}
				} finally {
					executorService.shutdownNow();
				}
			}
		} finally {
			monitor.done();
		}
	}

	private IStatus computeStatus(ExecutionException e, String message) {
		Throwable cause = e.getCause();
		if (cause.getMessage() != null) {
			message = NLS.bind(Messages.SiteVerifier_Error_with_cause, message, cause.getMessage());
		}
		return new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, message, e);
	}

	private static class VerifyUpdateSiteJob implements Callable<VerifyUpdateSiteJob> {

		private final String url;

		private boolean ok = false;

		public VerifyUpdateSiteJob(String url) {
			this.url = url;
		}

		public VerifyUpdateSiteJob call() throws Exception {
			URL baseUrl = new URL(url);
			List<URI> locations = new ArrayList<URI>();
			for (String location : new String[] {"content.jar", "content.xml", "site.xml", "compositeContent.jar", "compositeContent.xml"}) { //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				locations.add(new URL(baseUrl, location).toURI());
			}
			ok = TransportUtil.verifyAvailability(locations, true, new NullProgressMonitor());
			return this;
		}

	}

}
