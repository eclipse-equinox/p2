/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

/**
 * A dialog which prompts the user to restart.
 * 
 * @since 3.4
 */
public class ApplyProfileChangesDialog extends MessageDialog {
	public static final int PROFILE_IGNORE = 0;
	public static final int PROFILE_APPLYCHANGES = 1;
	public static final int PROFILE_RESTART = 2;
	private final static String[] yesNo = new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL};
	private final static String[] yesNoApply = new String[] {ProvUIMessages.ApplyProfileChangesDialog_Restart, ProvUIMessages.ApplyProfileChangesDialog_NotYet, ProvUIMessages.ApplyProfileChangesDialog_ApplyChanges};

	private int returnCode = PROFILE_IGNORE;

	private ApplyProfileChangesDialog(Shell parent, String title, String message, boolean mustRestart) {
		super(parent, title, null, // accept the default window icon
				message, QUESTION, mustRestart ? yesNo : yesNoApply, 0); // yes is the default
	}

	/**
	 * Prompt the user for restart or apply profile changes.
	 *
	 * @param parent the parent shell of the dialog, or <code>null</code> if none
	 * @param mustRestart indicates whether the user must restart to get the 
	 * 		changes.  If <code>false</code>, then the user may choose to apply
	 * 		the changes to the running profile rather than restarting.
	 * @return one of PROFILE_IGNORE (do nothing), PROFILE_APPLYCHANGES 
	 * 		(attempt to apply the changes), or PROFILE_RESTART (restart the system).
	 */
	public static int promptForRestart(Shell parent, boolean mustRestart) {
		String title = ProvUIMessages.PlatformUpdateTitle;
		IProduct product = Platform.getProduct();
		String productName = product != null && product.getName() != null ? product.getName() : ProvUIMessages.ApplicationInRestartDialog;
		String message = NLS.bind(mustRestart ? ProvUIMessages.PlatformRestartMessage : ProvUIMessages.OptionalPlatformRestartMessage, productName);
		ApplyProfileChangesDialog dialog = new ApplyProfileChangesDialog(parent, title, message, mustRestart);
		if (dialog.open() == Window.CANCEL)
			return PROFILE_IGNORE;
		return dialog.returnCode;
	}

	/**
	 * When a button is pressed, store the return code.
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int id) {
		if (id == 0) { // YES
			returnCode = PROFILE_RESTART;
		} else if (id == 1) { // NO
			returnCode = PROFILE_IGNORE;
		} else {
			returnCode = PROFILE_APPLYCHANGES;
		}

		super.buttonPressed(id);
	}
}
