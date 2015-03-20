/*******************************************************************************
 *  Copyright (c) 2005, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 * 	IBM Corporation - initial API and implementation
 * 	Code 9 - Additional function and fixes
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse;

import java.io.*;
import javax.xml.transform.TransformerException;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.pde.internal.publishing.Utils;
import org.eclipse.pde.internal.swt.tools.IconExe;

/**
 *
 */
public class BrandingIron {
	private static final String XDOC_ICON = "-Xdock:icon=../Resources/Eclipse.icns"; //$NON-NLS-1$
	private static final String XDOC_ICON_PREFIX = "-Xdock:icon=../Resources/"; //$NON-NLS-1$

	private String[] icons = null;
	private String name;
	private String applicationName;

	private String description;
	private String os = "win32"; //$NON-NLS-1$
	private boolean brandIcons = true;
	private String id;
	private Version version;

	public BrandingIron() {
	}

	public void setName(String value) {
		name = value;
		if (applicationName == null) {
			applicationName = value;
		}
	}

	/**
	 * Sets the name for the MacOS "<i>ProductName</i>.app" folder.
	 */
	public void setApplicationName(String value) {
		applicationName = value;
	}

	public void setIcons(String[] value) {
		icons = (value == null || value.length == 0) ? null : value;
	}

	public void setIcons(String value) {
		icons = org.eclipse.equinox.internal.frameworkadmin.utils.Utils.getTokens(value, ",");//$NON-NLS-1$
		if (icons[0].startsWith("${")) { //$NON-NLS-1$
			if (icons.length > 1) {
				String[] temp = new String[icons.length - 1];
				System.arraycopy(icons, 1, temp, 0, temp.length);
				icons = temp;
			} else {
				icons = null;
			}
		}
		//trim whitespace
		if (icons != null) {
			for (int i = 0; i < icons.length; i++) {
				icons[i] = icons[i].trim();
			}
		}
	}

	public void brand(ExecutablesDescriptor descriptor) throws Exception {
		// if the name property is not set it will be ${launcher.name} so just bail.
		if (name.startsWith("${")) //$NON-NLS-1$
			return;

		if (icons == null || icons[0].startsWith("${")) //$NON-NLS-1$
			brandIcons = false;

		File root = descriptor.getLocation();

		// if the root does not exists (happens in some packaging cases) or 
		// there is already a file with target name and we don't need to update its icons, don't do anything
		String testName = os.equals("win32") ? name + ".exe" : name; //$NON-NLS-1$ //$NON-NLS-2$
		if (!root.exists() || (!brandIcons && new File(root, testName).exists()))
			return;

		// make sure the descriptor's location is a canonical path otherwise
		// removing files from it may fail (this happens notably on Windows)
		try {
			root = root.getCanonicalFile();
		} catch (IOException e) {
			// try to live with an absolute path
			root = root.getAbsoluteFile();
		}
		descriptor.setLocation(root);

		if ("win32".equals(os)) //$NON-NLS-1$
			brandWindows(descriptor);
		else if ("linux".equals(os)) //$NON-NLS-1$
			brandLinux(descriptor);
		else if ("solaris".equals(os)) //$NON-NLS-1$
			brandSolaris(descriptor);
		else if ("macosx".equals(os)) //$NON-NLS-1$
			brandMac(descriptor);
		else if ("aix".equals(os)) //$NON-NLS-1$
			brandAIX(descriptor);
		else if ("hpux".equals(os)) //$NON-NLS-1$
			brandHPUX(descriptor);
		else
			renameLauncher(descriptor);
		descriptor.setExecutableName(name, true);
	}

	private void brandAIX(ExecutablesDescriptor descriptor) {
		renameLauncher(descriptor);
	}

	private void brandHPUX(ExecutablesDescriptor descriptor) {
		renameLauncher(descriptor);
	}

	private void brandLinux(ExecutablesDescriptor descriptor) throws Exception {
		renameLauncher(descriptor);
		if (brandIcons) {
			File icon = null;
			if (icons.length > 0) {
				for (int i = 0; i < icons.length; i++) {
					if (icons[i].toLowerCase().endsWith(".xpm")) { //$NON-NLS-1$
						icon = new File(icons[i]);
						break;
					}
				}
			}
			if (icon != null) {
				File targetIcon = new File(descriptor.getLocation(), "icon.xpm"); //$NON-NLS-1$
				Utils.copy(icon, targetIcon);
				descriptor.addFile(targetIcon);
			}
		}
	}

	private void brandSolaris(ExecutablesDescriptor descriptor) throws Exception {
		renameLauncher(descriptor);
		if (brandIcons == false)
			return;

		File root = descriptor.getLocation();
		for (int i = 0; i < icons.length; i++) {
			String icon = icons[i];

			int iconNameLength = icon.length();
			if (iconNameLength < 5)
				continue;
			String extension = icon.substring(iconNameLength - 5);
			// check if the extension is one of: .l.pm, .m.pm, .s.pm, .t.pm
			if (extension.charAt(0) == '.' && extension.endsWith(".pm") && "lmst".indexOf(extension.charAt(1)) >= 0) { //$NON-NLS-1$ //$NON-NLS-2$
				File targetIcon = new File(root, name + extension);
				Utils.copy(new File(icon), targetIcon);
				descriptor.addFile(targetIcon);
			}
		}
	}

	private void brandMac(ExecutablesDescriptor descriptor) throws Exception {
		//Initially the files are in: <root>/Eclipse.app/ 
		//and they must appear in <root>/MyAppName.app/
		//Because java does not support the rename of a folder, files are copied.

		//Initialize the target folders
		File root = descriptor.getLocation();

		File target = root;
		target.mkdirs();
		new File(target, "MacOS").mkdirs(); //$NON-NLS-1$
		new File(target, "Resources").mkdirs(); //$NON-NLS-1$

		File initialRoot = new File(root, "Launcher.app/Contents"); //$NON-NLS-1$
		if (!initialRoot.exists())
			initialRoot = new File(root, "launcher.app/Contents"); //$NON-NLS-1$
		if (!initialRoot.exists())
			initialRoot = new File(root, "Eclipse.app/Contents"); //$NON-NLS-1$
		if (!initialRoot.exists())
			initialRoot = new File(root, "eclipse.app/Contents"); //$NON-NLS-1$
		if (!initialRoot.exists())
			throw new FileNotFoundException("cannot find launcher root (Eclipse.app or Launcher.app)"); //$NON-NLS-1$
		// use the canonical rep to avoid possible issues from case-insensitive file systems
		initialRoot = initialRoot.getCanonicalFile();
		copyMacLauncher(descriptor, initialRoot, target);
		String iconName = ""; //$NON-NLS-1$
		if (brandIcons) {
			File icon = null;
			for (int i = 0; i < icons.length; i++) {
				if (icons[i].toLowerCase().endsWith(".icns")) { //$NON-NLS-1$
					icon = new File(icons[i]);
					if (icon.exists()) {
						break;
					}
				}
			}
			if (icon != null) {
				iconName = icon.getName(); // was previously: name + ".icns"
				File initialIcon = new File(initialRoot, "Resources/Eclipse.icns"); //$NON-NLS-1$
				try {
					// canonicalize to ensure case matches
					initialIcon = initialIcon.getCanonicalFile();
				} catch (IOException e) { /*ignore*/
				}
				File targetIcon = new File(target, "Resources/" + iconName); //$NON-NLS-1$

				initialIcon.delete();
				descriptor.removeFile(initialIcon);
				Utils.copy(icon, targetIcon);
				descriptor.addFile(targetIcon);
			}
		}
		copyMacIni(descriptor, initialRoot, target, iconName);
		modifyInfoPListFile(descriptor, initialRoot, target, iconName);
		File splashApp = new File(initialRoot, "Resources/Splash.app"); //$NON-NLS-1$
		if (splashApp.exists()) {
			brandMacSplash(descriptor, initialRoot, target, iconName);
		}

		// move over any files remaining in the original folder
		moveContents(descriptor, initialRoot, target);
		initialRoot.getParentFile().delete(); // initialRoot is xxx.app/Contents
	}

	/**
	 * Brand the splash.app Info.plist and  link or copy the mac launcher.
	 * It is assumed that the mac launcher has been branded already.
	 * @param descriptor
	 * @param initialRoot
	 * @param target
	 * @param iconName
	 */
	private void brandMacSplash(ExecutablesDescriptor descriptor, File initialRoot, File target, String iconName) {
		String splashContents = "Resources/Splash.app/Contents"; //$NON-NLS-1$
		modifyInfoPListFile(descriptor, new File(initialRoot, splashContents), new File(target, splashContents), iconName);

		//link the MacOS launcher for the splash app
		String splashMacOS = splashContents + "/MacOS"; //$NON-NLS-1$
		File macOSDir = new File(target, "MacOS"); //$NON-NLS-1$
		File splashMacOSDir = new File(target, splashMacOS);

		splashMacOSDir.mkdirs();

		File targetLauncher = new File(splashMacOSDir, name);

		int result = -1;
		String osName = System.getProperty("os.name"); //$NON-NLS-1$
		if (osName != null && !osName.startsWith("Windows")) { //$NON-NLS-1$
			try {
				String[] command = new String[] {"ln", "-sf", "../../../MacOS/" + name, name}; //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
				Process proc = Runtime.getRuntime().exec(command, null, splashMacOSDir);
				result = proc.waitFor();
			} catch (IOException e) {
				// ignore
			} catch (InterruptedException e) {
				// ignore
			}
		}

		if (result != 0) {
			//ln failed, or we are on windows, just copy the executable instead
			try {
				Utils.copy(new File(macOSDir, name), targetLauncher);
				try {
					Runtime.getRuntime().exec(new String[] {"chmod", "755", targetLauncher.getAbsolutePath()}); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (IOException e) {
					// ignore
				}
			} catch (IOException e) {
				System.out.println("Could not copy macosx splash launcher"); //$NON-NLS-1$
			}
		}

		File initialLauncher = findLauncher(new File(initialRoot, splashMacOS));
		if (initialLauncher != null) {
			try {
				if (!initialLauncher.getCanonicalFile().equals(targetLauncher.getCanonicalFile()))
					initialLauncher.delete();
			} catch (IOException e) {
				// ignore
			}
			descriptor.removeFile(initialLauncher);
		}
		descriptor.addFile(targetLauncher);
	}

	private File findLauncher(File root) {
		for (String launcherName : new String[] {"launcher", "eclipse"}) { //$NON-NLS-1$ //$NON-NLS-2$
			File launcher = new File(root, launcherName);
			if (launcher.exists())
				return launcher;
		}
		return null;
	}

	private void moveContents(ExecutablesDescriptor descriptor, File source, File target) {
		if (!source.exists())
			return;
		try {
			source = source.getCanonicalFile();
			target = target.getCanonicalFile();
		} catch (IOException e) {
			System.out.println("Could not copy macosx resources."); //$NON-NLS-1$
			return;
		}
		if (source.equals(target))
			return;

		target.getParentFile().mkdirs();
		if (source.isDirectory()) {
			target.mkdirs();
			File[] contents = source.listFiles();
			for (int i = 0; i < contents.length; i++) {
				File dest = new File(target, contents[i].getName());
				if (contents[i].isFile()) {
					contents[i].renameTo(dest);
					descriptor.replace(contents[i], dest);
				} else
					moveContents(descriptor, contents[i], dest);
			}
			source.delete();
		} else {
			source.renameTo(target);
			descriptor.replace(source, target);
		}
	}

	private void brandWindows(ExecutablesDescriptor descriptor) throws Exception {
		File root = descriptor.getLocation();
		File templateLauncher = new File(root, name + ".exe"); //$NON-NLS-1$
		if (!templateLauncher.exists())
			templateLauncher = new File(root, "launcher.exe"); //$NON-NLS-1$
		if (!templateLauncher.exists())
			templateLauncher = new File(root, "eclipse.exe"); //$NON-NLS-1$
		if (brandIcons) {
			if (templateLauncher.exists()) {
				String[] args = new String[icons.length + 1];
				args[0] = templateLauncher.getAbsolutePath();
				System.arraycopy(icons, 0, args, 1, icons.length);
				IconExe.main(args);
			} else {
				System.out.println("Could not find executable to brand"); //$NON-NLS-1$
			}
		}
		File targetLauncher = new File(root, name + ".exe"); //$NON-NLS-1$
		if (templateLauncher.exists() && !templateLauncher.getName().equals(name + ".exe")) { //$NON-NLS-1$
			templateLauncher.renameTo(targetLauncher);
			descriptor.replace(templateLauncher, targetLauncher);
		}
		descriptor.setExecutableName(name, true);
	}

	private void renameLauncher(ExecutablesDescriptor descriptor) {
		File root = descriptor.getLocation();
		File launcher = findLauncher(root);
		if (launcher == null)
			return;
		File targetLauncher = new File(root, name);
		launcher.renameTo(targetLauncher);
		descriptor.replace(launcher, targetLauncher);
		descriptor.setExecutableName(name, true);
	}

	private void copyMacLauncher(ExecutablesDescriptor descriptor, File initialRoot, File target) {
		File launcher = getCanonicalFile(new File(initialRoot, "MacOS/launcher")); //$NON-NLS-1$
		File eclipseLauncher = getCanonicalFile(new File(initialRoot, "MacOS/eclipse")); //$NON-NLS-1$
		File targetFile = getCanonicalFile(new File(target, "MacOS/" + name)); //$NON-NLS-1$
		if (!launcher.exists()) {
			launcher = eclipseLauncher;
		} else if (eclipseLauncher.exists() && !targetFile.equals(eclipseLauncher)) {
			//we may actually have both if exporting from the mac
			eclipseLauncher.delete();
			descriptor.removeFile(eclipseLauncher);
		}
		if (!targetFile.equals(launcher)) {
			try {
				Utils.copy(launcher, targetFile);
			} catch (IOException e) {
				System.out.println("Could not copy macosx launcher"); //$NON-NLS-1$
				return;
			}
			launcher.delete();
			launcher.getParentFile().delete();
			descriptor.replace(launcher, targetFile);
		}
		try {
			//Force the executable bit on the exe because it has been lost when copying the file
			Runtime.getRuntime().exec(new String[] {"chmod", "755", targetFile.getAbsolutePath()}); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IOException e) {
			//ignore
		}
		descriptor.setExecutableName(name, false);
	}

	private File getCanonicalFile(File file) {
		try {
			return file.getCanonicalFile();
		} catch (IOException e) {
			return file;
		}
	}

	private void copyMacIni(ExecutablesDescriptor descriptor, File initialRoot, File target, String iconName) {
		String brandedIniName = "MacOS/" + name + ".ini"; //$NON-NLS-1$//$NON-NLS-2$
		// 4 possibilities, in order of preference:
		// rcp.app/Contents/MacOS/rcp.ini   		(targetFile)
		// Eclipse.app/Contents/MacOS/rcp.ini		(brandedIni)
		// Eclipse.app/Contents/MacOS/eclipse.ini	(ini)
		// Eclipse.app/Contents/MacOS/Eclipse.ini	(ini2)
		File targetFile = getCanonicalFile(new File(target, brandedIniName));
		File brandedIni = getCanonicalFile(new File(initialRoot, brandedIniName));
		File ini = getCanonicalFile(new File(initialRoot, "MacOS/eclipse.ini")); //$NON-NLS-1$
		File ini2 = getCanonicalFile(new File(initialRoot, "MacOS/Eclipse.ini")); //$NON-NLS-1$

		if (targetFile.exists()) {
			//an ini already exists at the target, use that
			if (brandedIni.exists() && !brandedIni.equals(targetFile)) {
				brandedIni.delete();
				descriptor.removeFile(brandedIni);
			}
			if (ini.exists() && !ini.equals(targetFile)) {
				ini.delete();
				descriptor.removeFile(ini);
			}
			if (ini2.exists() && !ini2.equals(targetFile)) {
				ini2.delete();
				descriptor.removeFile(ini2);
			}
			ini = targetFile;
		} else if (brandedIni.exists()) {
			//take the one that is already branded
			if (ini.exists() && !ini.equals(brandedIni)) {
				ini.delete();
				descriptor.removeFile(ini);
			}
			if (ini2.exists() && !ini2.equals(brandedIni)) {
				ini2.delete();
				descriptor.removeFile(ini2);
			}
			ini = brandedIni;
		} else {
			if (ini.exists()) {
				if (ini2.exists() && !ini2.equals(ini)) {
					// this should not happen really
					System.out.printf("Found both %s and %s - discarding the latter", ini.getAbsolutePath(), ini2.getAbsolutePath()); //$NON-NLS-1$
					ini2.delete();
					descriptor.removeFile(ini2);
				}
			} else if (ini2.exists()) {
				ini = ini2;
			} else
				return;
		}

		StringBuffer buffer;
		try {
			buffer = readFile(ini);
		} catch (IOException e) {
			System.out.println("Impossible to brand ini file"); //$NON-NLS-1$
			return;
		}

		if (iconName.length() > 0) {
			int xdoc = scan(buffer, 0, XDOC_ICON);
			if (xdoc != -1) {
				String icns = XDOC_ICON_PREFIX + iconName;
				buffer.replace(xdoc, xdoc + XDOC_ICON.length(), icns);
			}
		}

		try {
			transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(targetFile));
			if (!ini.equals(targetFile)) {
				ini.delete();
				descriptor.replace(ini, targetFile);
			}
		} catch (FileNotFoundException e) {
			System.out.println("Impossible to brand ini file"); //$NON-NLS-1$
			return;
		} catch (IOException e) {
			System.out.println("Impossible to brand ini file"); //$NON-NLS-1$
			return;
		}
	}

	private void modifyInfoPListFile(ExecutablesDescriptor descriptor, File initialRoot, File targetRoot, String iconName) {
		File infoPList = new File(initialRoot, "Info.plist"); //$NON-NLS-1$
		InfoPListEditor infoPListEditor = null;
		try {
			infoPListEditor = InfoPListEditor.loadPListEditor(infoPList);
		} catch (IOException e) {
			System.out.println("Impossible to create info.plist editor for " + infoPList.getAbsolutePath() + ". Caused by " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}

		String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);
		infoPListEditor.setKey(InfoPListEditor.MARKER_KEY, name);
		infoPListEditor.setKey(InfoPListEditor.BUNDLE_KEY, capitalizedName);
		infoPListEditor.setKey(InfoPListEditor.BUNDLE_DISPLAYNAME_KEY, capitalizedName);
		infoPListEditor.setKey(InfoPListEditor.BUNDLE_ID_KEY, id == null ? name : id);
		if (description != null)
			infoPListEditor.setKey(InfoPListEditor.BUNDLE_INFO_KEY, description);

		if (version != null) {
			// CFBundleShortVersionString is to be 3 segments only
			// http://developer.apple.com/library/mac/documentation/General/Reference/InfoPlistKeyReference/Articles/CoreFoundationKeys.html#//apple_ref/doc/uid/20001431-111349
			StringBuffer sv = new StringBuffer(version.getSegmentCount() > 0 ? version.getSegment(0).toString() : "0"); //$NON-NLS-1$
			sv.append('.');
			sv.append(version.getSegmentCount() > 1 ? version.getSegment(1).toString() : "0"); //$NON-NLS-1$
			sv.append('.');
			sv.append(version.getSegmentCount() > 2 ? version.getSegment(2).toString() : "0"); //$NON-NLS-1$
			infoPListEditor.setKey(InfoPListEditor.BUNDLE_VERSION_KEY, version.toString());
			infoPListEditor.setKey(InfoPListEditor.BUNDLE_SHORT_VERSION_KEY, sv.toString());
		}

		if (iconName.length() > 0) {
			infoPListEditor.setKey(InfoPListEditor.ICON_KEY, iconName);
		}

		File target = new File(targetRoot, "Info.plist"); //$NON-NLS-1$;
		try {
			target.getParentFile().mkdirs();
			infoPListEditor.save(target);
		} catch (TransformerException e) {
			System.out.println("Impossible to save info.plist file " + target.getAbsolutePath()); //$NON-NLS-1$
			return;
		}
		try {
			if (!infoPList.getCanonicalFile().equals(target.getCanonicalFile()))
				infoPList.delete();
		} catch (IOException e) {
			//ignore
		}
		descriptor.replace(infoPList, target);
	}

	private int scan(StringBuffer buf, int start, String targetName) {
		return scan(buf, start, new String[] {targetName});
	}

	private int scan(StringBuffer buf, int start, String[] targets) {
		for (int i = start; i < buf.length(); i++) {
			for (int j = 0; j < targets.length; j++) {
				if (i < buf.length() - targets[j].length()) {
					String match = buf.substring(i, i + targets[j].length());
					if (targets[j].equalsIgnoreCase(match))
						return i;
				}
			}
		}
		return -1;
	}

	private StringBuffer readFile(File targetName) throws IOException {
		InputStreamReader reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(targetName)));
		StringBuffer result = new StringBuffer();
		char[] buf = new char[4096];
		int count;
		try {
			count = reader.read(buf, 0, buf.length);
			while (count != -1) {
				result.append(buf, 0, count);
				count = reader.read(buf, 0, buf.length);
			}
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignore exceptions here
			}
		}
		return result;
	}

	private void transferStreams(InputStream source, OutputStream destination) throws IOException {
		source = new BufferedInputStream(source);
		destination = new BufferedOutputStream(destination);
		try {
			byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead = -1;
				if ((bytesRead = source.read(buffer)) == -1)
					break;
				destination.write(buffer, 0, bytesRead);
			}
		} finally {
			try {
				source.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				destination.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public void setOS(String value) {
		os = value;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
