/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.examples.rcp.sdkbundlevisibility.p2;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ElementQueryDescriptor;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.UpdateAction;
import org.eclipse.equinox.internal.provisional.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * UpdateHandler invokes the check for updates UI
 * 
 * @since 3.4
 */
public class UpdateHandler extends PreloadingRepositoryHandler {

	boolean hasNoRepos = false;

	/**
	 * The constructor.
	 */
	public UpdateHandler() {
		// constructor
	}

	protected void doExecute(String profileId, QueryableMetadataRepositoryManager manager) {
		if (hasNoRepos) {
			boolean goToSites = MessageDialog.openQuestion(getShell(), Messages.UpdateHandler_NoSitesTitle, Messages.UpdateHandler_NoSitesMessage);
			if (goToSites) {
				Policy.getDefault().getRepositoryManipulator().manipulateRepositories(getShell());
			}
			return;
		}
		// get the profile roots
		ElementQueryDescriptor queryDescriptor = Policy.getDefault().getQueryProvider().getQueryDescriptor(new ProfileElement(null, profileId));
		Collection collection = queryDescriptor.performQuery(null);
		final IInstallableUnit[] roots = new IInstallableUnit[collection.size()];
		Iterator iter = collection.iterator();
		int i = 0;
		while (iter.hasNext()) {
			roots[i] = (IInstallableUnit) ProvUI.getAdapter(iter.next(), IInstallableUnit.class);
			i++;
		}
		// now create an update action whose selection is all the roots
		UpdateAction action = new UpdateAction(Policy.getDefault(), new ISelectionProvider() {

			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				// not dynamic
			}

			public ISelection getSelection() {
				return new StructuredSelection(roots);
			}

			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				// not dynamic
			}

			public void setSelection(ISelection selection) {
				// not mutable

			}
		}, profileId, false);
		action.setRepositoryManager(manager);
		action.run();
	}

	protected boolean preloadRepositories() {
		hasNoRepos = false;
		RepositoryManipulator repoMan = Policy.getDefault().getRepositoryManipulator();
		if (repoMan != null && repoMan.getKnownRepositories().length == 0) {
			hasNoRepos = true;
			return false;
		}
		return super.preloadRepositories();
	}
}
