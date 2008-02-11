/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.director;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.engine.Operand;

public class ProvisioningPlan {
	private IStatus status;
	private Operand[] operands;

	public ProvisioningPlan(IStatus status) {
		this(status, new Operand[0]);
	}

	public ProvisioningPlan(IStatus status, Operand[] operands) {
		this.status = status;
		this.operands = operands;
	}

	public IStatus getStatus() {
		return status;
	}

	/** 
	 * The operands to pass to the engine.
	 * @return the operands to be executed. This may be an empty array if the
	 * plan has errors or if there is nothing to do.
	 */
	public Operand[] getOperands() {
		return operands;
	}

}
