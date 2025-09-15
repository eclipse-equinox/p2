/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp, inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;

public class BlockMacUpdate extends ProvisioningAction {

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		return null;
	}

}
