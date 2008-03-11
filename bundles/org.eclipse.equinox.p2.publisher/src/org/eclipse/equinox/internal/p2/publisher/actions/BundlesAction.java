/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import java.io.File;
import java.util.ArrayList;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.internal.p2.publisher.IPublishingAction;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * Publish IUs for all of the bundles in the given set of locations.  The locations can 
 * be actual locations of the bundles or folders of bundles.
 */
public class BundlesAction extends Generator implements IPublishingAction {

	private File[] locations;

	public BundlesAction(File[] locations, IPublisherInfo info) {
		super(createGeneratorInfo(info));
		this.locations = expandLocations(locations);
	}

	private static IGeneratorInfo createGeneratorInfo(IPublisherInfo info) {
		EclipseInstallGeneratorInfoProvider result = new EclipseInstallGeneratorInfoProvider();
		result.setArtifactRepository(info.getArtifactRepository());
		result.setMetadataRepository(info.getMetadataRepository());
		result.setPublishArtifactRepository(info.publishArtifactRepository());
		result.setPublishArtifacts(info.publishArtifacts());
		return result;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		BundleDescription[] bundles = getBundleDescriptions(locations);
		generateBundleIUs(bundles, results, info.getArtifactRepository());
		return Status.OK_STATUS;
	}

	private File[] expandLocations(File[] list) {
		if (list == null)
			return new File[] {};
		ArrayList result = new ArrayList();
		for (int i = 0; i < list.length; i++) {
			File location = list[i];
			if (location.isDirectory()) {
				File[] entries = location.listFiles();
				for (int j = 0; j < entries.length; j++)
					result.add(entries[j]);
			} else {
				result.add(location);
			}
		}
		return (File[]) result.toArray(new File[result.size()]);
	}

}
