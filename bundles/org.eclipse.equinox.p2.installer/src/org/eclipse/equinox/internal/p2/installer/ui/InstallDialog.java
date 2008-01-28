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

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.installer.InstallerActivator;
import org.eclipse.equinox.internal.provisional.p2.installer.IInstallOperation;
import org.eclipse.equinox.internal.provisional.p2.installer.InstallDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * The install wizard that drives the install. This dialog is used for user input
 * prior to the install, progress feedback during the install, and displaying results
 * at the end of the install.
 */
public class InstallDialog {
	/**
	 * A progress monitor implementation that asynchronously updates the progress bar.
	 */
	class Monitor implements IProgressMonitor {

		boolean canceled = false, running = false;
		String subTaskName = ""; //$NON-NLS-1$//$NON-NLS-2$
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
			return returnCode == CANCEL;
		}

		public void setCanceled(boolean value) {
			returnCode = CANCEL;
		}

		public void setTaskName(String name) {
			subTaskName = name == null ? "" : name; //$NON-NLS-1$
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

	private static final String EXPLAIN_SHARED = "In a shared install, common components are stored together, allowing them to be shared across multiple products.";

	private static final String EXPLAIN_STANDALONE = "In a stand-alone install, each product is installed in its own directory without any sharing between products";

	private static final int OK = 0;
	private static final int CANCEL = 1;
	int returnCode = -1;
	private boolean waitingForClose = false;

	private Button cancelButton;
	private Composite contents;
	private Label installKindExplanation;
	private Composite installSettingsGroup;

	private Text locationField;
	private Button okButton;

	ProgressBar progress;
	private Button sharedButton;
	private Shell shell;
	private Button standaloneButton;
	Label subTaskLabel;
	Label taskLabel;

	/**
	 * Creates and opens a progress monitor dialog.
	 */
	public InstallDialog() {
		createShell();
		taskLabel = new Label(contents, SWT.WRAP | SWT.LEFT);
		taskLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createInstallSettingsControls();
		createProgressControls();
		createButtonBar();

		shell.layout();
		shell.open();
	}

	protected void browsePressed() {
		DirectoryDialog dirDialog = new DirectoryDialog(shell);
		dirDialog.setMessage("Select the install location");
		String location = dirDialog.open();
		if (location == null)
			location = ""; //$NON-NLS-1$
		locationField.setText(location);
		validateInstallSettings();
	}

	public void close() {
		if (shell == null)
			return;
		if (!shell.isDisposed())
			shell.dispose();
		shell = null;
	}

	private void createButtonBar() {
		Composite buttonBar = new Composite(contents, SWT.NONE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalAlignment = SWT.RIGHT;
		buttonBar.setLayoutData(data);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonBar.setLayout(layout);

		okButton = new Button(buttonBar, SWT.PUSH);
		data = new GridData(80, SWT.DEFAULT);
		okButton.setLayoutData(data);
		okButton.setEnabled(false);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				buttonPressed(OK);
			}
		});

		cancelButton = new Button(buttonBar, SWT.PUSH);
		data = new GridData(80, SWT.DEFAULT);
		cancelButton.setLayoutData(data);
		cancelButton.setText("Cancel");
		cancelButton.setEnabled(false);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				buttonPressed(CANCEL);
			}
		});
	}

	protected void buttonPressed(int code) {
		returnCode = code;
		if (waitingForClose)
			close();
		//grey out the cancel button to indicate the request was heard
		if (code == CANCEL && !cancelButton.isDisposed())
			cancelButton.setEnabled(false);
	}

	/**
	 * Creates the controls to prompt for the agent and install locations.
	 */
	private void createInstallSettingsControls() {
		installSettingsGroup = new Composite(contents, SWT.NONE);
		GridLayout layout = new GridLayout();
		installSettingsGroup.setLayout(layout);
		installSettingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

		Listener validateListener = new Listener() {
			public void handleEvent(Event event) {
				validateInstallSettings();
			}
		};

		//The group asking for the product install directory
		Group installLocationGroup = new Group(installSettingsGroup, SWT.NONE);
		installLocationGroup.setLayout(new GridLayout());
		installLocationGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		installLocationGroup.setText("Location");
		Label installLocationLabel = new Label(installLocationGroup, SWT.NONE);
		installLocationLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		installLocationLabel.setText("Select the product install directory:");

		//The sub-group with text entry field and browse button
		Composite locationFieldGroup = new Composite(installLocationGroup, SWT.NONE);
		locationFieldGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		locationFieldGroup.setLayout(layout);
		locationField = new Text(locationFieldGroup, SWT.SINGLE | SWT.BORDER);
		locationField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		locationField.addListener(SWT.Modify, validateListener);
		Button browseButton = new Button(locationFieldGroup, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				browsePressed();
			}
		});

		//Create the radio button group asking for the kind of install (shared vs. standalone)
		Group installKindGroup = new Group(installSettingsGroup, SWT.NONE);
		installKindGroup.setText("Layout");
		installKindGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		installKindGroup.setLayout(new GridLayout());
		standaloneButton = new Button(installKindGroup, SWT.RADIO);
		standaloneButton.setText("Stand-alone install");
		standaloneButton.addListener(SWT.Selection, validateListener);
		standaloneButton.setSelection(true);
		sharedButton = new Button(installKindGroup, SWT.RADIO);
		sharedButton.setText("Shared install");
		sharedButton.addListener(SWT.Selection, validateListener);
		installKindExplanation = new Label(installKindGroup, SWT.WRAP);
		GridData data = new GridData(SWT.DEFAULT, 40);
		data.grabExcessHorizontalSpace = true;
		installKindExplanation.setLayoutData(data);
		installKindExplanation.setText(EXPLAIN_STANDALONE);

		//make the entire group invisible until we actually need to prompt for locations
		installSettingsGroup.setVisible(false);
	}

	private void createProgressControls() {
		progress = new ProgressBar(contents, SWT.HORIZONTAL | SWT.SMOOTH);
		progress.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		progress.setVisible(false);
		subTaskLabel = new Label(contents, SWT.WRAP | SWT.LEFT);
		subTaskLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private void createShell() {
		shell = new Shell(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setBounds(300, 200, 600, 400);
		shell.setText("Installer");
		shell.setLayout(new FillLayout());
		contents = new Composite(shell, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		contents.setLayout(layout);
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
		okButton.setVisible(false);
		cancelButton.setText("Close");
		cancelButton.setEnabled(true);
		waitingForClose = true;
		while (shell != null && !shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	public boolean promptForLaunch(InstallDescription description) {
		Display display = getDisplay();
		if (display == null)
			return false;
		taskLabel.setText(NLS.bind("Install complete. Do you want to start {0} immediately?", description.getProductName()));
		subTaskLabel.setText(""); //$NON-NLS-1$
		progress.setVisible(false);
		okButton.setText("Launch");
		okButton.setVisible(true);
		cancelButton.setText("Close");
		cancelButton.setVisible(true);
		waitingForClose = true;
		while (shell != null && !shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return returnCode == OK;
	}

	/**
	 * Prompts the user for the install location, and whether the install should
	 * be shared or standalone.
	 */
	public void promptForLocations(InstallDescription description) {
		taskLabel.setText(NLS.bind("Select where you want {0} to be installed", description.getProductName()));
		okButton.setText("Install");
		okButton.setVisible(true);
		cancelButton.setText("Cancel");
		cancelButton.setEnabled(true);
		installSettingsGroup.setVisible(true);
		validateInstallSettings();
		Display display = getDisplay();
		returnCode = -1;
		while (returnCode == -1 && shell != null && !shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		if (returnCode == CANCEL)
			close();
		if (shell == null || shell.isDisposed())
			throw new OperationCanceledException();
		installSettingsGroup.setVisible(false);
		Path location = new Path(locationField.getText());
		description.setInstallLocation(location);
		if (standaloneButton.getSelection()) {
			description.setAgentLocation(location.append("p2")); //$NON-NLS-1$
			description.setBundleLocation(location);
		} else {
			String home = System.getProperty("user.home"); //$NON-NLS-1$
			description.setAgentLocation(new Path(home).append(".p2/")); //$NON-NLS-1$
			//setting bundle location to null will cause it to use default bundle location under agent location
			description.setBundleLocation(null);
		}
		okButton.setVisible(false);
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
		waitingForClose = false;
		taskLabel.setText("Installing...");
		cancelButton.setText("Cancel");
		cancelButton.setVisible(true);
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

	/**
	 * Validates that the user has correctly entered all required install settings.
	 */
	void validateInstallSettings() {
		boolean enabled = standaloneButton.getSelection() || sharedButton.getSelection();
		enabled &= Path.ROOT.isValidPath(locationField.getText());
		if (enabled) {
			//make sure the install location is an absolute path
			IPath location = new Path(locationField.getText());
			enabled &= location.isAbsolute();
		}
		okButton.setEnabled(enabled);

		if (standaloneButton.getSelection())
			installKindExplanation.setText(EXPLAIN_STANDALONE);
		else
			installKindExplanation.setText(EXPLAIN_SHARED);
	}
}