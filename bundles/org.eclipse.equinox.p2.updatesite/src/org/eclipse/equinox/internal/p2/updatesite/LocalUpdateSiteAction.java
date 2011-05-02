/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.io.File;
import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.MergeResultsAction;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.osgi.util.NLS;

/**
 * A publishing action that processes a local (File-based) update site and generates
 * metadata and artifacts for the features, bundles and site index (categories etc).
 */
public class LocalUpdateSiteAction implements IPublisherAction {
	protected String source;
	private UpdateSite updateSite;
	private String categoryQualifier;
	private String categoryVersion;

	protected LocalUpdateSiteAction() {
		// empty
	}

	/**
	 * Creates a local updatesite publisher action from a source location
	 * @param source The location of the directory that contains the site.xml file
	 * @param categoryQualifier The qualifier to prepend to categories. This qualifier is used
	 * to ensure that the category IDs are unique between update sites. If <b>null</b> a default
	 * qualifier will be generated
	 */
	public LocalUpdateSiteAction(String source, String categoryQualifier) {
		this.source = source;
		this.categoryQualifier = categoryQualifier;
	}

	/**
	 * Creates a local updatesite publisher action from an UpdateSite
	 * @param updateSite The updatesite to use
	 * @param categoryQualifier The qualifier to prepend to categories. This qualifier is used
	 * to ensure that the category IDs are unique between update sites.  If <b>null</b> a default
	 * qualifier will be generated
	 */
	public LocalUpdateSiteAction(UpdateSite updateSite, String categoryQualifier) {
		this.updateSite = updateSite;
		this.categoryQualifier = categoryQualifier;
	}

	public void setCategoryVersion(String version) {
		categoryVersion = version;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
		IPublisherAction[] actions = createActions();
		MultiStatus finalStatus = new MultiStatus(LocalUpdateSiteAction.class.getName(), 0, NLS.bind(Messages.Error_Generation, source != null ? source : (updateSite != null ? updateSite.getLocation().toString() : "Unknown")), null); //$NON-NLS-1$
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
		createAdvice();
		ArrayList<IPublisherAction> result = new ArrayList<IPublisherAction>();
		// create an action that just publishes the raw bundles and features
		IPublisherAction action = new MergeResultsAction(new IPublisherAction[] {createFeaturesAction(), createBundlesAction()}, IPublisherResult.MERGE_ALL_NON_ROOT);
		result.add(action);
		result.add(createSiteXMLAction());
		return result.toArray(new IPublisherAction[result.size()]);
	}

	private IPublisherAction createSiteXMLAction() {
		SiteXMLAction action = null;
		if (updateSite != null)
			action = new SiteXMLAction(updateSite, categoryQualifier);
		else if (source != null) {
			action = new SiteXMLAction(new File(source, "site.xml").toURI(), categoryQualifier); //$NON-NLS-1$
		}
		if (action != null && categoryVersion != null)
			action.setCategoryVersion(categoryVersion);
		return action;
	}

	private void createAdvice() {
		// empty
	}

	protected IPublisherAction createFeaturesAction() {
		return new FeaturesAction(new File[] {new File(source, "features")}); //$NON-NLS-1$
	}

	protected IPublisherAction createBundlesAction() {
		return new BundlesAction(new File[] {new File(source, "plugins")}); //$NON-NLS-1$
	}

}
