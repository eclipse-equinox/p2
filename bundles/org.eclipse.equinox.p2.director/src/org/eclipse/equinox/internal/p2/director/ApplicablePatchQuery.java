/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.query.Query;

/**
 * A query that accepts any patch that applies to a given installable unit.
 */
public class ApplicablePatchQuery extends Query {

	/**
	 * A query that matches any patch.
	 */
	public static final Query ANY = new Query() {
		public boolean isMatch(Object candidate) {
			return candidate instanceof IInstallableUnitPatch;
		}
	};

	IInstallableUnit iu;

	/**
	 * Creates a new patch query on the given installable unit. Patches that can
	 * be applied to this unit will be accepted as matches by the query.
	 * @param iu The unit to compare patches against
	 */
	public ApplicablePatchQuery(IInstallableUnit iu) {
		this.iu = iu;
	}

	public boolean isMatch(Object candidate) {
		if (!(candidate instanceof IInstallableUnitPatch))
			return false;
		IInstallableUnitPatch patchIU = (IInstallableUnitPatch) candidate;
		IRequiredCapability[][] scopeDescription = patchIU.getApplicabilityScope();
		if (scopeDescription.length == 0)
			return true;

		for (int i = 0; i < scopeDescription.length; i++) {
			int matchedScopeEntry = scopeDescription[i].length;
			for (int j = 0; j < scopeDescription[i].length; j++) {
				if (iu.satisfies(scopeDescription[i][j]))
					matchedScopeEntry--;
			}
			if (matchedScopeEntry == 0)
				return true;
		}
		return false;
	}
}
