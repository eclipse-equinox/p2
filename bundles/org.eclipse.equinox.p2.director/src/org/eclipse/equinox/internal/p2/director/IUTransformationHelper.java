/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;

public class IUTransformationHelper {
	static public IRequiredCapability[] toRequirements(Iterator ius, boolean optional) {
		ArrayList result = new ArrayList();
		while (ius.hasNext()) {
			IInstallableUnit current = (IInstallableUnit) ius.next();
			result.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, current.getId(), new VersionRange(current.getVersion(), true, current.getVersion(), true), null, optional, false));
		}
		return (IRequiredCapability[]) result.toArray(new IRequiredCapability[result.size()]);
	}

	static public IRequiredCapability[] toRequirements(IInstallableUnit[] ius, boolean optional) {
		IRequiredCapability[] result = new IRequiredCapability[ius.length];
		for (int i = 0; i < result.length; i++) {
			IInstallableUnit current = ius[i];
			result[i] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, current.getId(), new VersionRange(current.getVersion(), true, current.getVersion(), true), null, optional, false);
		}
		return result;
	}
}
