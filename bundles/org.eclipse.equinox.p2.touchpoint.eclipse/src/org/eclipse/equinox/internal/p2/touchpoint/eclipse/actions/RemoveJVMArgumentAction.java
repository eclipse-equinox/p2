/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class RemoveJVMArgumentAction extends ProvisioningAction {
	public static final String ID = "removeJvmArg"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		String jvmArg = (String) parameters.get(ActionConstants.PARM_JVM_ARG);
		if (jvmArg == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_JVM_ARG, ID));
		removeArg(jvmArg, parameters);
		return Status.OK_STATUS;
	}

	public IStatus undo(Map<String, Object> parameters) {
		String jvmArg = (String) parameters.get(ActionConstants.PARM_JVM_ARG);
		if (jvmArg == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_JVM_ARG, ID));
		AddJVMArgumentAction.addArg(jvmArg, parameters);
		return Status.OK_STATUS;
	}

	public static IStatus removeArg(String arg, Map<String, Object> parameters) {
		LauncherData launcherData = ((Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR)).getLauncherData();
		File storageArea = (File) parameters.get(ActionConstants.PARM_PROFILE_DATA_DIRECTORY);

		try {
			if (arg.startsWith(AddJVMArgumentAction.XMS))
				removeByteArg(arg, AddJVMArgumentAction.XMS, launcherData, storageArea);
			else if (arg.startsWith(AddJVMArgumentAction.XMX))
				removeByteArg(arg, AddJVMArgumentAction.XMX, launcherData, storageArea);
			else if (arg.startsWith(AddJVMArgumentAction.XX_MAX_PERM_SIZE))
				removeByteArg(arg, AddJVMArgumentAction.XX_MAX_PERM_SIZE, launcherData, storageArea);
			else
				// Argument with a non-byte value, no special handling
				launcherData.removeJvmArg(arg);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.ID, Messages.error_processing_vmargs, e);
		} catch (IllegalArgumentException e) {
			return new Status(IStatus.ERROR, Activator.ID, Messages.error_processing_vmargs, e);
		}
		return Status.OK_STATUS;
	}

	private static void removeByteArg(String arg, String flag, LauncherData launcherData, File storageArea) throws IOException {
		Properties storedValues = AddJVMArgumentAction.load(storageArea);

		String argValue = arg.substring(flag.length());
		String currentArg = AddJVMArgumentAction.getCurrentArg(flag, launcherData.getJvmArgs());
		// Check for user changes
		AddJVMArgumentAction.detectUserValue(currentArg, flag, storedValues);
		AddJVMArgumentAction.validateValue(arg.substring(flag.length()));

		removeArg(storedValues, argValue, flag);
		launcherData.removeJvmArg(currentArg);

		// Set the argument to use & save stored values
		AddJVMArgumentAction.setToMax(flag, storedValues, launcherData);
		AddJVMArgumentAction.save(storedValues, storageArea);
	}

	private static void removeArg(Properties storage, String value, String flag) {
		String[] args = AddJVMArgumentAction.getArgs(storage, flag);
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(value)) {
				args[i] = null;
				// Stop now that we've removed a matching argument
				break;
			}
		setArgs(storage, flag, args);
	}

	private static void setArgs(Properties storedValues, String flag, String[] args) {
		if (args == null || args.length == 0)
			// Null or empty list, unset flag
			storedValues.remove(flag);
		else {
			// Build a comma separated list of values for this flag
			String argString = ""; //$NON-NLS-1$
			for (int i = 0; i < args.length; i++)
				if (args[i] != null)
					argString += args[i] + ',';

			if (argString.length() > 0)
				// Strip the trailing comma
				storedValues.setProperty(flag, argString.substring(0, argString.length() - 1));
			else
				// Array was full of null values, unset flag
				storedValues.remove(flag);
		}
	}
}