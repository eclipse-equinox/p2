/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.directorywatcher;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import org.eclipse.equinox.internal.p2.update.Site;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;

/**
 * Entry advice captures the name, location, modified time, shape etc of something
 * discovered by the repository listener.  It is a simplified structure intended to represent
 * only one entry at a time and that entry is the the only entry being published.  
 */
public class EntryAdvice implements IPropertyAdvice {
	private Properties metadataProps = new Properties();
	private Properties artifactProps = new Properties();

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
			artifactProps.setProperty(RepositoryListener.ARTIFACT_REFERENCE, reference.toString());
		if (location.isDirectory())
			artifactProps.setProperty(RepositoryListener.ARTIFACT_FOLDER, Boolean.TRUE.toString());
		else
			artifactProps.remove(RepositoryListener.ARTIFACT_FOLDER);
		artifactProps.setProperty(RepositoryListener.FILE_NAME, location.getAbsolutePath());
		metadataProps.setProperty(RepositoryListener.FILE_NAME, location.getAbsolutePath());
		metadataProps.setProperty(RepositoryListener.FILE_LAST_MODIFIED, Long.toString(timestamp));
		if (linkFile != null)
			metadataProps.setProperty(Site.PROP_LINK_FILE, linkFile);
	}

	public Map getInstructions(File location) {
		return null;
	}

	public Properties getArtifactProperties(IInstallableUnit iu, IArtifactDescriptor descriptor) {
		return artifactProps;
	}

	public Properties getInstallableUnitProperties(InstallableUnitDescription iu) {
		return metadataProps;
	}
}
