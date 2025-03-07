/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *    IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

/**
 * Touchpoint action which allows the user to set the -vm parameter in the
 * eclipse.ini file.
 */
public class SetJvmAction extends ProvisioningAction {
	public static final String ID = "setJvm"; //$NON-NLS-1$

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		String jvmArg = (String) parameters.get(ActionConstants.PARM_JVM);
		if (jvmArg == null) {
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_JVM, ID));
		}
		LauncherData launcherData = ((Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR))
				.getLauncherData();
		File previous = launcherData.getJvm();
		File jvm = "null".equals(jvmArg) ? null : new File(jvmArg); //$NON-NLS-1$
		// make a backup - even if it is null
		getMemento().put(ActionConstants.PARM_PREVIOUS_VALUE, previous == null ? null : previous.getPath());
		launcherData.setJvm(jvm);
		if (jvm != null) {
			adjustWorkbenchSystemProperties(jvm);
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		String jvmArg = (String) parameters.get(ActionConstants.PARM_JVM);
		if (jvmArg == null) {
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_JVM, ID));
		}
		// make a backup - even if it is null
		String previous = (String) getMemento().get(ActionConstants.PARM_PREVIOUS_VALUE);
		LauncherData launcherData = ((Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR))
				.getLauncherData();
		final File jvm = previous == null ? null : new File(previous);
		launcherData.setJvm(jvm);
		if (jvm != null) {
			adjustWorkbenchSystemProperties(jvm);
		}
		return Status.OK_STATUS;
	}

	private static void adjustWorkbenchSystemProperties(File jvm) {
		try {
			String eclipseCommands = System.getProperty("eclipse.commands"); //$NON-NLS-1$
			if (eclipseCommands != null) {
				// set the eclipse.vm system property so the Workbench restart will use this vm
				// to restart.
				final String fullPath = jvm.getCanonicalPath();
				System.setProperty("eclipse.vm", fullPath); //$NON-NLS-1$

				// also adjust the -vm property in the eclipse.commands system property so that
				// vm is actually used.
				int index = eclipseCommands.indexOf("-vm"); //$NON-NLS-1$
				if (index != -1) {
					final String vmWithLineFeed = "-vm\n"; //$NON-NLS-1$
					// find the next line ending after -vm line.
					int index2 = eclipseCommands.indexOf('\n', index + vmWithLineFeed.length());
					if (index2 == -1) {
						eclipseCommands = eclipseCommands.substring(0, index) + vmWithLineFeed + fullPath;
					} else {
						String tmp = eclipseCommands.substring(0, index) + vmWithLineFeed + fullPath;
						eclipseCommands = tmp + eclipseCommands.substring(index2);
					}
					System.setProperty("eclipse.commands", eclipseCommands); //$NON-NLS-1$
				}
			}

		} catch (IOException e) {
			// ignore this error, should not really happen for file.getCanonicalPath() which
			// should be just installed just fine.
			// if this fails for some reason then only the restart wouldn't be using this,
			// but it wouldn't block the actual install.
		}
	}

}