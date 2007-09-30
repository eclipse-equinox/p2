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
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.console.ProvisioningHelper;
import org.eclipse.equinox.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.Query;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		Map args = context.getArguments();
		initializeFromArguments((String[]) args.get("application.args"));

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
		ProvisioningHelper.addArtifactRepository(artifactRepositoryLocation);
		IMetadataRepository metadataRepository = ProvisioningHelper.addMetadataRepository(metadataRepositoryLocation);
		IInstallableUnit[] roots = Query.query(new IQueryable[] {metadataRepository}, root, version == null ? null : new VersionRange(version, true, version, true), null, false, null);
		IStatus operationStatus = null;
		if (roots.length > 0) {
			if (install) {
				operationStatus = director.install(roots, profile, null, new NullProgressMonitor());
			} else {
				operationStatus = director.uninstall(roots, profile, new NullProgressMonitor());
			}
		} else {
			operationStatus = new Status(IStatus.INFO, "org.eclipse.equinox.p2.director.test", "The installable unit '" + root + "' has not been found");
		}

		if (operationStatus.isOK()) {
			System.out.println((install ? "installation" : "uninstallation") + " complete");
		} else {
			System.out.println((install ? "installation" : "uninstallation") + " failed. " + operationStatus);
			LogHelper.log(operationStatus);
		}
		return null;
	}

	public void stop() {
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

			if (args[i - 1].equalsIgnoreCase("-destination") || args[i - 1].equalsIgnoreCase("-dest"))
				destination = arg;

			if (args[i - 1].equalsIgnoreCase("-bundlepool") || args[i - 1].equalsIgnoreCase("-bp"))
				bundlePool = arg;

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

			if (args[i - 1].equalsIgnoreCase("-prov.os")) {
				os = arg;
			}
			if (args[i - 1].equalsIgnoreCase("-prov.ws")) {
				ws = arg;
			}
			if (args[i - 1].equalsIgnoreCase("-prov.nl")) {
				nl = arg;
			}
			if (args[i - 1].equalsIgnoreCase("-prov.arch")) {
				arch = arg;
			}
		}
	}

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
