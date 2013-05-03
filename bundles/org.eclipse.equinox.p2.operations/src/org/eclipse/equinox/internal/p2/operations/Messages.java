/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.operations;

import org.eclipse.osgi.util.NLS;

/**
 * @since 2.0
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.operations.messages"; //$NON-NLS-1$

	public static String InstallOperation_ComputeProfileChangeProgress;

	public static String InstallOperation_InstallJobName;
	public static String InstallOperation_ResolveJobName;

	public static String ProfileChangeOperation_NoProfileChangeRequest;
	public static String ProfileChangeOperation_ResolveTaskName;

	public static String ProvisioningJob_GenericErrorStatusMessage;
	public static String ProvisioningSession_AgentNotFound;

	public static String ProvisioningSession_InstallPlanConfigurationError;

	public static String PlanAnalyzer_IgnoringInstall;
	public static String PlanAnalyzer_LockedImpliedUpdate0;
	public static String PlanAnalyzer_PartialInstall;
	public static String PlanAnalyzer_PartialUninstall;
	public static String PlanAnalyzer_SideEffectInstall;
	public static String PlanAnalyzer_SideEffectUninstall;
	public static String PlannerResolutionJob_NullProvisioningPlan;

	public static String PlanAnalyzer_IgnoringImpliedDowngrade;
	public static String PlanAnalyzer_ImpliedUpdate;
	public static String PlanAnalyzer_Items;
	public static String PlanAnalyzer_NothingToDo;

	public static String PlanAnalyzer_NoUpdates;
	public static String PlanAnalyzer_AlreadyInstalled;
	public static String PlanAnalyzer_AnotherOperationInProgress;
	public static String PlanAnalyzer_RequestAltered;
	public static String PlanAnalyzer_UnexpectedError;

	public static String RepositoryTracker_DuplicateLocation;
	public static String RepositoryTracker_InvalidLocation;

	public static String ResolutionResult_SummaryStatus;

	public static String SizingPhaseSet_PhaseSetName;

	public static String UninstallOperation_ProvisioningJobName;
	public static String UninstallOperation_ResolveJobName;

	public static String UpdateOperation_ProfileChangeRequestProgress;
	public static String UpdateOperation_UpdateJobName;
	public static String UpdateOperation_ResolveJobName;

	public static String RemediationOperation_ProfileChangeRequestProgress;
	public static String RemediationOperation_ResolveJobName;
	public static String RemediationOperation_RemediationJobName;
	public static String RemediationOperation_NoRemedyFound;

	public static String OperationFactory_noAgent;
	public static String OperationFactory_noIUFound;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
