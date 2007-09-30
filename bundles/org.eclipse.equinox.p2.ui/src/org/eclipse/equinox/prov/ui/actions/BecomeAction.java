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

package org.eclipse.equinox.prov.ui.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.prov.engine.Profile;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.ui.IProfileChooser;
import org.eclipse.equinox.prov.ui.internal.ProvUIMessages;
import org.eclipse.equinox.prov.ui.operations.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;

public class BecomeAction extends ProfileModificationAction {
	public static final int ENTRYPOINT_FORCE = 1;
	public static final int ENTRYPOINT_OPTIONAL = 2;
	public static final int ENTRYPOINT_NEVER = 3;
	int entryPointStrategy = ENTRYPOINT_NEVER;

	public BecomeAction(String text, ISelectionProvider selectionProvider, IOperationConfirmer confirmer, Profile profile, IProfileChooser chooser, Shell shell) {
		super(text, selectionProvider, confirmer, profile, chooser, shell);
	}

	protected ProfileModificationOperation validateAndGetOperation(IInstallableUnit[] toBecome, Profile targetProfile, IProgressMonitor monitor) {
		return new BecomeOperation(ProvUIMessages.Ops_BecomeIUOperationLabel, targetProfile.getProfileId(), toBecome[0]);
	}

}