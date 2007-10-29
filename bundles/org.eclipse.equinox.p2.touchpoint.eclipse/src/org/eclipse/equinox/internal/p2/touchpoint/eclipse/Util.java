package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.frameworkadmin.FrameworkAdmin;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.helpers.*;
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

	static AgentLocation getAgentLocation() {
		return (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
	}

	static IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	static URL getBundlePoolLocation(Profile profile) {
		String path = profile.getValue(CACHE_PATH);
		if (path == null)
			path = Activator.getContext().getProperty(CACHE_PATH);
		if (path != null)
			try {
				// TODO this is a hack for now.
				return File.separatorChar == '/' ? new URL("file:" + path) : new URL("file:/" + path); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (MalformedURLException e) {
				// TODO Do nothing and use the default approach
			}
		AgentLocation location = getAgentLocation();
		if (location == null)
			return null;
		return location.getDataArea(Activator.ID);
	}

	static IFileArtifactRepository getBundlePoolRepo(Profile profile) {
		URL location = getBundlePoolLocation(profile);
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		IArtifactRepository repository = manager.loadRepository(location, null);
		if (repository == null) {
			// 	the given repo location is not an existing repo so we have to create something
			// TODO for now create a random repo by default.
			String repositoryName = location + " - bundle pool"; //$NON-NLS-1$
			repository = manager.createRepository(location, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository"); //$NON-NLS-1$
			// TODO: do we still need to do this
			tagAsImplementation(repository);
		}

		IFileArtifactRepository bundlePool = (IFileArtifactRepository) repository.getAdapter(IFileArtifactRepository.class);
		if (bundlePool == null) {
			throw new IllegalArgumentException("BundlePool repository not writeable: " + location); //$NON-NLS-1$
		}
		return bundlePool;
	}

	private static URL getDownloadCacheLocation() {
		AgentLocation location = getAgentLocation();
		return (location != null ? location.getArtifactRepositoryURL() : null);
	}

	static IFileArtifactRepository getDownloadCacheRepo() {
		URL location = getDownloadCacheLocation();
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		IArtifactRepository repository = manager.loadRepository(location, null);
		if (repository == null) {
			// 	the given repo location is not an existing repo so we have to create something
			// TODO for now create a random repo by default.
			String repositoryName = location + " - Agent download cache"; //$NON-NLS-1$
			repository = manager.createRepository(location, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository"); //$NON-NLS-1$
			// TODO: do we still need to do this
			tagAsImplementation(repository);
		}

		IFileArtifactRepository downloadCache = (IFileArtifactRepository) repository.getAdapter(IFileArtifactRepository.class);
		if (downloadCache == null) {
			throw new IllegalArgumentException("Agent download cache not writeable: " + location); //$NON-NLS-1$
		}
		return downloadCache;
	}

	// TODO: Will there be other repositories to tag as implementation?  Should this
	//		 method to some utility?
	private static void tagAsImplementation(IArtifactRepository repository) {
		//		if (repository != null && repository.getProperties().getProperty(IRepositoryInfo.IMPLEMENTATION_ONLY_KEY) == null) {
		//			IWritableRepositoryInfo writableInfo = (IWritableRepositoryInfo) repository.getAdapter(IWritableRepositoryInfo.class);
		//			if (writableInfo != null) {
		//				writableInfo.getModifiableProperties().setProperty(IRepositoryInfo.IMPLEMENTATION_ONLY_KEY, Boolean.valueOf(true).toString());
		//			}
		//		}
	}

	static BundleInfo createBundleInfo(File bundleFile, String manifest) {
		BundleInfo bundleInfo = new BundleInfo();
		try {
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

	static File getBundleFile(IArtifactKey artifactKey, boolean isZipped, Profile profile) throws IOException {

		if (!isZipped) {
			IFileArtifactRepository bundlePool = getBundlePoolRepo(profile);
			File bundleJar = bundlePool.getArtifactFile(artifactKey);
			return bundleJar;
		}

		// Handle zipped
		IFileArtifactRepository downloadCache = getDownloadCacheRepo();
		File bundleJar = downloadCache.getArtifactFile(artifactKey);
		if (bundleJar == null)
			return null;

		File bundleFolder = new File(getBundlePoolLocation(profile).getFile(), "plugins/" + artifactKey.getId() + '_' + artifactKey.getVersion()); //$NON-NLS-1$
		if (bundleFolder.exists())
			return bundleFolder;

		if (!bundleFolder.mkdir())
			throw new IOException("Can't create the folder: " + bundleFolder);

		FileUtils.unzipFile(bundleJar, bundleFolder);
		return bundleFolder;
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
