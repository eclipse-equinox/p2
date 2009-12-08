/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 * Daniel Le Berre - Fix in the encoding and the optimization function
 * Alban Browaeys - Optimized string concatenation in bug 251357
 * Jed Anderson - switch from opb files to API calls to DependencyHelper in bug 200380
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.LDAPQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.metadata.query.PatchQuery;
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
	static boolean DEBUG = Tracing.DEBUG_PLANNER_PROJECTOR;
	private static boolean DEBUG_ENCODING = false;
	private IQueryable picker;
	private QueryableArray patches;

	private Map noopVariables; //key IU, value AbstractVariable
	private List abstractVariables;

	private TwoTierMap slice; //The IUs that have been considered to be part of the problem

	private Dictionary selectionContext;

	DependencyHelper dependencyHelper;
	private Collection solution;
	private Collection assumptions;

	private MultiStatus result;

	private Collection alreadyInstalledIUs;
	private IQueryable lastState;

	private boolean considerMetaRequirements;
	private IInstallableUnit entryPoint;
	private Map fragments = new HashMap();

	static class AbstractVariable {
		public String toString() {
			return "AbstractVariable: " + hashCode(); //$NON-NLS-1$
		}
	}

	/**
	 * Job for computing SAT failure explanation in the background.
	 */
	class ExplanationJob extends Job {
		private Set explanation;

		public ExplanationJob() {
			super(Messages.Planner_NoSolution);
			//explanations cannot be canceled directly, so don't show it to the user
			setSystem(true);
		}

		public boolean belongsTo(Object family) {
			return family == ExplanationJob.this;
		}

		protected void canceling() {
			super.canceling();
			dependencyHelper.stopExplanation();
		}

		public Set getExplanationResult() {
			return explanation;
		}

		protected IStatus run(IProgressMonitor monitor) {
			long start = 0;
			if (DEBUG) {
				start = System.currentTimeMillis();
				Tracing.debug("Determining cause of failure: " + start); //$NON-NLS-1$
			}
			try {
				explanation = dependencyHelper.why();
				if (DEBUG) {
					long stop = System.currentTimeMillis();
					Tracing.debug("Explanation found: " + (stop - start)); //$NON-NLS-1$
					Tracing.debug("Explanation:"); //$NON-NLS-1$
					for (Iterator i = explanation.iterator(); i.hasNext();) {
						Tracing.debug(i.next().toString());
					}
				}
			} catch (TimeoutException e) {
				if (DEBUG)
					Tracing.debug("Timeout while computing explanations"); //$NON-NLS-1$
			} finally {
				//must never have a null result, because caller is waiting on result to be non-null
				if (explanation == null)
					explanation = Collections.EMPTY_SET;
			}
			synchronized (this) {
				ExplanationJob.this.notify();
			}
			return Status.OK_STATUS;
		}

	}

	public Projector(IQueryable q, Dictionary context, boolean considerMetaRequirements) {
		picker = q;
		noopVariables = new HashMap();
		slice = new TwoTierMap();
		selectionContext = context;
		abstractVariables = new ArrayList();
		result = new MultiStatus(DirectorActivator.PI_DIRECTOR, IStatus.OK, Messages.Planner_Problems_resolving_plan, null);
		assumptions = new ArrayList();
		this.considerMetaRequirements = considerMetaRequirements;
	}

	protected boolean isInstalled(IInstallableUnit iu) {
		return lastState.query(new InstallableUnitQuery(iu), null).size() == 0 ? false : true;
	}

	public void encode(IInstallableUnit entryPointIU, IInstallableUnit[] alreadyExistingRoots, IQueryable installedIUs, IInstallableUnit[] newRoots, IProgressMonitor monitor) {
		alreadyInstalledIUs = Arrays.asList(alreadyExistingRoots);
		lastState = installedIUs;
		this.entryPoint = entryPointIU;
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
			Collector collector = picker.query(InstallableUnitQuery.ANY, null);
			dependencyHelper = new DependencyHelper(solver);

			Iterator iusToEncode = collector.iterator();
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
				IInstallableUnit iuToEncode = (IInstallableUnit) iusToEncode.next();
				if (iuToEncode != entryPointIU) {
					processIU(iuToEncode, false);
				}
			}
			createConstraintsForSingleton();

			createMustHave(entryPointIU, alreadyExistingRoots, newRoots);

			createOptimizationFunction(entryPointIU);
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
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, Messages.Planner_Unsatisfiable_problem));
		}
	}

	//Create an optimization function favoring the highest version of each IU
	private void createOptimizationFunction(IInstallableUnit metaIu) {

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
			BigInteger weight = POWER;
			int count = toSort.size();
			for (int i = 0; i < count; i++) {
				IInstallableUnit iu = (IInstallableUnit) toSort.get(i);
				weightedObjects.add(WeightedObject.newWO(iu, isInstalled(iu) ? BigInteger.ONE : weight));
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
		IRequirement[] reqs = metaIu.getRequiredCapabilities();
		for (int j = 0; j < reqs.length; j++) {
			if (reqs[j].getMin() > 0)
				continue;
			Collector matches = picker.query(reqs[j].getMatches(), null);
			for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
				IInstallableUnit match = (IInstallableUnit) iterator.next();
				if (match instanceof IInstallableUnitPatch) {
					requestedPatches.add(match);
					countOptional = countOptional + 1;
				} else {
					weightedObjects.add(WeightedObject.newWO(match, isInstalled(match) ? BigInteger.ONE : optionalWeight));
				}
			}
		}

		BigInteger patchWeight = maxWeight.multiply(POWER).multiply(BigInteger.valueOf(countOptional)).negate();
		for (Iterator iterator = requestedPatches.iterator(); iterator.hasNext();) {
			weightedObjects.add(WeightedObject.newWO(iterator.next(), patchWeight));
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

	private void createMustHave(IInstallableUnit iu, IInstallableUnit[] alreadyExistingRoots, IInstallableUnit[] newRoots) throws ContradictionException {
		processIU(iu, true);
		if (DEBUG) {
			Tracing.debug(iu + "=1"); //$NON-NLS-1$
		}
		// dependencyHelper.setTrue(variable, new Explanation.IUToInstall(iu));
		assumptions.add(iu);
	}

	private void createNegation(IInstallableUnit iu, IRequirement req) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(iu + "=0"); //$NON-NLS-1$
		}
		dependencyHelper.setFalse(iu, new Explanation.MissingIU(iu, req));
	}

	// Check whether the requirement is applicable
	private boolean isApplicable(IRequirement req) {
		IQuery filter = req.getFilter();
		if (filter == null)
			return true;
		if (filter instanceof LDAPQuery)
			try {
				return DirectorActivator.context.createFilter(((LDAPQuery) filter).getFilter()).match(selectionContext);
			} catch (InvalidSyntaxException e) {
				return false;
			}
		throw new IllegalArgumentException();
	}

	private boolean isApplicable(IInstallableUnit iu) {
		LDAPQuery enablementFilter = (LDAPQuery) iu.getFilter();
		if (enablementFilter == null)
			return true;
		try {
			return DirectorActivator.context.createFilter(enablementFilter.getFilter()).match(selectionContext);
		} catch (InvalidSyntaxException e) {
			return false;
		}
	}

	private void expandNegatedRequirement(IRequirement req, IInstallableUnit iu, List optionalAbstractRequirements, boolean isRootIu) throws ContradictionException {
		if (!isApplicable(req))
			return;
		List matches = getApplicableMatches(req);
		if (matches.isEmpty()) {
			return;
		}
		Explanation explanation;
		if (isRootIu) {
			IInstallableUnit reqIu = (IInstallableUnit) matches.get(0);
			if (alreadyInstalledIUs.contains(reqIu)) {
				explanation = new Explanation.IUInstalled(reqIu);
			} else {
				explanation = new Explanation.IUToInstall(reqIu);
			}
		} else {
			explanation = new Explanation.HardRequirement(iu, req);
		}
		createNegationImplication(iu, matches, explanation);
	}

	private void expandRequirement(IRequirement req, IInstallableUnit iu, List optionalAbstractRequirements, boolean isRootIu) throws ContradictionException {
		if (req.getMax() == 0) {
			expandNegatedRequirement(req, iu, optionalAbstractRequirements, isRootIu);
			return;
		}
		if (!isApplicable(req))
			return;
		List matches = getApplicableMatches(req);
		if (isHostRequirement(iu, req)) {
			rememberHostMatches(iu, matches);
		}
		if (req.getMin() > 0) {
			if (matches.isEmpty()) {
				missingRequirement(iu, req);
			} else {
				IInstallableUnit reqIu = (IInstallableUnit) matches.get(0);
				Explanation explanation;
				if (isRootIu) {
					if (alreadyInstalledIUs.contains(reqIu)) {
						explanation = new Explanation.IUInstalled(reqIu);
					} else {
						explanation = new Explanation.IUToInstall(reqIu);
					}
				} else {
					explanation = new Explanation.HardRequirement(iu, req);
				}
				createImplication(iu, matches, explanation);
			}
		} else {
			if (!matches.isEmpty()) {
				AbstractVariable abs = getAbstractVariable();
				createImplication(new Object[] {abs, iu}, matches, Explanation.OPTIONAL_REQUIREMENT);
				optionalAbstractRequirements.add(abs);
			}
		}
	}

	private void expandRequirements(IRequirement[] reqs, IInstallableUnit iu, boolean isRootIu) throws ContradictionException {
		if (reqs.length == 0) {
			return;
		}
		List optionalAbstractRequirements = new ArrayList();
		for (int i = 0; i < reqs.length; i++) {
			expandRequirement(reqs[i], iu, optionalAbstractRequirements, isRootIu);
		}
		createOptionalityExpression(iu, optionalAbstractRequirements);
	}

	public void processIU(IInstallableUnit iu, boolean isRootIU) throws ContradictionException {
		iu = iu.unresolved();

		slice.put(iu.getId(), iu.getVersion(), iu);
		if (!isApplicable(iu)) {
			createNegation(iu, null);
			return;
		}

		Collector applicablePatches = getApplicablePatches(iu);
		expandLifeCycle(iu, isRootIU);
		//No patches apply, normal code path
		if (applicablePatches.size() == 0) {
			expandRequirements(getRequiredCapabilities(iu), iu, isRootIU);
		} else {
			//Patches are applicable to the IU
			expandRequirementsWithPatches(iu, applicablePatches, isRootIU);
		}
	}

	private IRequirement[] getRequiredCapabilities(IInstallableUnit iu) {
		if (considerMetaRequirements == false || iu.getMetaRequiredCapabilities().length == 0)
			return iu.getRequiredCapabilities();
		IRequirement[] aggregatedCapabilities = new IRequirement[iu.getRequiredCapabilities().length + iu.getMetaRequiredCapabilities().length];
		System.arraycopy(iu.getRequiredCapabilities(), 0, aggregatedCapabilities, 0, iu.getRequiredCapabilities().length);
		System.arraycopy(iu.getMetaRequiredCapabilities(), 0, aggregatedCapabilities, iu.getRequiredCapabilities().length, iu.getMetaRequiredCapabilities().length);
		return aggregatedCapabilities;
	}

	static final class Pending {
		List matches;
		Explanation explanation;
		Object left;
	}

	private void expandRequirementsWithPatches(IInstallableUnit iu, Collector applicablePatches, boolean isRootIu) throws ContradictionException {
		//Unmodified dependencies
		Map unchangedRequirements = new HashMap(getRequiredCapabilities(iu).length);
		Map nonPatchedRequirements = new HashMap(getRequiredCapabilities(iu).length);
		for (Iterator iterator = applicablePatches.iterator(); iterator.hasNext();) {
			IInstallableUnitPatch patch = (IInstallableUnitPatch) iterator.next();
			IRequirement[][] reqs = mergeRequirements(iu, patch);
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

				//Generate dependency when the patch is applied
				//P1 -> (A -> D) equiv. (P1 & A) -> D
				if (isApplicable(reqs[i][1])) {
					IRequirement req = reqs[i][1];
					List matches = getApplicableMatches(req);
					if (isHostRequirement(iu, req)) {
						rememberHostMatches(iu, matches);
					}
					if (req.getMin() > 0) {
						if (matches.isEmpty()) {
							missingRequirement(patch, req);
						} else {
							IInstallableUnit reqIu = (IInstallableUnit) matches.get(0);
							Explanation explanation;
							if (isRootIu) {
								if (alreadyInstalledIUs.contains(reqIu)) {
									explanation = new Explanation.IUInstalled(reqIu);
								} else {
									explanation = new Explanation.IUToInstall(reqIu);
								}
							} else {
								explanation = new Explanation.PatchedHardRequirement(iu, req, patch);
							}
							createImplication(new Object[] {patch, iu}, matches, explanation);
						}
					} else {
						if (!matches.isEmpty()) {
							AbstractVariable abs = getAbstractVariable();
							createImplication(new Object[] {patch, abs, iu}, matches, Explanation.OPTIONAL_REQUIREMENT);
							optionalAbstractRequirements.add(abs);
						}
					}
				}
				//Generate dependency when the patch is not applied
				//-P1 -> (A -> B) ( equiv. A -> (P1 or B) )
				if (isApplicable(reqs[i][0])) {
					IRequirement req = reqs[i][0];

					// Fix: if multiple patches apply to the same IU-req, we need to make sure we list each
					// patch as an optional match
					Pending pending = (Pending) nonPatchedRequirements.get(req);
					if (pending != null) {
						pending.matches.add(patch);
						continue;
					}

					List matches = getApplicableMatches(req);
					if (isHostRequirement(iu, req)) {
						rememberHostMatches(iu, matches);
					}
					if (req.getMin() > 0) {
						if (matches.isEmpty()) {
							dependencyHelper.implication(new Object[] {iu}).implies(patch).named(new Explanation.HardRequirement(iu, null));
						} else {
							matches.add(patch);
							IInstallableUnit reqIu = (IInstallableUnit) matches.get(0);///(IInstallableUnit) picker.query(new CapabilityQuery(req), new Collector(), null).iterator().next();

							Explanation explanation;
							if (isRootIu) {
								if (alreadyInstalledIUs.contains(reqIu)) {
									explanation = new Explanation.IUInstalled(reqIu);
								} else {
									explanation = new Explanation.IUToInstall(reqIu);
								}
							} else {
								explanation = new Explanation.HardRequirement(iu, req);
							}

							// Fix: make sure we collect all patches that will impact this IU-req, not just one
							pending = new Pending();
							pending.left = iu;
							pending.explanation = explanation;
							pending.matches = matches;
							nonPatchedRequirements.put(req, pending);
						}
					} else {
						if (!matches.isEmpty()) {
							AbstractVariable abs = getAbstractVariable();
							matches.add(patch);

							// Fix: make sure we collect all patches that will impact this IU-req, not just one
							pending = new Pending();
							pending.left = new Object[] {abs, iu};
							pending.explanation = Explanation.OPTIONAL_REQUIREMENT;
							pending.matches = matches;
							nonPatchedRequirements.put(req, pending);

							optionalAbstractRequirements.add(abs);
						}
					}
				}
			}
			createOptionalityExpression(iu, optionalAbstractRequirements);
		}

		// Fix: now create the pending non-patch requirements based on the full set of patches
		for (Iterator iterator = nonPatchedRequirements.values().iterator(); iterator.hasNext();) {
			Pending pending = (Pending) iterator.next();
			createImplication(pending.left, pending.matches, pending.explanation);
		}

		List optionalAbstractRequirements = new ArrayList();
		for (Iterator iterator = unchangedRequirements.entrySet().iterator(); iterator.hasNext();) {
			Entry entry = (Entry) iterator.next();
			List patchesApplied = (List) entry.getValue();
			List allPatches = new ArrayList(applicablePatches.toCollection());
			allPatches.removeAll(patchesApplied);
			List requiredPatches = new ArrayList();
			for (Iterator iterator2 = allPatches.iterator(); iterator2.hasNext();) {
				IInstallableUnitPatch patch = (IInstallableUnitPatch) iterator2.next();
				requiredPatches.add(patch);
			}
			IRequirement req = (IRequirement) entry.getKey();
			List matches = getApplicableMatches(req);
			if (isHostRequirement(iu, req)) {
				rememberHostMatches(iu, matches);
			}
			if (req.getMin() > 0) {
				if (matches.isEmpty()) {
					if (requiredPatches.isEmpty()) {
						missingRequirement(iu, req);
					} else {
						createImplication(iu, requiredPatches, new Explanation.HardRequirement(iu, req));
					}
				} else {
					if (!requiredPatches.isEmpty())
						matches.addAll(requiredPatches);
					IInstallableUnit reqIu = (IInstallableUnit) matches.get(0);//(IInstallableUnit) picker.query(new CapabilityQuery(req), new Collector(), null).iterator().next();
					Explanation explanation;
					if (isRootIu) {
						if (alreadyInstalledIUs.contains(reqIu)) {
							explanation = new Explanation.IUInstalled(reqIu);
						} else {
							explanation = new Explanation.IUToInstall(reqIu);
						}
					} else {
						explanation = new Explanation.HardRequirement(iu, req);
					}
					createImplication(iu, matches, explanation);
				}
			} else {
				if (!matches.isEmpty()) {
					if (!requiredPatches.isEmpty())
						matches.addAll(requiredPatches);
					AbstractVariable abs = getAbstractVariable();
					createImplication(new Object[] {abs, iu}, matches, Explanation.OPTIONAL_REQUIREMENT);
					optionalAbstractRequirements.add(abs);
				}
			}
		}
		createOptionalityExpression(iu, optionalAbstractRequirements);
	}

	private void expandLifeCycle(IInstallableUnit iu, boolean isRootIu) throws ContradictionException {
		if (!(iu instanceof IInstallableUnitPatch))
			return;
		IInstallableUnitPatch patch = (IInstallableUnitPatch) iu;
		IRequirement req = patch.getLifeCycle();
		if (req == null)
			return;
		expandRequirement(req, iu, Collections.EMPTY_LIST, isRootIu);
	}

	private void missingRequirement(IInstallableUnit iu, IRequirement req) throws ContradictionException {
		result.add(new Status(IStatus.WARNING, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Unsatisfied_dependency, iu, req)));
		createNegation(iu, req);
	}

	/**
	 * @param req
	 * @return a list of mandatory requirements if any, an empty list if req.isOptional().
	 */
	private List getApplicableMatches(IRequirement req) {
		List target = new ArrayList();
		Collector matches = picker.query(req.getMatches(), null);
		for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
			IInstallableUnit match = (IInstallableUnit) iterator.next();
			if (isApplicable(match)) {
				target.add(match);
			}
		}
		return target;
	}

	//Return a new array of requirements representing the application of the patch
	private IRequirement[][] mergeRequirements(IInstallableUnit iu, IInstallableUnitPatch patch) {
		if (patch == null)
			return null;
		IRequirementChange[] changes = patch.getRequirementsChange();
		IRequirement[] originalRequirements = new IRequirement[iu.getRequiredCapabilities().length];
		System.arraycopy(iu.getRequiredCapabilities(), 0, originalRequirements, 0, originalRequirements.length);
		List rrr = new ArrayList();
		boolean found = false;
		for (int i = 0; i < changes.length; i++) {
			for (int j = 0; j < originalRequirements.length; j++) {
				if (originalRequirements[j] != null && changes[i].matches((IRequiredCapability) originalRequirements[j])) {
					found = true;
					if (changes[i].newValue() != null)
						rrr.add(new IRequirement[] {originalRequirements[j], changes[i].newValue()});
					else
						// case where a requirement is removed
						rrr.add(new IRequirement[] {originalRequirements[j], null});
					originalRequirements[j] = null;
				}
				//				break;
			}
			if (!found && changes[i].applyOn() == null && changes[i].newValue() != null) //Case where a new requirement is added
				rrr.add(new IRequirement[] {null, changes[i].newValue()});
		}
		//Add all the unmodified requirements to the result
		for (int i = 0; i < originalRequirements.length; i++) {
			if (originalRequirements[i] != null)
				rrr.add(new IRequirement[] {originalRequirements[i], originalRequirements[i]});
		}
		return (IRequirement[][]) rrr.toArray(new IRequirement[rrr.size()][]);
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
		AbstractVariable noop = getNoOperationVariable(iu);
		for (Iterator i = optionalRequirements.iterator(); i.hasNext();) {
			AbstractVariable abs = (AbstractVariable) i.next();
			createIncompatibleValues(abs, noop);
		}
		optionalRequirements.add(noop);
		createImplication(iu, optionalRequirements, Explanation.OPTIONAL_REQUIREMENT);
	}

	//This will create as many implication as there is element in the right argument
	private void createNegationImplication(Object left, List right, Explanation name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + left + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		for (Iterator iterator = right.iterator(); iterator.hasNext();) {
			dependencyHelper.implication(new Object[] {left}).impliesNot(iterator.next()).named(name);
		}

	}

	private void createImplication(Object left, List right, Explanation name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + left + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		dependencyHelper.implication(new Object[] {left}).implies(right.toArray()).named(name);
	}

	private void createImplication(Object[] left, List right, Explanation name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + Arrays.asList(left) + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		dependencyHelper.implication(left).implies(right.toArray()).named(name);
	}

	//Return IUPatches that are applicable for the given iu
	private Collector getApplicablePatches(IInstallableUnit iu) {
		if (patches == null)
			patches = new QueryableArray((IInstallableUnit[]) picker.query(new PatchQuery(), null).toArray(IInstallableUnit.class));

		return patches.query(new ApplicablePatchQuery(iu), null);
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
					singletons.add(iu);
				} else {
					nonSingletons.add(iu);
				}
			}
			if (singletons.isEmpty())
				continue;

			IInstallableUnit[] singletonArray;
			if (nonSingletons.isEmpty()) {
				singletonArray = (IInstallableUnit[]) singletons.toArray(new IInstallableUnit[singletons.size()]);
				createAtMostOne(singletonArray);
			} else {
				singletonArray = (IInstallableUnit[]) singletons.toArray(new IInstallableUnit[singletons.size() + 1]);
				for (Iterator iterator2 = nonSingletons.iterator(); iterator2.hasNext();) {
					singletonArray[singletonArray.length - 1] = (IInstallableUnit) iterator2.next();
					createAtMostOne(singletonArray);
				}
			}
		}
	}

	private void createAtMostOne(IInstallableUnit[] ius) throws ContradictionException {
		if (DEBUG) {
			StringBuffer b = new StringBuffer();
			for (int i = 0; i < ius.length; i++) {
				b.append(ius[i].toString());
			}
			Tracing.debug("At most 1 of " + b); //$NON-NLS-1$
		}
		dependencyHelper.atMost(1, ius).named(new Explanation.Singleton(ius));
	}

	private void createIncompatibleValues(AbstractVariable v1, AbstractVariable v2) throws ContradictionException {
		AbstractVariable[] vars = {v1, v2};
		if (DEBUG) {
			StringBuffer b = new StringBuffer();
			for (int i = 0; i < vars.length; i++) {
				b.append(vars[i].toString());
			}
			Tracing.debug("At most 1 of " + b); //$NON-NLS-1$
		}
		dependencyHelper.atMost(1, vars).named(Explanation.OPTIONAL_REQUIREMENT);
	}

	private AbstractVariable getAbstractVariable() {
		AbstractVariable abstractVariable = new AbstractVariable();
		abstractVariables.add(abstractVariable);
		return abstractVariable;
	}

	private AbstractVariable getNoOperationVariable(IInstallableUnit iu) {
		AbstractVariable v = (AbstractVariable) noopVariables.get(iu);
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
			if (var instanceof IInstallableUnit) {
				IInstallableUnit iu = (IInstallableUnit) var;
				if (iu == entryPoint)
					continue;
				solution.add(iu);
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

	public Set getExplanation(IProgressMonitor monitor) {
		ExplanationJob job = new ExplanationJob();
		job.schedule();
		monitor.setTaskName(Messages.Planner_NoSolution);
		IProgressMonitor pm = new InfiniteProgress(monitor);
		pm.beginTask(Messages.Planner_NoSolution, 1000);
		try {
			synchronized (job) {
				while (job.getExplanationResult() == null && job.getState() != Job.NONE) {
					if (monitor.isCanceled()) {
						job.cancel();
						throw new OperationCanceledException();
					}
					pm.worked(1);
					try {
						job.wait(100);
					} catch (InterruptedException e) {
						if (DEBUG)
							Tracing.debug("Interrupted while computing explanations"); //$NON-NLS-1$
					}
				}
			}
		} finally {
			monitor.done();
		}
		return job.getExplanationResult();
	}

	public Map getFragmentAssociation() {
		Map resolvedFragments = new HashMap(fragments.size());
		for (Iterator iterator = fragments.entrySet().iterator(); iterator.hasNext();) {
			Entry fragment = (Entry) iterator.next();
			if (!dependencyHelper.getBooleanValueFor(fragment.getKey()))
				continue;
			Set potentialHosts = (Set) fragment.getValue();
			List resolvedHost = new ArrayList(potentialHosts.size());
			for (Iterator iterator2 = potentialHosts.iterator(); iterator2.hasNext();) {
				Object host = iterator2.next();
				if (dependencyHelper.getBooleanValueFor(host))
					resolvedHost.add(host);
			}
			if (resolvedHost.size() != 0)
				resolvedFragments.put(fragment.getKey(), resolvedHost);
		}
		return resolvedFragments;
	}

	private void rememberHostMatches(IInstallableUnit fragment, List matches) {
		Set existingMatches = (Set) fragments.get(fragment);
		if (existingMatches == null) {
			existingMatches = new HashSet();
			fragments.put(fragment, existingMatches);
			existingMatches.addAll(matches);
		}
		existingMatches.retainAll(matches);
	}

	private boolean isHostRequirement(IInstallableUnit iu, IRequirement req) {
		if (!(iu instanceof IInstallableUnitFragment))
			return false;
		IInstallableUnitFragment fragment = (IInstallableUnitFragment) iu;
		IRequirement[] reqs = fragment.getHost();
		for (int i = 0; i < reqs.length; i++) {
			if (req.equals(reqs[i]))
				return true;
		}
		return false;
	}

}