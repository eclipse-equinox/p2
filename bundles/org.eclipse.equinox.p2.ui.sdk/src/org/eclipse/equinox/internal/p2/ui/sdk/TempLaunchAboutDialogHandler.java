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
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.QueryableMetadataRepositoryManager;

/**
 * TempLaunchAboutDialogHandler invokes the preliminary version
 * of the pluggable about dialog.  This handler should go away when
 * the dialog is actually hooked into the platform about support
 * 
 * @since 3.5
 */
public class TempLaunchAboutDialogHandler extends PreloadingRepositoryHandler {

	/**
	 * The constructor.
	 */
	public TempLaunchAboutDialogHandler() {
		super();
	}

	protected void doExecute(String profileId, QueryableMetadataRepositoryManager manager) {
		ProvUI.openInstallationDialog(null);
	}

	protected boolean preloadRepositories() {
		return false;
	}
}
