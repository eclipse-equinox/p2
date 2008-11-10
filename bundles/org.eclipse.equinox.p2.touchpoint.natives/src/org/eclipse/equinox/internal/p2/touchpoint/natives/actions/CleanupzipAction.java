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
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Messages;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Util;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class CleanupzipAction extends ProvisioningAction {

	public static final String ACTION_CLEANUPZIP = "cleanupzip"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		return cleanupzip(parameters);
	}

	public IStatus undo(Map parameters) {
		return UnzipAction.unzip(parameters);
	}

	public static IStatus cleanupzip(Map parameters) {
		String source = (String) parameters.get(ActionConstants.PARM_SOURCE);
		if (source == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_SOURCE, ACTION_CLEANUPZIP));
		String target = (String) parameters.get(ActionConstants.PARM_TARGET);
		if (target == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_TARGET, ACTION_CLEANUPZIP));

		IInstallableUnit iu = (IInstallableUnit) parameters.get(ActionConstants.PARM_IU);
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);

		String unzipped = profile.getInstallableUnitProperty(iu, "unzipped" + ActionConstants.PIPE + source + ActionConstants.PIPE + target); //$NON-NLS-1$

		if (unzipped == null)
			return Status.OK_STATUS;

		StringTokenizer tokenizer = new StringTokenizer(unzipped, ActionConstants.PIPE);
		List directories = new ArrayList();
		while (tokenizer.hasMoreTokens()) {
			String fileName = tokenizer.nextToken();
			File file = new File(fileName);
			if (!file.exists())
				continue;

			if (file.isDirectory())
				directories.add(file);
			else
				file.delete();
		}

		for (Iterator it = directories.iterator(); it.hasNext();) {
			File directory = (File) it.next();
			directory.delete();
		}

		return Status.OK_STATUS;
	}

}