/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 * 	Daniel Le Berre - Fix in the encoding and the optimization function
 * Alban Browaeys - Optimized string concatenation in bug 251357
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.CapabilityQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.InvalidSyntaxException;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.tools.DependencyHelper;
import org.sat4j.pb.tools.WeightedObject;
import org.sat4j.specs.*;

/**
 * This class is the interface between SAT4J and the planner. It produces a
 * boolean satisfiability problem, invokes the solver, and converts the solver result
 * back into information understandable by the planner.
 * 
 * This class is based on the version from the 3.5.x development stream and 
 * included as part of the fix for bug 303936.
 */
public class Projector2 implements IProjector {
	private static boolean DEBUG = Tracing.DEBUG_PLANNER_PROJECTOR;
	private static boolean DEBUG_ENCODING = false;
	private IQueryable picker;
	private QueryableArray patches;

	private Map variables; //key IU, value IUVariable
	private Map noopVariables; //key IU, value AbstractVariable
	private List abstractVariables;

	private TwoTierMap slice; //The IUs that have been considered to be part of the problem

	private Dictionary selectionContext;

	private DependencyHelper dependencyHelper;
	private Collection solution;
	private Collection assumptions;

	private MultiStatus result;
	private Map fragments;

	static abstract class PropositionalVariable {

		abstract void handleMatches(List matches);
	}

	class AbstractVariable extends PropositionalVariable {

		public String toString() {
			return "AbstractVariable: " + hashCode(); //$NON-NLS-1$
		}

		void handleMatches(List matches) {
			// do nothing
		}
	}

	class Fragment extends PropositionalVariable {
		private final IInstallableUnit iu;
		private final List matches = new ArrayList();

		public Fragment(IInstallableUnit iu) {
			this.iu = iu;
		}

		public String toString() {
			return "Fragment" + iu + " -> " + matches; //$NON-NLS-1$ //$NON-NLS-2$
		}

		void handleMatches(List matches) {
			this.matches.addAll(matches);
		}

		List getMatches() {
			return matches;
		}
	}

	class IUVariable extends PropositionalVariable {

		private final IInstallableUnit iu;

		public IUVariable(IInstallableUnit iu) {
			this.iu = iu;
		}

		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (obj == this)
				return true;
			if (obj instanceof IUVariable) {
				IUVariable other = (IUVariable) obj;
				return other.iu.getId().equals(iu.getId()) && other.iu.getVersion().equals(iu.getVersion());
			}
			return false;
		}

		public int hashCode() {
			return iu.hashCode();
		}

		public String toString() {
			return iu.getId() + '_' + iu.getVersion();
		}

		public IInstallableUnit getInstallableUnit() {
			return iu;
		}

		void handleMatches(List matches) {
			// do nothing
		}
	}

	private IUVariable newIUVariable(IInstallableUnit iu) {
		IUVariable var = (IUVariable) variables.get(iu);
		if (var == null) {
			var = new IUVariable(iu);
			variables.put(iu, var);
		}
		return var;
	}

	private Fragment newFragmentVariable(IInstallableUnit iu) {
		Fragment var = (Fragment) fragments.get(iu);
		if (var == null) {
			var = new Fragment(iu);
			fragments.put(iu, var);
		}
		return var;
	}

	private boolean isFragment(IInstallableUnit iu) {
		return false;
	}

	public Projector2(IQueryable q, Dictionary context) {
		picker = q;
		variables = new HashMap();
		noopVariables = new HashMap();
		slice = new TwoTierMap();
		selectionContext = context;
		abstractVariables = new ArrayList();
		result = new MultiStatus(DirectorActivator.PI_DIRECTOR, IStatus.OK, Messages.Planner_Problems_resolving_plan, null);
		assumptions = new ArrayList();
	}

	public void encode(IInstallableUnit[] ius, IProgressMonitor monitor) {
		try {
			long start = 0;
			if (DEBUG) {
				start = System.currentTimeMillis();
				Tracing.debug("Start projection: " + start); //$NON-NLS-1$
			}
			IPBSolver solver;
			if (DEBUG_ENCODING) {
				solver = SolverFactory.newOPBStringSolver();
			} else {
				solver = SolverFactory.newEclipseP2();
			}
			solver.setTimeoutOnConflicts(3000);
			dependencyHelper = new DependencyHelper(solver);

			Iterator iusToEncode = picker.query(InstallableUnitQuery.ANY, new Collector(), null).iterator();

			List iusToOrder = new ArrayList();
			while (iusToEncode.hasNext()) {
				iusToOrder.add(iusToEncode.next());
			}
			Collections.sort(iusToOrder);
			iusToEncode = iusToOrder.iterator();

			while (iusToEncode.hasNext()) {
				if (monitor.isCanceled()) {
					result.merge(Status.CANCEL_STATUS);
					throw new OperationCanceledException();
				}
				processIU((IInstallableUnit) iusToEncode.next());
			}
			createConstraintsForSingleton();
			for (int i = 0; i < ius.length; i++) {
				IInstallableUnit iu = ius[i];
				createMustHave(iu);
			}
			createOptimizationFunction(ius);
			if (DEBUG) {
				long stop = System.currentTimeMillis();
				Tracing.debug("Projection complete: " + (stop - start)); //$NON-NLS-1$
			}
			if (DEBUG_ENCODING) {
				System.out.println(solver.toString());
			}
		} catch (IllegalStateException e) {
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, e.getMessage(), e));
		} catch (ContradictionException e) {
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, e.getMessage(), e));
		}
	}

	//Create an optimization function favoring the highest version of each IU
	private void createOptimizationFunction(IInstallableUnit[] ius) {

		List weightedObjects = new ArrayList();

		Set s = slice.entrySet();
		final BigInteger POWER = BigInteger.valueOf(2);

		BigInteger maxWeight = POWER;
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			HashMap conflictingEntries = (HashMap) entry.getValue();
			if (conflictingEntries.size() == 1) {
				continue;
			}
			List toSort = new ArrayList(conflictingEntries.values());
			Collections.sort(toSort, Collections.reverseOrder());
			BigInteger weight = BigInteger.ONE;
			int count = toSort.size();
			for (int i = 0; i < count; i++) {
				weightedObjects.add(WeightedObject.newWO(newIUVariable(((IInstallableUnit) toSort.get(i))), weight));
				weight = weight.multiply(POWER);
			}
			if (weight.compareTo(maxWeight) > 0)
				maxWeight = weight;
		}

		maxWeight = maxWeight.multiply(POWER);

		// Weight the no-op variables beneath the abstract variables
		for (Iterator iterator = noopVariables.values().iterator(); iterator.hasNext();) {
			weightedObjects.add(WeightedObject.newWO(iterator.next(), maxWeight));
		}

		maxWeight = maxWeight.multiply(POWER);

		// Add the abstract variables
		BigInteger abstractWeight = maxWeight.negate();
		for (Iterator iterator = abstractVariables.iterator(); iterator.hasNext();) {
			weightedObjects.add(WeightedObject.newWO(iterator.next(), abstractWeight));
		}

		maxWeight = maxWeight.multiply(POWER);

		BigInteger optionalWeight = maxWeight.negate();
		long countOptional = 1;
		List requestedPatches = new ArrayList();
		for (int i = 0; i < ius.length; i++) {
			RequiredCapability[] reqs1 = ius[i].getRequiredCapabilities();
			for (int j = 0; j < reqs1.length; j++) {
				if (!reqs1[j].isOptional())
					continue;
				Collector matches = picker.query(new CapabilityQuery(reqs1[j]), new Collector(), null);
				for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
					IInstallableUnit match = (IInstallableUnit) iterator.next();
					if (match instanceof IInstallableUnitPatch) {
						requestedPatches.add(match);
						countOptional = countOptional + 1;
					} else
						weightedObjects.add(WeightedObject.newWO(match, optionalWeight));
				}
			}
		}

		BigInteger patchWeight = maxWeight.multiply(POWER).multiply(BigInteger.valueOf(countOptional)).negate();
		for (Iterator iterator = requestedPatches.iterator(); iterator.hasNext();) {
			weightedObjects.add(WeightedObject.newWO(newIUVariable(((IInstallableUnit) iterator.next())), patchWeight));
		}
		if (!weightedObjects.isEmpty()) {
			createObjectiveFunction(weightedObjects);
		}
	}

	private void createObjectiveFunction(List weightedObjects) {
		if (DEBUG) {
			StringBuffer b = new StringBuffer();
			for (Iterator i = weightedObjects.iterator(); i.hasNext();) {
				WeightedObject object = (WeightedObject) i.next();
				if (b.length() > 0)
					b.append(", "); //$NON-NLS-1$
				b.append(object.getWeight());
				b.append(' ');
				b.append(object.thing);
			}
			Tracing.debug("objective function: " + b); //$NON-NLS-1$
		}
		dependencyHelper.setObjectiveFunction((WeightedObject[]) weightedObjects.toArray(new WeightedObject[weightedObjects.size()]));
	}

	private void createMustHave(IInstallableUnit iu) {
		IUVariable variable = newIUVariable(iu);
		if (DEBUG) {
			Tracing.debug(variable + "=1"); //$NON-NLS-1$
		}
		// dependencyHelper.setTrue(variable, new Explanation.IUToInstall(iu));
		assumptions.add(variable);
	}

	private void createNegation(IInstallableUnit iu, RequiredCapability req) throws ContradictionException {
		IUVariable variable = newIUVariable(iu);
		if (DEBUG) {
			Tracing.debug(variable + "=0"); //$NON-NLS-1$
		}
		dependencyHelper.setFalse(variable, new Explanation.MissingIU(iu, req));
	}

	// Check whether the requirement is applicable
	private boolean isApplicable(RequiredCapability req) {
		String filter = req.getFilter();
		if (filter == null)
			return true;
		try {
			return DirectorActivator.context.createFilter(filter).match(selectionContext);
		} catch (InvalidSyntaxException e) {
			return false;
		}
	}

	private boolean isApplicable(IInstallableUnit iu) {
		String enablementFilter = iu.getFilter();
		if (enablementFilter == null)
			return true;
		try {
			return DirectorActivator.context.createFilter(enablementFilter).match(selectionContext);
		} catch (InvalidSyntaxException e) {
			return false;
		}
	}

	private void expandRequirement(RequiredCapability req, IInstallableUnit iu, PropositionalVariable iuVar, List optionalAbstractRequirements) throws ContradictionException {
		if (!isApplicable(req))
			return;
		List matches = getApplicableMatches(req);
		if (!req.isOptional()) {
			if (matches.isEmpty()) {
				missingRequirement(iu, req);
			} else {
				createImplication(iuVar, matches, new Explanation.HardRequirement(iu, req));
			}
		} else {
			if (!matches.isEmpty()) {
				PropositionalVariable abs = getAbstractVariable();
				createImplication(abs, matches, Explanation.OPTIONAL_REQUIREMENT);
				optionalAbstractRequirements.add(abs);
			}
		}
	}

	private void expandRequirements(RequiredCapability[] reqs, IInstallableUnit iu, PropositionalVariable iuVar) throws ContradictionException {
		if (reqs.length == 0) {
			return;
		}
		List optionalAbstractRequirements = new ArrayList();
		for (int i = 0; i < reqs.length; i++) {
			expandRequirement(reqs[i], iu, iuVar, optionalAbstractRequirements);
		}
		createOptionalityExpression(iu, optionalAbstractRequirements);
	}

	public void processIU(IInstallableUnit iu) throws ContradictionException {
		iu = iu.unresolved();

		slice.put(iu.getId(), iu.getVersion(), iu);
		if (!isApplicable(iu)) {
			createNegation(iu, null);
			return;
		}

		Collector patches = getApplicablePatches(iu);
		expandLifeCycle(iu);
		PropositionalVariable iuVar;
		if (isFragment(iu)) {
			iuVar = newFragmentVariable(iu);
		} else {
			iuVar = newIUVariable(iu);

		}
		//No patches apply, normal code path
		if (patches.size() == 0) {
			expandRequirements(iu.getRequiredCapabilities(), iu, iuVar);
		} else {
			//Patches are applicable to the IU

			//Unmodified dependencies
			Map unchangedRequirements = new HashMap(iu.getRequiredCapabilities().length);
			for (Iterator iterator = patches.iterator(); iterator.hasNext();) {
				IInstallableUnitPatch patch = (IInstallableUnitPatch) iterator.next();
				RequiredCapability[][] reqs = mergeRequirements(iu, patch);
				if (reqs.length == 0)
					return;

				// Optional requirements are encoded via:
				// ABS -> (match1(req) or match2(req) or ... or matchN(req))
				// noop(IU)-> ~ABS
				// IU -> (noop(IU) or ABS)
				// Therefore we only need one optional requirement statement per IU
				List optionalAbstractRequirements = new ArrayList();
				for (int i = 0; i < reqs.length; i++) {
					//The requirement is unchanged
					if (reqs[i][0] == reqs[i][1]) {
						if (!isApplicable(reqs[i][0]))
							continue;

						List patchesAppliedElseWhere = (List) unchangedRequirements.get(reqs[i][0]);
						if (patchesAppliedElseWhere == null) {
							patchesAppliedElseWhere = new ArrayList();
							unchangedRequirements.put(reqs[i][0], patchesAppliedElseWhere);
						}
						patchesAppliedElseWhere.add(patch);
						continue;
					}

					PropositionalVariable patchVar = newIUVariable(patch);
					//Generate dependency when the patch is applied
					//P1 -> (A -> D) equiv. (P1 & A) -> D
					if (isApplicable(reqs[i][1])) {
						RequiredCapability req = reqs[i][1];
						List matches = getApplicableMatches(req);
						iuVar.handleMatches(matches);
						if (!req.isOptional()) {
							if (matches.isEmpty()) {
								missingRequirement(patch, req);
							} else {
								createImplication(new PropositionalVariable[] {patchVar, iuVar}, matches, new Explanation.HardRequirement(iu, req));
							}
						} else {
							if (!matches.isEmpty()) {
								PropositionalVariable abs = getAbstractVariable();
								createImplication(new PropositionalVariable[] {patchVar, abs}, matches, Explanation.OPTIONAL_REQUIREMENT);
								optionalAbstractRequirements.add(abs);
							}
						}
					}
					//Generate dependency when the patch is not applied
					//-P1 -> (A -> B) ( equiv. A -> (P1 or B) )
					if (isApplicable(reqs[i][0])) {
						RequiredCapability req = reqs[i][0];
						List matches = getApplicableMatches(req);
						iuVar.handleMatches(matches);
						if (!req.isOptional()) {
							if (matches.isEmpty()) {
								dependencyHelper.implication(new Object[] {iuVar}).implies(patchVar).named(new Explanation.PatchedHardRequirement(iu, patch));
							} else {
								matches.add(patchVar);
								createImplication(iuVar, matches, new Explanation.HardRequirement(iu, req));
							}
						} else {
							if (!matches.isEmpty()) {
								PropositionalVariable abs = getAbstractVariable();
								optionalAbstractRequirements.add(patchVar);
								createImplication(abs, matches, Explanation.OPTIONAL_REQUIREMENT);
								optionalAbstractRequirements.add(abs);
							}
						}
					}
				}
				createOptionalityExpression(iu, optionalAbstractRequirements);
			}
			List optionalAbstractRequirements = new ArrayList();
			for (Iterator iterator = unchangedRequirements.entrySet().iterator(); iterator.hasNext();) {
				Entry entry = (Entry) iterator.next();
				List patchesApplied = (List) entry.getValue();
				List allPatches = new ArrayList(patches.toCollection());
				allPatches.removeAll(patchesApplied);
				List requiredPatches = new ArrayList();
				for (Iterator iterator2 = allPatches.iterator(); iterator2.hasNext();) {
					IInstallableUnitPatch patch = (IInstallableUnitPatch) iterator2.next();
					requiredPatches.add(newIUVariable(patch));
				}
				RequiredCapability req = (RequiredCapability) entry.getKey();
				List matches = getApplicableMatches(req);
				iuVar.handleMatches(matches);
				if (!req.isOptional()) {
					if (matches.isEmpty()) {
						missingRequirement(iu, req);
					} else {
						if (!requiredPatches.isEmpty())
							matches.addAll(requiredPatches);
						createImplication(iuVar, matches, new Explanation.HardRequirement(iu, req));
					}
				} else {
					if (!matches.isEmpty()) {
						if (!requiredPatches.isEmpty())
							matches.addAll(requiredPatches);
						PropositionalVariable abs = getAbstractVariable();
						createImplication(abs, matches, Explanation.OPTIONAL_REQUIREMENT);
						optionalAbstractRequirements.add(abs);
					}
				}
			}
			createOptionalityExpression(iu, optionalAbstractRequirements);
		}
	}

	private void expandLifeCycle(IInstallableUnit iu) throws ContradictionException {
		if (!(iu instanceof IInstallableUnitPatch))
			return;
		IInstallableUnitPatch patch = (IInstallableUnitPatch) iu;
		RequiredCapability req = patch.getLifeCycle();
		if (req == null)
			return;
		expandRequirement(req, iu, newIUVariable(iu), Collections.EMPTY_LIST);
	}

	private void missingRequirement(IInstallableUnit iu, RequiredCapability req) throws ContradictionException {
		result.add(new Status(IStatus.WARNING, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Unsatisfied_dependency, iu, req)));
		createNegation(iu, req);
	}

	/**
	 *
	 * @param req
	 * @return a list of mandatory requirements if any, an empty list if req.isOptional().
	 */
	private List getApplicableMatches(RequiredCapability req) {
		List target = new ArrayList();
		Collector matches = picker.query(new CapabilityQuery(req), new Collector(), null);
		for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
			IInstallableUnit match = (IInstallableUnit) iterator.next();
			if (isApplicable(match)) {
				target.add(newIUVariable(match));
			}
		}
		return target;
	}

	//Return a new array of requirements representing the application of the patch
	private RequiredCapability[][] mergeRequirements(IInstallableUnit iu, IInstallableUnitPatch patch) {
		if (patch == null)
			return null;
		RequirementChange[] changes = patch.getRequirementsChange();
		RequiredCapability[] originalRequirements = new RequiredCapability[iu.getRequiredCapabilities().length];
		System.arraycopy(iu.getRequiredCapabilities(), 0, originalRequirements, 0, originalRequirements.length);
		List rrr = new ArrayList();
		boolean found = false;
		for (int i = 0; i < changes.length; i++) {
			for (int j = 0; j < originalRequirements.length; j++) {
				if (originalRequirements[j] != null && changes[i].matches(originalRequirements[j])) {
					found = true;
					if (changes[i].newValue() != null)
						rrr.add(new RequiredCapability[] {originalRequirements[j], changes[i].newValue()});
					else
						// case where a requirement is removed
						rrr.add(new RequiredCapability[] {originalRequirements[j], null});
					originalRequirements[j] = null;
				}
				//				break;
			}
			if (!found && changes[i].applyOn() == null && changes[i].newValue() != null) //Case where a new requirement is added
				rrr.add(new RequiredCapability[] {null, changes[i].newValue()});
		}
		//Add all the unmodified requirements to the result
		for (int i = 0; i < originalRequirements.length; i++) {
			if (originalRequirements[i] != null)
				rrr.add(new RequiredCapability[] {originalRequirements[i], originalRequirements[i]});
		}
		return (RequiredCapability[][]) rrr.toArray(new RequiredCapability[rrr.size()][]);
	}

	/**
	 * Optional requirements are encoded via:
	 * ABS -> (match1(req) or match2(req) or ... or matchN(req))
	 * noop(IU)-> ~ABS
	 * IU -> (noop(IU) or ABS)
	 * @param iu
	 * @param optionalRequirements
	 * @throws ContradictionException
	 */
	private void createOptionalityExpression(IInstallableUnit iu, List optionalRequirements) throws ContradictionException {
		if (optionalRequirements.isEmpty())
			return;
		IUVariable iuVar = newIUVariable(iu);
		PropositionalVariable noop = getNoOperationVariable(iu);
		for (Iterator i = optionalRequirements.iterator(); i.hasNext();) {
			PropositionalVariable abs = (PropositionalVariable) i.next();
			createIncompatibleValues(abs, noop);
		}
		optionalRequirements.add(noop);
		createImplication(iuVar, optionalRequirements, Explanation.OPTIONAL_REQUIREMENT);
	}

	private void createImplication(PropositionalVariable left, List right, Explanation name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + left + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		dependencyHelper.implication(new Object[] {left}).implies(right.toArray()).named(name);
	}

	private void createImplication(PropositionalVariable[] left, List right, Explanation name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + left + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		dependencyHelper.implication(left).implies(right.toArray()).named(name);
	}

	//Return IUPatches that are applicable for the given iu
	private Collector getApplicablePatches(IInstallableUnit iu) {
		if (patches == null)
			patches = new QueryableArray((IInstallableUnit[]) picker.query(ApplicablePatchQuery.ANY, new Collector(), null).toArray(IInstallableUnit.class));

		return patches.query(new ApplicablePatchQuery(iu), new Collector(), null);
	}

	//Create constraints to deal with singleton
	//When there is a mix of singleton and non singleton, several constraints are generated
	private void createConstraintsForSingleton() throws ContradictionException {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			HashMap conflictingEntries = (HashMap) entry.getValue();
			if (conflictingEntries.size() < 2)
				continue;

			Collection conflictingVersions = conflictingEntries.values();
			List singletons = new ArrayList();
			List nonSingletons = new ArrayList();
			for (Iterator conflictIterator = conflictingVersions.iterator(); conflictIterator.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) conflictIterator.next();
				if (iu.isSingleton()) {
					singletons.add(newIUVariable(iu));
				} else {
					nonSingletons.add(newIUVariable(iu));
				}
			}
			if (singletons.isEmpty())
				continue;

			PropositionalVariable[] singletonArray = (PropositionalVariable[]) singletons.toArray(new PropositionalVariable[singletons.size() + 1]);
			for (Iterator iterator2 = nonSingletons.iterator(); iterator2.hasNext();) {
				singletonArray[singletonArray.length - 1] = (PropositionalVariable) iterator2.next();
				createAtMostOne(singletonArray);
			}
			singletonArray = (PropositionalVariable[]) singletons.toArray(new PropositionalVariable[singletons.size()]);
			createAtMostOne(singletonArray);
		}
	}

	private void createAtMostOne(PropositionalVariable[] vars) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug("At most 1 of " + Arrays.asList(vars)); //$NON-NLS-1$
		}
		IInstallableUnit[] ius = new IInstallableUnit[vars.length];
		int i = 0;
		for (int j = 0; j < vars.length; j++) {
			ius[i++] = ((IUVariable) vars[j]).getInstallableUnit();
		}
		dependencyHelper.atMost(1, vars).named(new Explanation.Singleton(ius));
	}

	private void createIncompatibleValues(PropositionalVariable v1, PropositionalVariable v2) throws ContradictionException {
		PropositionalVariable[] vars = {v1, v2};
		if (DEBUG) {
			Tracing.debug("At most 1 of " + Arrays.asList(vars)); //$NON-NLS-1$
		}
		dependencyHelper.atMost(1, vars).named(Explanation.OPTIONAL_REQUIREMENT);
	}

	private PropositionalVariable getAbstractVariable() {
		AbstractVariable abstractVariable = new AbstractVariable();
		abstractVariables.add(abstractVariable);
		return abstractVariable;
	}

	private PropositionalVariable getNoOperationVariable(IInstallableUnit iu) {
		PropositionalVariable v = (PropositionalVariable) noopVariables.get(iu);
		if (v == null) {
			v = new AbstractVariable();
			noopVariables.put(iu, v);
		}
		return v;
	}

	public IStatus invokeSolver(IProgressMonitor monitor) {
		if (result.getSeverity() == IStatus.ERROR)
			return result;
		// CNF filename is given on the command line
		long start = System.currentTimeMillis();
		if (DEBUG)
			Tracing.debug("Invoking solver: " + start); //$NON-NLS-1$
		try {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			if (dependencyHelper.hasASolution(assumptions)) {
				if (DEBUG) {
					Tracing.debug("Satisfiable !"); //$NON-NLS-1$
				}
				backToIU();
				long stop = System.currentTimeMillis();
				if (DEBUG)
					Tracing.debug("Solver solution found: " + (stop - start)); //$NON-NLS-1$
			} else {
				long stop = System.currentTimeMillis();
				if (DEBUG) {
					Tracing.debug("Unsatisfiable !"); //$NON-NLS-1$
					Tracing.debug("Solver solution NOT found: " + (stop - start)); //$NON-NLS-1$
				}
				result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, Messages.Planner_Unsatisfiable_problem));
			}
		} catch (TimeoutException e) {
			result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, Messages.Planner_Timeout));
		} catch (Exception e) {
			result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, Messages.Planner_Unexpected_problem, e));
		}
		if (DEBUG)
			System.out.println();
		return result;
	}

	private void backToIU() {
		solution = new ArrayList();
		IVec sat4jSolution = dependencyHelper.getSolution();
		for (Iterator i = sat4jSolution.iterator(); i.hasNext();) {
			Object var = i.next();
			if (var instanceof IUVariable) {
				IUVariable iuVar = (IUVariable) var;
				solution.add(iuVar.getInstallableUnit());
			}
		}
	}

	private void printSolution(Collection state) {
		ArrayList l = new ArrayList(state);
		Collections.sort(l);
		Tracing.debug("Solution:"); //$NON-NLS-1$
		Tracing.debug("Numbers of IUs selected: " + l.size()); //$NON-NLS-1$
		for (Iterator iterator = l.iterator(); iterator.hasNext();) {
			Tracing.debug(iterator.next().toString());
		}
	}

	public Collection extractSolution() {
		if (DEBUG)
			printSolution(solution);
		return solution;
	}

	public Collection extractFragments() {
		Collection col = new ArrayList();
		for (Iterator iterator = fragments.values().iterator(); iterator.hasNext();) {
			Fragment fragment = (Fragment) iterator.next();
			for (Iterator it = fragment.matches.iterator(); it.hasNext();) {
				IUVariable var = (IUVariable) it.next();
				if (dependencyHelper.getBooleanValueFor(var)) {
					col.add(var.getInstallableUnit());
				}
			}
		}
		return col;
	}

}