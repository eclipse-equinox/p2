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
package org.eclipse.equinox.internal.p2.installer.ui;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.installer.InstallerActivator;
import org.eclipse.equinox.p2.installer.IInstallOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;

/**
 * A simple implementation of a progress monitor dialog, used for running
 * an install operation.
 */
public class InstallDialog {
	/**
	 * Encapsulates a result passed from an operation running in a background
	 * thread to the UI thread.
	 */
	static class Result {
		private boolean done;
		private IStatus status;

		synchronized boolean isDone() {
			return done;
		}

		synchronized IStatus getStatus() {
			return status;
		}

		synchronized void done() {
			done = true;
		}

		synchronized void failed(Throwable t) {
			String msg = "An internal error has occurred";
			status = new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, msg, t);
		}

		public void setStatus(IStatus status) {
			this.status = status;
		}
	}

	/**
	 * A progress monitor implementation that asynchronously updates the progress bar.
	 */
	class Monitor implements IProgressMonitor {
		double totalWork, usedWork;
		boolean canceled = false, done = false;
		String taskName, subTaskName;

		public void beginTask(final String name, final int work) {
			totalWork = work;
			update();
		}

		void update() {
			Display display = getDisplay();
			if (display == null)
				return;
			display.asyncExec(new Runnable() {
				public void run() {
					taskLabel.setText(taskName);
					subTaskLabel.setText(subTaskName);
					progress.setVisible(!done);
					progress.setMaximum(1000);
					progress.setMinimum(0);
					int value = (int) (usedWork / totalWork * 1000);
					if (progress.getSelection() < value)
						progress.setSelection(value);
				}
			});
		}

		public void done() {
			done = true;
			update();
		}

		public void internalWorked(double work) {
			usedWork = Math.min(usedWork + work, totalWork);
			update();
		}

		public boolean isCanceled() {
			return canceled;
		}

		public void setCanceled(boolean value) {
			this.canceled = value;
		}

		public void setTaskName(String name) {
			taskName = name;
			update();
		}

		public void subTask(String name) {
			subTaskName = name;
			update();
		}

		public void worked(int work) {
			internalWorked(work);
		}
	}

	private Shell shell;
	private Composite contents;
	private Button closeButton;
	Label taskLabel;
	Label subTaskLabel;
	ProgressBar progress;

	/**
	 * Flag indicating whether the user has indicated if it is ok to close the dialog
	 */
	private boolean okToClose;

	/**
	 * Creates and opens a progress monitor dialog.
	 */
	public InstallDialog() {
		shell = new Shell(SWT.APPLICATION_MODAL);
		shell.setBounds(300, 200, 600, 400);
		contents = new Composite(shell, SWT.NONE);
		contents.setLayout(new RowLayout(SWT.VERTICAL));
		taskLabel = new Label(contents, SWT.WRAP | SWT.LEFT);
		progress = new ProgressBar(contents, SWT.HORIZONTAL | SWT.SMOOTH);
		progress.setVisible(false);
		subTaskLabel = new Label(contents, SWT.WRAP | SWT.LEFT);
		closeButton = new Button(contents, SWT.PUSH);
		closeButton.setLayoutData(new RowData(60, 20));
		closeButton.setText("Ok");
		closeButton.setVisible(false);
		closeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setOkToClose();
			}
		});
	}

	public Display getDisplay() {
		Shell theShell = shell;
		if (theShell == null || theShell.isDisposed())
			return null;
		return theShell.getDisplay();
	}

	public Shell getShell() {
		return shell;
	}

	public void close() {
		shell.dispose();
		shell = null;
	}

	synchronized void setOkToClose() {
		okToClose = true;
	}

	/**
	 * Shows a message in the dialog.
	 */
	public void setMessage(String message) {
		taskLabel.setText(message);
	}

	/**
	 * This method runs the given operation in the context of a progress dialog.
	 * The dialog is opened automatically prior to starting the operation, and closed
	 * automatically upon completion.
	 * <p>
	 * This method must be called from the UI thread. The operation will be
	 * executed outside the UI thread.
	 * 
	 * @param operation The operation to run
	 * @return The result of the operation
	 */
	public IStatus run(final IInstallOperation operation) {
		final Result result = new Result();
		Thread thread = new Thread() {
			public void run() {
				try {
					result.setStatus(operation.install(new Monitor()));
				} catch (ThreadDeath t) {
					//must rethrow or the thread won't die
					throw t;
				} catch (RuntimeException t) {
					result.failed(t);
				} catch (Error t) {
					result.failed(t);
				} finally {
					result.done();
					//kick the event loop
					Display display = getDisplay();
					if (display != null)
						display.wake();
				}
			}
		};
		thread.start();
		Display display = getDisplay();
		while (!result.isDone()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return result.getStatus();
	}

	/**
	 * Asks the user to close the dialog, and returns once the dialog is closed.
	 */
	public void promptForClose() {
		Display display = getDisplay();
		if (display == null)
			return;
		closeButton.setVisible(true);
		while (!canClose()) {
			while (!display.readAndDispatch())
				display.sleep();
		}

	}

	private synchronized boolean canClose() {
		return okToClose;
	}

}