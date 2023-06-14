/*******************************************************************************
 *  Copyright (c) 2008, 2017 EclipseSource and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *      Eclipse Source - initial API and implementation
 *      IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.pde.internal.publishing.Activator;

public class ExecutablesDescriptor {

	private File location;
	private Set<File> files;
	private String executableName;
	private boolean temporary = false;
	private final String os;
	private File iniFile;

	public static File findExecutable(String os, File root, String baseName) {
		// TODO this may need to get more intelligent
		// if MacOS its going to be baseName.app/Contents/MacOS/baseName
		if (Constants.OS_MACOSX.equals(os)) {
			return new File(root, baseName + ".app/Contents/MacOS/" + baseName); //$NON-NLS-1$
		}
		// if it is not Mac and not Windows it must be a UNIX flavor
		if (!Constants.OS_WIN32.equals(os)) {
			return new File(root, baseName);
		}
		// otherwise we are left with windows
		return new File(root, baseName + ".exe"); //$NON-NLS-1$
	}

	/**
	 * Return the root directory of the executables folder for the given configSpec.  The folder
	 * is expected to be part of the standard Eclipse executables feature whose structure is 
	 * embedded here.
	 * @param executablesFeatureLocation the location of the executables feature
	 * @param configSpec the configuration to lookup
	 * @return the root location of the requested executables
	 */
	public static ExecutablesDescriptor createExecutablesFromFeature(File executablesFeatureLocation, String configSpec) {
		// TODO consider handling JAR'd features here...
		if (executablesFeatureLocation == null || !executablesFeatureLocation.exists())
			return null;
		String[] config = AbstractPublisherAction.parseConfigSpec(configSpec);
		File result = new File(executablesFeatureLocation, "bin/" + config[0] + "/" + config[1] + "/" + config[2]); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (!result.exists())
			return null;
		return new ExecutablesDescriptor(config[1], "launcher", result, new File[] {result}); //$NON-NLS-1$
	}

	/**
	 * Create an executable descriptor based on the given location, os and name.
	 * This method is typically used to identify the executable related files in existing
	 * unmanaged configurations.
	 * @param os
	 * @param location
	 * @param executable
	 * @return the created descriptor
	 */
	public static ExecutablesDescriptor createDescriptor(String os, String executable, File location) {
		if (Constants.OS_MACOSX.equals(os))
			return createMacDescriptor(os, executable, location);

		// if it is not Mac and not Windows it must be a UNIX flavor
		if (!Constants.OS_WIN32.equals(os))
			return createUnixDescriptor(os, executable, location);

		// Nothing else so it must be Windows
		return createWindowsDescriptor(os, executable, location);
	}

	private static ExecutablesDescriptor createWindowsDescriptor(String os, String executable, File location) {
		ExecutablesDescriptor result = new ExecutablesDescriptor(os, executable, location, null);
		File file = new File(location, executable + ".exe"); //$NON-NLS-1$
		if (file.isFile()) {
			result.addFile(file);
			result.iniFile = new File(location, executable + ".ini"); //$NON-NLS-1$
		}
		file = new File(location, "eclipsec.exe"); //$NON-NLS-1$
		if (file.isFile())
			result.addFile(file);
		return result;
	}

	private static ExecutablesDescriptor createUnixDescriptor(String os, String executable, File location) {
		ExecutablesDescriptor result = new ExecutablesDescriptor(os, executable, location, null);
		File[] files = location.listFiles();
		for (int i = 0; files != null && i < files.length; i++) {
			String extension = IPath.fromOSString(files[i].getName()).getFileExtension();
			if (files[i].isFile() && (extension == null || extension.equals("so"))) //$NON-NLS-1$
				result.addFile(files[i]);
		}
		result.iniFile = new File(location, executable + ".ini"); //$NON-NLS-1$
		return result;
	}

	private static ExecutablesDescriptor createMacDescriptor(String os, String executable, File location) {
		File files[] = location.listFiles((FilenameFilter) (dir, name) -> {
			int length = name.length();
			return length > 3 && name.substring(length - 4, length).equalsIgnoreCase(".app"); //$NON-NLS-1$
		});
		ExecutablesDescriptor result = new ExecutablesDescriptor(os, executable, location, files);
		result.iniFile = new File(location, executable + ".ini"); //$NON-NLS-1$
		return result;
	}

	public ExecutablesDescriptor(String os, String executable, File location, File[] files) {
		this.os = os;
		this.executableName = executable;
		this.location = location;
		if (files == null)
			this.files = new HashSet<>(11);
		else {
			this.files = new HashSet<>(files.length);
			for (File file : files) {
				addAllFiles(file);
			}
		}
	}

	public ExecutablesDescriptor(ExecutablesDescriptor descriptor) {
		this.os = descriptor.os;
		this.location = descriptor.location;
		this.executableName = descriptor.executableName;
		this.temporary = descriptor.temporary;
		this.files = new HashSet<>(descriptor.files);
	}

	public void addAllFiles(File file) {
		if (file.isFile())
			files.add(relativize(file));
		else {
			File absolute = file.isAbsolute() ? file : new File(location, file.getPath());
			File[] list = absolute.listFiles();
			for (File list1 : list) {
				addAllFiles(list1);
			}
		}
	}

	public void addFile(File file) {
		files.add(relativize(file));
	}

	// do a simple relativization by removing all the bits before the location
	private File relativize(File file) {
		if (!file.isAbsolute())
			return file;
		String path = file.getPath();
		if (!path.startsWith(location.getPath()))
			throw new IllegalArgumentException(file.toString() + " must be related to " + location); //$NON-NLS-1$
		path = path.substring(location.getPath().length());
		// trim off any separator.  This accomodates people who set the location with a trailing /
		if (path.startsWith("/") || path.startsWith("\\")) //$NON-NLS-1$//$NON-NLS-2$
			path = path.substring(1);
		return new File(path);
	}

	public void removeFile(File file) {
		files.remove(relativize(file));
	}

	public void replace(File oldFile, File newFile) {
		removeFile(oldFile);
		addFile(newFile);
	}

	public File[] getFiles() {
		File[] result = files.toArray(new File[files.size()]);
		for (int i = 0; i < result.length; i++)
			result[i] = new File(location, result[i].getPath());
		return result;
	}

	public String getExecutableName() {
		return executableName;
	}

	public File getExecutable() {
		return findExecutable(os, location, executableName);
	}

	public File getIniLocation() {
		return iniFile;
	}

	public File getLocation() {
		return location;
	}

	public void setLocation(File value) {
		location = value;
	}

	public boolean isTemporary() {
		return temporary;
	}

	public void setExecutableName(String value, boolean updateFiles) {
		if (updateFiles)
			updateExecutableName(value);
		executableName = value;
	}

	public void makeTemporaryCopy() {
		if (isTemporary())
			return;
		File tempFile = null;
		try {
			tempFile = File.createTempFile("p2.brandingIron", ""); //$NON-NLS-1$ //$NON-NLS-2$
			tempFile.delete();
			for (File file : files)
				FileUtils.copy(location, tempFile, file, true);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error publishing artifacts", e)); //$NON-NLS-1$
		}
		location = tempFile;
		temporary = true;
	}

	/**
	 * If the executable represented by this descriptor has been branded then a mess
	 * of files have been renamed.  Here scan the descriptor's file list and update the names
	 * taking into account the different layout on different OSes.
	 * @param newName the new name of the executable.
	 */
	private void updateExecutableName(String newName) {
		if (newName.equalsIgnoreCase(executableName))
			return;
		Set<File> filesCopy = new HashSet<>(files);
		for (File file : filesCopy) {
			String base = file.getParent();

			// use String concatenation here because new File("", "foo") is absolute on at least windows...
			base = base == null ? "" : base + "/"; //$NON-NLS-1$ //$NON-NLS-2$
			if (Constants.OS_MACOSX.equals(os) && base.startsWith(executableName + ".app")) //$NON-NLS-1$
				base = newName + ".app" + base.substring(executableName.length() + 4); //$NON-NLS-1$
			if (file.getName().equalsIgnoreCase(executableName))
				replace(file, new File(base + newName));
			else if (file.getName().equalsIgnoreCase(executableName + ".exe")) //$NON-NLS-1$
				replace(file, new File(base + newName + ".exe")); //$NON-NLS-1$
			else if (file.getName().equalsIgnoreCase(executableName + "c.exe")) //$NON-NLS-1$
				replace(file, new File(base + newName + "c.exe")); //$NON-NLS-1$
			else if (file.getName().equalsIgnoreCase(executableName + ".ini")) //$NON-NLS-1$
				replace(file, new File(base + newName + ".ini")); //$NON-NLS-1$
			else if (Constants.OS_MACOSX.equals(os))
				replace(file, new File(base + file.getName()));
		}
	}
}
