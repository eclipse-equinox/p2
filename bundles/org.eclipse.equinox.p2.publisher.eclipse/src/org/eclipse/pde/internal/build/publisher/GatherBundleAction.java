/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.util.Dictionary;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.publishing.Utils;

public class GatherBundleAction extends BundlesAction {
	private GatheringComputer computer = null;
	private String unpack = null;
	private File manifestRoot = null;
	private File bundleLocation = null;

	public GatherBundleAction(File location, File manifestRoot) {
		super(new File[] {location});
		this.manifestRoot = manifestRoot;
		this.bundleLocation = location;
	}

	@Override
	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		return super.perform(publisherInfo, results, monitor);
	}

	@Override
	protected void publishArtifact(IArtifactDescriptor descriptor, File base, File[] inclusions, IPublisherInfo publisherInfo) {
		//ignore passed in inclusions, publish according to our computer
		publishArtifact(descriptor, computer.getFiles(), null, publisherInfo, computer);
	}

	@Override
	protected BundleDescription[] getBundleDescriptions(File[] bundleLocations, IProgressMonitor monitor) {
		Dictionary<String, String> manifest = basicLoadManifestIgnoringExceptions(manifestRoot);
		if (manifest == null)
			return null;

		BundleDescription bundle = createBundleDescription(manifest, bundleLocation);
		createShapeAdvice(bundle);
		return new BundleDescription[] {bundle};
	}

	protected void createShapeAdvice(BundleDescription bundle) {
		@SuppressWarnings("unchecked")
		Dictionary<String, String> manifest = (Dictionary<String, String>) bundle.getUserObject();
		String shape = manifest.get(BUNDLE_SHAPE);
		if (shape == null) {
			if (unpack != null) {
				shape = Boolean.parseBoolean(unpack) ? IBundleShapeAdvice.DIR : IBundleShapeAdvice.JAR;
			} else {
				shape = Utils.guessUnpack(bundle, Utils.getBundleClasspath(manifest)) ? IBundleShapeAdvice.DIR : IBundleShapeAdvice.JAR;
			}
		}
		BundleShapeAdvice advice = new BundleShapeAdvice(bundle.getSymbolicName(), PublisherHelper.fromOSGiVersion(bundle.getVersion()), shape);
		info.addAdvice(advice);
	}

	public void setComputer(GatheringComputer computer) {
		this.computer = computer;
	}

	public void setUnpack(String unpack) {
		this.unpack = unpack;
	}
}
