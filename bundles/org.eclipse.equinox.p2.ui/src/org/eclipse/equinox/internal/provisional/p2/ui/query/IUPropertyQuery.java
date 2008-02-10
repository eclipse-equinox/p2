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
package org.eclipse.equinox.internal.provisional.p2.ui.query;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.Query;

/**
 * A query that searches for {@link IInstallableUnit} instances that have
 * a property whose value matches the provided value.
 */
public class IUPropertyQuery extends Query {
	private String propertyName;
	private String propertyValue;

	/**
	 * Creates a new query on the given property name and value.
	 */
	public IUPropertyQuery(String propertyName, String propertyValue) {
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.query2.Query#isMatch(java.lang.Object)
	 */
	public boolean isMatch(Object object) {
		if (!(object instanceof IInstallableUnit))
			return false;
		IInstallableUnit candidate = (IInstallableUnit) object;
		String value = getProperty(candidate, propertyName);
		if (value != null && value.equals(propertyValue))
			return true;
		return false;
	}

	protected String getProperty(IInstallableUnit iu, String name) {
		return iu.getProperty(name);
	}
}
