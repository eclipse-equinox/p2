/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.eclipse.GeneratorBundleInfo;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

/**
 * Publish IUs that install/configure the standard things like bundles, features and source bundles
 */
public class DefaultCUsAction extends AbstractPublisherAction {

	private String flavor;
	private int startLevel;
	private boolean start;

	public DefaultCUsAction(IPublisherInfo info, String flavor, int startLevel, boolean start) {
		this.flavor = flavor;
		this.startLevel = startLevel;
		this.start = start;
	}

	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		setPublisherInfo(publisherInfo);
		generateDefaultConfigIU(results);
		return Status.OK_STATUS;
	}

	protected void generateDefaultConfigIU(IPublisherResult result) {
		//		TODO this is a bit of a hack.  We need to have the default IU fragment generated with code that configures
		//		and unconfigures.  The Generator should be decoupled from any particular provider but it is not clear
		//		that we should add the create* methods to IGeneratorInfo...
		//		MockBundleDescription bd1 = new MockBundleDescription("defaultConfigure");
		//		MockBundleDescription bd2 = new MockBundleDescription("defaultUnconfigure");
		result.addIU(BundlesAction.createDefaultBundleConfigurationUnit(createDefaultConfigurationBundleInfo(), createDefaultUnconfigurationBundleInfo(), flavor), IPublisherResult.ROOT);
		result.addIU(PublisherHelper.createDefaultFeatureConfigurationUnit(flavor), IPublisherResult.ROOT);
		result.addIU(PublisherHelper.createDefaultConfigurationUnitForSourceBundles(flavor), IPublisherResult.ROOT);
	}

	protected GeneratorBundleInfo createDefaultConfigurationBundleInfo() {
		GeneratorBundleInfo result = new GeneratorBundleInfo();
		result.setSymbolicName("defaultConfigure"); //$NON-NLS-1$
		result.setVersion("1.0.0"); //$NON-NLS-1$
		result.setStartLevel(startLevel);
		result.setMarkedAsStarted(start);
		// These should just be in the install section now
		//		result.setSpecialConfigCommands("installBundle(bundle:${artifact});");
		return result;
	}

	protected GeneratorBundleInfo createDefaultUnconfigurationBundleInfo() {
		GeneratorBundleInfo result = new GeneratorBundleInfo();
		result.setSymbolicName("defaultUnconfigure"); //$NON-NLS-1$
		result.setVersion("1.0.0"); //$NON-NLS-1$
		// These should just be in the uninstall section now
		//		result.setSpecialConfigCommands("uninstallBundle(bundle:${artifact});");
		return result;
	}

}
