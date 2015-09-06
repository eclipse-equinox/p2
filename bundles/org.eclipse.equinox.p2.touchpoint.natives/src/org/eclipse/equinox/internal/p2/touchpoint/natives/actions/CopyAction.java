/*******************************************************************************
 * Copyright (c) 2009, 2015 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.p2.touchpoint.natives.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

/**
 * Copies from PARM_COPY_SOURCE to PARAM_COPY_TARGET
 * The optional parameter PARAM_COPY_OVERWRITE overwrites and existing file if set to true, else
 * and existing file with the same name is an error. The default is false.
 * If the source is a directory, a merge copy to the target is performed.
 * Copy will copy files and directories (recursively).
 *  
 */
public class CopyAction extends ProvisioningAction {
	public static final String ID = "cp"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		return copy(parameters, true);
	}

	/** Perform the copy.
	 * 
	 * @param parameters action parameters
	 * @param restoreable  flag indicating if the operation should be backed up
	 * @return status
	 */
	public static IStatus copy(Map<String, Object> parameters, boolean restoreable) {
		String target = (String) parameters.get(ActionConstants.PARM_COPY_TARGET);
		IBackupStore backupStore = restoreable ? (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP) : null;

		if (target == null)
			return new Status(IStatus.ERROR, Activator.ID, IStatus.OK, NLS.bind(Messages.param_not_set, ActionConstants.PARM_COPY_TARGET, ID), null);

		String source = (String) parameters.get(ActionConstants.PARM_COPY_SOURCE);
		if (source == null)
			return new Status(IStatus.ERROR, Activator.ID, IStatus.OK, NLS.bind(Messages.param_not_set, ActionConstants.PARM_COPY_SOURCE, ID), null);

		String overwrite = (String) parameters.get(ActionConstants.PARM_COPY_OVERWRITE);
		Profile profile = (Profile) parameters.get(ActionConstants.PARM_PROFILE);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(ActionConstants.PARM_IU);

		String originalSource = source;
		if (source.equals(ActionConstants.PARM_AT_ARTIFACT)) {
			String artifactLocation = (String) parameters.get(NativeTouchpoint.PARM_ARTIFACT_LOCATION);
			if (artifactLocation == null) {
				IArtifactKey artifactKey = (IArtifactKey) parameters.get(NativeTouchpoint.PARM_ARTIFACT);
				return Util.createError(NLS.bind(Messages.artifact_not_available, artifactKey));
			}
			source = artifactLocation;
		}

		File sourceFile = new File(source);
		File targetFile = new File(target);
		File[] copiedFiles = null;
		try {
			copiedFiles = mergeCopy(sourceFile, targetFile, Boolean.parseBoolean(overwrite), backupStore);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.ID, IStatus.OK, NLS.bind(Messages.copy_failed, sourceFile.getPath()), e);
		}
		// keep copied file in the profile as memento for CleanupCopy
		StringBuffer copiedFileNameBuffer = new StringBuffer();
		for (int i = 0; i < copiedFiles.length; i++)
			copiedFileNameBuffer.append(copiedFiles[i].getAbsolutePath()).append(ActionConstants.PIPE);

		profile.setInstallableUnitProperty(iu, "copied" + ActionConstants.PIPE + originalSource + ActionConstants.PIPE + target, copiedFileNameBuffer.toString()); //$NON-NLS-1$

		return Status.OK_STATUS;
	}

	public IStatus undo(Map<String, Object> parameters) {
		return CleanupcopyAction.cleanupcopy(parameters, false);
	}

	/**
	 * Merge-copy file or directory.
	 * @param source
	 * @param target
	 * @param overwrite
	 * @throws IOException
	 */
	private static File[] mergeCopy(File source, File target, boolean overwrite, IBackupStore backupStore) throws IOException {
		ArrayList<File> copiedFiles = new ArrayList<File>();
		xcopy(copiedFiles, source, target, overwrite, backupStore);
		return copiedFiles.toArray(new File[copiedFiles.size()]);
	}

	/**
	 * Merge-copy file or directory.
	 * @param copiedFiles - ArrayList where copied files are collected
	 * @param source
	 * @param target
	 * @param overwrite
	 * @throws IOException
	 */
	private static void xcopy(ArrayList<File> copiedFiles, File source, File target, boolean overwrite, IBackupStore backupStore) throws IOException {
		if (!source.exists())
			throw new IOException("Source: " + source + "does not exists"); //$NON-NLS-1$//$NON-NLS-2$

		if (source.isDirectory()) {
			if (target.exists() && target.isFile()) {
				if (!overwrite)
					throw new IOException("Target: " + target + " already exists"); //$NON-NLS-1$//$NON-NLS-2$
				if (backupStore != null)
					backupStore.backup(target);
				else
					target.delete();
			}
			if (!target.exists())
				target.mkdirs();
			copiedFiles.add(target);
			File[] children = source.listFiles();
			if (children == null)
				throw new IOException("Error while retrieving children of directory: " + source); //$NON-NLS-1$
			for (int i = 0; i < children.length; i++)
				xcopy(copiedFiles, children[i], new File(target, children[i].getName()), overwrite, backupStore);
			return;
		}
		if (target.exists() && !overwrite)
			throw new IOException("Target: " + target + " already exists"); //$NON-NLS-1$//$NON-NLS-2$

		if (!target.getParentFile().exists() && !target.getParentFile().mkdirs())
			throw new IOException("Target: Path " + target.getParent() + " could not be created"); //$NON-NLS-1$//$NON-NLS-2$

		try {
			Util.copyStream(new FileInputStream(source), true, new FileOutputStream(target), true);
		} catch (IOException e) {
			// get the original IOException to the log
			e.printStackTrace();
			throw new IOException("Error while copying:" + source.getAbsolutePath()); //$NON-NLS-1$
		}
		copiedFiles.add(target);
	}
}
