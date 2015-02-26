/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *     Sonatype, Inc. - ongoing development
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.installer;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.internal.provisional.p2.installer.IInstallOperation;
import org.eclipse.equinox.internal.provisional.p2.installer.InstallDescription;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.NLS;

/**
 * This operation performs installation or update of an Eclipse-based product.
 */
public class InstallUpdateProductOperation implements IInstallOperation {

	private IArtifactRepositoryManager artifactRepoMan;
	private IProvisioningAgent agent;
	private IDirector director;
	private final InstallDescription installDescription;
	private boolean isInstall = true;
	private IMetadataRepositoryManager metadataRepoMan;
	private IProfileRegistry profileRegistry;
	private IStatus result;

	public InstallUpdateProductOperation(IProvisioningAgent agent, InstallDescription description) {
		this.agent = agent;
		this.installDescription = description;
	}

	/**
	 * Determine what top level installable units should be installed by the director
	 */
	private Collection<IInstallableUnit> computeUnitsToInstall() throws CoreException {
		ArrayList<IInstallableUnit> units = new ArrayList<IInstallableUnit>();
		IVersionedId roots[] = installDescription.getRoots();
		for (int i = 0; i < roots.length; i++) {
			IVersionedId root = roots[i];
			IInstallableUnit iu = findUnit(root);
			if (iu != null)
				units.add(iu);
		}
		return units;
	}

	/**
	 * This profile is being updated; return the units to uninstall from the profile.
	 */
	private IQueryResult<IInstallableUnit> computeUnitsToUninstall(IProfile p) {
		return p.query(QueryUtil.createIUAnyQuery(), null);
	}

	/**
	 * Create and return the profile into which units will be installed.
	 */
	private IProfile createProfile() throws ProvisionException {
		IProfile profile = getProfile();
		if (profile == null) {
			Map<String, String> properties = new HashMap<String, String>();
			properties.put(IProfile.PROP_INSTALL_FOLDER, installDescription.getInstallLocation().toString());
			EnvironmentInfo info = ServiceHelper.getService(InstallerActivator.getDefault().getContext(), EnvironmentInfo.class);
			String env = "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			properties.put(IProfile.PROP_ENVIRONMENTS, env);
			properties.put(IProfile.PROP_NAME, installDescription.getProductName());
			properties.putAll(installDescription.getProfileProperties());
			IPath location = installDescription.getBundleLocation();
			if (location != null)
				properties.put(IProfile.PROP_CACHE, location.toOSString());
			profile = profileRegistry.addProfile(getProfileId(), properties);
		}
		return profile;
	}

	IPlanner getPlanner() {
		return (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
	}

	/**
	 * Performs the actual product install or update.
	 */
	private void doInstall(SubMonitor monitor) throws CoreException {
		prepareMetadataRepositories();
		prepareArtifactRepositories();
		IProfile p = createProfile();
		Collection<IInstallableUnit> toInstall = computeUnitsToInstall();
		monitor.worked(5);

		IStatus s;
		IProfileChangeRequest request = getPlanner().createChangeRequest(p);
		if (isInstall) {
			monitor.setTaskName(NLS.bind(Messages.Op_Installing, installDescription.getProductName()));
			request.addAll(toInstall);
			s = director.provision(request, null, monitor.newChild(90));
		} else {
			monitor.setTaskName(NLS.bind(Messages.Op_Updating, installDescription.getProductName()));
			IQueryResult<IInstallableUnit> toUninstall = computeUnitsToUninstall(p);
			request.removeAll(toUninstall.toUnmodifiableSet());
			request.addAll(toInstall);
			s = director.provision(request, null, monitor.newChild(90));
		}
		if (!s.isOK())
			throw new CoreException(s);
	}

	/**
	 * Returns an exception of severity error with the given error message.
	 */
	private CoreException fail(String message) {
		return fail(message, null);
	}

	/**
	 * Returns an exception of severity error with the given error message.
	 */
	private CoreException fail(String message, Throwable throwable) {
		return new CoreException(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, message, throwable));
	}

	/**
	 * Finds and returns the installable unit with the given id, and optionally the
	 * given version.
	 */
	private IInstallableUnit findUnit(IVersionedId spec) throws CoreException {
		String id = spec.getId();
		if (id == null)
			throw fail(Messages.Op_NoId);
		Version version = spec.getVersion();
		VersionRange range = VersionRange.emptyRange;
		if (version != null && !version.equals(Version.emptyVersion))
			range = new VersionRange(version, true, version, true);
		IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(id, range);
		Iterator<IInstallableUnit> matches = metadataRepoMan.query(query, null).iterator();
		// pick the newest match
		IInstallableUnit newest = null;
		while (matches.hasNext()) {
			IInstallableUnit candidate = matches.next();
			if (newest == null || (newest.getVersion().compareTo(candidate.getVersion()) < 0))
				newest = candidate;
		}
		if (newest == null)
			throw fail(Messages.Op_IUNotFound + id);
		return newest;
	}

	/**
	 * Returns the profile being installed into.
	 */
	private IProfile getProfile() {
		return profileRegistry.getProfile(getProfileId());
	}

	/**
	 * Returns the id of the profile to use for install/update based on this operation's install description.
	 */
	private String getProfileId() {
		IPath location = installDescription.getInstallLocation();
		if (location != null)
			return location.toString();
		return installDescription.getProductName();
	}

	/**
	 * Returns the result of the install operation, or <code>null</code> if
	 * no install operation has been run.
	 */
	public IStatus getResult() {
		return result;
	}

	private Object getService(String name) throws CoreException {
		Object service = agent.getService(name);
		if (service == null)
			throw fail(Messages.Op_NoServiceImpl + name);
		return service;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.installer.IInstallOperation#install(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus install(IProgressMonitor pm) {
		SubMonitor monitor = SubMonitor.convert(pm, Messages.Op_Preparing, 100);
		try {
			try {
				preInstall();
				isInstall = getProfile() == null;
				doInstall(monitor);
				result = new Status(IStatus.OK, InstallerActivator.PI_INSTALLER, isInstall ? Messages.Op_InstallComplete : Messages.Op_UpdateComplete, null);
				monitor.setTaskName(Messages.Op_Cleanup);
			} finally {
				postInstall();
			}
		} catch (CoreException e) {
			result = e.getStatus();
		} finally {
			monitor.done();
		}
		return result;
	}

	/**
	 * Returns whether this operation represents the product being installed
	 * for the first time, in a new profile.
	 */
	public boolean isFirstInstall() {
		return isInstall;
	}

	private void postInstall() {
		//nothing to do
	}

	private void preInstall() throws CoreException {
		//obtain required services
		director = (IDirector) getService(IDirector.SERVICE_NAME);
		metadataRepoMan = (IMetadataRepositoryManager) getService(IMetadataRepositoryManager.SERVICE_NAME);
		artifactRepoMan = (IArtifactRepositoryManager) getService(IArtifactRepositoryManager.SERVICE_NAME);
		profileRegistry = (IProfileRegistry) getService(IProfileRegistry.SERVICE_NAME);
	}

	private void prepareArtifactRepositories() throws ProvisionException {
		URI[] repos = installDescription.getArtifactRepositories();
		if (repos == null)
			return;

		// Repositories must be registered before they are loaded
		// This is to avoid them being possibly overridden with the configuration as a referenced repository
		for (int i = 0; i < repos.length; i++) {
			artifactRepoMan.addRepository(repos[i]);
			artifactRepoMan.loadRepository(repos[i], null);
		}
	}

	private void prepareMetadataRepositories() throws ProvisionException {
		URI[] repos = installDescription.getMetadataRepositories();
		if (repos == null)
			return;

		// Repositories must be registered before they are loaded
		// This is to avoid them being possibly overridden with the configuration as a referenced repository
		for (int i = 0; i < repos.length; i++) {
			metadataRepoMan.addRepository(repos[i]);
			metadataRepoMan.loadRepository(repos[i], null);
		}
	}
}
