/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *     Code 9 - Ongoing development
#      SAP AG - consolidation of publishers for PDE formats
 *******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class EclipsePublisherHelper {
	public static IInstallableUnit[] createEclipseIU(BundleDescription bd, boolean isFolderPlugin, IArtifactKey key, Map<String, String> extraProperties) {
		ArrayList<IInstallableUnit> iusCreated = new ArrayList<>(1);
		IPublisherInfo info = new PublisherInfo();
		String shape = isFolderPlugin ? IBundleShapeAdvice.DIR : IBundleShapeAdvice.JAR;
		info.addAdvice(new BundleShapeAdvice(bd.getSymbolicName(), PublisherHelper.fromOSGiVersion(bd.getVersion()), shape));
		IInstallableUnit iu = BundlesAction.createBundleIU(bd, key, info);
		addExtraProperties(iu, extraProperties);
		iusCreated.add(iu);
		return (iusCreated.toArray(new IInstallableUnit[iusCreated.size()]));
	}

	private static void addExtraProperties(IInstallableUnit iiu, Map<String, String> extraProperties) {
		if (iiu instanceof InstallableUnit iu) {
			for (Entry<String, String> entry : extraProperties.entrySet()) {
				iu.setProperty(entry.getKey(), entry.getValue());
			}
		}
	}
}
