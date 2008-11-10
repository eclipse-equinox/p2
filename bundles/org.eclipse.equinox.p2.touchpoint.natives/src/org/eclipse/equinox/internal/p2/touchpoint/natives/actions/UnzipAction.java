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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Messages;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Util;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class UnzipAction extends ProvisioningAction {

	public static final String ACTION_UNZIP = "unzip"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		return unzip(parameters);
	}

	public IStatus undo(Map parameters) {
		return CleanupzipAction.cleanupzip(parameters);
	}

	public static IStatus unzip(Map parameters) {
		String source = (String) parameters.get(ActionConstants.PARM_SOURCE);
		if (source == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_SOURCE, ACTION_UNZIP));

		String originalSource = source;
		String target = (String) parameters.get(ActionConstants.PARM_TARGET);
		if (target == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_TARGET, ACTION_UNZIP));

		IInstallableUnit iu = (IInstallableUnit) parameters.get(ActionConstants.PARM_IU);
		Profile profile = (Profile) parameters.get(ActionConstants.PARM_PROFILE);

		if (source.equals(ActionConstants.PARM_ARTIFACT)) {
			//TODO: fix wherever this occurs -- investigate as this is probably not desired
			if (iu.getArtifacts() == null || iu.getArtifacts().length == 0)
				return Status.OK_STATUS;

			IArtifactKey artifactKey = iu.getArtifacts()[0];

			IFileArtifactRepository downloadCache;
			try {
				downloadCache = Util.getDownloadCacheRepo();
			} catch (ProvisionException e) {
				return e.getStatus();
			}
			File fileLocation = downloadCache.getArtifactFile(artifactKey);
			if ((fileLocation == null) || !fileLocation.exists())
				return Util.createError(NLS.bind(Messages.artifact_not_available, artifactKey));
			source = fileLocation.getAbsolutePath();
		}

		File[] unzippedFiles = unzip(source, target, null);
		StringBuffer unzippedFileNameBuffer = new StringBuffer();
		for (int i = 0; i < unzippedFiles.length; i++)
			unzippedFileNameBuffer.append(unzippedFiles[i].getAbsolutePath()).append(ActionConstants.PIPE);

		profile.setInstallableUnitProperty(iu, "unzipped" + ActionConstants.PIPE + originalSource + ActionConstants.PIPE + target, unzippedFileNameBuffer.toString()); //$NON-NLS-1$

		return Status.OK_STATUS;
	}

	// this is a drastically simplified version of the code that was in org.eclipse.equinox.internal.p2.touchpoint.natives (Zip, BackupFiles)
	// In particular backing up files might be useful to look at 
	private static File[] unzip(String source, String destination, String backupDir) {
		File zipFile = new File(source);
		if (zipFile == null || !zipFile.exists()) {
			Util.log(UnzipAction.class.getName() + " the files to be unzipped is not here"); //$NON-NLS-1$
		}

		try {
			String taskName = NLS.bind(Messages.unzipping, source);
			return FileUtils.unzipFile(zipFile, new File(destination), taskName, new NullProgressMonitor());
		} catch (IOException e) {
			Util.log(UnzipAction.class.getName() + " error unzipping zipfile: " + zipFile.getAbsolutePath() + "destination: " + destination); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}
}