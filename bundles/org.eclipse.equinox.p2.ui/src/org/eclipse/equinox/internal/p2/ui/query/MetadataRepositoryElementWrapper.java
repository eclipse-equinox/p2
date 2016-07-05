/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.query;

import java.net.URI;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * ElementWrapper that accepts the matched repo URLs and
 * wraps them in a MetadataRepositoryElement.
 * 
 * @since 3.4
 */
public class MetadataRepositoryElementWrapper extends QueriedElementWrapper {

	public MetadataRepositoryElementWrapper(IQueryable<URI> queryable, Object parent) {
		super(queryable, parent);
	}

	/**
	 * Accepts a result that matches the query criteria.
	 * 
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	protected boolean shouldWrap(Object match) {
		if ((match instanceof URI))
			return true;
		return false;
	}

	/**
	 * Transforms the item to a UI element
	 */
	protected Object wrap(Object item) {
		// Assume the item is enabled

		// if the parent is a queried element then use its provisioning UI to find out about enablement
		if (parent instanceof QueriedElement) {
			QueriedElement qe = (QueriedElement) parent;
			ProvisioningUI provisioningUI = qe.getProvisioningUI();
			ProvisioningSession session = provisioningUI.getSession();
			boolean enabled = ProvUI.getMetadataRepositoryManager(session).isEnabled((URI) item);
			return super.wrap(new MetadataRepositoryElement(parent, qe.getQueryContext(), provisioningUI, (URI) item, enabled));
		}
		return super.wrap(new MetadataRepositoryElement(parent, (URI) item, true));
	}

}
