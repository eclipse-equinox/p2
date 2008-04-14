/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director.app;

import java.io.File;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.console.ProvisioningHelper;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Version;

public class Application implements IApplication {
	private Path destination;
	private URL artifactRepositoryLocation;
	private URL metadataRepositoryLocation;
	private String root;
	private Version version = null;
	private String flavor;
	private String profileId;
	private String profileProperties; // a comma-separated list of property pairs "tag=value"
	private boolean install;
	private String bundlePool = null;
	private String nl;
	private String os;
	private String arch;
	private String ws;
	private boolean roamingProfile = false;
	private IPlanner planner;
	private IEngine engine;

	private ProfileChangeRequest buildProvisioningRequest(IProfile profile, Collector roots) {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		markRoots(request, roots);
		if (install) {
			request.addInstallableUnits((IInstallableUnit[]) roots.toArray(IInstallableUnit.class));
		} else {
			request.removeInstallableUnits((IInstallableUnit[]) roots.toArray(IInstallableUnit.class));
		}
		return request;
	}

	private String getEnvironmentProperty() {
		Properties values = new Properties();
		if (os != null)
			values.put("osgi.os", os); //$NON-NLS-1$
		if (nl != null)
			values.put("osgi.nl", nl); //$NON-NLS-1$
		if (ws != null)
			values.put("osgi.ws", ws); //$NON-NLS-1$
		if (arch != null)
			values.put("osgi.arch", arch); //$NON-NLS-1$
		if (values.isEmpty())
			return null;
		return toString(values);
	}

	private IProfile initializeProfile() {
		if (profileId == null)
			profileId = IProfileRegistry.SELF;
		IProfile profile = ProvisioningHelper.getProfile(profileId);
		if (profile == null) {
			Properties props = new Properties();
			props.setProperty(IProfile.PROP_INSTALL_FOLDER, destination.toOSString());
			props.setProperty(IProfile.PROP_FLAVOR, flavor);
			if (bundlePool != null)
				if (bundlePool.equals(Messages.destination_commandline))
					props.setProperty(IProfile.PROP_CACHE, destination.toOSString());
				else
					props.setProperty(IProfile.PROP_CACHE, bundlePool);
			if (roamingProfile)
				props.setProperty(IProfile.PROP_ROAMING, Boolean.TRUE.toString());

			String env = getEnvironmentProperty();
			if (env != null)
				props.setProperty(IProfile.PROP_ENVIRONMENTS, env);
			if (profileProperties != null) {
				putProperties(profileProperties, props);
			}
			profile = ProvisioningHelper.addProfile(profileId, props);
			String currentFlavor = profile.getProperty(IProfile.PROP_FLAVOR);
			if (currentFlavor != null && !currentFlavor.endsWith(flavor))
				throw new RuntimeException(Messages.Inconsistent_flavor);
		}
		return profile;
	}

	private void initializeRepositories() {
		ProvisioningHelper.addArtifactRepository(artifactRepositoryLocation);
		ProvisioningHelper.addMetadataRepository(metadataRepositoryLocation);
	}

	private void initializeServices() {
		IDirector director = (IDirector) ServiceHelper.getService(Activator.getContext(), IDirector.class.getName());
		if (director == null)
			throw new RuntimeException(Messages.Missing_director);

		planner = (IPlanner) ServiceHelper.getService(Activator.getContext(), IPlanner.class.getName());
		if (planner == null)
			throw new RuntimeException(Messages.Missing_planner);

		engine = (IEngine) ServiceHelper.getService(Activator.getContext(), IEngine.SERVICE_NAME);
		if (engine == null)
			throw new RuntimeException(Messages.Missing_Engine);
	}

	private void markRoots(ProfileChangeRequest request, Collector roots) {
		for (Iterator iterator = roots.iterator(); iterator.hasNext();) {
			request.setInstallableUnitProfileProperty((IInstallableUnit) iterator.next(), IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
		}
	}

	private IStatus planAndExecute(IProfile profile, ProvisioningContext context, ProfileChangeRequest request) {
		ProvisioningPlan result;
		IStatus operationStatus;
		result = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		if (!result.getStatus().isOK())
			operationStatus = result.getStatus();
		else {
			operationStatus = engine.perform(profile, new DefaultPhaseSet(), result.getOperands(), context, new NullProgressMonitor());
		}
		return operationStatus;
	}

	private void printRequest(ProfileChangeRequest request) {
		IInstallableUnit[] toAdd = request.getAddedInstallableUnits();
		IInstallableUnit[] toRemove = request.getRemovedInstallableUnits();
		for (int i = 0; i < toAdd.length; i++) {
			System.out.println(NLS.bind(Messages.Installing, toAdd[i].getId(), toAdd[i].getVersion()));
		}
		for (int i = 0; i < toRemove.length; i++) {
			System.out.println(NLS.bind(Messages.Uninstalling, toRemove[i].getId(), toRemove[i].getVersion()));
		}
	}

	public void processArguments(String[] args) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {

			if (args[i].equals("-roaming")) { //$NON-NLS-1$
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

			if (args[i - 1].equalsIgnoreCase("-profile")) //$NON-NLS-1$
				profileId = arg;

			if (args[i - 1].equalsIgnoreCase("-profileProperties") || args[i - 1].equalsIgnoreCase("-props")) //$NON-NLS-1$ //$NON-NLS-2$
				profileProperties = arg;

			// we create a path object here to handle ../ entries in the middle of paths
			if (args[i - 1].equalsIgnoreCase("-destination") || args[i - 1].equalsIgnoreCase("-dest")) //$NON-NLS-1$ //$NON-NLS-2$
				destination = new Path(arg);

			// we create a path object here to handle ../ entries in the middle of paths
			if (args[i - 1].equalsIgnoreCase("-bundlepool") || args[i - 1].equalsIgnoreCase("-bp")) //$NON-NLS-1$ //$NON-NLS-2$
				bundlePool = new Path(arg).toOSString();

			if (args[i - 1].equalsIgnoreCase("-metadataRepository") || args[i - 1].equalsIgnoreCase("-mr")) //$NON-NLS-1$ //$NON-NLS-2$
				metadataRepositoryLocation = new URL(arg);

			if (args[i - 1].equalsIgnoreCase("-artifactRepository") || args[i - 1].equalsIgnoreCase("-ar")) //$NON-NLS-1$ //$NON-NLS-2$
				artifactRepositoryLocation = new URL(arg);

			if (args[i - 1].equalsIgnoreCase("-flavor")) //$NON-NLS-1$
				flavor = arg;

			if (args[i - 1].equalsIgnoreCase("-installIU")) { //$NON-NLS-1$
				root = arg;
				install = true;
			}

			if (args[i - 1].equalsIgnoreCase("-version")) { //$NON-NLS-1$
				version = new Version(arg);
			}

			if (args[i - 1].equalsIgnoreCase("-uninstallIU")) { //$NON-NLS-1$
				root = arg;
				install = false;
			}

			if (args[i - 1].equalsIgnoreCase("-p2.os")) { //$NON-NLS-1$
				os = arg;
			}
			if (args[i - 1].equalsIgnoreCase("-p2.ws")) { //$NON-NLS-1$
				ws = arg;
			}
			if (args[i - 1].equalsIgnoreCase("-p2.nl")) { //$NON-NLS-1$
				nl = arg;
			}
			if (args[i - 1].equalsIgnoreCase("-p2.arch")) { //$NON-NLS-1$
				arch = arg;
			}
		}
	}

	/**
	 * @param pairs	a comma separated list of tag=value pairs
	 * @param properties the collection into which the pairs are put
	 */
	private void putProperties(String pairs, Properties properties) {
		StringTokenizer tok = new StringTokenizer(pairs, ",", true); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String next = tok.nextToken().trim();
			int i = next.indexOf('=');
			if (i > 0 && i < next.length() - 1) {
				String tag = next.substring(0, i).trim();
				String value = next.substring(i + 1, next.length()).trim();
				if (tag.length() > 0 && value.length() > 0) {
					properties.put(tag, value);
				}
			}
		}
	}

	public Object run(String[] args) throws Exception {
		long time = -System.currentTimeMillis();
		initializeServices();
		processArguments(args);
		IProfile profile = initializeProfile();
		initializeRepositories();

		Collector roots = ProvisioningHelper.getInstallableUnits(null, new InstallableUnitQuery(root, version == null ? VersionRange.emptyRange : new VersionRange(version, true, version, true)), new NullProgressMonitor());
		if (roots.size() <= 0)
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Missing_IU, root));

		if (!updateRoamingProperties(profile).isOK())
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Cant_change_roaming, profile.getProfileId()));

		ProvisioningContext context = new ProvisioningContext();
		ProfileChangeRequest request = buildProvisioningRequest(profile, roots);
		printRequest(request);
		IStatus operationStatus = planAndExecute(profile, context, request);

		time += System.currentTimeMillis();
		if (operationStatus.isOK())
			System.out.println(NLS.bind(Messages.Operation_complete, new Long(time)));
		else {
			System.out.println(Messages.Operation_failed);
			LogHelper.log(operationStatus);
		}
		return null;
	}

	public Object start(IApplicationContext context) throws Exception {
		return run((String[]) context.getArguments().get("application.args")); //$NON-NLS-1$
	}

	public void stop() {
		//nothing to do
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

	private IStatus updateRoamingProperties(IProfile profile) {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		if (!Boolean.valueOf(profile.getProperty(IProfile.PROP_ROAMING)).booleanValue())
			return Status.OK_STATUS;
		if (!destination.equals(new File(profile.getProperty(IProfile.PROP_INSTALL_FOLDER)))) {
			request.setProfileProperty(IProfile.PROP_INSTALL_FOLDER, destination.toOSString());
		}
		if (!destination.equals(new File(profile.getProperty(IProfile.PROP_CACHE)))) {
			request.setProfileProperty(IProfile.PROP_CACHE, destination.toOSString());
		}
		if (request.getProfileProperties().size() == 0)
			return Status.OK_STATUS;

		ProvisioningPlan result = planner.getProvisioningPlan(request, new ProvisioningContext(), new NullProgressMonitor());
		if (!result.getStatus().isOK())
			return result.getStatus();

		return engine.perform(profile, new DefaultPhaseSet(), result.getOperands(), new ProvisioningContext(), new NullProgressMonitor());
	}
}
