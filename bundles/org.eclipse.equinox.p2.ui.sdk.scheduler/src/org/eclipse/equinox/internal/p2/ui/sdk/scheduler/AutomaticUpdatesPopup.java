/*******************************************************************************
 * Copyright (c) 2007, 2023 IBM Corporation and others.
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
 *     Johannes Michler <orgler@gmail.com> - Bug 321568 -  [ui] Preference for automatic-update-reminder doesn't work in multilanguage-environments
 *     Vasili Gulevich (Spirent Communications) - factor out UpdatesPopup
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Async popup dialog for notifying the user of eligible updates.
 * 
 * @since 3.4
 */
public class AutomaticUpdatesPopup extends UpdatesPopup {
	public static final String[] ELAPSED_VALUES = { PreferenceConstants.PREF_REMIND_30Minutes,
			PreferenceConstants.PREF_REMIND_60Minutes, PreferenceConstants.PREF_REMIND_240Minutes };
	public static final String[] ELAPSED_LOCALIZED_STRINGS = {
			AutomaticUpdateMessages.AutomaticUpdateScheduler_30Minutes,
			AutomaticUpdateMessages.AutomaticUpdateScheduler_60Minutes,
			AutomaticUpdateMessages.AutomaticUpdateScheduler_240Minutes };
	private static final long MINUTE = 60 * 1000L;
	private static final String PREFS_HREF = "PREFS"; //$NON-NLS-1$
	private static final String DIALOG_SETTINGS_SECTION = "AutomaticUpdatesPopup"; //$NON-NLS-1$

	IPreferenceStore prefs;
	long remindDelay = -1L;
	IPropertyChangeListener prefListener;
	WorkbenchJob remindJob;
	boolean downloaded;
	Link remindLink;

	public AutomaticUpdatesPopup(Shell parentShell, boolean alreadyDownloaded, IPreferenceStore prefs) {
		super(parentShell, alreadyDownloaded ? AutomaticUpdateMessages.AutomaticUpdatesPopup_ClickToReviewDownloaded
				: AutomaticUpdateMessages.AutomaticUpdatesPopup_ClickToReviewNotDownloaded);
		downloaded = alreadyDownloaded;
		this.prefs = prefs;
		remindDelay = computeRemindDelay();
	}

	@Override
	protected Composite createDialogArea(Composite parent) {
		Composite result = super.createDialogArea(parent);
		createRemindSection(result);
		return result;
	}

	private void createRemindSection(Composite parent) {
		remindLink = new Link(parent, SWT.MULTI | SWT.WRAP | SWT.RIGHT);
		updateRemindText();
		remindLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(),
					PreferenceConstants.PREF_PAGE_AUTO_UPDATES, null, null);
			dialog.open();

		}));
		remindLink.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	private void updateRemindText() {
		if (prefs.getBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE))
			remindLink.setText(NLS.bind(AutomaticUpdateMessages.AutomaticUpdatesPopup_RemindAndPrefLink, new String[] {
					getElapsedTimeString(prefs.getString(PreferenceConstants.PREF_REMIND_ELAPSED)), PREFS_HREF }));
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

	@Override
	public int open() {
		prefListener = this::handlePreferenceChange;
		prefs.addPropertyChangeListener(prefListener);
		return super.open();
	}

	@Override
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
			@Override
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
	 * Computes the number of milliseconds for the delay in reminding the user of
	 * updates
	 */
	long computeRemindDelay() {
		if (prefs.getBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE)) {
			String elapsed = prefs.getString(PreferenceConstants.PREF_REMIND_ELAPSED);
			for (int d = 0; d < ELAPSED_VALUES.length; d++)
				if (ELAPSED_VALUES[d].equals(elapsed))
					switch (d) {
					case 0:
						// 30 minutes
						return 30 * MINUTE;
					case 1:
						// 60 minutes
						return 60 * MINUTE;
					case 2:
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

	public static String getElapsedTimeString(String elapsedTimeKey) {
		for (int i = 0; i < AutomaticUpdatesPopup.ELAPSED_VALUES.length; i++) {
			if (AutomaticUpdatesPopup.ELAPSED_VALUES[i].equals(elapsedTimeKey))
				return AutomaticUpdatesPopup.ELAPSED_LOCALIZED_STRINGS[i];
		}
		return AutomaticUpdatesPopup.ELAPSED_LOCALIZED_STRINGS[0];
	}
}
