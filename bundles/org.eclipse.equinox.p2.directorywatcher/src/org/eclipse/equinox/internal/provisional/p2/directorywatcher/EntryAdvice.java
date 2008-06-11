package org.eclipse.equinox.internal.provisional.p2.directorywatcher;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Properties;
import org.eclipse.equinox.internal.p2.publisher.actions.IBundleAdvice;
import org.eclipse.equinox.internal.p2.publisher.actions.IFeatureAdvice;
import org.eclipse.equinox.internal.p2.publisher.features.Feature;
import org.osgi.framework.Version;

public class EntryAdvice implements IFeatureAdvice, IBundleAdvice {

	public Properties getProperties(Feature feature, File location) {
		if (location == null) {
			String path = feature.getLocation();
			if (path != null)
				location = new File(path);
		}
		if (location == null)
			return null;
		Properties props = new Properties();
		props.setProperty(RepositoryListener.FILE_NAME, location.getAbsolutePath());
		props.setProperty(RepositoryListener.FILE_LAST_MODIFIED, Long.toString(location.lastModified()));
		return props;
	}

	public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
		return true;
	}

	public Properties getProperties(File location) {
		Properties result = new Properties();
		try {
			result.put(RepositoryListener.ARTIFACT_REFERENCE, location.toURL().toExternalForm());
		} catch (MalformedURLException e) {
			// never happens
		}
		if (location.isDirectory())
			result.put(RepositoryListener.ARTIFACT_FOLDER, Boolean.TRUE.toString());

		result.put(RepositoryListener.FILE_NAME, location.getAbsolutePath());
		result.put(RepositoryListener.FILE_LAST_MODIFIED, Long.toString(location.lastModified()));
		return result;
	}
}
