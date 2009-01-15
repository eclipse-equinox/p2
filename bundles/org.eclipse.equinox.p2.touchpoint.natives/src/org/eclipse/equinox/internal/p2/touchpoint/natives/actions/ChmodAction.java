/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.io.*;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Messages;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Util;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class ChmodAction extends ProvisioningAction {
	private static final String ACTION_CHMOD = "chmod"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		String targetDir = (String) parameters.get(ActionConstants.PARM_TARGET_DIR);
		if (targetDir == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_TARGET_DIR, ACTION_CHMOD));
		String targetFile = (String) parameters.get(ActionConstants.PARM_TARGET_FILE);
		if (targetFile == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_TARGET_FILE, ACTION_CHMOD));
		String permissions = (String) parameters.get(ActionConstants.PARM_PERMISSIONS);
		if (permissions == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_PERMISSIONS, ACTION_CHMOD));

		chmod(targetDir, targetFile, permissions);
		return Status.OK_STATUS;
	}

	public IStatus undo(Map parameters) {
		//TODO: implement undo ??
		return Status.OK_STATUS;
	}

	public void chmod(String targetDir, String targetFile, String perms) {
		Runtime r = Runtime.getRuntime();
		try {
			Process process = r.exec(new String[] {"chmod", perms, targetDir + IPath.SEPARATOR + targetFile}); //$NON-NLS-1$
			readOffStream(process.getErrorStream());
			readOffStream(process.getInputStream());
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				// mark thread interrupted and continue
				Thread.currentThread().interrupt();
			}
		} catch (IOException e) {
			// ignore
		}
	}

	private void readOffStream(InputStream inputStream) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		try {
			while (reader.readLine() != null) {
				// do nothing
			}
		} catch (IOException e) {
			// ignore
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}