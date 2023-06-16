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

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.installer.*;
import org.eclipse.equinox.internal.provisional.p2.installer.IInstallOperation;
import org.eclipse.equinox.internal.provisional.p2.installer.InstallDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
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
		String subTaskName = ""; //$NON-NLS-1$
		double totalWork, usedWork;

		@Override
		public void beginTask(final String name, final int work) {
			totalWork = work;
			running = true;
			update();
		}

		@Override
		public void done() {
			running = false;
			usedWork = totalWork;
			update();
		}

		@Override
		public void internalWorked(double work) {
			usedWork = Math.min(usedWork + work, totalWork);
			update();
		}

		@Override
		public boolean isCanceled() {
			return returnCode == CANCEL;
		}

		@Override
		public void setCanceled(boolean value) {
			returnCode = CANCEL;
		}

		@Override
		public void setTaskName(String name) {
			subTaskName = name == null ? "" : name; //$NON-NLS-1$
			update();
		}

		@Override
		public void subTask(String name) {
			subTaskName = name == null ? "" : name; //$NON-NLS-1$
			update();
		}

		void update() {
			Display display = getDisplay();
			if (display == null)
				return;
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					Shell theShell = getShell();
					if (theShell == null || theShell.isDisposed())
						return;
					progressSubTask.setText(shorten(subTaskName));
					if (progressBar.isDisposed())
						return;
					progressBar.setVisible(running);
					progressBar.setMaximum(1000);
					progressBar.setMinimum(0);
					int value = (int) (usedWork / totalWork * 1000);
					if (progressBar.getSelection() < value)
						progressBar.setSelection(value);
				}

				private String shorten(String text) {
					if (text.length() <= 64)
						return text;
					int len = text.length();
					return text.substring(0, 30) + "..." + text.substring(len - 30, len); //$NON-NLS-1$
				}
			});
		}

		@Override
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
			String msg = Messages.Dialog_InternalError;
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

	private static final int BUTTON_WIDTH = 100;
	private static final int CANCEL = 1;
	private static final int OK = 0;

	private Button cancelButton;
	private Composite contents;
	private Button okButton;

	ProgressBar progressBar;
	Label progressSubTask;
	Label progressTask;

	int returnCode = -1;

	private Button settingsBrowse;
	private Label settingsExplain;
	private Composite settingsGroup;
	private Text settingsLocation;
	private Label settingsLocationLabel;
	private Button settingsShared;
	private Button settingsStandalone;

	private Shell shell;

	private boolean waitingForClose = false;
	private Button proxySettingsButton;
	private Button manualProxyTypeCheckBox;

	/**
	 * Creates and opens a progress monitor dialog.
	 */
	public InstallDialog() {
		createShell();
		progressTask = new Label(contents, SWT.WRAP | SWT.LEFT);
		progressTask.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createInstallSettingsControls();
		createProgressControls();
		createButtonBar();

		shell.pack();
		shell.layout();
		shell.open();
	}

	protected void browsePressed() {
		DirectoryDialog dirDialog = new DirectoryDialog(shell);
		dirDialog.setMessage(Messages.Dialog_SelectLocation);
		String location = dirDialog.open();
		if (location == null)
			location = ""; //$NON-NLS-1$
		settingsLocation.setText(location);
		validateInstallSettings();
	}

	protected void buttonPressed(int code) {
		returnCode = code;
		if (waitingForClose)
			close();
		//grey out the cancel button to indicate the request was heard
		if (code == CANCEL && !cancelButton.isDisposed())
			cancelButton.setEnabled(false);
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
		data = new GridData(BUTTON_WIDTH, SWT.DEFAULT);
		okButton.setLayoutData(data);
		okButton.setText(Messages.Dialog_InstallButton);
		okButton.setEnabled(false);
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				buttonPressed(OK);
			}
		});

		cancelButton = new Button(buttonBar, SWT.PUSH);
		data = new GridData(BUTTON_WIDTH, SWT.DEFAULT);
		cancelButton.setLayoutData(data);
		cancelButton.setText(Messages.Dialog_CancelButton);
		cancelButton.setEnabled(false);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				buttonPressed(CANCEL);
			}
		});
	}

	/**
	 * Creates the controls to prompt for the agent and install locations.
	 */
	private void createInstallSettingsControls() {
		settingsGroup = new Composite(contents, SWT.NONE);
		GridLayout layout = new GridLayout();
		settingsGroup.setLayout(layout);
		settingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

		Listener validateListener = event -> validateInstallSettings();

		//The group asking for the product install directory
		Group installLocationGroup = new Group(settingsGroup, SWT.NONE);
		installLocationGroup.setLayout(new GridLayout());
		installLocationGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		installLocationGroup.setText(Messages.Dialog_LocationField);
		settingsLocationLabel = new Label(installLocationGroup, SWT.NONE);
		settingsLocationLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		settingsLocationLabel.setText(Messages.Dialog_LocationLabel);

		//The sub-group with text entry field and browse button
		Composite locationFieldGroup = new Composite(installLocationGroup, SWT.NONE);
		locationFieldGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		locationFieldGroup.setLayout(layout);
		settingsLocation = new Text(locationFieldGroup, SWT.SINGLE | SWT.BORDER);
		settingsLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		settingsLocation.addListener(SWT.Modify, validateListener);
		settingsLocation.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent event) {
				if (event.character == SWT.CR || event.character == SWT.KEYPAD_CR)
					buttonPressed(OK);
			}
		});
		settingsBrowse = new Button(locationFieldGroup, SWT.PUSH);
		settingsBrowse.setLayoutData(new GridData(BUTTON_WIDTH, SWT.DEFAULT));
		settingsBrowse.setText(Messages.Dialog_BrowseButton);
		settingsBrowse.addListener(SWT.Selection, event -> browsePressed());

		//Create the radio button group asking for the kind of install (shared vs. standalone)
		Group installKindGroup = new Group(settingsGroup, SWT.NONE);
		installKindGroup.setText(Messages.Dialog_LayoutGroup);
		installKindGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		installKindGroup.setLayout(new GridLayout());
		settingsStandalone = new Button(installKindGroup, SWT.RADIO);
		settingsStandalone.setText(Messages.Dialog_StandaloneButton);
		settingsStandalone.addListener(SWT.Selection, validateListener);
		settingsStandalone.setSelection(true);
		settingsShared = new Button(installKindGroup, SWT.RADIO);
		settingsShared.setText(Messages.Dialog_SharedButton);
		settingsShared.addListener(SWT.Selection, validateListener);
		settingsExplain = new Label(installKindGroup, SWT.WRAP);
		GridData data = new GridData(SWT.DEFAULT, 40);
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.FILL;
		settingsExplain.setLayoutData(data);
		settingsExplain.setText(Messages.Dialog_ExplainStandalone);

		//The group asking for the product proxy configuration
		Group proxySettingsGroup = new Group(settingsGroup, SWT.NONE);
		proxySettingsGroup.setLayout(new GridLayout());
		proxySettingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		proxySettingsGroup.setText(Messages.Dialog_ProxiesGroup);

		//The sub-group with check box, label entry field and settings button
		Composite proxySettingsFieldGroup = new Composite(proxySettingsGroup, SWT.NONE);
		proxySettingsFieldGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		proxySettingsFieldGroup.setLayout(layout);

		manualProxyTypeCheckBox = new Button(proxySettingsFieldGroup, SWT.CHECK);
		manualProxyTypeCheckBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		manualProxyTypeCheckBox.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			IProxyService proxies = (IProxyService) InstallApplication.getService(InstallerActivator.getDefault().getContext(), IProxyService.class.getName());
			if (proxies != null) {
				//When the manual check box is selected the system properties should be disabled.
				//This cases the net component to switch to manual proxy provider.
				proxies.setSystemProxiesEnabled(!manualProxyTypeCheckBox.getSelection());
				if (proxySettingsButton != null) {
					proxySettingsButton.setEnabled(manualProxyTypeCheckBox.getSelection());
				}
			} else {
				openMessage(Messages.ProxiesDialog_ServiceNotAvailableMessage, SWT.ICON_ERROR | SWT.OK);
			}
		}));
		manualProxyTypeCheckBox.setText(Messages.Dialog_ManualProxyCheckBox);
		proxySettingsButton = new Button(proxySettingsFieldGroup, SWT.PUSH);
		proxySettingsButton.setLayoutData(new GridData(BUTTON_WIDTH, SWT.DEFAULT));
		proxySettingsButton.setText(Messages.Dialog_SettingsButton);
		proxySettingsButton.setEnabled(manualProxyTypeCheckBox.getSelection());
		proxySettingsButton.addListener(SWT.Selection, event -> {
			IProxyService proxies = (IProxyService) InstallApplication.getService(InstallerActivator.getDefault().getContext(), IProxyService.class.getName());
			if (proxies != null) {
				ProxiesDialog proxiesDialog = new ProxiesDialog(proxies);
				proxiesDialog.open();
			} else {
				openMessage(Messages.ProxiesDialog_ServiceNotAvailableMessage, SWT.ICON_ERROR | SWT.OK);
			}
		});

		//make the entire group invisible until we actually need to prompt for locations
		settingsGroup.setVisible(false);
	}

	private void createProgressControls() {
		progressBar = new ProgressBar(contents, SWT.HORIZONTAL | SWT.SMOOTH);
		progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		progressBar.setVisible(false);
		progressSubTask = new Label(contents, SWT.WRAP | SWT.LEFT);
		progressSubTask.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private void createShell() {
		shell = new Shell(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.MIN | SWT.RESIZE);
		shell.setBounds(300, 200, 600, 400);
		shell.setText(Messages.Dialog_ShellTitle);
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
		progressTask.setText(message);
		progressSubTask.setText(""); //$NON-NLS-1$
		progressBar.setVisible(false);
		okButton.setVisible(false);
		cancelButton.setText(Messages.Dialog_CloseButton);
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
		progressTask.setText(NLS.bind(Messages.Dialog_PromptStart, description.getProductName()));
		progressSubTask.setText(""); //$NON-NLS-1$
		progressBar.setVisible(false);
		okButton.setText(Messages.Dialog_LaunchButton);
		okButton.setVisible(true);
		cancelButton.setText(Messages.Dialog_CloseButton);
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
		progressTask.setText(NLS.bind(Messages.Dialog_LocationPrompt, description.getProductName()));
		okButton.setText(Messages.Dialog_InstallButton);
		okButton.setVisible(true);
		cancelButton.setText(Messages.Dialog_CancelButton);
		cancelButton.setEnabled(true);
		settingsGroup.setVisible(true);
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
		setInstallSettingsEnablement(false);
		IPath location = IPath.fromOSString(settingsLocation.getText());
		description.setInstallLocation(location);
		if (settingsStandalone.getSelection()) {
			//force everything to be co-located regardless of what values were set in the install description
			description.setAgentLocation(location.append("p2")); //$NON-NLS-1$
			description.setBundleLocation(location);
		} else {
			if (description.getAgentLocation() == null)
				description.setAgentLocation(IPath.fromOSString(System.getProperty("user.home")).append(".p2/")); //$NON-NLS-1$ //$NON-NLS-2$
			//use bundle pool location specified in install description
			//by default this will be null, causing the bundle pool to be nested in the agent location
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
			@Override
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
						display.syncExec(() -> {
							//do nothing
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
		progressTask.setText(Messages.Dialog_InstalllingProgress);
		cancelButton.setText(Messages.Dialog_CancelButton);
		thread.start();
		Display display = getDisplay();
		while (!result.isDone()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return result.getStatus();
	}

	private void setInstallSettingsEnablement(boolean value) {
		settingsLocation.setEnabled(value);
		settingsShared.setEnabled(value);
		settingsStandalone.setEnabled(value);
		settingsGroup.setEnabled(value);
		settingsExplain.setEnabled(value);
		settingsBrowse.setEnabled(value);
		settingsLocationLabel.setEnabled(value);
	}

	public void setMessage(String message) {
		if (progressTask != null && !progressTask.isDisposed())
			progressTask.setText(message);
	}

	/**
	 * Validates that the user has correctly entered all required install settings.
	 */
	void validateInstallSettings() {
		boolean enabled = settingsStandalone.getSelection() || settingsShared.getSelection();
		enabled &= IPath.ROOT.isValidPath(settingsLocation.getText());
		if (enabled) {
			//make sure the install location is an absolute path
			IPath location = IPath.fromOSString(settingsLocation.getText());
			enabled &= location.isAbsolute();
		}
		okButton.setEnabled(enabled);

		if (settingsStandalone.getSelection())
			settingsExplain.setText(Messages.Dialog_ExplainStandalone);
		else
			settingsExplain.setText(Messages.Dialog_ExplainShared);
	}

	private void openMessage(String msg, int style) {
		MessageBox messageBox = new MessageBox(Display.getDefault().getActiveShell(), style);
		messageBox.setMessage(msg);
		messageBox.open();
	}
}