/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.ui.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.IProfileChooser;
import org.eclipse.equinox.p2.ui.operations.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;

public class UninstallAction extends ProfileModificationAction {

	public UninstallAction(String text, ISelectionProvider selectionProvider, IOperationConfirmer confirmer, Profile profile, IProfileChooser chooser, Shell shell) {
		super(text, selectionProvider, confirmer, profile, chooser, shell);
	}

	protected ProfileModificationOperation validateAndGetOperation(IInstallableUnit[] ius, Profile targetProfile, IProgressMonitor monitor) {
		return new UninstallOperation(ProvUIMessages.Ops_UninstallIUOperationLabel, targetProfile.getProfileId(), ius);
	}
}