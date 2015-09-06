/*******************************************************************************
 *  Copyright (c) 2008, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.io.*;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class LinkAction extends ProvisioningAction {
	public static final String ID = "ln"; //$NON-NLS-1$
	private static final boolean WINDOWS = java.io.File.separatorChar == '\\';

	public IStatus execute(Map<String, Object> parameters) {
		String targetDir = (String) parameters.get(ActionConstants.PARM_TARGET_DIR);
		if (targetDir == null)
			return new Status(IStatus.ERROR, Activator.ID, IStatus.OK, NLS.bind(Messages.param_not_set, ActionConstants.PARM_TARGET_DIR, ID), null);

		String linkTarget = (String) parameters.get(ActionConstants.PARM_LINK_TARGET);
		if (linkTarget == null)
			return new Status(IStatus.ERROR, Activator.ID, IStatus.OK, NLS.bind(Messages.param_not_set, ActionConstants.PARM_LINK_TARGET, ID), null);

		String linkName = (String) parameters.get(ActionConstants.PARM_LINK_NAME);
		if (linkName == null)
			return new Status(IStatus.ERROR, Activator.ID, IStatus.OK, NLS.bind(Messages.param_not_set, ActionConstants.PARM_LINK_NAME, ID), null);

		String force = (String) parameters.get(ActionConstants.PARM_LINK_FORCE);

		IBackupStore store = (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP);

		try {
			ln(targetDir, linkTarget, linkName, Boolean.parseBoolean(force), store);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.ID, IStatus.OK, NLS.bind(Messages.link_failed, linkName, ID), e);
		}
		return Status.OK_STATUS;
	}

	public IStatus undo(Map<String, Object> parameters) {
		if (WINDOWS)
			return Status.OK_STATUS;
		String targetDir = (String) parameters.get(ActionConstants.PARM_TARGET_DIR);
		String linkName = (String) parameters.get(ActionConstants.PARM_LINK_NAME);

		if (targetDir != null && linkName != null) {
			File linkFile = new File(targetDir, linkName);
			linkFile.delete(); // ok since if something was overwritten - it is restored from backup
		}
		return Status.OK_STATUS;
	}

	/**
	 * Creates a link to the source file linkTarget - the created link is targetDir/linkName. 
	 * TODO: Only runs on systems with a "ln -s" command supported.
	 * TODO: Does not report errors if the "ln -s" fails
	 * @param targetDir the directory where the link is created
	 * @param linkTarget the source
	 * @param linkName the name of the created link
	 * @param force if overwrite of existing file should be performed.
	 * @param store an (optional - set to null) backup store to use
	 * @throws IOException if backup of existing file fails
	 */
	private void ln(String targetDir, String linkTarget, String linkName, boolean force, IBackupStore store) throws IOException {
		if (WINDOWS)
			return;
		// backup a file that would be overwritten using "force == true"
		if (force && store != null) {
			File xFile = new File(targetDir, linkName);
			if (xFile.exists())
				store.backup(xFile);
		}
		Runtime r = Runtime.getRuntime();
		try {
			Process process = r.exec(new String[] {"ln", "-s" + (force ? "f" : ""), linkTarget, targetDir + IPath.SEPARATOR + linkName}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			readOffStream(process.getErrorStream());
			readOffStream(process.getInputStream());
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				// mark thread interrupted and continue
				Thread.currentThread().interrupt();
			}
		} catch (IOException e) {
			// ignore
		}
	}

	private void readOffStream(InputStream inputStream) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		try {
			while (reader.readLine() != null) {
				// do nothing
			}
		} catch (IOException e) {
			// ignore
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}
