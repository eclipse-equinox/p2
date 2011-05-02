/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.actions;

import org.eclipse.equinox.p2.query.QueryUtil;

import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.ISelectionProvider;

/**
 * 
 * Abstract class that implements the enablement rules for actions that
 * affect IU's already in a profile.  The action only enables when all of the
 * IU's involved are top level IU's from the same profile.
 * 
 * @since 3.5
 *
 */
public abstract class ExistingIUInProfileAction extends ProfileModificationAction {

	public ExistingIUInProfileAction(ProvisioningUI ui, String label, ISelectionProvider selectionProvider, String profileId) {
		super(ui, label, selectionProvider, profileId);
	}

	protected boolean isEnabledFor(Object[] selectionArray) {
		Object parent = null;
		// We don't want to prompt for a profile during validation,
		// so we only consider the profile id that was set, or the profile
		// referred to by the element itself..
		IProfile profile = getProfile();
		if (selectionArray.length > 0) {
			for (int i = 0; i < selectionArray.length; i++) {
				if (selectionArray[i] instanceof InstalledIUElement) {
					InstalledIUElement element = (InstalledIUElement) selectionArray[i];
					// If the parents are different, then they are either from 
					// different profiles or are nested in different parts of the tree.
					// Either way, this makes the selection invalid.
					if (parent == null) {
						parent = element.getParent(element);
					} else if (parent != element.getParent(element)) {
						return false;
					}
					// Now consider the validity of the element on its own
					if (!isSelectable(element.getIU(), profile))
						return false;
				} else {
					IInstallableUnit iu = ProvUI.getAdapter(selectionArray[i], IInstallableUnit.class);
					if (iu == null || !isSelectable(iu))
						return false;
				}
			}
			return true;
		}
		return false;
	}

	protected boolean isSelectable(IIUElement element) {
		if (!super.isSelectable(element))
			return false;
		Object parent = element.getParent(element);
		if (parent != null) {
			IProfile profile = ProvUI.getAdapter(parent, IProfile.class);
			if (profile != null)
				return isSelectable(element.getIU(), profile);
		}
		return false;
	}

	protected boolean isSelectable(IInstallableUnit iu) {
		if (!super.isSelectable(iu))
			return false;
		return isSelectable(iu, getProfile());
	}

	private boolean isSelectable(IInstallableUnit iu, IProfile profile) {
		int lock = getLock(profile, iu);
		if ((lock & getLockConstant()) == getLockConstant())
			return false;
		return !profile.query(QueryUtil.createPipeQuery(QueryUtil.createIUQuery(iu), getPolicy().getVisibleInstalledIUQuery()), null).isEmpty();
	}

	protected abstract int getLockConstant();
}
