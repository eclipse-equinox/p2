/*******************************************************************************
 *  Copyright (c) 2008, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *     IBM Corporation - ongoing development
 *     Cloudsmith Inc - ongoing development
 *     Landmark Graphics Corporation - bug 397183
 *     SAP SE - bug 465602
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class ChmodAction extends ProvisioningAction {
	private static final String ACTION_CHMOD = "chmod"; //$NON-NLS-1$
	private static final boolean WINDOWS = java.io.File.separatorChar == '\\';

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Changes the file permission attributes. This is not applicable on Windows so
	 * it is a no-op in that case.
	 * </p>
	 *
	 * <table>
	 * <tr>
	 * <td>target</td>
	 * <td>targetFile</td>
	 * <td>absoluteFiles</td>
	 * <td>validity</td>
	 * </tr>
	 * <tr>
	 * <td>Y</td>
	 * <td>Y</td>
	 * <td>Y</td>
	 * <td>can't set all these arguments</td>
	 * </tr>
	 * <tr>
	 * <td>Y</td>
	 * <td>Y</td>
	 * <td>N</td>
	 * <td>Y today, add the ability to specify a list of fileNames</td>
	 * </tr>
	 * <tr>
	 * <td>Y</td>
	 * <td>N</td>
	 * <td>Y</td>
	 * <td>warning? targetFolder unnecessary</td>
	 * </tr>
	 * <tr>
	 * <td>Y</td>
	 * <td>N</td>
	 * <td>N</td>
	 * <td>incorrect, missing targetFile</td>
	 * </tr>
	 * <tr>
	 * <td>N</td>
	 * <td>Y</td>
	 * <td>Y</td>
	 * <td>incorrect, target file can't be with absoluteFiles</td>
	 * </tr>
	 * <tr>
	 * <td>N</td>
	 * <td>Y</td>
	 * <td>N</td>
	 * <td>incorrect, missing the targetFolder</td>
	 * </tr>
	 * <tr>
	 * <td>N</td>
	 * <td>N</td>
	 * <td>Y</td>
	 * <td>Y</td>
	 * </tr>
	 * <tr>
	 * <td>N</td>
	 * <td>N</td>
	 * <td>N</td>
	 * <td>N</td>
	 * </tr>
	 * </table>
	 */
	@Override
	public IStatus execute(Map<String, Object> parameters) {
		// on Windows this isn't implemented, so we can return right here.
		if (WINDOWS) {
			return Status.OK_STATUS;
		}
		Object absoluteFiles = parameters.get(ActionConstants.PARM_ABSOLUTE_FILES); // String or String[]
		String targetDir = (String) parameters.get(ActionConstants.PARM_TARGET_DIR);
		String targetFile = (String) parameters.get(ActionConstants.PARM_TARGET_FILE);

		if (targetFile != null && absoluteFiles != null) {
			return Util.createError(Messages.chmod_param_cant_be_set_together);
		}

		if (targetDir != null && targetFile == null) {
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_TARGET_FILE, ACTION_CHMOD));
		}

		if (targetDir == null && targetFile != null) {
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_TARGET_DIR, ACTION_CHMOD));
		}

		String permissions = (String) parameters.get(ActionConstants.PARM_PERMISSIONS);
		if (permissions == null) {
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_PERMISSIONS, ACTION_CHMOD));
		}
		String optionsString = (String) parameters.get(ActionConstants.PARM_OPTIONS);

		String[] filesToProcess = absoluteFiles != null
				? ((absoluteFiles instanceof String) ? new String[] { (String) absoluteFiles }
						: (String[]) absoluteFiles)
				: makeFilesAbsolute(targetDir, targetFile);

		MultiStatus rStatus = new MultiStatus(Activator.ID, IStatus.OK,
				NLS.bind(Messages.action_0_status, ACTION_CHMOD), null);

		for (String fileToChmod : filesToProcess) {
			// Check that file exists
			try {
				File probe = new File(fileToChmod).getCanonicalFile();
				if (!probe.exists()) {
					rStatus.add(Util.createError(
							NLS.bind(Messages.action_0_failed_file_1_doesNotExist, ACTION_CHMOD, probe.toString())));
					continue;
				}
			} catch (IOException e) {
				rStatus.add(Util.createError(NLS.bind(Messages.action_0_failed_on_file_1_reason_2,
						new String[] { ACTION_CHMOD, fileToChmod, e.getMessage() }), e));
				continue;
			}
			IStatus chmodStatus = doChmod(fileToChmod, permissions, optionsString);
			if (!chmodStatus.isOK()) {
				rStatus.merge(chmodStatus);
			}
		}

		return rStatus;
	}

	private String[] makeFilesAbsolute(String targetDir, String targetFile) {
		return new String[] { new String(targetDir + IPath.SEPARATOR + targetFile) };
	}

	private IStatus doChmod(String fileToChmod, String permissions, String optionsString) {
		String options[] = null;
		if (optionsString != null) {
			ArrayList<String> collect = new ArrayList<>();
			String r = optionsString.trim();
			while (r.length() > 0) {
				int spaceIdx = r.indexOf(' ');
				if (spaceIdx < 0) {
					collect.add(r);
					r = ""; //$NON-NLS-1$
				} else {
					collect.add(r.substring(0, spaceIdx));
					r = r.substring(spaceIdx + 1);
					r = r.trim();
				}
			}
			if (collect.size() > 0) {
				options = new String[collect.size()];
				collect.toArray(options);
			}
		}

		return chmod(fileToChmod, permissions, options);
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		// TODO: implement undo ??
		return Status.OK_STATUS;
	}

	public IStatus chmod(String fileToChmod, String perms, String[] options) {
		if (WINDOWS) {
			return Status.OK_STATUS;
		}
		Runtime r = Runtime.getRuntime();
		try {
			// Note: 3 is from chmod, permissions, and target
			String[] args = new String[3 + (options == null ? 0 : options.length)];
			int i = 0;
			args[i++] = "chmod"; //$NON-NLS-1$
			if (options != null) {
				for (String option : options) {
					args[i++] = option;
				}
			}
			args[i++] = perms;
			args[i] = fileToChmod;
			Process process = r.exec(args);
			readOffStream(process.getErrorStream());
			readOffStream(process.getInputStream());
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				// mark thread interrupted and continue
				Thread.currentThread().interrupt();
			}
		} catch (IOException e) {
			return Util.createError(NLS.bind(Messages.action_0_failed_on_file_1_reason_2,
					new String[] { ACTION_CHMOD, fileToChmod, e.getMessage() }), e);
		}
		return Status.OK_STATUS;
	}

	private void readOffStream(InputStream inputStream) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			while (reader.readLine() != null) {
				// do nothing
			}
		} catch (IOException e) {
			// ignore
		}
	}
}