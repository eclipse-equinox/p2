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

import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.query.Query;

/**
 * A query that searches a repository for all {@link IInstallableUnit} instances that 
 * meet any one of the given iu's capabilities.  This is used, for example, when
 * collecting all of the IU's mentioned by another IU, such as a category or group IU.
 */
public class AnyRequiredCapabilityQuery extends Query {
	private RequiredCapability[] requirements;

	/**
	 * Creates a new query for the capabilities of the given IU.
	 */
	public AnyRequiredCapabilityQuery(IInstallableUnit iu) {
		this.requirements = iu.getRequiredCapabilities();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.query2.Query#isMatch(java.lang.Object)
	 */
	public boolean isMatch(Object object) {
		if (!(object instanceof IInstallableUnit))
			return false;
		IInstallableUnit candidate = (IInstallableUnit) object;
		ProvidedCapability[] provides = candidate.getProvidedCapabilities();
		for (int i = 0; i < requirements.length; i++) {
			for (int j = 0; j < provides.length; j++) {
				if (provides[j].isSatisfiedBy(requirements[i])) {
					return true;
				}
			}
		}
		return false;
	}
}
