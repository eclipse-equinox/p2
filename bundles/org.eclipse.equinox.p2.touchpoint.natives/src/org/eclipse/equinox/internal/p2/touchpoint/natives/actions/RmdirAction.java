/*******************************************************************************
 *  Copyright (c) 2008, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.natives.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class RmdirAction extends ProvisioningAction {
	public static final String ID = "rmdir"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		String path = (String) parameters.get(ActionConstants.PARM_PATH);
		if (path == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_PATH, ID));

		IBackupStore store = (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP);

		File dir = new File(path);
		if (!dir.isDirectory())
			return Util.createError(NLS.bind(Messages.rmdir_failed, path, ID));
		if (store != null)
			try {
				store.backupDirectory(dir);
			} catch (IOException e) {
				// Only return a warning here, not an error. See Bug 331609 for more detail.
				return new Status(IStatus.WARNING, Activator.ID, IStatus.OK, NLS.bind(Messages.rmdir_failed, path, ID), e);
			} catch (IllegalArgumentException e) {
				// Ignore the delete/backup if the directory was not empty as this preserves the
				// the original semantics. See Bug 272312 for more detail.
			}
		else
			dir.delete();
		return Status.OK_STATUS;
	}

	public IStatus undo(Map<String, Object> parameters) {
		String path = (String) parameters.get(ActionConstants.PARM_PATH);
		IBackupStore store = (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP);
		if (path == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_PATH, ID));
		// only need to create a dir if backup was not used
		if (store == null)
			new File(path).mkdir();
		return Status.OK_STATUS;
	}
}