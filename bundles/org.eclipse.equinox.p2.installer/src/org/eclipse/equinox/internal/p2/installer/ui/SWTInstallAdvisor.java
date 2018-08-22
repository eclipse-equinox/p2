/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	public IStatus performInstall(IInstallOperation operation) {
		return dialog.run(operation);
	}

	@Override
	public InstallDescription prepareInstallDescription(InstallDescription description) {
		if (description.getInstallLocation() == null)
			dialog.promptForLocations(description);
		return description;
	}

	@Override
	public boolean promptForLaunch(InstallDescription description) {
		return dialog.promptForLaunch(description);
	}

	@Override
	public void setResult(IStatus status) {
		String message;
		if (status.getSeverity() == IStatus.CANCEL) {
			message = Messages.Advisor_Canceled;
		} else {
			message = status.getMessage();
		}
		dialog.promptForClose(message);
	}

	@Override
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

	@Override
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
		display.syncExec(() -> activeDialog.close());
		display.dispose();
	}

}
