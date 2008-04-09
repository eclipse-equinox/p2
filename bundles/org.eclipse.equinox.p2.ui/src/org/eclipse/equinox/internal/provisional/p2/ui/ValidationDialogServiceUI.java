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
package org.eclipse.equinox.internal.provisional.p2.ui;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.UserValidationDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class ValidationDialogServiceUI implements IServiceUI {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.IServiceUI#getUsernamePassword(java.lang.String)
	 */
	public String[] getUsernamePassword(final String location) {

		final Object[] result = new Object[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			public void run() {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

				String[] buttonLabels = new String[] {ProvUIMessages.ServiceUI_OK, ProvUIMessages.ServiceUI_Cancel};
				String message = NLS.bind(ProvUIMessages.ServiceUI_LoginDetails, location);
				UserValidationDialog dialog = new UserValidationDialog(shell, ProvUIMessages.ServiceUI_LoginRequired, null, message, buttonLabels);
				if (dialog.open() == Window.OK) {
					result[0] = dialog.getResult();
				}
			}

		});
		return result[0] instanceof String[] ? (String[]) result[0] : null;
	}
}
