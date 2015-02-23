/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Fix compiler problems from generified IAdaptable#getAdapter
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * Element wrapper class for a metadata repository that gets its
 * contents in a deferred manner.  A metadata repository can be the root
 * (input) of a viewer, when the view is filtered by repo, or a child of
 * an input, when the view is showing many repos.  
 * 
 * @since 3.4
 */
public class MetadataRepositoryElement extends RootElement implements IRepositoryElement<IInstallableUnit> {

	URI location;
	boolean isEnabled;
	String name;
	Object[] cache;

	public MetadataRepositoryElement(Object parent, URI location, boolean isEnabled) {
		this(parent, null, null, location, isEnabled);
	}

	public MetadataRepositoryElement(IUViewQueryContext queryContext, ProvisioningUI ui, URI location, boolean isEnabled) {
		this(null, queryContext, ui, location, isEnabled);
	}

	public MetadataRepositoryElement(Object parent, IUViewQueryContext queryContext, ProvisioningUI ui, URI location, boolean isEnabled) {
		super(parent, queryContext, ui);
		this.location = location;
		this.isEnabled = isEnabled;
	}

	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IMetadataRepository.class)
			return (T) getQueryable();
		if (adapter == IRepository.class)
			return (T) getQueryable();
		return super.getAdapter(adapter);
	}

	protected Object[] fetchChildren(Object o, IProgressMonitor monitor) {
		if (cache != null)
			return cache;

		SubMonitor sub = SubMonitor.convert(monitor, 200);
		// Ensure the repository is loaded using the monitor, so we respond to cancelation.
		// Otherwise, a non-loaded repository could be loaded in the query provider without a monitor.
		// If the load fails, return an explanation element.
		try {
			getMetadataRepository(sub.newChild(100));
			//only invoke super if we successfully loaded the repository
			cache = super.fetchChildren(o, sub.newChild(100));
		} catch (ProvisionException e) {
			getProvisioningUI().getRepositoryTracker().reportLoadFailure(location, e);
			// TODO see https://bugs.eclipse.org/bugs/show_bug.cgi?id=276784
			cache = new Object[] {new EmptyElementExplanation(this, IStatus.ERROR, e.getLocalizedMessage(), "")}; //$NON-NLS-1$
		}
		return cache;
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
	public IQueryable<?> getQueryable() {
		if (queryable == null)
			queryable = getRepository(new NullProgressMonitor());
		return queryable;
	}

	public IMetadataRepository getRepository(IProgressMonitor monitor) {
		try {
			return getMetadataRepository(monitor);
		} catch (ProvisionException e) {
			getProvisioningUI().getRepositoryTracker().reportLoadFailure(location, e);
		} catch (OperationCanceledException e) {
			// nothing to report
		}
		return null;
	}

	private IMetadataRepository getMetadataRepository(IProgressMonitor monitor) throws ProvisionException {
		if (queryable == null) {
			queryable = getProvisioningUI().loadMetadataRepository(location, false, monitor);
		}
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
			name = getMetadataRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NICKNAME);
			if (name == null)
				name = getMetadataRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NAME);
			if (name == null)
				name = ""; //$NON-NLS-1$
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
		if (getProvisioningUI().getRepositoryTracker().hasNotFoundStatusBeenReported(location))
			return ProvUIMessages.RepositoryElement_NotFound;
		String description = getMetadataRepositoryManager().getRepositoryProperty(location, IRepository.PROP_DESCRIPTION);
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
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
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
		return getProvisioningUI().getPolicy();
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

	IMetadataRepositoryManager getMetadataRepositoryManager() {
		return ProvUI.getMetadataRepositoryManager(getProvisioningUI().getSession());
	}

	/**
	 * MetadataRepositoryElements can sometimes be roots and sometimes children.
	 * When they are roots the should have a ui set directly.  As children they should
	 * defer to the parent to get the ui.
	 */
	public ProvisioningUI getProvisioningUI() {
		ProvisioningUI ui = super.getProvisioningUI();
		if (ui != null)
			return ui;
		Object parent = getParent(this);
		if (parent != null && parent instanceof QueriedElement)
			return ((QueriedElement) parent).getProvisioningUI();
		// if all else fails get the global UI.  This should not really happen but 
		// we need to account for some possible historical cases.
		return ProvisioningUI.getDefaultUI();
	}
}
