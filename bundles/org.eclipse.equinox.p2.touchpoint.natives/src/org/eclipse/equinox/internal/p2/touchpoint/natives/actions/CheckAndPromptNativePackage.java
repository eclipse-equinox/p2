/*******************************************************************************
 *  Copyright (c) 2014 Rapicorp, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.natives.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class CheckAndPromptNativePackage extends ProvisioningAction {
	public static final String ID = "checkAndPromptNativePackage"; //$NON-NLS-1$
	private static final String IS_INSTALLED = "isInstalled.sh"; //$NON-NLS-1$
	private static final String IS_RUNNING = "isRunning.sh"; //$NON-NLS-1$
	private static final String SHELL = "/bin/sh"; //$NON-NLS-1$

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		//Get and check the paremeters
		String distro = (String) parameters.get(ActionConstants.PARM_LINUX_DISTRO);
		String packageName = (String) parameters.get(ActionConstants.PARM_LINUX_PACKAGE_NAME);
		String packageVersion = (String) parameters.get(ActionConstants.PARM_LINUX_PACKAGE_VERSION);
		String versionComparator = (String) parameters.get(ActionConstants.PARM_LINUX_VERSION_COMPARATOR);

		if (distro == null || packageName == null || (versionComparator != null && packageVersion == null))
			return new Status(IStatus.ERROR, Activator.ID, Messages.Incorrect_Command);
		
		distro = distro.toLowerCase();

		//If we are not running the distro we are provisioning, do nothing and return
		if (!runningDistro(distro))
			return Status.OK_STATUS;

		//Check if the desired package is installed and collect information in the touchpoint
		File scriptToExecute = NativeTouchpoint.getFileFromBundle(distro, IS_INSTALLED);
		if (scriptToExecute == null)
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Cannot_Find_status, distro));

		try {
			List<String> cmd = new ArrayList<String>(4);
			cmd.add(SHELL);
			cmd.add(scriptToExecute.getAbsolutePath());
			cmd.add(packageName);
			if (packageVersion != null) {
				if (versionComparator == null)
					versionComparator = "ge"; //$NON-NLS-1$

				cmd.add(versionComparator);
				cmd.add(packageVersion);
			}
			int exitValue = new ProcessBuilder(cmd).start().waitFor();
			switch (exitValue) {
				case 0 :
					return Status.OK_STATUS;
				case 1 :
				case 2 :
					((NativeTouchpoint) getTouchpoint()).addPackageToInstall(new NativePackageEntry(packageName, packageVersion, versionComparator));
					((NativeTouchpoint) getTouchpoint()).setDistro(distro);
					return Status.OK_STATUS;
			}
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Cannot_Check_Package, new String[] {packageName, packageVersion, distro}));
		} catch (InterruptedException e) {
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Cannot_Check_Package, new String[] {packageName, packageVersion, distro}));
		}
		return Status.OK_STATUS;
	}

	//Check if the given distro is currently being run
	protected boolean runningDistro(String distro) {
		try {
			File scriptToExecute = NativeTouchpoint.getFileFromBundle(distro, IS_RUNNING);
			if (scriptToExecute == null)
				return false;

			List<String> cmd = new ArrayList<String>(4);
			cmd.add(SHELL);
			cmd.add(scriptToExecute.getAbsolutePath());
			int exitValue = new ProcessBuilder(cmd).start().waitFor();
			if (exitValue == 0)
				return true;
			return false;
		} catch (IOException e) {
			return false;
		} catch (InterruptedException e) {
			return false;
		}
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		//Nothing to do since we are not modifying any state.
		return null;
	}

}