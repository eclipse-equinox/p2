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
	private static final String PROP_ROOTS = "eclipse.p2.roots";//$NON-NLS-1$

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

			// Process the retro root id and rootVersion properties
			String id = properties.getProperty(PROP_ROOT_ID);
			if (id != null) {
				String version = properties.getProperty(PROP_ROOT_VERSION);
				description.setRoots(new VersionedName[] {new VersionedName(id, version)});
			}

			String rootSpec = properties.getProperty(PROP_ROOTS);
			if (rootSpec != null) {
				String[] rootList = getArrayFromString(rootSpec, ",");
				VersionedName[] roots = new VersionedName[rootList.length];
				for (int i = 0; i < rootList.length; i++)
					roots[i] = VersionedName.parse(rootList[i]);
				description.setRoots(roots);
			}

			//any remaining properties are profile properties
			Map profileProperties = new HashMap(properties);
			profileProperties.remove(PROP_PROFILE_NAME);
			profileProperties.remove(PROP_ARTIFACT_REPOSITORY);
			profileProperties.remove(PROP_METADATA_REPOSITORY);
			profileProperties.remove(PROP_IS_AUTO_START);
			profileProperties.remove(PROP_LAUNCHER_NAME);
			profileProperties.remove(PROP_AGENT_LOCATION);
			profileProperties.remove(PROP_BUNDLE_LOCATION);
			profileProperties.remove(PROP_ROOT_ID);
			profileProperties.remove(PROP_ROOT_VERSION);
			profileProperties.remove(PROP_ROOTS);
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

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified.
	 */
	public static String[] getArrayFromString(String list, String separator) {
		if (list == null || list.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		List result = new ArrayList();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				result.add(token);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

}
