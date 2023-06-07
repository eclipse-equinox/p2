/*******************************************************************************
 * Copyright (c) 2013, 2018 Red Hat, Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.operations;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.*;

public class RequestFlexer {
	static final String INCLUSION_RULES = "org.eclipse.equinox.p2.internal.inclusion.rules"; //$NON-NLS-1$
	static final String INCLUSION_OPTIONAL = "OPTIONAL"; //$NON-NLS-1$
	static final String INCLUSION_STRICT = "STRICT"; //$NON-NLS-1$
	static final String EXPLANATION_ENABLEMENT = "org.eclipse.equinox.p2.director.explain"; //$NON-NLS-1$

	IPlanner planner;

	private boolean allowInstalledUpdate = false;
	private boolean allowInstalledRemoval = false;
	private boolean allowDifferentVersion = false;
	private boolean allowPartialInstall = false;

	private boolean ensureProductPresence = true;
	private boolean honorSharedSettings = true;

	private ProvisioningContext provisioningContext;

	Set<IRequirement> requirementsForElementsBeingInstalled = new HashSet<>();
	Set<IRequirement> requirementsForElementsAlreadyInstalled = new HashSet<>();
	Map<IRequirement, Map<String, String>> propertiesPerRequirement = new HashMap<>();
	Map<IRequirement, List<String>> removedPropertiesPerRequirement = new HashMap<>();

	IProfile profile;

	private boolean foundDifferentVersionsForElementsToInstall = false;
	private boolean foundDifferentVersionsForElementsInstalled = false;
	private Set<IInstallableUnit> futureOptionalIUs;

	public RequestFlexer(IPlanner planner) {
		this.planner = planner;
	}

	public void setAllowInstalledElementChange(boolean allow) {
		allowInstalledUpdate = allow;
	}

	public void setAllowInstalledElementRemoval(boolean allow) {
		allowInstalledRemoval = allow;
	}

	public void setAllowDifferentVersion(boolean allow) {
		allowDifferentVersion = allow;
	}

	public void setAllowPartialInstall(boolean allow) {
		allowPartialInstall = allow;
	}

	public void setProvisioningContext(ProvisioningContext context) {
		provisioningContext = context;
	}

	public void setEnsureProduct(boolean productPresent) {
		ensureProductPresence = productPresent;
	}

	public IProfileChangeRequest getChangeRequest(IProfileChangeRequest request, IProfile prof, IProgressMonitor monitor) {
		this.profile = prof;
		SubMonitor sub = SubMonitor.convert(monitor, 2);
		IProfileChangeRequest loosenedRequest = computeLooseRequest(request, sub.newChild(1));
		if (canShortCircuit(request)) {
			return null;
		}
		IProvisioningPlan intermediaryPlan = resolve(loosenedRequest, sub.newChild(1));
		if (!intermediaryPlan.getStatus().isOK())
			return null;
		if (intermediaryPlan.getAdditions().query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).isEmpty() && intermediaryPlan.getRemovals().query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).isEmpty())
			//No changes, we can't return anything
			return null;
		if (!productContainmentOK(intermediaryPlan)) {
			return null;
		}
		IProfileChangeRequest effectiveRequest = computeEffectiveChangeRequest(intermediaryPlan, loosenedRequest, request);
		if (isRequestUseless(effectiveRequest))
			return null;
		return effectiveRequest;
	}

	private boolean isRequestUseless(IProfileChangeRequest effectiveRequest) {
		if (effectiveRequest.getAdditions().isEmpty() && effectiveRequest.getRemovals().isEmpty())
			return true;
		if (effectiveRequest.getRemovals().containsAll(effectiveRequest.getAdditions()))
			return true;
		return false;
	}

	private boolean canShortCircuit(IProfileChangeRequest originalRequest) {
		//Case where the user is asking to install only some of the requested IUs but there is only one IU to install. 
		if (allowPartialInstall && !allowInstalledUpdate && !allowDifferentVersion && !allowInstalledRemoval)
			if (originalRequest.getAdditions().size() == 1)
				return true;

		//When we can find a different version of the IU but the only version available is the one the user is asking to install
		if (allowDifferentVersion && !allowPartialInstall && !allowInstalledRemoval && !allowInstalledUpdate)
			if (!foundDifferentVersionsForElementsToInstall)
				return true;

		if (allowInstalledUpdate && !allowDifferentVersion && !allowPartialInstall && !allowInstalledRemoval)
			if (!foundDifferentVersionsForElementsInstalled)
				return true;

		if (!allowPartialInstall && !allowInstalledUpdate && !allowDifferentVersion && !allowInstalledRemoval)
			return true;

		return false;
	}

	//From the loosened request and the plan resulting from its resolution, create a new profile change request representing the delta between where the profile currently is 
	// and the plan returned.
	//To perform this efficiently, this relies on a traversal of the requirements that are part of the loosened request.  
	private IProfileChangeRequest computeEffectiveChangeRequest(IProvisioningPlan intermediaryPlan, IProfileChangeRequest loosenedRequest, IProfileChangeRequest originalRequest) {
		IProfileChangeRequest finalChangeRequest = planner.createChangeRequest(profile);
		// We have the following two variables because a IPCRequest can not be muted
		Set<IInstallableUnit> iusToAdd = new HashSet<>();
		Set<IInstallableUnit> iusToRemove = new HashSet<>();

		for (IRequirement beingInstalled : requirementsForElementsBeingInstalled) {
			IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery(beingInstalled.getMatches());
			IQueryResult<IInstallableUnit> matches = intermediaryPlan.getFutureState().query(QueryUtil.createLatestQuery(query), null);
			IInstallableUnit replacementIU = null;
			if (!matches.isEmpty()) {
				replacementIU = matches.iterator().next();
				iusToAdd.add(replacementIU);
				adaptIUPropertiesToNewIU(beingInstalled, replacementIU, finalChangeRequest);
			}
		}

		for (IRequirement alreadyInstalled : requirementsForElementsAlreadyInstalled) {
			IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery(alreadyInstalled.getMatches());
			IQueryResult<IInstallableUnit> matches = intermediaryPlan.getFutureState().query(QueryUtil.createLatestQuery(query), null);
			IInstallableUnit potentialRootChange = null;
			if (!matches.isEmpty())
				potentialRootChange = matches.iterator().next();

			IQueryResult<IInstallableUnit> iuAlreadyInstalled = profile.available(query, new NullProgressMonitor());

			if (!iuAlreadyInstalled.isEmpty()) {//This deals with the case where the root has not changed
				if (potentialRootChange != null && iuAlreadyInstalled.toUnmodifiableSet().contains(potentialRootChange))
					continue;
			}
			iusToRemove.addAll(iuAlreadyInstalled.toUnmodifiableSet());
			if (potentialRootChange != null) {
				if (!iusToAdd.contains(potentialRootChange)) {//So we don't add the same IU twice for addition
					iusToAdd.add(potentialRootChange);
					adaptIUPropertiesToNewIU(alreadyInstalled, potentialRootChange, finalChangeRequest);
				}
			}
		}

		iusToRemove.addAll(originalRequest.getRemovals());

		//Remove entries that are both in the additions and removals (since this is a no-op)
		HashSet<IInstallableUnit> commons = new HashSet<>(iusToAdd);
		if (commons.retainAll(iusToRemove)) {
			iusToAdd.removeAll(commons);
			iusToRemove.removeAll(commons);
		}

		//Finish construction of the IPCR
		finalChangeRequest.addAll(iusToAdd);
		finalChangeRequest.removeAll(iusToRemove);
		if (originalRequest.getExtraRequirements() != null)
			finalChangeRequest.addExtraRequirements(originalRequest.getExtraRequirements());
		return finalChangeRequest;
	}

	private void adaptIUPropertiesToNewIU(IRequirement beingInstalled, IInstallableUnit newIU, IProfileChangeRequest finalChangeRequest) {
		Map<String, String> associatedProperties = propertiesPerRequirement.get(beingInstalled);
		if (associatedProperties != null) {
			Set<Entry<String, String>> entries = associatedProperties.entrySet();
			for (Entry<String, String> entry : entries) {
				finalChangeRequest.setInstallableUnitProfileProperty(newIU, entry.getKey(), entry.getValue());
			}
		}
		List<String> removedProperties = removedPropertiesPerRequirement.get(beingInstalled);
		if (removedProperties != null) {
			for (String toRemove : removedProperties) {
				finalChangeRequest.removeInstallableUnitProfileProperty(newIU, toRemove);
			}
		}
	}

	//Create a request where the original requirements are "loosened" according to flags specified in this instance
	//The resulting profile change request uses the requirements specified using p2QL and those appear in the extraRequirements.
	private IProfileChangeRequest computeLooseRequest(IProfileChangeRequest originalRequest, IProgressMonitor monitor) {
		IProfileChangeRequest loosenedRequest = planner.createChangeRequest(profile);
		SubMonitor sub = SubMonitor.convert(monitor, 2);
		loosenUpOriginalRequest(loosenedRequest, originalRequest, sub.newChild(1));
		loosenUpInstalledSoftware(loosenedRequest, originalRequest, sub.newChild(1));
		return loosenedRequest;
	}

	private boolean removalRequested(IInstallableUnit removalRequested, IProfileChangeRequest request) {
		return request.getRemovals().contains(removalRequested);
	}

	private IProvisioningPlan resolve(IProfileChangeRequest temporaryRequest, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
		String explainPropertyBackup = null;
		try {
			temporaryRequest.setProfileProperty("_internal_user_defined_", "true"); //$NON-NLS-1$//$NON-NLS-2$
			if (provisioningContext != null) {
				explainPropertyBackup = provisioningContext.getProperty(EXPLANATION_ENABLEMENT);
				provisioningContext.setProperty(EXPLANATION_ENABLEMENT, Boolean.FALSE.toString());
			}
			return planner.getProvisioningPlan(temporaryRequest, provisioningContext, subMonitor.split(1));
		} finally {
			if (provisioningContext != null) {
				if (explainPropertyBackup == null)
					provisioningContext.getProperties().remove(EXPLANATION_ENABLEMENT);
				else
					provisioningContext.setProperty(EXPLANATION_ENABLEMENT, explainPropertyBackup);
			}
		}
	}

	//Loosen the request originally emitted.
	//For example if the user said "install A 1.0", then a new Requirement is added saying (install A 1.0 or install A 2.0), this depending on the configuration flags 
	private void loosenUpOriginalRequest(IProfileChangeRequest newRequest, IProfileChangeRequest originalRequest, IProgressMonitor monitor) {
		//First deal with the IUs that are being added
		Collection<IInstallableUnit> requestedAdditions = originalRequest.getAdditions();
		SubMonitor subMonitor = SubMonitor.convert(monitor, requestedAdditions.size());
		for (IInstallableUnit addedIU : requestedAdditions) {
			SubMonitor iterationMonitor = subMonitor.split(1);
			Collection<IInstallableUnit> potentialUpdates = allowDifferentVersion ? findAllVersionsAvailable(addedIU, iterationMonitor) : new ArrayList<>();
			foundDifferentVersionsForElementsToInstall = (foundDifferentVersionsForElementsToInstall || (potentialUpdates.size() == 0 ? false : true));
			potentialUpdates.add(addedIU); //Make sure that we include the IU that we were initially trying to install

			Collection<IRequirement> newRequirement = new ArrayList<>(1);
			IRequirement req = createORRequirement(potentialUpdates, allowPartialInstall || isRequestedInstallationOptional(addedIU, originalRequest));
			newRequirement.add(req);
			newRequest.addExtraRequirements(newRequirement);
			requirementsForElementsBeingInstalled.addAll(newRequirement);
			rememberIUProfileProperties(addedIU, req, originalRequest, false);
		}

		//Deal with the IUs requested for removal
		newRequest.removeAll(originalRequest.getRemovals());

		//Deal with extra requirements that could have been specified
		if (originalRequest.getExtraRequirements() != null)
			newRequest.addExtraRequirements(originalRequest.getExtraRequirements());
	}

	//This keeps track for each requirement created (those created to loosen the constraint), of the original IU and the properties associated with it in the profile
	//This is used for more easily construct the final profile change request
	private void rememberIUProfileProperties(IInstallableUnit iu, IRequirement req, IProfileChangeRequest originalRequest, boolean includeProfile) {
		Map<String, String> allProperties = new HashMap<>();
		if (includeProfile) {
			Map<String, String> tmp = new HashMap<>(profile.getInstallableUnitProperties(iu));
			List<String> propertiesToRemove = ((ProfileChangeRequest) originalRequest).getInstallableUnitProfilePropertiesToRemove().get(iu);
			if (propertiesToRemove != null) {
				for (String toRemove : propertiesToRemove) {
					tmp.remove(toRemove);
				}
			}
			allProperties.putAll(tmp);
		}

		Map<String, String> propertiesInRequest = ((ProfileChangeRequest) originalRequest).getInstallableUnitProfilePropertiesToAdd().get(iu);
		if (propertiesInRequest != null)
			allProperties.putAll(propertiesInRequest);

		propertiesPerRequirement.put(req, allProperties);

		List<String> removalInRequest = ((ProfileChangeRequest) originalRequest).getInstallableUnitProfilePropertiesToRemove().get(iu);
		if (removalInRequest != null)
			removedPropertiesPerRequirement.put(req, removalInRequest);
	}

	private boolean isRequestedInstallationOptional(IInstallableUnit iu, IProfileChangeRequest originalRequest) {
		Map<String, String> match = ((ProfileChangeRequest) originalRequest).getInstallableUnitProfilePropertiesToAdd().get(iu);
		if (match == null)
			return false;
		return INCLUSION_OPTIONAL.equals(match.get(INCLUSION_RULES));
	}

	private Collection<IInstallableUnit> findAllVersionsAvailable(IInstallableUnit iu, IProgressMonitor monitor) {
		Collection<IInstallableUnit> allVersions = new HashSet<>();
		SubMonitor sub = SubMonitor.convert(monitor, 2);
		allVersions.addAll(findIUsWithSameId(iu, sub.newChild(1)));
		allVersions.addAll(findUpdates(iu, sub.newChild(1)));
		return allVersions;
	}

	private Collection<IInstallableUnit> findIUsWithSameId(IInstallableUnit iu, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, 2);
		IQueryable<IInstallableUnit> metadata = provisioningContext.getMetadata(sub.newChild(1));
		return metadata.query(QueryUtil.createIUQuery(iu.getId()), sub.newChild(1)).toUnmodifiableSet();
	}

	private Collection<IInstallableUnit> findUpdates(IInstallableUnit iu, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
		Collection<IInstallableUnit> availableUpdates = new HashSet<>();
		IQueryResult<IInstallableUnit> updatesAvailable = planner.updatesFor(iu, provisioningContext, subMonitor.split(1));
		for (IInstallableUnit unit : updatesAvailable) {
			availableUpdates.add(unit);
		}
		return availableUpdates;
	}

	//Create an OR expression that is matching all the entries from the given collection
	private IRequirement createORRequirement(Collection<IInstallableUnit> findUpdates, boolean optional) {
		StringBuilder expression = new StringBuilder();
		Object[] expressionParameters = new Object[findUpdates.size() * 2];
		int count = 0;
		for (IInstallableUnit iu : findUpdates) {
			expression.append("(id == $").append(count * 2).append(" && version == $").append(count * 2 + 1).append(')'); //$NON-NLS-1$//$NON-NLS-2$
			if (findUpdates.size() > 1 && count < findUpdates.size() - 1)
				expression.append(" || "); //$NON-NLS-1$
			expressionParameters[count * 2] = iu.getId();
			expressionParameters[count * 2 + 1] = iu.getVersion();
			count++;
		}
		IMatchExpression<IInstallableUnit> iuMatcher = ExpressionUtil.getFactory().matchExpression(ExpressionUtil.parse(expression.toString()), expressionParameters);
		return MetadataFactory.createRequirement(iuMatcher, null, optional ? 0 : 1, 1, true);
	}

	//Loosen up the IUs that are already part of the profile
	//Given how we are creating our request, this needs to take into account the removal from the original request as well as the change in inclusion 
	private IProfileChangeRequest loosenUpInstalledSoftware(IProfileChangeRequest request, IProfileChangeRequest originalRequest, IProgressMonitor monitor) {
		if (!allowInstalledRemoval && !allowInstalledUpdate)
			return request;
		Set<IInstallableUnit> allRoots = getRoots();

		for (IInstallableUnit existingIU : allRoots) {
			Collection<IInstallableUnit> potentialUpdates = allowInstalledUpdate ? findUpdates(existingIU, monitor) : new HashSet<>();
			foundDifferentVersionsForElementsInstalled = (foundDifferentVersionsForElementsInstalled || (potentialUpdates.size() == 0 ? false : true));
			potentialUpdates.add(existingIU);
			Collection<IRequirement> newRequirement = new ArrayList<>(1);
			//when the element is requested for removal or is installed optionally we make sure to mark it optional, otherwise the removal woudl fail
			IRequirement req = createORRequirement(potentialUpdates, allowInstalledRemoval || removalRequested(existingIU, originalRequest) || isOptionallyInstalled(existingIU, originalRequest));
			newRequirement.add(req);
			request.addExtraRequirements(newRequirement);
			requirementsForElementsAlreadyInstalled.addAll(newRequirement);
			request.remove(existingIU);
			rememberIUProfileProperties(existingIU, req, originalRequest, true);
		}

		return request;
	}

	private Set<IInstallableUnit> getRoots() {
		Set<IInstallableUnit> allRoots = profile.query(new IUProfilePropertyQuery(INCLUSION_RULES, IUProfilePropertyQuery.ANY), null).toSet();
		if (!honorSharedSettings)
			return allRoots;
		IQueryResult<IInstallableUnit> baseRoots = profile.query(new IUProfilePropertyQuery("org.eclipse.equinox.p2.base", Boolean.TRUE.toString()), null);
		allRoots.removeAll(baseRoots.toUnmodifiableSet());
		return allRoots;
	}

	//This return whether or not the given IU is installed optionally or not.
	//This also take into account the future state
	private boolean isOptionallyInstalled(IInstallableUnit existingIU, IProfileChangeRequest request) {
		return computeFutureStateOfInclusion((ProfileChangeRequest) request).contains(existingIU);
	}

	//Given the change request, this returns the collection of optional IUs 
	private Set<IInstallableUnit> computeFutureStateOfInclusion(ProfileChangeRequest profileChangeRequest) {
		if (futureOptionalIUs != null)
			return futureOptionalIUs;

		futureOptionalIUs = profileChangeRequest.getProfile().query(new IUProfilePropertyQuery(INCLUSION_RULES, INCLUSION_OPTIONAL), null).toSet();

		Set<Entry<IInstallableUnit, List<String>>> propertiesBeingRemoved = profileChangeRequest.getInstallableUnitProfilePropertiesToRemove().entrySet();
		for (Entry<IInstallableUnit, List<String>> propertyRemoved : propertiesBeingRemoved) {
			if (propertyRemoved.getValue().contains(INCLUSION_RULES)) {
				futureOptionalIUs.remove(propertyRemoved.getKey());
			}
		}

		Set<Entry<IInstallableUnit, Map<String, String>>> propertiesBeingAdded = profileChangeRequest.getInstallableUnitProfilePropertiesToAdd().entrySet();
		for (Entry<IInstallableUnit, Map<String, String>> propertyBeingAdded : propertiesBeingAdded) {
			String inclusionRule = propertyBeingAdded.getValue().get(INCLUSION_RULES);
			if (inclusionRule == null) {
				continue;
			}
			if (INCLUSION_STRICT.equals(inclusionRule)) {
				futureOptionalIUs.remove(propertyBeingAdded.getKey());
			}
			if (INCLUSION_OPTIONAL.equals(inclusionRule)) {
				futureOptionalIUs.add(propertyBeingAdded.getKey());
			}
		}
		return futureOptionalIUs;
	}

	private boolean productContainmentOK(IProvisioningPlan intermediaryPlan) {
		if (!ensureProductPresence)
			return true;
		if (!hasProduct())
			return true;
		//At this point we know we had a product installed and we want to make sure there is one in the resulting solution
		if (!intermediaryPlan.getFutureState().query(QueryUtil.createIUProductQuery(), new NullProgressMonitor()).isEmpty())
			return true;
		//Support for legacy identification of product using the lineUp.
		if (!intermediaryPlan.getFutureState().query(QueryUtil.createIUPropertyQuery("lineUp", "true"), new NullProgressMonitor()).isEmpty()) //$NON-NLS-1$//$NON-NLS-2$
			return true;
		return false;
	}

	private boolean hasProduct() {
		if (!profile.available(QueryUtil.createIUProductQuery(), new NullProgressMonitor()).isEmpty()) {
			return true;
		}
		//Support for legacy identification of product using the lineUp.
		if (!profile.available(QueryUtil.createIUPropertyQuery("lineUp", "true"), new NullProgressMonitor()).isEmpty()) //$NON-NLS-1$//$NON-NLS-2$
			return true;
		return false;
	}

}
