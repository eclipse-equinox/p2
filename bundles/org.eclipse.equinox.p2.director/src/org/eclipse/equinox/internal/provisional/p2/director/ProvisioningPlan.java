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

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.provisional.p2.engine.Operand;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.*;

public class ProvisioningPlan {
	IStatus status;
	Operand[] operands;

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

	public IQueryable getRemovals() {
		return new QueryablePlan(false);
	}

	public IQueryable getAdditions() {
		return new QueryablePlan(true);
	}

	private class QueryablePlan implements IQueryable {
		private boolean addition;

		public QueryablePlan(boolean add) {
			this.addition = add;
		}

		public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
			if (operands == null || status.getSeverity() == IStatus.ERROR)
				return collector;
			Collection list = new ArrayList();
			for (int i = 0; i < operands.length; i++) {
				if (!(operands[i] instanceof InstallableUnitOperand))
					continue;
				InstallableUnitOperand op = ((InstallableUnitOperand) operands[i]);
				IInstallableUnit iu = addition ? op.second() : op.first();
				if (iu != null)
					list.add(iu);
			}
			return query.perform(list.iterator(), collector);
		}
	}
}
