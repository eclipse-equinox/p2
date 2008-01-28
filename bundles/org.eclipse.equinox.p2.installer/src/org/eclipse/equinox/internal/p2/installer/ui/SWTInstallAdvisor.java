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
package org.eclipse.equinox.internal.p2.installer.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.installer.Messages;
import org.eclipse.equinox.internal.provisional.p2.installer.*;
import org.eclipse.swt.widgets.Display;

/**
 * Install context that creates a simple SWT-based UI and interacts with a user.
 */
public class SWTInstallAdvisor extends InstallAdvisor {
	private InstallDialog dialog;
	private boolean started = false;
	private boolean stopped = false;

	public IStatus performInstall(IInstallOperation operation) {
		return dialog.run(operation);
	}

	public InstallDescription prepareInstallDescription(InstallDescription description) {
		if (description.getInstallLocation() == null)
			dialog.promptForLocations(description);
		return description;
	}

	public boolean promptForLaunch(InstallDescription description) {
		return dialog.promptForLaunch(description);
	}

	public void setResult(IStatus status) {
		String message;
		if (status.getSeverity() == IStatus.CANCEL) {
			message = Messages.Advisor_Canceled;
		} else {
			message = status.getMessage();
		}
		dialog.promptForClose(message);
	}

	public synchronized void start() {
		if (stopped || started)
			return;
		started = true;
		Display display = Display.getCurrent();
		if (display == null)
			display = new Display();
		dialog = new InstallDialog();
		dialog.setMessage(Messages.Advisor_Preparing);
	}

	public synchronized void stop() {
		if (stopped || !started)
			return;
		stopped = true;
		final InstallDialog activeDialog = dialog;
		if (activeDialog == null)
			return;
		//clear the window now, so the reference is gone no matter what happens during cleanup
		dialog = null;
		final Display display = activeDialog.getDisplay();
		if (display == null || display.isDisposed())
			return;
		display.syncExec(new Runnable() {
			public void run() {
				activeDialog.close();
			}
		});
		display.dispose();
	}

}
