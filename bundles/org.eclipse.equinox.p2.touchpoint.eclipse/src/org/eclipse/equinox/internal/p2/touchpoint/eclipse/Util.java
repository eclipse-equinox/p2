/*******************************************************************************
 *  Copyright (c) 2007, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Incorporated - fix for bug 225145
 *     Code 9 - ongoing development
 *     Pascal Rapicault - Support for bundled macosx http://bugs.eclipse.org/57349
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.core.spi.Constants;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.NLS;

public class Util {

	/**
	 * TODO "cache" is probably not the right term for this location
	 */
	private static final String REPOSITORY_TYPE = IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY;
	private static final String CACHE_EXTENSIONS = "org.eclipse.equinox.p2.cache.extensions"; //$NON-NLS-1$
	private static final String PIPE = "|"; //$NON-NLS-1$

	/**
	 * Bit-mask value representing this profile's bundle pool
	 */
	public static final int AGGREGATE_CACHE = 0x01;
	/**
	 * Bit-mask value representing the shared profile's bundle pool in a shared install
	 */
	public static final int AGGREGATE_SHARED_CACHE = 0x02;
	/**
	 * Bit-mask value representing the extension locations, such as the dropins folder.
	 */
	public static final int AGGREGATE_CACHE_EXTENSIONS = 0x04;

	public static IAgentLocation getAgentLocation(IProvisioningAgent agent) {
		return (IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME);
	}

	public static IArtifactRepositoryManager getArtifactRepositoryManager(IProvisioningAgent agent) {
		return (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
	}

	public static URI getBundlePoolLocation(IProvisioningAgent agent, IProfile profile) {
		String path = profile.getProperty(IProfile.PROP_CACHE);
		if (path != null)
			return new File(path).toURI();
		IAgentLocation location = getAgentLocation(agent);
		if (location == null)
			return null;
		return location.getDataArea(Activator.ID);
	}

	public static synchronized IFileArtifactRepository getBundlePoolRepository(IProvisioningAgent agent, IProfile profile) {
		URI location = getBundlePoolLocation(agent, profile);
		if (location == null)
			return null;
		IArtifactRepositoryManager manager = getArtifactRepositoryManager(agent);
		try {
			return (IFileArtifactRepository) manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			//the repository doesn't exist, so fall through and create a new one
		}
		try {
			String repositoryName = Messages.BundlePool;
			Map<String, String> properties = new HashMap<String, String>(1);
			properties.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			return (IFileArtifactRepository) manager.createRepository(location, repositoryName, REPOSITORY_TYPE, properties);
		} catch (ProvisionException e) {
			LogHelper.log(e);
			throw new IllegalArgumentException(NLS.bind(Messages.bundle_pool_not_writeable, location));
		}
	}

	public static IFileArtifactRepository getAggregatedBundleRepository(IProvisioningAgent agent, IProfile profile) {
		return getAggregatedBundleRepository(agent, profile, AGGREGATE_CACHE | AGGREGATE_SHARED_CACHE | AGGREGATE_CACHE_EXTENSIONS);
	}

	public static IFileArtifactRepository getAggregatedBundleRepository(IProvisioningAgent agent, IProfile profile, int repoFilter) {
		List<IFileArtifactRepository> bundleRepositories = new ArrayList<IFileArtifactRepository>();

		// we check for a shared bundle pool first as it should be preferred over the user bundle pool in a shared install
		IArtifactRepositoryManager manager = getArtifactRepositoryManager(agent);
		if ((repoFilter & AGGREGATE_SHARED_CACHE) != 0) {
			String sharedCache = profile.getProperty(IProfile.PROP_SHARED_CACHE);
			if (sharedCache != null) {
				try {
					URI repoLocation = new File(sharedCache).toURI();
					IArtifactRepository repository = manager.loadRepository(repoLocation, null);
					if (repository != null && repository instanceof IFileArtifactRepository && !bundleRepositories.contains(repository))
						bundleRepositories.add((IFileArtifactRepository) repository);
				} catch (ProvisionException e) {
					//skip repository if it could not be read
				}
			}
		}

		if ((repoFilter & AGGREGATE_CACHE) != 0) {
			IFileArtifactRepository bundlePool = Util.getBundlePoolRepository(agent, profile);
			if (bundlePool != null)
				bundleRepositories.add(bundlePool);
		}

		if ((repoFilter & AGGREGATE_CACHE_EXTENSIONS) != 0) {
			List<String> repos = getListProfileProperty(profile, CACHE_EXTENSIONS);
			for (String repo : repos) {
				try {
					URI repoLocation;
					try {
						repoLocation = new URI(repo);
					} catch (URISyntaxException e) {
						//in 1.0 we wrote unencoded URL strings, so try as an unencoded string
						repoLocation = URIUtil.fromString(repo);
					}
					IArtifactRepository repository = manager.loadRepository(repoLocation, null);
					if (repository != null && repository instanceof IFileArtifactRepository && !bundleRepositories.contains(repository))
						bundleRepositories.add((IFileArtifactRepository) repository);
				} catch (ProvisionException e) {
					//skip repositories that could not be read
				} catch (URISyntaxException e) {
					// unexpected, URLs should be pre-checked
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e));
				}
			}
		}
		return new AggregatedBundleRepository(agent, bundleRepositories);
	}

	private static List<String> getListProfileProperty(IProfile profile, String key) {
		List<String> listProperty = new ArrayList<String>();
		String dropinRepositories = profile.getProperty(key);
		if (dropinRepositories != null) {
			StringTokenizer tokenizer = new StringTokenizer(dropinRepositories, PIPE);
			while (tokenizer.hasMoreTokens()) {
				listProperty.add(tokenizer.nextToken());
			}
		}
		return listProperty;
	}

	public static BundleInfo createBundleInfo(File bundleFile, IInstallableUnit unit) {
		BundleInfo bundleInfo = new BundleInfo();
		if (bundleFile != null)
			bundleInfo.setLocation(bundleFile.toURI());

		Collection<IProvidedCapability> capabilities = unit.getProvidedCapabilities();
		for (IProvidedCapability capability : capabilities) {
			String nameSpace = capability.getNamespace();
			if (nameSpace.equals("osgi.bundle")) { //$NON-NLS-1$
				bundleInfo.setSymbolicName(capability.getName());
				bundleInfo.setVersion(capability.getVersion().toString());
			} else if (nameSpace.equals("osgi.fragment")) { //$NON-NLS-1$
				String fragmentName = capability.getName();
				String fragmentHost = getFragmentHost(unit, fragmentName);
				// shouldn't happen as long as the metadata is well-formed
				if (fragmentHost == null)
					LogHelper.log(createError("Unable to find fragment host for IU: " + unit)); //$NON-NLS-1$
				else
					bundleInfo.setFragmentHost(fragmentHost);
				bundleInfo.setVersion(capability.getVersion().toString());
			}
		}
		return bundleInfo;
	}

	private static String getFragmentHost(IInstallableUnit unit, String fragmentName) {
		Collection<IRequirement> requires = unit.getRequirements();
		for (IRequirement iRequirement : requires) {
			if (iRequirement instanceof IRequiredCapability) {
				IRequiredCapability requiredCapability = (IRequiredCapability) iRequirement;
				if (fragmentName.equals(requiredCapability.getName())) {
					String fragmentHost = requiredCapability.getName();
					if (!requiredCapability.getRange().toString().equals("0.0.0")) { //$NON-NLS-1$
						fragmentHost += ";bundle-version=\"" + requiredCapability.getRange() + '"'; //$NON-NLS-1$
					}
					return fragmentHost;
				}
			}
		}
		return null;
	}

	public static File getArtifactFile(IProvisioningAgent agent, IArtifactKey artifactKey, IProfile profile) {
		IFileArtifactRepository aggregatedView = getAggregatedBundleRepository(agent, profile);
		File bundleJar = aggregatedView.getArtifactFile(artifactKey);
		return bundleJar;
	}

	public static File getConfigurationFolder(IProfile profile) {
		String config = profile.getProperty(IProfile.PROP_CONFIGURATION_FOLDER);
		if (config != null)
			return new File(config);
		return new File(getInstallFolder(profile), "configuration"); //$NON-NLS-1$
	}

	/*
	 * Do a look-up and return the OSGi install area if it is set.
	 */
	public static URL getOSGiInstallArea() {
		Location location = (Location) ServiceHelper.getService(Activator.getContext(), Location.class.getName(), Location.INSTALL_FILTER);
		if (location == null)
			return null;
		if (!location.isSet())
			return null;
		return location.getURL();
	}

	/*
	 * Helper method to return the eclipse.home location. Return
	 * null if it is unavailable.
	 */
	public static File getEclipseHome() {
		Location eclipseHome = (Location) ServiceHelper.getService(Activator.getContext(), Location.class.getName(), Location.ECLIPSE_HOME_FILTER);
		if (eclipseHome == null || !eclipseHome.isSet())
			return null;
		URL url = eclipseHome.getURL();
		if (url == null)
			return null;
		return URLUtil.toFile(url);
	}

	/**
	 * Returns the install folder for the profile, or <code>null</code>
	 * if no install folder is defined.
	 */
	public static File getInstallFolder(IProfile profile) {
		String folder = profile.getProperty(IProfile.PROP_INSTALL_FOLDER);
		return folder == null ? null : new File(folder);
	}

	public static File getLauncherPath(IProfile profile) {
		String name = profile.getProperty(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME);
		if (name == null || name.length() == 0)
			name = "eclipse"; //$NON-NLS-1$
		String launcherName = getLauncherName(name, (isMacOSBundled(profile) ? Constants.MACOSX_BUNDLED : getOSFromProfile(profile)), getInstallFolder(profile));
		return launcherName == null ? null : new File(getInstallFolder(profile), launcherName);
	}

	public static boolean isMacOSBundled(IProfile profile) {
		String environments = profile.getProperty(IProfile.PROP_ENVIRONMENTS);
		if (environments == null)
			return false;
		if (environments.indexOf(Constants.MACOSX_BUNDLED + "=true") != -1) //$NON-NLS-1$
			return true;
		return false;
	}

	/**
	 * Returns the name of the Eclipse application launcher.
	 */
	private static String getLauncherName(String name, String os, File installFolder) {
		if (os == null) {
			EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
			if (info == null)
				return null;
			os = info.getOS();
		}

		if (os.equals(org.eclipse.osgi.service.environment.Constants.OS_WIN32)) {
			IPath path = new Path(name);
			if ("exe".equals(path.getFileExtension())) //$NON-NLS-1$
				return name;
			return name + ".exe"; //$NON-NLS-1$
		}
		if (os.equals(Constants.MACOSX_BUNDLED)) {
			return "/Contents/MacOS/" + name; //$NON-NLS-1$
		}

		if (os.equals(org.eclipse.osgi.service.environment.Constants.OS_MACOSX)) {
			IPath path = new Path(name);
			if (path.segment(0).endsWith(".app")) //$NON-NLS-1$
				return name;

			String appName = null;
			if (installFolder != null) {
				File appFolder = new File(installFolder, name + ".app"); //$NON-NLS-1$
				if (appFolder.exists()) {
					try {
						appName = appFolder.getCanonicalFile().getName();
					} catch (IOException e) {
						appName = appFolder.getName();
					}
				}
			}

			StringBuffer buffer = new StringBuffer();
			if (appName != null) {
				buffer.append(appName);
			} else {
				buffer.append(name.substring(0, 1).toUpperCase());
				buffer.append(name.substring(1));
				buffer.append(".app"); //$NON-NLS-1$
			}
			buffer.append("/Contents/MacOS/"); //$NON-NLS-1$
			buffer.append(name);
			return buffer.toString();
		}
		return name;
	}

	public static String getOSFromProfile(IProfile profile) {
		String environments = profile.getProperty(IProfile.PROP_ENVIRONMENTS);
		if (environments == null)
			return null;
		for (StringTokenizer tokenizer = new StringTokenizer(environments, ","); tokenizer.hasMoreElements();) { //$NON-NLS-1$
			String entry = tokenizer.nextToken();
			int i = entry.indexOf('=');
			String key = entry.substring(0, i).trim();
			if (!key.equals("osgi.os")) //$NON-NLS-1$
				continue;
			return entry.substring(i + 1).trim();
		}
		return null;
	}

	public static IStatus createError(String message) {
		return createError(message, null);
	}

	public static IStatus createError(String message, Exception e) {
		return new Status(IStatus.ERROR, Activator.ID, message, e);
	}

	public static File getLauncherConfigLocation(IProfile profile) {
		String launcherConfig = profile.getProperty(IProfile.PROP_LAUNCHER_CONFIGURATION);
		return launcherConfig == null ? null : new File(launcherConfig);
	}

	public static String resolveArtifactParam(Map<String, Object> parameters) throws CoreException {
		String artifactLocation = (String) parameters.get(EclipseTouchpoint.PARM_ARTIFACT_LOCATION);
		if (artifactLocation != null)
			return artifactLocation;

		IArtifactKey artifactKey = (IArtifactKey) parameters.get(EclipseTouchpoint.PARM_ARTIFACT);
		if (artifactKey == null) {
			IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);
			throw new CoreException(Util.createError(NLS.bind(Messages.iu_contains_no_arifacts, iu)));
		}

		throw new CoreException(Util.createError(NLS.bind(Messages.artifact_file_not_found, artifactKey)));
	}
}
