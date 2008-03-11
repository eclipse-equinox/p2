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
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;

/**
 * Publish IUs that install/configure the standard things like bundles, features and source bundles
 */
public class DefaultCUsAction extends Generator implements IPublishingAction {

	public DefaultCUsAction(IPublisherInfo info, String flavor) {
		super(createGeneratorInfo(info, flavor));
	}

	private static IGeneratorInfo createGeneratorInfo(IPublisherInfo info, String flavor) {
		EclipseInstallGeneratorInfoProvider result = new EclipseInstallGeneratorInfoProvider();
		result.setArtifactRepository(info.getArtifactRepository());
		result.setMetadataRepository(info.getMetadataRepository());
		result.setPublishArtifactRepository(info.publishArtifactRepository());
		result.setPublishArtifacts(info.publishArtifacts());
		result.setFlavor(flavor);
		return result;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		generateDefaultConfigIU(results);
		return Status.OK_STATUS;
	}

}
