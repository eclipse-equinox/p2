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
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * A simple implementation of a progress monitor dialog, used for running
 * an install operation.
 */
public class InstallDialog {
	/**
	 * A progress monitor implementation that asynchronously updates the progress bar.
	 */
	class Monitor implements IProgressMonitor {

		boolean canceled = false, running = false;
		String taskName = "", subTaskName = ""; //$NON-NLS-1$//$NON-NLS-2$
		double totalWork, usedWork;

		public void beginTask(final String name, final int work) {
			totalWork = work;
			running = true;
			update();
		}

		public void done() {
			running = false;
			usedWork = totalWork;
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
			taskName = name == null ? "" : name; //$NON-NLS-1$
			update();
		}

		public void subTask(String name) {
			subTaskName = name == null ? "" : name; //$NON-NLS-1$
			update();
		}

		void update() {
			Display display = getDisplay();
			if (display == null)
				return;
			display.asyncExec(new Runnable() {
				public void run() {
					Shell theShell = getShell();
					if (theShell == null || theShell.isDisposed())
						return;
					taskLabel.setText(taskName);
					subTaskLabel.setText(shorten(subTaskName));
					if (progress.isDisposed())
						return;
					progress.setVisible(running);
					progress.setMaximum(1000);
					progress.setMinimum(0);
					int value = (int) (usedWork / totalWork * 1000);
					if (progress.getSelection() < value)
						progress.setSelection(value);
				}

				private String shorten(String text) {
					if (text.length() <= 64)
						return text;
					int len = text.length();
					return text.substring(0, 30) + "..." + text.substring(len - 30, len);
				}
			});
		}

		public void worked(int work) {
			internalWorked(work);
		}
	}

	/**
	 * Encapsulates a result passed from an operation running in a background
	 * thread to the UI thread.
	 */
	static class Result {
		private boolean done;
		private IStatus status;

		synchronized void done() {
			done = true;
		}

		synchronized void failed(Throwable t) {
			String msg = "An internal error has occurred";
			status = new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, msg, t);
		}

		synchronized IStatus getStatus() {
			return status;
		}

		synchronized boolean isDone() {
			return done;
		}

		public void setStatus(IStatus status) {
			this.status = status;
		}
	}

	private Button closeButton;
	private Composite contents;
	/**
	 * Flag indicating whether the user has indicated if it is ok to close the dialog
	 */
	private boolean okToClose;
	ProgressBar progress;
	private Shell shell;
	Label subTaskLabel;

	Label taskLabel;

	/**
	 * Creates and opens a progress monitor dialog.
	 */
	public InstallDialog() {
		shell = new Shell(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setBounds(300, 200, 600, 400);
		shell.setText("Installer");
		shell.setLayout(new FillLayout());
		contents = new Composite(shell, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		contents.setLayout(layout);
		taskLabel = new Label(contents, SWT.WRAP | SWT.LEFT);
		taskLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		progress = new ProgressBar(contents, SWT.HORIZONTAL | SWT.SMOOTH);
		progress.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		progress.setVisible(false);
		subTaskLabel = new Label(contents, SWT.WRAP | SWT.LEFT);
		subTaskLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		//spacer
		Label spacer = new Label(contents, SWT.NONE);
		spacer.setLayoutData(new GridData(GridData.FILL_BOTH));

		closeButton = new Button(contents, SWT.PUSH);
		GridData data = new GridData(80, SWT.DEFAULT);
		data.verticalAlignment = SWT.END;
		data.horizontalAlignment = SWT.RIGHT;
		closeButton.setLayoutData(data);
		closeButton.setText("OK");
		closeButton.setVisible(false);
		closeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setOkToClose();
			}
		});
		shell.layout();
		shell.open();
	}

	private synchronized boolean canClose() {
		return okToClose;
	}

	public void close() {
		shell.dispose();
		shell = null;
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

	/**
	 * Asks the user to close the dialog, and returns once the dialog is closed.
	 */
	public void promptForClose(String message) {
		Display display = getDisplay();
		if (display == null)
			return;
		taskLabel.setText(message);
		subTaskLabel.setText(""); //$NON-NLS-1$
		progress.setVisible(false);
		closeButton.setVisible(true);
		while (!canClose()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

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
					Display display = getDisplay();
					//ensure all events from the operation have run
					if (display != null) {
						display.syncExec(new Runnable() {
							public void run() {
								//do nothing
							}
						});
					}
					result.done();
					//wake the event loop
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

	public void setMessage(String message) {
		if (taskLabel != null && !taskLabel.isDisposed())
			taskLabel.setText(message);
	}

	synchronized void setOkToClose() {
		okToClose = true;
	}

}