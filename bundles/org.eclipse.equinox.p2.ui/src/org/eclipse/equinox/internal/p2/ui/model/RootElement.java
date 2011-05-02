/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * Element class that represents the root of a viewer.  It can be configured
 * with its own ui and query context.
 * 
 * @since 3.5
 *
 */
public abstract class RootElement extends RemoteQueriedElement {

	private IUViewQueryContext queryContext;
	private ProvisioningUI ui;

	public RootElement(ProvisioningUI ui) {
		this(null, ProvUI.getQueryContext(ui.getPolicy()), ui);
	}

	public RootElement(IUViewQueryContext queryContext, ProvisioningUI ui) {
		this(null, queryContext, ui);
	}

	/*
	 * Special method for subclasses that can sometimes be a root, and sometimes not.
	 */
	protected RootElement(Object parent, IUViewQueryContext queryContext, ProvisioningUI ui) {
		super(parent);
		this.queryContext = queryContext;
		this.ui = ui;
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
		return ui.getPolicy();
	}

	public ProvisioningUI getProvisioningUI() {
		return ui;
	}
}
