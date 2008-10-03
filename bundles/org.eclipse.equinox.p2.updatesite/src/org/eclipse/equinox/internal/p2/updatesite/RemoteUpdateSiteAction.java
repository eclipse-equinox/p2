/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.osgi.util.NLS;

/**
 * A publishing action that processes a remote (URL-based) update site and generates
 * metadata and artifacts for the features, bundles and site index (categories etc).  The 
 * IUs generated for the bundles are "partial" as the bundles themselves are not downloaded.
 */
public class RemoteUpdateSiteAction implements IPublisherAction {
	protected String source;
	private UpdateSite updateSite;

	public RemoteUpdateSiteAction(UpdateSite updateSite) {
		this.updateSite = updateSite;
	}

	public RemoteUpdateSiteAction(String source) {
		this.source = source;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
		IPublisherAction[] actions = createActions();
		MultiStatus finalStatus = new MultiStatus(this.getClass().getName(), 0, NLS.bind(Messages.Error_Generation, source != null ? source : (updateSite != null ? updateSite.getLocation().toExternalForm() : "Unknown")), null); //$NON-NLS-1$
		for (int i = 0; i < actions.length; i++) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			finalStatus.merge(actions[i].perform(info, results, monitor));
		}
		if (!finalStatus.isOK())
			return finalStatus;
		return Status.OK_STATUS;
	}

	protected IPublisherAction[] createActions() {
		ArrayList result = new ArrayList();
		result.add(new RemoteFeaturesAction(updateSite));
		result.add(createSiteXMLAction());
		return (IPublisherAction[]) result.toArray(new IPublisherAction[result.size()]);
	}

	private IPublisherAction createSiteXMLAction() {
		if (updateSite != null)
			return new SiteXMLAction(updateSite);
		if (source != null) {
			try {
				return new SiteXMLAction(new URL(source + "/site.xml")); //$NON-NLS-1$
			} catch (MalformedURLException e) {
				// never happens
				return null;
			}
		}
		return null;
	}
}
