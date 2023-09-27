/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	IBM Corporation - initial API and implementation
 * 	Genuitec - bug fixes
 *  Sonatype, Inc. - ongoing development
 *  Red Hat, Inc. - support for remediation page
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import static org.eclipse.core.runtime.IStatus.*;
import static org.eclipse.equinox.internal.p2.director.DirectorActivator.PI_DIRECTOR;
import static org.eclipse.equinox.internal.provisional.p2.director.RequestStatus.ADDED;
import static org.eclipse.equinox.internal.provisional.p2.director.RequestStatus.REMOVED;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.director.Explanation.MissingIU;
import org.eclipse.equinox.internal.p2.director.Explanation.Singleton;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.query.UpdateQuery;
import org.eclipse.equinox.internal.p2.rollback.FormerState;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerStatus;
import org.eclipse.equinox.internal.provisional.p2.director.RequestStatus;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.planner.*;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.osgi.util.NLS;

public class SimplePlanner implements IPlanner {
	private static final boolean DEBUG = Tracing.DEBUG_PLANNER_OPERANDS;

	private static final int ExpandWork = 12;
	private static final String INCLUDE_PROFILE_IUS = "org.eclipse.equinox.p2.internal.profileius"; //$NON-NLS-1$
	public static final String INCLUSION_RULES = "org.eclipse.equinox.p2.internal.inclusion.rules"; //$NON-NLS-1$
	private static final String ID_IU_FOR_ACTIONS = "org.eclipse.equinox.p2.engine.actions.root"; //$NON-NLS-1$
	private static final String EXPLANATION = "org.eclipse.equinox.p2.director.explain"; //$NON-NLS-1$
	private static final String CONSIDER_METAREQUIREMENTS = "org.eclipse.equinox.p2.planner.resolveMetaRequirements"; //$NON-NLS-1$

	static final int UNSATISFIABLE = 1; // status code indicating that the problem is not satisfiable

	private final IProvisioningAgent agent;
	private final IProfileRegistry profileRegistry;
	private final IEngine engine;

	private IProvisioningPlan generateProvisioningPlan(Collection<IInstallableUnit> fromState,
			Collection<IInstallableUnit> toState, ProfileChangeRequest changeRequest, IProvisioningPlan installerPlan,
			ProvisioningContext context) {
		IProvisioningPlan plan = engine.createPlan(changeRequest.getProfile(), context);
		plan.setFuturePlan(new CollectionResult<>(toState));
		planIUOperations(plan, fromState, toState);
		planPropertyOperations(plan, changeRequest, toState);

		if (DEBUG) {
			Object[] operands = new Object[0];
			try {
				Method getOperands = plan.getClass().getMethod("getOperands"); //$NON-NLS-1$
				operands = (Object[]) getOperands.invoke(plan);
			} catch (Throwable e) {
				// ignore
			}
			for (Object operand : operands) {
				Tracing.debug(operand.toString());
			}
		}

		Map<IInstallableUnit, RequestStatus>[] changes = buildDetailedSuccess(toState, changeRequest);
		Map<IInstallableUnit, RequestStatus> requestChanges = changes[0];
		Map<IInstallableUnit, RequestStatus> requestSideEffects = changes[1];
		IStatus status = convertChangesToStatus(requestChanges, requestSideEffects);
		QueryableArray plannedState = new QueryableArray(toState);
		PlannerStatus plannerStatus = new PlannerStatus(status, null, requestChanges, requestSideEffects, plannedState);
		plan.setStatus(plannerStatus);
		plan.setInstallerPlan(installerPlan);
		return plan;
	}

	private Map<IInstallableUnit, RequestStatus>[] buildDetailedErrors(ProfileChangeRequest changeRequest) {
		Collection<IInstallableUnit> requestedAdditions = changeRequest.getAdditions();
		Collection<IInstallableUnit> requestedRemovals = changeRequest.getRemovals();
		Map<IInstallableUnit, RequestStatus> requestStatus = new HashMap<>(
				requestedAdditions.size() + requestedRemovals.size());
		for (IInstallableUnit added : requestedAdditions) {
			requestStatus.put(added, new RequestStatus(added, ADDED, ERROR, null));
		}
		for (IInstallableUnit removed : requestedRemovals) {
			requestStatus.put(removed, new RequestStatus(removed, REMOVED, ERROR, null));
		}
		@SuppressWarnings("unchecked")
		Map<IInstallableUnit, RequestStatus>[] maps = new Map[] { requestStatus, null };
		return maps;
	}

	private Map<IInstallableUnit, RequestStatus>[] buildDetailedSuccess(Collection<IInstallableUnit> toState,
			ProfileChangeRequest changeRequest) {
		Collection<IInstallableUnit> requestedAdditions = changeRequest.getAdditions();
		Collection<IInstallableUnit> requestedRemovals = new ArrayList<>(changeRequest.getRemovals());
		requestedRemovals.removeAll(requestedAdditions);

		Map<IInstallableUnit, RequestStatus> requestStatus = new HashMap<>(
				requestedAdditions.size() + requestedRemovals.size());

		for (IInstallableUnit added : requestedAdditions) {
			int status = toState.contains(added) ? OK : ERROR;
			requestStatus.put(added, new RequestStatus(added, ADDED, status, null));
		}

		for (IInstallableUnit removed : requestedRemovals) {
			int status = !toState.contains(removed) ? OK : ERROR;
			requestStatus.put(removed, new RequestStatus(removed, REMOVED, status, null));
		}
		// Compute the side effect changes (e.g. things installed optionally going away)
		IQueryResult<IInstallableUnit> includedIUs = changeRequest.getProfile()
				.query(new IUProfilePropertyQuery(INCLUSION_RULES, IUProfilePropertyQuery.ANY), null);
		Map<IInstallableUnit, RequestStatus> sideEffectStatus = new HashMap<>();
		for (IInstallableUnit removal : includedIUs) {
			if (!toState.contains(removal) && !requestStatus.containsKey(removal)) {
				sideEffectStatus.put(removal, new RequestStatus(removal, REMOVED, INFO, null));
			}
		}
		@SuppressWarnings("unchecked")
		Map<IInstallableUnit, RequestStatus>[] maps = new Map[] { requestStatus, sideEffectStatus };
		return maps;
	}

	/**
	 * Converts a list of {@link Projector#getExplanation(IProgressMonitor) resolver
	 * explanations} to a human-readable status object
	 *
	 * @param explanations List of resolver explanations ordered by logical
	 *                     causality. I.e. read from start to end should forms
	 *                     logical chain of statements as to why resolution failed.
	 * @return the {@link IStatus}
	 */
	private static IStatus convertExplanationToStatus(Set<Explanation> explanations) {
		// Hack to create a useful message when a user installs something intended only
		// for the PDE target platform into a general purpose profile.
		List<IStatus> forTargets = new ArrayList<>(0);
		for (Explanation next : explanations) {
			if (next instanceof MissingIU missingIU && missingIU.req instanceof IRequiredCapability requiredCapability
					&& requiredCapability.getMatches().getParameters().length == 3
					&& "A.PDE.Target.Platform".equals(requiredCapability.getNamespace())) {//$NON-NLS-1$
				// This IU requires the PDE target platform IU and it is missing.
				// I.e. this IU is intended only for the PDE target platform.
				forTargets.add(Status.error(Explanation.getUserReadableName(missingIU.iu)));
			}
		}
		if (!forTargets.isEmpty()) {
			// The following line could be removed if Bug 309863 is fixed
			forTargets.add(Status.error(Messages.Director_For_Target_Unselect_Required));
			// Return a multi status with all the IUs that require A.PDE.Target.Platform
			return new MultiStatus(PI_DIRECTOR, 1, forTargets.toArray(IStatus[]::new), Messages.Director_For_Target,
					null);
		}

		// Use the most specific message for the root of the MultiStatus
		// Missing IU takes precedence over singleton conflicts
		String message = Messages.Director_Unsatisfied_Dependencies;
		int errorCode = 1;
		for (Explanation next : explanations) {
			if (next instanceof MissingIU) {
				message = Messages.Explanation_rootMissing;
				errorCode = 10053;
				break;
			}

			if (next instanceof Singleton) {
				message = Messages.Explanation_rootSingleton;
				errorCode = 10054;
				break;
			}
		}

		// The children follow the order of logical causality as produced by the
		// Projector
		MultiStatus root = new MultiStatus(PI_DIRECTOR, errorCode, message, null);
		explanations.stream().map(Explanation::toStatus).forEach(root::add);
		return root;
	}

	/**
	 * TODO Find a way to emit INFO status. The issue is that all
	 * {@link IStatus#isOK()} calls will fail.
	 *
	 * @param requestChanges
	 * @param requestSideEffects
	 * @return the {@link IStatus}
	 */
	private static IStatus convertChangesToStatus(Map<IInstallableUnit, RequestStatus> requestChanges,
			Map<IInstallableUnit, RequestStatus> requestSideEffects) {
		MultiStatus root = new MultiStatus(PI_DIRECTOR, OK, Messages.Director_Satisfied_Dependencies, null);

		Stream.concat(requestChanges.values().stream(), requestSideEffects.values().stream())
				.map(SimplePlanner::buildChangeStatus).forEach(root::add);

		return root;
	}

	private static IStatus buildChangeStatus(RequestStatus rs) {
		final String change = switch (rs.getInitialRequestType()) {
		case ADDED -> "Add"; //$NON-NLS-1$
		case REMOVED -> "Remove"; //$NON-NLS-1$
		default -> "UNKNOWN change type " + rs.getInitialRequestType(); //$NON-NLS-1$
		};
		final IStatus status;
		if (!rs.isOK()) {
			String message = NLS.bind(Messages.Director_Unsatisfied_Change, change,
					Explanation.getUserReadableName(rs.getIu()));

			if (rs.getExplanations().isEmpty()) {
				status = new Status(OK, PI_DIRECTOR, message, rs.getException());
			} else {
				status = new MultiStatus(PI_DIRECTOR, OK,
						rs.getExplanations().stream().map(Explanation::toStatus).toArray(IStatus[]::new), message,
						rs.getException());
			}
		} else {
			String message = NLS.bind(Messages.Director_Satisfied_Change, change,
					Explanation.getUserReadableName(rs.getIu()));
			status = new Status(OK, PI_DIRECTOR, message, rs.getException());
		}

		return status;
	}

	private void planPropertyOperations(IProvisioningPlan plan, ProfileChangeRequest profileChangeRequest,
			Collection<IInstallableUnit> toState) {

		// First deal with profile properties to remove.
		String[] toRemove = profileChangeRequest.getPropertiesToRemove();
		for (String element : toRemove) {
			plan.setProfileProperty(element, null);
		}
		// Now deal with profile property changes/additions
		Map<String, String> propertyChanges = profileChangeRequest.getPropertiesToAdd();
		for (Map.Entry<String, String> entry : propertyChanges.entrySet()) {
			plan.setProfileProperty(entry.getKey(), entry.getValue());
		}

		// Now deal with iu property changes/additions.
		Map<IInstallableUnit, Map<String, String>> allIUPropertyChanges = profileChangeRequest
				.getInstallableUnitProfilePropertiesToAdd();
		for (Map.Entry<IInstallableUnit, Map<String, String>> entry : allIUPropertyChanges.entrySet()) {
			IInstallableUnit iu = entry.getKey();
			if (!toState.contains(iu))
				continue;
			for (Map.Entry<String, String> entry2 : entry.getValue().entrySet()) {
				plan.setInstallableUnitProfileProperty(iu, entry2.getKey(), entry2.getValue());
			}
		}
		// Now deal with iu property removals.
		Map<IInstallableUnit, List<String>> allIUPropertyDeletions = profileChangeRequest
				.getInstallableUnitProfilePropertiesToRemove();
		for (Map.Entry<IInstallableUnit, List<String>> entry : allIUPropertyDeletions.entrySet()) {
			IInstallableUnit iu = entry.getKey();
			List<String> iuPropertyRemovals = entry.getValue();
			for (String key : iuPropertyRemovals) {
				plan.setInstallableUnitProfileProperty(iu, key, null);
			}

		}
	}

	private void planIUOperations(IProvisioningPlan plan, Collection<IInstallableUnit> fromState,
			Collection<IInstallableUnit> toState) {
		new OperationGenerator(plan).generateOperation(fromState, toState);
	}

	@Override
	public IProvisioningPlan getDiffPlan(IProfile currentProfile, IProfile targetProfile, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, ExpandWork);
		sub.setTaskName(Messages.Director_Task_Resolving_Dependencies);
		try {
			IProfileChangeRequest profileChangeRequest = FormerState.generateProfileDeltaChangeRequest(currentProfile,
					targetProfile);
			ProvisioningContext context = new ProvisioningContext(agent);
			if (context.getProperty(INCLUDE_PROFILE_IUS) == null)
				context.setProperty(INCLUDE_PROFILE_IUS, Boolean.FALSE.toString());
			context.setExtraInstallableUnits(Arrays.asList(
					targetProfile.available(QueryUtil.createIUAnyQuery(), null).toArray(IInstallableUnit.class)));
			return getProvisioningPlan(profileChangeRequest, context, sub.newChild(ExpandWork / 2));
		} finally {
			sub.done();
		}
	}

	public static Collection<IInstallableUnit> findPlannerMarkedIUs(final IProfile profile) {
		IQuery<IInstallableUnit> markerQuery = new IUProfilePropertyQuery(INCLUSION_RULES, IUProfilePropertyQuery.ANY);
		return profile.query(markerQuery, null).toUnmodifiableSet();
	}

	public static Map<String, String> createSelectionContext(Map<String, String> properties) {
		HashMap<String, String> result = new HashMap<>(properties);
		String environments = properties.get(IProfile.PROP_ENVIRONMENTS);
		if (environments == null)
			return result;
		for (StringTokenizer tokenizer = new StringTokenizer(environments, ","); tokenizer.hasMoreElements();) { //$NON-NLS-1$
			String entry = tokenizer.nextToken();
			int i = entry.indexOf('=');
			String key = entry.substring(0, i).trim();
			String value = entry.substring(i + 1).trim();
			result.put(key, value);
		}
		return result;
	}

	private Collection<IInstallableUnit> gatherAvailableInstallableUnits(List<IInstallableUnit> additionalSource,
			ProvisioningContext context, IProgressMonitor monitor) {
		Map<String, IInstallableUnit> resultsMap = new HashMap<>();
		if (additionalSource != null) {
			for (IInstallableUnit element : additionalSource) {
				String key = element.getId() + "_" + element.getVersion().toString(); //$NON-NLS-1$
				resultsMap.put(key, element);
			}
		}
		if (context == null) {
			context = new ProvisioningContext(agent);
		} else {
			for (IInstallableUnit iu : context.getExtraInstallableUnits()) {
				String key = iu.getId() + '_' + iu.getVersion().toString();
				resultsMap.put(key, iu);
			}
		}
		SubMonitor sub = SubMonitor.convert(monitor, 1000);
		IQueryable<IInstallableUnit> queryable = context.getMetadata(sub.newChild(500));
		IQueryResult<IInstallableUnit> matches = queryable.query(QueryUtil.createIUQuery(null, VersionRange.emptyRange),
				sub.newChild(500));
		for (IInstallableUnit iu : matches) {
			String key = iu.getId() + "_" + iu.getVersion().toString(); //$NON-NLS-1$
			IInstallableUnit currentIU = resultsMap.get(key);
			if (currentIU == null || hasHigherFidelity(iu, currentIU))
				resultsMap.put(key, iu);
		}
		sub.done();
		return resultsMap.values();
	}

	private static boolean hasHigherFidelity(IInstallableUnit iu, IInstallableUnit currentIU) {
		return Boolean.parseBoolean(currentIU.getProperty(IInstallableUnit.PROP_PARTIAL_IU))
				&& !Boolean.parseBoolean(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU));
	}

	public SimplePlanner(IProvisioningAgent agent) {
		Assert.isNotNull(agent);
		this.agent = agent;
		this.engine = agent.getService(IEngine.class);
		this.profileRegistry = agent.getService(IProfileRegistry.class);
		Assert.isNotNull(engine);
		Assert.isNotNull(profileRegistry);
	}

	private boolean satisfyMetaRequirements(Map<String, String> props) {
		return props == null || props.get(CONSIDER_METAREQUIREMENTS) == null
				|| Boolean.parseBoolean(props.get(CONSIDER_METAREQUIREMENTS));
	}

	private boolean satisfyMetaRequirements(IProfile p) {
		return satisfyMetaRequirements(p.getProperties());
	}

	/**
	 * Performs a provisioning request resolution
	 *
	 * @param profileChangeRequest The requested change.
	 * @param context              The context for the resolution pass
	 * @param monitor
	 *
	 * @return Return a {@link Projector} that captures the complete future state of
	 *         the profile that satisfies the request. If the request can't be
	 *         satisfied return an {@link IProvisioningPlan} where the error is
	 *         captured in {@link IProvisioningPlan#getStatus()}
	 */
	private Object getSolutionFor(ProfileChangeRequest profileChangeRequest, ProvisioningContext context,
			IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, ExpandWork);
		sub.setTaskName(Messages.Director_Task_Resolving_Dependencies);
		try {
			IProfile profile = profileChangeRequest.getProfile();

			Object[] updatedPlan = updatePlannerInfo(profileChangeRequest, context);

			Map<String, String> newSelectionContext = createSelectionContext(
					profileChangeRequest.getProfileProperties());

			List<IInstallableUnit> extraIUs = new ArrayList<>(profileChangeRequest.getAdditions());
			extraIUs.addAll(profileChangeRequest.getRemovals());
			if (context == null || context.getProperty(INCLUDE_PROFILE_IUS) == null
					|| context.getProperty(INCLUDE_PROFILE_IUS).equalsIgnoreCase(Boolean.TRUE.toString())) {
				profile.available(QueryUtil.createIUAnyQuery(), null).forEach(extraIUs::add);
			}

			Collection<IInstallableUnit> availableIUs = gatherAvailableInstallableUnits(extraIUs, context,
					sub.newChild(ExpandWork / 4));
			Slicer slicer = new Slicer(new QueryableArray(availableIUs), newSelectionContext,
					satisfyMetaRequirements(profileChangeRequest.getProfileProperties()));
			IQueryable<IInstallableUnit> slice = slicer.slice(List.of((IInstallableUnit) updatedPlan[0]),
					sub.newChild(ExpandWork / 4));
			if (slice == null) {
				IProvisioningPlan plan = engine.createPlan(profile, context);
				plan.setStatus(slicer.getStatus());
				return plan;
			}
			slice = new CompoundQueryable<>(List.of(slice, new QueryableArray(profileChangeRequest.getAdditions())));
			Projector projector = new Projector(slice, newSelectionContext, slicer.getNonGreedyIUs(),
					satisfyMetaRequirements(profileChangeRequest.getProfileProperties()));
			projector.setUserDefined(profileChangeRequest.getPropertiesToAdd().containsKey("_internal_user_defined_")); //$NON-NLS-1$
			projector.encode((IInstallableUnit) updatedPlan[0], (IInstallableUnit[]) updatedPlan[1], profile,
					profileChangeRequest.getAdditions(), sub.newChild(ExpandWork / 4));

			IStatus s = projector.invokeSolver(sub.newChild(ExpandWork / 4));
			switch (s.getSeverity()) {
			case CANCEL: {
				IProvisioningPlan plan = engine.createPlan(profile, context);
				plan.setStatus(s);
				projector.close();
				return plan;
			}

			// Convert the projector explanation chain into an IStatus
			case ERROR: {
				sub.setTaskName(Messages.Planner_NoSolution);
				if (s.getCode() != UNSATISFIABLE || (context != null && !(context.getProperty(EXPLANATION) == null
						|| Boolean.parseBoolean(context.getProperty(EXPLANATION))))) {
					IProvisioningPlan plan = engine.createPlan(profile, context);
					plan.setStatus(s);
					projector.close();
					return plan;
				}

				// Extract the explanation
				Set<Explanation> explanation = projector.getExplanation(sub.newChild(ExpandWork / 4));
				IStatus explanationStatus = convertExplanationToStatus(explanation);

				Map<IInstallableUnit, RequestStatus>[] changes = buildDetailedErrors(profileChangeRequest);
				Map<IInstallableUnit, RequestStatus> requestChanges = changes[0];
				Map<IInstallableUnit, RequestStatus> requestSideEffects = changes[1];
				PlannerStatus plannerStatus = new PlannerStatus(explanationStatus,
						new RequestStatus(null, REMOVED, ERROR, explanation), requestChanges, requestSideEffects, null);

				IProvisioningPlan plan = engine.createPlan(profile, context);
				plan.setStatus(plannerStatus);
				projector.close();
				return plan;
			}

			// The resolution succeeded. We can forget about the warnings since there is a
			// solution.
			case OK: {
				return projector;
			}

			// Log the unexpected status type, but continue
			default: {
				if (Tracing.DEBUG) {
					LogHelper.log(s);
				}
				return projector;
			}
			}
		} finally {
			sub.done();
		}
	}

	@Override
	public IProvisioningPlan getProvisioningPlan(IProfileChangeRequest request, ProvisioningContext context,
			IProgressMonitor monitor) {
		ProfileChangeRequest pcr = (ProfileChangeRequest) request;
		SubMonitor sub = SubMonitor.convert(monitor, ExpandWork);
		sub.setTaskName(Messages.Director_Task_Resolving_Dependencies);
		try {
			// Get the solution for the initial request
			Object resolutionResult = getSolutionFor(pcr, context, sub.newChild(ExpandWork / 2));
			// a return value of a plan indicates failure when resolving so return.
			if (resolutionResult instanceof IProvisioningPlan plan) {
				return plan;
			}

			Projector projector = (Projector) resolutionResult;
			Collection<IInstallableUnit> newState = projector.extractSolution();
			Collection<IInstallableUnit> fullState = new ArrayList<>();
			fullState.addAll(newState);
			newState = AttachmentHelper.attachFragments(newState.stream(), projector.getFragmentAssociation());

			IProvisioningPlan temporaryPlan = generatePlan(projector, newState, pcr, context);
			projector.close();

			// Create a plan for installing necessary pieces to complete the installation
			// (e.g touchpoint actions)
			return createInstallerPlan(pcr.getProfile(), pcr, fullState, newState, temporaryPlan, context,
					sub.newChild(ExpandWork / 2));
		} catch (OperationCanceledException e) {
			IProvisioningPlan plan = engine.createPlan(pcr.getProfile(), context);
			plan.setStatus(Status.CANCEL_STATUS);
			return plan;
		} finally {
			sub.done();
		}
	}

	// private IProvisioningPlan
	// generateAbsoluteProvisioningPlan(ProfileChangeRequest profileChangeRequest,
	// ProvisioningContext context, IProgressMonitor monitor) {
	// Set<IInstallableUnit> toState =
	// profileChangeRequest.getProfile().query(QueryUtil.createIUAnyQuery(),
	// null).toSet();
	// HashSet<IInstallableUnit> fromState = new HashSet<IInstallableUnit>(toState);
	// toState.removeAll(profileChangeRequest.getRemovals());
	// toState.addAll(profileChangeRequest.getAdditions());
	//
	// IProvisioningPlan plan = engine.createPlan(profileChangeRequest.getProfile(),
	// context);
	// planIUOperations(plan, fromState, toState);
	// planPropertyOperations(plan, profileChangeRequest);
	//
	// if (DEBUG) {
	// Object[] operands = new Object[0];
	// try {
	// Method getOperands = plan.getClass().getMethod("getOperands", new Class[0]);
	// //$NON-NLS-1$
	// operands = (Object[]) getOperands.invoke(plan, new Object[0]);
	// } catch (Throwable e) {
	// // ignore
	// }
	// for (int i = 0; i < operands.length; i++) {
	// Tracing.debug(operands[i].toString());
	// }
	// }
	// Map<IInstallableUnit, RequestStatus>[] changes =
	// computeActualChangeRequest(toState, profileChangeRequest);
	// Map<IInstallableUnit, RequestStatus> requestChanges = (changes == null) ?
	// null : changes[0];
	// Map<IInstallableUnit, RequestStatus> requestSideEffects = (changes == null) ?
	// null : changes[1];
	// QueryableArray plannedState = new QueryableArray(toState.toArray(new
	// IInstallableUnit[toState.size()]));
	// PlannerStatus plannerStatus = new PlannerStatus(Status.OK_STATUS, null,
	// requestChanges, requestSideEffects, plannedState);
	// plan.setStatus(plannerStatus);
	// return plan;
	// }

	// Verify that all the meta requirements necessary to perform the uninstallation
	// (if necessary) and all t
	private Collection<IRequirement> areMetaRequirementsSatisfied(IProfile oldProfile,
			Collection<IInstallableUnit> newProfile, IProvisioningPlan initialPlan) {
		Collection<IRequirement> allMetaRequirements = extractMetaRequirements(newProfile, initialPlan);
		for (IRequirement requirement : allMetaRequirements) {
			if (oldProfile
					.query(QueryUtil.createLimitQuery(QueryUtil.createMatchQuery(requirement.getMatches()), 1), null)
					.isEmpty())
				return allMetaRequirements;
		}
		return null;
	}

	// Return all the meta requirements for the list of IU specified and all the
	// meta requirements listed necessary to satisfy the uninstallation
	private Collection<IRequirement> extractMetaRequirements(Collection<IInstallableUnit> ius, IProvisioningPlan plan) {
		Set<IRequirement> allMetaRequirements = new HashSet<>();
		for (IInstallableUnit iu : ius) {
			allMetaRequirements.addAll(iu.getMetaRequirements());
		}
		IQueryResult<IInstallableUnit> queryResult = plan.getRemovals().query(QueryUtil.createIUAnyQuery(), null);
		for (IInstallableUnit iu : queryResult) {
			allMetaRequirements.addAll(iu.getMetaRequirements());
		}
		return allMetaRequirements;
	}

	private IProvisioningPlan createInstallerPlan(IProfile profile, ProfileChangeRequest initialRequest,
			Collection<IInstallableUnit> unattachedState, Collection<IInstallableUnit> expectedState,
			IProvisioningPlan initialPlan, ProvisioningContext initialContext, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, ExpandWork);

		try {
			sub.setTaskName(Messages.Director_Task_installer_plan);
			if (profileRegistry == null) {
				IProvisioningPlan plan = engine.createPlan(initialRequest.getProfile(), initialContext);
				plan.setStatus(Status.error(Messages.Planner_no_profile_registry));
				return plan;
			}

			// No installer agent set
			if (agent.getService(IProvisioningAgent.INSTALLER_AGENT) == null) {
				return initialPlan;
			}

			IProfile installerProfile = ((IProvisioningAgent) agent
					.getService(IProvisioningAgent.INSTALLER_AGENT)).getService(IProfileRegistry.class)
							.getProfile((String) agent.getService(IProvisioningAgent.INSTALLER_PROFILEID));
			if (installerProfile == null)
				return initialPlan;

			// The target and the installer are in the same agent / profile registry
			if (haveSameLocation(agent, (IProvisioningAgent) agent.getService(IProvisioningAgent.INSTALLER_AGENT))) {
				// The target and the installer are the same profile (e.g. the eclipse SDK)
				if (profile.getProfileId().equals(installerProfile.getProfileId())) {
					if (profile.getTimestamp() != installerProfile.getTimestamp()) {
						IProvisioningPlan plan = engine.createPlan(initialRequest.getProfile(), initialContext);
						plan.setStatus(
								Status.error(NLS.bind(Messages.Planner_profile_out_of_sync, profile.getProfileId())));
						return plan;
					}
					return createInstallerPlanForCohostedCase(profile, initialRequest, initialPlan, unattachedState,
							expectedState, initialContext, sub);
				}

			}

			if (satisfyMetaRequirements(profile) && !profile.getProfileId().equals(installerProfile.getProfileId())) {
				// co-hosted case from external installer
				IProvisioningPlan planForProfile = generatePlan(null, expectedState, initialRequest, initialContext);
				return createInstallerPlanForExternalInstaller(profile, initialRequest, planForProfile, expectedState,
						initialContext, installerProfile, sub);
			}

			return createInstallerPlanForExternalInstaller(profile, initialRequest, initialPlan, expectedState,
					initialContext, installerProfile, sub);

		} finally {
			sub.done();
		}
	}

	private boolean haveSameLocation(IProvisioningAgent agent1, IProvisioningAgent agent2) {
		if (agent1 == null || agent2 == null)
			return false;
		if (agent1 == agent2)
			return true;
		IAgentLocation thisLocation = agent1.getService(IAgentLocation.class);
		IAgentLocation otherLocation = agent2.getService(IAgentLocation.class);
		if (thisLocation == null || otherLocation == null || (thisLocation == null && otherLocation == null))
			return false;
		return thisLocation.getRootLocation().equals(otherLocation.getRootLocation());
	}

	// Deal with the case where the agent profile is different than the one being
	// provisioned
	private IProvisioningPlan createInstallerPlanForExternalInstaller(IProfile targetedProfile,
			ProfileChangeRequest initialRequest, IProvisioningPlan initialPlan,
			Collection<IInstallableUnit> expectedState, ProvisioningContext initialContext, IProfile agentProfile,
			SubMonitor sub) {
		IProfileRegistry installerRegistry = (IProfileRegistry) ((IProvisioningAgent) agent
				.getService(IProvisioningAgent.INSTALLER_AGENT)).getService(IProfileRegistry.SERVICE_NAME);
		IProfile installerProfile = installerRegistry
				.getProfile((String) agent.getService(IProvisioningAgent.INSTALLER_PROFILEID));

		Collection<IRequirement> metaRequirements = areMetaRequirementsSatisfied(installerProfile, expectedState,
				initialPlan);
		if (metaRequirements == null)
			return initialPlan;

		IInstallableUnit actionsIU = createIUForMetaRequirements(targetedProfile, metaRequirements);
		IInstallableUnit previousActionsIU = getPreviousIUForMetaRequirements(installerProfile,
				getActionGatheringIUId(targetedProfile), sub);

		ProfileChangeRequest agentRequest = new ProfileChangeRequest(installerProfile);
		agentRequest.add(actionsIU);
		if (previousActionsIU != null)
			agentRequest.remove(previousActionsIU);
		Object externalInstallerPlan = getSolutionFor(agentRequest, initialContext, sub.split(10));
		if (externalInstallerPlan instanceof IProvisioningPlan provPlan
				&& provPlan.getStatus().getSeverity() == ERROR) {
			MultiStatus externalInstallerStatus = new MultiStatus(PI_DIRECTOR, 0, Messages.Planner_can_not_install_preq,
					null);
			externalInstallerStatus.add(provPlan.getStatus());
			IProvisioningPlan plan = engine.createPlan(initialRequest.getProfile(), initialContext);
			plan.setStatus(externalInstallerStatus);
			IProvisioningPlan installerPlan = engine.createPlan(agentProfile, initialContext);
			installerPlan.setStatus(externalInstallerStatus);
			plan.setInstallerPlan(installerPlan);
			return plan;
		}

		initialPlan
				.setInstallerPlan(generatePlan((Projector) externalInstallerPlan, null, agentRequest, initialContext));
		return initialPlan;
	}

	// Deal with the case where the actions needs to be installed in the same
	// profile than the one we are performing the initial request
	// The expectedState represents the result of the initialRequest where the
	// metaRequirements have been satisfied.
	private IProvisioningPlan createInstallerPlanForCohostedCase(IProfile profile, ProfileChangeRequest initialRequest,
			IProvisioningPlan initialPlan, Collection<IInstallableUnit> unattachedState,
			Collection<IInstallableUnit> expectedState, ProvisioningContext initialContext, SubMonitor monitor) {
		Collection<IRequirement> metaRequirements = initialRequest.getRemovals().isEmpty()
				? areMetaRequirementsSatisfied(profile, expectedState, initialPlan)
				: extractMetaRequirements(expectedState, initialPlan);
		if (metaRequirements == null || metaRequirements.isEmpty())
			return initialPlan;

		// Let's compute a plan that satisfy all the metaRequirements. We limit
		// ourselves to only the IUs that were part of the previous solution.
		IInstallableUnit metaRequirementIU = createIUForMetaRequirements(profile, metaRequirements);
		IInstallableUnit previousMetaRequirementIU = getPreviousIUForMetaRequirements(profile,
				getActionGatheringIUId(profile), monitor);

		// Create an agent request from the initial request
		ProfileChangeRequest agentRequest = new ProfileChangeRequest(profile);
		for (Map.Entry<String, String> entry : initialRequest.getPropertiesToAdd().entrySet()) {
			agentRequest.setProfileProperty(entry.getKey(), entry.getValue());
		}
		String[] removedProperties = initialRequest.getPropertiesToRemove();
		for (String removedPropertie : removedProperties) {
			agentRequest.removeProfileProperty(removedPropertie);
		}
		initialRequest.getInstallableUnitProfilePropertiesToRemove().forEach((iu, propertyKeys) -> {
			for (String propKey : propertyKeys) {
				agentRequest.removeInstallableUnitProfileProperty(iu, propKey);
			}
		});

		if (previousMetaRequirementIU != null)
			agentRequest.remove(previousMetaRequirementIU);
		agentRequest.add(metaRequirementIU);

		ProvisioningContext agentCtx = new ProvisioningContext(agent);
		agentCtx.setMetadataRepositories();
		List<IInstallableUnit> extraIUs = new ArrayList<>(unattachedState);
		agentCtx.setExtraInstallableUnits(extraIUs);
		Object agentSolution = getSolutionFor(agentRequest, agentCtx, monitor.newChild(3));
		if (agentSolution instanceof IProvisioningPlan solutionPlan
				&& solutionPlan.getStatus().getSeverity() == ERROR) {
			MultiStatus agentStatus = new MultiStatus(PI_DIRECTOR, 0,
					Messages.Planner_actions_and_software_incompatible, null);
			agentStatus.add(solutionPlan.getStatus());
			IProvisioningPlan plan = engine.createPlan(initialRequest.getProfile(), initialContext);
			plan.setStatus(agentStatus);
			IProvisioningPlan installerPlan = engine.createPlan(initialRequest.getProfile(), initialContext);
			installerPlan.setStatus(agentStatus);
			plan.setInstallerPlan(installerPlan);
			return plan;
		}

		// Compute the installer plan. It is the difference between what is currently in
		// the profile and the solution we just computed
		Collection<IInstallableUnit> agentState = ((Projector) agentSolution).extractSolution();
		agentState.remove(metaRequirementIU); // Remove the fake IU
		agentState = AttachmentHelper.attachFragments(agentState.stream(),
				((Projector) agentSolution).getFragmentAssociation());

		ProvisioningContext noRepoContext = createNoRepoContext(initialRequest);
		// ...This computes the attachment of what is currently in the profile
		Object initialSolution = getSolutionFor(
				new ProfileChangeRequest(new EverythingOptionalProfile(initialRequest.getProfile())), noRepoContext,
				new NullProgressMonitor());
		if (initialSolution instanceof IProvisioningPlan plan) {
			LogHelper.log(Status.error("The resolution of the previous state contained in profile " //$NON-NLS-1$
					+ initialRequest.getProfile().getProfileId() + " version " //$NON-NLS-1$
					+ initialRequest.getProfile().getTimestamp() + " failed.")); //$NON-NLS-1$
			return plan;
		}
		var profileState = initialRequest.getProfile().query(QueryUtil.createIUAnyQuery(), null).stream();
		Collection<IInstallableUnit> initialState = AttachmentHelper.attachFragments(profileState,
				((Projector) initialSolution).getFragmentAssociation());

		IProvisioningPlan agentPlan = generateProvisioningPlan(initialState, agentState, initialRequest, null,
				initialContext);

		// Compute the installation plan. It is the difference between the state after
		// the installer plan has run and the expectedState.
		return generateProvisioningPlan(agentState, expectedState, initialRequest, agentPlan, initialContext);
	}

	// Compute the set of operands based on the solution obtained previously
	private IProvisioningPlan generatePlan(Projector newSolution, Collection<IInstallableUnit> newState,
			ProfileChangeRequest request, ProvisioningContext context) {
		// Compute the attachment of the new state if not provided
		if (newState == null) {
			newState = newSolution.extractSolution();
			newState = AttachmentHelper.attachFragments(newState.stream(), newSolution.getFragmentAssociation());
		}
		ProvisioningContext noRepoContext = createNoRepoContext(request);

		// Compute the attachment of the previous state
		Object initialSolution = getSolutionFor(
				new ProfileChangeRequest(new EverythingOptionalProfile(request.getProfile())), noRepoContext,
				new NullProgressMonitor());
		if (initialSolution instanceof IProvisioningPlan plan) {
			LogHelper.log(Status.error(
					"The resolution of the previous state contained in profile " + request.getProfile().getProfileId() //$NON-NLS-1$
							+ " version " + request.getProfile().getTimestamp() + " failed.")); //$NON-NLS-1$//$NON-NLS-2$
			return plan;
		}
		Stream<IInstallableUnit> profileState = request.getProfile().query(QueryUtil.createIUAnyQuery(), null).stream();
		Collection<IInstallableUnit> initialState = AttachmentHelper.attachFragments(profileState,
				((Projector) initialSolution).getFragmentAssociation());

		// Generate the plan
		return generateProvisioningPlan(initialState, newState, request, null, context);
	}

	private ProvisioningContext createNoRepoContext(ProfileChangeRequest request) {
		ProvisioningContext noRepoContext = new ProvisioningContext(agent);
		noRepoContext.setMetadataRepositories();
		noRepoContext.setArtifactRepositories();
		noRepoContext.setProperty(INCLUDE_PROFILE_IUS, Boolean.FALSE.toString());
		noRepoContext.setExtraInstallableUnits(new ArrayList<>(request.getProfile()
				.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()).toUnmodifiableSet()));
		return noRepoContext;
	}

	private IInstallableUnit getPreviousIUForMetaRequirements(IProfile profile, String iuId, IProgressMonitor monitor) {
		IQueryResult<IInstallableUnit> c = profile.query(QueryUtil.createIUQuery(iuId), monitor);
		if (c.isEmpty())
			return null;
		return c.iterator().next();
	}

	private String getActionGatheringIUId(IProfile profile) {
		return ID_IU_FOR_ACTIONS + '.' + profile.getProfileId();
	}

	private IInstallableUnit createIUForMetaRequirements(IProfile profile, Collection<IRequirement> metaRequirements) {
		InstallableUnitDescription description = new InstallableUnitDescription();
		String id = getActionGatheringIUId(profile);
		description.setId(id);
		Version version = Version.createOSGi(1, 0, 0, Long.toString(profile.getTimestamp()));
		description.setVersion(version);
		description.addRequirements(metaRequirements);

		List<IProvidedCapability> providedCapabilities = new ArrayList<>();
		IProvidedCapability providedCapability = MetadataFactory
				.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, id, version);
		providedCapabilities.add(providedCapability);
		description.addProvidedCapabilities(providedCapabilities);

		return MetadataFactory.createInstallableUnit(description);
	}

	private IInstallableUnit createIURepresentingTheProfile(Set<IRequirement> allRequirements) {
		InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
		String time = Long.toString(System.currentTimeMillis());
		iud.setId(time);
		iud.setVersion(Version.createOSGi(0, 0, 0, time));
		iud.setRequirements(allRequirements.toArray(new IRequirement[allRequirements.size()]));
		return MetadataFactory.createInstallableUnit(iud);
	}

	// The planner uses installable unit properties to keep track of what it has
	// been asked to install. This updates this information
	// It returns at index 0 a meta IU representing everything that needs to be
	// installed
	// It returns at index 1 all the IUs that are in the profile after the removal
	// have been done, but before the addition have been done
	private Object[] updatePlannerInfo(ProfileChangeRequest profileChangeRequest, ProvisioningContext context) {
		IQueryResult<IInstallableUnit> alreadyInstalled = profileChangeRequest.getProfile()
				.query(new IUProfilePropertyQuery(INCLUSION_RULES, IUProfilePropertyQuery.ANY), null);

		Collection<IInstallableUnit> additionRequested = profileChangeRequest.getAdditions();
		Collection<IInstallableUnit> removalRequested = profileChangeRequest.getRemovals();

		for (Map.Entry<IInstallableUnit, List<String>> object : profileChangeRequest
				.getInstallableUnitProfilePropertiesToRemove().entrySet()) {
			if (object.getValue().contains(INCLUSION_RULES))
				profileChangeRequest.setInstallableUnitProfileProperty(object.getKey(), INCLUSION_RULES,
						ProfileInclusionRules.createStrictInclusionRule(object.getKey()));
		}
		// Remove the iu properties associated to the ius removed and the iu properties
		// being removed as well
		if (!removalRequested.isEmpty()) {
			for (Iterator<IInstallableUnit> iterator = alreadyInstalled.iterator(); iterator.hasNext();) {
				IInstallableUnit iu = iterator.next();
				for (IInstallableUnit removed : removalRequested) {
					if (iu.equals(removed)) {
						profileChangeRequest.removeInstallableUnitProfileProperty(removed, INCLUSION_RULES);
						iterator.remove();
						break;
					}
				}
			}
		}
		Set<IRequirement> gatheredRequirements = new HashSet<>();

		// Process all the IUs being added
		Map<IInstallableUnit, Map<String, String>> iuPropertiesToAdd = profileChangeRequest
				.getInstallableUnitProfilePropertiesToAdd();
		for (IInstallableUnit added : additionRequested) {
			Map<String, String> propertiesForIU = iuPropertiesToAdd.get(added);
			IRequirement profileRequirement = null;
			if (propertiesForIU != null) {
				profileRequirement = createRequirement(added, propertiesForIU.get(INCLUSION_RULES));
			}
			if (profileRequirement == null) {
				profileChangeRequest.setInstallableUnitProfileProperty(added, INCLUSION_RULES,
						ProfileInclusionRules.createStrictInclusionRule(added));
				profileRequirement = createStrictRequirement(added);
			}
			gatheredRequirements.add(profileRequirement);
		}

		// Process the IUs that were already there
		for (IInstallableUnit iu : alreadyInstalled) {
			Map<String, String> propertiesForIU = iuPropertiesToAdd.get(iu);
			IRequirement profileRequirement = null;
			// Test if the value has changed
			if (propertiesForIU != null) {
				profileRequirement = createRequirement(iu, propertiesForIU.get(INCLUSION_RULES));
			}
			if (profileRequirement == null) {
				profileRequirement = createRequirement(iu,
						profileChangeRequest.getProfile().getInstallableUnitProperty(iu, INCLUSION_RULES));
			}
			gatheredRequirements.add(profileRequirement);
		}

		// Now add any other requirement that we need to see satisfied
		if (profileChangeRequest.getExtraRequirements() != null)
			gatheredRequirements.addAll(profileChangeRequest.getExtraRequirements());
		IInstallableUnit[] existingRoots = profileChangeRequest.getProfile()
				.query(new IUProfilePropertyQuery(INCLUSION_RULES, IUProfilePropertyQuery.ANY), null)
				.toArray(IInstallableUnit.class);
		return new Object[] { createIURepresentingTheProfile(gatheredRequirements), existingRoots };
	}

	private IRequirement createRequirement(IInstallableUnit iu, String rule) {
		if (rule == null)
			return null;
		if (rule.equals(ProfileInclusionRules.createStrictInclusionRule(iu))) {
			return createStrictRequirement(iu);
		}
		if (rule.equals(ProfileInclusionRules.createOptionalInclusionRule(iu))) {
			return createOptionalRequirement(iu);
		}
		return null;
	}

	private IRequirement createOptionalRequirement(IInstallableUnit iu) {
		return MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(),
				new VersionRange(iu.getVersion(), true, iu.getVersion(), true), null, true, false, true);
	}

	private IRequirement createStrictRequirement(IInstallableUnit iu) {
		return MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(),
				new VersionRange(iu.getVersion(), true, iu.getVersion(), true), null, false, false, true);
	}

	@Override
	public IQueryResult<IInstallableUnit> updatesFor(IInstallableUnit toUpdate, ProvisioningContext context,
			IProgressMonitor monitor) {
		Map<String, IInstallableUnit> resultsMap = new HashMap<>();

		SubMonitor sub = SubMonitor.convert(monitor, 1000);
		IQueryable<IInstallableUnit> queryable = context.getMetadata(sub.newChild(500));
		IQueryResult<IInstallableUnit> matches = queryable.query(new UpdateQuery(toUpdate), sub.newChild(500));
		for (IInstallableUnit iu : matches) {
			String key = iu.getId() + "_" + iu.getVersion().toString(); //$NON-NLS-1$
			IInstallableUnit currentIU = resultsMap.get(key);
			if (currentIU == null || hasHigherFidelity(iu, currentIU))
				resultsMap.put(key, iu);
		}
		sub.done();
		return new CollectionResult<>(resultsMap.values());
	}

	// helper class to trick the resolver to believe that everything is optional
	private static class EverythingOptionalProfile implements IProfile {
		private IProfile profile;

		public EverythingOptionalProfile(IProfile p) {
			profile = p;
		}

		@Override
		public IQueryResult<IInstallableUnit> available(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
			return profile.available(query, monitor);
		}

		@Override
		public Map<String, String> getInstallableUnitProperties(IInstallableUnit iu) {
			return profile.getInstallableUnitProperties(iu);
		}

		@Override
		public String getInstallableUnitProperty(IInstallableUnit iu, String key) {
			if (INCLUSION_RULES.equals(key))
				return ProfileInclusionRules.createOptionalInclusionRule(iu);
			return profile.getInstallableUnitProperty(iu, key);
		}

		@Override
		public String getProfileId() {
			return profile.getProfileId();
		}

		@Override
		public Map<String, String> getProperties() {
			return profile.getProperties();
		}

		@Override
		public String getProperty(String key) {
			return profile.getProperty(key);
		}

		@Override
		public IProvisioningAgent getProvisioningAgent() {
			return profile.getProvisioningAgent();
		}

		@Override
		public long getTimestamp() {
			return profile.getTimestamp();
		}

		@Override
		public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
			return profile.query(query, monitor);
		}
	}

	@Override
	public IProfileChangeRequest createChangeRequest(IProfile profileToChange) {
		return new ProfileChangeRequest(profileToChange);
	}
}
