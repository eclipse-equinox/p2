/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.query;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery;

/**
 * A query that searches a repository for all {@link IInstallableUnit} instances that 
 * meet any one of the given capabilities.  
 */
public class AnyRequiredCapabilityQuery extends MatchQuery {
	private IRequiredCapability[] requirements;

	/**
	 * Creates a new query for the capabilities of the given IU.
	 */
	public AnyRequiredCapabilityQuery(IRequiredCapability[] requirements) {
		this.requirements = requirements;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.query2.Query#isMatch(java.lang.Object)
	 */
	public boolean isMatch(Object object) {
		if (!(object instanceof IInstallableUnit))
			return false;
		IInstallableUnit candidate = (IInstallableUnit) object;
		for (int i = 0; i < requirements.length; i++)
			if (candidate.satisfies(requirements[i]))
				return true;
		return false;
	}
}
