/*******************************************************************************
 *  Copyright (c) 2015 SAP SE and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      SAP SE - initial API and implementation
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

public class CheckAndPromptNativePackageWindowsRegistry extends ProvisioningAction {
	public static final String ID = "checkAndPromptNativePackageWindowsRegistry"; //$NON-NLS-1$
	public static final String WINDOWS_DISTRO = "windows"; //$NON-NLS-1$
	private static final boolean IS_WINDOWS = java.io.File.separatorChar == '\\';
	private static final String IS_INSTALLED = "isInstalled.bat"; //$NON-NLS-1$
	private static final String SHELL = "cmd.exe"; //$NON-NLS-1$

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		//If we are not running on Windows, do nothing and return
		if (!IS_WINDOWS)
			return Status.OK_STATUS;

		//Get and check the paremeters
		String packageName = (String) parameters.get(ActionConstants.PARM_LINUX_PACKAGE_NAME);
		String packageVersion = (String) parameters.get(ActionConstants.PARM_LINUX_PACKAGE_VERSION);
		String key = (String) parameters.get(ActionConstants.PARM_WINDOWS_REGISTRY_KEY);
		String attName = (String) parameters.get(ActionConstants.PARM_WINDOWS_REGISTRY_ATTRIBUTE_NAME);
		String attValue = (String) parameters.get(ActionConstants.PARM_WINDOWS_REGISTRY_ATTRIBUTE_VALUE);

		if (key == null || (attName != null && attValue == null))
			return new Status(IStatus.ERROR, Activator.ID, Messages.Incorrect_Command);

		//Check if the desired package is installed and collect information in the touchpoint
		File scriptToExecute = NativeTouchpoint.getFileFromBundle(WINDOWS_DISTRO, IS_INSTALLED);
		if (scriptToExecute == null)
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Cannot_Find_status, WINDOWS_DISTRO));

		try {
			List<String> cmd = new ArrayList<String>(6);
			cmd.add(SHELL);
			cmd.add("/c"); //$NON-NLS-1$
			cmd.add(scriptToExecute.getAbsolutePath());
			cmd.add(key);
			if (attName != null) {
				cmd.add(attName);
				cmd.add(attValue);
			}
			int exitValue = new ProcessBuilder(cmd).start().waitFor();
			switch (exitValue) {
				case 0 :
					return Status.OK_STATUS;
				case 1 :
				case 2 :
				default :
					NativePackageEntry packageEntry = new NativePackageEntry(packageName, packageVersion, null);
					String downloadLink = (String) parameters.get(ActionConstants.PARM_DOWNLOAD_LINK);
					packageEntry.setDownloadLink(downloadLink);
					((NativeTouchpoint) getTouchpoint()).addPackageToInstall(packageEntry);
					((NativeTouchpoint) getTouchpoint()).setDistro(WINDOWS_DISTRO);
					return Status.OK_STATUS;
			}
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Cannot_Check_Package, new String[] {packageName, packageVersion, WINDOWS_DISTRO}));
		} catch (InterruptedException e) {
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Cannot_Check_Package, new String[] {packageName, packageVersion, WINDOWS_DISTRO}));
		}
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		//Nothing to do since we are not modifying any state.
		return null;
	}

}