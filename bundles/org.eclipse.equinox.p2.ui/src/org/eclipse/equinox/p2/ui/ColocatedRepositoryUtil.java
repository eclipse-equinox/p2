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

package org.eclipse.equinox.p2.ui;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.Path;

/**
 * Utility methods for manipulating colocated repository URLS
 * 
 * @since 3.4
 */
public class ColocatedRepositoryUtil {
	private static final String DEFAULT_ARTIFACTREPO_NAME = "artifactRepository/"; //$NON-NLS-1$
	private static final String DEFAULT_METADATAREPO_NAME = "metadataRepository/"; //$NON-NLS-1$

	public static URL makeArtifactRepositoryURL(URL url) {
		String urlSpec = url.toExternalForm();
		URL newURL;
		try {
			if (!urlSpec.endsWith("/")) //$NON-NLS-1$
				urlSpec += "/"; //$NON-NLS-1$
			newURL = new URL(urlSpec + DEFAULT_ARTIFACTREPO_NAME);
		} catch (MalformedURLException e) {
			return null;
		}
		return newURL;
	}

	public static URL makeMetadataRepositoryURL(URL url) {
		String urlSpec = url.toExternalForm();
		URL newURL;
		try {
			if (!urlSpec.endsWith("/")) //$NON-NLS-1$
				urlSpec += "/"; //$NON-NLS-1$
			newURL = new URL(urlSpec + DEFAULT_METADATAREPO_NAME);
		} catch (MalformedURLException e) {
			return null;
		}
		return newURL;
	}

	// A colocated repository URL is one level above the metadata or artifact
	// repository URL.  For example, foo/servers/ instead of foo/servers/metadataRepository
	public static URL makeColocatedRepositoryURL(URL metadataURL) {
		String protocol = metadataURL.getProtocol();
		String host = metadataURL.getHost();
		int port = metadataURL.getPort();
		Path path = new Path(metadataURL.getPath());
		String pathString = path.removeLastSegments(1).toString();
		try {
			return new URL(protocol, host, port, pathString);
		} catch (MalformedURLException e) {
			return metadataURL;
		}
	}
}
