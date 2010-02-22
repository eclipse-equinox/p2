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
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.ISelectionProvider;

public class UninstallAction extends ExistingIUInProfileAction {

	public UninstallAction(ProvisioningUI ui, ISelectionProvider selectionProvider, String profileId) {
		super(ui, ProvUI.UNINSTALL_COMMAND_LABEL, selectionProvider, profileId);
		setToolTipText(ProvUI.UNINSTALL_COMMAND_TOOLTIP);
	}

	protected String getTaskName() {
		return ProvUIMessages.UninstallIUProgress;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.actions.AlterExistingProfileIUAction#getLockConstant()
	 */
	protected int getLockConstant() {
		return IProfile.LOCK_UNINSTALL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.actions.ProfileModificationAction#getProfileChangeOperation(org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit[])
	 */
	protected ProfileChangeOperation getProfileChangeOperation(Collection<IInstallableUnit> ius) {
		return ui.getUninstallOperation(ius, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.actions.ProfileModificationAction#performAction(org.eclipse.equinox.p2.operations.ProfileChangeOperation, org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit[])
	 */
	protected int performAction(ProfileChangeOperation operation, Collection<IInstallableUnit> ius) {
		return ui.openUninstallWizard(ius, (UninstallOperation) operation, null);
	}
}
