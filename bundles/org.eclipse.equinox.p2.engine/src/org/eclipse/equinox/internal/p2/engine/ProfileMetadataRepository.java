package org.eclipse.equinox.internal.p2.engine;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.Activator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.eclipse.osgi.util.NLS;

public class ProfileMetadataRepository extends AbstractMetadataRepository {

	private static final String FILE_SCHEME = "file"; //$NON-NLS-1$
	private static final String DOT_PROFILE = ".profile"; //$NON-NLS-1$
	public static final String TYPE = "org.eclipse.equinox.p2.engine.repo.metadataRepository"; //$NON-NLS-1$
	public static final Integer VERSION = new Integer(1);
	private IProfile profile;

	public ProfileMetadataRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
		super(location.toString(), TYPE, VERSION.toString(), location, null, null, null);

		try {
			profile = getProfile(location);
		} catch (RuntimeException e) {
			throw new ProvisionException(new Status(IStatus.ERROR, EngineActivator.ID, ProvisionException.REPOSITORY_FAILED_READ, e.getMessage(), e));
		}
		publishArtifactRepos();
	}

	private void publishArtifactRepos() {
		List artifactRepos = new ArrayList();
		String bundlePool = profile.getProperty(IProfile.PROP_CACHE);
		if (bundlePool != null)
			artifactRepos.add(new File(bundlePool).toURI());

		String sharedBundlePool = profile.getProperty(IProfile.PROP_SHARED_CACHE);
		if (sharedBundlePool != null)
			artifactRepos.add(new File(sharedBundlePool).toURI());

		String dropinRepositories = profile.getProperty("org.eclipse.equinox.p2.cache.extensions"); //$NON-NLS-1$
		if (dropinRepositories != null) {
			StringTokenizer tokenizer = new StringTokenizer(dropinRepositories, "|"); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String repoLocation = ""; //$NON-NLS-1$
				try {
					repoLocation = tokenizer.nextToken();
					artifactRepos.add(new URI(repoLocation));
				} catch (URISyntaxException e) {
					LogHelper.log(new Status(IStatus.WARNING, EngineActivator.ID, "invalid repo reference with location: " + repoLocation, e)); //$NON-NLS-1$
				}
			}
		}

		IProvisioningEventBus bus = (IProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), IProvisioningEventBus.SERVICE_NAME);
		if (bus == null)
			return;
		for (Iterator it = artifactRepos.iterator(); it.hasNext();) {
			URI repo = (URI) it.next();
			bus.publishEvent(new RepositoryEvent(repo, IRepository.TYPE_ARTIFACT, RepositoryEvent.DISCOVERED, true));
		}
	}

	public void initialize(RepositoryState state) {
		// nothing to do
	}

	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		return profile.query(query, collector, monitor);
	}

	public static void validate(URI location, IProgressMonitor monitor) throws ProvisionException {
		try {
			getProfile(location);
		} catch (RuntimeException e) {
			throw new ProvisionException(new Status(IStatus.ERROR, EngineActivator.ID, ProvisionException.REPOSITORY_FAILED_READ, e.getMessage(), e));
		}
	}

	private static IProfile getProfile(URI location) throws ProvisionException {
		if (!FILE_SCHEME.equalsIgnoreCase(location.getScheme()))
			fail(location, ProvisionException.REPOSITORY_NOT_FOUND);

		File target = new File(location);
		if (!target.exists())
			fail(location, ProvisionException.REPOSITORY_NOT_FOUND);

		long timestamp = -1;
		int index = target.getName().lastIndexOf(DOT_PROFILE);
		if (index == -1)
			fail(location, ProvisionException.REPOSITORY_NOT_FOUND);
		String profileId = target.getName().substring(0, index);
		if (target.isFile()) {
			try {
				timestamp = Long.parseLong(profileId);
			} catch (NumberFormatException e) {
				fail(location, ProvisionException.REPOSITORY_FAILED_READ);
			}
			target = target.getParentFile();
			if (target == null)
				fail(location, ProvisionException.REPOSITORY_NOT_FOUND);
			index = target.getName().lastIndexOf(DOT_PROFILE);
			profileId = target.getName().substring(0, index);
		}
		profileId = SimpleProfileRegistry.unescape(profileId);

		File registryDirectory = target.getParentFile();
		if (registryDirectory == null)
			fail(location, ProvisionException.REPOSITORY_NOT_FOUND);
		SimpleProfileRegistry profileRegistry = new SimpleProfileRegistry(registryDirectory, null, false);
		if (timestamp == -1) {
			long[] timestamps = profileRegistry.listProfileTimestamps(profileId);
			timestamp = timestamps[timestamps.length - 1];
		}
		IProfile profile = profileRegistry.getProfile(profileId, timestamp);
		if (profile == null)
			fail(location, ProvisionException.REPOSITORY_NOT_FOUND);

		return profile;
	}

	private static void fail(URI location, int code) throws ProvisionException {
		switch (code) {
			case ProvisionException.REPOSITORY_NOT_FOUND :
				String msg = NLS.bind(Messages.io_NotFound, location);
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, null));
			case ProvisionException.REPOSITORY_FAILED_READ :
				msg = NLS.bind(Messages.io_FailedRead, location);
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, null));
		}
	}
}
