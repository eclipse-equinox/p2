/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ElementQueryDescriptor;
import org.eclipse.equinox.internal.p2.ui.QueryProvider;
import org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * Element class that represents an element that gets its children
 * by using a query.
 * 
 * @since 3.4
 *
 */
public abstract class QueriedElement extends ProvElement {

	protected IQueryable<?> queryable;
	// This cache is used internally to facilitate child elements
	// that want to eliminate duplicates from the parent hierarchy.
	// This cache is *not* used as a general purpose child cache.
	private Collection<?> cachedChildren;
	private static final Object[] EMPTY= new Object[0];
	
	protected QueriedElement(Object parent) {
		super(parent);
	}

	public Policy getPolicy() {
		Object parent = getParent(this);
		if (parent instanceof QueriedElement)
			return ((QueriedElement) parent).getPolicy();
		return getProvisioningUI().getPolicy();
	}

	public ProvisioningUI getProvisioningUI() {
		Object parent = getParent(this);
		if (parent instanceof QueriedElement)
			return ((QueriedElement) parent).getProvisioningUI();
		// if we really can't find a UI then get the default. In general this should
		// not happen though.  Turn on tracing in getDefaultUI() to see places where 
		// it is happening.
		return ProvisioningUI.getDefaultUI();

	}

	public IUViewQueryContext getQueryContext() {
		Object parent = getParent(this);
		if (parent instanceof QueriedElement)
			return ((QueriedElement) parent).getQueryContext();
		return null;
	}

	public Object[] getChildren(Object o) {
		return fetchChildren(o, new NullProgressMonitor());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
		return null;
	}

	/**
	 * Return the query type that is appropriate for this element when there
	 * is no query context.
	 * @return  The integer query type
	 */
	protected abstract int getDefaultQueryType();

	/**
	 * Return the query type that should be used for this element.
	 * Depending on the element, the query type may take the query context
	 * into account.  Subclasses should override this method if there are
	 * context-dependent decisions to be made to determine the query.
	 * @return The integer query type
	 */
	public int getQueryType() {
		return getDefaultQueryType();
	}

	protected Object[] fetchChildren(Object o, IProgressMonitor monitor) {
		cachedChildren = Collections.emptyList();
		QueryProvider provider = new QueryProvider(getProvisioningUI());
		ElementQueryDescriptor queryDescriptor = provider.getQueryDescriptor(this);
		if (queryDescriptor == null)
			return new Object[0];
		Collection<?> results = queryDescriptor.performQuery(monitor);
		cachedChildren = Collections.unmodifiableCollection(results);
		if (results.size() > 0) {
			Collection<Object> returnedChildren = new HashSet<Object>();
			returnedChildren.addAll(results);
			Object[] siblings = getSiblings();
			for (int i = 0; i < siblings.length; i++) {
				returnedChildren.remove(siblings[i]);
			}
			return returnedChildren.toArray();
		}
		return new Object[0];
	}

	public void setQueryable(IQueryable<?> queryable) {
		this.queryable = queryable;
	}

	public IQueryable<?> getQueryable() {
		return queryable;
	}

	/**
	 * Return a boolean indicating whether the receiver
	 * has enough information to get its queryable.  This is used in lieu
	 * of {{@link #getQueryable()} when lazy initialization
	 * of the queryable is not desired, and a client wishes
	 * to know whether the queryable could be obtained.  Subclasses
	 * that cache information needed to retrieve the queryable rather
	 * than the queryable itself should
	 * override this. 
	 * 
	 * @return <code>true</code> if the receiver has enough
	 * information to retrieve its queryable, <code>false</code> 
	 * if it does not.
	 */
	public boolean knowsQueryable() {
		return queryable != null;
	}

	/**
	 * Return a boolean indicating whether the receiver
	 * actually has its queryable.  This is used in lieu
	 * of {{@link #getQueryable()} when lazy initialization
	 * of the queryable is not desired.  For example, when
	 * working with an element whose queryable may be 
	 * expensive to obtain, clients may check this before
	 * actually getting the queryable.  Subclasses
	 * should typically not need to override this.
	 * 
	 * @return <code>true</code> if the receiver has its
	 * queryable, <code>false</code> if it does not yet.
	 */
	public boolean hasQueryable() {
		return queryable != null;
	}

	public Object[] getCachedChildren() {
		if (cachedChildren == null)
			return EMPTY;
		return cachedChildren.toArray();
	}

	protected Object[] getSiblings() {
		Object parent = getParent(this);
		if (parent instanceof QueriedElement)
			return ((QueriedElement) parent).getCachedChildren();
		if (parent instanceof IUElementListRoot)
			return ((IUElementListRoot) parent).getChildren(parent);
		return new Object[0];
	}
}
