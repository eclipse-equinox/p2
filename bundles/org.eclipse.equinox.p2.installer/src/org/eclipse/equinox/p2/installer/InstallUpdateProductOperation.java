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
package org.eclipse.equinox.p2.installer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.installer.InstallerActivator;
import org.eclipse.equinox.prov.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.prov.core.helpers.ServiceHelper;
import org.eclipse.equinox.prov.director.IDirector;
import org.eclipse.equinox.prov.engine.IProfileRegistry;
import org.eclipse.equinox.prov.engine.Profile;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * This operation performs installation or update of an Eclipse-based product.
 */
public class InstallUpdateProductOperation implements IRunnableWithProgress {

	/**
	 * This constant comes from value of FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME.
	 * This profile property is being used as a short term solution for branding of the launcher.
	 */
	private static final String PROP_LAUNCHER_NAME = "org.eclipse.equinox.frameworkhandler.launcher.name";

	/**
	 * Constant for config folder property copied from EclipseTouchpoint.CONFIG_FOLDER.
	 */
	private final static String CONFIG_FOLDER = "eclipse.configurationFolder";

	private IArtifactRepositoryManager artifactRepoMan;
	private BundleContext bundleContext;
	private IDirector director;
	private final IInstallDescription installDescription;
	private boolean isInstall = true;
	private IMetadataRepositoryManager metadataRepoMan;
	private IProfileRegistry profileRegistry;
	private IStatus result;

	private ArrayList serviceReferences = new ArrayList();

	public InstallUpdateProductOperation(BundleContext context, IInstallDescription description) {
		this.bundleContext = context;
		this.installDescription = description;
	}

	/**
	 * Determine what top level installable units should be installed by the director
	 */
	private IInstallableUnit[] computeUnitsToInstall() throws CoreException {
		IInstallableUnit root = installDescription.getRootInstallableUnit();
		//The install description just contains a prototype of the root IU. We need
		//to find the real IU in an available metadata repository
		return new IInstallableUnit[] {findUnit(root.getId(), root.getVersion())};
	}

	/**
	 * Create and return the profile into which units will be installed.
	 */
	private Profile createProfile() {
		Profile profile = getProfile();
		if (profile == null) {
			profile = new Profile(installDescription.getProductName());
			profile.setValue(Profile.PROP_INSTALL_FOLDER, installDescription.getInstallLocation().toString());
			profile.setValue(Profile.PROP_FLAVOR, installDescription.getFlavor());
			profile.setValue(PROP_LAUNCHER_NAME, installDescription.getLauncherName());
			EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(InstallerActivator.getDefault().getContext(), EnvironmentInfo.class.getName());
			String env = "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch();
			profile.setValue(Profile.PROP_ENVIRONMENTS, env);
			profileRegistry.addProfile(profile);
		}
		return profile;
	}

	/**
	 * Throws an exception of severity error with the given error message.
	 */
	private CoreException fail(String message) {
		return fail(message, null);
	}

	/**
	 * Throws an exception of severity error with the given error message.
	 */
	private CoreException fail(String message, Throwable throwable) {
		return new CoreException(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, message, throwable));
	}

	/**
	 * Finds and returns the installable unit with the given id, and optionally the
	 * given version.
	 */
	private IInstallableUnit findUnit(String id, Version version) throws CoreException {
		if (id == null)
			throw fail("Installable unit id not specified");
		VersionRange range = VersionRange.emptyRange;
		if (version != null)
			range = new VersionRange(version, true, version, true);
		IMetadataRepository[] repos = metadataRepoMan.getKnownRepositories();
		for (int i = 0; i < repos.length; i++) {
			IInstallableUnit[] found = repos[i].query(id, range, null, true, null);
			if (found.length > 0)
				return found[0];
		}
		throw fail("Installable unit not found: " + id);
	}

	/**
	 * Returns the profile being installed into.
	 */
	private Profile getProfile() {
		return profileRegistry.getProfile(installDescription.getProductName());
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
			throw fail("Install requires a service that is not available: " + name);
		Object service = bundleContext.getService(ref);
		if (service == null)
			throw fail("Install requires a service implementation that is not available: " + name);
		serviceReferences.add(ref);
		return service;
	}

	/**
	 * Performs the actual product install or update.
	 */
	private void install(SubMonitor monitor) throws CoreException {
		prepareMetadataRepository();
		prepareArtifactRepository();
		Profile p = createProfile();
		IInstallableUnit[] toInstall = computeUnitsToInstall();
		monitor.worked(5);

		IStatus s;
		if (isInstall) {
			monitor.subTask("Installing...");
			s = director.install(toInstall, p, null, monitor.newChild(90));
		} else {
			monitor.subTask("Updating...");
			IInstallableUnit[] toUninstall = computeUnitsToUninstall(p);
			s = director.replace(toUninstall, toInstall, p, monitor.newChild(90));
		}
		if (!s.isOK())
			throw new CoreException(s);
	}

	/**
	 * This profile is being updated; return the units to uninstall from the profile.
	 */
	private IInstallableUnit[] computeUnitsToUninstall(Profile profile) {
		ArrayList units = new ArrayList();
		for (Iterator it = profile.getInstallableUnits(); it.hasNext();)
			units.add(it.next());
		return (IInstallableUnit[]) units.toArray(new IInstallableUnit[units.size()]);
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
		director = (IDirector) getService(IDirector.class.getName());
		metadataRepoMan = (IMetadataRepositoryManager) getService(IMetadataRepositoryManager.class.getName());
		artifactRepoMan = (IArtifactRepositoryManager) getService(IArtifactRepositoryManager.class.getName());
		profileRegistry = (IProfileRegistry) getService(IProfileRegistry.class.getName());
	}

	private void prepareArtifactRepository() {
		URL artifactRepo = installDescription.getArtifactRepository();
		if (artifactRepo != null)
			artifactRepoMan.loadRepository(artifactRepo, null);
	}

	private void prepareMetadataRepository() {
		URL metadataRepo = installDescription.getMetadataRepository();
		if (metadataRepo != null)
			metadataRepoMan.loadRepository(metadataRepo, null);
	}

	/**
	 * Registers information about the agent with the installed product,
	 * so it knows how to kick the agent to perform updates.
	 */
	private void registerAgent() throws CoreException {
		Profile profile = getProfile();
		File config = null;
		String configString = profile.getValue(CONFIG_FOLDER);
		if (configString == null)
			config = new File(new File(profile.getValue(Profile.PROP_INSTALL_FOLDER)), "configuration");
		else
			config = new File(configString);
		File agentFolder = new File(config, "org.eclipse.equinox.p2.installer");
		agentFolder.mkdirs();
		File agentFile = new File(agentFolder, "agent.properties");

		String commands = computeAgentCommandLine();

		Properties agentData = new Properties();
		agentData.put("eclipse.commands", commands);
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(agentFile));
			agentData.store(out, commands);
		} catch (IOException e) {
			throw fail("Error writing agent configuration data", e);
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException e) {
				//ignore
			}
		}
	}

	/**
	 * Returns the command line string that will launch the agent.
	 */
	private String computeAgentCommandLine() throws CoreException {
		String commands = System.getProperty("eclipse.commands");
		StringBuffer output = new StringBuffer(commands.length());
		StringTokenizer tokens = new StringTokenizer(commands, "\n");
		String launcherName = null;
		while (tokens.hasMoreTokens()) {
			String next = tokens.nextToken();
			//discard the launcher token
			if ("-launcher".equals(next) && tokens.hasMoreTokens()) {
				launcherName = tokens.nextToken();
			} else {
				output.append(' ');
				output.append(next);
			}
		}
		if (launcherName == null)
			throw fail("Unable to determine agent launcher name");
		return launcherName + output.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor pm) throws InvocationTargetException {
		SubMonitor monitor = SubMonitor.convert(pm, "Preparing to install", 100);
		try {
			try {
				preInstall();
				isInstall = getProfile() == null;
				String taskName = isInstall ? "Installing {0}" : "Updating {0}";
				monitor.setTaskName(NLS.bind(taskName, installDescription.getProductName()));
				install(monitor);
				result = new Status(IStatus.OK, InstallerActivator.PI_INSTALLER, isInstall ? "Install complete" : "Update complete", null);
				monitor.setTaskName("Some final housekeeping");
				if (isInstall)
					registerAgent();
			} finally {
				postInstall();
			}
		} catch (CoreException e) {
			this.result = e.getStatus();
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}
}
