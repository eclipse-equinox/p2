/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Activator;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Messages;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class LinkAction extends ProvisioningAction {
	public static final String ID = "ln"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
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

		ln(targetDir, linkTarget, linkName, Boolean.valueOf(force).booleanValue());
		return Status.OK_STATUS;
	}

	public IStatus undo(Map parameters) {
		String linkTarget = (String) parameters.get(ActionConstants.PARM_LINK_TARGET);
		String linkName = (String) parameters.get(ActionConstants.PARM_LINK_NAME);

		if (linkTarget != null && linkName != null) {
			File linkFile = new File(linkTarget, linkName);
			linkFile.delete();
		}
		return null;
	}

	private void ln(String targetDir, String linkTarget, String linkName, boolean force) {
		Runtime r = Runtime.getRuntime();
		try {
			r.exec(new String[] {"ln", "-s" + (force ? "f" : ""), linkTarget, targetDir + IPath.SEPARATOR + linkName}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		} catch (IOException e) {
			// ignore
		}
	}
}
