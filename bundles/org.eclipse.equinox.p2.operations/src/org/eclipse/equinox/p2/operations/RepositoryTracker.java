/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.operations.Activator;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.osgi.util.NLS;

/**
 * Abstract class for a mechanism that tracks the repositories and reports
 * on their status.
 * 
 * @since 2.0
 * 
 */

public abstract class RepositoryTracker {

	// What repositories to show
	private int artifactRepositoryFlags = IRepositoryManager.REPOSITORIES_NON_SYSTEM;
	private int metadataRepositoryFlags = IRepositoryManager.REPOSITORIES_NON_SYSTEM;
	/**
	 * List<URI> of repositories that have already been reported to the user as not found.
	 */
	private final List reposNotFound = Collections.synchronizedList(new ArrayList());

	/**
	 * Return an array of URLs containing the repositories already known.
	 */
	public abstract URI[] getKnownRepositories(ProvisioningSession session);

	public IStatus getInvalidLocationStatus(String urlText) {
		return new Status(IStatus.ERROR, Activator.ID, IStatusCodes.INVALID_REPOSITORY_LOCATION, NLS.bind(Messages.RepositoryTracker_InvalidLocation, urlText), null);
	}

	public URI locationFromString(String locationString) {
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

	public IStatus validateRepositoryLocation(ProvisioningSession session, URI location, boolean contactRepositories, IProgressMonitor monitor) {
		// First validate syntax issues
		IStatus localValidationStatus = RepositoryHelper.checkRepositoryLocationSyntax(location);
		if (!localValidationStatus.isOK()) {
			// bad syntax, but it could just be non-absolute.
			// In this case, use the helper
			String locationString = URIUtil.toUnencodedString(location);
			if (locationString.length() > 0 && (locationString.charAt(0) == '/' || locationString.charAt(0) == File.separatorChar)) {
				location = RepositoryHelper.localRepoURIHelper(location);
				localValidationStatus = RepositoryHelper.checkRepositoryLocationSyntax(location);
			}
		}

		if (!localValidationStatus.isOK())
			return localValidationStatus;

		// Syntax was ok, now look for duplicates
		URI[] knownRepositories = getKnownRepositories(session);
		for (int i = 0; i < knownRepositories.length; i++) {
			if (URIUtil.sameURI(knownRepositories[i], location)) {
				localValidationStatus = new Status(IStatus.ERROR, Activator.ID, IStatusCodes.INVALID_REPOSITORY_LOCATION, Messages.RepositoryTracker_DuplicateLocation, null);
				break;
			}
		}

		if (!localValidationStatus.isOK())
			return localValidationStatus;

		if (contactRepositories)
			return validateRepositoryLocationWithManager(session, location, monitor);

		return localValidationStatus;
	}

	protected abstract IStatus validateRepositoryLocationWithManager(ProvisioningSession session, URI location, IProgressMonitor monitor);

	// This assumes that callers already checked whether it *should*
	// be reported so that we don't need to loop through the list
	// when the caller just has done so in order to know whether to report.
	public void addNotFound(URI location) {
		reposNotFound.add(location);
	}

	// We don't check for things like case variants or end slash variants
	// because we know that the repository managers already did this.
	public boolean hasNotFoundStatusBeenReported(URI location) {
		return reposNotFound.contains(location);
	}

	public void clearRepositoriesNotFound() {
		reposNotFound.clear();
	}

	public void clearRepositoryNotFound(URI location) {
		reposNotFound.remove(location);
	}

	public int getArtifactRepositoryFlags() {
		return artifactRepositoryFlags;
	}

	public void setArtifactRepositoryFlags(int flags) {
		artifactRepositoryFlags = flags;
	}

	public int getMetadataRepositoryFlags() {
		return metadataRepositoryFlags;
	}

	public void setMetadataRepositoryFlags(int flags) {
		metadataRepositoryFlags = flags;
	}

	public void reportLoadFailure(final URI location, IStatus status) {
		LogHelper.log(status);
	}
}
