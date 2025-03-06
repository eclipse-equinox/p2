/*******************************************************************************
 * Copyright (c) 2011, 2017 WindRiver Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *     Ericsson AB - Ongoing development
 *     Ericsson AB (Pascal Rapicault)
 *     Ericsson AB (Hamdan Msheik)
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler.migration;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.dialogs.ISelectableIUsPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.InstallWizard;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.AutomaticUpdatePlugin;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class MigrationWizard extends InstallWizard implements IImportWizard {

	private IProfile toImportFrom;
	Collection<IInstallableUnit> unitsToMigrate;
	private URI[] reposToMigrate;
	private final List<URI> addedRepos = new ArrayList<>();
	private boolean firstTime = false;

	public MigrationWizard() {
		this(ProvisioningUI.getDefaultUI(), null, null, null);
	}

	public MigrationWizard(IProfile toImportFrom, Collection<IInstallableUnit> unitsToMigrate, URI[] reposToMigrate,
			boolean firstTime) {
		this(ProvisioningUI.getDefaultUI(), null, null, null);
		this.toImportFrom = toImportFrom;
		this.unitsToMigrate = unitsToMigrate;
		this.reposToMigrate = reposToMigrate;
		this.firstTime = firstTime;
		addRepos();
	}

	public MigrationWizard(ProvisioningUI ui, InstallOperation operation,
			Collection<IInstallableUnit> initialSelections, LoadMetadataRepositoryJob preloadJob) {
		super(ui, operation, initialSelections, preloadJob);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(firstTime ? ProvUIMessages.MigrationWizard_WINDOWTITLE_FIRSTRUN
				: ProvUIMessages.MigrationWizard_WINDOWTITLE);
		setDefaultPageImageDescriptor(ImageDescriptor
				.createFromURL(Platform.getBundle(ProvUIActivator.PLUGIN_ID).getEntry("icons/install_wiz.png"))); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
	}

	@Override
	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		if (unitsToMigrate != null) {
			return new MigrationPage(ui, this, toImportFrom, unitsToMigrate, firstTime);
		}
		return new MigrationPage(ui, this, firstTime);
	}

	@Override
	protected ProvisioningContext getProvisioningContext() {
		return ((MigrationPage) mainPage).getProvisioningContext();
	}

	@Override
	public boolean performFinish() {
		cleanupProfileRegistry();
		boolean finished = super.performFinish();
		if (finished) {
			rememberMigrationCompleted();
		}
		return finished;
	}

	private void addRepos() {
		IProvisioningAgent agent = ServiceHelper.getService(AutomaticUpdatePlugin.getContext(),
				IProvisioningAgent.class);
		IMetadataRepositoryManager metaManager = agent.getService(IMetadataRepositoryManager.class);
		IArtifactRepositoryManager artifactManager = agent.getService(IArtifactRepositoryManager.class);
		List<URI> currentMetaRepos = Arrays
				.asList(metaManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL));

		if (reposToMigrate != null && metaManager != null && artifactManager != null) {
			for (URI repoToMigrate : reposToMigrate) {
				if (!currentMetaRepos.contains(repoToMigrate)) {
					metaManager.addRepository(repoToMigrate);
					artifactManager.addRepository(repoToMigrate);
					addedRepos.add(repoToMigrate);
				}
			}
		}
	}

	private void removeRepos() {
		IProvisioningAgent agent = ServiceHelper.getService(AutomaticUpdatePlugin.getContext(),
				IProvisioningAgent.class);
		IMetadataRepositoryManager metaManager = agent.getService(IMetadataRepositoryManager.class);
		IArtifactRepositoryManager artifactManager = agent.getService(IArtifactRepositoryManager.class);

		if (metaManager != null && artifactManager != null) {
			for (int i = 0; i < addedRepos.size(); i++) {
				metaManager.removeRepository(reposToMigrate[i]);
				artifactManager.removeRepository(reposToMigrate[i]);
			}
		}
	}

	// Remember that we completed the migration
	private void rememberMigrationCompleted() {
		new MigrationSupport().rememberMigrationCompleted();
	}

	// Purge the profile registry from all the entries that are no longer relevant
	// We keep the base we import from on purpose to help with debugging
	private void cleanupProfileRegistry() {
		IProfileRegistry registry = ProvisioningUI.getDefaultUI().getSession().getProvisioningAgent()
				.getService(IProfileRegistry.class);
		for (long timestamp : registry.listProfileTimestamps(toImportFrom.getProfileId())) {
			if (timestamp < toImportFrom.getTimestamp()) {
				try {
					registry.removeProfile(toImportFrom.getProfileId(), timestamp);
				} catch (ProvisionException e) {
					// Can't happen
				}
			}
		}
	}

	@Override
	public boolean performCancel() {
		String[] buttons = new String[] { IDialogConstants.YES_LABEL, ProvUIMessages.MigrationPage_LATER_BUTTON,
				IDialogConstants.NO_LABEL };
		MessageDialog dialog = new MessageDialog(getShell(), ProvUIMessages.MigrationPage_CONFIRMATION_TITLE, null,
				ProvUIMessages.MigrationPage_CONFIRMATION_DIALOG, MessageDialog.QUESTION, buttons, 2);

		return rememberCancellationDecision(dialog.open());
	}

	// Method public for test
	public boolean rememberCancellationDecision(int answer) {
		boolean result = false;
		switch (answer) {
		case -1: // if the user closes the dialog without clicking any button.
			break;
		case 0:
			result = true;
			removeRepos();
			rememberMigrationCompleted();
			break;
		case 1:
			result = true;
			removeRepos();
			break;
		case 2:
			result = false;
			break;
		}
		return result;
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage toReturn = page;

		try {
			toReturn = super.getNextPage(page);
		} catch (OperationCanceledException oce) {
			// swallow and stay on the same page
		}

		return toReturn;
	}

}
