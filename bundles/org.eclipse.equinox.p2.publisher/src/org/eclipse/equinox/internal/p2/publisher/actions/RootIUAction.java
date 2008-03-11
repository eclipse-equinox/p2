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

import java.util.Collection;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.internal.p2.publisher.IPublishingAction;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;

/**
 * Create a top level IU that lists all the current roots as well as any explicitly identified
 * top level IUs.
 */
public class RootIUAction extends Generator implements IPublishingAction {

	private String version;
	private String id;
	private String[] topLevel;

	public RootIUAction(String id, String version, String[] topLevel, IPublisherInfo info) {
		super(createGeneratorInfo(info));
		this.id = id;
		this.version = version;
		this.topLevel = topLevel;
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
		generateRootIU(results);
		return Status.OK_STATUS;
	}

	protected void generateRootIU(IPublisherResult result) {
		Collection children = result.getIUs(null, IPublisherResult.ROOT);
		for (int i = 0; i < topLevel.length; i++) {
			String iuSpec = topLevel[i];
			IInstallableUnit iu = result.getIU(iuSpec, null);
			if (iu != null)
				children.add(iu);
		}
		InstallableUnitDescription descriptor = createTopLevelIUDescription(children, id, version, /* name */id, null, false);
		IInstallableUnit rootIU = MetadataFactory.createInstallableUnit(descriptor);
		if (rootIU == null)
			return;
		result.addIU(rootIU, IPublisherResult.NON_ROOT);

		// TODO why do we create a category here?
		result.addIU(generateDefaultCategory(rootIU, rootCategory), IPublisherResult.NON_ROOT);
	}
}
