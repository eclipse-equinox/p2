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

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.Explanation.IUToInstall;
import org.eclipse.equinox.internal.provisional.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.provisional.p2.engine.Operand;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.*;

public class ProvisioningPlan {
	IStatus status;
	Operand[] operands;
	Map actualChangeRequest;
	Map sideEffectChanges;
	Set explanation;

	public ProvisioningPlan(IStatus status) {
		this(status, new Operand[0], null, null);
	}

	public ProvisioningPlan(IStatus status, Operand[] operands, Map[] actualChangeRequest, Set explanation) {
		this.status = status;
		this.operands = operands;
		if (actualChangeRequest != null) {
			this.actualChangeRequest = actualChangeRequest[0];
			this.sideEffectChanges = actualChangeRequest[1];
		}
		this.explanation = explanation;
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

	public RequestStatus getRequestStatus(IInstallableUnit iu) {
		if (actualChangeRequest == null)
			return null;
		return (RequestStatus) actualChangeRequest.get(iu);
	}

	public Map getSideEffectChanges() {
		if (sideEffectChanges == null)
			return Collections.EMPTY_MAP;
		return sideEffectChanges;
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

	public Set getExplanation() {
		return explanation;
	}

	/**
	 * To get the Root IUs that are conflicting.
	 * Note that for the moment, the set contains IRequiredCapability,
	 * not IUs.
	 * 
	 * @return an empty set if all the Root IUs are installable, one 
	 * element that cannot be installed (missing dependency?) or two 
	 * elements that cannot be installed altogether (singleton contraint?).
	 */
	public Set getUninstallableRootIUs() {
		Set set = new HashSet();
		for (Object o : explanation) {
			if (!(o instanceof Explanation.IUToInstall))
				break;
			set.add(((IUToInstall) o).iu);
		}
		return set;
	}
}
