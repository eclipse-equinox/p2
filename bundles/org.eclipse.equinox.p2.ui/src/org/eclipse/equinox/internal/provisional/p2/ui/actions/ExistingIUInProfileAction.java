/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.actions;

import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
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

	private String lastValidatedProfileId;

	public ExistingIUInProfileAction(String label, Policy policy, ISelectionProvider selectionProvider, String profileId) {
		super(policy, label, selectionProvider, profileId);
	}

	protected boolean isEnabledFor(Object[] selectionArray) {
		Object parent = null;
		lastValidatedProfileId = null;
		// We don't want to prompt for a profile during validation,
		// so we only consider the profile id that was set, or the profile
		// referred to by the element itself..
		IProfile profile = getProfile(false);
		if (selectionArray.length > 0) {
			for (int i = 0; i < selectionArray.length; i++) {
				if (selectionArray[i] instanceof InstalledIUElement) {
					InstalledIUElement element = (InstalledIUElement) selectionArray[i];
					// If we couldn't find a profile in the action itself, check the element's queryable
					if (profile == null) {
						IQueryable queryable = element.getQueryable();
						if (queryable instanceof IProfile) {
							profile = (IProfile) queryable;
							lastValidatedProfileId = profile.getProfileId();
						} else
							return false;
					}
					// If the parents are different, then they are either from 
					// different profiles or are nested in different parts of the tree.
					// Either way, this makes the selection invalid.
					if (parent == null) {
						parent = element.getParent(element);
					} else if (parent != element.getParent(element)) {
						lastValidatedProfileId = null;
						return false;
					}
					// Now consider the validity of the element on its own
					if (!isSelectable(element.getIU(), profile))
						return false;
				} else {
					IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(selectionArray[i], IInstallableUnit.class);
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
			IProfile profile = (IProfile) ProvUI.getAdapter(parent, IProfile.class);
			if (profile != null)
				return isSelectable(element.getIU(), profile);
		}
		return false;
	}

	protected boolean isSelectable(IInstallableUnit iu) {
		if (!super.isSelectable(iu))
			return false;
		IProfile profile = getProfile(false);
		if (profile != null) {
			return isSelectable(iu, profile);
		}
		return false;
	}

	private boolean isSelectable(IInstallableUnit iu, IProfile profile) {
		int lock = getLock(profile, iu);
		if ((lock & getLockConstant()) == getLockConstant())
			return false;
		String propName = getPolicy().getQueryContext().getVisibleInstalledIUProperty();
		if (propName != null && getProfileProperty(profile, iu, propName) == null) {
			return false;
		}
		return true;
	}

	/*
	 * Overridden to consider the profile parent of the elements
	 * if one was not specified in the actions.
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.actions.ProfileModificationAction#getProfileId(boolean)
	 */
	protected String getProfileId(boolean chooseProfile) {
		if (profileId == null && lastValidatedProfileId != null)
			return lastValidatedProfileId;
		return super.getProfileId(chooseProfile);
	}

	protected abstract int getLockConstant();
}
