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

public class LinkAction extends ProvisioningAction {
	public static final String ID = "ln"; //$NON-NLS-1$

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

		String linkTarget = (String) parameters.get(ActionConstants.PARM_LINK_TARGET);
		if (linkTarget == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_LINK_TARGET, ID));

		String linkName = (String) parameters.get(ActionConstants.PARM_LINK_NAME);
		if (linkName == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_LINK_NAME, ID));

		ln(targetDir, linkTarget, linkName);
		return Status.OK_STATUS;
	}

	public IStatus undo(Map parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	private void ln(String targetDir, String linkTarget, String linkName) {
		Runtime r = Runtime.getRuntime();
		try {
			r.exec(new String[] {"ln", "-s", linkTarget, targetDir + IPath.SEPARATOR + linkName}); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
