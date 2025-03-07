/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.io.File;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Messages;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Util;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class MkdirAction extends ProvisioningAction {
	public static final String ID = "mkdir"; //$NON-NLS-1$

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		String path = (String) parameters.get(ActionConstants.PARM_PATH);
		if (path == null) {
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_PATH, ID));
		}
		File dir = new File(path);
		// A created or existing directory is ok
		dir.mkdir();
		if (dir.isDirectory()) {
			return Status.OK_STATUS;
		}
		// mkdir could have failed because of permissions, or because of an existing file
		return Util.createError(NLS.bind(Messages.mkdir_failed, path, ID));
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		String path = (String) parameters.get(ActionConstants.PARM_PATH);
		if (path == null) {
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_PATH, ID));
		}
		File dir = new File(path);
		// although not perfect, it at least prevents a faulty mkdir to delete a file on undo
		// worst case is that an empty directory could be deleted
		if (dir.isDirectory()) {
			dir.delete();
		}
		return Status.OK_STATUS;
	}
}