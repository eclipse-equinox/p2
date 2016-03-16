/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.operations;

import java.util.Collection;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerStatus;
import org.eclipse.equinox.internal.provisional.p2.director.RequestStatus;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

/**
 * This class analyzes a profile change request and the resultant provisioning plan,
 * and reports problems in a way that can be communicated to a user.
 * 
 * @since 3.5
 */
public class PlanAnalyzer {

	public static IStatus getStatus(int statusCode, IInstallableUnit affectedIU) {
		switch (statusCode) {
			case IStatusCodes.NOTHING_TO_UPDATE :
				return new Status(IStatus.INFO, Activator.ID, statusCode, Messages.PlanAnalyzer_NoUpdates, null);
			case IStatusCodes.PROFILE_CHANGE_ALTERED :
				return new MultiStatus(Activator.ID, statusCode, Messages.PlanAnalyzer_RequestAltered, null);
			case IStatusCodes.ALTERED_IMPLIED_UPDATE :
				return new Status(IStatus.INFO, Activator.ID, statusCode, NLS.bind(Messages.PlanAnalyzer_ImpliedUpdate, getIUString(affectedIU)), null);
			case IStatusCodes.ALTERED_IGNORED_IMPLIED_UPDATE :
				return new Status(IStatus.WARNING, Activator.ID, statusCode, NLS.bind(Messages.PlanAnalyzer_LockedImpliedUpdate0, getIUString(affectedIU)), null);
			case IStatusCodes.ALTERED_IGNORED_IMPLIED_DOWNGRADE :
				return new Status(IStatus.WARNING, Activator.ID, statusCode, NLS.bind(Messages.PlanAnalyzer_IgnoringImpliedDowngrade, getIUString(affectedIU)), null);
			case IStatusCodes.ALTERED_IGNORED_ALREADY_INSTALLED :
				return new Status(IStatus.WARNING, Activator.ID, statusCode, NLS.bind(Messages.PlanAnalyzer_AlreadyInstalled, getIUString(affectedIU)), null);
			case IStatusCodes.ALTERED_PARTIAL_INSTALL :
				return new Status(IStatus.INFO, Activator.ID, statusCode, NLS.bind(Messages.PlanAnalyzer_PartialInstall, getIUString(affectedIU)), null);
			case IStatusCodes.ALTERED_PARTIAL_UNINSTALL :
				return new Status(IStatus.INFO, Activator.ID, statusCode, NLS.bind(Messages.PlanAnalyzer_PartialUninstall, getIUString(affectedIU)), null);
			case IStatusCodes.UNEXPECTED_NOTHING_TO_DO :
				return new Status(IStatus.ERROR, Activator.ID, statusCode, NLS.bind(Messages.PlanAnalyzer_NothingToDo, getIUString(affectedIU)), null);
			case IStatusCodes.OPERATION_ALREADY_IN_PROGRESS :
				return new Status(IStatus.ERROR, Activator.ID, statusCode, Messages.PlanAnalyzer_AnotherOperationInProgress, null);
			default :
				return new Status(IStatus.ERROR, Activator.ID, statusCode, NLS.bind(Messages.PlanAnalyzer_UnexpectedError, Integer.valueOf(statusCode), getIUString(affectedIU)), null);
		}
	}

	public static MultiStatus getProfileChangeAlteredStatus() {
		return (MultiStatus) getStatus(IStatusCodes.PROFILE_CHANGE_ALTERED, null);
	}

	public static ResolutionResult computeResolutionResult(ProfileChangeRequest originalRequest, IProvisioningPlan plan, MultiStatus originalStatus) {
		Assert.isNotNull(originalRequest);
		Assert.isNotNull(plan);
		Assert.isNotNull(originalStatus);

		ResolutionResult report = new ResolutionResult();

		// If the plan was canceled, no further analysis is needed
		if (plan.getStatus().getSeverity() == IStatus.CANCEL) {
			report.addSummaryStatus(plan.getStatus());
			return report;
		}

		if (nothingToDo(originalRequest)) {
			report.addSummaryStatus(getStatus(IStatusCodes.UNEXPECTED_NOTHING_TO_DO, null));
			IStatus[] details = originalStatus.getChildren();
			for (int i = 0; i < details.length; i++)
				report.addSummaryStatus(details[i]);
			return report;
		}

		// If there was already some status supplied before resolution, this should get included
		// with the report.  For example, this might contain information about the profile request
		// being altered before resolution began.
		if (originalStatus != null && originalStatus.getChildren().length > 0) {
			report.addSummaryStatus(originalStatus);
		}

		// If the overall plan had a non-OK status, capture that in the report.
		if (!plan.getStatus().isOK())
			report.addSummaryStatus(plan.getStatus());

		// Now we compare what was requested with what is going to happen.
		// In the long run, when a RequestStatus can provide actual explanation/status
		// about failures, we might want to add this information to the overall status.
		// As it stands now, if the provisioning plan is in error, that info is more detailed
		// than the request status.  So we will only add request status info to the overall
		// status when the overall status is not in error.

		PlannerStatus plannerStatus = plan.getStatus() instanceof PlannerStatus ? (PlannerStatus) plan.getStatus() : null;
		// If there is no additional plannerStatus details just return the report
		if (plannerStatus == null)
			return report;

		if (plan.getStatus().getSeverity() != IStatus.ERROR) {
			Collection<IInstallableUnit> iusAdded = originalRequest.getAdditions();
			for (IInstallableUnit added : iusAdded) {
				RequestStatus rs = plannerStatus.getRequestChanges().get(added);
				if (rs.getSeverity() == IStatus.ERROR) {
					// This is a serious error so it must also appear in the overall status
					IStatus fail = new Status(IStatus.ERROR, Activator.ID, IStatusCodes.ALTERED_IGNORED_INSTALL_REQUEST, NLS.bind(Messages.PlanAnalyzer_IgnoringInstall, getIUString(added)), null);
					report.addStatus(added, fail);
					report.addSummaryStatus(fail);
				}
			}
			Collection<IInstallableUnit> iusRemoved = originalRequest.getRemovals();
			for (IInstallableUnit removed : iusRemoved) {
				RequestStatus rs = plannerStatus.getRequestChanges().get(removed);
				if (rs.getSeverity() == IStatus.ERROR) {
					// TODO see https://bugs.eclipse.org/bugs/show_bug.cgi?id=255984
					// We are making assumptions here about why the planner chose to ignore an uninstall.
					// Assume it could not be uninstalled because of some other dependency, yet the planner did not view
					// this as an error.  So we inform the user that we can only uninstall parts of it.  The root property will be
					// removed per the original change request.
					IStatus fail = new Status(IStatus.INFO, Activator.ID, IStatusCodes.ALTERED_PARTIAL_UNINSTALL, NLS.bind(Messages.PlanAnalyzer_PartialUninstall, getIUString(removed)), null);
					report.addStatus(removed, fail);
					report.addSummaryStatus(fail);
				}
			}
		} else {
			if (plannerStatus.getCode() == IStatusCodes.MISSING_REQUIREMENTS) {
				IStatus existingSummaryStatus = report.getSummaryStatus();
				MultiStatus newSummaryStatus = new MultiStatus(Activator.ID, IStatusCodes.MISSING_REQUIREMENTS, Messages.ResolutionResult_SummaryStatus, null);
				if (!existingSummaryStatus.isOK()) {
					newSummaryStatus.addAll(existingSummaryStatus);
					report.setSummaryStatus(newSummaryStatus);
				}
			}
		}

		// Now process the side effects
		if (plannerStatus.getRequestSideEffects() != null) {
			for (Entry<IInstallableUnit, RequestStatus> entry : plannerStatus.getRequestSideEffects().entrySet()) {
				IInstallableUnit iu = entry.getKey();
				RequestStatus rs = entry.getValue();
				if (rs.getInitialRequestType() == RequestStatus.ADDED) {
					report.addStatus(iu, new Status(rs.getSeverity(), Activator.ID, IStatusCodes.ALTERED_SIDE_EFFECT_INSTALL, NLS.bind(Messages.PlanAnalyzer_SideEffectInstall, getIUString(iu)), null));
				} else {
					report.addStatus(iu, new Status(rs.getSeverity(), Activator.ID, IStatusCodes.ALTERED_SIDE_EFFECT_REMOVE, NLS.bind(Messages.PlanAnalyzer_SideEffectUninstall, getIUString(iu)), null));
				}
			}
		}

		return report;

	}

	private static String getIUString(IInstallableUnit iu) {
		if (iu == null)
			return Messages.PlanAnalyzer_Items;
		// Get the iu name in the default locale
		String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
		if (name != null)
			return name;
		return iu.getId();
	}

	private static boolean nothingToDo(ProfileChangeRequest request) {
		return request.getAdditions().size() == 0 && request.getRemovals().size() == 0 && request.getInstallableUnitProfilePropertiesToAdd().size() == 0 && request.getInstallableUnitProfilePropertiesToRemove().size() == 0;
	}
}
