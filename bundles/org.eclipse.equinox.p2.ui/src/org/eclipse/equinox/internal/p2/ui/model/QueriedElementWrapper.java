/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.ElementWrapper;

/**
 * A wrapper that assigns a query provider and the queryable
 * who was performing the query to the wrapped elements
 * as they are accepted.
 * 
 * @since 3.4
 */
public abstract class QueriedElementWrapper extends ElementWrapper {

	protected IQueryable queryable;
	protected Object parent;

	public QueriedElementWrapper(IQueryable queryable, Object parent) {
		this.queryable = queryable;
		this.parent = parent;
	}

	/**
	 * Sets an item as Queryable if it is a QueriedElement
	 */
	protected Object wrap(Object item) {
		if (item instanceof QueriedElement) {
			QueriedElement element = (QueriedElement) item;
			if (!element.knowsQueryable()) {
				element.setQueryable(queryable);
			}
		}
		return item;
	}
}
