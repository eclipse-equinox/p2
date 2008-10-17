/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.natives.*;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class ChmodAction extends ProvisioningAction {
	public IStatus execute(Map parameters) {
		String targetDir = (String) parameters.get(NativeTouchpoint.PARM_TARGET_DIR);
		if (targetDir == null)
			return NativeTouchpoint.createError(NLS.bind(Messages.param_not_set, NativeTouchpoint.PARM_TARGET_DIR, NativeTouchpoint.ACTION_CHMOD));
		String targetFile = (String) parameters.get(NativeTouchpoint.PARM_TARGET_FILE);
		if (targetFile == null)
			return NativeTouchpoint.createError(NLS.bind(Messages.param_not_set, NativeTouchpoint.PARM_TARGET_FILE, NativeTouchpoint.ACTION_CHMOD));
		String permissions = (String) parameters.get(NativeTouchpoint.PARM_PERMISSIONS);
		if (permissions == null)
			return NativeTouchpoint.createError(NLS.bind(Messages.param_not_set, NativeTouchpoint.PARM_PERMISSIONS, NativeTouchpoint.ACTION_CHMOD));

		new Permissions().chmod(targetDir, targetFile, permissions);
		return Status.OK_STATUS;
	}

	public IStatus undo(Map parameters) {
		//TODO: implement undo ??
		return Status.OK_STATUS;
	}
}