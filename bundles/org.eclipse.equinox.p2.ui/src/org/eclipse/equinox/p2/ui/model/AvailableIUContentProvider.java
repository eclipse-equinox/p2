/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.ui.model;

import java.util.ArrayList;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider for views of available IU's.  The input
 * can be a single metadata repository, an array of repos, AllMetadataRepositories.
 * Unknown inputs will not check any repos.
 * 
 * @since 3.4
 */
public class AvailableIUContentProvider implements IStructuredContentProvider {

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		// we don't hook listeners or anything on the input, so there is
		// nothing to do.
	}

	public void dispose() {
		// nothing to do
	}

	public Object[] getElements(Object input) {
		IMetadataRepository[] reposToCheck;
		if (input instanceof IMetadataRepository[]) {
			reposToCheck = (IMetadataRepository[]) input;
		} else if (input instanceof AllMetadataRepositories) {
			Object[] children = ((AllMetadataRepositories) input).getChildren(null);
			if (children == null)
				reposToCheck = new IMetadataRepository[0];
			else {
				reposToCheck = new IMetadataRepository[children.length];
				for (int i = 0; i < children.length; i++) {
					reposToCheck[i] = (IMetadataRepository) ProvUI.getAdapter(children[i], IMetadataRepository.class);
				}
			}
		} else if (input instanceof IMetadataRepository) {
			reposToCheck = new IMetadataRepository[] {(IMetadataRepository) input};
		} else {
			reposToCheck = new IMetadataRepository[0];
		}
		ArrayList list = new ArrayList();
		for (int i = 0; i < reposToCheck.length; i++) {
			// Shouldn't happen, but if an element was not adaptable it would
			if (reposToCheck[i] == null)
				break;
			// TODO maybe this should be configurable, but for now assume we never
			// want to see content from implementation repositories.
			Object implOnly = reposToCheck[i].getProperties().get(IRepository.IMPLEMENTATION_ONLY_KEY);
			if (implOnly == null || !Boolean.valueOf((String) implOnly).booleanValue()) {
				IInstallableUnit[] ius = reposToCheck[i].getInstallableUnits(null);
				for (int j = 0; j < ius.length; j++) {
					list.add(ius[j]);
				}
			}
		}
		return list.toArray();
	}
}