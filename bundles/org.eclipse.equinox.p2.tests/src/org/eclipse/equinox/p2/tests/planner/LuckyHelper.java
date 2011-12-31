/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * Sonatype, Inc. - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.metadata.query.UpdateQuery;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.*;
import org.eclipse.equinox.p2.query.*;

public class LuckyHelper {
	private static final String INCLUSION_RULES = "org.eclipse.equinox.p2.internal.inclusion.rules"; //$NON-NLS-1$
	private static final String INCLUSION_OPTIONAL = "OPTIONAL"; //$NON-NLS-1$
	private static final String INCLUSION_STRICT = "STRICT"; //$NON-NLS-1$

	private IProfile profile;
	private IPlanner planner;
	private IProfileChangeRequest originalRequest;
	private ProvisioningContext provisioningContext;

	private Set<IInstallableUnit> strictRoots;
	private Set<IInstallableUnit> optionalRoots;

	private IProfileChangeRequest updateFinderRequest;

	IProfileChangeRequest computeProfileChangeRequest(IProfile profile, IPlanner plannor, IProfileChangeRequest request, ProvisioningContext context, IProgressMonitor monitor) {
		//		IProfileRegistry profileRegistry = (IProfileRegistry) session.getProvisioningAgent().getService(IProfileRegistry.SERVICE_NAME);
		this.planner = plannor; //(IPlanner) session.getProvisioningAgent().getService(IPlanner.SERVICE_NAME);
		this.profile = profile; //profileRegistry.getProfile(getProfileId());
		this.provisioningContext = context;
		originalRequest = request;

		gatherRoots();
		CollectionResult<IInstallableUnit> allInitialRoots = buildUpdateFinderRequest();

		IProvisioningPlan updateFinderPlan = planner.getProvisioningPlan(updateFinderRequest, provisioningContext, null);
		if (updateFinderPlan.getAdditions().query(QueryUtil.ALL_UNITS, null).isEmpty()) {
			return null;
		}

		return buildFinalRequest(allInitialRoots, updateFinderPlan);
	}

	//Gather the initial roots and merge them with those from the original request
	private void gatherRoots() {
		strictRoots = profile.query(new IUProfilePropertyQuery(INCLUSION_RULES, INCLUSION_STRICT), null).toSet();
		optionalRoots = profile.query(new IUProfilePropertyQuery(INCLUSION_RULES, INCLUSION_OPTIONAL), null).toSet();

		//Take into account the changes from the request (potential addition or removals)
		if (originalRequest != null) {
			strictRoots.removeAll(originalRequest.getRemovals());
			optionalRoots.removeAll(originalRequest.getRemovals());
			Collection<IInstallableUnit> added = originalRequest.getAdditions();
			for (IInstallableUnit iuAdded : added) {
				Map props = ((ProfileChangeRequest) originalRequest).getInstallableUnitProfilePropertiesToAdd();
				Map propertyForIU = (Map) props.get(iuAdded);
				if (propertyForIU == null || INCLUSION_STRICT.equals(propertyForIU.get(INCLUSION_RULES))) {
					strictRoots.add(iuAdded);
				} else if (INCLUSION_OPTIONAL.equals(propertyForIU.get(INCLUSION_RULES))) {
					optionalRoots.add(iuAdded);
				}
			}
		}
	}

	//Produce a profile change request which is used to find the best update
	private CollectionResult<IInstallableUnit> buildUpdateFinderRequest() {
		Set<IInstallableUnit> tmpRoots = new HashSet<IInstallableUnit>(strictRoots);
		tmpRoots.addAll(optionalRoots);

		CollectionResult<IInstallableUnit> allInitialRoots = new CollectionResult<IInstallableUnit>(tmpRoots);

		updateFinderRequest = planner.createChangeRequest(profile);
		Collection<IRequirement> limitingRequirements = new ArrayList<IRequirement>();
		if (originalRequest != null && originalRequest.getExtraRequirements() != null)
			limitingRequirements.addAll(originalRequest.getExtraRequirements());

		//Create a profile change request that attempts at installing updates for all the existing roots.
		for (Iterator<IInstallableUnit> iterator = allInitialRoots.query(QueryUtil.ALL_UNITS, null).iterator(); iterator.hasNext();) {
			IInstallableUnit currentlyInstalled = iterator.next();

			//find all the potential updates for the currentlyInstalled iu
			IQueryResult<IInstallableUnit> updatesAvailable = planner.updatesFor(currentlyInstalled, provisioningContext, null);
			for (Iterator<IInstallableUnit> iterator2 = updatesAvailable.iterator(); iterator2.hasNext();) {
				IInstallableUnit update = iterator2.next();
				updateFinderRequest.add(update);
				updateFinderRequest.setInstallableUnitInclusionRules(update, ProfileInclusionRules.createOptionalInclusionRule(update));
			}

			//force the original IU to optional, but make sure that the solution at least includes it
			updateFinderRequest.setInstallableUnitInclusionRules(currentlyInstalled, ProfileInclusionRules.createOptionalInclusionRule(currentlyInstalled));
			limitingRequirements.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, currentlyInstalled.getId(), new VersionRange(currentlyInstalled.getVersion(), true, Version.MAX_VERSION, true), null, false, false));
		}
		updateFinderRequest.addExtraRequirements(limitingRequirements);
		return allInitialRoots;
	}

	//Analyze the plan produced by the execution of the updateFinderRequest, and produce a plan representing the recommended changes 
	private IProfileChangeRequest buildFinalRequest(CollectionResult<IInstallableUnit> allInitialRoots, IProvisioningPlan updateFinderPlan) {
		//Take into account all the removals
		IProfileChangeRequest finalChangeRequest = planner.createChangeRequest(profile);
		if (originalRequest != null && originalRequest.getExtraRequirements() != null)
			finalChangeRequest.addExtraRequirements(originalRequest.getExtraRequirements());
		IQueryResult<IInstallableUnit> removals = updateFinderPlan.getRemovals().query(QueryUtil.ALL_UNITS, null);
		for (Iterator<IInstallableUnit> iterator = removals.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = iterator.next();
			if (!allInitialRoots.query(QueryUtil.createIUQuery(iu), null).isEmpty()) {
				finalChangeRequest.remove(iu);
			}
		}

		//Take into account the additions for stricts
		for (Iterator<IInstallableUnit> iterator = strictRoots.iterator(); iterator.hasNext();) {
			IInstallableUnit formerRoot = iterator.next();
			IQueryResult<IInstallableUnit> update = updateFinderPlan.getAdditions().query(new UpdateQuery(formerRoot), null);
			if (!update.isEmpty())
				finalChangeRequest.addAll(update.toUnmodifiableSet());
			else if (originalRequest != null && originalRequest.getAdditions().contains(formerRoot)) //deal with the case of the elements added by the request
				finalChangeRequest.add(formerRoot);
		}

		//Take into account the additions for optionals
		for (Iterator<IInstallableUnit> iterator = optionalRoots.iterator(); iterator.hasNext();) {
			IInstallableUnit formerRoot = iterator.next();
			IQueryResult<IInstallableUnit> update = updateFinderPlan.getAdditions().query(new UpdateQuery(formerRoot), null);
			if (!update.isEmpty()) {
				for (Iterator<IInstallableUnit> it = update.iterator(); it.hasNext();) {
					IInstallableUnit updatedOptionalIU = it.next();
					finalChangeRequest.add(updatedOptionalIU);
					finalChangeRequest.setInstallableUnitInclusionRules(updatedOptionalIU, ProfileInclusionRules.createOptionalInclusionRule(updatedOptionalIU));
				}
			} else if (originalRequest != null && originalRequest.getAdditions().contains(formerRoot)) {
				finalChangeRequest.add(formerRoot);
				finalChangeRequest.setInstallableUnitInclusionRules(formerRoot, ProfileInclusionRules.createOptionalInclusionRule(formerRoot));
			}
		}
		return finalChangeRequest;
	}

}
