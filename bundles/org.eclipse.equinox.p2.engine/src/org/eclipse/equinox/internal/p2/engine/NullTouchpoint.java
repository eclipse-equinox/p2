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
package org.eclipse.equinox.internal.p2.engine;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.engine.Touchpoint;
import org.eclipse.equinox.p2.engine.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.TouchpointType;

/**
 * A touchpoint that performs no processing.
 */
public class NullTouchpoint extends Touchpoint {
	public static final Touchpoint INSTANCE = new NullTouchpoint();

	/**
	 * Public constructor only intended to be called by extension registry.
	 */
	public NullTouchpoint() {
		super();
	}

	public TouchpointType getTouchpointType() {
		return TouchpointType.NONE;
	}

	public boolean supports(String phaseId) {
		if (phaseId.equals("install") || phaseId.equals("uninstall"))
			return true;
		return false;
	}

	public ProvisioningAction getAction(String actionId) {
		return new ProvisioningAction() {
			public IStatus execute(Map parameters) {
				return Status.OK_STATUS;
			}

			public IStatus undo(Map parameters) {
				return Status.OK_STATUS;
			}
		};
	}
}
