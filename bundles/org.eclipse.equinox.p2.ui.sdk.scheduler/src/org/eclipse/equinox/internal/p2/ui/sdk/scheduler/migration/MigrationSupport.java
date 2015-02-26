/*******************************************************************************
 * Copyright (c) 2013, 2015 Ericsson AB and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson AB - initial API and implementation
 *     Ericsson AB (Pascal Rapicault)
 *     Ericsson AB (Hamdan Msheik)
 *     Red Hat Inc. - Bug 460967 
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler.migration;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.metadata.query.UpdateQuery;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.*;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class MigrationSupport {
	private static final String ECLIPSE_P2_SKIP_MIGRATION_WIZARD = "eclipse.p2.skipMigrationWizard"; //$NON-NLS-1$
	private static final String ECLIPSE_P2_SKIP_MOVED_INSTALL_DETECTION = "eclipse.p2.skipMovedInstallDetection"; //$NON-NLS-1$

	//The return value indicates if the migration dialog has been shown or not. It does not indicate whether the migration has completed.
	public boolean performMigration(IProvisioningAgent agent, IProfileRegistry registry, IProfile currentProfile) {
		boolean skipWizard = Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty(ECLIPSE_P2_SKIP_MIGRATION_WIZARD));
		if (skipWizard)
			return false;

		IProfile previousProfile = null;
		URI[] reposToMigrate = null;
		if (!skipFirstTimeMigration() && !configurationSpecifiedManually() && isFirstTimeRunningThisSharedInstance(agent, registry, currentProfile)) {
			File searchRoot = getSearchLocation();
			if (searchRoot == null)
				return false;

			IProvisioningAgent otherConfigAgent = new PreviousConfigurationFinder(getConfigurationLocation().getParentFile()).findPreviousInstalls(searchRoot, getInstallFolder());
			if (otherConfigAgent == null) {
				return false;
			}
			previousProfile = ((IProfileRegistry) otherConfigAgent.getService(IProfileRegistry.SERVICE_NAME)).getProfile(IProfileRegistry.SELF);
			if (previousProfile == null)
				return false;

			reposToMigrate = ((IMetadataRepositoryManager) otherConfigAgent.getService(IMetadataRepositoryManager.SERVICE_NAME)).getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
			reposToMigrate = copyOf(reposToMigrate, reposToMigrate.length + 1);
			reposToMigrate[reposToMigrate.length - 1] = getURIForProfile(otherConfigAgent, previousProfile);
		}

		if (previousProfile == null && baseChangedSinceLastPresentationOfWizard(agent, registry, currentProfile))
			previousProfile = findMostRecentReset(registry, currentProfile);

		if (previousProfile == null)
			return false;

		Collection<IInstallableUnit> unitsToMigrate = findUnitstoMigrate(previousProfile, currentProfile);
		if (!unitsToMigrate.isEmpty()) {
			openMigrationWizard(previousProfile, unitsToMigrate, reposToMigrate);
		} else {
			//There is nothing to migrate, so we mark the migration complete
			rememberMigrationCompleted();
		}
		return true;
	}

	private static URI[] copyOf(URI[] original, int newLength) {
		URI[] copy = new URI[newLength];
		int copyCount = Math.min(original.length, newLength);
		System.arraycopy(original, 0, copy, 0, copyCount);
		return copy;
	}

	private URI getURIForProfile(IProvisioningAgent agent, IProfile profile) {
		IAgentLocation agentLocation = (IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME);
		return URIUtil.append(agentLocation.getRootLocation(), "org.eclipse.equinox.p2.engine/profileRegistry/" + profile.getProfileId() + ".profile"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private File getInstallFolder() {
		Location configurationLocation = ServiceHelper.getService(EngineActivator.getContext(), Location.class, Location.INSTALL_FILTER);
		return new File(configurationLocation.getURL().getPath());
	}

	//The search location is two level up from the configuration location.
	private File getSearchLocation() {
		File parent = getConfigurationLocation().getParentFile();
		if (parent == null)
			return null;
		return parent.getParentFile();
	}

	private File getConfigurationLocation() {
		Location configurationLocation = ServiceHelper.getService(EngineActivator.getContext(), Location.class, Location.CONFIGURATION_FILTER);
		File configurationFolder = new File(configurationLocation.getURL().getPath());
		return configurationFolder;
	}

	//Check if the user has explicitly specified -configuration on the command line
	private boolean configurationSpecifiedManually() {
		String commandLine = System.getProperty("eclipse.commands"); //$NON-NLS-1$
		if (commandLine == null)
			return false;
		return commandLine.contains("-configuration\n"); //$NON-NLS-1$
	}

	private boolean skipFirstTimeMigration() {
		return Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty(ECLIPSE_P2_SKIP_MOVED_INSTALL_DETECTION));
	}

	private boolean isFirstTimeRunningThisSharedInstance(IProvisioningAgent agent, IProfileRegistry registry, IProfile currentProfile) {
		long[] history = registry.listProfileTimestamps(currentProfile.getProfileId());
		boolean isInitial = IProfile.STATE_SHARED_INSTALL_VALUE_INITIAL.equals(registry.getProfileStateProperties(currentProfile.getProfileId(), history[0]).get(IProfile.STATE_PROP_SHARED_INSTALL));
		if (isInitial) {
			if (getLastMigration() >= history[0])
				return false;
			//This detect the case where the user has not done any migration.
			Map<String, String> sharedRelatedValues = registry.getProfileStateProperties(currentProfile.getProfileId(), IProfile.STATE_PROP_SHARED_INSTALL);
			if (sharedRelatedValues.containsValue(IProfile.STATE_SHARED_INSTALL_VALUE_NEW))
				return false;
			return true;
		}
		return false;
	}

	/**
	 * @param previousProfile is the profile used previous to the current one
	 * @param currentProfile is the current profile used by eclipse.
	 * @return true if set difference between previousProfile units and currentProfile units not empty, otherwise false
	 */
	protected Collection<IInstallableUnit> findUnitstoMigrate(IProfile previousProfile, IProfile currentProfile) {
		//First, try the case of inclusion
		Set<IInstallableUnit> previousProfileUnits = getUserRoots(previousProfile);
		Set<IInstallableUnit> currentProfileUnits = currentProfile.available(new UserVisibleRootQuery(), null).toSet();
		previousProfileUnits.removeAll(currentProfileUnits);

		//For the IUs left in the previous profile, look for those that could be in the base but not as roots
		Iterator<IInstallableUnit> previousProfileIterator = previousProfileUnits.iterator();
		while (previousProfileIterator.hasNext()) {
			if (!currentProfile.available(QueryUtil.createIUQuery(previousProfileIterator.next()), null).isEmpty())
				previousProfileIterator.remove();
		}

		//For the IUs left in the previous profile, look for those that could be available in the root but as higher versions (they could be root or not)
		previousProfileIterator = previousProfileUnits.iterator();
		while (previousProfileIterator.hasNext()) {
			if (!currentProfile.available(new UpdateQuery(previousProfileIterator.next()), null).isEmpty())
				previousProfileIterator.remove();
		}

		return previousProfileUnits;
	}

	private Set<IInstallableUnit> getUserRoots(IProfile previousProfile) {
		IQueryResult<IInstallableUnit> allRoots = previousProfile.query(new UserVisibleRootQuery(), null);
		Set<IInstallableUnit> rootsFromTheBase = previousProfile.query(new IUProfilePropertyQuery("org.eclipse.equinox.p2.base", "true"), null).toUnmodifiableSet();
		Set<IInstallableUnit> userRoots = allRoots.toSet();
		userRoots.removeAll(rootsFromTheBase);
		return userRoots;
	}

	protected void openMigrationWizard(final IProfile inputProfile, final Collection<IInstallableUnit> unitsToMigrate, final URI[] reposToMigrate) {
		Display d = Display.getDefault();
		d.asyncExec(new Runnable() {
			public void run() {
				WizardDialog migrateWizard = new MigrationWizardDialog(getWorkbenchWindowShell(), new MigrationWizard(inputProfile, unitsToMigrate, reposToMigrate, reposToMigrate != null));
				migrateWizard.create();
				migrateWizard.open();
			}
		});
	}

	private boolean baseChangedSinceLastPresentationOfWizard(IProvisioningAgent agent, IProfileRegistry registry, IProfile profile) {
		long lastProfileMigrated = getLastMigration();
		long lastResetTimestamp = findMostRecentResetTimestamp(registry, profile);
		return lastProfileMigrated <= lastResetTimestamp;
	}

	//The timestamp from which we migrated or -1
	private long findMostRecentResetTimestamp(IProfileRegistry registry, IProfile profile) {
		long[] history = registry.listProfileTimestamps(profile.getProfileId());
		int index = history.length - 1;
		boolean found = false;
		while (!(found = IProfile.STATE_SHARED_INSTALL_VALUE_BEFOREFLUSH.equals(registry.getProfileStateProperties(profile.getProfileId(), history[index]).get(IProfile.STATE_PROP_SHARED_INSTALL))) && index > 0) {
			index--;
		}
		if (!found)
			return -1;
		return history[index];
	}

	private IProfile findMostRecentReset(IProfileRegistry registry, IProfile profile) {
		long ts = findMostRecentResetTimestamp(registry, profile);
		if (ts == -1)
			return null;
		return registry.getProfile(profile.getProfileId(), ts);
	}

	Shell getWorkbenchWindowShell() {
		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		return activeWindow != null ? activeWindow.getShell() : null;
	}

	public void rememberMigrationCompleted() {
		IProfileRegistry registry = (IProfileRegistry) ProvisioningUI.getDefaultUI().getSession().getProvisioningAgent().getService(IProfileRegistry.SERVICE_NAME);
		long[] history = registry.listProfileTimestamps(ProvisioningUI.getDefaultUI().getProfileId());
		AutomaticUpdatePlugin.getDefault().getPreferenceStore().setValue(AutomaticUpdateScheduler.MIGRATION_DIALOG_SHOWN, history[history.length - 1]);
		AutomaticUpdatePlugin.getDefault().savePreferences();
	}

	//Get the timestamp that we migrated from. O if we have not migrated.
	public long getLastMigration() {
		return AutomaticUpdatePlugin.getDefault().getPreferenceStore().getLong(AutomaticUpdateScheduler.MIGRATION_DIALOG_SHOWN);
	}
}
