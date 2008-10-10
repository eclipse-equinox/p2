/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
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
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.osgi.framework.Version;

/**
 * Entry advice captures the name, location, modified time, shape etc of something
 * discovered by the repository listener.  It is a simplified structure intended to represent
 * only one entry at a time and that entry is the the only entry being published.  
 */
public class EntryAdvice implements IFeatureAdvice, IBundleAdvice {
	private Properties metadataProps = new Properties();
	private Properties artifactProps = new Properties();

	public Properties getIUProperties(Feature feature) {
		return metadataProps;
	}

	public Properties getArtifactProperties(Feature feature) {
		return artifactProps;
	}

	public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
		return true;
	}

	public Properties getIUProperties(File location) {
		return metadataProps;
	}

	public Properties getArtifactProperties(File location) {
		return artifactProps;
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
}
