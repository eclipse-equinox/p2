/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Landmark Graphics Corporation - bug 397183
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.p2.touchpoint.natives.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.engine.spi.Value;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class UnzipAction extends ProvisioningAction {

	public static final String ACTION_UNZIP = "unzip"; //$NON-NLS-1$
	private Value<String[]> result;

	public IStatus execute(Map<String, Object> parameters) {
		return unzip(parameters, true);
	}

	public IStatus undo(Map<String, Object> parameters) {
		return CleanupzipAction.cleanupzip(parameters, false);
	}

	/**
	 * Unzip as directed by parameters.
	 * Record what was zipped in the profile.
	 * @param parameters
	 * @param restoreable - if the unzip should be backed up
	 * @return status
	 */
	public IStatus unzip(Map<String, Object> parameters, boolean restoreable) {
		String source = (String) parameters.get(ActionConstants.PARM_SOURCE);
		if (source == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_SOURCE, ACTION_UNZIP));

		String originalSource = source;
		String target = (String) parameters.get(ActionConstants.PARM_TARGET);
		if (target == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_TARGET, ACTION_UNZIP));

		IInstallableUnit iu = (IInstallableUnit) parameters.get(ActionConstants.PARM_IU);
		Profile profile = (Profile) parameters.get(ActionConstants.PARM_PROFILE);

		if (source.equals(ActionConstants.PARM_AT_ARTIFACT)) {
			String artifactLocation = (String) parameters.get(NativeTouchpoint.PARM_ARTIFACT_LOCATION);
			if (artifactLocation == null) {
				IArtifactKey artifactKey = (IArtifactKey) parameters.get(NativeTouchpoint.PARM_ARTIFACT);
				return Util.createError(NLS.bind(Messages.artifact_not_available, artifactKey));
			}
			source = artifactLocation;
		}
		IBackupStore store = restoreable ? (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP) : null;

		String path = (String) parameters.get(ActionConstants.PARM_PATH);
		String includePattern = (String) parameters.get(ActionConstants.PARM_INCLUDE);
		String excludePattern = (String) parameters.get(ActionConstants.PARM_EXCLUDE);

		File[] unzippedFiles = unzip(source, target, path, includePattern, excludePattern, store);
		String[] filesAsString = new String[unzippedFiles.length];
		for (int i = 0; i < unzippedFiles.length; i++) {
			filesAsString[i] = unzippedFiles[i].getAbsolutePath();
		}
		result = new Value<String[]>(filesAsString);

		StringBuffer unzippedFileNameBuffer = new StringBuffer();
		for (int i = 0; i < unzippedFiles.length; i++)
			unzippedFileNameBuffer.append(unzippedFiles[i].getAbsolutePath()).append(ActionConstants.PIPE);

		profile.setInstallableUnitProperty(iu, "unzipped" + ActionConstants.PIPE + originalSource + ActionConstants.PIPE + target, unzippedFileNameBuffer.toString()); //$NON-NLS-1$

		return Status.OK_STATUS;
	}

	/**
	 * Unzips a source zip into the given destination. Any existing contents in the destination
	 * are backed up in the provided backup store.
	 */
	private static File[] unzip(String source, String destination, String path, String includePattern, String excludePattern, IBackupStore store) {
		File zipFile = new File(source);
		if (zipFile == null || !zipFile.exists()) {
			Util.log(UnzipAction.class.getName() + " the files to be unzipped is not here"); //$NON-NLS-1$
		}
		try {
			String taskName = NLS.bind(Messages.unzipping, source);
			String[] includes = includePattern == null ? null : includePattern.split("\\s+"); //$NON-NLS-1$
			String[] excludes = excludePattern == null ? null : excludePattern.split("\\s+"); //$NON-NLS-1$
			return Util.unzipFile(zipFile, new File(destination), path, includes, excludes, store, taskName, new NullProgressMonitor());
		} catch (IOException e) {
			Util.log(UnzipAction.class.getName() + " error unzipping zipfile: " + zipFile.getAbsolutePath() + "destination: " + destination); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return new File[0];
	}

	@Override
	public Value<String[]> getResult() {
		return result;
	}
}