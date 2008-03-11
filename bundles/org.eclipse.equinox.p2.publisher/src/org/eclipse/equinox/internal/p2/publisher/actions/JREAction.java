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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.internal.p2.publisher.IPublishingAction;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;

public class JREAction extends Generator implements IPublishingAction {

	private File location;

	public JREAction(IPublisherInfo info, File location) {
		super(createGeneratorInfo(info));
		this.location = location;
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
		IArtifactDescriptor artifact = MetadataGeneratorHelper.createJREData(location, results);
		publishArtifact(artifact, new File[] {location}, info.getArtifactRepository(), false, true);
		return Status.OK_STATUS;
	}

}
