/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.osgi.util.NLS;

/**
 * A publishing action that processes a remote (URL-based) update site and generates
 * metadata and artifacts for the features, bundles and site index (categories etc).  The 
 * IUs generated for the bundles are "partial" as the bundles themselves are not downloaded.
 */
public class RemoteUpdateSiteAction implements IPublisherAction {
	private final UpdateSite updateSite;
	private final String categoryQualifier;

	/**
	 * Creates a local remote updatesite publisher action from an UpdateSite
	 * @param updateSite The UpdateSite to use
	 * @param categoryQualifier The qualifier to prepend to categories. This qualifier is used
	 * to ensure that the category IDs are unique between update sites. If <b>null</b> a default
	 * qualifier will be generated
	 */
	public RemoteUpdateSiteAction(UpdateSite updateSite, String categoryQualifier) {
		this.updateSite = updateSite;
		this.categoryQualifier = categoryQualifier;
	}

	@Override
	public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
		if (Tracing.DEBUG_PUBLISHING)
			Tracing.debug("Generating metadata for update site: " + updateSite.getLocation()); //$NON-NLS-1$
		IPublisherAction[] actions = createActions();
		MultiStatus finalStatus = new MultiStatus(this.getClass().getName(), 0, NLS.bind(Messages.Error_Generation, updateSite != null ? updateSite.getLocation().toString() : "Unknown"), null); //$NON-NLS-1$
		for (IPublisherAction action : actions) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			finalStatus.merge(action.perform(info, results, monitor));
		}
		if (Tracing.DEBUG_PUBLISHING)
			Tracing.debug("Generation for update site complete: " + updateSite.getLocation()); //$NON-NLS-1$
		if (!finalStatus.isOK())
			return finalStatus;
		return Status.OK_STATUS;
	}

	protected IPublisherAction[] createActions() {
		ArrayList<IPublisherAction> result = new ArrayList<>();
		result.add(new RemoteFeaturesAction(updateSite));
		result.add(createSiteXMLAction());
		return result.toArray(new IPublisherAction[result.size()]);
	}

	private IPublisherAction createSiteXMLAction() {
		return new SiteXMLAction(updateSite, categoryQualifier);
	}
}
