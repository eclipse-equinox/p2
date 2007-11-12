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
package org.eclipse.equinox.internal.p2.ui.sdk.prefs;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.equinox.internal.p2.ui.sdk.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class AutomaticUpdatesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Button enabledCheck;
	private Button onStartupRadio;
	private Button onScheduleRadio;
	private Combo dayCombo;
	private Label atLabel;
	private Combo hourCombo;
	private Button searchOnlyRadio;
	private Button searchAndDownloadRadio;
	private Group updateScheduleGroup;
	private Group downloadGroup;

	public void init(IWorkbench workbench) {
		// nothing to init
	}

	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);

		enabledCheck = new Button(container, SWT.CHECK);
		enabledCheck.setText(ProvSDKMessages.AutomaticUpdatesPreferencePage_findUpdates);

		createSpacer(container, 1);

		updateScheduleGroup = new Group(container, SWT.NONE);
		updateScheduleGroup.setText(ProvSDKMessages.AutomaticUpdatesPreferencePage_UpdateSchedule);
		layout = new GridLayout();
		layout.numColumns = 3;
		updateScheduleGroup.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		updateScheduleGroup.setLayoutData(gd);

		onStartupRadio = new Button(updateScheduleGroup, SWT.RADIO);
		onStartupRadio.setText(ProvSDKMessages.AutomaticUpdatesPreferencePage_findOnStart);
		gd = new GridData();
		gd.horizontalSpan = 3;
		onStartupRadio.setLayoutData(gd);
		onStartupRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		onScheduleRadio = new Button(updateScheduleGroup, SWT.RADIO);
		onScheduleRadio.setText(ProvSDKMessages.AutomaticUpdatesPreferencePage_findOnSchedule);
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
		atLabel.setText(ProvSDKMessages.AutomaticUpdatesPreferencePage_at);

		hourCombo = new Combo(updateScheduleGroup, SWT.READ_ONLY);
		hourCombo.setItems(AutomaticUpdateScheduler.HOURS);
		gd = new GridData();
		gd.widthHint = 100;
		hourCombo.setLayoutData(gd);

		createSpacer(container, 1);

		downloadGroup = new Group(container, SWT.NONE);
		downloadGroup.setText(ProvSDKMessages.AutomaticUpdatesPreferencePage_downloadOptions);
		layout = new GridLayout();
		layout.numColumns = 3;
		downloadGroup.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		downloadGroup.setLayoutData(gd);

		searchOnlyRadio = new Button(downloadGroup, SWT.RADIO);
		searchOnlyRadio.setText(ProvSDKMessages.AutomaticUpdatesPreferencePage_searchAndNotify);
		gd = new GridData();
		gd.horizontalSpan = 3;
		searchOnlyRadio.setLayoutData(gd);
		searchOnlyRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		searchAndDownloadRadio = new Button(downloadGroup, SWT.RADIO);
		searchAndDownloadRadio.setText(ProvSDKMessages.AutomaticUpdatesPreferencePage_downloadAndNotify);
		gd = new GridData();
		gd.horizontalSpan = 3;
		searchAndDownloadRadio.setLayoutData(gd);
		searchAndDownloadRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

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
		Preferences pref = ProvSDKUIActivator.getDefault().getPluginPreferences();
		enabledCheck.setSelection(pref.getBoolean(PreferenceConstants.P_ENABLED));
		setSchedule(pref.getString(PreferenceConstants.P_SCHEDULE));

		dayCombo.setText(AutomaticUpdateScheduler.DAYS[getDay(pref, false)]);
		hourCombo.setText(AutomaticUpdateScheduler.HOURS[getHour(pref, false)]);

		searchOnlyRadio.setSelection(!pref.getBoolean(PreferenceConstants.P_DOWNLOAD));
		searchAndDownloadRadio.setSelection(pref.getBoolean(PreferenceConstants.P_DOWNLOAD));

		pageChanged();
	}

	private void setSchedule(String value) {
		if (value.equals(PreferenceConstants.VALUE_ON_STARTUP))
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
	}

	protected void performDefaults() {
		super.performDefaults();
		Preferences pref = ProvSDKUIActivator.getDefault().getPluginPreferences();
		enabledCheck.setSelection(pref.getDefaultBoolean(PreferenceConstants.P_ENABLED));

		setSchedule(pref.getDefaultString(PreferenceConstants.P_SCHEDULE));
		onScheduleRadio.setSelection(pref.getDefaultBoolean(PreferenceConstants.P_SCHEDULE));

		dayCombo.setText(AutomaticUpdateScheduler.DAYS[getDay(pref, true)]);
		hourCombo.setText(AutomaticUpdateScheduler.HOURS[getHour(pref, true)]);

		searchOnlyRadio.setSelection(!pref.getDefaultBoolean(PreferenceConstants.P_DOWNLOAD));
		searchAndDownloadRadio.setSelection(pref.getDefaultBoolean(PreferenceConstants.P_DOWNLOAD));
		pageChanged();
	}

	/** 
	 * Method declared on IPreferencePage.
	 * Subclasses should override
	 */
	public boolean performOk() {
		Preferences pref = ProvSDKUIActivator.getDefault().getPluginPreferences();
		pref.setValue(PreferenceConstants.P_ENABLED, enabledCheck.getSelection());
		if (onStartupRadio.getSelection())
			pref.setValue(PreferenceConstants.P_SCHEDULE, PreferenceConstants.VALUE_ON_STARTUP);
		else
			pref.setValue(PreferenceConstants.P_SCHEDULE, PreferenceConstants.VALUE_ON_SCHEDULE);

		pref.setValue(AutomaticUpdateScheduler.P_DAY, dayCombo.getText());
		pref.setValue(AutomaticUpdateScheduler.P_HOUR, hourCombo.getText());

		pref.setValue(PreferenceConstants.P_DOWNLOAD, searchAndDownloadRadio.getSelection());

		ProvSDKUIActivator.getDefault().savePluginPreferences();

		ProvSDKUIActivator.getScheduler().rescheduleUpdate();
		return true;
	}

	private int getDay(Preferences pref, boolean useDefault) {
		String day = useDefault ? pref.getDefaultString(AutomaticUpdateScheduler.P_DAY) : pref.getString(AutomaticUpdateScheduler.P_DAY);
		for (int i = 0; i < AutomaticUpdateScheduler.DAYS.length; i++)
			if (AutomaticUpdateScheduler.DAYS[i].equals(day))
				return i;
		return 0;
	}

	private int getHour(Preferences pref, boolean useDefault) {
		String hour = useDefault ? pref.getDefaultString(AutomaticUpdateScheduler.P_HOUR) : pref.getString(AutomaticUpdateScheduler.P_HOUR);
		for (int i = 0; i < AutomaticUpdateScheduler.HOURS.length; i++)
			if (AutomaticUpdateScheduler.HOURS[i].equals(hour))
				return i;
		return 0;
	}
}
