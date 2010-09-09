/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Johannes Michler <orgler@gmail.com> - Bug 321568 -  [ui] Preference for automatic-update-reminder doesn't work in multilanguage-environments
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * AutomaticUpdatesPopup is an async popup dialog for notifying
 * the user of updates.
 * 
 * @since 3.4
 */
public class AutomaticUpdatesPopup extends PopupDialog {
	public static final String[] ELAPSED_VALUES = {PreferenceConstants.PREF_REMIND_30Minutes, PreferenceConstants.PREF_REMIND_60Minutes, PreferenceConstants.PREF_REMIND_240Minutes};
	public static final String[] ELAPSED_LOCALIZED_STRINGS = {AutomaticUpdateMessages.AutomaticUpdateScheduler_30Minutes, AutomaticUpdateMessages.AutomaticUpdateScheduler_60Minutes, AutomaticUpdateMessages.AutomaticUpdateScheduler_240Minutes};
	private static final long MINUTE = 60 * 1000L;
	private static final String PREFS_HREF = "PREFS"; //$NON-NLS-1$
	private static final String DIALOG_SETTINGS_SECTION = "AutomaticUpdatesPopup"; //$NON-NLS-1$
	private static final int POPUP_OFFSET = 20;

	IPreferenceStore prefs;
	long remindDelay = -1L;
	IPropertyChangeListener prefListener;
	WorkbenchJob remindJob;
	boolean downloaded;
	Composite dialogArea;
	Link remindLink;
	MouseListener clickListener;

	public AutomaticUpdatesPopup(Shell parentShell, boolean alreadyDownloaded, IPreferenceStore prefs) {
		super(parentShell, PopupDialog.INFOPOPUPRESIZE_SHELLSTYLE | SWT.MODELESS, false, true, true, false, false, AutomaticUpdateMessages.AutomaticUpdatesPopup_UpdatesAvailableTitle, null);
		downloaded = alreadyDownloaded;
		this.prefs = prefs;
		remindDelay = computeRemindDelay();
		clickListener = new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				AutomaticUpdatePlugin.getDefault().getAutomaticUpdater().launchUpdate();
			}
		};
	}

	protected Control createDialogArea(Composite parent) {
		dialogArea = new Composite(parent, SWT.NONE);
		dialogArea.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		dialogArea.setLayout(layout);
		dialogArea.addMouseListener(clickListener);

		// The "click to update" label
		Label infoLabel = new Label(dialogArea, SWT.NONE);
		if (downloaded)
			infoLabel.setText(AutomaticUpdateMessages.AutomaticUpdatesPopup_ClickToReviewDownloaded);
		else
			infoLabel.setText(AutomaticUpdateMessages.AutomaticUpdatesPopup_ClickToReviewNotDownloaded);
		infoLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
		infoLabel.addMouseListener(clickListener);

		createRemindSection(dialogArea);

		return dialogArea;

	}

	private void createRemindSection(Composite parent) {
		remindLink = new Link(parent, SWT.MULTI | SWT.WRAP | SWT.RIGHT);
		updateRemindText();
		remindLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(), PreferenceConstants.PREF_PAGE_AUTO_UPDATES, null, null);
				dialog.open();

			}
		});
		remindLink.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	private void updateRemindText() {
		if (prefs.getBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE))
			remindLink.setText(NLS.bind(AutomaticUpdateMessages.AutomaticUpdatesPopup_RemindAndPrefLink, new String[] {getElapsedTimeString(prefs.getString(PreferenceConstants.PREF_REMIND_ELAPSED)), PREFS_HREF}));
		else
			remindLink.setText(AutomaticUpdateMessages.AutomaticUpdatesPopup_PrefLinkOnly);
		remindLink.getParent().layout(true);
	}

	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = AutomaticUpdatePlugin.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section == null) {
			section = settings.addNewSection(DIALOG_SETTINGS_SECTION);
		}
		return section;
	}

	public int open() {
		prefListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				handlePreferenceChange(event);
			}
		};
		prefs.addPropertyChangeListener(prefListener);
		return super.open();
	}

	public boolean close() {
		return close(true);
	}

	public boolean close(boolean remind) {
		if (remind && prefs.getBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE))
			scheduleRemindJob();
		else
			cancelRemindJob();
		if (prefListener != null) {
			prefs.removePropertyChangeListener(prefListener);
			prefListener = null;
		}
		return super.close();

	}

	void scheduleRemindJob() {
		// Cancel any pending remind job if there is one
		if (remindJob != null)
			remindJob.cancel();
		// If no updates have been found, there is nothing to remind
		if (remindDelay < 0)
			return;
		remindJob = new WorkbenchJob(AutomaticUpdateMessages.AutomaticUpdatesPopup_ReminderJobTitle) {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				open();
				return Status.OK_STATUS;
			}
		};
		remindJob.setSystem(true);
		remindJob.setPriority(Job.INTERACTIVE);
		remindJob.schedule(remindDelay);

	}

	/*
	 * Computes the number of milliseconds for the delay
	 * in reminding the user of updates
	 */
	long computeRemindDelay() {
		if (prefs.getBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE)) {
			String elapsed = prefs.getString(PreferenceConstants.PREF_REMIND_ELAPSED);
			for (int d = 0; d < ELAPSED_VALUES.length; d++)
				if (ELAPSED_VALUES[d].equals(elapsed))
					switch (d) {
						case 0 :
							// 30 minutes
							return 30 * MINUTE;
						case 1 :
							// 60 minutes
							return 60 * MINUTE;
						case 2 :
							// 240 minutes
							return 240 * MINUTE;
					}
		}
		return -1L;
	}

	void cancelRemindJob() {
		if (remindJob != null) {
			remindJob.cancel();
			remindJob = null;
		}
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(AutomaticUpdateMessages.AutomaticUpdatesPopup_UpdatesAvailableTitle);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#getInitialLocation(org.eclipse.swt.graphics.Point)
	 */
	protected Point getInitialLocation(Point initialSize) {
		Shell parent = getParentShell();
		Point parentSize, parentLocation;

		if (parent != null) {
			parentSize = parent.getSize();
			parentLocation = parent.getLocation();
		} else {
			Rectangle bounds = getShell().getDisplay().getBounds();
			parentSize = new Point(bounds.width, bounds.height);
			parentLocation = new Point(0, 0);
		}
		// We have to take parent location into account because SWT considers all
		// shell locations to be in display coordinates, even if the shell is parented.
		return new Point(parentSize.x - initialSize.x + parentLocation.x - POPUP_OFFSET, parentSize.y - initialSize.y + parentLocation.y - POPUP_OFFSET);
	}

	void handlePreferenceChange(PropertyChangeEvent event) {
		if (PreferenceConstants.PREF_REMIND_SCHEDULE.equals(event.getProperty())) {
			// Reminders turned on
			if (prefs.getBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE)) {
				if (remindLink == null)
					createRemindSection(dialogArea);
				else {
					updateRemindText();
					getShell().layout(true, true);
				}
				computeRemindDelay();
				scheduleRemindJob();
			} else { // reminders turned off
				if (remindLink != null) {
					updateRemindText();
					getShell().layout(true, true);
				}
				cancelRemindJob();
			}
		} else if (PreferenceConstants.PREF_REMIND_ELAPSED.equals(event.getProperty())) {
			// Reminding schedule changed
			computeRemindDelay();
			scheduleRemindJob();
		}
	}

	/*
	 * Overridden so that clicking in the title menu area closes the dialog.
	 * Also creates a close box menu in the title area.
	 */
	protected Control createTitleMenuArea(Composite parent) {
		Composite titleComposite = (Composite) super.createTitleMenuArea(parent);
		titleComposite.addMouseListener(clickListener);

		ToolBar toolBar = new ToolBar(titleComposite, SWT.FLAT);
		ToolItem closeButton = new ToolItem(toolBar, SWT.PUSH, 0);

		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(toolBar);
		closeButton.setImage(AutomaticUpdatePlugin.getDefault().getImageRegistry().get((AutomaticUpdatePlugin.IMG_TOOL_CLOSE)));
		closeButton.setHotImage(AutomaticUpdatePlugin.getDefault().getImageRegistry().get((AutomaticUpdatePlugin.IMG_TOOL_CLOSE_HOT)));
		closeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				close();
			}
		});
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=177183
		toolBar.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				close();
			}
		});
		return titleComposite;
	}

	/*
	 * Overridden to adjust the span of the title label.
	 * Reachy, reachy....
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.PopupDialog#createTitleControl(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createTitleControl(Composite parent) {
		Control control = super.createTitleControl(parent);
		Object data = control.getLayoutData();
		if (data instanceof GridData) {
			((GridData) data).horizontalSpan = 1;
		}
		return control;
	}

	public static String getElapsedTimeString(String elapsedTimeKey) {
		for (int i = 0; i < AutomaticUpdatesPopup.ELAPSED_VALUES.length; i++) {
			if (AutomaticUpdatesPopup.ELAPSED_VALUES[i].equals(elapsedTimeKey))
				return AutomaticUpdatesPopup.ELAPSED_LOCALIZED_STRINGS[i];
		}
		return AutomaticUpdatesPopup.ELAPSED_LOCALIZED_STRINGS[0];
	}
}
