package org.eclipse.equinox.p2.artifact.repository;

import java.io.File;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

public interface IFileArtifactRepository extends IArtifactRepository {
	/**
	 * Return the location of the full local file corresponding to the given 
	 * artifact key to the given key, or <code>null</code> if not available.
	 * 
	 * @return the location of the requested artifact or<code>null</code> if not available
	 */
	public File getArtifactFile(IArtifactKey key);

	/**
	 * Return the location of the local file corresponding to the given 
	 * artifact descriptor, or <code>null</code> if not available.
	 * 
	 * @return the location of the requested descriptor or<code>null</code> if not available
	 */
	public File getArtifactFile(IArtifactDescriptor descriptor);
}
