/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.osgi.util.NLS;

//TODO: Do we need this class or is just using FileUtils.unzip sufficient?
//TODO Be careful here with the permissions.... We may need to have a proper unzip technology here that supports file permissions for linux
public class Zip {
	public File[] unzip(String source, String destination, String backupDir) {
		//		IArtifact artifact = data.getArtifact();
		//		String destination = performVariableSubstitutions(data.getDestination());
		//		if (canInstallArtifact()) {
		//TODO if artifact has isExploded==true should pass in progress monitor
		// for unzipping
		File zipFile = new File(source);
		if (zipFile == null || !zipFile.exists()) {
			// internal error?
			Util.log(this.getClass().getName() + " the files to be unzipped is not here"); //$NON-NLS-1$
			//			throw Util.coreException(null, NLS.bind(Messages.failed_to_download_artifact, source));
		}

		//		IProgressMonitor[] pm =  new SplitProgressMonitor(new NullProgressMonitor(), 2);
		//		IProgressMonitor backupPM = pm[0];
		//		IProgressMonitor unzipPM = pm[1];
		try {
			// Set Destination location
			//			Path destPath = new Path(destination);
			//			if (destPath.isAbsolute()) {
			//				destinationDir = destPath.toFile();
			//			} else {
			//				destinationDir = new File(getLocation(Profile.INSTALL_LOCATION));
			//				if (!destination.equals(".")) { //$NON-NLS-1$
			//					destinationDir = new File(destinationDir, destination);
			//				}
			//			}

			if (backupDir != null) {
				try {
					//				backupPM.beginTask(Messages.backing_up, 1);
					BackupFiles backupFiles = new BackupFiles(new File(backupDir));
					backupFiles.backupFilesInZip(backupDir, zipFile.toURL(), new File(destination), null);
				} catch (IOException e) {
					Util.log(this.getClass().getName() + " something went wrong when backing up the files"); //$NON-NLS-1$
					//				throw Util.coreException(e, NLS.bind(Messages.error_backing_up, zipFile));
				} finally {
					//				backupPM.done();
				}
			}
			try {
				String taskName = NLS.bind(Messages.unzipping, source);
				return FileUtils.unzipFile(zipFile, new File(destination), taskName, new NullProgressMonitor());
			} catch (IOException e) {
				Util.log(this.getClass().getName() + " something went wrong when unzipping"); //$NON-NLS-1$
				Util.log("zipfile: " + zipFile.getAbsolutePath()); //$NON-NLS-1$
				Util.log("destination: " + destination); //$NON-NLS-1$
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
