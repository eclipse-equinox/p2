/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	IBM Corporation - initial API and implementation
 * 	Genuitec - bug fixes
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.rollback.FormerState;
import org.eclipse.equinox.internal.provisional.p2.core.*;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.osgi.util.NLS;

public class SimplePlanner implements IPlanner {
	private static boolean DEBUG = Tracing.DEBUG_PLANNER_OPERANDS;

	private static final int ExpandWork = 12;
	private static final String INCLUDE_PROFILE_IUS = "org.eclipse.equinox.p2.internal.profileius"; //$NON-NLS-1$
	public static final String INCLUSION_RULES = "org.eclipse.equinox.p2.internal.inclusion.rules"; //$NON-NLS-1$
	private static final String ID_IU_FOR_ACTIONS = "org.eclipse.equinox.p2.engine.actions.root"; //$NON-NLS-1$
	private static final String EXPLANATION = "org.eclipse.equinox.p2.director.explain"; //$NON-NLS-1$
	private static final String CONSIDER_METAREQUIREMENTS = "org.eclipse.equinox.p2.planner.resolveMetaRequirements"; //$NON-NLS-1$

	private ProvisioningPlan generateProvisioningPlan(Collection fromState, Collection toState, ProfileChangeRequest changeRequest, ProvisioningPlan installerPlan) {
		InstallableUnitOperand[] iuOperands = generateOperations(fromState, toState);
		PropertyOperand[] propertyOperands = generatePropertyOperations(changeRequest);

		Operand[] operands = new Operand[iuOperands.length + propertyOperands.length];
		System.arraycopy(iuOperands, 0, operands, 0, iuOperands.length);
		System.arraycopy(propertyOperands, 0, operands, iuOperands.length, propertyOperands.length);

		if (DEBUG) {
			for (int i = 0; i < operands.length; i++) {
				Tracing.debug(operands[i].toString());
			}
		}
		return new ProvisioningPlan(Status.OK_STATUS, operands, computeActualChangeRequest(toState, changeRequest), null, installerPlan, changeRequest);
	}

	private Map[] buildDetailedErrors(ProfileChangeRequest changeRequest) {
		IInstallableUnit[] added = changeRequest.getAddedInstallableUnits();
		IInstallableUnit[] removed = changeRequest.getRemovedInstallableUnits();
		Map requestStatus = new HashMap(added.length + removed.length);
		for (int i = 0; i < added.length; i++) {
			requestStatus.put(added[i], new RequestStatus(added[i], RequestStatus.ADDED, IStatus.ERROR, null));
		}
		for (int i = 0; i < removed.length; i++) {
			requestStatus.put(removed[i], new RequestStatus(removed[i], RequestStatus.REMOVED, IStatus.ERROR, null));
		}
		return new Map[] {requestStatus, null};
	}

	private Map[] computeActualChangeRequest(Collection toState, ProfileChangeRequest changeRequest) {
		IInstallableUnit[] added = changeRequest.getAddedInstallableUnits();
		IInstallableUnit[] removed = changeRequest.getRemovedInstallableUnits();
		Map requestStatus = new HashMap(added.length + removed.length);
		for (int i = 0; i < added.length; i++) {
			if (toState.contains(added[i]))
				requestStatus.put(added[i], new RequestStatus(added[i], RequestStatus.ADDED, IStatus.OK, null));
			else
				requestStatus.put(added[i], new RequestStatus(added[i], RequestStatus.ADDED, IStatus.ERROR, null));
		}

		for (int i = 0; i < removed.length; i++) {
			if (!toState.contains(removed[i]))
				requestStatus.put(removed[i], new RequestStatus(removed[i], RequestStatus.REMOVED, IStatus.OK, null));
			else
				requestStatus.put(removed[i], new RequestStatus(removed[i], RequestStatus.REMOVED, IStatus.ERROR, null));
		}

		//Compute the side effect changes (e.g. things installed optionally going away)
		Collection includedIUs = new HashSet(changeRequest.getProfile().query(new IUProfilePropertyQuery(changeRequest.getProfile(), INCLUSION_RULES, null), new Collector(), null).toCollection());
		Map sideEffectStatus = new HashMap(includedIUs.size());
		includedIUs.removeAll(toState);
		for (Iterator iterator = includedIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit removal = (IInstallableUnit) iterator.next();
			if (!requestStatus.containsKey(removal))
				sideEffectStatus.put(removal, new RequestStatus(removal, RequestStatus.REMOVED, IStatus.INFO, null));
		}
		return new Map[] {requestStatus, sideEffectStatus};
	}

	/**
	 * Converts a set containing a list of resolver explanations into a human-readable status object.
	 */
	private IStatus convertExplanationToStatus(Set explanations) {
		if (explanations == null)
			return new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, Messages.Director_Unsatisfied_Dependencies);
		MultiStatus root = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, Messages.Director_Unsatisfied_Dependencies, null);
		//try to find a more specific root message if possible
		String specificMessage = null;
		for (Iterator it = explanations.iterator(); it.hasNext();) {
			final Object next = it.next();
			if (next instanceof Explanation) {
				root.add(((Explanation) next).toStatus());
				if (specificMessage == null && next instanceof Explanation.MissingIU)
					specificMessage = Messages.Explanation_rootMissing;
				else if (specificMessage == null && next instanceof Explanation.Singleton) {
					specificMessage = Messages.Explanation_rootSingleton;
				}
			} else
				root.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, next.toString()));
		}
		//use a more specific root message if available
		if (specificMessage != null) {
			MultiStatus newRoot = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, specificMessage, null);
			newRoot.merge(root);
			root = newRoot;
		}
		return root;
	}

	private PropertyOperand[] generatePropertyOperations(ProfileChangeRequest profileChangeRequest) {
		IProfile profile = profileChangeRequest.getProfile();
		List operands = new ArrayList();
		// First deal with profile properties to remove.  Only generate an operand if the property was there in the first place
		String[] toRemove = profileChangeRequest.getPropertiesToRemove();
		Map existingProperties = profile.getProperties();
		for (int i = 0; i < toRemove.length; i++) {
			if (existingProperties.containsKey(toRemove[i]))
				operands.add(new PropertyOperand(toRemove[i], existingProperties.get(toRemove[i]), null));
		}
		// Now deal with profile property changes/additions
		Map propertyChanges = profileChangeRequest.getPropertiesToAdd();
		Iterator iter = propertyChanges.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			operands.add(new PropertyOperand((String) entry.getKey(), existingProperties.get(entry.getKey()), entry.getValue()));
		}
		// Now deal with iu property changes/additions.
		// TODO we aren't yet checking that the IU will exist in the final profile, will the engine do this?
		Map allIUPropertyChanges = profileChangeRequest.getInstallableUnitProfilePropertiesToAdd();
		iter = allIUPropertyChanges.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			IInstallableUnit iu = (IInstallableUnit) entry.getKey();
			Map iuPropertyChanges = (Map) entry.getValue();
			Iterator iuPropIter = iuPropertyChanges.entrySet().iterator();
			while (iuPropIter.hasNext()) {
				Map.Entry entry2 = (Map.Entry) iuPropIter.next();
				Object oldValue = profile.getInstallableUnitProperty(iu, (String) entry2.getKey());
				operands.add(new InstallableUnitPropertyOperand(iu, (String) entry2.getKey(), oldValue, entry2.getValue()));
			}
		}
		// Now deal with iu property removals.
		// TODO we could optimize by not generating property removals for IU's that aren't there or won't be there.
		Map allIUPropertyDeletions = profileChangeRequest.getInstallableUnitProfilePropertiesToRemove();
		iter = allIUPropertyDeletions.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			IInstallableUnit iu = (IInstallableUnit) entry.getKey();
			Map existingIUProperties = profile.getInstallableUnitProperties(iu);
			List iuPropertyRemovals = (List) entry.getValue();
			for (Iterator it = iuPropertyRemovals.iterator(); it.hasNext();) {
				String key = (String) it.next();
				if (existingIUProperties.containsKey(key))
					operands.add(new InstallableUnitPropertyOperand(iu, key, existingIUProperties.get(key), null));
			}

		}
		return (PropertyOperand[]) operands.toArray(new PropertyOperand[operands.size()]);
	}

	private InstallableUnitOperand[] generateOperations(Collection fromState, Collection toState) {
		return new OperationGenerator().generateOperation(fromState, toState);
	}

	public ProvisioningPlan getDiffPlan(IProfile currentProfile, IProfile targetProfile, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, ExpandWork);
		sub.setTaskName(Messages.Director_Task_Resolving_Dependencies);
		try {
			ProfileChangeRequest profileChangeRequest = FormerState.generateProfileDeltaChangeRequest(currentProfile, targetProfile);
			ProvisioningContext context = new ProvisioningContext(new URI[0]);
			if (context.getProperty(INCLUDE_PROFILE_IUS) == null)
				context.setProperty(INCLUDE_PROFILE_IUS, Boolean.FALSE.toString());
			context.setExtraIUs(new ArrayList(targetProfile.available(InstallableUnitQuery.ANY, new Collector(), null).toCollection()));
			return getProvisioningPlan(profileChangeRequest, context, sub.newChild(ExpandWork / 2));
		} finally {
			sub.done();
		}
	}

	public static IInstallableUnit[] findPlannerMarkedIUs(final IProfile profile) {
		Query markerQuery = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				if (!(candidate instanceof IInstallableUnit))
					return false;

				IInstallableUnit iu = (IInstallableUnit) candidate;

				String inclusion = profile.getInstallableUnitProperty(iu, INCLUSION_RULES);
				return (inclusion != null);
			}
		};
		return (IInstallableUnit[]) profile.query(markerQuery, new Collector(), null).toArray(IInstallableUnit.class);
	}

	public static Dictionary createSelectionContext(Map properties) {
		Hashtable result = new Hashtable(properties);
		String environments = (String) properties.get(IProfile.PROP_ENVIRONMENTS);
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

	private IInstallableUnit[] gatherAvailableInstallableUnits(IInstallableUnit[] additionalSource, URI[] repositories, ProvisioningContext context, IProgressMonitor monitor) {
		Map resultsMap = new HashMap();
		if (additionalSource != null) {
			for (int i = 0; i < additionalSource.length; i++) {
				String key = additionalSource[i].getId() + "_" + additionalSource[i].getVersion().toString(); //$NON-NLS-1$
				resultsMap.put(key, additionalSource[i]);
			}
		}
		if (context != null) {
			for (Iterator iter = context.getExtraIUs().iterator(); iter.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) iter.next();
				String key = iu.getId() + '_' + iu.getVersion().toString();
				resultsMap.put(key, iu);
			}
		}

		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) ServiceHelper.getService(DirectorActivator.context, IMetadataRepositoryManager.class.getName());
		if (repositories == null)
			repositories = repoMgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);

		SubMonitor sub = SubMonitor.convert(monitor, repositories.length * 200);
		for (int i = 0; i < repositories.length; i++) {
			try {
				if (sub.isCanceled())
					throw new OperationCanceledException();

				IMetadataRepository repository = repoMgr.loadRepository(repositories[i], sub.newChild(100));
				Collector matches = repository.query(new InstallableUnitQuery(null, VersionRange.emptyRange), new Collector(), sub.newChild(100));
				for (Iterator it = matches.iterator(); it.hasNext();) {
					IInstallableUnit iu = (IInstallableUnit) it.next();
					String key = iu.getId() + "_" + iu.getVersion().toString(); //$NON-NLS-1$
					IInstallableUnit currentIU = (IInstallableUnit) resultsMap.get(key);
					if (currentIU == null || hasHigherFidelity(iu, currentIU))
						resultsMap.put(key, iu);
				}
			} catch (ProvisionException e) {
				//skip unreadable repositories
			}
		}
		sub.done();
		Collection results = resultsMap.values();
		return (IInstallableUnit[]) results.toArray(new IInstallableUnit[results.size()]);
	}

	private static boolean hasHigherFidelity(IInstallableUnit iu, IInstallableUnit currentIU) {
		if (Boolean.valueOf(currentIU.getProperty(IInstallableUnit.PROP_PARTIAL_IU)).booleanValue() && !Boolean.valueOf(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU)).booleanValue())
			return true;
		return false;
	}

	private boolean satisfyMetaRequirements(Map props) {
		if (props == null)
			return true;
		if (props.get(CONSIDER_METAREQUIREMENTS) == null || "true".equalsIgnoreCase((String) props.get(CONSIDER_METAREQUIREMENTS))) //$NON-NLS-1$
			return true;
		return false;
	}

	private boolean satisfyMetaRequirements(IProfile p) {
		return satisfyMetaRequirements(p.getProperties());
	}

	//Return the set of IUs representing the complete future state of the profile to satisfy the request or return a ProvisioningPlan when the request can not be satisfied
	private Object getSolutionFor(ProfileChangeRequest profileChangeRequest, ProvisioningContext context, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, ExpandWork);
		sub.setTaskName(Messages.Director_Task_Resolving_Dependencies);
		try {
			IProfile profile = profileChangeRequest.getProfile();

			Object[] updatedPlan = updatePlannerInfo(profileChangeRequest, context);

			URI[] metadataRepositories = (context != null) ? context.getMetadataRepositories() : null;
			Dictionary newSelectionContext = createSelectionContext(profileChangeRequest.getProfileProperties());

			List extraIUs = new ArrayList(Arrays.asList(profileChangeRequest.getAddedInstallableUnits()));
			extraIUs.addAll(Arrays.asList(profileChangeRequest.getRemovedInstallableUnits()));
			if (context == null || context.getProperty(INCLUDE_PROFILE_IUS) == null || context.getProperty(INCLUDE_PROFILE_IUS).equalsIgnoreCase(Boolean.TRUE.toString()))
				extraIUs.addAll(profile.available(InstallableUnitQuery.ANY, new Collector(), null).toCollection());

			IInstallableUnit[] availableIUs = gatherAvailableInstallableUnits((IInstallableUnit[]) extraIUs.toArray(new IInstallableUnit[extraIUs.size()]), metadataRepositories, context, sub.newChild(ExpandWork / 4));

			Slicer slicer = new Slicer(new QueryableArray(availableIUs), newSelectionContext, satisfyMetaRequirements(profileChangeRequest.getProfileProperties()));
			IQueryable slice = slicer.slice(new IInstallableUnit[] {(IInstallableUnit) updatedPlan[0]}, sub.newChild(ExpandWork / 4));
			if (slice == null)
				return new ProvisioningPlan(slicer.getStatus(), profileChangeRequest, null);
			Projector projector = new Projector(slice, newSelectionContext, satisfyMetaRequirements(profileChangeRequest.getProfileProperties()));
			projector.encode((IInstallableUnit) updatedPlan[0], (IInstallableUnit[]) updatedPlan[1], profileChangeRequest.getAddedInstallableUnits(), sub.newChild(ExpandWork / 4));
			IStatus s = projector.invokeSolver(sub.newChild(ExpandWork / 4));
			if (s.getSeverity() == IStatus.CANCEL)
				return new ProvisioningPlan(s, profileChangeRequest, null);
			if (s.getSeverity() == IStatus.ERROR) {
				sub.setTaskName(Messages.Planner_NoSolution);
				if (context != null && !(context.getProperty(EXPLANATION) == null || Boolean.TRUE.toString().equalsIgnoreCase(context.getProperty(EXPLANATION))))
					return new ProvisioningPlan(s, profileChangeRequest, null);

				//Extract the explanation
				Set explanation = projector.getExplanation(sub.newChild(ExpandWork / 4));
				IStatus explanationStatus = convertExplanationToStatus(explanation);
				return new ProvisioningPlan(explanationStatus, new Operand[0], buildDetailedErrors(profileChangeRequest), new RequestStatus(null, RequestStatus.REMOVED, IStatus.ERROR, explanation), null, profileChangeRequest);
			}
			//The resolution succeeded. We can forget about the warnings since there is a solution.
			if (Tracing.DEBUG && s.getSeverity() != IStatus.OK)
				LogHelper.log(s);
			s = Status.OK_STATUS;

			return projector;
		} finally {
			sub.done();
		}
	}

	public ProvisioningPlan getProvisioningPlan(ProfileChangeRequest profileChangeRequest, ProvisioningContext context, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, ExpandWork);
		sub.setTaskName(Messages.Director_Task_Resolving_Dependencies);
		try {
			//Get the solution for the initial request
			Object resolutionResult = getSolutionFor(profileChangeRequest, context, sub.newChild(ExpandWork / 2));
			if (resolutionResult instanceof ProvisioningPlan)
				return (ProvisioningPlan) resolutionResult;

			Collection newState = ((Projector) resolutionResult).extractSolution();
			Collection fullState = new ArrayList();
			fullState.addAll(newState);
			newState = AttachmentHelper.attachFragments(newState, ((Projector) resolutionResult).getFragmentAssociation());

			ProvisioningPlan temporaryPlan = generatePlan((Projector) resolutionResult, newState, profileChangeRequest);

			//Create a plan for installing necessary pieces to complete the installation (e.g touchpoint actions)
			return createInstallerPlan(profileChangeRequest.getProfile(), profileChangeRequest, fullState, newState, temporaryPlan, context, sub.newChild(ExpandWork / 2));
		} catch (OperationCanceledException e) {
			return new ProvisioningPlan(Status.CANCEL_STATUS, profileChangeRequest, null);
		} finally {
			sub.done();
		}
	}

	//Verify that all the meta requirements necessary to perform the uninstallation (if necessary) and all t
	private Collection areMetaRequirementsSatisfied(IProfile oldProfile, Collection newProfile, ProvisioningPlan initialPlan) {
		Collection allMetaRequirements = extractMetaRequirements(newProfile, initialPlan);
		for (Iterator iterator = allMetaRequirements.iterator(); iterator.hasNext();) {
			IRequiredCapability requirement = (IRequiredCapability) iterator.next();
			if (oldProfile.query(new CapabilityQuery(requirement), new HasMatchCollector(), null).isEmpty())
				return allMetaRequirements;
		}
		return null;
	}

	//Return all the meta requirements for the list of IU specified and all the meta requirements listed necessary to satisfy the uninstallation 
	private Collection extractMetaRequirements(Collection ius, ProvisioningPlan plan) {
		Set allMetaRequirements = new HashSet();
		for (Iterator iterator = ius.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			IRequiredCapability[] reqs = iu.getMetaRequiredCapabilities();
			for (int i = 0; i < reqs.length; i++) {
				allMetaRequirements.add(reqs[i]);
			}
		}
		Collector c2 = plan.getRemovals().query(InstallableUnitQuery.ANY, new Collector(), null);
		for (Iterator iterator = c2.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			IRequiredCapability[] reqs = iu.getMetaRequiredCapabilities();
			for (int i = 0; i < reqs.length; i++) {
				allMetaRequirements.add(reqs[i]);
			}
		}
		return allMetaRequirements;
	}

	private ProvisioningPlan createInstallerPlan(IProfile profile, ProfileChangeRequest initialRequest, Collection unattachedState, Collection expectedState, ProvisioningPlan initialPlan, ProvisioningContext initialContext, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, ExpandWork);

		try {
			sub.setTaskName(Messages.Director_Task_installer_plan);
			IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(DirectorActivator.context, IProfileRegistry.class.getName());
			if (profileRegistry == null)
				return new ProvisioningPlan(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, Messages.Planner_no_profile_registry), initialRequest, null);

			IProfile agentProfile = profileRegistry.getProfile(IProfileRegistry.SELF);
			if (agentProfile == null)
				return initialPlan;

			if (profile.getProfileId().equals(agentProfile.getProfileId())) {
				if (profile.getTimestamp() != agentProfile.getTimestamp())
					return new ProvisioningPlan(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_profile_out_of_sync, profile.getProfileId())), initialRequest, null);
				return createInstallerPlanForCohostedCase(profile, initialRequest, initialPlan, unattachedState, expectedState, initialContext, sub);
			}

			if (satisfyMetaRequirements(profile) && !profile.getProfileId().equals(agentProfile.getProfileId())) {
				return createInstallerPlanForCohostedCaseFromExternalInstaller(profile, initialRequest, initialPlan, expectedState, initialContext, agentProfile, sub);
			}
			return createInstallerPlanForExternalInstaller(profile, initialRequest, initialPlan, expectedState, initialContext, agentProfile, sub);
		} finally {
			sub.done();
		}
	}

	private ProvisioningPlan createInstallerPlanForCohostedCaseFromExternalInstaller(IProfile profile, ProfileChangeRequest initialRequest, ProvisioningPlan initialPlan, Collection newState, ProvisioningContext initialContext, IProfile agentProfile, SubMonitor sub) {
		ProvisioningPlan planForProfile = generatePlan(null, newState, initialRequest);
		return createInstallerPlanForExternalInstaller(profile, initialRequest, planForProfile, newState, initialContext, agentProfile, sub);
	}

	//Deal with the case where the agent profile is different than the one being provisioned
	private ProvisioningPlan createInstallerPlanForExternalInstaller(IProfile targetedProfile, ProfileChangeRequest initialRequest, ProvisioningPlan initialPlan, Collection expectedState, ProvisioningContext initialContext, IProfile agentProfile, SubMonitor sub) {
		Collection metaRequirements = areMetaRequirementsSatisfied(agentProfile, expectedState, initialPlan);
		if (metaRequirements == null)
			return initialPlan;

		IInstallableUnit actionsIU = createIUForMetaRequirements(targetedProfile, metaRequirements);
		IInstallableUnit previousActionsIU = getPreviousIUForMetaRequirements(agentProfile, getActionGatheringIUId(targetedProfile), sub);

		ProfileChangeRequest agentRequest = new ProfileChangeRequest(agentProfile);
		agentRequest.addInstallableUnits(new IInstallableUnit[] {actionsIU});
		if (previousActionsIU != null)
			agentRequest.removeInstallableUnits(new IInstallableUnit[] {previousActionsIU});
		Object externalInstallerPlan = getSolutionFor(agentRequest, initialContext, sub.newChild(10));
		if (externalInstallerPlan instanceof ProvisioningPlan && ((ProvisioningPlan) externalInstallerPlan).getStatus().getSeverity() == IStatus.ERROR) {
			MultiStatus externalInstallerStatus = new MultiStatus(DirectorActivator.PI_DIRECTOR, 0, Messages.Planner_can_not_install_preq, null);
			externalInstallerStatus.add(((ProvisioningPlan) externalInstallerPlan).getStatus());
			return new ProvisioningPlan(externalInstallerStatus, initialRequest, new ProvisioningPlan(externalInstallerStatus, agentRequest, null));
		}

		initialPlan.setInstallerPlan(generatePlan((Projector) externalInstallerPlan, null, agentRequest));
		return initialPlan;
	}

	//Deal with the case where the actions needs to be installed in the same profile than the one we are performing the initial request
	//The expectedState represents the result of the initialRequest where the metaRequirements have been satisfied.
	private ProvisioningPlan createInstallerPlanForCohostedCase(IProfile profile, ProfileChangeRequest initialRequest, ProvisioningPlan initialPlan, Collection unattachedState, Collection expectedState, ProvisioningContext initialContext, SubMonitor monitor) {
		Collection metaRequirements = initialRequest.getRemovedInstallableUnits().length == 0 ? areMetaRequirementsSatisfied(profile, expectedState, initialPlan) : extractMetaRequirements(expectedState, initialPlan);
		if (metaRequirements == null || metaRequirements.isEmpty())
			return initialPlan;

		//Let's compute a plan that satisfy all the metaRequirements. We limit ourselves to only the IUs that were part of the previous solution.
		IInstallableUnit metaRequirementIU = createIUForMetaRequirements(profile, metaRequirements);
		IInstallableUnit previousMetaRequirementIU = getPreviousIUForMetaRequirements(profile, getActionGatheringIUId(profile), monitor);

		//Create an agent request from the initial request
		ProfileChangeRequest agentRequest = new ProfileChangeRequest(profile);
		for (Iterator it = initialRequest.getPropertiesToAdd().entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			agentRequest.setProfileProperty((String) entry.getKey(), entry.getValue());
		}
		String[] removedProperties = initialRequest.getPropertiesToRemove();
		for (int i = 0; i < removedProperties.length; i++) {
			agentRequest.removeProfileProperty(removedProperties[i]);
		}
		Map removedIUProperties = initialRequest.getInstallableUnitProfilePropertiesToRemove();
		for (Iterator iterator = removedIUProperties.entrySet().iterator(); iterator.hasNext();) {
			Entry entry = (Entry) iterator.next();
			ArrayList value = (ArrayList) entry.getValue();
			for (Iterator iterator2 = value.iterator(); iterator2.hasNext();) {
				agentRequest.removeInstallableUnitProfileProperty((IInstallableUnit) entry.getKey(), (String) iterator2.next());
			}
		}

		if (previousMetaRequirementIU != null)
			agentRequest.removeInstallableUnits(new IInstallableUnit[] {previousMetaRequirementIU});
		agentRequest.addInstallableUnits(new IInstallableUnit[] {metaRequirementIU});

		ProvisioningContext agentCtx = new ProvisioningContext(new URI[0]);
		ArrayList extraIUs = new ArrayList(unattachedState);
		agentCtx.setExtraIUs(extraIUs);
		Object agentSolution = getSolutionFor(agentRequest, agentCtx, monitor.newChild(3));
		if (agentSolution instanceof ProvisioningPlan && ((ProvisioningPlan) agentSolution).getStatus().getSeverity() == IStatus.ERROR) {
			MultiStatus agentStatus = new MultiStatus(DirectorActivator.PI_DIRECTOR, 0, Messages.Planner_actions_and_software_incompatible, null);
			agentStatus.add(((ProvisioningPlan) agentSolution).getStatus());
			return new ProvisioningPlan(agentStatus, initialRequest, new ProvisioningPlan(agentStatus, agentRequest, null));
		}

		//Compute the installer plan. It is the difference between what is currently in the profile and the solution we just computed
		Collection agentState = ((Projector) agentSolution).extractSolution();
		agentState.remove(metaRequirementIU); //Remove the fake IU
		agentState = AttachmentHelper.attachFragments(agentState, ((Projector) agentSolution).getFragmentAssociation());

		ProvisioningContext noRepoContext = createNoRepoContext(initialRequest);
		//...This computes the attachment of what is currently in the profile 
		Object initialSolution = getSolutionFor(new ProfileChangeRequest(new EverythingOptionalProfile(initialRequest.getProfile())), noRepoContext, new NullProgressMonitor());
		if (initialSolution instanceof ProvisioningPlan) {
			LogHelper.log(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, "The resolution of the previous state contained in profile " + initialRequest.getProfile().getProfileId() + " version " + initialRequest.getProfile().getTimestamp() + " failed.")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			return (ProvisioningPlan) initialSolution;
		}
		Collection initialState = initialRequest.getProfile().query(InstallableUnitQuery.ANY, new Collector(), null).toCollection();
		initialState = AttachmentHelper.attachFragments(initialState, ((Projector) initialSolution).getFragmentAssociation());

		ProvisioningPlan agentPlan = generateProvisioningPlan(initialState, agentState, initialRequest, null);

		//Compute the installation plan. It is the difference between the state after the installer plan has run and the expectedState.
		return generateProvisioningPlan(agentState, expectedState, initialRequest, agentPlan);
	}

	//Compute the set of operands based on the solution obtained previously
	private ProvisioningPlan generatePlan(Projector newSolution, Collection newState, ProfileChangeRequest request) {
		//Compute the attachment of the new state if not provided
		if (newState == null) {
			newState = newSolution.extractSolution();
			newState = AttachmentHelper.attachFragments(newState, newSolution.getFragmentAssociation());
		}
		ProvisioningContext noRepoContext = createNoRepoContext(request);

		//Compute the attachment of the previous state
		Object initialSolution = getSolutionFor(new ProfileChangeRequest(new EverythingOptionalProfile(request.getProfile())), noRepoContext, new NullProgressMonitor());
		if (initialSolution instanceof ProvisioningPlan) {
			LogHelper.log(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, "The resolution of the previous state contained in profile " + request.getProfile().getProfileId() + " version " + request.getProfile().getTimestamp() + " failed.")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			return (ProvisioningPlan) initialSolution;
		}
		Collection initialState = request.getProfile().query(InstallableUnitQuery.ANY, new Collector(), null).toCollection();
		initialState = AttachmentHelper.attachFragments(initialState, ((Projector) initialSolution).getFragmentAssociation());

		//Generate the plan
		return generateProvisioningPlan(initialState, newState, request, null);
	}

	private ProvisioningContext createNoRepoContext(ProfileChangeRequest request) {
		ProvisioningContext noRepoContext = new ProvisioningContext(new URI[0]);
		noRepoContext.setArtifactRepositories(new URI[0]);
		noRepoContext.setProperty(INCLUDE_PROFILE_IUS, Boolean.FALSE.toString());
		ArrayList extraIUs = new ArrayList();
		extraIUs.addAll(request.getProfile().query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).toCollection());
		noRepoContext.setExtraIUs(extraIUs);
		return noRepoContext;
	}

	private IInstallableUnit getPreviousIUForMetaRequirements(IProfile profile, String iuId, IProgressMonitor monitor) {
		Collector c = profile.query(new InstallableUnitQuery(iuId), new Collector(), monitor);
		if (c.size() == 0)
			return null;
		return (IInstallableUnit) c.toArray(IInstallableUnit.class)[0];
	}

	private String getActionGatheringIUId(IProfile profile) {
		return ID_IU_FOR_ACTIONS + '.' + profile.getProfileId();
	}

	private IInstallableUnit createIUForMetaRequirements(IProfile profile, Collection metaRequirements) {
		InstallableUnitDescription description = new InstallableUnitDescription();
		String id = getActionGatheringIUId(profile);
		description.setId(id);
		Version version = Version.createOSGi(1, 0, 0, Long.toString(profile.getTimestamp()));
		description.setVersion(version);
		description.addRequiredCapabilities(metaRequirements);

		ArrayList providedCapabilities = new ArrayList();
		IProvidedCapability providedCapability = MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, id, version);
		providedCapabilities.add(providedCapability);
		description.addProvidedCapabilities(providedCapabilities);

		IInstallableUnit actionsIU = MetadataFactory.createInstallableUnit(description);
		return actionsIU;
	}

	private IInstallableUnit createIURepresentingTheProfile(ArrayList allRequirements) {
		InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
		String time = Long.toString(System.currentTimeMillis());
		iud.setId(time);
		iud.setVersion(Version.createOSGi(0, 0, 0, time));
		iud.setRequiredCapabilities((IRequiredCapability[]) allRequirements.toArray(new IRequiredCapability[allRequirements.size()]));
		return MetadataFactory.createInstallableUnit(iud);
	}

	//The planner uses installable unit properties to keep track of what it has been asked to install. This updates this information
	//It returns at index 0 a meta IU representing everything that needs to be installed
	//It returns at index 1 all the IUs that are in the profile after the removal have been done, but before the addition have been done 
	private Object[] updatePlannerInfo(ProfileChangeRequest profileChangeRequest, ProvisioningContext context) {
		Collection includedIUs = profileChangeRequest.getProfile().query(new IUProfilePropertyQuery(profileChangeRequest.getProfile(), INCLUSION_RULES, null), new Collector(), null).toCollection();
		Collection alreadyInstalled = new HashSet(includedIUs);

		IInstallableUnit[] added = profileChangeRequest.getAddedInstallableUnits();
		IInstallableUnit[] removed = profileChangeRequest.getRemovedInstallableUnits();

		for (Iterator iterator = profileChangeRequest.getInstallableUnitProfilePropertiesToRemove().entrySet().iterator(); iterator.hasNext();) {
			Map.Entry object = (Map.Entry) iterator.next();
			if (((List) object.getValue()).contains(INCLUSION_RULES))
				profileChangeRequest.setInstallableUnitProfileProperty((IInstallableUnit) object.getKey(), INCLUSION_RULES, PlannerHelper.createStrictInclusionRule((IInstallableUnit) object.getKey()));
		}
		//Remove the iu properties associated to the ius removed and the iu properties being removed as well
		if (removed.length != 0) {
			for (Iterator iterator = alreadyInstalled.iterator(); iterator.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) iterator.next();
				for (int i = 0; i < removed.length; i++) {
					if (iu.equals(removed[i])) {
						profileChangeRequest.removeInstallableUnitProfileProperty(removed[i], INCLUSION_RULES);
						iterator.remove();
						break;
					}
				}
			}
		}
		ArrayList gatheredRequirements = new ArrayList();

		//Process all the IUs being added
		Map iuPropertiesToAdd = profileChangeRequest.getInstallableUnitProfilePropertiesToAdd();
		for (int i = 0; i < added.length; i++) {
			Map propertiesForIU = (Map) iuPropertiesToAdd.get(added[i]);
			IRequiredCapability profileRequirement = null;
			if (propertiesForIU != null) {
				profileRequirement = createRequirement(added[i], (String) propertiesForIU.get(INCLUSION_RULES));
			}
			if (profileRequirement == null) {
				profileChangeRequest.setInstallableUnitProfileProperty(added[i], INCLUSION_RULES, PlannerHelper.createStrictInclusionRule(added[i]));
				profileRequirement = createStrictRequirement(added[i]);
			}
			gatheredRequirements.add(profileRequirement);
		}

		//Process the IUs that were already there
		for (Iterator iterator = alreadyInstalled.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			Map propertiesForIU = (Map) iuPropertiesToAdd.get(iu);
			IRequiredCapability profileRequirement = null;
			//Test if the value has changed
			if (propertiesForIU != null) {
				profileRequirement = createRequirement(iu, (String) propertiesForIU.get(INCLUSION_RULES));
			}
			if (profileRequirement == null) {
				profileRequirement = createRequirement(iu, profileChangeRequest.getProfile().getInstallableUnitProperty(iu, INCLUSION_RULES));
			}
			if (!gatheredRequirements.contains(profileRequirement))
				gatheredRequirements.add(profileRequirement);
		}

		//Now add any other requirement that we need to see satisfied
		if (context != null && context.getAdditionalRequirements() != null)
			gatheredRequirements.addAll(context.getAdditionalRequirements());
		return new Object[] {createIURepresentingTheProfile(gatheredRequirements), (IInstallableUnit[]) alreadyInstalled.toArray(new IInstallableUnit[alreadyInstalled.size()])};
	}

	private IRequiredCapability createRequirement(IInstallableUnit iu, String rule) {
		if (rule == null)
			return null;
		if (rule.equals(PlannerHelper.createStrictInclusionRule(iu))) {
			return createStrictRequirement(iu);
		}
		if (rule.equals(PlannerHelper.createOptionalInclusionRule(iu))) {
			return createOptionalRequirement(iu);
		}
		return null;
	}

	private IRequiredCapability createOptionalRequirement(IInstallableUnit iu) {
		return MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), new VersionRange(iu.getVersion(), true, iu.getVersion(), true), null, true, false, true);
	}

	private IRequiredCapability createStrictRequirement(IInstallableUnit iu) {
		return MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), new VersionRange(iu.getVersion(), true, iu.getVersion(), true), null, false, false, true);
	}

	public IInstallableUnit[] updatesFor(IInstallableUnit toUpdate, ProvisioningContext context, IProgressMonitor monitor) {
		Map resultsMap = new HashMap();

		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) ServiceHelper.getService(DirectorActivator.context, IMetadataRepositoryManager.class.getName());
		URI[] repositories = context.getMetadataRepositories();
		if (repositories == null)
			repositories = repoMgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);

		SubMonitor sub = SubMonitor.convert(monitor, repositories.length * 200);
		for (int i = 0; i < repositories.length; i++) {
			try {
				if (sub.isCanceled())
					throw new OperationCanceledException();
				IMetadataRepository repository = repoMgr.loadRepository(repositories[i], sub.newChild(100));
				Collector matches = repository.query(new UpdateQuery(toUpdate), new Collector(), sub.newChild(100));
				for (Iterator it = matches.iterator(); it.hasNext();) {
					IInstallableUnit iu = (IInstallableUnit) it.next();
					String key = iu.getId() + "_" + iu.getVersion().toString(); //$NON-NLS-1$
					IInstallableUnit currentIU = (IInstallableUnit) resultsMap.get(key);
					if (currentIU == null || hasHigherFidelity(iu, currentIU))
						resultsMap.put(key, iu);
				}
			} catch (ProvisionException e) {
				//skip unreadable repositories
			}
		}
		sub.done();
		Collection results = resultsMap.values();
		return (IInstallableUnit[]) results.toArray(new IInstallableUnit[results.size()]);
	}

	//helper class to trick the resolver to believe that everything is optional
	private static class EverythingOptionalProfile implements IProfile {
		private IProfile profile;

		public EverythingOptionalProfile(IProfile p) {
			profile = p;
		}

		public Collector available(Query query, Collector collector, IProgressMonitor monitor) {
			return profile.available(query, collector, monitor);
		}

		public Map getInstallableUnitProperties(IInstallableUnit iu) {
			return profile.getInstallableUnitProperties(iu);
		}

		public String getInstallableUnitProperty(IInstallableUnit iu, String key) {
			if (INCLUSION_RULES.equals(key))
				return PlannerHelper.createOptionalInclusionRule(iu);
			return profile.getInstallableUnitProperty(iu, key);
		}

		public Map getLocalProperties() {
			return profile.getLocalProperties();
		}

		public String getLocalProperty(String key) {
			return profile.getLocalProperty(key);
		}

		public IProfile getParentProfile() {
			return profile.getParentProfile();
		}

		public String getProfileId() {
			return profile.getProfileId();
		}

		public Map getProperties() {
			return profile.getProperties();
		}

		public String getProperty(String key) {
			return profile.getProperty(key);
		}

		public String[] getSubProfileIds() {
			return profile.getSubProfileIds();
		}

		public long getTimestamp() {
			return profile.getTimestamp();
		}

		public boolean hasSubProfiles() {
			return profile.hasSubProfiles();
		}

		public boolean isRootProfile() {
			return profile.isRootProfile();
		}

		public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
			return profile.query(query, collector, monitor);
		}
	}
}
