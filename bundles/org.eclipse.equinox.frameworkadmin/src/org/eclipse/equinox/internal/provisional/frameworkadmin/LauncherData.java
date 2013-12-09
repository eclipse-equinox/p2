/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.frameworkadmin;

import java.io.File;
import java.util.*;

/**
 * This object is instantiated by {@link Manipulator#getLauncherData()};
 * The class that keeps some parameters of the {@link Manipulator}
 *  created this object. The manipulating of the parameters will affect
 *  the  {@link Manipulator}.
 *  
 * 
 * @see Manipulator
 */
public class LauncherData {
	private File fwPersistentDataLocation = null;
	private File jvm = null;
	private List<String> jvmArgs = new LinkedList<String>();
	private List<String> programArgs = new LinkedList<String>();

	private boolean clean;
	private File fwConfigLocation;
	private File home = null;
	private File fwJar = null;

	private File launcher = null;
	private File launcherConfigLocation = null;

	private String fwName;
	private String fwVersion;
	private String launcherName;
	private String launcherVersion;
	private String os;

	public LauncherData(String fwName, String fwVersion, String launcherName, String launcherVersion) {
		this.fwName = fwName;
		this.fwVersion = fwVersion;
		this.launcherName = launcherName;
		this.launcherVersion = launcherVersion;
		this.initialize();
	}

	public void addJvmArg(String arg) {
		if (arg == null)
			return;
		jvmArgs.add(arg);
	}

	public void addProgramArg(String arg) {
		if (arg == null)
			return;
		programArgs.add(arg);
	}

	public File getFwConfigLocation() {
		return fwConfigLocation;
	}

	public File getFwJar() {
		return fwJar;
	}

	public String getFwName() {
		return fwName;
	}

	public File getFwPersistentDataLocation() {
		return fwPersistentDataLocation;
	}

	public String getFwVersion() {
		return fwVersion;
	}

	public File getHome() {
		return home;
	}

	public File getJvm() {
		return jvm;
	}

	public String[] getJvmArgs() {
		String[] args = new String[jvmArgs.size()];
		jvmArgs.toArray(args);
		return args;
	}

	public File getLauncher() {
		return launcher;
	}

	public File getLauncherConfigLocation() {
		return launcherConfigLocation;
	}

	public String getLauncherName() {
		return launcherName;
	}

	public String getLauncherVersion() {
		return launcherVersion;
	}

	public String[] getProgramArgs() {
		String[] args = new String[programArgs.size()];
		programArgs.toArray(args);
		return args;
	}

	public void initialize() {
		fwPersistentDataLocation = null;
		jvm = null;
		jvmArgs.clear();
		programArgs.clear();
		clean = false;
		fwConfigLocation = null;
		fwJar = null;
		launcher = null;
	}

	public boolean isClean() {
		return clean;
	}

	public void removeJvmArg(String arg) {
		jvmArgs.remove(arg);
	}

	public void removeProgramArg(String arg) {
		// We want to handle program args as key/value pairs subsequently 
		// a key MUST start with a "-", all other args are ignored. For 
		// backwards compatibility we remove all program args until the 
		// next program arg key 
		// (see bug 253862)
		if (!arg.startsWith("-")) //$NON-NLS-1$
			return;

		int index = programArgs.indexOf(arg);
		if (index == -1)
			return;

		programArgs.remove(index);
		while (index < programArgs.size()) {
			String next = programArgs.get(index);
			if (next.charAt(0) == '-')
				return;
			programArgs.remove(index);
		}
	}

	public void setFwConfigLocation(File fwConfigLocation) {
		this.fwConfigLocation = fwConfigLocation;
	}

	public void setFwJar(File fwJar) {
		this.fwJar = fwJar;
	}

	public void setFwPersistentDataLocation(File fwPersistentDataLocation, boolean clean) {
		this.fwPersistentDataLocation = fwPersistentDataLocation;
		this.clean = clean;
	}

	public void setHome(File home) {
		this.home = home;
	}

	public void setJvm(File file) {
		this.jvm = file;
		if (file == null)
			removeProgramArg("-vm"); //$NON-NLS-1$
	}

	public void setJvmArgs(String[] args) {
		if (args == null || args.length == 0) {
			jvmArgs.clear();
			return;
		}
		for (int i = 0; i < args.length; i++)
			this.addJvmArg(args[i]);
	}

	public void setLauncher(File launcherFile) {
		launcher = launcherFile;
	}

	public void setLauncherConfigLocation(File launcherConfigLocation) {
		this.launcherConfigLocation = launcherConfigLocation;
	}

	public void setOS(String os) {
		this.os = os;
	}

	public String getOS() {
		return os;
	}

	public void setProgramArgs(String[] args) {
		if (args == null || args.length == 0) {
			programArgs.clear();
			return;
		}
		for (int i = 0; i < args.length; i++)
			this.addProgramArg(args[i]);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Class:" + this.getClass().getName() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("fwName=" + this.fwName + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("fwVersion=" + this.fwVersion + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("launcherName=" + this.launcherName + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("launcherVersion=" + this.launcherVersion + "\n"); //$NON-NLS-1$ //$NON-NLS-2$

		sb.append("jvm=" + this.jvm + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.jvmArgs.size() == 0)
			sb.append("jvmArgs = null\n"); //$NON-NLS-1$
		else {
			sb.append("jvmArgs=\n"); //$NON-NLS-1$
			int i = 0;
			for (Iterator<String> iterator = jvmArgs.iterator(); iterator.hasNext(); iterator.next())
				sb.append("\tjvmArgs[" + i++ + "]=" + iterator + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		}
		if (this.programArgs.size() == 0)
			sb.append("programArgs = null\n"); //$NON-NLS-1$
		else {
			sb.append("programArgs=\n"); //$NON-NLS-1$
			int i = 0;
			for (Iterator<String> iterator = programArgs.iterator(); iterator.hasNext(); iterator.next())
				sb.append("\tprogramArgs[" + i++ + "]=" + iterator + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		sb.append("fwConfigLocation=" + this.fwConfigLocation + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("fwJar=" + this.fwJar + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("fwPersistentDataLocation=" + this.fwPersistentDataLocation + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("home=" + this.home + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("launcher=" + this.launcher + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("launcherConfigLocation=" + this.launcherConfigLocation + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("clean=" + this.isClean() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$

		return sb.toString();
	}
}
