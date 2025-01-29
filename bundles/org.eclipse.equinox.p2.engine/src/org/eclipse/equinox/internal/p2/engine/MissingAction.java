/*******************************************************************************
 *  Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.osgi.util.NLS;

/**
 * @since 2.0
 */
public class MissingAction extends ProvisioningAction {

	private final String actionId;
	private final VersionRange versionRange;

	public MissingAction(String actionId, VersionRange versionRange) {
		this.actionId = actionId;
		this.versionRange = versionRange;
	}

	public String getActionId() {
		return actionId;
	}

	public VersionRange getVersionRange() {
		return versionRange;
	}

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		throw new IllegalArgumentException(NLS.bind(Messages.action_not_found, actionId + (versionRange == null ? "" : "/" + versionRange.toString()))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		// do nothing as we want this action to undo successfully
		return Status.OK_STATUS;
	}
}
