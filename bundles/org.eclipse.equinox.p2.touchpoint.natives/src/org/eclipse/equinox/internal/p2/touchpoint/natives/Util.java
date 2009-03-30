package org.eclipse.equinox.internal.p2.touchpoint.natives;

import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.osgi.util.NLS;

public class Util {

	public static void log(String message) {
		LogHelper.log(createError(message));
	}

	public static IStatus createError(String message) {
		return new Status(IStatus.ERROR, Activator.ID, message);
	}

	public static String getInstallFolder(IProfile profile) {
		return profile.getProperty(IProfile.PROP_INSTALL_FOLDER);
	}

	private static AgentLocation getAgentLocation() {
		return (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
	}

	public static IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	public static IFileArtifactRepository getDownloadCacheRepo() throws ProvisionException {
		URI location = getDownloadCacheLocation();
		if (location == null)
			throw new IllegalStateException(Messages.could_not_obtain_download_cache);
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		if (manager == null)
			throw new IllegalStateException(Messages.artifact_repo_not_found);
		IArtifactRepository repository;
		try {
			repository = manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			// the download cache doesn't exist or couldn't be read. Create new cache.
			String repositoryName = location + " - Agent download cache"; //$NON-NLS-1$
			Map properties = new HashMap(1);
			properties.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			repository = manager.createRepository(location, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		}

		IFileArtifactRepository downloadCache = (IFileArtifactRepository) repository.getAdapter(IFileArtifactRepository.class);
		if (downloadCache == null)
			throw new ProvisionException(NLS.bind(Messages.download_cache_not_writeable, location));
		return downloadCache;
	}

	static private URI getDownloadCacheLocation() {
		AgentLocation location = getAgentLocation();
		return (location != null ? location.getArtifactRepositoryURI() : null);
	}
}
