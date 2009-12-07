/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.installer;

import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import org.eclipse.equinox.p2.metadata.IVersionedId;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.installer.IInstallOperation;
import org.eclipse.equinox.internal.provisional.p2.installer.InstallDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * This operation performs installation or update of an Eclipse-based product.
 */
public class InstallUpdateProductOperation implements IInstallOperation {

	private IArtifactRepositoryManager artifactRepoMan;
	private BundleContext bundleContext;
	private IDirector director;
	private final InstallDescription installDescription;
	private boolean isInstall = true;
	private IMetadataRepositoryManager metadataRepoMan;
	private IProfileRegistry profileRegistry;
	private IStatus result;

	private ArrayList serviceReferences = new ArrayList();

	public InstallUpdateProductOperation(BundleContext context, InstallDescription description) {
		this.bundleContext = context;
		this.installDescription = description;
	}

	/**
	 * Determine what top level installable units should be installed by the director
	 */
	private IInstallableUnit[] computeUnitsToInstall() throws CoreException {
		ArrayList result = new ArrayList();
		IVersionedId roots[] = installDescription.getRoots();
		for (int i = 0; i < roots.length; i++) {
			IVersionedId root = roots[i];
			IInstallableUnit iu = findUnit(root);
			if (iu != null)
				result.add(iu);
		}
		return (IInstallableUnit[]) result.toArray(new IInstallableUnit[result.size()]);
	}

	/**
	 * This profile is being updated; return the units to uninstall from the profile.
	 */
	private IInstallableUnit[] computeUnitsToUninstall(IProfile p) {
		return (IInstallableUnit[]) p.query(InstallableUnitQuery.ANY, new Collector(), null).toArray(IInstallableUnit.class);
	}

	/**
	 * Create and return the profile into which units will be installed.
	 */
	private IProfile createProfile() throws ProvisionException {
		IProfile profile = getProfile();
		if (profile == null) {
			Map properties = new HashMap();
			properties.put(IProfile.PROP_INSTALL_FOLDER, installDescription.getInstallLocation().toString());
			EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(InstallerActivator.getDefault().getContext(), EnvironmentInfo.class.getName());
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

	/**
	 * Performs the actual product install or update.
	 */
	private void doInstall(SubMonitor monitor) throws CoreException {
		prepareMetadataRepositories();
		prepareArtifactRepositories();
		IProfile p = createProfile();
		IInstallableUnit[] toInstall = computeUnitsToInstall();
		monitor.worked(5);

		IStatus s;
		ProfileChangeRequest request = new ProfileChangeRequest(p);
		if (isInstall) {
			monitor.setTaskName(NLS.bind(Messages.Op_Installing, installDescription.getProductName()));
			request.addInstallableUnits(toInstall);
			s = director.provision(request, null, monitor.newChild(90));
		} else {
			monitor.setTaskName(NLS.bind(Messages.Op_Updating, installDescription.getProductName()));
			IInstallableUnit[] toUninstall = computeUnitsToUninstall(p);
			request.removeInstallableUnits(toUninstall);
			request.addInstallableUnits(toInstall);
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
		IQuery query = new InstallableUnitQuery(id, range);
		Collector collector = new Collector();
		Iterator matches = metadataRepoMan.query(query, collector, null).iterator();
		// pick the newest match
		IInstallableUnit newest = null;
		while (matches.hasNext()) {
			IInstallableUnit candidate = (IInstallableUnit) matches.next();
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
		ServiceReference ref = bundleContext.getServiceReference(name);
		if (ref == null)
			throw fail(Messages.Op_NoService + name);
		Object service = bundleContext.getService(ref);
		if (service == null)
			throw fail(Messages.Op_NoServiceImpl + name);
		serviceReferences.add(ref);
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
		for (Iterator it = serviceReferences.iterator(); it.hasNext();) {
			ServiceReference sr = (ServiceReference) it.next();
			bundleContext.ungetService(sr);
		}
		serviceReferences.clear();
	}

	private void preInstall() throws CoreException {
		//obtain required services
		serviceReferences.clear();
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
