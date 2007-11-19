/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director.app;

import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.console.ProvisioningHelper;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.director.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.phases.Sizing;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * @since 3.3
 */
public class Application implements IApplication {

	private String destination;
	private URL artifactRepositoryLocation;
	private URL metadataRepositoryLocation;
	private String root;
	private String flavor;
	private String profileId;
	private boolean install;
	private String bundlePool = null;
	private String nl;
	private String os;
	private String arch;
	private String ws;
	private boolean roamingProfile = false;
	private Version version = null;

	private String getEnvironmentProperty() {
		Properties values = new Properties();
		if (os != null)
			values.put("osgi.os", os);
		if (nl != null)
			values.put("osgi.nl", nl);
		if (ws != null)
			values.put("osgi.ws", ws);
		if (arch != null)
			values.put("osgi.arch", arch);
		if (values.isEmpty())
			return null;
		return toString(values);
	}

	public void initializeFromArguments(String[] args) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {

			if (args[i].equals("-roaming")) {
				roamingProfile = true;
			}

			// check for args without parameters (i.e., a flag arg)

			// check for args with parameters. If we are at the last
			// argument or
			// if the next one
			// has a '-' as the first character, then we can't have an arg
			// with
			// a parm so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;

			String arg = args[++i];

			if (args[i - 1].equalsIgnoreCase("-profile"))
				profileId = arg;

			// we create a path object here to handle ../ entries in the middle of paths
			if (args[i - 1].equalsIgnoreCase("-destination") || args[i - 1].equalsIgnoreCase("-dest"))
				destination = new Path(arg).toOSString();

			// we create a path object here to handle ../ entries in the middle of paths
			if (args[i - 1].equalsIgnoreCase("-bundlepool") || args[i - 1].equalsIgnoreCase("-bp"))
				bundlePool = new Path(arg).toOSString();

			if (args[i - 1].equalsIgnoreCase("-metadataRepository") || args[i - 1].equalsIgnoreCase("-mr"))
				metadataRepositoryLocation = new URL(arg);

			if (args[i - 1].equalsIgnoreCase("-artifactRepository") | args[i - 1].equalsIgnoreCase("-ar"))
				artifactRepositoryLocation = new URL(arg);

			if (args[i - 1].equalsIgnoreCase("-flavor"))
				flavor = arg;

			if (args[i - 1].equalsIgnoreCase("-installIU")) {
				root = arg;
				install = true;
			}

			if (args[i - 1].equalsIgnoreCase("-version")) {
				version = new Version(arg);
			}

			if (args[i - 1].equalsIgnoreCase("-uninstallIU")) {
				root = arg;
				install = false;
			}

			if (args[i - 1].equalsIgnoreCase("-p2.os")) {
				os = arg;
			}
			if (args[i - 1].equalsIgnoreCase("-p2.ws")) {
				ws = arg;
			}
			if (args[i - 1].equalsIgnoreCase("-p2.nl")) {
				nl = arg;
			}
			if (args[i - 1].equalsIgnoreCase("-p2.arch")) {
				arch = arg;
			}
		}
	}

	public Object run(String[] args) throws Exception {
		long time = -System.currentTimeMillis();
		initializeFromArguments(args);

		Properties props = new Properties();
		props.setProperty(Profile.PROP_INSTALL_FOLDER, destination);
		props.setProperty(Profile.PROP_FLAVOR, flavor);
		if (bundlePool != null)
			if (bundlePool.equals("<destination>"))
				props.setProperty("eclipse.p2.cache", destination);
			else
				props.setProperty("eclipse.p2.cache", bundlePool);
		if (roamingProfile)
			props.setProperty("eclipse.p2.roaming", "true");

		String env = getEnvironmentProperty();
		if (env != null)
			props.setProperty(Profile.PROP_ENVIRONMENTS, env);
		Profile profile = ProvisioningHelper.addProfile(profileId, props);
		String currentFlavor = profile.getValue(Profile.PROP_FLAVOR);
		if (currentFlavor != null && !currentFlavor.endsWith(flavor))
			throw new RuntimeException("Install flavor not consistent with profile flavor");

		IDirector director = (IDirector) ServiceHelper.getService(Activator.getContext(), IDirector.class.getName());
		if (director == null)
			throw new RuntimeException("Director could not be loaded");

		IPlanner planner = (IPlanner) ServiceHelper.getService(Activator.getContext(), IPlanner.class.getName());
		if (planner == null)
			throw new RuntimeException("Planner could not be loaded");

		Engine engine = (Engine) ServiceHelper.getService(Activator.getContext(), Engine.class.getName());
		if (engine == null)
			throw new RuntimeException("Engine could not be loaded");

		ProvisioningHelper.addArtifactRepository(artifactRepositoryLocation);
		IMetadataRepository metadataRepository = ProvisioningHelper.addMetadataRepository(metadataRepositoryLocation);
		VersionRange range = version == null ? VersionRange.emptyRange : new VersionRange(version, true, version, true);
		IInstallableUnit[] roots = (IInstallableUnit[]) metadataRepository.query(new InstallableUnitQuery(root, range), new Collector(), null).toArray(IInstallableUnit.class);
		ProvisioningPlan result = null;
		IStatus operationStatus = null;
		if (roots.length > 0) {
			if (install) {
				result = planner.getInstallPlan(roots, profile, new NullProgressMonitor());
			} else {
				result = planner.getUninstallPlan(roots, profile, new NullProgressMonitor());
			}
			if (!result.getStatus().isOK())
				operationStatus = result.getStatus();
			else {
				Sizing sizeComputer = new Sizing(100, "Compute sizes"); //$NON-NLS-1$
				PhaseSet set = new PhaseSet(new Phase[] {sizeComputer}) {};
				operationStatus = engine.perform(profile, set, result.getOperands(), new NullProgressMonitor());
				System.out.println("Estimated size on disk " + sizeComputer.getDiskSize());
				System.out.println("Estimated download size " + sizeComputer.getDlSize());
				operationStatus = engine.perform(profile, new DefaultPhaseSet(), result.getOperands(), new NullProgressMonitor());
			}
		} else {
			operationStatus = new Status(IStatus.INFO, "org.eclipse.equinox.p2.director.test", "The installable unit '" + root + "' has not been found");
		}

		time += System.currentTimeMillis();
		if (operationStatus.isOK()) {
			System.out.println((install ? "installation" : "uninstallation") + " complete (" + time + "ms)");
		} else {
			System.out.println((install ? "installation" : "uninstallation") + " failed. " + operationStatus);
			LogHelper.log(operationStatus);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		return run((String[]) context.getArguments().get("application.args"));
	}

	public void stop() {
	}

	private String toString(Properties context) {
		StringBuffer result = new StringBuffer();
		for (Enumeration iter = context.keys(); iter.hasMoreElements();) {
			String key = (String) iter.nextElement();
			result.append(key);
			result.append('=');
			result.append(context.get(key));
			if (iter.hasMoreElements())
				result.append(',');
		}
		return result.toString();
	}
}
