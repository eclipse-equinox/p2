/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.query;

import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.query.Query;

/**
 * A query that searches for {@link IRepository} instances that have
 * a property whose value matches the provided value.
 */
public class RepositoryPropertyQuery extends Query {
	private String propertyName;
	private Object propertyValue;
	private boolean mustMatch;

	/**
	 * Creates a new query on the given property name and value.
	 * The boolean indicates whether the property value should match or 
	 * should not match in order to satisfy the query.
	 */
	public RepositoryPropertyQuery(String propertyName, Object propertyValue, boolean match) {
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
		this.mustMatch = match;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.query2.Query#isMatch(java.lang.Object)
	 */
	public boolean isMatch(Object object) {
		if (!(object instanceof IRepository))
			return false;
		IRepository candidate = (IRepository) object;
		Object value = candidate.getProperties().get(propertyName);
		if (value != null && value.equals(propertyValue))
			return mustMatch;
		return !mustMatch;
	}
}
