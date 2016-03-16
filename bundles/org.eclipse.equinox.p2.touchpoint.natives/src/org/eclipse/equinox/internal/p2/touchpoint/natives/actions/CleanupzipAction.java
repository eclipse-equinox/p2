/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.p2.touchpoint.natives.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class CleanupzipAction extends ProvisioningAction {

	private static final String UNZIPPED = "unzipped"; //$NON-NLS-1$
	public static final String ACTION_CLEANUPZIP = "cleanupzip"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		return cleanupzip(parameters, true);
	}

	public IStatus undo(Map<String, Object> parameters) {
		return new UnzipAction().unzip(parameters, false);
	}

	public static IStatus cleanupzip(Map<String, Object> parameters, boolean restoreable) {
		String source = (String) parameters.get(ActionConstants.PARM_SOURCE);
		if (source == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_SOURCE, ACTION_CLEANUPZIP));
		String target = (String) parameters.get(ActionConstants.PARM_TARGET);
		if (target == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_TARGET, ACTION_CLEANUPZIP));

		IInstallableUnit iu = (IInstallableUnit) parameters.get(ActionConstants.PARM_IU);
		Profile profile = (Profile) parameters.get(ActionConstants.PARM_PROFILE);

		String iuPropertyKey = UNZIPPED + ActionConstants.PIPE + source + ActionConstants.PIPE + target;

		String unzipped = profile.getInstallableUnitProperty(iu, iuPropertyKey);
		if (unzipped == null) {
			// best effort
			// we try to substitute the current target with what was written.
			Map<String, String> iuProperties = profile.getInstallableUnitProperties(iu);
			String sourcePrefix = UNZIPPED + ActionConstants.PIPE + source + ActionConstants.PIPE;
			for (String key : iuProperties.keySet()) {
				if (key.startsWith(sourcePrefix)) {
					if (unzipped == null) {
						iuPropertyKey = key;
						String storedTarget = key.substring(sourcePrefix.length());
						unzipped = substituteTarget(storedTarget, target, iuProperties.get(key));
					} else
						return Status.OK_STATUS; // possible two unzips of this source - give up on best effort
				}
			}
			// no match
			if (unzipped == null)
				return Status.OK_STATUS;
		}

		IBackupStore store = restoreable ? (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP) : null;
		StringTokenizer tokenizer = new StringTokenizer(unzipped, ActionConstants.PIPE);
		List<File> directories = new ArrayList<File>();
		while (tokenizer.hasMoreTokens()) {
			String fileName = tokenizer.nextToken();
			File file = new File(fileName);
			if (!file.exists())
				continue;

			if (file.isDirectory())
				directories.add(file);
			else {
				if (store != null)
					try {
						store.backup(file);
					} catch (IOException e) {
						return new Status(IStatus.ERROR, Activator.ID, IStatus.OK, NLS.bind(Messages.backup_file_failed, file.getPath()), e);
					}
				else
					file.delete();
			}
		}
		// sort directories by path length longest path is in top
		// this will make sure that a sub folder will be removed before its parent
		Collections.sort(directories, new Comparator<File>() {

			public int compare(File f1, File f2) {
				if (f1 == f2)
					return 0;
				if (f1 != null && f2 == null)
					return -1;
				if (f1 == null)
					return 1;
				try {
					return Integer.valueOf(f2.getCanonicalPath().length()).compareTo(f1.getCanonicalPath().length());
				} catch (IOException e) {
					// did our best. try an absolute path compare
					return Integer.valueOf(f2.getAbsolutePath().length()).compareTo(f1.getAbsolutePath().length());
				}
			}

		});
		for (File directory : directories) {
			if (store != null) {
				File[] children = directory.listFiles();
				// Since backup will deny backup of non empty directory a check must be made
				if (children == null || children.length == 0)
					try {
						store.backupDirectory(directory);
					} catch (IOException e) {
						return new Status(IStatus.ERROR, Activator.ID, IStatus.OK, NLS.bind(Messages.backup_file_failed, directory.getPath()), e);
					}
			} else
				directory.delete();
		}

		profile.removeInstallableUnitProperty(iu, iuPropertyKey);
		return Status.OK_STATUS;
	}

	private static String substituteTarget(String oldTarget, String newTarget, String value) {
		StringBuffer buffer = new StringBuffer();
		StringTokenizer tokenizer = new StringTokenizer(value, ActionConstants.PIPE);
		while (tokenizer.hasMoreTokens()) {
			String fileName = tokenizer.nextToken().trim();
			if (fileName.length() == 0)
				continue;
			if (fileName.startsWith(oldTarget))
				fileName = newTarget + fileName.substring(oldTarget.length());

			buffer.append(fileName).append(ActionConstants.PIPE);
		}
		return buffer.toString();
	}
}
