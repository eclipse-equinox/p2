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
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class RootFilesAction extends AbstractPublishingAction {

	private String configSpec;
	private String idBase;
	private String versionSpec = "1.0.0"; //$NON-NLS-1$
	private File root;
	private File[] exclusions;
	private String flavor;

	public RootFilesAction(IPublisherInfo info, File root, File[] exclusions, String configSpec, String idBase, String version, String flavor) {
		this.root = root;
		this.exclusions = exclusions;
		this.configSpec = configSpec;
		this.idBase = idBase == null ? "org.eclipse" : idBase; //$NON-NLS-1$
		// if the given version is not the default "replace me" version then save it
		if (version != null && !version.equals("0.0.0")) //$NON-NLS-1$
			this.versionSpec = version;
		this.flavor = flavor;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		generateExecutableIUs(info, results);
		return Status.OK_STATUS;
	}

	/**
	 * Generates IUs and CUs for the files that make up the launcher for a given
	 * ws/os/arch combination.
	 */
	protected void generateExecutableIUs(IPublisherInfo info, IPublisherResult result) {
		// Create the IU for the executable
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(true);
		String idPrefix = idBase + ".rootfiles"; //$NON-NLS-1$
		String executableId = idPrefix + '.' + createIdString(configSpec);
		iu.setId(executableId);
		Version version = new Version(versionSpec);
		iu.setVersion(version);
		String filter = createFilterSpec(configSpec);
		iu.setFilter(filter);
		IArtifactKey key = MetadataGeneratorHelper.createLauncherArtifactKey(executableId, version);
		iu.setArtifacts(new IArtifactKey[] {key});
		iu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_NATIVE);
		ProvidedCapability launcherCapability = MetadataFactory.createProvidedCapability(flavor + idBase, idPrefix, version); //$NON-NLS-1$
		iu.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(executableId, version), launcherCapability});
		result.addIU(MetadataFactory.createInstallableUnit(iu), IPublisherResult.ROOT);

		// Create the CU that installs/configures the executable
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = flavor + executableId;
		cu.setId(configUnitId);
		cu.setVersion(version);
		cu.setFilter(filter);
		cu.setHost(new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, executableId, new VersionRange(version, true, version, true), null, false, false)});
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		//TODO bug 218890, would like the fragment to provide the launcher capability as well, but can't right now.
		cu.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(configUnitId, version)});

		cu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_NATIVE);
		Map touchpointData = new HashMap();
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$

		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		IInstallableUnit unit = MetadataFactory.createInstallableUnit(cu);
		result.addIU(unit, IPublisherResult.ROOT);

		//Create the artifact descriptor.  we have several files so no path on disk
		IArtifactDescriptor descriptor = MetadataGeneratorHelper.createArtifactDescriptor(key, null, false, true);
		publishArtifact(descriptor, null, excludeFiles(root, exclusions), info, INCLUDE_ROOT);
	}

	private File[] excludeFiles(File base, File[] exclusions) {
		ArrayList result = new ArrayList();
		File[] list = base.listFiles();
		for (int i = 0; i < list.length; i++) {
			File file = list[i];
			if (exclusions == null)
				result.add(file);
			else {
				boolean found = false;
				for (int j = 0; j < exclusions.length; j++)
					if (file.equals(exclusions[j]))
						found = true;
				if (!found)
					result.add(file);
			}
		}
		return (File[]) result.toArray(new File[result.size()]);
	}

}
