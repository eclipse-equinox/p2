/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ExecutablesDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class EquinoxExecutableAction extends AbstractPublisherAction {

	protected String configSpec;
	protected String idBase;
	protected String versionSpec = "1.0.0"; //$NON-NLS-1$
	protected ExecutablesDescriptor executables;
	protected String flavor;

	protected EquinoxExecutableAction() {
	}

	public EquinoxExecutableAction(ExecutablesDescriptor executables, String configSpec, String idBase, String version, String flavor) {
		this.executables = executables;
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
		String idPrefix = idBase + ".executable"; //$NON-NLS-1$
		String executableId = idPrefix + '.' + createIdString(configSpec);
		iu.setId(executableId);
		Version version = new Version(versionSpec);
		iu.setVersion(version);
		String filter = createFilterSpec(configSpec);
		iu.setFilter(filter);
		iu.setSingleton(true);
		IArtifactKey key = PublisherHelper.createLauncherArtifactKey(executableId, version);
		iu.setArtifacts(new IArtifactKey[] {key});
		iu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		ProvidedCapability launcherCapability = MetadataFactory.createProvidedCapability(flavor + idBase, idPrefix, version); //$NON-NLS-1$
		iu.setCapabilities(new ProvidedCapability[] {PublisherHelper.createSelfCapability(executableId, version), launcherCapability});

		// setup a requirement between the executable and the launcher fragment that has the shared library
		String[] config = parseConfigSpec(configSpec);
		String ws = config[0];
		String os = config[1];
		String arch = config[2];
		String launcherFragment = EquinoxLauncherCUAction.ORG_ECLIPSE_EQUINOX_LAUNCHER + '.' + ws + '.' + os;
		if (!Constants.OS_MACOSX.equals(os))
			launcherFragment += '.' + arch;
		iu.setRequiredCapabilities(new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, launcherFragment, VersionRange.emptyRange, filter, false, false)});
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
		cu.setCapabilities(new ProvidedCapability[] {PublisherHelper.createSelfCapability(configUnitId, version)});

		// TODO temporary measure for handling the Eclipse launcher feature files.
		ExecutablesDescriptor files = brandExecutables(executables);

		cu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		Map touchpointData = new HashMap();
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		File[] fileList = files.getFiles();
		for (int i = 0; i < fileList.length; i++) {
			File file = fileList[i];
			if (Constants.OS_MACOSX.equals(os)) {
				File macOSFolder = new File(file, "Contents/MacOS"); //$NON-NLS-1$
				if (macOSFolder.exists()) {
					File[] launcherFiles = macOSFolder.listFiles();
					for (int j = 0; j < launcherFiles.length; j++) {
						configurationData += " chmod(targetDir:${installFolder}/" + file.getName() + "/Contents/MacOS/, targetFile:" + launcherFiles[j].getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						if (new Path(launcherFiles[j].getName()).getFileExtension() == null)
							PublisherHelper.generateLauncherSetter(launcherFiles[j].getName(), executableId, version, configSpec, result);
					}
				}
			}
			if (!Constants.OS_WIN32.equals(os) && !Constants.OS_MACOSX.equals(os)) {
				configurationData += " chmod(targetDir:${installFolder}, targetFile:" + file.getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$
				// if the file has no extension then it is the executable.  Not the best rule but ok for now
				if (new Path(file.getName()).getFileExtension() == null)
					PublisherHelper.generateLauncherSetter(file.getName(), executableId, version, configSpec, result);
			}
		}
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		IInstallableUnit unit = MetadataFactory.createInstallableUnit(cu);
		result.addIU(unit, IPublisherResult.ROOT);

		//Create the artifact descriptor.  we have several files so no path on disk
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, null);
		publishArtifact(descriptor, fileList, files.getLocation(), info, INCLUDE_ROOT);
		if (files.isTemporary())
			FileUtils.deleteAll(files.getLocation());
	}

	protected ExecutablesDescriptor brandExecutables(ExecutablesDescriptor descriptor) {
		ExecutablesDescriptor result = new ExecutablesDescriptor(descriptor);
		result.makeTemporaryCopy();
		File[] list = descriptor.getFiles();
		for (int i = 0; i < list.length; i++)
			mungeLauncherFileName(list[i], descriptor);
		result.setExecutableName("eclipse", true); //$NON-NLS-1$
		return result;
	}

	/**
	 * @TODO This method is a temporary hack to rename the launcher.exe files
	 * to eclipse.exe (or "launcher" to "eclipse"). Eventually we will either hand-craft
	 * metadata/artifacts for launchers, or alter the delta pack to contain eclipse-branded
	 * launchers.
	 */
	private void mungeLauncherFileName(File file, ExecutablesDescriptor descriptor) {
		if (file.getName().equals("launcher")) { //$NON-NLS-1$
			File newFile = new File(file.getParentFile(), "eclipse"); //$NON-NLS-1$
			file.renameTo(newFile); //$NON-NLS-1$
			descriptor.replace(file, newFile);
		} else if (file.getName().equals("launcher.exe")) { //$NON-NLS-1$
			File newFile = new File(file.getParentFile(), "eclipse.exe"); //$NON-NLS-1$
			file.renameTo(newFile); //$NON-NLS-1$
			descriptor.replace(file, newFile);
		}
	}

}
