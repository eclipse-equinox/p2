/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import java.util.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.provisional.p2.installer.InstallDescription;
import org.osgi.framework.Version;

/**
 * This class is responsible for loading install descriptions from a stream.
 */
public class InstallDescriptionParser {
	private static final String PROP_AGENT_LOCATION = "eclipse.p2.agentLocation"; //$NON-NLS-1$
	private static final String PROP_ARTIFACT_REPOSITORY = "eclipse.p2.artifacts";//$NON-NLS-1$
	private static final String PROP_BUNDLE_LOCATION = "eclipse.p2.bundleLocation";//$NON-NLS-1$
	private static final String PROP_INSTALL_LOCATION = "eclipse.p2.installLocation";//$NON-NLS-1$
	private static final String PROP_IS_AUTO_START = "eclipse.p2.autoStart";//$NON-NLS-1$
	private static final String PROP_LAUNCHER_NAME = "eclipse.p2.launcherName";//$NON-NLS-1$
	private static final String PROP_METADATA_REPOSITORY = "eclipse.p2.metadata";//$NON-NLS-1$
	private static final String PROP_PROFILE_NAME = "eclipse.p2.profileName";//$NON-NLS-1$
	private static final String PROP_ROOT_ID = "eclipse.p2.rootId";//$NON-NLS-1$
	private static final String PROP_ROOT_VERSION = "eclipse.p2.rootVersion";//$NON-NLS-1$

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
			description.setAutoStart(Boolean.TRUE.toString().equalsIgnoreCase(properties.getProperty(PROP_IS_AUTO_START)));
			description.setLauncherName(properties.getProperty(PROP_LAUNCHER_NAME));
			String locationString = properties.getProperty(PROP_INSTALL_LOCATION);
			if (locationString != null)
				description.setInstallLocation(new Path(locationString));
			locationString = properties.getProperty(PROP_AGENT_LOCATION);
			if (locationString != null)
				description.setAgentLocation(new Path(locationString));
			locationString = properties.getProperty(PROP_BUNDLE_LOCATION);
			if (locationString != null)
				description.setBundleLocation(new Path(locationString));
			description.setRootId(properties.getProperty(PROP_ROOT_ID));
			String versionString = properties.getProperty(PROP_ROOT_VERSION);
			Version version = versionString == null ? null : new Version(versionString);
			description.setRootVersion(version);

			//any remaining properties are profile properties
			Map profileProperties = new HashMap(properties);
			profileProperties.remove(PROP_PROFILE_NAME);
			profileProperties.remove(PROP_ARTIFACT_REPOSITORY);
			profileProperties.remove(PROP_METADATA_REPOSITORY);
			profileProperties.remove(PROP_IS_AUTO_START);
			profileProperties.remove(PROP_AGENT_LOCATION);
			profileProperties.remove(PROP_BUNDLE_LOCATION);
			profileProperties.remove(PROP_ROOT_ID);
			profileProperties.remove(PROP_ROOT_VERSION);
			description.setProfileProperties(profileProperties);
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
