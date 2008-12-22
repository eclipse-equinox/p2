/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.util.Iterator;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.UpdateAction;
import org.eclipse.equinox.internal.provisional.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.viewers.*;

/**
 * UpdateHandler invokes the check for updates UI
 * 
 * @since 3.4
 */
public class UpdateHandler extends PreloadingRepositoryHandler {

	/**
	 * The constructor.
	 */
	public UpdateHandler() {
		// constructor
	}

	protected void doExecute(String profileId, QueryableMetadataRepositoryManager manager) {
		// get the profile roots
		ElementQueryDescriptor queryDescriptor = Policy.getDefault().getQueryProvider().getQueryDescriptor(new ProfileElement(null, profileId));
		Collector collector = queryDescriptor.queryable.query(queryDescriptor.query, queryDescriptor.collector, null);
		final IInstallableUnit[] roots = new IInstallableUnit[collector.size()];
		Iterator iter = collector.iterator();
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
}
