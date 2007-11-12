/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.CompoundIterator;
import org.eclipse.equinox.p2.resolution.ResolutionHelper;
import org.eclipse.equinox.p2.resolution.UnsatisfiedCapability;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

/**
 * Rather than trying to satisfy a particular IU, we try to satisfy all the requirements of this IU and thus recursively.
 * The core data structure here is a map of all the requirements encountered. More precisely it is a map whose key is a simplified requirement object (ns and name only), 
 * and whose value is a data structure holding:
 *	  - the intersection of all occurrences of the constraints encountered
 *   - the list of IUs satisfying this intersection
 * The algorithm is rather simple:
 *   - gather the requirements of a set of bundles and add them to the map computing the intersection
 *   - find the IUs matching each of these requirement
 *   - repeat the two previous steps until the last step no longer find matches 
 * Then the resolver is invoked and for each unsatisfied IU that comes out of the resolver we verify if it belongs to an entry in the map where at least one other IU has been picked.
 * If so we are good, otherwise we have a failure.
 * Known problems:
 *   - Malicious IU. A malicious IU could be brought into the system, thus causing a detection of impossible solution. Is this really unavoidable?
 *   - The more general problem is that we do not backtrack and therefore when we pick an IU and it may cause problems that we will not discover soon enough. Looking ahead may help us
 * in avoiding those cases but still we may have problems.
 * 
 * To be implemented:
 *   - merge of requirements with filters
 *   - check if the IU is applicable according to its platform filter with the given environment
 *   - return unsatisfied constraints
 *   - do the recommendation
 * TODO It may worth experimenting with two pickers (or chained) . One would pick on the alreadyInstalled ius and one on the available ones. This could help us favor already installed ius over the others.
 */
public class NewDependencyExpander {

	private class Match {
		/**
		 * The {@link IInstallableUnit}s satisfying this requirement.
		 */
		Set candidates;
		/**
		 * The environment against which capability filters are evaluated for this match.
		 */
		Dictionary env;
		RequiredCapability req;

		public Match(RequiredCapability range, Dictionary environment) {
			this.req = range;
			this.env = environment;
			candidates = new HashSet(2);
		}

		/**
		 * For debugging purposes only
		 */
		public String toString() {
			return "Match[" + req + ']'; //$NON-NLS-1$
		}
	}

	/**
	 * Represents a requirement name in the map of required capabilities.
	 */
	private class MatchKey {
		String name;
		String namespace;

		MatchKey(RequiredCapability capability) {
			this.namespace = capability.getNamespace();
			this.name = capability.getName();
		}

		public boolean equals(Object object) {
			if (!(object instanceof MatchKey))
				return false;
			MatchKey that = (MatchKey) object;
			return this.namespace.equals(that.namespace) && this.name.equals(that.name);
		}

		public int hashCode() {
			return 31 * namespace.hashCode() + name.hashCode();
		}

		public String toString() {
			return "MatchKey(" + namespace + '/' + name + ')'; //$NON-NLS-1$
		}
	}

	// Installable units that are optional have a dependency on themselves.
	private class OptionalInstallableUnit implements IInstallableUnit {
		private boolean optionalReqs;
		private IInstallableUnit wrapped;

		OptionalInstallableUnit(IInstallableUnit iu, boolean generateOptionalReqs) {
			wrapped = iu;
			optionalReqs = generateOptionalReqs;
		}

		public String getFilter() {
			return wrapped.getFilter();
		}

		public String getId() {
			return wrapped.getId();
		}

		public Version getVersion() {
			return wrapped.getVersion();
		}

		public String getProperty(String key) {
			return wrapped.getProperty(key);
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			final OptionalInstallableUnit other = (OptionalInstallableUnit) obj;
			if (wrapped == null) {
				if (other.wrapped != null)
					return false;
			} else if (!wrapped.equals(other.wrapped))
				return false;
			return true;
		}

		public RequiredCapability[] getRequiredCapabilities() {
			ArrayList result = new ArrayList();
			ProvidedCapability[] caps = wrapped.getProvidedCapabilities();
			for (int i = 0; i < caps.length; i++) {
				result.add(new RequiredCapability(caps[i].getNamespace(), caps[i].getName(), new VersionRange(caps[i].getVersion(), true, caps[i].getVersion(), true), wrapped.getFilter(), optionalReqs, false));
			}
			result.addAll(Arrays.asList(wrapped.getRequiredCapabilities()));
			return (RequiredCapability[]) result.toArray(new RequiredCapability[result.size()]);
		}

		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((wrapped == null) ? 0 : wrapped.hashCode());
			return result;
		}

		public String toString() {
			return wrapped.toString();
		}

		public void accept(IMetadataVisitor visitor) {
			wrapped.accept(visitor);
		}

		public String getApplicabilityFilter() {
			return wrapped.getApplicabilityFilter();
		}

		public IArtifactKey[] getArtifacts() {
			return wrapped.getArtifacts();
		}

		public Map getProperties() {
			return wrapped.getProperties();
		}

		public ProvidedCapability[] getProvidedCapabilities() {
			return wrapped.getProvidedCapabilities();
		}

		public TouchpointData[] getTouchpointData() {
			return wrapped.getTouchpointData();
		}

		public TouchpointType getTouchpointType() {
			return wrapped.getTouchpointType();
		}

		public boolean isFragment() {
			return wrapped.isFragment();
		}

		public boolean isSingleton() {
			return wrapped.isSingleton();
		}

		public int compareTo(Object arg) {
			return wrapped.compareTo(arg);
		}

		public IInstallableUnitFragment[] getFragments() {
			return null;
		}

		public boolean isResolved() {
			return false;
		}

		public IInstallableUnit unresolved() {
			return this;
		}
	}

	static final int OperationWork = 100;

	private final Set alreadyInstalled = new HashSet();

	private boolean includeOptional;

	/**
	 * A map of all the requirements ever encountered in the system. The key is
	 * a MatchKey and the value is a List of Match objects.
	 */
	private Map must = new HashMap();
	private Picker picker;

	private Profile profile;

	private RecommendationDescriptor recommendations;

	private ResolutionHelper resolver;

	private IInstallableUnit[] roots;

	private Collection solution;

	public NewDependencyExpander(IInstallableUnit[] r, IInstallableUnit[] alreadyInstalled, IInstallableUnit[] availableIUs, Profile profile, boolean includeOptional) {
		this.profile = profile;
		this.roots = (r == null) ? new IInstallableUnit[0] : r;
		this.includeOptional = includeOptional;
		alreadyInstalled = alreadyInstalled == null ? new IInstallableUnit[0] : alreadyInstalled;
		this.alreadyInstalled.addAll(Arrays.asList(alreadyInstalled));

		IInstallableUnit[] result = new IInstallableUnit[roots.length + alreadyInstalled.length + availableIUs.length];
		System.arraycopy(roots, 0, result, 0, roots.length);
		System.arraycopy(alreadyInstalled, 0, result, roots.length, alreadyInstalled.length);
		System.arraycopy(availableIUs, 0, result, roots.length + alreadyInstalled.length, availableIUs.length);
		picker = new Picker(result, new RecommendationDescriptor(new HashSet()));
		Collection filterForRoot = new ArrayList();
		if (r != null && r.length > 0) {
			filterForRoot.add(new BasicIUFilter(r));
			filterForRoot.add(new RequirementBasedFilter(r[0].getRequiredCapabilities()));
		}
		picker.prefer(filterForRoot);
	}

	/**
	 * Creates a problem status for the given unsatisfied dependency.
	 */
	private void addUnsatisfied(RequiredCapability req, Collection toAdd, MultiStatus problems) {
		for (Iterator it = toAdd.iterator(); it.hasNext();) {
			IInstallableUnit unit = (IInstallableUnit) it.next();
			RequiredCapability[] required = unit.getRequiredCapabilities();
			for (int i = 0; i < required.length; i++) {
				if (required[i].equals(req)) {
					UnsatisfiedCapability unsatisfied = new UnsatisfiedCapability(req, unit);
					String msg = NLS.bind(Messages.Director_Unsatisfied_Dependency, unsatisfied);
					problems.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, msg));
				}
			}
		}
	}

	private void algo(SubMonitor p, MultiStatus problems) {
		//First we create dependencies on the root themselves. The first iteration will mostly consist in rediscovering the roots.
		Collection toAdd = asOptionalIUs(Arrays.asList(roots), false);
		toAdd.addAll(alreadyInstalled);
		do {
			//reset work at each iteration, then use up a third. This results in an infinite series where remaining ticks gets steadily smaller
			p.setWorkRemaining(100);
			extractVisibilityData(toAdd);
			extractRequirements(toAdd);
			toAdd = collectMatches(toAdd, problems);
			p.worked(33);
		} while (toAdd.size() != 0);

		//don't bother invoking the resolver if we already have problems
		//		if (!problems.isOK())
		//			return;
		invokeResolver(problems);
		//		if (problems.isOK())
		extractSolution();
	}

	private void extractVisibilityData(Collection ius) {
		Collection filters = new ArrayList();
		for (Iterator iterator = ius.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			if ("true".equalsIgnoreCase(iu.getProperty("lineUp"))) {
				filters.add(new RequirementBasedFilter(iu.getRequiredCapabilities()));
			}
		}
		picker.prefer(filters);
	}

	private Collection asOptionalIUs(Collection asList, boolean generateOptionalReqs) {
		ArrayList result = new ArrayList();
		for (Iterator iterator = asList.iterator(); iterator.hasNext();) {
			result.add(new OptionalInstallableUnit(((IInstallableUnit) iterator.next()), generateOptionalReqs));
		}
		return result;
	}

	private Collection collectFlavorProviders(Collection toSearchFor) {
		String flavor = profile.getValue(Profile.PROP_FLAVOR);
		if (flavor == null)
			return new HashSet();
		IInstallableUnit[][] picked = picker.findInstallableUnit(null, null, new RequiredCapability[] {new RequiredCapability(IInstallableUnit.NAMESPACE_FLAVOR, flavor, VersionRange.emptyRange, null, false, false)}, true /* fragmentsOnly */);
		IInstallableUnit[] ius;
		if (picked[0].length > 0)
			ius = picked[0];
		else
			ius = picked[1];
		Set results = new HashSet(ius.length);
		for (int i = 0; i < ius.length; i++) {
			IInstallableUnit tmp = match(toSearchFor, ius[i]);
			if (tmp != null)
				results.add(new OptionalInstallableUnit(tmp, false));
		}
		return results;
	}

	private Collection collectInstallableUnitFragments(Collection ius) {
		Set picked = new HashSet();
		for (Iterator iterator = ius.iterator(); iterator.hasNext();) {
			IInstallableUnit current = (IInstallableUnit) iterator.next();
			IInstallableUnit[][] candidates = picker.findInstallableUnit(null, null, new RequiredCapability[] {new RequiredCapability("fragment", current.getId(), VersionRange.emptyRange, null, true, false)}, false /* not fragmentsOnly */); //$NON-NLS-1$
			IInstallableUnit[] matches = candidates[0].length > 0 ? candidates[0] : candidates[1];
			if (matches.length > 0) { //TODO Here we need to check the filter of the found iu
				if (matches.length == 1) {
					picked.add(matches[0]);
					continue;
				}
				//verify that each IU requires the current iu
				ProvidedCapability capForCurrent = new ProvidedCapability(IInstallableUnit.NAMESPACE_IU, current.getId(), current.getVersion());
				Map toAdd = new HashMap();
				for (int i = 0; i < matches.length; i++) {
					RequiredCapability[] reqs = matches[i].getRequiredCapabilities();
					boolean isReallyAFragment = false;
					for (int j = 0; j < reqs.length; j++) {
						isReallyAFragment = capForCurrent.isSatisfiedBy(reqs[j]);
					}
					if (!isReallyAFragment)
						continue;
					IInstallableUnit match = (IInstallableUnit) toAdd.get(matches[i].getId());
					if (match == null || match.getVersion().compareTo(matches[i].getVersion()) < 0)
						toAdd.put(matches[i].getId(), matches[i]);
				}
				picked.addAll(toAdd.values());
				//in the reminder, check if more than one is good, then pick the highest one.
			}
		}
		return picked;
	}

	// For each requirement, find the potential IUs
	private Collection collectMatches(Collection toAdd, MultiStatus problems) {
		Collection thingsAdded = new HashSet();
		for (Iterator iterator = must.values().iterator(); iterator.hasNext();) {
			List all = (List) iterator.next();
			for (Iterator matches = all.iterator(); matches.hasNext();) {
				Match current = (Match) matches.next();
				Collection[] picked = picker.findInstallableUnit(null, null, current.req);
				Collection found = picked[0].size() > 0 ? picked[0] : picked[1];
				if (current.candidates.addAll(found)) {
					thingsAdded.addAll(found);
					thingsAdded.addAll(collectOptionalInstallableUnits(found));
				}
				if (current.candidates.size() == 0 && requirementEnabled(current.req))
					addUnsatisfied(current.req, toAdd, problems);
			}
			if (all.size() > 2) {
				throw new IllegalStateException("Can't deal with three or more different versions of the same IU " + ((Match) all.get(0)).req + ". See bug 200380");
			}
			if (all.size() > 1) {
				//TODO This algorithm needs to be generalized to consider all the potential candidates.
				Set set1 = ((Match) all.get(0)).candidates;
				Set set2 = ((Match) all.get(1)).candidates;
				boolean potentialSolution = false;
				for (Iterator iteratorSet1 = set1.iterator(); iteratorSet1.hasNext() && !potentialSolution;) {
					IInstallableUnit itemSet1 = (IInstallableUnit) iteratorSet1.next();
					for (Iterator iteratorSet2 = set2.iterator(); iteratorSet2.hasNext() && !potentialSolution;) {
						IInstallableUnit itemSet2 = (IInstallableUnit) iteratorSet2.next();
						if (itemSet1.getId().equals(itemSet2.getId()) && ((itemSet1.isSingleton() == true && itemSet1.isSingleton() == itemSet2.isSingleton()) || itemSet1.isSingleton() != itemSet2.isSingleton())) {
							continue; //This combination would not work. Keep on searching
						}
						potentialSolution = true;
					}
				}
				if (potentialSolution == false) {
					String msg = "Can't find a solution where both: " + all.get(0) + " and " + all.get(1) + " would be satisfied.";
					problems.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, msg));
				}
			}

		}
		return thingsAdded;
	}

	private Collection collectOptionalInstallableUnits(Collection optionalFor) {
		if (!includeOptional)
			return new ArrayList(0);
		Collection result = collectFlavorProviders(optionalFor);
		result.addAll(collectInstallableUnitFragments(optionalFor));
		//		if (result.size() != 0) {//Find the optional pieces of the optional pieces. TODO I think there can be cases where we would cycle infinitely
		//			result.addAll(collectOptionalInstallableUnits(result));
		//		}
		return result;
	}

	/**
	 * Eliminate false positives from the set of unsatisfied capabilities returned
	 * by the resolver.  This includes optional dependencies, and dependencies for
	 * which we have an available installable unit.
	 */
	private UnsatisfiedCapability[] collectUnsatisfiedDependencies(UnsatisfiedCapability[] unresolved) {
		ArrayList reallyUnsatisfied = new ArrayList(unresolved.length);
		for (int i = 0; i < unresolved.length; i++) {
			List all = (List) must.get(new MatchKey(unresolved[i].getRequiredCapability()));
			for (Iterator iterator = all.iterator(); iterator.hasNext();) {
				Match m = (Match) iterator.next();
				if (requirementEnabled(m.req) && !oneResolved(m.candidates))
					reallyUnsatisfied.add(unresolved[i]);
			}
		}
		return (UnsatisfiedCapability[]) reallyUnsatisfied.toArray(new UnsatisfiedCapability[reallyUnsatisfied.size()]);
	}

	private List createList(Match m) {
		List result = new LinkedList();
		result.add(m);
		return result;
	}

	public IStatus expand(SubMonitor p) {
		MultiStatus result = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, Messages.Director_Unsatisfied_Dependencies, null);
		try {
			algo(SubMonitor.convert(p, "Resolving", 10), result);
		} catch (IllegalStateException e) {
			return new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, 1, e.getMessage(), null);
		}
		if (result.isOK())
			return Status.OK_STATUS;
		return result;
	}

	// return a map from a requirement to the set of installable units
	// 		  depending on that requirement
	private void extractRequirements(Collection ius) {
		//map of MatchKey->Match
		for (Iterator iterator = ius.iterator(); iterator.hasNext();) {
			IInstallableUnit currentUnit = (IInstallableUnit) iterator.next();
			RequiredCapability[] toAdd = currentUnit.getRequiredCapabilities();
			outer: for (int i = 0; i < toAdd.length; i++) {
				RequiredCapability current = toAdd[i];
				if (isApplicable(current) && !isMeta(current)) {
					MatchKey key = new MatchKey(current);
					List match = (List) must.get(key);
					if (match == null) {
						//We've never seen a requirement like this. Make a new match
						must.put(key, createList(new Match(current, profile.getSelectionContext())));
					} else {
						//look for an existing match whose version range is overlapping the new one
						for (Iterator matches = match.iterator(); matches.hasNext();) {
							Match currentMatch = (Match) matches.next();
							VersionRange newRange = intersect(currentMatch.req.getRange(), current.getRange());
							if (newRange != null) {
								//merge version range and environment with existing match
								currentMatch.req = new RequiredCapability(current.getNamespace(), current.getName(), newRange, current.getFilter(), currentMatch.req.isOptional() && current.isOptional(), false);
								currentMatch.env = mergeEnvironments(currentMatch.env, current);
								continue outer;
							}
						}
						//the new match is disjoint from existing ones, so add a new match to the list
						match.add(new Match(current, profile.getSelectionContext()));
					}
				}
			}
		}
	}

	private void extractSolution() {
		solution = Collections.unmodifiableCollection(resolver.getAllResolved());
	}

	public Collection getAllInstallableUnits() {
		if (solution == null)
			solution = new ArrayList(0);
		return solution;
	}

	public Collection getNewInstallableUnits() {
		HashSet newIUs = new HashSet(getAllInstallableUnits());
		newIUs.removeAll(alreadyInstalled);
		return newIUs;
	}

	public RecommendationDescriptor getRecommendations() {
		// TODO Auto-generated method stub
		return null;
	}

	private VersionRange intersect(VersionRange r1, VersionRange r2) {
		Version resultMin = null;
		boolean resultMinIncluded = false;
		Version resultMax = null;
		boolean resultMaxIncluded = false;

		int minCompare = r1.getMinimum().compareTo(r2.getMinimum());
		if (minCompare < 0) {
			resultMin = r2.getMinimum();
			resultMinIncluded = r2.getIncludeMinimum();
		} else if (minCompare > 0) {
			resultMin = r1.getMinimum();
			resultMinIncluded = r1.getIncludeMinimum();
		} else {//minCompare == 0
			resultMin = r1.getMinimum();
			resultMinIncluded = r1.getIncludeMinimum() && r2.getIncludeMinimum();
		}

		int maxCompare = r1.getMaximum().compareTo(r2.getMaximum());
		if (maxCompare > 0) {
			resultMax = r2.getMaximum();
			resultMaxIncluded = r2.getIncludeMaximum();
		} else if (maxCompare < 0) {
			resultMax = r1.getMaximum();
			resultMaxIncluded = r1.getIncludeMaximum();
		} else {//maxCompare == 0
			resultMax = r1.getMaximum();
			resultMaxIncluded = r1.getIncludeMaximum() && r2.getIncludeMaximum();
		}

		int resultRangeComparison = resultMin.compareTo(resultMax);
		if (resultRangeComparison < 0)
			return new VersionRange(resultMin, resultMinIncluded, resultMax, resultMaxIncluded);
		else if (resultRangeComparison == 0 && resultMinIncluded == resultMaxIncluded) {
			return new VersionRange(resultMin, resultMinIncluded, resultMax, resultMaxIncluded);
		} else
			return null;
	}

	private void invokeResolver(MultiStatus problems) {
		resolver = new ResolutionHelper(profile.getSelectionContext(), recommendations);
		Set toInstall = new HashSet(must.size());
		for (Iterator iterator = must.values().iterator(); iterator.hasNext();) {
			List allMatches = (List) iterator.next();
			for (Iterator matches = allMatches.iterator(); matches.hasNext();) {
				Match current = (Match) matches.next();
				toInstall.addAll(current.candidates);
			}
		}
		toInstall.removeAll(alreadyInstalled);
		UnsatisfiedCapability[] unsatisfied = collectUnsatisfiedDependencies(resolver.install(toInstall, alreadyInstalled));
		for (int i = 0; i < unsatisfied.length; i++) {
			problems.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Director_Unsatisfied_Dependency, unsatisfied[i])));
		}
	}

	// Check whether the requirement is applicable
	private boolean isApplicable(RequiredCapability req) {
		String filter = req.getFilter();
		if (filter == null)
			return true;
		try {
			return DirectorActivator.context.createFilter(filter).match(profile.getSelectionContext());
		} catch (InvalidSyntaxException e) {
			return false;
		}
	}

	/**
	 * Because information to discover additional things to install is mixed with information 
	 * about inter-component dependencies, we end up having dependencies that cause
	 * the whole world to be selected. We are here filtering them out.
	 */
	private boolean isMeta(RequiredCapability requiredCapability) {
		String namespace = requiredCapability.getNamespace();
		return namespace.equals(IInstallableUnit.CAPABILITY_ECLIPSE_TYPES) || namespace.equals(IInstallableUnit.NAMESPACE_IU_KIND) || namespace.equals(IInstallableUnit.NAMESPACE_FLAVOR);
	}

	private IInstallableUnit match(Collection close, IInstallableUnit picked) {
		CompoundIterator it = new CompoundIterator(new Iterator[] {close.iterator()}, null, null, picked.getRequiredCapabilities(), true);
		if (it.hasNext())
			return picked;
		return null;
	}

	private Dictionary mergeEnvironments(Dictionary context, RequiredCapability newCapability) {
		String[] newSelectors = newCapability.getSelectors();
		if (newSelectors == null || newSelectors.length == 0)
			return context;
		if (context == null)
			context = new Hashtable();
		String trueString = Boolean.TRUE.toString();
		for (int i = 0; i < newSelectors.length; i++) {
			context.put(newSelectors[i], trueString);
		}
		return context;
	}

	private boolean oneResolved(Collection ius) {
		for (Iterator iterator = ius.iterator(); iterator.hasNext();) {
			if (resolver.isResolved((IInstallableUnit) iterator.next()))
				return true;
		}
		return false;
	}

	private boolean requirementEnabled(RequiredCapability req) {
		if (req.isOptional())
			return false;
		return isApplicable(req);
	}
}
