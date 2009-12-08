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
package org.eclipse.equinox.p2.engine;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQuery;

/**
 * @since 2.0
 */
public class ProvisioningPlan implements IProvisioningPlan {
	IStatus status;
	Operand[] operands;
	Map actualChangeRequest;
	Map sideEffectChanges;
	IProvisioningPlan installerPlan;
	IStatus globalRequestStatus;
	IProfile profile;
	IQueryable completeState;
	private final ProvisioningContext context;

	public ProvisioningPlan(IProfile profile, Operand[] operands, ProvisioningContext context) {
		this(Status.OK_STATUS, operands, null, Status.OK_STATUS, null, profile, null, context);
	}

	public ProvisioningPlan(IStatus status, IProfile profile, IProvisioningPlan installerPlan, ProvisioningContext context) {
		this(status, new Operand[0], null, null, installerPlan, profile, null, null);
	}

	public ProvisioningPlan(IStatus status, Operand[] operands, Map[] actualChangeRequest, IStatus globalStatus, IProvisioningPlan installerPlan, IProfile profile, IQueryable futureState, ProvisioningContext context) {
		this.status = status;
		this.operands = operands;
		if (actualChangeRequest != null) {
			this.actualChangeRequest = actualChangeRequest[0];
			this.sideEffectChanges = actualChangeRequest[1];
		}
		this.globalRequestStatus = globalStatus;
		this.installerPlan = installerPlan;
		this.profile = profile;
		if (futureState == null) {
			futureState = new IQueryable() {
				public Collector query(IQuery query, IProgressMonitor monitor) {
					return new Collector();
				}
			};
		}
		completeState = futureState;
		if (context == null)
			context = new ProvisioningContext();
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getStatus()
	 */
	public IStatus getStatus() {
		return status;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getProfile()
	 */
	public IProfile getProfile() {
		return profile;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getOperands()
	 */
	public Operand[] getOperands() {
		return operands;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getRemovals()
	 */
	public IQueryable getRemovals() {
		return new QueryablePlan(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getAdditions()
	 */
	public IQueryable getAdditions() {
		return new QueryablePlan(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getRequestStatus(org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit)
	 */
	public IStatus getRequestStatus(IInstallableUnit iu) {
		if (actualChangeRequest == null)
			return null;
		return (IStatus) actualChangeRequest.get(iu);
	}

	public IStatus getRequestStatus() {
		return globalRequestStatus;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getSideEffectChanges()
	 */
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

		public Collector query(IQuery query, IProgressMonitor monitor) {
			Collector collector = new Collector();
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

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getInstallerPlan()
	 */
	public IProvisioningPlan getInstallerPlan() {
		return installerPlan;
	}

	public ProvisioningContext getContext() {
		return context;
	}

	public void setInstallerPlan(IProvisioningPlan p) {
		installerPlan = p;
	}

	public IQueryable getCompleteState() {
		return completeState;
	}
}
