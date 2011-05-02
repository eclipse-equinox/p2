/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class AddJVMArgumentAction extends ProvisioningAction {
	public static final String ID = "addJvmArg"; //$NON-NLS-1$
	protected static final String STORAGE = "org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions" + File.separator + "jvmargs"; //$NON-NLS-1$//$NON-NLS-2$

	protected static final String XMX = "-Xmx"; //$NON-NLS-1$
	protected static final String XMS = "-Xms"; //$NON-NLS-1$
	protected static final String XX_MAX_PERM_SIZE = "-XX:MaxPermSize="; //$NON-NLS-1$
	protected static final String PREFIX_USER_VALUE = "eclipse.userDefined:"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		String jvmArg = (String) parameters.get(ActionConstants.PARM_JVM_ARG);
		if (jvmArg == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_JVM_ARG, ID));
		return addArg(jvmArg, parameters);
	}

	public IStatus undo(Map<String, Object> parameters) {
		String jvmArg = (String) parameters.get(ActionConstants.PARM_JVM_ARG);
		if (jvmArg == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_JVM_ARG, ID));
		return RemoveJVMArgumentAction.removeArg(jvmArg, parameters);
	}

	protected static String getUserArg(Properties storedValues, String flag) {
		return storedValues.getProperty(PREFIX_USER_VALUE + flag);
	}

	protected static IStatus addArg(String arg, Map<String, Object> parameters) {
		LauncherData launcherData = ((Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR)).getLauncherData();
		File storageArea = (File) parameters.get(ActionConstants.PARM_PROFILE_DATA_DIRECTORY);
		try {
			if (arg.startsWith(XMS))
				addByteArg(arg, XMS, launcherData, storageArea);
			else if (arg.startsWith(XMX))
				addByteArg(arg, XMX, launcherData, storageArea);
			else if (arg.startsWith(XX_MAX_PERM_SIZE))
				addByteArg(arg, XX_MAX_PERM_SIZE, launcherData, storageArea);
			else
				// Argument with a non-byte value, no special handling
				launcherData.addJvmArg(arg);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.ID, Messages.error_processing_vmargs, e);
		} catch (IllegalArgumentException e) {
			return new Status(IStatus.ERROR, Activator.ID, Messages.error_processing_vmargs, e);
		}
		return Status.OK_STATUS;
	}

	protected static void addByteArg(String arg, String flag, LauncherData launcherData, File storageArea) throws IOException {
		Properties storedValues = load(storageArea);
		String currentArg = getCurrentArg(flag, launcherData.getJvmArgs());

		// Check for user changes
		detectUserValue(currentArg, flag, storedValues);
		validateValue(arg.substring(flag.length()));

		rememberArg(storedValues, arg.substring(flag.length()), flag);
		launcherData.removeJvmArg(currentArg);

		// Set the argument to use & save stored values
		setToMax(flag, storedValues, launcherData);
		save(storedValues, storageArea);
	}

	// Throws exception if the argument is not a valid byte argument 
	protected static void validateValue(String arg) {
		getByteValue(arg, getBytePower(arg));
	}

	// Determine if the user has changed config, if so save their value
	protected static void detectUserValue(String currentValue, String flag, Properties storedValues) {
		String maxValue = getMaxValue(getArgs(storedValues, flag));

		if (currentValue == null)
			// User has removed value from file
			setUserArg(storedValues, flag, null);
		else if (maxValue == null || !maxValue.equals(currentValue.substring(flag.length())))
			// User has set an initial value, or modified the file 
			setUserArg(storedValues, flag, currentValue.substring(flag.length()));
	}

	protected static String getMaxValue(String[] values) {
		if (values == null || values.length == 0)
			return null;

		int max = 0;
		for (int i = 1; i < values.length; i++)
			if (compareSize(values[max], values[i]) < 0)
				max = i;
		return values[max];
	}

	protected static void setToMax(String flag, Properties storedValues, LauncherData launcherData) {
		String maxStored = getMaxValue(getArgs(storedValues, flag));
		String userDefined = AddJVMArgumentAction.getUserArg(storedValues, flag);

		if (maxStored != null || userDefined != null) {
			// Replacement is available either stored, or user defined
			if (maxStored == null)
				launcherData.addJvmArg(flag + userDefined);
			else if (userDefined == null)
				launcherData.addJvmArg(flag + maxStored);
			else if (AddJVMArgumentAction.compareSize(maxStored, userDefined) > 0)
				launcherData.addJvmArg(flag + maxStored);
			else
				launcherData.addJvmArg(flag + userDefined);
		}
	}

	// Returns:  1 when a>b, 0 when a=b, -1 when a<b
	protected static int compareSize(String a, String b) {
		double aVal, bVal;
		int aPower = getBytePower(a);
		int bPower = getBytePower(b);

		aVal = getByteValue(a, aPower);
		bVal = getByteValue(b, bPower);
		// Ensure a value is expressed with the highest power (e.g. 2G not 2048M) 
		while (aVal > 1024) {
			aVal /= 1024;
			aPower += 10;
		}
		while (bVal > 1024) {
			bVal /= 1024;
			bPower += 10;
		}

		if (aPower > bPower && aVal != 0)
			return 1;
		else if (aPower < bPower && bVal != 0)
			return -1;
		// Both have same power, so direct comparison 
		else if (aVal > bVal)
			return 1;
		else if (aVal < bVal)
			return -1;
		else
			return 0;
	}

	// Parse the numeric portion of an argument
	private static double getByteValue(String arg, int power) {
		try {
			if (power == 0)
				return Integer.parseInt(arg);
			return Integer.parseInt(arg.substring(0, arg.length() - 1));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(NLS.bind(Messages.invalid_byte_format, arg));
		}
	}

	private static int getBytePower(String arg) {
		// If last digit determines if the value is in bytes, 
		// kilobytes, megabytes, or gigabytes 
		switch (arg.charAt(arg.length() - 1)) {
			case 'k' :
			case 'K' :
				return 10;
			case 'm' :
			case 'M' :
				return 20;
			case 'g' :
			case 'G' :
				return 30;
			default :
				return 0;
		}
	}

	// Get the current used argument if there is one 
	protected static String getCurrentArg(String flag, String[] jvmArgs) {
		for (int i = 0; i < jvmArgs.length; i++)
			if (jvmArgs[i] != null && jvmArgs[i].startsWith(flag))
				return jvmArgs[i];
		return null;
	}

	// Add one new value to those stored
	protected static void rememberArg(Properties storedValues, String value, String flag) {
		String argString = storedValues.getProperty(flag);

		if (argString == null)
			argString = ""; //$NON-NLS-1$
		else if (argString.length() > 0)
			argString += ',';

		argString += value;

		if (argString.length() != 0)
			storedValues.put(flag, argString);
	}

	protected static String[] getArgs(Properties storage, String flag) {
		String argString = storage.getProperty(flag);
		if (argString == null || argString.length() == 0)
			return new String[0];

		List<String> list = new ArrayList<String>();
		int i = 0;
		String arg = ""; //$NON-NLS-1$

		// Split comma-separated list into a List
		while (i < argString.length()) {
			char c = argString.charAt(i++);
			if (c == ',') {
				list.add(arg);
				arg = ""; //$NON-NLS-1$
			} else
				arg += c;
		}

		list.add(arg);
		return list.toArray(new String[list.size()]);
	}

	// Store a single user argument, null removes stored values
	private static void setUserArg(Properties storage, String flag, String value) {
		if (value == null)
			storage.remove(PREFIX_USER_VALUE + flag);
		else
			storage.setProperty(PREFIX_USER_VALUE + flag, value);
	}

	protected static Properties load(File storageArea) throws IOException {
		Properties args = new Properties();
		File file = new File(storageArea, STORAGE);
		if (file.exists()) {
			// Only load if file already exists
			FileInputStream in = null;
			try {
				try {
					in = new FileInputStream(file);
					args.load(in);
				} finally {
					if (in != null)
						in.close();
				}
			} catch (FileNotFoundException e) {
				// Should not occur as we check to see if it exists
			}
		}
		return args;
	}

	protected static void save(Properties args, File storageArea) throws IOException {
		FileOutputStream out = null;
		File file = new File(storageArea, STORAGE);
		if (!file.exists())
			// Ensure parent directory exists 
			file.getParentFile().mkdirs();

		try {
			try {
				out = new FileOutputStream(file);
				args.store(out, null);
			} finally {
				if (out != null)
					out.close();
			}
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(NLS.bind(Messages.unable_to_open_file, file));
		}
	}
}