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
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.dialogs.AddRepositoryDialog;
import org.eclipse.equinox.p2.ui.operations.AddColocatedRepositoryOperation;
import org.eclipse.equinox.p2.ui.operations.ProvisioningOperation;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog that allows colocated metadata and artifact repositories
 * to be defined and added.
 * 
 * @since 3.4
 * 
 */
public class AddColocatedRepositoryDialog extends AddRepositoryDialog {

	public AddColocatedRepositoryDialog(Shell parentShell, IRepository[] knownRepositories) {
		super(parentShell, knownRepositories);

	}

	protected ProvisioningOperation getOperation(URL url) {
		return new AddColocatedRepositoryOperation(getShell().getText(), url);
	}

	protected URL makeRepositoryURL(String urlString) {
		// TODO need to do better validation of the URL
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=211102	
		URL newURL;
		try {
			newURL = new URL(urlString);
		} catch (MalformedURLException e) {
			// TODO need friendlier user message rather than just reporting exception
			ProvUI.handleException(e, ProvSDKMessages.AddColocatedRepositoryDialog_InvalidURL);
			return null;
		}
		String urlSpec = newURL.toExternalForm();
		try {
			if (!urlSpec.endsWith("/")) //$NON-NLS-1$
				urlSpec += "/"; //$NON-NLS-1$
			newURL = new URL(urlSpec);
		} catch (MalformedURLException e) {
			return null;
		}
		return newURL;
	}
}
