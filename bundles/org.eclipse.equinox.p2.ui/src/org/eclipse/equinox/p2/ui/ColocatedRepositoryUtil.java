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

import java.net.URL;

/**
 * Utility methods for manipulating colocated repository URLS
 * 
 * @since 3.4
 */
public class ColocatedRepositoryUtil {

	public static URL makeArtifactRepositoryURL(URL url) {
		return url;
	}

	public static URL makeMetadataRepositoryURL(URL url) {
		return url;
	}

	// A colocated repository URL is one level above the metadata or artifact
	// repository URL.  For example, foo/servers/ instead of foo/servers/metadataRepository
	public static URL makeColocatedRepositoryURL(URL metadataURL) {
		return metadataURL;
	}
}
