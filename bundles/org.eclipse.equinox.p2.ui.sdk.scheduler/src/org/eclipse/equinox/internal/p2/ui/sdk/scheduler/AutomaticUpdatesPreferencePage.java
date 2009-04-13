/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import org.osgi.service.prefs.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

/**
 * Preference page for automated updates.
 * 
 * @since 3.4
 *
 */
public class AutomaticUpdatesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Button enabledCheck;
	private Button onStartupRadio, onScheduleRadio;
	private Combo dayCombo;
	private Label atLabel;
	private Combo hourCombo;
	private Button searchOnlyRadio, searchAndDownloadRadio;
	private Button remindOnceRadio, remindScheduleRadio;
	private Combo remindElapseCombo;
	private Group updateScheduleGroup, downloadGroup, remindGroup;

	public void init(IWorkbench workbench) {
		// nothing to init
	}

	protected Control createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IAutomaticUpdaterHelpContextIds.AUTOMATIC_UPDATES_PREFERENCE_PAGE);

		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);

		enabledCheck = new Button(container, SWT.CHECK);
		enabledCheck.setText(AutomaticUpdateMessages.AutomaticUpdatesPreferencePage_findUpdates);

		createSpacer(container, 1);

		updateScheduleGroup = new Group(container, SWT.NONE);
		updateScheduleGroup.setText(AutomaticUpdateMessages.AutomaticUpdatesPreferencePage_UpdateSchedule);
		layout = new GridLayout();
		layout.numColumns = 3;
		updateScheduleGroup.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		updateScheduleGroup.setLayoutData(gd);

		onStartupRadio = new Button(updateScheduleGroup, SWT.RADIO);
		onStartupRadio.setText(AutomaticUpdateMessages.AutomaticUpdatesPreferencePage_findOnStart);
		gd = new GridData();
		gd.horizontalSpan = 3;
		onStartupRadio.setLayoutData(gd);
		onStartupRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		onScheduleRadio = new Button(updateScheduleGroup, SWT.RADIO);
		onScheduleRadio.setText(AutomaticUpdateMessages.AutomaticUpdatesPreferencePage_findOnSchedule);
		gd = new GridData();
		gd.horizontalSpan = 3;
		onScheduleRadio.setLayoutData(gd);
		onScheduleRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		dayCombo = new Combo(updateScheduleGroup, SWT.READ_ONLY);
		dayCombo.setItems(AutomaticUpdateScheduler.DAYS);
		gd = new GridData();
		gd.widthHint = 200;
		gd.horizontalIndent = 30;
		dayCombo.setLayoutData(gd);

		atLabel = new Label(updateScheduleGroup, SWT.NULL);
		atLabel.setText(AutomaticUpdateMessages.AutomaticUpdatesPreferencePage_at);

		hourCombo = new Combo(updateScheduleGroup, SWT.READ_ONLY);
		hourCombo.setItems(AutomaticUpdateScheduler.HOURS);
		gd = new GridData();
		hourCombo.setLayoutData(gd);

		createSpacer(container, 1);

		downloadGroup = new Group(container, SWT.NONE);
		downloadGroup.setText(AutomaticUpdateMessages.AutomaticUpdatesPreferencePage_downloadOptions);
		layout = new GridLayout();
		layout.numColumns = 3;
		downloadGroup.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		downloadGroup.setLayoutData(gd);

		searchOnlyRadio = new Button(downloadGroup, SWT.RADIO);
		searchOnlyRadio.setText(AutomaticUpdateMessages.AutomaticUpdatesPreferencePage_searchAndNotify);
		gd = new GridData();
		gd.horizontalSpan = 3;
		searchOnlyRadio.setLayoutData(gd);
		searchOnlyRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		searchAndDownloadRadio = new Button(downloadGroup, SWT.RADIO);
		searchAndDownloadRadio.setText(AutomaticUpdateMessages.AutomaticUpdatesPreferencePage_downloadAndNotify);
		gd = new GridData();
		gd.horizontalSpan = 3;
		searchAndDownloadRadio.setLayoutData(gd);
		searchAndDownloadRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		createSpacer(container, 1);

		remindGroup = new Group(container, SWT.NONE);
		remindGroup.setText(AutomaticUpdateMessages.AutomaticUpdatesPreferencePage_RemindGroup);
		layout = new GridLayout();
		layout.numColumns = 3;
		remindGroup.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		remindGroup.setLayoutData(gd);

		remindOnceRadio = new Button(remindGroup, SWT.RADIO);
		remindOnceRadio.setText(AutomaticUpdateMessages.AutomaticUpdatesPreferencePage_RemindOnce);
		gd = new GridData();
		gd.horizontalSpan = 3;
		remindOnceRadio.setLayoutData(gd);
		remindOnceRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		remindScheduleRadio = new Button(remindGroup, SWT.RADIO);
		remindScheduleRadio.setText(AutomaticUpdateMessages.AutomaticUpdatesPreferencePage_RemindSchedule);
		gd = new GridData();
		gd.horizontalSpan = 3;
		remindScheduleRadio.setLayoutData(gd);
		remindScheduleRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		remindElapseCombo = new Combo(remindGroup, SWT.READ_ONLY);
		remindElapseCombo.setItems(AutomaticUpdatesPopup.ELAPSED);

		gd = new GridData();
		gd.widthHint = 200;
		gd.horizontalIndent = 30;
		remindElapseCombo.setLayoutData(gd);

		initialize();

		enabledCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		Dialog.applyDialogFont(container);
		return container;
	}

	protected void createSpacer(Composite composite, int columnSpan) {
		Label label = new Label(composite, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalSpan = columnSpan;
		label.setLayoutData(gd);
	}

	private void initialize() {
		Preferences pref = AutomaticUpdatePlugin.getPreferences();
		enabledCheck.setSelection(pref.getBoolean(PreferenceConstants.PREF_AUTO_UPDATE_ENABLED, false));
		setSchedule(pref.get(PreferenceConstants.PREF_AUTO_UPDATE_SCHEDULE, ""));
		
		dayCombo.setText(AutomaticUpdateScheduler.DAYS[getDay(pref)]);
		hourCombo.setText(AutomaticUpdateScheduler.HOURS[getHour(pref)]);
		
		remindScheduleRadio.setSelection(pref.getBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE, false));
		remindOnceRadio.setSelection(!pref.getBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE, false));
		remindElapseCombo.setText(pref.get(PreferenceConstants.PREF_REMIND_ELAPSED, ""));
		searchOnlyRadio.setSelection(!pref.getBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY, false));
		searchAndDownloadRadio.setSelection(pref.getBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY, false));
		
		pageChanged();
	}

	private void setSchedule(String value) {
		if (value.equals(PreferenceConstants.PREF_UPDATE_ON_STARTUP))
			onStartupRadio.setSelection(true);
		else
			onScheduleRadio.setSelection(true);
	}

	void pageChanged() {
		boolean master = enabledCheck.getSelection();
		updateScheduleGroup.setEnabled(master);
		onStartupRadio.setEnabled(master);
		onScheduleRadio.setEnabled(master);
		dayCombo.setEnabled(master && onScheduleRadio.getSelection());
		atLabel.setEnabled(master && onScheduleRadio.getSelection());
		hourCombo.setEnabled(master && onScheduleRadio.getSelection());
		downloadGroup.setEnabled(master);
		searchOnlyRadio.setEnabled(master);
		searchAndDownloadRadio.setEnabled(master);
		remindGroup.setEnabled(master);
		remindScheduleRadio.setEnabled(master);
		remindOnceRadio.setEnabled(master);
		remindElapseCombo.setEnabled(master && remindScheduleRadio.getSelection());
	}

	protected void performDefaults() {
		super.performDefaults();
		Preferences defaultPref = AutomaticUpdatePlugin.getDefaultPreferences();
		enabledCheck.setSelection(defaultPref.getBoolean(PreferenceConstants.PREF_AUTO_UPDATE_ENABLED, false));

		setSchedule(defaultPref.get(PreferenceConstants.PREF_AUTO_UPDATE_SCHEDULE, ""));
		onScheduleRadio.setSelection(defaultPref.getBoolean(PreferenceConstants.PREF_AUTO_UPDATE_SCHEDULE, false));

		dayCombo.setText(AutomaticUpdateScheduler.DAYS[getDay(defaultPref)]);
		hourCombo.setText(AutomaticUpdateScheduler.HOURS[getHour(defaultPref)]);

		remindOnceRadio.setSelection(!defaultPref.getBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE, false));
		remindScheduleRadio.setSelection(defaultPref.getBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE, false));
		remindElapseCombo.setText(defaultPref.get(PreferenceConstants.PREF_REMIND_ELAPSED, ""));

		searchOnlyRadio.setSelection(!defaultPref.getBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY, false));
		searchAndDownloadRadio.setSelection(defaultPref.getBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY, false));

		pageChanged();
	}

	/** 
	 * Method declared on IPreferencePage.
	 * Subclasses should override
	 */
	public boolean performOk() {
		Preferences pref = AutomaticUpdatePlugin.getPreferences();
		pref.putBoolean(PreferenceConstants.PREF_AUTO_UPDATE_ENABLED, enabledCheck.getSelection());
		if (onStartupRadio.getSelection())
			pref.put(PreferenceConstants.PREF_AUTO_UPDATE_SCHEDULE, PreferenceConstants.PREF_UPDATE_ON_STARTUP);
		else
			pref.put(PreferenceConstants.PREF_AUTO_UPDATE_SCHEDULE, PreferenceConstants.PREF_UPDATE_ON_SCHEDULE);
		
		if (remindScheduleRadio.getSelection()) {
			pref.putBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE, true);
			pref.put(PreferenceConstants.PREF_REMIND_ELAPSED, remindElapseCombo.getText());
		} else {
			pref.putBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE, false);
		}
		
		pref.put(AutomaticUpdateScheduler.P_DAY, dayCombo.getText());
		pref.put(AutomaticUpdateScheduler.P_HOUR, hourCombo.getText());
		
		pref.putBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY, searchAndDownloadRadio.getSelection());

		AutomaticUpdatePlugin.savePreferences();

		AutomaticUpdatePlugin.getDefault().getScheduler().rescheduleUpdate();
		return true;
	}

	private int getDay(Preferences pref) {
		String day = pref.get(AutomaticUpdateScheduler.P_DAY, "");
		for (int i = 0; i < AutomaticUpdateScheduler.DAYS.length; i++)
			if (AutomaticUpdateScheduler.DAYS[i].equals(day))
				return i;
		return 0;
	}

	private int getHour(Preferences pref) {
		String hour = pref.get(AutomaticUpdateScheduler.P_HOUR, "");
		for (int i = 0; i < AutomaticUpdateScheduler.HOURS.length; i++)
			if (AutomaticUpdateScheduler.HOURS[i].equals(hour))
				return i;
		return 0;
	}
}
