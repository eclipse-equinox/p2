/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.osgi.util.NLS;

/**
 * 
 * URLValidator can be used to validate a repository URL.  Validation may
 * involve rules known by the validator itself or contact with a repository
 * manager.
 * 
 * @since 3.4
 *
 */
public abstract class URLValidator {
	public static final String FILE_PROTOCOL = "file"; //$NON-NLS-1$
	public static final String FILE_PROTOCOL_PREFIX = "file:"; //$NON-NLS-1$
	public static final String JAR_PATH_PREFIX = "jar:";//$NON-NLS-1$
	public static final String JAR_PATH_SUFFIX = "!/"; //$NON-NLS-1$
	public static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	public static final String ZIP_EXTENSION = ".zip"; //$NON-NLS-1$

	public static final int LOCAL_VALIDATION_ERROR = 3000;
	public static final int REPO_AUTO_GENERATED = 3001;
	public static final int ALTERNATE_ACTION_TAKEN = 3002;

	public static Status getInvalidURLStatus(String urlText) {
		return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, LOCAL_VALIDATION_ERROR, NLS.bind(ProvUIMessages.URLValidator_UnrecognizedURL, urlText), null);
	}

	public static boolean isFileURL(URL url) {
		return url.getProtocol().equals(FILE_PROTOCOL);
	}

	public static String makeJarURLString(String path) {
		String lowerCase = path.toLowerCase();
		if (lowerCase.endsWith(JAR_EXTENSION) || lowerCase.endsWith(ZIP_EXTENSION))
			return JAR_PATH_PREFIX + FILE_PROTOCOL_PREFIX + path + JAR_PATH_SUFFIX;
		return makeFileURLString(path);
	}

	public static String makeFileURLString(String path) {
		return FILE_PROTOCOL_PREFIX + '/' + path;
	}

	protected abstract IStatus validateRepositoryURL(URL url, boolean contactRepositories, IProgressMonitor monitor);
}
