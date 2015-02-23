/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Fix compiler problems from generified IAdaptable#getAdapter
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;

/**
 * Element wrapper class for a artifact repository that gets its
 * contents in a deferred manner.
 * 
 * @since 3.4
 */
public class ArtifactRepositoryElement extends RemoteQueriedElement implements IRepositoryElement<IArtifactKey> {

	URI location;
	IArtifactRepository repo;
	boolean isEnabled;

	public ArtifactRepositoryElement(Object parent, URI location) {
		this(parent, location, true);
	}

	public ArtifactRepositoryElement(Object parent, URI location, boolean isEnabled) {
		super(parent);
		this.location = location;
		this.isEnabled = isEnabled;
	}

	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IArtifactRepository.class)
			return (T) getRepository(null);
		if (adapter == IRepository.class)
			return (T) getRepository(null);
		return super.getAdapter(adapter);
	}

	protected String getImageId(Object obj) {
		return ProvUIImages.IMG_ARTIFACT_REPOSITORY;
	}

	public String getLabel(Object o) {
		String name = getName();
		if (name != null && name.length() > 0) {
			return name;
		}
		return URIUtil.toUnencodedString(getLocation());
	}

	public IArtifactRepository getRepository(IProgressMonitor monitor) {
		if (repo == null)
			try {
				repo = getArtifactRepositoryManager().loadRepository(location, monitor);
			} catch (ProvisionException e) {
				getProvisioningUI().getRepositoryTracker().reportLoadFailure(location, e);
			} catch (OperationCanceledException e) {
				// nothing to report
			}
		return repo;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.RepositoryElement#getURL()
	 */
	public URI getLocation() {
		return location;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.RepositoryElement#getName()
	 */
	public String getName() {
		String name = getArtifactRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NICKNAME);
		if (name == null)
			name = getArtifactRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NAME);
		if (name == null)
			name = ""; //$NON-NLS-1$
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.RepositoryElement#getDescription()
	 */
	public String getDescription() {
		if (getProvisioningUI().getRepositoryTracker().hasNotFoundStatusBeenReported(location))
			return ProvUIMessages.RepositoryElement_NotFound;
		String description = getArtifactRepositoryManager().getRepositoryProperty(location, IRepository.PROP_DESCRIPTION);
		if (description == null)
			return ""; //$NON-NLS-1$
		return description;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.RepositoryElement#isEnabled()
	 */
	public boolean isEnabled() {
		return isEnabled;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.IRepositoryElement#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.QueriedElement#getDefaultQueryType()
	 */
	protected int getDefaultQueryType() {
		return QueryProvider.AVAILABLE_ARTIFACTS;
	}

	/*
	 * overridden to lazily fetch repository
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#getQueryable()
	 */
	public IQueryable<?> getQueryable() {
		if (queryable == null)
			queryable = getRepository(new NullProgressMonitor());
		return queryable;
	}

	IArtifactRepositoryManager getArtifactRepositoryManager() {
		return ProvUI.getArtifactRepositoryManager(getProvisioningUI().getSession());
	}
}
