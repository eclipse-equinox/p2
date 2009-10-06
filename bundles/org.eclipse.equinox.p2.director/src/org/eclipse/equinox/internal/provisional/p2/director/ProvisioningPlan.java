/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.director;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.provisional.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.provisional.p2.engine.Operand;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;

public class ProvisioningPlan {
	IStatus status;
	Operand[] operands;
	Map actualChangeRequest;
	Map sideEffectChanges;
	ProvisioningPlan installerPlan;
	RequestStatus globalRequestStatus;
	ProfileChangeRequest originalChangeRequest;
	IQueryable completeState;

	public ProvisioningPlan(IStatus status, ProfileChangeRequest originalRequest, ProvisioningPlan installerPlan) {
		this(status, new Operand[0], null, null, installerPlan, originalRequest, null);
	}

	public ProvisioningPlan(IStatus status, Operand[] operands, Map[] actualChangeRequest, RequestStatus globalStatus, ProvisioningPlan installerPlan, ProfileChangeRequest originalRequest, IQueryable futureState) {
		this.status = status;
		this.operands = operands;
		if (actualChangeRequest != null) {
			this.actualChangeRequest = actualChangeRequest[0];
			this.sideEffectChanges = actualChangeRequest[1];
		}
		this.globalRequestStatus = globalStatus;
		this.installerPlan = installerPlan;
		originalChangeRequest = originalRequest;
		if (futureState == null)
			futureState = new QueryableArray(new IInstallableUnit[0]);
		completeState = futureState;
	}

	public IStatus getStatus() {
		return status;
	}

	public ProfileChangeRequest getProfileChangeRequest() {
		return originalChangeRequest;
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

	public RequestStatus getRequestStatus() {
		return globalRequestStatus;
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

	public ProvisioningPlan getInstallerPlan() {
		return installerPlan;
	}

	public void setInstallerPlan(ProvisioningPlan p) {
		installerPlan = p;
	}

	public IQueryable getCompleteState() {
		return completeState;
	}

	protected void setCompleteState(IQueryable state) {
		completeState = state;
	}
}
