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
import java.util.Iterator;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.*;

/**
 * Content provider for profile views. The children are the known profiles, and
 * the children of profiles are wrapped IU's (wrapped in InstalledIUElement).
 * They are wrapped so that the association with the parent profile is retained.
 * 
 * @since 3.4
 */
public class ProfileContentProvider implements IStructuredContentProvider, ITreeContentProvider {

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		// we don't hook listeners or anything on the input, so there is
		// nothing to do.
	}

	public void dispose() {
		// nothing to do
	}

	public Object[] getElements(Object input) {
		// If there is no input specified, assume we are looking at all profiles.
		if (input == null) {
			return getChildren(new AllProfiles());
		}
		return getChildren(input);
	}

	public Object getParent(Object child) {
		return null;
	}

	public Object[] getChildren(Object parent) {
		if (parent instanceof AllProfiles) {
			return ((AllProfiles) parent).getChildren(parent);
		}
		if (parent instanceof Profile) {
			// We wrap installed IU's in elements so that they know their
			// "parent profile" in a particular UI view.
			Iterator allIUs = ((Profile) parent).getInstallableUnits();
			ArrayList list = new ArrayList();
			while (allIUs.hasNext()) {
				list.add(new InstalledIUElement((Profile) parent, (IInstallableUnit) allIUs.next()));
			}
			return list.toArray(new Object[list.size()]);
		}
		return new Object[0];
	}

	public boolean hasChildren(Object parent) {
		if (parent instanceof AllProfiles) {
			return ((AllProfiles) parent).getChildren(parent).length > 0;
		}
		if (parent instanceof Profile) {
			Iterator allIUs = ((Profile) parent).getInstallableUnits();
			return allIUs.hasNext();

		}
		return false;
	}
}