/*******************************************************************************
 *Copyright (c) 2008 IBM Corporation and others.
 *All rights reserved. This program and the accompanying materials
 *are made available under the terms of the Eclipse Public License v1.0
 *which accompanies this distribution, and is available at
 *http://www.eclipse.org/legal/epl-v10.html
 *
 *Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.query.Query;

public class PatchingIU extends Query {

	public PatchingIU(RequiredCapability req) {
	}

	public boolean isMatch(Object candidate) {
		if (!(candidate instanceof IInstallableUnit))
			return false;

		IInstallableUnit iu = (IInstallableUnit) candidate;
		if (iu.getProperty("patch").equals("true"))
			return true;
		return hasApplicablePatch(iu);
	}

	private boolean hasApplicablePatch(IInstallableUnit iu) {
		//iu.getPatches
		return true;
	}
}
