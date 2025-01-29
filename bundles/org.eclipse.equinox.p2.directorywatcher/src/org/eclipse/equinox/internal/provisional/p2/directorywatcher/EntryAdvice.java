/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others. 
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.directorywatcher;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.internal.p2.update.Site;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

/**
 * Entry advice captures the name, location, modified time, shape etc of something
 * discovered by the repository listener.  It is a simplified structure intended to represent
 * only one entry at a time and that entry is the the only entry being published.  
 */
public class EntryAdvice implements IPropertyAdvice {
	private final Map<String, String> metadataProps = new HashMap<>();
	private final Map<String, String> artifactProps = new HashMap<>();

	@Override
	public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
		return true;
	}

	void setProperties(File location, long timestamp, URI reference) {
		setProperties(location, timestamp, reference, null);
	}

	void setProperties(File location, long timestamp, URI reference, String linkFile) {
		if (reference == null)
			artifactProps.remove(RepositoryListener.ARTIFACT_REFERENCE);
		else
			artifactProps.put(RepositoryListener.ARTIFACT_REFERENCE, reference.toString());
		if (location.isDirectory())
			artifactProps.put(RepositoryListener.ARTIFACT_FOLDER, Boolean.TRUE.toString());
		else
			artifactProps.remove(RepositoryListener.ARTIFACT_FOLDER);
		artifactProps.put(RepositoryListener.FILE_NAME, location.getAbsolutePath());
		metadataProps.put(RepositoryListener.FILE_NAME, location.getAbsolutePath());
		metadataProps.put(RepositoryListener.FILE_LAST_MODIFIED, Long.toString(timestamp));
		if (linkFile != null)
			metadataProps.put(Site.PROP_LINK_FILE, linkFile);
	}

	@Override
	public Map<String, String> getArtifactProperties(IInstallableUnit iu, IArtifactDescriptor descriptor) {
		return artifactProps;
	}

	@Override
	public Map<String, String> getInstallableUnitProperties(InstallableUnitDescription iu) {
		return metadataProps;
	}
}
