/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class ChmodAction extends ProvisioningAction {
	public static final String ID = "chmod"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		String targetDir = (String) parameters.get(ActionConstants.PARM_TARGET_DIR);
		if (targetDir == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_TARGET_DIR, ID));
		if (targetDir.equals(ActionConstants.PARM_ARTIFACT)) {
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
		String targetFile = (String) parameters.get(ActionConstants.PARM_TARGET_FILE);
		if (targetFile == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_TARGET_FILE, ID));

		String permissions = (String) parameters.get(ActionConstants.PARM_PERMISSIONS);
		if (permissions == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_PERMISSIONS, ID));

		chmod(targetDir, targetFile, permissions);
		return Status.OK_STATUS;
	}

	public IStatus undo(Map parameters) {
		//TODO: implement undo ??
		return Status.OK_STATUS;
	}

	private void chmod(String targetDir, String targetFile, String perms) {
		Runtime r = Runtime.getRuntime();
		try {
			r.exec(new String[] {"chmod", perms, targetDir + IPath.SEPARATOR + targetFile}); //$NON-NLS-1$
		} catch (IOException e) {
			// FIXME:  we should probably throw some sort of error here
		}
	}
}
