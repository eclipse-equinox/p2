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
package org.eclipse.equinox.internal.p2.metadata.query;

import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.MatchQuery;

/**
 * A query that finds all IUs that are considered an "Update" of the 
 * specified IU.  
 */
public final class UpdateQuery extends MatchQuery<IInstallableUnit> {
	private IInstallableUnit updateFrom;

	public UpdateQuery(IInstallableUnit updateFrom) {
		this.updateFrom = updateFrom;
	}

	public boolean isMatch(IInstallableUnit candidate) {
		if (candidate instanceof IInstallableUnitPatch && !(updateFrom instanceof IInstallableUnitPatch)) {
			IInstallableUnitPatch potentialPatch = (IInstallableUnitPatch) candidate;
			IRequirement lifeCycle = potentialPatch.getLifeCycle();
			if (lifeCycle == null)
				return false;
			return updateFrom.satisfies(lifeCycle);
		}
		IUpdateDescriptor descriptor = candidate.getUpdateDescriptor();
		if (descriptor != null && descriptor.isUpdateOf(updateFrom)) {
			if (!updateFrom.getId().equals(candidate.getId()))
				return true;
			return updateFrom.getVersion().compareTo(candidate.getVersion()) < 0;
		}
		return false;
	}
}
