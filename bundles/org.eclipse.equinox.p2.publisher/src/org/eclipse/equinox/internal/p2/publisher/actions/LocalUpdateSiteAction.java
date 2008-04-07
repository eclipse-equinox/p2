/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.p2.publisher.features.UpdateSite;

/**
 * A publishing action that processes a local (File-based) update site and generates
 * metadata and artifacts for the features, bundles and site index (categories etc).
 */
public class LocalUpdateSiteAction implements IPublishingAction {
	protected String source;
	private UpdateSite updateSite;

	protected LocalUpdateSiteAction() {
	}

	public LocalUpdateSiteAction(String source) {
		this.source = source;
	}

	public LocalUpdateSiteAction(UpdateSite updateSite) {
		this.updateSite = updateSite;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		IPublishingAction[] actions = createActions();
		for (int i = 0; i < actions.length; i++)
			actions[i].perform(info, results);
		return Status.OK_STATUS;
	}

	protected IPublishingAction[] createActions() {
		createAdvice();
		ArrayList result = new ArrayList();
		// create an action that just publishes the raw bundles and features
		IPublishingAction action = new MergeResultsAction(new IPublishingAction[] {createFeaturesAction(), createBundlesAction()}, IPublisherResult.MERGE_ALL_NON_ROOT);
		result.add(action);
		result.add(createSiteXMLAction());
		return (IPublishingAction[]) result.toArray(new IPublishingAction[result.size()]);
	}

	private IPublishingAction createSiteXMLAction() {
		if (updateSite != null)
			return new SiteXMLAction(updateSite);
		if (source != null) {
			try {
				return new SiteXMLAction(new File(source, "site.xml").toURL()); //$NON-NLS-1$
			} catch (MalformedURLException e) {
				// never happens
				return null;
			}
		}
		return null;
	}

	private void createAdvice() {
	}

	protected IPublishingAction createFeaturesAction() {
		return new FeaturesAction(new File[] {new File(source, "features")}); //$NON-NLS-1$
	}

	protected IPublishingAction createBundlesAction() {
		return new BundlesAction(new File[] {new File(source, "plugins")}); //$NON-NLS-1$
	}

}
