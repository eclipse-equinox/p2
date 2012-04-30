/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
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
import org.eclipse.equinox.internal.p2.operations.*;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.osgi.util.NLS;

/**
 * RepositoryTracker defines a service that retrieves repositories, tracks their status, and
 * reports errors.
 * 
 * @since 2.0
 */
public abstract class RepositoryTracker {

	/**
	 * A status code used to indicate that a repository location was not valid.
	 */
	public static final int STATUS_INVALID_REPOSITORY_LOCATION = IStatusCodes.INVALID_REPOSITORY_LOCATION;

	// What repositories to show
	private int artifactRepositoryFlags = IRepositoryManager.REPOSITORIES_NON_SYSTEM;
	private int metadataRepositoryFlags = IRepositoryManager.REPOSITORIES_NON_SYSTEM;
	/**
	 * List<URI> of repositories that have already been reported to the user as not found.
	 */
	private final List<URI> reposNotFound = Collections.synchronizedList(new ArrayList<URI>());

	/**
	 * Return an array of repository locations known for the specified provisioning session.
	 * 
	 * @param session the provisioning session providing the provisioning services
	 * @return an array of repository locations known by this tracker
	 */
	public abstract URI[] getKnownRepositories(ProvisioningSession session);

	/**
	 * Return a status appropriate for reporting an invalid repository location.
	 * @param locationText the text representation of the location
	 * @return a status that describes an invalid location
	 */
	public IStatus getInvalidLocationStatus(String locationText) {
		return new Status(IStatus.ERROR, Activator.ID, IStatusCodes.INVALID_REPOSITORY_LOCATION, NLS.bind(Messages.RepositoryTracker_InvalidLocation, locationText), null);
	}

	/**
	 * Return a repository location represented by the supplied string. The provided
	 * string should either be an unencoded string representation of a URI, or a
	 * local file system path. This method is generally suitable for converting a 
	 * location string entered by an end user into a suitable URI representation.
	 * 
	 * @param locationString a text representation of the location
	 * @return a repository location URI, or <code>null</code> if the
	 * text could not be interpreted.
	 */
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

	/**
	 * Validate the specified repository location.
	 * 
	 * @param session the provisioning session providing the repository services
	 * @param location the location in question
	 * @param contactRepositories <code>true</code> if the appropriate repository manager(s) should be
	 * consulted regarding the validity of the location, or <code>false</code> if the repository manager
	 * should not be consulted.
	 * @param monitor the progress monitor
	 * @return a status indicating the current status of the repository
	 */

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

		return Status.OK_STATUS;
	}

	/**
	 * Return a boolean indicating whether this tracker already contains the specified
	 * repository location.
	 * 
	 * @param location the location in question
	 * @param session the provisioning session providing the repository services
	 * @since 2.1
	 */
	protected boolean contains(URI location, ProvisioningSession session) {
		// This is a fallback implementation in the absence of a repository tracker
		// that would know what to do.
		URI[] knownRepositories = getKnownRepositories(session);
		for (int i = 0; i < knownRepositories.length; i++) {
			if (URIUtil.sameURI(knownRepositories[i], location)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add the specified location to the list of "not found" repositories.
	 * This list is used to ensure that errors are not reported multiple times
	 * for the same repository.
	 * 
	 * The caller is already assumed to have reported any errors if necessary.
	 * 
	 * @param location the location of the repository that cannot be found
	 */
	public void addNotFound(URI location) {
		reposNotFound.add(location);
	}

	/**
	 * Answer a boolean indicating whether not found status has already been
	 * reported for the specified location.
	 * 
	 * @param location the location in question
	 * @return <code>true</code> if the repository has already been reported as
	 * being not found, <code>false</code> if no status has been reported for this
	 * location.
	 */
	public boolean hasNotFoundStatusBeenReported(URI location) {
		// We don't check for things like case variants or end slash variants
		// because we know that the repository managers already did this.
		return reposNotFound.contains(location);
	}

	/**
	 * Clear the list of repositories that have already been reported as not found.
	 */
	public void clearRepositoriesNotFound() {
		reposNotFound.clear();
	}

	/**
	 * Remove the specified repository from the list of repositories that
	 * have already been reported as not found.  This method has no effect
	 * if the repository has never been reported as not found.
	 * 
	 * @param location the location in question
	 */
	public void clearRepositoryNotFound(URI location) {
		reposNotFound.remove(location);
	}

	/**
	 * Return the repository flags suitable for retrieving known repositories from 
	 * a repository manager
	 * 
	 * @return the repository flags
	 * 
	 */
	public int getArtifactRepositoryFlags() {
		return artifactRepositoryFlags;
	}

	/**
	 * Set the repository flags suitable for retrieving known repositories from 
	 * a repository manager
	 * 
	 * @param flags the repository flags
	 * 
	 */
	public void setArtifactRepositoryFlags(int flags) {
		artifactRepositoryFlags = flags;
	}

	/**
	 * Return the repository flags suitable for retrieving known repositories from 
	 * a repository manager
	 * 
	 * @return the repository flags
	 * 
	 */
	public int getMetadataRepositoryFlags() {
		return metadataRepositoryFlags;
	}

	/**
	 * Set the repository flags suitable for retrieving known repositories from 
	 * a repository manager
	 * 
	 * @param flags the repository flags
	 * 
	 */

	public void setMetadataRepositoryFlags(int flags) {
		metadataRepositoryFlags = flags;
	}

	/**
	 * Report a failure to load the specified repository.
	 * <p>
	 * This default implementation simply logs the failure. Subclasses may override
	 * to provide additional error reporting.
	 * </p>
	 * @param location the location of the failed repository
	 * @param exception the failure that occurred
	 */
	public void reportLoadFailure(final URI location, ProvisionException exception) {
		// special handling when the repo location is bad.  We don't want to continually report it
		int code = exception.getStatus().getCode();
		if (code == IStatusCodes.INVALID_REPOSITORY_LOCATION || code == ProvisionException.REPOSITORY_INVALID_LOCATION || code == ProvisionException.REPOSITORY_NOT_FOUND) {
			if (hasNotFoundStatusBeenReported(location))
				return;
			addNotFound(location);
		}

		LogHelper.log(exception.getStatus());
	}

	/**
	 * Add a repository at the specified location.
	 *
	 * @param location the location of the new repository
	 * @param nickname the nickname for the repository, or <code>null</code> if there is no nickname
	 * @param session the session to use for provisioning services
	 */
	public abstract void addRepository(URI location, String nickname, ProvisioningSession session);

	/**
	 * Remove the repositories at the specified locations
	 *
	 * @param locations the locations
	 * @param session the session to use for provisioning services
	 */
	public abstract void removeRepositories(URI[] locations, ProvisioningSession session);

	/**
	 * Refresh the repositories at the specified locations
	 * @param locations the locations
	 * @param session the session to use for provisioning services
	 * @param monitor the progress monitor to use
	 */
	public abstract void refreshRepositories(URI[] locations, ProvisioningSession session, IProgressMonitor monitor);
}
