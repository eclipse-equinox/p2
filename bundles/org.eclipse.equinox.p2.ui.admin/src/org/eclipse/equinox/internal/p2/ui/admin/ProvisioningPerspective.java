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
package org.eclipse.equinox.internal.p2.ui.admin;

import org.eclipse.ui.*;

/**
 * Perspective which makes the standard provisioning views available.
 * 
 * @since 3.4
 * 
 */
public class ProvisioningPerspective implements IPerspectiveFactory {

	private IPageLayout factory;

	public ProvisioningPerspective() {
		super();
	}

	public void createInitialLayout(IPageLayout layout) {
		this.factory = layout;
		addViews();
		addActionSets();
		addNewWizardShortcuts();
		addPerspectiveShortcuts();
		addViewShortcuts();
	}

	private void addViews() {
		// Creates the overall folder layout.
		// Note that each new Folder uses a percentage of the remaining
		// EditorArea.

		IFolderLayout bottom = factory.createFolder("bottomRight", //$NON-NLS-1$
				IPageLayout.BOTTOM, 0.75f, factory.getEditorArea());
		bottom.addView("org.eclipse.p2.ui.admin.ProfilesView"); //$NON-NLS-1$

		IFolderLayout topLeft = factory.createFolder("topLeft", //$NON-NLS-1$
				IPageLayout.LEFT, 0.4f, factory.getEditorArea());
		topLeft.addView("org.eclipse.p2.ui.admin.MetadataRepositoriesView"); //$NON-NLS-1$
		topLeft.addView("org.eclipse.p2.ui.admin.ArtifactRepositoriesView"); //$NON-NLS-1$
	}

	private void addActionSets() {
		factory.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET); // NON-NLS-1
	}

	private void addPerspectiveShortcuts() {
		factory.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective"); //$NON-NLS-1$
	}

	private void addNewWizardShortcuts() {
		// factory.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");//NON-NLS-1
	}

	private void addViewShortcuts() {
		factory.addShowViewShortcut("org.eclipse.p2.ui.admin.MetadataRepositoriesView"); //$NON-NLS-1$
		factory.addShowViewShortcut("org.eclipse.p2.ui.admin.ArtifactRepositoriesView"); //$NON-NLS-1$
		factory.addShowViewShortcut("org.eclipse.p2.ui.admin.ProfilesView"); //$NON-NLS-1$
	}

}
