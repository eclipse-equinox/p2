/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.installer;

import java.io.*;
import java.net.URL;
import java.util.Properties;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.installer.InstallDescription;
import org.osgi.framework.Version;

/**
 * This class is responsible for loading install descriptions from various file formats.
 */
public class InstallDescriptionParser {
	private static final String PROP_ARTIFACT_REPOSITORY = "artifactRepository";
	private static final String PROP_INSTALL_LOCATION = "installLocation";
	private static final String PROP_IS_AUTO_START = "autoStart";
	private static final String PROP_LAUNCHER_NAME = "launcherName";
	private static final String PROP_METADATA_REPOSITORY = "metadataRepository";
	private static final String PROP_PROFILE_FLAVOR = "flavor";
	private static final String PROP_PROFILE_NAME = "profileName";
	private static final String PROP_ROOT_ID = "rootId";
	private static final String PROP_ROOT_VERSION = "rootVersion";

	/**
	 * Loads and returns an install description that is stored in a properties file.
	 * 
	 * @param stream The stream to load the install description from. The stream
	 * will be closed prior to this method returning, whether the description is
	 * read successfully or not.
	 */
	public static InstallDescription loadFromProperties(InputStream stream, SubMonitor monitor) throws IOException {
		BufferedInputStream in = null;
		try {
			Properties properties = new Properties();
			in = new BufferedInputStream(stream);
			properties.load(in);
			InstallDescription description = new InstallDescription(properties.getProperty(PROP_PROFILE_NAME));
			description.setArtifactRepository(new URL(properties.getProperty(PROP_ARTIFACT_REPOSITORY)));
			description.setMetadataRepository(new URL(properties.getProperty(PROP_METADATA_REPOSITORY)));
			description.setFlavor(properties.getProperty(PROP_PROFILE_FLAVOR));
			description.setAutoStart(Boolean.TRUE.toString().equalsIgnoreCase(properties.getProperty(PROP_IS_AUTO_START)));
			description.setLauncherName(properties.getProperty(PROP_LAUNCHER_NAME));
			String locationString = properties.getProperty(PROP_INSTALL_LOCATION);
			if (locationString != null)
				description.setInstallLocation(new Path(locationString));
			description.setRootId(properties.getProperty(PROP_ROOT_ID));
			String versionString = properties.getProperty(PROP_ROOT_VERSION);
			Version version = versionString == null ? null : new Version(versionString);
			description.setRootVersion(version);
			return description;
		} finally {
			safeClose(in);
		}
	}

	private static void safeClose(InputStream in) {
		try {
			if (in != null)
				in.close();
		} catch (IOException e) {
			//ignore secondary failure during close
		}
	}
}
