/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.publisher.*;

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

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		IPublisherAction[] actions = createActions();
		for (int i = 0; i < actions.length; i++)
			actions[i].perform(info, results);
		return Status.OK_STATUS;
	}

	protected IPublisherAction[] createActions() {
		try {
			ArrayList result = new ArrayList();
			result.add(new RemoteFeaturesAction(updateSite.loadFeatures()));
			result.add(createSiteXMLAction());
			return (IPublisherAction[]) result.toArray(new IPublisherAction[result.size()]);
		} catch (ProvisionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new IPublisherAction[0];
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
