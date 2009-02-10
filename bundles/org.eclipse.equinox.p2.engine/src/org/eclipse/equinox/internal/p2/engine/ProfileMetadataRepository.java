package org.eclipse.equinox.internal.p2.engine;

import java.io.File;
import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;

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

	private static IProfile getProfile(URI location) {
		if (!FILE_SCHEME.equalsIgnoreCase(location.getScheme()))
			throw new IllegalArgumentException("Profile Repostory must use 'file' protocol."); //$NON-NLS-1$

		File target = new File(location);
		if (!target.exists())
			throw new IllegalArgumentException("Profile not found: " + location.toString()); //$NON-NLS-1$

		long timestamp = -1;
		int index = target.getName().lastIndexOf(DOT_PROFILE);
		String profileId = target.getName().substring(0, index);
		if (target.isFile()) {
			try {
				timestamp = Long.parseLong(profileId);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Bad timestamp format id syntax: " + profileId); //$NON-NLS-1$
			}
			target = target.getParentFile();
			if (target == null) {
				throw new IllegalArgumentException("Profile not found: " + location.toString()); //$NON-NLS-1$
			}
			index = target.getName().lastIndexOf(DOT_PROFILE);
			profileId = target.getName().substring(0, index);
		}
		profileId = SimpleProfileRegistry.unescape(profileId);

		File registryDirectory = target.getParentFile();
		if (registryDirectory == null)
			throw new IllegalArgumentException("Profile registry not found for profile: " + location.toString()); //$NON-NLS-1$
		SimpleProfileRegistry profileRegistry = new SimpleProfileRegistry(registryDirectory, null, false);
		if (timestamp == -1) {
			long[] timestamps = profileRegistry.listProfileTimestamps(profileId);
			timestamp = timestamps[timestamps.length - 1];
		}
		IProfile profile = profileRegistry.getProfile(profileId, timestamp);
		if (profile == null)
			throw new IllegalArgumentException("Profile not found in registry: " + profileId); //$NON-NLS-1$

		return profile;
	}
}
