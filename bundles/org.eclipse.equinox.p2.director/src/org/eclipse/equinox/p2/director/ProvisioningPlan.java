/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.director;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.Operand;
import org.eclipse.equinox.p2.engine.PropertyOperand;

public class ProvisioningPlan {
	private IStatus status;
	private Operand[] operands;
	private PropertyOperand[] propertyOperands;

	public ProvisioningPlan(IStatus status) {
		this(status, new Operand[0], new PropertyOperand[0]);
	}

	public ProvisioningPlan(IStatus status, Operand[] operands) {
		this(status, operands, new PropertyOperand[0]);
	}

	public ProvisioningPlan(IStatus status, Operand[] operands, PropertyOperand[] propertyOperands) {
		this.status = status;
		this.operands = operands;
		this.propertyOperands = propertyOperands;
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

	/** 
	 * The property operands to pass to the engine.
	 * @return the property operands to be executed. This may be an empty array if the
	 * plan has errors or if there is nothing to do.
	 */
	public PropertyOperand[] getPropertyOperands() {
		return propertyOperands;
	}
}
