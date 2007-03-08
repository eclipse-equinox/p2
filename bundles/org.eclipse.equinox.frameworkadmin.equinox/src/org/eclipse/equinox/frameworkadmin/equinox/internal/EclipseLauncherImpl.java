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
package org.eclipse.equinox.frameworkadmin.equinox.internal;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.internal.frameworkadmin.utils.SimpleBundlesState;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.osgi.service.log.LogService;

public class EclipseLauncherImpl {
	static String getStringOfCmd(String[] cmdarray) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < cmdarray.length; i++) {
			sb.append(cmdarray[i]);
			sb.append(" ");
		}
		return sb.toString();
	}

	//	BundleContext context = null;

	EquinoxFwAdminImpl fwAdmin = null;

	EclipseLauncherImpl(EquinoxFwAdminImpl fwAdmin) {
		//EclipseLauncherImpl(BundleContext context, EquinoxFwAdminImpl fwAdmin) {
		//		this.context = context;
		this.fwAdmin = fwAdmin;
	}

	public Process launch(Manipulator manipulator, File cwd) throws IllegalArgumentException, IOException, FrameworkAdminRuntimeException {
		SimpleBundlesState.checkAvailability(fwAdmin);
		Log.log(LogService.LOG_DEBUG, this, "launch(Manipulator , File )", "");
		LauncherData launcherData = manipulator.getLauncherData();
		if (launcherData.getLauncher() == null)
			return launchInMemory(manipulator, cwd);
		return launchByLauncher(manipulator, cwd);
	}

	private Process launchByLauncher(Manipulator manipulator, File cwd) throws IOException {
		LauncherData launcherData = manipulator.getLauncherData();

		if (launcherData.getLauncher() == null)
			throw new IllegalStateException("launcherData.getLauncher() must be set.");
		String[] cmdarray = new String[] {launcherData.getLauncher().getAbsolutePath()};
		//		try {
		if (cwd == null)
			cwd = launcherData.getLauncher().getParentFile();
		Process process = Runtime.getRuntime().exec(cmdarray, null, cwd);
		Log.log(LogService.LOG_DEBUG, "\t" + getStringOfCmd(cmdarray));
		return process;
	}

	private Process launchInMemory(Manipulator manipulator, File cwd) throws IOException {
		LauncherData launcherData = manipulator.getLauncherData();
		Utils.checkAbsoluteFile(launcherData.getFwJar(), "fwJar");
		//		this.launcherCInfo.fwJar = fwJar;
		//		if (cwd == null)
		//			cwd = fwJar.;
		Utils.checkAbsoluteDir(cwd, "cwd");
		//		this.launcherCInfo.cwd = cwd;

		List cmdList = new LinkedList();
		if (launcherData.getJvm() != null)
			cmdList.add(launcherData.getJvm().getAbsolutePath());
		else
			cmdList.add("java");

		if (launcherData.getJvmArgs() != null)
			for (int i = 0; i < launcherData.getJvmArgs().length; i++)
				cmdList.add(launcherData.getJvmArgs()[i]);

		cmdList.add("-jar");
		cmdList.add(Utils.getRelativePath(launcherData.getFwJar(), cwd));

		//		cmdList.add(EquinoxConstants.OPTION_CONSOLE);
		//		cmdList.add("9000");
		//		cmdList.add(EquinoxConstants.OPTION_INSTANCE);
		//		cmdList.add("C:\\ws");

		EquinoxManipulatorImpl.checkConsistencyOfFwConfigLocAndFwPersistentDataLoc(launcherData);//checkConsistency(this.launcherCInfo.fwConfigFile, this.launcherCInfo.fwInstancePrivateArea);
		cmdList.add(EquinoxConstants.OPTION_CONFIGURATION);
		cmdList.add(Utils.getRelativePath(launcherData.getFwPersistentDataLocation(), cwd));

		if (launcherData.isClean())
			cmdList.add(EquinoxConstants.OPTION_CLEAN);

		String[] cmdarray = new String[cmdList.size()];
		cmdList.toArray(cmdarray);
		Log.log(LogService.LOG_DEBUG, "In CWD = " + cwd + "\n\t" + getStringOfCmd(cmdarray));
		Process process = Runtime.getRuntime().exec(cmdarray, null, cwd);
		return process;
	}
}
