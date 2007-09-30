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
package org.eclipse.equinox.prov.ui.internal.sdk;

import org.eclipse.equinox.prov.ui.dialogs.ColocatedRepositoryManipulatorGroup;
import org.eclipse.equinox.prov.ui.viewers.InternalRepositoryFilter;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.*;

/**
 * Dialog that allows users to update, add, or remove repositories.
 * 
 * @since 3.4
 */
public class RepositoryManipulationDialog extends TrayDialog {

	private final static int WIDTH_IN_DLUS = 480;
	private final static int HEIGHT_IN_DLUS = 240;

	/**
	 * Create an instance of this Dialog.
	 * 
	 */
	public RepositoryManipulationDialog(Shell shell) {
		super(shell);
	}

	protected void configureShell(Shell shell) {
		shell.setText(ProvSDKMessages.RepositoryManipulationDialog_UpdateSitesDialogTitle);
		super.configureShell(shell);
	}

	protected Control createDialogArea(Composite parent) {
		GC gc = new GC(parent);
		gc.setFont(JFaceResources.getDialogFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();

		ColocatedRepositoryManipulatorGroup group = new ColocatedRepositoryManipulatorGroup(parent, new ViewerFilter[] {new InternalRepositoryFilter()}, WIDTH_IN_DLUS, HEIGHT_IN_DLUS, fontMetrics);
		Dialog.applyDialogFont(group.getControl());
		return group.getControl();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}

}
