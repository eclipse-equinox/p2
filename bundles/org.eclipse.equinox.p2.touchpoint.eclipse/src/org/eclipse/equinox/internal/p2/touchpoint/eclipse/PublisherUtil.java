/*******************************************************************************
 *  Copyright (c) 2009, 2011 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *      EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.AdviceFileAdvice;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class PublisherUtil {

	/**
	 * Returns an IU corresponding to the given artifact key and bundle, or
	 * <code>null</code> if an IU could not be created.
	 */
	public static IInstallableUnit createBundleIU(IArtifactKey artifactKey, File bundleFile) {
		BundleDescription bundleDescription = BundlesAction.createBundleDescriptionIgnoringExceptions(bundleFile);
		if (bundleDescription == null)
			return null;
		PublisherInfo info = new PublisherInfo();
		Version version = Version.create(bundleDescription.getVersion().toString());
		AdviceFileAdvice advice = new AdviceFileAdvice(bundleDescription.getSymbolicName(), version,
				IPath.fromOSString(bundleFile.getAbsolutePath()), AdviceFileAdvice.BUNDLE_ADVICE_FILE);
		if (advice.containsAdvice())
			info.addAdvice(advice);
		String shape = bundleFile.isDirectory() ? IBundleShapeAdvice.DIR : IBundleShapeAdvice.JAR;
		info.addAdvice(new BundleShapeAdvice(bundleDescription.getSymbolicName(), version, shape));
		return BundlesAction.createBundleIU(bundleDescription, artifactKey, info);
	}

}
