/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *     Ericsson AB (Hamdan Msheik) - Bug 398833
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler.migration;

import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.dialogs.ISelectableIUsPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.InstallWizard;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.AutomaticUpdatePlugin;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.AutomaticUpdateScheduler;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class ImportFromInstallationWizard_c extends InstallWizard implements IImportWizard {
	private IProfile toImportFrom;

	public ImportFromInstallationWizard_c() {
		this(ProvisioningUI.getDefaultUI(), null, null, null);
	}

	public ImportFromInstallationWizard_c(IProfile toImportFrom) {
		this(ProvisioningUI.getDefaultUI(), null, null, null);
		this.toImportFrom = toImportFrom;
	}

	public ImportFromInstallationWizard_c(ProvisioningUI ui, InstallOperation operation, Collection<IInstallableUnit> initialSelections, LoadMetadataRepositoryJob preloadJob) {
		super(ui, operation, initialSelections, preloadJob);
		IDialogSettings workbenchSettings = ProvUIActivator.getDefault().getDialogSettings();
		String sectionName = "ImportFromInstallationWizard"; //$NON-NLS-1$
		IDialogSettings section = workbenchSettings.getSection(sectionName);
		if (section == null) {
			section = workbenchSettings.addNewSection(sectionName);
		}
		setDialogSettings(section);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(ProvUIMessages.ImportWizard_WINDOWTITLE);
		setDefaultPageImageDescriptor(ImageDescriptor.createFromURL(Platform.getBundle(ProvUIActivator.PLUGIN_ID).getEntry("icons/install_wiz.gif"))); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
	}

	@Override
	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		if (toImportFrom != null)
			return new ImportFromInstallationPage_c(ui, this, toImportFrom);
		return new ImportFromInstallationPage_c(ui, this);
	}

	@Override
	protected ProvisioningContext getProvisioningContext() {
		return ((ImportFromInstallationPage_c) mainPage).getProvisioningContext();
	}

	@Override
	public boolean performFinish() {
		cleanupProfileRegistry();
		rememberShownMigration(toImportFrom.getTimestamp());
		return super.performFinish();
	}

	//Remember the timestamp that we migrated from.
	private void rememberShownMigration(long timestamp) {
		AutomaticUpdatePlugin.getDefault().getPreferenceStore().setValue(AutomaticUpdateScheduler.MIGRATION_DIALOG_SHOWN, timestamp);
		try {
			Preferences prefs = ConfigurationScope.INSTANCE.getNode("org.eclipse.equinox.p2.ui"); //$NON-NLS-1$
			prefs.putBoolean(AbstractPage_c.REMIND_ME_LATER, true);
			prefs.flush();
		} catch (BackingStoreException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, AutomaticUpdatePlugin.PLUGIN_ID, 0, ProvUIMessages.ERROR_SAVING_PREFERENCES, e), StatusManager.LOG);
		}
	}

	//Purge the profile registry from all the entries that are no longer relevant
	//We keep the base we import from on purpose to help with debugging
	private void cleanupProfileRegistry() {
		IProfileRegistry registry = (IProfileRegistry) ProvisioningUI.getDefaultUI().getSession().getProvisioningAgent().getService(IProfileRegistry.SERVICE_NAME);
		long[] history = registry.listProfileTimestamps(toImportFrom.getProfileId());
		for (int i = 0; i < history.length; i++) {
			if (history[i] < toImportFrom.getTimestamp())
				try {
					registry.removeProfile(toImportFrom.getProfileId(), history[i]);
				} catch (ProvisionException e) {
					//Can't happen
				}
		}
	}

	public boolean performCancel() {

		Preferences prefs = ConfigurationScope.INSTANCE.getNode("org.eclipse.equinox.p2.ui"); //$NON-NLS-1$
		boolean result = false;
		MessageDialog dialog = new MessageDialog(getShell(), "Migration dialaog", null, ProvUIMessages.ImportFromInstallationPage_CONFIRMATION_DIALOG, MessageDialog.QUESTION, new String[] {"Yes", "Yes, but remind me later", "No"}, 2);

		int answer = dialog.open();
		try {
			switch (answer) {
				case -1 : // if the user closes the dialog without clicking any button.
					break;
				case 0 :
					result = true;
					prefs.putBoolean(AbstractPage_c.REMIND_ME_LATER, false);
					prefs.flush();
					break;
				case 1 :
					result = true;
					prefs.putBoolean(AbstractPage_c.REMIND_ME_LATER, true);
					prefs.flush();
					break;
				case 2 :
					result = false;
					break;
			}
		} catch (BackingStoreException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, AutomaticUpdatePlugin.PLUGIN_ID, 0, ProvUIMessages.ERROR_SAVING_PREFERENCES, e), StatusManager.LOG);
		}
		return result;

	}

}
