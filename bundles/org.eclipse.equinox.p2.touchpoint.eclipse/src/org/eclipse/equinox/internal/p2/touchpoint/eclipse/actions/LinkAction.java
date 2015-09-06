/*******************************************************************************
 *  Copyright (c) 2008, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.io.*;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

//This is basically a copy of the ln action in the native touchpoint only it provides @artifact support and does not support the backup store.
//We should just use the native touchpoint copy when we have a replacement for the use of @artifact in parameters
public class LinkAction extends ProvisioningAction {
	public static final String ID = "ln"; //$NON-NLS-1$
	private static final boolean WINDOWS = java.io.File.separatorChar == '\\';

	public IStatus execute(Map<String, Object> parameters) {
		String targetDir = (String) parameters.get(ActionConstants.PARM_TARGET_DIR);
		if (targetDir == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_TARGET_DIR, ID));

		if (targetDir.equals(ActionConstants.PARM_AT_ARTIFACT)) {
			try {
				targetDir = Util.resolveArtifactParam(parameters);
			} catch (CoreException e) {
				return e.getStatus();
			}
			File dir = new File(targetDir);
			if (!dir.isDirectory()) {
				return Util.createError(NLS.bind(Messages.artifact_not_directory, dir));
			}
		}

		String linkTarget = (String) parameters.get(ActionConstants.PARM_LINK_TARGET);
		if (linkTarget == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_LINK_TARGET, ID));

		String linkName = (String) parameters.get(ActionConstants.PARM_LINK_NAME);
		if (linkName == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_LINK_NAME, ID));

		String force = (String) parameters.get(ActionConstants.PARM_LINK_FORCE);

		ln(targetDir, linkTarget, linkName, Boolean.parseBoolean(force));
		return Status.OK_STATUS;
	}

	public IStatus undo(Map<String, Object> parameters) {
		return null;
	}

	/**
	 * Creates a link to the source file linkTarget - the created link is targetDir/linkName. 
	 * TODO: Only runs on systems with a "ln -s" command supported.
	 * TODO: Does not report errors if the "ln -s" fails
	 * @param targetDir the directory where the link is created
	 * @param linkTarget the source
	 * @param linkName the name of the created link
	 * @param force if overwrite of existing file should be performed.
	 */
	private void ln(String targetDir, String linkTarget, String linkName, boolean force) {
		if (WINDOWS)
			return;

		Runtime r = Runtime.getRuntime();
		try {
			Process process = r.exec(new String[] {"ln", "-s" + (force ? "f" : ""), linkTarget, targetDir + IPath.SEPARATOR + linkName}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
