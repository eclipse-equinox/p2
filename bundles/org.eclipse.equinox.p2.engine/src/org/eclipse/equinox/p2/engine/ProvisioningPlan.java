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

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;

/**
 * @since 2.0
 */
public class ProvisioningPlan implements IProvisioningPlan {
	final IStatus status;
	final IProfile profile;
	final Operand[] operands;
	final ProvisioningContext context;
	private IProvisioningPlan installerPlan;

	public ProvisioningPlan(IProfile profile, Operand[] operands, ProvisioningContext context) {
		this(Status.OK_STATUS, profile, operands, context, null);
	}

	public ProvisioningPlan(IStatus status, IProfile profile, ProvisioningContext context, IProvisioningPlan installerPlan) {
		this(status, profile, new Operand[0], context, installerPlan);
	}

	public ProvisioningPlan(IStatus status, IProfile profile, Operand[] operands, ProvisioningContext context, IProvisioningPlan installerPlan) {
		this.status = status;
		this.profile = profile;
		this.operands = (operands == null) ? new Operand[0] : operands;
		this.context = (context == null) ? new ProvisioningContext() : context;
		this.installerPlan = installerPlan;
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
	public IQueryable<IInstallableUnit> getRemovals() {
		return new QueryablePlan(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getAdditions()
	 */
	public IQueryable<IInstallableUnit> getAdditions() {
		return new QueryablePlan(true);
	}

	private class QueryablePlan implements IQueryable<IInstallableUnit> {
		private boolean addition;

		public QueryablePlan(boolean add) {
			this.addition = add;
		}

		public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
			if (operands == null || status.getSeverity() == IStatus.ERROR)
				return Collector.emptyCollector();
			Collection<IInstallableUnit> list = new ArrayList<IInstallableUnit>();
			for (int i = 0; i < operands.length; i++) {
				if (!(operands[i] instanceof InstallableUnitOperand))
					continue;
				InstallableUnitOperand op = ((InstallableUnitOperand) operands[i]);
				IInstallableUnit iu = addition ? op.second() : op.first();
				if (iu != null)
					list.add(iu);
			}
			return query.perform(list.iterator());
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
}
