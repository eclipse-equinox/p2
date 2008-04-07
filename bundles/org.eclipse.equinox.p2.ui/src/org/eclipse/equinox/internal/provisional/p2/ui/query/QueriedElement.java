/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.query;

import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;

/**
 * Element class that represents an element that gets its children
 * by using a query.
 * 
 * @since 3.4
 *
 */
public abstract class QueriedElement extends ProvElement {

	IQueryProvider queryProvider;
	protected IQueryable queryable;
	protected QueryContext queryContext = null;

	protected QueriedElement(QueryContext queryContext) {
		this.queryContext = queryContext;
	}

	public Object[] getChildren(Object o) {
		if (queryProvider == null)
			return new Object[0];
		ElementQueryDescriptor queryDescriptor = getQueryProvider().getQueryDescriptor(this, getQueryType());
		if (queryDescriptor == null)
			return new Object[0];
		queryDescriptor.queryable.query(queryDescriptor.query, queryDescriptor.collector, null);
		return queryDescriptor.collector.toArray(Object.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.ProvElement#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		return null;
	}

	protected abstract int getDefaultQueryType();

	protected int getQueryType() {
		if (queryContext == null)
			return getDefaultQueryType();
		return queryContext.getQueryType();
	}

	public void setQueryProvider(IQueryProvider queryProvider) {
		this.queryProvider = queryProvider;
	}

	public IQueryProvider getQueryProvider() {
		return queryProvider;
	}

	public void setQueryable(IQueryable queryable) {
		this.queryable = queryable;
	}

	public QueryContext getQueryContext() {
		return queryContext;
	}

	public void setQueryContext(QueryContext context) {
		queryContext = context;
	}

	public IQueryable getQueryable() {
		return queryable;
	}

	public boolean knowsQueryable() {
		return queryable != null;
	}

}
