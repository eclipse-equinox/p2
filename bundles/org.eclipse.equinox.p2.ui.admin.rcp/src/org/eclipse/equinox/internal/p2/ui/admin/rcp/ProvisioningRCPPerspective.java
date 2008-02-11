/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin.rcp;

import org.eclipse.ui.*;

/**
 * Perspective which makes the standard provisioning views available.
 * 
 * @since 3.4
 * 
 */
public class ProvisioningRCPPerspective implements IPerspectiveFactory {

	private IPageLayout factory;
	private static final String METADATA_REPOSITORIES = "org.eclipse.p2.ui.admin.MetadataRepositoriesView"; //$NON-NLS-1$
	private static final String ARTIFACT_REPOSITORIES = "org.eclipse.p2.ui.admin.ArtifactRepositoriesView"; //$NON-NLS-1$
	private static final String PROFILES = "org.eclipse.p2.ui.admin.ProfilesView"; //$NON-NLS-1$

	public ProvisioningRCPPerspective() {
		super();
	}

	public void createInitialLayout(IPageLayout layout) {
		this.factory = layout;
		addViews();
	}

	private void addViews() {
		IFolderLayout top = factory.createFolder("top", //$NON-NLS-1$
				IPageLayout.TOP, 0.5f, factory.getEditorArea());
		top.addView(METADATA_REPOSITORIES);
		factory.getViewLayout(METADATA_REPOSITORIES).setCloseable(false);
		top.addView(ARTIFACT_REPOSITORIES);
		factory.getViewLayout(ARTIFACT_REPOSITORIES).setCloseable(false);
		factory.addView(PROFILES, IPageLayout.BOTTOM, 0.65f, "top"); //$NON-NLS-1$
		factory.getViewLayout(PROFILES).setCloseable(false);
		factory.setEditorAreaVisible(false);
	}
}
