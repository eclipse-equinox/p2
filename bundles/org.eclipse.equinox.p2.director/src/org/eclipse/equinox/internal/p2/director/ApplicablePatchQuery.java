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

public class ApplicablePatchQuery extends Query {

	public static final Query ANY = new Query() {
		public boolean isMatch(Object candidate) {
			return candidate instanceof IInstallableUnitPatch;
		}
	};

	IInstallableUnit iu;

	public ApplicablePatchQuery(IInstallableUnit iu) {
		this.iu = iu;
	}

	public boolean isMatch(Object candidate) {
		if (!(candidate instanceof IInstallableUnitPatch))
			return false;
		IInstallableUnitPatch patchIU = (IInstallableUnitPatch) candidate;
		RequiredCapability[][] scopeDescription = patchIU.getApplicabilityScope();
		if (scopeDescription == null)
			return false;
		if (scopeDescription.length == 0)
			return true;

		ProvidedCapability[] cap = iu.getProvidedCapabilities();
		for (int i = 0; i < scopeDescription.length; i++) {
			int matchedScopeEntry = scopeDescription[i].length;
			for (int j = 0; j < scopeDescription[i].length; j++) {
				for (int k = 0; k < cap.length; k++) {
					if (cap[k].isSatisfiedBy(scopeDescription[i][j])) {
						matchedScopeEntry--;
						break;
					}
				}
			}
			if (matchedScopeEntry == 0)
				return true;
		}
		return false;
	}
}
