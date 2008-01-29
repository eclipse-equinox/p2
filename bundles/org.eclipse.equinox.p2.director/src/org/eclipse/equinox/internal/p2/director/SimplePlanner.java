/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.resolution.ResolutionHelper;
import org.eclipse.equinox.internal.p2.rollback.FormerState;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.metadata.query.UpdateQuery;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;

public class SimplePlanner implements IPlanner {
	static final int ExpandWork = 12;

	private IInstallableUnit[] getInstallableUnits(Profile profile) {
		return (IInstallableUnit[]) profile.query(InstallableUnitQuery.ANY, new Collector(), null).toArray(IInstallableUnit.class);
	}

	private Profile getProfile(String profileId) {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(DirectorActivator.context, IProfileRegistry.class.getName());
		if (profileRegistry == null)
			return null;
		return profileRegistry.getProfile(profileId);
	}

	private ProvisioningPlan generateProvisioningPlan(Collection fromState, Collection toState, List fromStateOrder, List newStateOrder, ProfileChangeRequest changeRequest) {
		InstallableUnitOperand[] iuOperands = generateOperations(fromState, toState, fromStateOrder, newStateOrder);
		PropertyOperand[] propertyOperands = generatePropertyOperations(changeRequest);

		Operand[] operands = new Operand[iuOperands.length + propertyOperands.length];
		System.arraycopy(iuOperands, 0, operands, 0, iuOperands.length);
		System.arraycopy(propertyOperands, 0, operands, iuOperands.length, propertyOperands.length);

		return new ProvisioningPlan(Status.OK_STATUS, operands);
	}

	private PropertyOperand[] generatePropertyOperations(ProfileChangeRequest profileChangeRequest) {
		Profile profile = profileChangeRequest.getProfile();
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
				Object oldValue = profile.getInstallableUnitProfileProperty(iu, key);
				operands.add(new InstallableUnitPropertyOperand(iu, key, oldValue, iuPropertyChanges.get(key)));
			}
		}
		// Now deal with iu property removals.
		// TODO we could optimize by not generating property removals for IU's that aren't there or won't be there.  
		Map allIUPropertyDeletions = profileChangeRequest.getInstallableUnitProfilePropertiesToRemove();
		iter = allIUPropertyDeletions.keySet().iterator();
		while (iter.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) iter.next();
			toRemove = (String[]) allIUPropertyDeletions.get(iu);
			Map existingIUProperties = profile.getInstallableUnitProfileProperties(iu);
			for (int i = 0; i < toRemove.length; i++) {
				if (existingIUProperties.containsKey(toRemove[i]))
					operands.add(new InstallableUnitPropertyOperand(iu, toRemove[i], existingIUProperties.get(toRemove[i]), null));
			}

		}
		return (PropertyOperand[]) operands.toArray(new PropertyOperand[operands.size()]);
	}

	private InstallableUnitOperand[] generateOperations(Collection fromState, Collection toState, List fromStateOrder, List newStateOrder) {
		return sortOperations(new OperationGenerator().generateOperation(fromState, toState), newStateOrder, fromStateOrder);
	}

	private InstallableUnitOperand[] sortOperations(InstallableUnitOperand[] toSort, List installOrder, List uninstallOrder) {
		List updateOp = new ArrayList();
		for (int i = 0; i < toSort.length; i++) {
			InstallableUnitOperand op = toSort[i];
			if (op.first() == null && op.second() != null) {
				installOrder.set(installOrder.indexOf(op.second()), op);
				continue;
			}
			if (op.first() != null && op.second() == null) {
				uninstallOrder.set(uninstallOrder.indexOf(op.first()), op);
				continue;
			}
			if (op.first() != null && op.second() != null) {
				updateOp.add(op);
				continue;
			}
		}
		int i = 0;
		for (Iterator iterator = installOrder.iterator(); iterator.hasNext();) {
			Object elt = iterator.next();
			if (elt instanceof InstallableUnitOperand) {
				toSort[i++] = (InstallableUnitOperand) elt;
			}
		}
		for (Iterator iterator = uninstallOrder.iterator(); iterator.hasNext();) {
			Object elt = iterator.next();
			if (elt instanceof InstallableUnitOperand) {
				toSort[i++] = (InstallableUnitOperand) elt;
			}
		}
		for (Iterator iterator = updateOp.iterator(); iterator.hasNext();) {
			Object elt = iterator.next();
			if (elt instanceof InstallableUnitOperand) {
				toSort[i++] = (InstallableUnitOperand) elt;
			}
		}
		return toSort;
	}

	public ProvisioningPlan getRevertPlan(IInstallableUnit profileSnapshot, ProvisioningContext context, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, ExpandWork);
		sub.setTaskName(Messages.Director_Task_Resolving_Dependencies);
		try {
			MultiStatus result = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, Messages.Director_Become_Problems, null);

			if (!Boolean.valueOf(profileSnapshot.getProperty(IInstallableUnit.PROP_PROFILE_IU_KEY)).booleanValue()) {
				result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Director_Unexpected_IU, profileSnapshot.getId())));
				return new ProvisioningPlan(result);
			}
			Profile profile = getProfile(profileSnapshot.getId());
			if (profile == null) {
				result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Director_Unexpected_IU, profileSnapshot.getId())));
				return new ProvisioningPlan(result);
			}

			//TODO if the profile changes (locations are being modified, etc), should not we do a full uninstall then an install?
			//Maybe it depends on the kind of changes in a profile
			//We need to get all the ius that were part of the profile and give that to be what to become

			Dictionary snapshotSelectionContext = createSelectionContext(getSnapshotProperties(profileSnapshot));
			IInstallableUnit[] availableIUs = gatherAvailableInstallableUnits(new IInstallableUnit[] {profileSnapshot}, context.getMetadataRepositories(), sub.newChild(ExpandWork / 2));
			NewDependencyExpander toExpander = new NewDependencyExpander(new IInstallableUnit[] {profileSnapshot}, null, availableIUs, snapshotSelectionContext, true);
			toExpander.expand(sub.newChild(ExpandWork / 2));
			ResolutionHelper newStateHelper = new ResolutionHelper(snapshotSelectionContext, toExpander.getRecommendations());
			Collection newState = newStateHelper.attachCUs(toExpander.getAllInstallableUnits());
			newState.remove(profileSnapshot);

			Collection oldIUs = new HashSet();
			for (Iterator it = profile.query(InstallableUnitQuery.ANY, new Collector(), null).iterator(); it.hasNext();) {
				oldIUs.add(it.next());
			}

			Dictionary oldSelectionContext = createSelectionContext(profile.getProperties());
			ResolutionHelper oldStateHelper = new ResolutionHelper(oldSelectionContext, null);
			Collection oldState = oldStateHelper.attachCUs(oldIUs);
			ProfileChangeRequest profileChangeRequest = generateChangeRequest(profile, profileSnapshot, newState);
			return generateProvisioningPlan(oldState, newState, oldStateHelper.getSorted(), newStateHelper.getSorted(), profileChangeRequest);
		} finally {
			sub.done();
		}
	}

	private Dictionary createSelectionContext(Map properties) {
		Hashtable result = new Hashtable(properties);
		String environments = (String) properties.get(Profile.PROP_ENVIRONMENTS);
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

	private Map getSnapshotProperties(IInstallableUnit profileSnapshot) {
		Map result = new HashMap();
		for (Iterator it = profileSnapshot.getProperties().entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			String key = (String) entry.getKey();
			if (IInstallableUnit.PROP_PROFILE_IU_KEY.equals(key) || key.startsWith(FormerState.IUPROP_PREFIX))
				continue;

			result.put(key, entry.getValue());
		}
		return result;
	}

	// TODO note that this only describes property changes, not the IU changes.
	private ProfileChangeRequest generateChangeRequest(Profile currentProfile, IInstallableUnit iuDescribingNewState, Collection newIUs) {
		ProfileChangeRequest request = new ProfileChangeRequest(currentProfile);

		for (Iterator iter = currentProfile.getProperties().keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			request.removeProfileProperty(key);
		}
		IInstallableUnit[] ius = getInstallableUnits(currentProfile);
		for (int i = 0; i < ius.length; i++) {
			for (Iterator iter = currentProfile.getInstallableUnitProfileProperties(ius[i]).keySet().iterator(); iter.hasNext();) {
				String key = (String) iter.next();
				request.removeInstallableUnitProfileProperty(ius[i], key);
			}
		}
		Map profileProperties = iuDescribingNewState.getProperties();
		for (Iterator iter = profileProperties.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			// ignore the property that confirms this IU is a profile snapshot
			if (IInstallableUnit.PROP_PROFILE_IU_KEY.equals(key))
				continue;
			if (key.startsWith(FormerState.IUPROP_PREFIX)) {
				int postID = key.indexOf(FormerState.IUPROP_POSTFIX, FormerState.IUPROP_PREFIX.length());
				String id = key.substring(FormerState.IUPROP_PREFIX.length(), postID);
				for (Iterator iuIter = newIUs.iterator(); iuIter.hasNext();) {
					IInstallableUnit iu = (IInstallableUnit) iuIter.next();
					if (id.equals(iu.getId())) {
						String iuPropKey = key.substring(postID + FormerState.IUPROP_POSTFIX.length());
						request.setInstallableUnitProfileProperty(iu, iuPropKey, profileProperties.get(key));
						continue;
					}
				}
			} else {
				request.setProfileProperty(key, profileProperties.get(key));
			}
		}
		return request;
	}

	protected IInstallableUnit[] gatherAvailableInstallableUnits(IInstallableUnit[] additionalSource, URL[] repositories, IProgressMonitor monitor) {
		Map resultsMap = new HashMap();
		if (additionalSource != null) {
			for (int i = 0; i < additionalSource.length; i++) {
				String key = additionalSource[i].getId() + "_" + additionalSource[i].getVersion().toString(); //$NON-NLS-1$
				resultsMap.put(key, additionalSource[i]);
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

	private boolean hasHigherFidelity(IInstallableUnit iu, IInstallableUnit currentIU) {
		if (new Boolean(currentIU.getProperty("iu.mock")).booleanValue() && !new Boolean(iu.getProperty("iu.mock")).booleanValue()) //$NON-NLS-1$ //$NON-NLS-2$
			return true;

		return false;
	}

	public ProvisioningPlan getProvisioningPlan(ProfileChangeRequest profileChangeRequest, ProvisioningContext context, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, ExpandWork);
		sub.setTaskName(Messages.Director_Task_Resolving_Dependencies);
		try {
			MultiStatus result = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, Messages.Director_Install_Problems, null);
			//find the things being updated in the profile
			Profile profile = profileChangeRequest.getProfile();

			IInstallableUnit[] alreadyInstalled = getInstallableUnits(profile);
			IInstallableUnit[] uninstallRoots = profileChangeRequest.getRemovedInstallableUnits();

			Dictionary currentSelectionContext = createSelectionContext(profile.getProperties());
			//compute the transitive closure and remove them.
			ResolutionHelper oldStateHelper = new ResolutionHelper(currentSelectionContext, null);
			Collection oldState = oldStateHelper.attachCUs(Arrays.asList(alreadyInstalled));

			NewDependencyExpander expander = new NewDependencyExpander(uninstallRoots, new IInstallableUnit[0], alreadyInstalled, currentSelectionContext, true);
			IStatus expanderResult = expander.expand(sub.newChild(ExpandWork / 3));
			if (!expanderResult.isOK()) {
				result.merge(expanderResult);
				return new ProvisioningPlan(result);
			}
			Collection toUninstallClosure = new ResolutionHelper(currentSelectionContext, null).attachCUs(expander.getAllInstallableUnits());

			//add the new set.
			Collection remainingIUs = new HashSet(oldState);
			remainingIUs.removeAll(toUninstallClosure);
			//		for (int i = 0; i < updateRoots.length; i++) {
			//			remainingIUs.add(updateRoots[i]);
			//		}
			URL[] metadataRepositories = (context != null) ? context.getMetadataRepositories() : null;
			IInstallableUnit[] allUnits = gatherAvailableInstallableUnits(null, metadataRepositories, sub.newChild(ExpandWork / 3));
			Dictionary newSelectionContext = createSelectionContext(profileChangeRequest.getProfileProperties());
			String newFlavor = profileChangeRequest.getProfileProperty(Profile.PROP_FLAVOR);

			NewDependencyExpander finalExpander = new NewDependencyExpander(profileChangeRequest.getAddedInstallableUnits(), (IInstallableUnit[]) remainingIUs.toArray(new IInstallableUnit[remainingIUs.size()]), allUnits, newSelectionContext, true);
			IStatus finalExpanderResult = finalExpander.expand(sub.newChild(ExpandWork / 3));
			if (!finalExpanderResult.isOK()) {
				result.merge(finalExpanderResult);
				return new ProvisioningPlan(result);
			}

			ResolutionHelper newStateHelper = new ResolutionHelper(newSelectionContext, null);
			Collection newState = newStateHelper.attachCUs(finalExpander.getAllInstallableUnits());

			return generateProvisioningPlan(oldState, newState, oldStateHelper.getSorted(), newStateHelper.getSorted(), profileChangeRequest);
		} finally {
			sub.done();
		}
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
