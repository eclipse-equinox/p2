/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * 	IBM Corporation - initial API and implementation
 * 	Genuitec - bug fixes
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.resolution.ResolutionHelper;
import org.eclipse.equinox.internal.p2.rollback.FormerState;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.UpdateQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Version;

public class SimplePlanner implements IPlanner {
	private static final int ExpandWork = 12;
	private static final String PLANNER_MARKER = "private.org.eclipse.equinox.p2.planner.installed"; //$NON-NLS-1$
	public static final String INCLUSION_RULES = "org.eclipse.equinox.p2.internal.inclusion.rules"; //$NON-NLS-1$

	private IProfile getProfile(String profileId) {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(DirectorActivator.context, IProfileRegistry.class.getName());
		if (profileRegistry == null)
			return null;
		return profileRegistry.getProfile(profileId);
	}

	private ProvisioningPlan generateProvisioningPlan(IStatus status, Collection fromState, Collection toState, ProfileChangeRequest changeRequest) {
		InstallableUnitOperand[] iuOperands = generateOperations(fromState, toState);
		PropertyOperand[] propertyOperands = generatePropertyOperations(changeRequest);

		Operand[] operands = new Operand[iuOperands.length + propertyOperands.length];
		System.arraycopy(iuOperands, 0, operands, 0, iuOperands.length);
		System.arraycopy(propertyOperands, 0, operands, iuOperands.length, propertyOperands.length);

		if (status == null)
			status = Status.OK_STATUS;
		return new ProvisioningPlan(status, operands);
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
		Iterator iter = propertyChanges.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			operands.add(new PropertyOperand(key, existingProperties.get(key), propertyChanges.get(key)));
		}
		// Now deal with iu property changes/additions.  
		// TODO we aren't yet checking that the IU will exist in the final profile, will the engine do this?
		Map allIUPropertyChanges = profileChangeRequest.getInstallableUnitProfilePropertiesToAdd();
		iter = allIUPropertyChanges.keySet().iterator();
		while (iter.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) iter.next();
			Map iuPropertyChanges = (Map) allIUPropertyChanges.get(iu);
			Iterator iuPropIter = iuPropertyChanges.keySet().iterator();
			while (iuPropIter.hasNext()) {
				String key = (String) iuPropIter.next();
				Object oldValue = profile.getInstallableUnitProperty(iu, key);
				operands.add(new InstallableUnitPropertyOperand(iu, key, oldValue, iuPropertyChanges.get(key)));
			}
		}
		// Now deal with iu property removals.
		// TODO we could optimize by not generating property removals for IU's that aren't there or won't be there.  
		Map allIUPropertyDeletions = profileChangeRequest.getInstallableUnitProfilePropertiesToRemove();
		iter = allIUPropertyDeletions.keySet().iterator();
		while (iter.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) iter.next();
			Map existingIUProperties = profile.getInstallableUnitProperties(iu);
			List iuPropertyRemovals = (List) allIUPropertyDeletions.get(iu);
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

	public ProvisioningPlan getRevertPlan(IInstallableUnit profileSnapshot, ProvisioningContext context, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, ExpandWork);
		sub.setTaskName(Messages.Director_Task_Resolving_Dependencies);
		try {
			MultiStatus result = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, Messages.Director_Revert_Problems, null);

			if (!Boolean.valueOf(profileSnapshot.getProperty(IInstallableUnit.PROP_TYPE_PROFILE)).booleanValue()) {
				result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Director_Unexpected_IU, profileSnapshot.getId())));
				return new ProvisioningPlan(result);
			}
			IProfile profile = getProfile(profileSnapshot.getId());
			if (profile == null) {
				result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Director_Unexpected_IU, profileSnapshot.getId())));
				return new ProvisioningPlan(result);
			}
			IProfile revertProfile = FormerState.IUToProfile(profileSnapshot, profile, context, sub.newChild(ExpandWork / 2));

			ProfileChangeRequest profileChangeRequest = FormerState.generateProfileDeltaChangeRequest(profile, revertProfile);
			return getProvisioningPlan(profileChangeRequest, context, sub.newChild(ExpandWork / 2));

		} catch (CoreException e) {
			return new ProvisioningPlan(e.getStatus());
		} finally {
			sub.done();
		}
	}

	public static IInstallableUnit[] findPlannerMarkedIUs(final IProfile profile) {
		Query markerQuery = new Query() {
			public boolean isMatch(Object candidate) {
				if (!(candidate instanceof IInstallableUnit))
					return false;

				IInstallableUnit iu = (IInstallableUnit) candidate;

				// TODO: remove marker -- temporary backwards compatibility only
				String marker = profile.getInstallableUnitProperty(iu, PLANNER_MARKER);
				if (marker != null && Boolean.valueOf(marker).booleanValue())
					return true;

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

	public static IInstallableUnit[] gatherAvailableInstallableUnits(IInstallableUnit[] additionalSource, URL[] repositories, ProvisioningContext context, IProgressMonitor monitor) {
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
			repositories = repoMgr.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL);

		SubMonitor sub = SubMonitor.convert(monitor, repositories.length * 200);
		for (int i = 0; i < repositories.length; i++) {
			try {
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

	public ProvisioningPlan getProvisioningPlan(ProfileChangeRequest profileChangeRequest, ProvisioningContext context, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, ExpandWork);
		sub.setTaskName(Messages.Director_Task_Resolving_Dependencies);
		try {
			IProfile profile = profileChangeRequest.getProfile();

			IInstallableUnit[] allIUs = updatePlannerInfo(profileChangeRequest);

			URL[] metadataRepositories = (context != null) ? context.getMetadataRepositories() : null;
			Dictionary newSelectionContext = createSelectionContext(profileChangeRequest.getProfileProperties());

			List extraIUs = new ArrayList(Arrays.asList(profileChangeRequest.getAddedInstallableUnits()));
			extraIUs.addAll(Arrays.asList(profileChangeRequest.getRemovedInstallableUnits()));
			extraIUs.addAll(profile.available(InstallableUnitQuery.ANY, new Collector(), null).toCollection());

			IInstallableUnit[] availableIUs = gatherAvailableInstallableUnits((IInstallableUnit[]) extraIUs.toArray(new IInstallableUnit[extraIUs.size()]), metadataRepositories, context, sub.newChild(ExpandWork / 4));

			Slicer slicer = new Slicer(allIUs, availableIUs, newSelectionContext);
			IQueryable slice = slicer.slice(allIUs, monitor);
			if (slice == null)
				return new ProvisioningPlan(slicer.getStatus());
			IProjector projector = ProjectorFactory.create(slice, newSelectionContext);
			projector.encode(allIUs, sub.newChild(ExpandWork / 4));
			IStatus s = projector.invokeSolver(sub.newChild(ExpandWork / 4));
			if (s.getSeverity() == IStatus.ERROR) {
				sub.setTaskName(Messages.Planner_NoSolution);
				//log the error from the new solver so it is not lost
				LogHelper.log(s);
				if (!"true".equalsIgnoreCase(context == null ? null : context.getProperty("org.eclipse.equinox.p2.disable.error.reporting"))) {
					//We invoke the old resolver to get explanations for now
					IStatus oldResolverStatus = new NewDependencyExpander(allIUs, null, availableIUs, newSelectionContext, false).expand(sub.newChild(ExpandWork / 4));
					if (!oldResolverStatus.isOK())
						s = oldResolverStatus;
				}
				return new ProvisioningPlan(s);
			}
			//The resolution succeeded. We can forget about the warnings since there is a solution.
			if (Tracing.DEBUG && s.getSeverity() != IStatus.OK)
				LogHelper.log(s);
			s = Status.OK_STATUS;

			Collection newState = projector.extractSolution();
			newState.remove(allIUs[0]);

			ResolutionHelper newStateHelper = new ResolutionHelper(newSelectionContext, null);
			newState = newStateHelper.attachCUs(newState);

			ResolutionHelper oldStateHelper = new ResolutionHelper(createSelectionContext(profile.getProperties()), null);
			Collection oldState = oldStateHelper.attachCUs(profile.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection());

			return generateProvisioningPlan(s, oldState, newState, profileChangeRequest);
		} finally {
			sub.done();
		}
	}

	private IInstallableUnit createIURepresentingTheProfile(ArrayList allRequirements) {
		InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
		String time = Long.toString(System.currentTimeMillis());
		iud.setId(time);
		iud.setVersion(new Version(0, 0, 0, time));
		iud.setRequiredCapabilities((RequiredCapability[]) allRequirements.toArray(new RequiredCapability[allRequirements.size()]));
		return MetadataFactory.createInstallableUnit(iud);
	}

	//The planner uses installable unit properties to keep track of what it has been asked to install. This updates this information
	private IInstallableUnit[] updatePlannerInfo(ProfileChangeRequest profileChangeRequest) {
		//Support for backward compatibility. Convert planner_marker properties into strict inclusion rules
		Collector previousMarkers = profileChangeRequest.getProfile().query(new IUProfilePropertyQuery(profileChangeRequest.getProfile(), PLANNER_MARKER, Boolean.TRUE.toString()), new Collector(), null);
		for (Iterator iterator = previousMarkers.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			profileChangeRequest.setInstallableUnitInclusionRules(iu, PlannerHelper.createStrictInclusionRule(iu));
			profileChangeRequest.removeInstallableUnitProfileProperty(iu, PLANNER_MARKER);
		}

		Collection includedIUs = profileChangeRequest.getProfile().query(new IUProfilePropertyQuery(profileChangeRequest.getProfile(), INCLUSION_RULES, null), new Collector(), null).toCollection();
		Collection alreadyInstalled = new HashSet(includedIUs);
		alreadyInstalled.addAll(previousMarkers.toCollection());

		IInstallableUnit[] added = profileChangeRequest.getAddedInstallableUnits();
		IInstallableUnit[] removed = profileChangeRequest.getRemovedInstallableUnits();

		for (Iterator iterator = profileChangeRequest.getInstallableUnitProfilePropertiesToRemove().entrySet().iterator(); iterator.hasNext();) {
			Map.Entry object = (Map.Entry) iterator.next();
			if (((List) object.getValue()).contains(INCLUSION_RULES))
				profileChangeRequest.setInstallableUnitProfileProperty((IInstallableUnit) object.getKey(), INCLUSION_RULES, PlannerHelper.createStrictInclusionRule((IInstallableUnit) object.getKey()));
		}
		//Remove the iu properties associated to the ius removed and the iu properties being removed as well
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

		ArrayList gatheredRequirements = new ArrayList();

		//Process all the IUs being added
		Map iuPropertiesToAdd = profileChangeRequest.getInstallableUnitProfilePropertiesToAdd();
		for (int i = 0; i < added.length; i++) {
			Map propertiesForIU = (Map) iuPropertiesToAdd.get(added[i]);
			RequiredCapability profileRequirement = null;
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
			RequiredCapability profileRequirement = null;
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
		return new IInstallableUnit[] {createIURepresentingTheProfile(gatheredRequirements)};
	}

	private RequiredCapability createRequirement(IInstallableUnit iu, String rule) {
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

	private RequiredCapability createOptionalRequirement(IInstallableUnit iu) {
		return MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), new VersionRange(iu.getVersion(), true, iu.getVersion(), true), null, true, false, true);
	}

	private RequiredCapability createStrictRequirement(IInstallableUnit iu) {
		return MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), new VersionRange(iu.getVersion(), true, iu.getVersion(), true), null, false, false, true);
	}

	public IInstallableUnit[] updatesFor(IInstallableUnit toUpdate, ProvisioningContext context, IProgressMonitor monitor) {
		Map resultsMap = new HashMap();

		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) ServiceHelper.getService(DirectorActivator.context, IMetadataRepositoryManager.class.getName());
		URL[] repositories = context.getMetadataRepositories();
		if (repositories == null)
			repositories = repoMgr.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL);

		SubMonitor sub = SubMonitor.convert(monitor, repositories.length * 200);
		for (int i = 0; i < repositories.length; i++) {
			try {
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
}
