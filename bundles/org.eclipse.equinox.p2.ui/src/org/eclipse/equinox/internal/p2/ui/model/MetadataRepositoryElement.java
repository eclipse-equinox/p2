/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IRepositoryElement;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Element wrapper class for a metadata repository that gets its
 * contents in a deferred manner.  A metadata repository can be the root
 * (input) of a viewer, when the view is filtered by repo, or a child of
 * an input, when the view is showing many repos.  
 * 
 * @since 3.4
 */
public class MetadataRepositoryElement extends RootElement implements IRepositoryElement {

	URI location;
	boolean isEnabled;
	String name;

	public MetadataRepositoryElement(Object parent, URI location, boolean isEnabled) {
		this(parent, null, null, location, isEnabled);
	}

	public MetadataRepositoryElement(IUViewQueryContext queryContext, Policy policy, URI location, boolean isEnabled) {
		super(null, queryContext, policy);
		this.location = location;
		this.isEnabled = isEnabled;
	}

	private MetadataRepositoryElement(Object parent, IUViewQueryContext queryContext, Policy policy, URI location, boolean isEnabled) {
		super(parent, queryContext, policy);
		this.location = location;
		this.isEnabled = isEnabled;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IMetadataRepository.class)
			return getQueryable();
		if (adapter == IRepository.class)
			return getQueryable();
		return super.getAdapter(adapter);
	}

	protected Object[] fetchChildren(Object o, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, 200);
		// Ensure the repository is loaded using the monitor, so we respond to cancelation.
		// Otherwise, a non-loaded repository could be loaded in the query provider without a monitor.
		// If the load fails, return an explanation element.
		try {
			getMetadataRepository(sub.newChild(100));
			//only invoke super if we successfully loaded the repository
			return super.fetchChildren(o, sub.newChild(100));
		} catch (ProvisionException e) {
			ProvUI.reportLoadFailure(location, e.getStatus(), StatusManager.SHOW, getPolicy().getRepositoryManipulator());
			// TODO see https://bugs.eclipse.org/bugs/show_bug.cgi?id=276784
			return new Object[] {new EmptyElementExplanation(this, IStatus.ERROR, e.getLocalizedMessage(), "")}; //$NON-NLS-1$
		}
	}

	protected String getImageId(Object obj) {
		return ProvUIImages.IMG_METADATA_REPOSITORY;
	}

	protected int getDefaultQueryType() {
		return QueryProvider.AVAILABLE_IUS;
	}

	public String getLabel(Object o) {
		String n = getName();
		if (n != null && n.length() > 0) {
			return n;
		}
		return URIUtil.toUnencodedString(getLocation());
	}

	/*
	 * overridden to lazily fetch repository
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#getQueryable()
	 */
	public IQueryable getQueryable() {
		if (queryable == null)
			return (IQueryable) getRepository(new NullProgressMonitor());
		return queryable;
	}

	public IRepository getRepository(IProgressMonitor monitor) {
		try {
			return getMetadataRepository(monitor);
		} catch (ProvisionException e) {
			ProvUI.reportLoadFailure(location, e.getStatus(), StatusManager.SHOW, getPolicy().getRepositoryManipulator());
		}
		return null;
	}

	private IMetadataRepository getMetadataRepository(IProgressMonitor monitor) throws ProvisionException {
		if (queryable == null)
			queryable = ProvisioningUtil.loadMetadataRepository(location, monitor);
		return (IMetadataRepository) queryable;

	}

	/*
	 * overridden to check whether url is specified rather
	 * than loading the repo via getQueryable()
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#knowsQueryable()
	 */
	public boolean knowsQueryable() {
		return location != null;
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
		if (name == null) {
			try {
				name = ProvisioningUtil.getMetadataRepositoryProperty(location, IRepository.PROP_NICKNAME);
				if (name == null)
					name = ProvisioningUtil.getMetadataRepositoryProperty(location, IRepository.PROP_NAME);
				if (name == null)
					name = ""; //$NON-NLS-1$
			} catch (ProvisionException e) {
				name = ""; //$NON-NLS-1$
			}
		}
		return name;
	}

	public void setNickname(String name) {
		this.name = name;
	}

	public void setLocation(URI location) {
		this.location = location;
		setQueryable(null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.RepositoryElement#getDescription()
	 */
	public String getDescription() {
		if (ProvUI.hasNotFoundStatusBeenReported(location))
			return ProvUIMessages.MetadataRepositoryElement_NotFound;
		try {
			String description = ProvisioningUtil.getMetadataRepositoryProperty(location, IRepository.PROP_DESCRIPTION);
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

	/*
	 * Overridden to check whether a repository instance has already been loaded.
	 * This is necessary to prevent background loading of an already loaded repository
	 * by the DeferredTreeContentManager, which will add redundant children to the
	 * viewer.  
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=229069
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=226343
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#hasQueryable()
	 */
	public boolean hasQueryable() {
		if (queryable != null)
			return true;
		if (location == null)
			return false;
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null || !(manager instanceof MetadataRepositoryManager))
			return false;
		IMetadataRepository repo = ((MetadataRepositoryManager) manager).getRepository(location);
		if (repo == null)
			return false;
		queryable = repo;
		return true;
	}

	public Policy getPolicy() {
		Object parent = getParent(this);
		if (parent == null)
			return super.getPolicy();
		if (parent instanceof QueriedElement)
			return ((QueriedElement) parent).getPolicy();
		return Policy.getDefault();
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("Metadata Repository Element - "); //$NON-NLS-1$
		result.append(URIUtil.toUnencodedString(location));
		if (hasQueryable())
			result.append(" (loaded)"); //$NON-NLS-1$
		else
			result.append(" (not loaded)"); //$NON-NLS-1$
		return result.toString();
	}
}
