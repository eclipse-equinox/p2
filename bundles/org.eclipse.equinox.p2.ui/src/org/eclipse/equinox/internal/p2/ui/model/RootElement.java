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
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;

/**
 * Element class that represents the root of a viewer.  It can be configured
 * with its own policy and query context.
 * 
 * @since 3.5
 *
 */
public abstract class RootElement extends RemoteQueriedElement {

	private IUViewQueryContext queryContext;
	private Policy policy;

	public RootElement(Policy policy) {
		this(null, policy.getQueryContext(), policy);
	}

	public RootElement(IUViewQueryContext queryContext, Policy policy) {
		this(null, queryContext, policy);
	}

	/*
	 * Special method for subclasses that can sometimes be a root, and sometimes not.
	 */
	protected RootElement(Object parent, IUViewQueryContext queryContext, Policy policy) {
		super(parent);
		this.queryContext = queryContext;
		this.policy = policy;
	}

	/**
	 * Set the query context that is used when querying the receiver.
	 * 
	 * @param context the query context to use
	 */
	public void setQueryContext(IUViewQueryContext context) {
		queryContext = context;
	}

	public IUViewQueryContext getQueryContext() {
		return queryContext;
	}

	public Policy getPolicy() {
		return policy;
	}
}
