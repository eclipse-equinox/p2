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
import java.io.FilenameFilter;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class EquinoxExecutableAction extends AbstractPublishingAction {

	private String configSpec;
	private String idBase;
	private String versionSpec = "1.0.0"; //$NON-NLS-1$
	private File[] executables;
	private String flavor;

	public static File findExecutable(File root, String os, String baseName) {
		// TODO this may need to get more intelligent
		// if MacOS its going to be baseName.app/Contents/MacOS/baseName
		if (Constants.OS_MACOSX.equals(os)) {
			return new File(root, baseName + ".app/Contents/MacOS/" + baseName);
		}
		// if it is a UNIX flavor
		if (!Constants.OS_WIN32.equals(os) && !Constants.OS_MACOSX.equals(os)) {
			return new File(root, baseName);
		}
		// otherwise we are left with windows
		return new File(root, baseName + ".exe");
	}

	public static File[] findExecutables(File root, String os, String baseName) {
		// if MacOS
		if (Constants.OS_MACOSX.equals(os)) {
			return root.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.substring(name.length() - 4, name.length()).equalsIgnoreCase(".app"); //$NON-NLS-1$
				}
			});
		}
		// if it is a UNIX flavor
		if (!Constants.OS_WIN32.equals(os) && !Constants.OS_MACOSX.equals(os)) {
			ArrayList result = new ArrayList();
			File[] files = root.listFiles();
			for (int i = 0; files != null && i < files.length; i++) {
				String extension = new Path(files[i].getName()).getFileExtension();
				if (files[i].isFile() && (extension == null || extension.equals("so")))
					result.add(files[i]);
			}
			return (File[]) result.toArray(new File[result.size()]);
		}
		// otherwise we are left with windows
		ArrayList result = new ArrayList();
		File executable = new File(root, baseName + ".exe");
		if (executable.isFile())
			result.add(executable);
		executable = new File(root, "eclipsec.exe");
		if (executable.isFile())
			result.add(executable);
		return (File[]) result.toArray(new File[result.size()]);
	}

	public EquinoxExecutableAction(IPublisherInfo info, File[] executables, String configSpec, String idBase, String version, String flavor) {
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
		IArtifactKey key = MetadataGeneratorHelper.createLauncherArtifactKey(executableId, version);
		iu.setArtifacts(new IArtifactKey[] {key});
		iu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_NATIVE);
		ProvidedCapability launcherCapability = MetadataFactory.createProvidedCapability(flavor + idBase, idPrefix, version); //$NON-NLS-1$
		iu.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(executableId, version), launcherCapability});

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
		cu.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(configUnitId, version)});

		// TODO temporary measure for handling the Eclipse launcher feature files.
		mungeLauncherFileNames(executables);

		cu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_NATIVE);
		Map touchpointData = new HashMap();
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		for (int i = 0; i < executables.length; i++) {
			File file = executables[i];

			if (Constants.OS_MACOSX.equals(os)) {
				File macOSFolder = new File(file, "Contents/MacOS"); //$NON-NLS-1$
				if (macOSFolder.exists()) {
					File[] launcherFiles = macOSFolder.listFiles();
					for (int j = 0; j < launcherFiles.length; j++) {
						configurationData += " chmod(targetDir:${installFolder}/" + file.getName() + "/Contents/MacOS/, targetFile:" + launcherFiles[j].getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						if (new Path(launcherFiles[j].getName()).getFileExtension() == null)
							MetadataGeneratorHelper.generateLauncherSetter(launcherFiles[j].getName(), executableId, version, configSpec, result);
					}
				}
			}
			if (!Constants.OS_WIN32.equals(os) && !Constants.OS_MACOSX.equals(os)) {
				configurationData += " chmod(targetDir:${installFolder}, targetFile:" + file.getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$
				// if the file has no extension then it is the executable.  Not the best rule but ok for now
				if (new Path(file.getName()).getFileExtension() == null)
					MetadataGeneratorHelper.generateLauncherSetter(file.getName(), executableId, version, configSpec, result);
			}
		}
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		IInstallableUnit unit = MetadataFactory.createInstallableUnit(cu);
		result.addIU(unit, IPublisherResult.ROOT);

		//Create the artifact descriptor.  we have several files so no path on disk
		IArtifactDescriptor descriptor = MetadataGeneratorHelper.createArtifactDescriptor(key, null, false, true);
		publishArtifact(descriptor, null, executables, info, INCLUDE_ROOT);
	}

	private void mungeLauncherFileNames(File[] files) {
		for (int i = 0; i < files.length; i++)
			mungeLauncherFileName(files[i]);
	}

	/**
	 * @TODO This method is a temporary hack to rename the launcher.exe files
	 * to eclipse.exe (or "launcher" to "eclipse"). Eventually we will either hand-craft
	 * metadata/artifacts for launchers, or alter the delta pack to contain eclipse-branded
	 * launchers.
	 */
	private void mungeLauncherFileName(File file) {
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			for (int i = 0; i < children.length; i++) {
				mungeLauncherFileName(children[i]);
			}
		} else if (file.isFile()) {
			if (file.getName().equals("launcher")) //$NON-NLS-1$
				file.renameTo(new File(file.getParentFile(), "eclipse")); //$NON-NLS-1$
			else if (file.getName().equals("launcher.exe")) //$NON-NLS-1$
				file.renameTo(new File(file.getParentFile(), "eclipse.exe")); //$NON-NLS-1$
		}
	}
}
