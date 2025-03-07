/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.natives.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class CleanupcopyAction extends ProvisioningAction {

	public static final String ACTION_CLEANUPCOPY = "cleanupcopy"; //$NON-NLS-1$

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		return cleanupcopy(parameters, true);
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		return CopyAction.copy(parameters, false);
	}

	/**
	 * Perform a cleanup of a previously made copy action.
	 * 
	 * @param parameters  action parameters
	 * @param restoreable flag indicating if the operation should be backed up
	 * @return status
	 */
	public static IStatus cleanupcopy(Map<String, Object> parameters, boolean restoreable) {
		String source = (String) parameters.get(ActionConstants.PARM_SOURCE);
		if (source == null) {
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_SOURCE, ACTION_CLEANUPCOPY));
		}
		String target = (String) parameters.get(ActionConstants.PARM_TARGET);
		if (target == null) {
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_TARGET, ACTION_CLEANUPCOPY));
		}
		IBackupStore backupStore = (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP);

		IInstallableUnit iu = (IInstallableUnit) parameters.get(ActionConstants.PARM_IU);
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);

		String copied = profile.getInstallableUnitProperty(iu, CopyAction.buildCopiedFileIUPropertyKey(target, source));

		if (copied == null) {
			return Status.OK_STATUS;
		}

		StringTokenizer tokenizer = new StringTokenizer(copied, ActionConstants.PIPE);
		List<File> directories = new ArrayList<>();
		while (tokenizer.hasMoreTokens()) {
			String fileName = tokenizer.nextToken();
			File file = new File(fileName);
			if (!file.exists()) {
				continue;
			}

			// directories need to be deleted from the bottom-up, but directories are listed
			// in traversal order during copy, so we need to reverse the directory list
			if (file.isDirectory()) {
				directories.add(0, file);
			} else {
				if (restoreable) {
					try {
						backupStore.backup(file);
					} catch (IOException e) {
						return Util.createError(NLS.bind(Messages.backup_file_failed, file));
					}
				} else {
					file.delete();
				}
			}
		}

		for (File directory : directories) {
			File[] children = directory.listFiles();
			if (children == null) {
				return Util.createError(NLS.bind(Messages.Error_list_children_0, directory));
			}

			if (children.length == 0) {
				if (restoreable) {
					try {
						backupStore.backup(directory);
					} catch (IOException e) {
						return Util.createError(NLS.bind(Messages.backup_file_failed, directory));
					}
				} else {
					directory.delete();
				}
			}
		}

		return Status.OK_STATUS;
	}

}