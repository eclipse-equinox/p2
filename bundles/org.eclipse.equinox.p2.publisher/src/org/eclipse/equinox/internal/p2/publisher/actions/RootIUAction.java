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

import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * Create a top level IU that lists all the current roots as well as any explicitly identified
 * top level IUs.
 */
public class RootIUAction extends AbstractPublishingAction {

	private String version;
	private String id;
	private String[] topLevel;
	private String name;

	public RootIUAction(String id, String version, String name, String[] topLevel, IPublisherInfo info) {
		this.id = id;
		this.version = version;
		this.name = name;
		this.topLevel = topLevel;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		generateRootIU(results);
		return Status.OK_STATUS;
	}

	protected void generateRootIU(IPublisherResult result) {
		Collection children = result.getIUs(null, IPublisherResult.ROOT);
		for (int i = 0; topLevel != null && i < topLevel.length; i++) {
			String iuSpec = topLevel[i];
			IInstallableUnit iu = result.getIU(iuSpec, null);
			if (iu != null)
				children.add(iu);
		}
		InstallableUnitDescription descriptor = createTopLevelIUDescription(children, id, version, name, null, false);
		IInstallableUnit rootIU = MetadataFactory.createInstallableUnit(descriptor);
		if (rootIU == null)
			return;
		result.addIU(rootIU, IPublisherResult.NON_ROOT);

		// TODO why do we create a category here?
		//		result.addIU(generateDefaultCategory(rootIU, rootCategory), IPublisherResult.NON_ROOT);
	}

	protected static InstallableUnitDescription createTopLevelIUDescription(Collection children, String id, String version, String name, Collection requires, boolean configureLauncherData) {
		InstallableUnitDescription root = new MetadataFactory.InstallableUnitDescription();
		root.setSingleton(true);
		root.setId(id);
		root.setVersion(new Version(version));
		root.setProperty(IInstallableUnit.PROP_NAME, name);

		ArrayList requiredCapabilities = new ArrayList(children.size());
		for (Iterator iterator = children.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
			//			boolean isOptional = checkOptionalRootDependency(iu);
			requiredCapabilities.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, iu.getFilter(), false, false));
		}
		if (requires != null)
			requiredCapabilities.addAll(requires);
		root.setRequiredCapabilities((RequiredCapability[]) requiredCapabilities.toArray(new RequiredCapability[requiredCapabilities.size()]));
		root.setArtifacts(new IArtifactKey[0]);

		root.setProperty("lineUp", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		root.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(id, VersionRange.emptyRange, IUpdateDescriptor.NORMAL, null));
		root.setProperty(IInstallableUnit.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		root.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(id, new Version(version))});
		root.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_OSGI);
		return root;
	}

}
