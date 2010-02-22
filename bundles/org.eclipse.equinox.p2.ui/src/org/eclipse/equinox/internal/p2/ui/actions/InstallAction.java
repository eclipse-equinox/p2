/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.actions;

import java.util.Collection;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.ISelectionProvider;

public class InstallAction extends ProfileModificationAction {

	public InstallAction(ProvisioningUI ui, ISelectionProvider selectionProvider, String profileId) {
		super(ui, ProvUI.INSTALL_COMMAND_LABEL, selectionProvider, profileId);
		setToolTipText(ProvUI.INSTALL_COMMAND_TOOLTIP);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.actions.ProfileModificationAction#isEnabledFor(java.lang.Object[])
	 */
	protected boolean isEnabledFor(Object[] selectionArray) {
		if (selectionArray.length == 0)
			return false;
		// We allow non-IU's to be selected at this point, but there
		// must be at least one installable unit selected that is
		// selectable
		for (int i = 0; i < selectionArray.length; i++) {
			if (selectionArray[i] instanceof InstalledIUElement && isSelectable((IIUElement) selectionArray[i]))
				return true;
			IInstallableUnit iu = ProvUI.getAdapter(selectionArray[i], IInstallableUnit.class);
			if (iu != null && isSelectable(iu))
				return true;
		}
		return false;
	}

	/*
	 * Overridden to reject categories and nested IU's (parent is a non-category IU)
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.actions.ProfileModificationAction#isSelectable(org.eclipse.equinox.internal.p2.ui.model.IUElement)
	 */
	protected boolean isSelectable(IIUElement element) {
		return super.isSelectable(element) && !(element.getParent(element) instanceof AvailableIUElement);
	}

	protected int performAction(ProfileChangeOperation operation, Collection<IInstallableUnit> ius) {
		return getProvisioningUI().openInstallWizard(ius, (InstallOperation) operation, null);
	}

	protected ProfileChangeOperation getProfileChangeOperation(Collection<IInstallableUnit> ius) {
		InstallOperation op = new InstallOperation(getSession(), ius);
		op.setProfileId(profileId);
		//		op.setRootMarkerKey(getPolicy().getQueryContext().getVisibleInstalledIUProperty());
		return op;
	}
}
