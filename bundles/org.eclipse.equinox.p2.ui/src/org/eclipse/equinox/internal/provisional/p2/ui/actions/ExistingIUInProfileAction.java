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

import org.eclipse.equinox.internal.p2.ui.model.IUElement;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
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

	public ExistingIUInProfileAction(String label, Policy policy, ISelectionProvider selectionProvider, String profileId) {
		super(policy, label, selectionProvider, profileId);
	}

	protected boolean isEnabledFor(Object[] selectionArray) {
		Object parent = null;
		// We don't want to prompt for a profile during validation,
		// so we only consider the profile id.
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
						} else
							return false;
					}
					int lock = getLock(profile, element.getIU());
					if ((lock & getLockConstant()) == getLockConstant())
						return false;
					// If the parents are different, then they are either from 
					// different profiles or are nested in different parts of the tree.
					// Either way, this makes the selection invalid.
					if (parent == null) {
						parent = element.getParent(element);
					} else if (parent != element.getParent(element)) {
						return false;
					}
					// If it is not a visible IU, it is not uninstallable by the user
					String propName = getPolicy().getQueryContext().getVisibleInstalledIUProperty();
					if (propName != null && getProfileProperty(profile, element.getIU(), propName) == null) {
						return false;
					}
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

	protected boolean isSelectable(IUElement element) {
		return super.isSelectable(element) && !(element.getParent(element) instanceof IUElement);
	}

	protected boolean isSelectable(IInstallableUnit iu) {
		if (!super.isSelectable(iu))
			return false;
		IProfile profile = getProfile(false);
		int lock = getLock(profile, iu);
		return ((lock & getLockConstant()) == IInstallableUnit.LOCK_NONE);
	}

	protected abstract int getLockConstant();
}
