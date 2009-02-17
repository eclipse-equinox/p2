/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
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
 */
public class Projector {
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

	private MultiStatus result;

	abstract class PropositionalVariable {

	}

	class AbstractVariable extends PropositionalVariable {
		@Override
		public String toString() {
			return "AbstractVariable: " + hashCode();
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
			return iu.getId().hashCode() * 19 + iu.getVersion().hashCode() * 5779;
		}

		public String toString() {
			return iu.getId() + '_' + iu.getVersion();
		}

		public IInstallableUnit getInstallableUnit() {
			return iu;
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

	public Projector(IQueryable q, Dictionary context) {
		picker = q;
		variables = new HashMap();
		noopVariables = new HashMap();
		slice = new TwoTierMap();
		selectionContext = context;
		abstractVariables = new ArrayList();
		result = new MultiStatus(DirectorActivator.PI_DIRECTOR, IStatus.OK, Messages.Planner_Problems_resolving_plan, null);
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
			solver.setTimeoutOnConflicts(1000);
			dependencyHelper = new DependencyHelper(solver, 100000);

			Iterator iusToEncode = picker.query(InstallableUnitQuery.ANY, new Collector(), null).iterator();
			if (DEBUG) {
				List iusToOrder = new ArrayList();
				while (iusToEncode.hasNext()) {
					iusToOrder.add(iusToEncode.next());
				}
				Collections.sort(iusToOrder);
				iusToEncode = iusToOrder.iterator();
			}
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
			// TODO: jkca is this the right answer here?
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

		BigInteger patchWeight = maxWeight.negate();
		if (patches != null) {
			for (int i = 0; i < ius.length; i++) {
				IRequiredCapability[] reqs = ius[i].getRequiredCapabilities();
				for (int j = 0; j < reqs.length; j++) {
					Collector matches = patches.query(new CapabilityQuery(reqs[j]), new Collector(), null);
					for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
						IInstallableUnitPatch match = (IInstallableUnitPatch) iterator.next();
						weightedObjects.add(WeightedObject.newWO(newIUVariable(match), patchWeight));
					}
				}
			}
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
			Tracing.debug("objective function: " + b);
		}
		dependencyHelper.setObjectiveFunction((WeightedObject[]) weightedObjects.toArray(new WeightedObject[weightedObjects.size()]));
	}

	private void createMustHave(IInstallableUnit iu) throws ContradictionException {
		IUVariable variable = newIUVariable(iu);
		if (DEBUG) {
			Tracing.debug(variable + "=1"); //$NON-NLS-1$
		}
		dependencyHelper.setTrue(variable, variable.toString());
	}

	private void createNegation(IInstallableUnit iu) throws ContradictionException {
		IUVariable variable = newIUVariable(iu);
		if (DEBUG) {
			Tracing.debug(variable + "=0"); //$NON-NLS-1$
		}
		dependencyHelper.setFalse(variable, '~' + variable.toString());
	}

	//	private void createExistence(IInstallableUnit iu) throws ContradictionException {
	//		IUVariable variable = newIUVariable(iu);
	//		if (DEBUG) {
	//			Tracing.debug(variable + "=1"); //$NON-NLS-1$
	//		}
	//		dependencyHelper.setTrue(variable, variable.toString());
	//	}

	// Check whether the requirement is applicable
	private boolean isApplicable(IRequiredCapability req) {
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

	private void expandRequirement(IRequiredCapability req, IInstallableUnit iu, PropositionalVariable iuVar, List optionalAbstractRequirements) throws ContradictionException {
		if (!isApplicable(req))
			return;
		List matches = getApplicableMatches(req);
		if (!req.isOptional()) {
			if (matches.isEmpty()) {
				missingRequirement(iu, req);
			} else {
				createImplication(iuVar, matches, iuVar + "->" + req);
			}
		} else {
			if (!matches.isEmpty()) {
				PropositionalVariable abs = getAbstractVariable();
				createImplication(abs, matches, "abs -> " + matches);
				optionalAbstractRequirements.add(abs);
			}
		}
	}

	private void expandRequirements(IRequiredCapability[] reqs, IInstallableUnit iu, PropositionalVariable iuVar) throws ContradictionException {
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
			createNegation(iu);
			return;
		}

		Collector patches = getApplicablePatches(iu);
		expandLifeCycle(iu);
		PropositionalVariable iuVar = newIUVariable(iu);
		//No patches apply, normal code path
		if (patches.size() == 0) {
			expandRequirements(iu.getRequiredCapabilities(), iu, iuVar);
		} else {
			//Patches are applicable to the IU

			//Unmodified dependencies
			Map unchangedRequirements = new HashMap(iu.getRequiredCapabilities().length);
			for (Iterator iterator = patches.iterator(); iterator.hasNext();) {
				IInstallableUnitPatch patch = (IInstallableUnitPatch) iterator.next();
				IRequiredCapability[][] reqs = mergeRequirements(iu, patch);
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
						IRequiredCapability req = reqs[i][1];
						List matches = getApplicableMatches(req);
						if (!req.isOptional()) {
							if (matches.isEmpty()) {
								missingRequirement(patch, req);
							} else {
								createImplication(new PropositionalVariable[] {patchVar, iuVar}, matches, "abstract->" + matches);
							}
						} else {
							if (!matches.isEmpty()) {
								PropositionalVariable abs = getAbstractVariable();
								createImplication(new PropositionalVariable[] {patchVar, abs}, matches, "abs -> " + matches);
								optionalAbstractRequirements.add(abs);
							}
						}
					}
					//Generate dependency when the patch is not applied
					//-P1 -> (A -> B) ( equiv. A -> (P1 or B) )
					if (isApplicable(reqs[i][0])) {
						IRequiredCapability req = reqs[i][0];
						List matches = getApplicableMatches(req);
						if (!req.isOptional()) {
							if (matches.isEmpty()) {
								//TODO Need to change NAME
								dependencyHelper.implication(iuVar).implies(patchVar).named("NAME");
							} else {
								matches.add(patchVar);
								createImplication(iuVar, matches, "B->" + matches);
							}
						} else {
							if (!matches.isEmpty()) {
								PropositionalVariable abs = getAbstractVariable();
								optionalAbstractRequirements.add(patchVar);
								createImplication(abs, matches, "abs -> " + optionalAbstractRequirements);
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
				IRequiredCapability req = (IRequiredCapability) entry.getKey();
				List matches = getApplicableMatches(req);
				if (!req.isOptional()) {
					if (matches.isEmpty()) {
						missingRequirement(iu, req);
					} else {
						if (!requiredPatches.isEmpty())
							matches.addAll(requiredPatches);
						createImplication(iuVar, matches, iuVar + "->" + req);
					}
				} else {
					if (!matches.isEmpty()) {
						if (!requiredPatches.isEmpty())
							matches.addAll(requiredPatches);
						PropositionalVariable abs = getAbstractVariable();
						createImplication(abs, matches, "abs -> " + matches);
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
		IRequiredCapability req = patch.getLifeCycle();
		if (req == null)
			return;
		expandRequirement(req, iu, newIUVariable(iu), Collections.EMPTY_LIST);
	}

	private void missingRequirement(IInstallableUnit iu, IRequiredCapability req) throws ContradictionException {
		result.add(new Status(IStatus.WARNING, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Unsatisfied_dependency, iu, req)));
		createNegation(iu);
	}

	/**
	 *
	 * @param iu
	 * @param req
	 * @param expandedOptionalRequirement a collector list to gather optional requirements. It will be updated
	 *        if req.isOptional()
	 * @return a list of mandatory requirements if any, an empty list if req.isOptional().
	 */
	private List getApplicableMatches(IRequiredCapability req) {
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
	private IRequiredCapability[][] mergeRequirements(IInstallableUnit iu, IInstallableUnitPatch patch) {
		if (patch == null)
			return null;
		IRequirementChange[] changes = patch.getRequirementsChange();
		IRequiredCapability[] originalRequirements = new IRequiredCapability[iu.getRequiredCapabilities().length];
		System.arraycopy(iu.getRequiredCapabilities(), 0, originalRequirements, 0, originalRequirements.length);
		List rrr = new ArrayList();
		boolean found = false;
		for (int i = 0; i < changes.length; i++) {
			for (int j = 0; j < originalRequirements.length; j++) {
				if (originalRequirements[j] != null && changes[i].matches(originalRequirements[j])) {
					found = true;
					if (changes[i].newValue() != null)
						rrr.add(new IRequiredCapability[] {originalRequirements[j], changes[i].newValue()});
					else
						// case where a requirement is removed
						rrr.add(new IRequiredCapability[] {originalRequirements[j], null});
					originalRequirements[j] = null;
				}
				//				break;
			}
			if (!found && changes[i].applyOn() == null && changes[i].newValue() != null) //Case where a new requirement is added
				rrr.add(new IRequiredCapability[] {null, changes[i].newValue()});
		}
		//Add all the unmodified requirements to the result
		for (int i = 0; i < originalRequirements.length; i++) {
			if (originalRequirements[i] != null)
				rrr.add(new IRequiredCapability[] {originalRequirements[i], originalRequirements[i]});
		}
		return (IRequiredCapability[][]) rrr.toArray(new IRequiredCapability[rrr.size()][]);
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
			createAtMostOne(new PropositionalVariable[] {abs, noop});
		}
		optionalRequirements.add(noop);
		createImplication(iuVar, optionalRequirements, iu + "-> noop " + optionalRequirements);
	}

	private void createImplication(PropositionalVariable left, List right, String name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + left + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		dependencyHelper.implication(left).implies(right.toArray()).named(name);
	}

	private void createImplication(PropositionalVariable[] left, List right, String name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + left + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		dependencyHelper.implication(left).implies(right.toArray()).named(name);
	}

	//	private void createImplication(PropositionalVariable[] left, PropositionalVariable right, String name) throws ContradictionException {
	//		if (DEBUG) {
	//			Tracing.debug(name + ": " + Arrays.toString(left) + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
	//		}
	//		dependencyHelper.implication(left).implies(right).named(name);
	//	}

	//	private void createImplication(Object[] left, List right, String name) throws ContradictionException {
	//		if (DEBUG) {
	//			Tracing.debug(name + ": " + Arrays.toString(left) + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
	//		}
	//		dependencyHelper.implication(left).implies(right.toArray()).named(name);
	//	}

	//	private void createImplication(PropositionalVariable left, PropositionalVariable[] right, String name) throws ContradictionException {
	//		if (DEBUG) {
	//			Tracing.debug(name + ": " + left + "->" + Arrays.toString(right)); //$NON-NLS-1$ //$NON-NLS-2$
	//		}
	//		dependencyHelper.implication(left).implies(right).named(name);
	//	}

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
			Tracing.debug("At most 1 of " + Arrays.toString(vars)); //$NON-NLS-1$
		}
		dependencyHelper.atMost(1, vars).named("At most 1 of " + vars); //$NON-NLS-1$
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
			if (dependencyHelper.hasASolution()) {
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
				if (DEBUG) {
					start = System.currentTimeMillis();
					Tracing.debug("Determining cause of failure: " + start);
				}
				Set why = dependencyHelper.why();
				if (DEBUG) {
					stop = System.currentTimeMillis();
					Tracing.debug("Explanation found: " + (stop - start));
					Tracing.debug("Explanation:");
					for (Iterator i = why.iterator(); i.hasNext();) {
						Tracing.debug(i.next().toString());
					}
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
}
