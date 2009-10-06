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
package org.eclipse.equinox.internal.p2.ui.model;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IRepositoryElement;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

/**
 * Element wrapper class for a artifact repository that gets its
 * contents in a deferred manner.
 * 
 * @since 3.4
 */
public class ArtifactRepositoryElement extends ProvElement implements IDeferredWorkbenchAdapter, IRepositoryElement {

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

	public Object getAdapter(Class adapter) {
		if (adapter == IArtifactRepository.class)
			return getRepository(null);
		if (adapter == IRepository.class)
			return getRepository(null);
		return super.getAdapter(adapter);
	}

	protected String getImageId(Object obj) {
		return ProvUIImages.IMG_ARTIFACT_REPOSITORY;
	}

	protected Object[] fetchChildren(Object o, IProgressMonitor monitor) {
		IArtifactRepository repository = (IArtifactRepository) getRepository(monitor);
		if (repository == null)
			return new ArtifactElement[0];
		IArtifactKey[] keys = repository.getArtifactKeys();
		ArtifactElement[] elements = new ArtifactElement[keys.length];
		for (int i = 0; i < keys.length; i++) {
			elements[i] = new ArtifactElement(this, keys[i], repo);
		}
		return elements;
	}

	public String getLabel(Object o) {
		String name = getName();
		if (name != null && name.length() > 0) {
			return name;
		}
		return URIUtil.toUnencodedString(getLocation());
	}

	public IRepository getRepository(IProgressMonitor monitor) {
		if (repo == null)
			try {
				repo = ProvisioningUtil.loadArtifactRepository(location, monitor);
			} catch (ProvisionException e) {
				handleException(e, NLS.bind(ProvUIMessages.MetadataRepositoryElement_RepositoryLoadError, location));
			} catch (OperationCanceledException e) {
				// Nothing to report
			}
		return repo;
	}

	public ISchedulingRule getRule(Object object) {
		return null;
	}

	public boolean isContainer() {
		return true;
	}

	public void fetchDeferredChildren(Object o, IElementCollector collector, IProgressMonitor monitor) {
		collector.add(fetchChildren(o, monitor), monitor);
	}

	public Object[] getChildren(Object o) {
		return fetchChildren(o, null);
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
		try {
			String name = ProvisioningUtil.getArtifactRepositoryProperty(location, IRepository.PROP_NICKNAME);
			if (name == null)
				name = ProvisioningUtil.getArtifactRepositoryProperty(location, IRepository.PROP_NAME);
			if (name == null)
				name = ""; //$NON-NLS-1$
			return name;
		} catch (ProvisionException e) {
			return ""; //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.RepositoryElement#getDescription()
	 */
	public String getDescription() {
		try {
			String description = ProvisioningUtil.getArtifactRepositoryProperty(location, IRepository.PROP_DESCRIPTION);
			if (description == null)
				return ""; //$NON-NLS-1$
			return description;
		} catch (ProvisionException e) {
			return ""; //$NON-NLS-1$
		}
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
}
