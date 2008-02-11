/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.util.Map;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.BundleDescriptionFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.MetadataGeneratorHelper;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class MetadataGeneratorUtils {

	public static IInstallableUnit createBundleIU(IArtifactKey artifactKey, File bundleFile) {
		BundleDescriptionFactory factory = initializeBundleDescriptionFactory(Activator.getContext());
		BundleDescription bundleDescription = factory.getBundleDescription(bundleFile);
		return MetadataGeneratorHelper.createBundleIU(bundleDescription, (Map) bundleDescription.getUserObject(), bundleFile.isDirectory(), artifactKey);
	}

	private static BundleDescriptionFactory initializeBundleDescriptionFactory(BundleContext context) {

		ServiceReference reference = context.getServiceReference(PlatformAdmin.class.getName());
		if (reference == null)
			throw new IllegalStateException("PlatformAdmin not registered.");
		PlatformAdmin platformAdmin = (PlatformAdmin) context.getService(reference);
		if (platformAdmin == null)
			throw new IllegalStateException("PlatformAdmin not registered.");

		try {
			StateObjectFactory stateObjectFactory = platformAdmin.getFactory();
			return new BundleDescriptionFactory(stateObjectFactory, null);
		} finally {
			context.ungetService(reference);
		}
	}

}
