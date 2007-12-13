package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.frameworkadmin.FrameworkAdmin;
import org.eclipse.equinox.internal.p2.core.helpers.Headers;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.TouchpointData;
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
	private static final String REPOSITORY_TYPE = "org.eclipse.equinox.p2.artifact.repository.simpleRepository"; //$NON-NLS-1$
	private static final Object PROFILE_EXTENSION = "profile.extension";

	static AgentLocation getAgentLocation() {
		return (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
	}

	static IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	private static URL getBundlePoolLocation(Profile profile) {
		String path = profile.getValue(CACHE_PATH);
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

	static IFileArtifactRepository getBundlePoolRepository(Profile profile) {
		URL location = getBundlePoolLocation(profile);
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		IArtifactRepository bundlePool = manager.loadRepository(location, null);
		if (bundlePool == null) {
			// 	the given repo location is not an existing repo so we have to create something
			String repositoryName = location + " - bundle pool"; //$NON-NLS-1$
			bundlePool = manager.createRepository(location, repositoryName, REPOSITORY_TYPE);
		}

		if (bundlePool == null) {
			throw new IllegalArgumentException("BundlePool repository not writeable: " + location); //$NON-NLS-1$
		}
		return (IFileArtifactRepository) bundlePool;
	}

	static IFileArtifactRepository getAggregatedBundleRepository(Profile profile) {
		Set bundleRepositories = new HashSet();
		bundleRepositories.add(Util.getBundlePoolRepository(profile));

		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		IArtifactRepository[] knownRepositories = manager.getKnownRepositories();
		for (int i = 0; i < knownRepositories.length; i++) {
			IArtifactRepository repository = knownRepositories[i];
			String profileExtension = (String) repository.getProperties().get(PROFILE_EXTENSION);
			if (profileExtension != null && profileExtension.equals(profile.getProfileId()))
				bundleRepositories.add(repository);

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

	static File getBundleFile(IArtifactKey artifactKey, Profile profile) {
		IFileArtifactRepository aggregatedView = getAggregatedBundleRepository(profile);
		File bundleJar = aggregatedView.getArtifactFile(artifactKey);
		return bundleJar;
	}

	static File getConfigurationFolder(Profile profile) {
		String config = profile.getValue(CONFIG_FOLDER);
		if (config != null)
			return new File(config);
		return new File(getInstallFolder(profile), "configuration"); //$NON-NLS-1$
	}

	static File getInstallFolder(Profile profile) {
		return new File(profile.getValue(Profile.PROP_INSTALL_FOLDER));
	}

	/**
	 * Returns the name of the Eclipse application launcher.
	 */
	static String getLauncherName(Profile profile) {
		String name = profile.getValue(FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME);
		if (name != null)
			return name;
		//create a default name based on platform
		//TODO Need a better solution for launcher name branding
		EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
		if (info.getOS() == org.eclipse.osgi.service.environment.Constants.OS_WIN32)
			return "eclipse.exe"; //$NON-NLS-1$
		return "eclipse"; //$NON-NLS-1$
	}

	static String getManifest(TouchpointData[] data) {
		for (int i = 0; i < data.length; i++) {
			String manifest = data[i].getInstructions("manifest"); //$NON-NLS-1$
			if (manifest != null)
				return manifest;
		}
		return null;
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
	static String computeRelativeAgentLocation(Profile profile) {
		URL agentURL = Util.getAgentLocation().getURL();
		//TODO handle proper path/url conversion
		IPath agentPath = new Path(agentURL.getPath());
		IPath configPath = new Path(Util.getConfigurationFolder(profile).getAbsolutePath());
		if (configPath.isPrefixOf(agentPath))
			return "@config.dir/" + agentPath.removeFirstSegments(configPath.segmentCount()).makeRelative().setDevice(null); //$NON-NLS-1$
		return agentURL.toString();
	}

}
