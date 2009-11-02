/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata.query;

import org.eclipse.equinox.internal.provisional.p2.metadata.*;

public class UpdateQuery extends MatchQuery {
	private IInstallableUnit updateFrom;

	public UpdateQuery(IInstallableUnit updateFrom) {
		this.updateFrom = updateFrom;
	}

	public boolean isMatch(Object obj) {
		if (!(obj instanceof IInstallableUnit))
			return false;
		if (obj instanceof IInstallableUnitPatch && !(updateFrom instanceof IInstallableUnitPatch)) {
			IInstallableUnitPatch potentialPatch = (IInstallableUnitPatch) obj;
			IRequiredCapability lifeCycle = potentialPatch.getLifeCycle();
			if (lifeCycle == null)
				return false;
			return updateFrom.satisfies(lifeCycle);
		}
		IInstallableUnit candidate = (IInstallableUnit) obj;
		IUpdateDescriptor descriptor = candidate.getUpdateDescriptor();
		if (descriptor != null && descriptor.isUpdateOf(updateFrom) && updateFrom.getVersion().compareTo(candidate.getVersion()) < 0)
			return true;
		return false;
	}
}
