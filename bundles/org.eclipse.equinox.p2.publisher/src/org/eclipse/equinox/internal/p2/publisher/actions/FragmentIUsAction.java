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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.internal.p2.publisher.IPublishingAction;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;
import org.osgi.framework.Version;

public class FragmentIUsAction extends Generator implements IPublishingAction {

	protected GeneratorBundleInfo[] bundles;
	private String flavor;

	public FragmentIUsAction(IPublisherInfo info, GeneratorBundleInfo[] bundles, String flavor) {
		super(createGeneratorInfo(info));
		this.bundles = bundles;
		this.flavor = flavor;
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
		for (int i = 0; i < bundles.length; i++)
			createFragment(bundles[i], results);
		return Status.OK_STATUS;
	}

	protected IInstallableUnit createFragment(GeneratorBundleInfo bundle, IPublisherResult results) {
		IInstallableUnit configuredIU = results.getIU(bundle.getSymbolicName(), null);
		if (configuredIU == null || configuredIU.isFragment())
			return null;
		bundle.setVersion(configuredIU.getVersion().toString());
		String filter = configuredIU == null ? null : configuredIU.getFilter();
		IInstallableUnit cu = MetadataGeneratorHelper.createBundleConfigurationUnit(bundle.getSymbolicName(), new Version(bundle.getVersion()), false, bundle, flavor, filter);
		//the configuration unit should share the same platform filter as the IU being configured.
		if (cu != null)
			results.addIU(cu, IPublisherResult.ROOT);
		return cu;
	}
}
