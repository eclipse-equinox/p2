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
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider for profile views. The children are the known profiles, and
 * the children of profiles are wrapped IU's (wrapped in InstalledIUElement).
 * They are wrapped so that the association with the parent profile is retained.
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
		// If there is no input specified, assume we are looking at all
		// metadata repositories.
		if (input == null) {
			return getElements(new AllMetadataRepositories());
		}
		IMetadataRepository[] reposToCheck = new IMetadataRepository[0];
		if (input instanceof IMetadataRepository[]) {
			reposToCheck = (IMetadataRepository[]) input;
		} else if (input instanceof AllMetadataRepositories) {
			reposToCheck = (IMetadataRepository[]) ((AllMetadataRepositories) input).getChildren(null);
		}
		ArrayList list = new ArrayList();
		for (int i = 0; i < reposToCheck.length; i++) {
			// TODO maybe this should be configurable, but for now assume we never
			// want to see content from implementation repositories.
			if (!(Boolean.valueOf(reposToCheck[i].getProperties().getProperty(IRepository.IMPLEMENTATION_ONLY_KEY, "false"))).booleanValue()) { //$NON-NLS-1$
				IInstallableUnit[] ius = reposToCheck[i].getInstallableUnits(null);
				for (int j = 0; j < ius.length; j++) {
					list.add(ius[j]);
				}
			}
		}
		return list.toArray();
	}
}