/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.director;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQueryable;

public class PlannerStatus implements IStatus {

	private final IStatus status;
	private final RequestStatus globalRequestStatus;
	private final Map<IInstallableUnit, RequestStatus> requestChanges;
	private final Map<IInstallableUnit, RequestStatus> requestSideEffects;
	private final IQueryable<IInstallableUnit> plannedState;

	private static final IQueryable<IInstallableUnit> EMPTY_IU_QUERYABLE = (query, monitor) -> Collector.emptyCollector();

	public PlannerStatus(IStatus status, RequestStatus globalRequestStatus, Map<IInstallableUnit, RequestStatus> requestChanges, Map<IInstallableUnit, RequestStatus> requestSideEffects, IQueryable<IInstallableUnit> plannedState) {
		this.status = status;
		this.globalRequestStatus = globalRequestStatus;
		this.requestChanges = requestChanges;
		this.requestSideEffects = requestSideEffects;
		this.plannedState = (plannedState == null) ? EMPTY_IU_QUERYABLE : plannedState;
	}

	/**
	 * Returns a request status object containing additional global details on the planning of the request
	 * 
	 * @return An IStatus object with global details on the planning process
	 */
	public RequestStatus getRequestStatus() {
		return globalRequestStatus;
	}

	/**
	 * Returns a map of the problems associated with changes to the given installable unit
	 * in this plan. A status with severity {@link IStatus#OK} is returned if the unit
	 * can be provisioned successfully
	 * 
	 * @return A map of {@link IInstallableUnit} to {@link IStatus} of the requested 
	 * changes and their corresponding explanation.
	 */
	public Map<IInstallableUnit, RequestStatus> getRequestChanges() {
		return requestChanges;
	}

	/**
	 * Returns a map of side-effects that will occur as a result of the plan being executed.
	 * Side-effects of an install may include:
	 * <ul>
	 * <li>Optional software being installed that will become satisfied once the plan
	 * is executed.</li>
	 * <li>Optional software currently in the profile that will be uninstalled as a result
	 * of the plan being executed. This occurs when the optional software has dependencies
	 * that are incompatible with the software being installed.
	 * This includes additional software that will be installed as a result of the change,
	 * or optional changes and their corresponding explanation.
	 * @return A map of {@link IInstallableUnit} to {@link IStatus} of the additional side effect
	 * status, or <code>null</code> if there are no side effects.
	 */
	public Map<IInstallableUnit, RequestStatus> getRequestSideEffects() {
		return requestSideEffects;
	}

	/**
	 * Returns the set of InstallableUnits that make up the expected planned state in terms 
	 * of additions and removals to the profile based on the planning process. 
	 * 
	 * @return An IQueryable of the InstallableUnits in the planned state. 
	 */
	public IQueryable<IInstallableUnit> getPlannedState() {
		return plannedState;
	}

	// Remaining Methods Delegate to wrapped Status 
	@Override
	public IStatus[] getChildren() {
		return status.getChildren();
	}

	@Override
	public int getCode() {
		return status.getCode();
	}

	@Override
	public Throwable getException() {
		return status.getException();
	}

	@Override
	public String getMessage() {
		return status.getMessage();
	}

	@Override
	public String getPlugin() {
		return status.getPlugin();
	}

	@Override
	public int getSeverity() {
		return status.getSeverity();
	}

	@Override
	public boolean isMultiStatus() {
		return status.isMultiStatus();
	}

	@Override
	public boolean isOK() {
		return status.isOK();
	}

	@Override
	public boolean matches(int severityMask) {
		return status.matches(severityMask);
	}

}
