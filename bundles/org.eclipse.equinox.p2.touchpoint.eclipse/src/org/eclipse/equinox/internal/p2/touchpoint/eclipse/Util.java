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
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.TouchpointData;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class Util {

	/**
	 * TODO "cache" is probably not the right term for this location
	 */
	private final static String CACHE_PATH = "eclipse.p2.cache"; //$NON-NLS-1$
	private final static String CONFIG_FOLDER = "eclipse.configurationFolder"; //$NON-NLS-1$
	private static final String REPOSITORY_TYPE = IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY;
	private static final Object PROFILE_EXTENSION = "profile.extension"; //$NON-NLS-1$

	static AgentLocation getAgentLocation() {
		return (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
	}

	static IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	static URL getBundlePoolLocation(IProfile profile) {
		String path = profile.getProperty(CACHE_PATH);
		if (path == null)
			path = Activator.getContext().getProperty(CACHE_PATH);
		if (path != null)
			try {
				// TODO this is a hack for now.
				return new File(path).toURL();
			} catch (MalformedURLException e) {
				// TODO Do nothing and use the default approach
			}
		AgentLocation location = getAgentLocation();
		if (location == null)
			return null;
		return location.getDataArea(Activator.ID);
	}

	static IFileArtifactRepository getBundlePoolRepository(IProfile profile) {
		URL location = getBundlePoolLocation(profile);
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		try {
			return (IFileArtifactRepository) manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			//the repository doesn't exist, so fall through and create a new one
		}
		try {
			String repositoryName = location + " - bundle pool"; //$NON-NLS-1$
			IArtifactRepository bundlePool = manager.createRepository(location, repositoryName, REPOSITORY_TYPE);
			bundlePool.setProperty(IRepository.PROP_SYSTEM, Boolean.valueOf(true).toString());
			return (IFileArtifactRepository) bundlePool;
		} catch (ProvisionException e) {
			LogHelper.log(e);
			throw new IllegalArgumentException("Bundle pool repository not writeable: " + location); //$NON-NLS-1$
		}
	}

	static IFileArtifactRepository getAggregatedBundleRepository(IProfile profile) {
		Set bundleRepositories = new HashSet();
		bundleRepositories.add(Util.getBundlePoolRepository(profile));

		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		URL[] knownRepositories = manager.getKnownRepositories(IArtifactRepositoryManager.REPOSITORIES_ALL);
		for (int i = 0; i < knownRepositories.length; i++) {
			try {
				IArtifactRepository repository = manager.loadRepository(knownRepositories[i], null);
				String profileExtension = (String) repository.getProperties().get(PROFILE_EXTENSION);
				if (profileExtension != null && profileExtension.equals(profile.getProfileId()))
					bundleRepositories.add(repository);
			} catch (ProvisionException e) {
				//skip repositories that could not be read
			}
		}
		return new AggregatedBundleRepository(bundleRepositories);
	}

	static BundleInfo createBundleInfo(File bundleFile, String manifest) {
		BundleInfo bundleInfo = new BundleInfo();
		try {
			if (bundleFile != null)
				bundleInfo.setLocation(bundleFile.toURL().toExternalForm());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		bundleInfo.setManifest(manifest);
		try {
			Headers headers = Headers.parseManifest(new ByteArrayInputStream(manifest.getBytes()));
			ManifestElement[] element = ManifestElement.parseHeader("bsn", (String) headers.get(Constants.BUNDLE_SYMBOLICNAME)); //$NON-NLS-1$
			bundleInfo.setSymbolicName(element[0].getValue());
			bundleInfo.setVersion((String) headers.get(Constants.BUNDLE_VERSION));
		} catch (BundleException e) {
			e.printStackTrace();
		}
		return bundleInfo;
	}

	static File getBundleFile(IArtifactKey artifactKey, IProfile profile) {
		IFileArtifactRepository aggregatedView = getAggregatedBundleRepository(profile);
		File bundleJar = aggregatedView.getArtifactFile(artifactKey);
		return bundleJar;
	}

	static File getConfigurationFolder(IProfile profile) {
		String config = profile.getProperty(CONFIG_FOLDER);
		if (config != null)
			return new File(config);
		return new File(getInstallFolder(profile), "configuration"); //$NON-NLS-1$
	}

	static File getInstallFolder(IProfile profile) {
		return new File(profile.getProperty(IProfile.PROP_INSTALL_FOLDER));
	}

	static File getLauncherPath(IProfile profile) {
		return new File(getInstallFolder(profile), getLauncherName(profile));
	}

	/**
	 * Returns the name of the Eclipse application launcher.
	 */
	private static String getLauncherName(IProfile profile) {
		String name = profile.getProperty(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME);

		String os = getOSFromProfile(profile);
		if (os == null) {
			EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
			if (info != null)
				os = info.getOS();
		}
		if (name == null)
			name = "eclipse"; //$NON-NLS-1$

		if (os.equals(org.eclipse.osgi.service.environment.Constants.OS_MACOSX)) {
			return name + ".app/Contents/MacOS/" + name.toLowerCase();
		}
		return name;
	}

	private static String getOSFromProfile(IProfile profile) {
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

	static String getManifest(TouchpointData[] data, File bundleFile) {
		for (int i = 0; i < data.length; i++) {
			String manifest = data[i].getInstructions("manifest"); //$NON-NLS-1$
			if (manifest != null && manifest.length() > 0)
				return manifest;
		}
		if (bundleFile == null)
			return null;

		if (bundleFile.isDirectory()) {
			File manifestFile = new File(bundleFile, JarFile.MANIFEST_NAME);
			byte[] buffer = new byte[(int) manifestFile.length()];
			InputStream fis = null;
			try {
				fis = new FileInputStream(manifestFile);
				fis.read(buffer);
				return new String(buffer);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} finally {
				if (fis != null)
					try {
						fis.close();
					} catch (IOException e) {
						// ignore
					}
			}
		}

		ZipFile bundleJar = null;
		try {
			bundleJar = new ZipFile(bundleFile);
			ZipEntry manifestEntry = bundleJar.getEntry(JarFile.MANIFEST_NAME);
			byte[] buffer = new byte[(int) manifestEntry.getSize()];
			bundleJar.getInputStream(manifestEntry).read(buffer);
			return new String(buffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} finally {
			if (bundleJar != null)
				try {
					bundleJar.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

	public static void initFromManifest(String manifest, BundleInfo bInfo) {
		try {
			bInfo.setManifest(manifest);
			Headers headers = Headers.parseManifest(new ByteArrayInputStream(manifest.getBytes()));
			ManifestElement[] element = ManifestElement.parseHeader("bsn", (String) headers.get(Constants.BUNDLE_SYMBOLICNAME)); //$NON-NLS-1$
			bInfo.setSymbolicName(element[0].getValue());
			bInfo.setVersion((String) headers.get(Constants.BUNDLE_VERSION));
		} catch (BundleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Returns the agent location, if possible as a path relative to the configuration
	 * directory using the @config.dir substitution variable. AgentLocation will
	 * substitute this variable with the configuration folder location on startup.
	 * If the agent location is not a sub-directory of the configuration folder, this
	 * method simply returns the absolute agent location expressed as a URL.
	 */
	static String computeRelativeAgentLocation(IProfile profile) {
		URL agentURL = Util.getAgentLocation().getURL();
		//TODO handle proper path/url conversion
		IPath agentPath = new Path(agentURL.getPath());
		IPath configPath = new Path(Util.getConfigurationFolder(profile).getAbsolutePath());
		if (configPath.isPrefixOf(agentPath))
			return "@config.dir/" + agentPath.removeFirstSegments(configPath.segmentCount()).makeRelative().setDevice(null); //$NON-NLS-1$
		if (agentPath.removeLastSegments(1).equals(configPath.removeLastSegments(1)))
			return "@config.dir/../" + agentPath.lastSegment(); //$NON-NLS-1$
		return agentURL.toString();
	}

}
