/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin;

import java.io.File;

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
	private static final String[] NULL_STRINGS = new String[0];
	private File fwPersistentDataLocation = null;
	private File jvm = null;
	private String[] jvmArgs;
	private boolean clean;
	private File fwConfigLocation;
	private File homeLocation = null;
	private File fwJar = null;

	private File launcher = null;
	private File launcherConfigLocation = null;

	private String fwName;
	private String fwVersion;
	private String launcherName;
	private String launcherVersion;

	public LauncherData(String fwName, String fwVersion, String launcherName, String launcherVersion) {
		this.fwName = fwName;
		this.fwVersion = fwVersion;
		this.launcherName = launcherName;
		this.launcherVersion = launcherVersion;
		this.initialize();
	}

	public void addJvmArgs(String[] args) {
		if (args == null) {
			jvmArgs = NULL_STRINGS;
			return;
		}
		if (jvmArgs.length == 0)
			this.setJvmArgs(args);
		String[] newArgs = new String[jvmArgs.length + args.length];
		System.arraycopy(jvmArgs, 0, newArgs, 0, jvmArgs.length);
		System.arraycopy(args, 0, newArgs, jvmArgs.length, args.length);
		jvmArgs = newArgs;
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
		return homeLocation;
	}

	public File getJvm() {
		return jvm;
	}

	public String[] getJvmArgs() {
		return jvmArgs;
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

	public void initialize() {
		fwPersistentDataLocation = null;
		jvm = null;
		jvmArgs = NULL_STRINGS;
		clean = false;
		fwConfigLocation = null;
		fwJar = null;
		launcher = null;
	}

	public boolean isClean() {
		return clean;
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

	public void setHomeLocation(File homeLocation) {
		this.homeLocation = homeLocation;
	}

	public void setJvm(File file) {
		this.jvm = file;
	}

	public void setJvmArgs(String[] args) {
		if (args == null) {
			jvmArgs = NULL_STRINGS;
			return;
		}
		String[] jvmArgs = new String[args.length];
		System.arraycopy(args, 0, jvmArgs, 0, args.length);
	}

	public void setLauncher(File launcherFile) {
		launcher = launcherFile;
	}

	public void setLauncherConfigLocation(File launcherConfigLocation) {
		this.launcherConfigLocation = launcherConfigLocation;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Class:" + this.getClass().getName() + "\n");
		sb.append("fwName=" + this.fwName + "\n");
		sb.append("fwVersion=" + this.fwVersion + "\n");
		sb.append("launcherName=" + this.launcherName + "\n");
		sb.append("launcherVersion=" + this.launcherVersion + "\n");

		sb.append("jvm=" + this.jvm + "\n");
		if (this.jvmArgs.length == 0)
			sb.append("jvmArgs = null\n");
		else {
			sb.append("jvmArgs=\n");
			for (int i = 0; i < this.jvmArgs.length; i++)
				sb.append("\tjvmArgs[" + i + "]=" + jvmArgs[i] + "\n");
		}

		sb.append("fwConfigLocation=" + this.fwConfigLocation + "\n");
		sb.append("fwJar=" + this.fwJar + "\n");
		sb.append("fwPersistentDataLocation=" + this.fwPersistentDataLocation + "\n");
		sb.append("homeLocation=" + this.homeLocation + "\n");
		sb.append("launcher=" + this.launcher + "\n");
		sb.append("launcherConfigLocation=" + this.launcherConfigLocation + "\n");
		sb.append("clean=" + this.isClean() + "\n");

		return sb.toString();
	}
}
