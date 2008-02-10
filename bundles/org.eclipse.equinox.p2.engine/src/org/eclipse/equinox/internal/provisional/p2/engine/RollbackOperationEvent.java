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
package org.eclipse.equinox.internal.provisional.p2.engine;

import org.eclipse.core.runtime.IStatus;

public class RollbackOperationEvent extends TransactionEvent {

	private static final long serialVersionUID = -2076492953949691215L;
	private IStatus cause;

	public RollbackOperationEvent(IProfile profile, PhaseSet phaseSet, InstallableUnitOperand[] deltas, IEngine engine, IStatus cause) {
		super(profile, phaseSet, deltas, engine);
		this.cause = cause;
	}

	public IStatus getStatus() {
		return cause;
	}
}
