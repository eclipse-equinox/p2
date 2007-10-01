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
package org.eclipse.equinox.internal.p2.installer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.installer.InstallAdvisor;
import org.eclipse.equinox.p2.installer.IInstallDescription;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Install context that creates a GUI and interacts with a user.
 */
public class GraphicalInstallAdvisor extends InstallAdvisor {
	private ProgressMonitorDialog window;
	private boolean stopped = false;
	private boolean started = false;

	public synchronized void start() {
		if (stopped || started)
			return;
		started = true;
		Display display = Display.getCurrent();
		if (display == null)
			display = new Display();
		Shell shell = new Shell(display);
		shell.setBounds(300, 200, 600, 400);
		window = new ProgressMonitorDialog(shell);
	}

	public synchronized void stop() {
		if (stopped || !started)
			return;
		stopped = true;
		final Window activeWindow = window;
		if (activeWindow == null)
			return;
		//clear the window now, so the reference is gone no matter what happens during cleanup
		window = null;
		final Display display = activeWindow.getShell().getDisplay();
		if (display == null || display.isDisposed())
			return;
		display.syncExec(new Runnable() {
			public void run() {
				activeWindow.close();
			}
		});
		display.dispose();
	}

	public IRunnableContext getRunnableContext() {
		return window;
	}

	public String getInstallLocation(IInstallDescription description) {
		DirectoryDialog dialog = new DirectoryDialog(window.getShell());
		dialog.setMessage(NLS.bind("Where do you want to install {0}?", description.getProductName()));
		return dialog.open();
	}

	public void reportStatus(IStatus status) {
		switch (status.getSeverity()) {
			case IStatus.OK :
				break;
			case IStatus.INFO :
				MessageDialog.openInformation(window.getShell(), null, status.getMessage());
				break;
			case IStatus.WARNING :
			case IStatus.ERROR :
				ErrorDialog.openError(window.getShell(), null, null, status);
		}
	}

}
