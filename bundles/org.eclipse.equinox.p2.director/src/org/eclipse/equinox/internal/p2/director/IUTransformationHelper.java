/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.osgi.service.resolver.VersionRange;

public class IUTransformationHelper {
	static public RequiredCapability[] toRequirements(Iterator ius, boolean optional) {
		ArrayList result = new ArrayList();
		while (ius.hasNext()) {
			IInstallableUnit current = (IInstallableUnit) ius.next();
			result.add(new RequiredCapability(IInstallableUnit.NAMESPACE_IU, current.getId(), new VersionRange(current.getVersion(), true, current.getVersion(), true), null, optional, false));
		}
		return (RequiredCapability[]) result.toArray(new RequiredCapability[result.size()]);
	}

	static public RequiredCapability[] toRequirements(IInstallableUnit[] ius, boolean optional) {
		RequiredCapability[] result = new RequiredCapability[ius.length];
		for (int i = 0; i < result.length; i++) {
			IInstallableUnit current = ius[i];
			result[i] = new RequiredCapability(IInstallableUnit.NAMESPACE_IU, current.getId(), new VersionRange(current.getVersion(), true, current.getVersion(), true), null, optional, false);
		}
		return result;
	}
}
