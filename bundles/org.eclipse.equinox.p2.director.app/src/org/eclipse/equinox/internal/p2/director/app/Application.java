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
import org.eclipse.equinox.internal.provisional.p2.engine.phases.Sizing;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Version;

public class Application implements IApplication {
	private String destination;
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

	public void initializeFromArguments(String[] args) throws Exception {
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
				destination = new Path(arg).toOSString();

			// we create a path object here to handle ../ entries in the middle of paths
			if (args[i - 1].equalsIgnoreCase("-bundlepool") || args[i - 1].equalsIgnoreCase("-bp")) //$NON-NLS-1$ //$NON-NLS-2$
				bundlePool = new Path(arg).toOSString();

			if (args[i - 1].equalsIgnoreCase("-metadataRepository") || args[i - 1].equalsIgnoreCase("-mr")) //$NON-NLS-1$ //$NON-NLS-2$
				metadataRepositoryLocation = new URL(arg);

			if (args[i - 1].equalsIgnoreCase("-artifactRepository") | args[i - 1].equalsIgnoreCase("-ar")) //$NON-NLS-1$ //$NON-NLS-2$
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
		initializeFromArguments(args);

		if (profileId == null)
			profileId = IProfileRegistry.SELF;
		IProfile profile = ProvisioningHelper.getProfile(profileId);
		if (profile == null) {
			Properties props = new Properties();
			props.setProperty(IProfile.PROP_INSTALL_FOLDER, destination);
			props.setProperty(IProfile.PROP_FLAVOR, flavor);
			if (bundlePool != null)
				if (bundlePool.equals(Messages.destination_commandline))
					props.setProperty(IProfile.PROP_CACHE, destination);
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
		IDirector director = (IDirector) ServiceHelper.getService(Activator.getContext(), IDirector.class.getName());
		if (director == null)
			throw new RuntimeException(Messages.Missing_director);

		IPlanner planner = (IPlanner) ServiceHelper.getService(Activator.getContext(), IPlanner.class.getName());
		if (planner == null)
			throw new RuntimeException(Messages.Missing_planner);

		IEngine engine = (IEngine) ServiceHelper.getService(Activator.getContext(), IEngine.SERVICE_NAME);
		if (engine == null)
			throw new RuntimeException(Messages.Missing_Engine);

		ProvisioningHelper.addArtifactRepository(artifactRepositoryLocation);
		IMetadataRepository metadataRepository = ProvisioningHelper.addMetadataRepository(metadataRepositoryLocation);
		VersionRange range = version == null ? VersionRange.emptyRange : new VersionRange(version, true, version, true);
		IInstallableUnit[] roots = (IInstallableUnit[]) metadataRepository.query(new InstallableUnitQuery(root, range), new Collector(), null).toArray(IInstallableUnit.class);
		ProvisioningPlan result = null;
		IStatus operationStatus = null;
		ProvisioningContext context = new ProvisioningContext();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		if (roots.length > 0) {
			if (install) {
				request.addInstallableUnits(roots);
			} else {
				request.removeInstallableUnits(roots);
			}
			result = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
			if (!result.getStatus().isOK())
				operationStatus = result.getStatus();
			else {
				Sizing sizeComputer = new Sizing(100, "Compute sizes"); //$NON-NLS-1$
				PhaseSet set = new PhaseSet(new Phase[] {sizeComputer}) {/*empty */};
				operationStatus = engine.perform(profile, set, result.getOperands(), context, new NullProgressMonitor());
				System.out.println(Messages.Disk_size + sizeComputer.getDiskSize());
				System.out.println(Messages.Download_size + sizeComputer.getDlSize());
				request = new ProfileChangeRequest(profile);
				if (install)
					request.addInstallableUnits(roots);
				else
					request.removeInstallableUnits(roots);
				operationStatus = director.provision(request, null, new NullProgressMonitor());
			}
		} else {
			operationStatus = new Status(IStatus.INFO, Activator.ID, NLS.bind(Messages.Missing_IU, root));
		}

		time += System.currentTimeMillis();
		if (operationStatus.isOK()) {
			System.out.println(NLS.bind(install ? Messages.Installation_complete : Messages.Uninstallation_complete, new Long(time)));
		} else {
			System.out.println(install ? Messages.Installation_failed : Messages.Uninstallation_failed + operationStatus);
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
}
