/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.policy;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.osgi.util.NLS;

/**
 * 
 * RepositoryLocationValidator can be used to validate a repository URL.  Validation may
 * involve rules known by the validator itself or contact with a repository
 * manager.
 * 
 * @since 3.4
 *
 */
public abstract class RepositoryLocationValidator {

	public static final int LOCAL_VALIDATION_ERROR = 3000;
	public static final int ALTERNATE_ACTION_TAKEN = 3001;

	public static IStatus getInvalidLocationStatus(String urlText) {
		return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, LOCAL_VALIDATION_ERROR, NLS.bind(ProvUIMessages.URLValidator_UnrecognizedURL, urlText), null);
	}

	public static URI locationFromString(String locationString) {
		URI userLocation;
		try {
			userLocation = URIUtil.fromString(locationString);
		} catch (URISyntaxException e) {
			return null;
		}
		// If a path separator char was used, interpret as a local file URI
		String uriString = URIUtil.toUnencodedString(userLocation);
		if (uriString.length() > 0 && (uriString.charAt(0) == '/' || uriString.charAt(0) == File.separatorChar))
			return RepositoryHelper.localRepoURIHelper(userLocation);
		return userLocation;
	}

	public abstract IStatus validateRepositoryLocation(URI url, boolean contactRepositories, IProgressMonitor monitor);
}
