/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
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

public class RemoveAction extends ProvisioningAction {
	public static final String ID = "remove"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		String path = (String) parameters.get(ActionConstants.PARM_PATH);
		if (path == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_PATH, ID));
		File file = new File(path);
		// ignore if the file is already removed
		if (!file.exists())
			return Status.OK_STATUS;
		IBackupStore store = (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP);
		if (store == null)
			return Util.createError(NLS.bind(Messages.param_not_set, NativeTouchpoint.PARM_BACKUP, ID));
		try {
			store.backupAll(file);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.ID, IStatus.OK, NLS.bind(Messages.backup_file_failed, file.getPath()), e);
		}
		return Status.OK_STATUS;
	}

	public IStatus undo(Map<String, Object> parameters) {
		// Does not have to do anything as the backup will restore what was deleted
		return Status.OK_STATUS;
	}
}