/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives;

import java.io.File;
import java.io.IOException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.osgi.util.NLS;

//TODO: Do we need this class or is just using FileUtils.unzip sufficient?
//TODO Be careful here with the permissions.... We may need to have a proper unzip technology here that supports file permissions for linux
public class Zip {
	private void log(String message) {
		LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message, null));
	}

	public File[] unzip(String source, String destination, String backupDir) {
		//		IArtifact artifact = data.getArtifact();
		//		String destination = performVariableSubstitutions(data.getDestination());
		//		if (canInstallArtifact()) {
		//TODO if artifact has isExploded==true should pass in progress monitor
		// for unzipping
		File zipFile = new File(source);
		if (zipFile == null || !zipFile.exists()) {
			// internal error?
			log(this.getClass().getName() + " the files to be unzipped is not here"); //$NON-NLS-1$
			//			throw Util.coreException(null, NLS.bind(Messages.failed_to_download_artifact, source));
		}

		try {
			if (backupDir != null) {
				try {
					//				backupPM.beginTask(Messages.backing_up, 1);
					BackupFiles backupFiles = new BackupFiles(new File(backupDir));
					backupFiles.backupFilesInZip(backupDir, zipFile.toURL(), new File(destination), null);
				} catch (IOException e) {
					log(this.getClass().getName() + " error backing up the files"); //$NON-NLS-1$
					//				throw Util.coreException(e, NLS.bind(Messages.error_backing_up, zipFile));
				} finally {
					//				backupPM.done();
				}
			}
			try {
				String taskName = NLS.bind(Messages.unzipping, source);
				return FileUtils.unzipFile(zipFile, new File(destination), taskName, new NullProgressMonitor());
			} catch (IOException e) {
				log(this.getClass().getName() + " error unzipping zipfile: " + zipFile.getAbsolutePath() + "destination: " + destination); //$NON-NLS-1$ //$NON-NLS-2$
				//				throw Util.coreException(e.getMessage());
			} finally {
				//				unzipPM.done();
			}
		} finally {
			//			monitor.done();
		}
		//		}
		return null;
	}
}
