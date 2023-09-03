/*******************************************************************************
 *  Copyright (c) 2007, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import java.util.stream.Stream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;

/**
 * @since 2.0
 */
public class ProvisioningPlan implements IProvisioningPlan {

	final IProfile profile;
	final List<Operand> operands = new ArrayList<>();
	final ProvisioningContext context;
	IQueryable<IInstallableUnit> futureState;
	IStatus status;
	private IProvisioningPlan installerPlan;

	public ProvisioningPlan(IProfile profile, Operand[] operands, ProvisioningContext context) {
		this(Status.OK_STATUS, profile, operands, context, null);
	}

	public ProvisioningPlan(IStatus status, IProfile profile, ProvisioningContext context, IProvisioningPlan installerPlan) {
		this(status, profile, null, context, installerPlan);
	}

	public ProvisioningPlan(IStatus status, IProfile profile, Operand[] operands, ProvisioningContext context, IProvisioningPlan installerPlan) {
		Assert.isNotNull(profile);
		this.status = status;
		this.profile = profile;
		if (operands != null)
			this.operands.addAll(Arrays.asList(operands));
		this.context = (context == null) ? new ProvisioningContext(profile.getProvisioningAgent()) : context;
		this.installerPlan = installerPlan;
	}

	@Override
	public IStatus getStatus() {
		return status;
	}

	@Override
	public void setStatus(IStatus status) {
		this.status = status;
	}

	@Override
	public IProfile getProfile() {
		return profile;
	}

	public Operand[] getOperands() {
		return operands.toArray(new Operand[operands.size()]);
	}

	@Override
	public boolean isEmpty() {
		return operands.isEmpty();
	}

	@Override
	public IQueryable<IInstallableUnit> getRemovals() {
		return new QueryablePlan(false);
	}

	@Override
	public IQueryable<IInstallableUnit> getAdditions() {
		return new QueryablePlan(true);
	}

	private class QueryablePlan implements IQueryable<IInstallableUnit> {
		private boolean addition;

		public QueryablePlan(boolean add) {
			this.addition = add;
		}

		private Stream<IInstallableUnit> installableUnits() {
			if (operands.isEmpty() || status.getSeverity() == IStatus.ERROR) {
				return Stream.empty();
			}
			return operands.stream() //
					.filter(InstallableUnitOperand.class::isInstance).map(InstallableUnitOperand.class::cast)
					.map(addition ? InstallableUnitOperand::second : InstallableUnitOperand::first)
					.filter(Objects::nonNull);
		}

		@Override
		public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
			Iterator<IInstallableUnit> units = installableUnits().iterator();
			if (!units.hasNext()) {
				return Collector.emptyCollector();
			}
			return query.perform(units);
		}

		@Override
		public boolean contains(IInstallableUnit element) {
			return installableUnits().anyMatch(iu -> Objects.equals(iu, element));
		}
	}

	@Override
	public IProvisioningPlan getInstallerPlan() {
		return installerPlan;
	}

	@Override
	public ProvisioningContext getContext() {
		return context;
	}

	@Override
	public void setInstallerPlan(IProvisioningPlan p) {
		installerPlan = p;
	}

	@Override
	public void addInstallableUnit(IInstallableUnit iu) {
		operands.add(new InstallableUnitOperand(null, iu));
	}

	@Override
	public void removeInstallableUnit(IInstallableUnit iu) {
		operands.add(new InstallableUnitOperand(iu, null));
	}

	@Override
	public void updateInstallableUnit(IInstallableUnit iu1, IInstallableUnit iu2) {
		operands.add(new InstallableUnitOperand(iu1, iu2));
	}

	@Override
	public void setProfileProperty(String name, String value) {
		String currentValue = profile.getProperty(name);
		if (value == null && currentValue == null)
			return;
		operands.add(new PropertyOperand(name, currentValue, value));
	}

	@Override
	public void setInstallableUnitProfileProperty(IInstallableUnit iu, String name, String value) {
		String currentValue = profile.getInstallableUnitProperty(iu, name);
		if (value == null && currentValue == null)
			return;
		operands.add(new InstallableUnitPropertyOperand(iu, name, currentValue, value));
	}

	@Override
	public IQueryable<IInstallableUnit> getFutureState() {
		return futureState;
	}

	@Override
	public void setFuturePlan(IQueryable<IInstallableUnit> futureState) {
		this.futureState = futureState;
	}
}
