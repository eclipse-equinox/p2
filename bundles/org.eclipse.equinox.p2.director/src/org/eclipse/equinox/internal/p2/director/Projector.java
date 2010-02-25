/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 * Daniel Le Berre - Fix in the encoding and the optimization function
 * Alban Browaeys - Optimized string concatenation in bug 251357
 * Jed Anderson - switch from opb files to API calls to DependencyHelper in bug 200380
 *     Sonatype, Inc. - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Filter;
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
	private IQueryable<IInstallableUnit> picker;
	private QueryableArray patches;

	private Map<IInstallableUnit, AbstractVariable> noopVariables; //key IU, value AbstractVariable
	private List<AbstractVariable> abstractVariables;

	private Map<String, Map<Version, IInstallableUnit>> slice; //The IUs that have been considered to be part of the problem

	private Dictionary<String, String> selectionContext;

	DependencyHelper<Object, Explanation> dependencyHelper;
	private Collection<IInstallableUnit> solution;
	private Collection<Object> assumptions;

	private MultiStatus result;

	private Collection<IInstallableUnit> alreadyInstalledIUs;
	private IQueryable<IInstallableUnit> lastState;

	private boolean considerMetaRequirements;
	private IInstallableUnit entryPoint;
	private Map<IInstallableUnitFragment, Set<IInstallableUnit>> fragments = new HashMap<IInstallableUnitFragment, Set<IInstallableUnit>>();

	private int numberOfInstalledIUs;

	static class AbstractVariable {
		public String toString() {
			return "AbstractVariable: " + hashCode(); //$NON-NLS-1$
		}
	}

	/**
	 * Job for computing SAT failure explanation in the background.
	 */
	class ExplanationJob extends Job {
		private Set<Explanation> explanation;

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

		public Set<Explanation> getExplanationResult() {
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
					for (Explanation ex : explanation) {
						Tracing.debug(ex.toString());
					}
				}
			} catch (TimeoutException e) {
				if (DEBUG)
					Tracing.debug("Timeout while computing explanations"); //$NON-NLS-1$
			} finally {
				//must never have a null result, because caller is waiting on result to be non-null
				if (explanation == null)
					explanation = CollectionUtils.emptySet();
			}
			synchronized (this) {
				ExplanationJob.this.notify();
			}
			return Status.OK_STATUS;
		}

	}

	public Projector(IQueryable<IInstallableUnit> q, Dictionary<String, String> context, boolean considerMetaRequirements) {
		picker = q;
		noopVariables = new HashMap<IInstallableUnit, AbstractVariable>();
		slice = new HashMap<String, Map<Version, IInstallableUnit>>();
		selectionContext = context;
		abstractVariables = new ArrayList<AbstractVariable>();
		result = new MultiStatus(DirectorActivator.PI_DIRECTOR, IStatus.OK, Messages.Planner_Problems_resolving_plan, null);
		assumptions = new ArrayList<Object>();
		this.considerMetaRequirements = considerMetaRequirements;
	}

	protected boolean isInstalled(IInstallableUnit iu) {
		return !lastState.query(QueryUtil.createIUQuery(iu), null).isEmpty();
	}

	public void encode(IInstallableUnit entryPointIU, IInstallableUnit[] alreadyExistingRoots, IQueryable<IInstallableUnit> installedIUs, Collection<IInstallableUnit> newRoots, IProgressMonitor monitor) {
		alreadyInstalledIUs = Arrays.asList(alreadyExistingRoots);
		numberOfInstalledIUs = sizeOf(installedIUs);
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
			IQueryResult<IInstallableUnit> queryResult = picker.query(QueryUtil.createIUAnyQuery(), null);
			if (DEBUG_ENCODING) {
				dependencyHelper = new DependencyHelper<Object, Explanation>(solver, false);
			} else {
				dependencyHelper = new DependencyHelper<Object, Explanation>(solver);
			}
			Iterator<IInstallableUnit> iusToEncode = queryResult.iterator();
			List<IInstallableUnit> iusToOrder = new ArrayList<IInstallableUnit>();
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
				IInstallableUnit iuToEncode = iusToEncode.next();
				if (iuToEncode != entryPointIU) {
					processIU(iuToEncode, false);
				}
			}
			createConstraintsForSingleton();

			createMustHave(entryPointIU, alreadyExistingRoots);

			createOptimizationFunction(entryPointIU, newRoots);
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

	/**
	 * Efficiently compute the size of a queryable
	 */
	private int sizeOf(IQueryable<IInstallableUnit> installedIUs) {
		IQueryResult<IInstallableUnit> qr = installedIUs.query(QueryUtil.createIUAnyQuery(), null);
		if (qr instanceof Collector<?>)
			return ((Collector<?>) qr).size();
		return qr.unmodifiableSet().size();
	}

	//Create an optimization function favoring the highest version of each IU
	private void createOptimizationFunction(IInstallableUnit metaIu, Collection<IInstallableUnit> newRoots) {

		List<WeightedObject<? extends Object>> weightedObjects = new ArrayList<WeightedObject<? extends Object>>();

		Set<Entry<String, Map<Version, IInstallableUnit>>> s = slice.entrySet();
		final BigInteger POWER = BigInteger.valueOf(numberOfInstalledIUs > 0 ? numberOfInstalledIUs + 1 : 2);

		BigInteger maxWeight = POWER;
		for (Entry<String, Map<Version, IInstallableUnit>> entry : s) {
			Map<Version, IInstallableUnit> conflictingEntries = entry.getValue();
			if (conflictingEntries.size() == 1) {
				continue;
			}
			List<IInstallableUnit> toSort = new ArrayList<IInstallableUnit>(conflictingEntries.values());
			Collections.sort(toSort, Collections.reverseOrder());
			BigInteger weight = POWER;
			int count = toSort.size();
			boolean installedIuMet = false;
			boolean rootedMet = false;
			for (int i = 0; i < count; i++) {
				IInstallableUnit iu = toSort.get(i);
				if (!rootedMet && isInstalled(iu)) {
					installedIuMet = true;
					weightedObjects.add(WeightedObject.newWO(iu, BigInteger.ONE));
				} else if (!installedIuMet && !rootedMet && isRoot(iu, newRoots)) {
					rootedMet = true;
					weightedObjects.add(WeightedObject.newWO(iu, BigInteger.ONE));
				} else {
					weightedObjects.add(WeightedObject.newWO(iu, weight));
				}
				weight = weight.multiply(POWER);
			}
			if (weight.compareTo(maxWeight) > 0)
				maxWeight = weight;
		}

		// no need to add one here, since maxWeight is strickly greater than the
		// maximal weight used so far.
		maxWeight = maxWeight.multiply(POWER);

		// Add the abstract variables
		BigInteger abstractWeight = maxWeight.negate();
		for (AbstractVariable var : abstractVariables) {
			weightedObjects.add(WeightedObject.newWO(var, abstractWeight));
		}

		maxWeight = maxWeight.multiply(POWER).add(BigInteger.ONE);

		BigInteger optionalWeight = maxWeight.negate();
		long countOptional = 1;
		List<IInstallableUnit> requestedPatches = new ArrayList<IInstallableUnit>();
		Collection<IRequirement> reqs = metaIu.getRequiredCapabilities();
		for (IRequirement req : reqs) {
			if (req.getMin() > 0)
				continue;
			IQueryResult<IInstallableUnit> matches = picker.query(QueryUtil.createMatchQuery(req.getMatches()), null);
			for (Iterator<IInstallableUnit> iterator = matches.iterator(); iterator.hasNext();) {
				IInstallableUnit match = iterator.next();
				if (match instanceof IInstallableUnitPatch) {
					requestedPatches.add(match);
					countOptional = countOptional + 1;
				} else {
					weightedObjects.add(WeightedObject.newWO(match, optionalWeight));
				}
			}
		}

		BigInteger patchWeight = maxWeight.multiply(POWER).multiply(BigInteger.valueOf(countOptional)).negate();
		for (Iterator<IInstallableUnit> iterator = requestedPatches.iterator(); iterator.hasNext();) {
			weightedObjects.add(WeightedObject.newWO(iterator.next(), patchWeight));
		}
		if (!weightedObjects.isEmpty()) {
			createObjectiveFunction(weightedObjects);
		}
	}

	private boolean isRoot(IInstallableUnit iu, Collection<IInstallableUnit> newRoots) {
		return newRoots.contains(iu);
	}

	private void createObjectiveFunction(List<WeightedObject<? extends Object>> weightedObjects) {
		if (DEBUG) {
			StringBuffer b = new StringBuffer();
			for (WeightedObject<? extends Object> object : weightedObjects) {
				if (b.length() > 0)
					b.append(", "); //$NON-NLS-1$
				b.append(object.getWeight());
				b.append(' ');
				b.append(object.thing);
			}
			Tracing.debug("objective function: " + b); //$NON-NLS-1$
		}
		@SuppressWarnings("unchecked")
		WeightedObject<Object>[] array = (WeightedObject<Object>[]) weightedObjects.toArray(new WeightedObject<?>[weightedObjects.size()]);
		dependencyHelper.setObjectiveFunction(array);
	}

	private void createMustHave(IInstallableUnit iu, IInstallableUnit[] alreadyExistingRoots) throws ContradictionException {
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
		Filter filter = req.getFilter();
		return filter == null || filter.match(selectionContext);
	}

	private boolean isApplicable(IInstallableUnit iu) {
		Filter filter = iu.getFilter();
		return filter == null || filter.match(selectionContext);
	}

	private void expandNegatedRequirement(IRequirement req, IInstallableUnit iu, List<AbstractVariable> optionalAbstractRequirements, boolean isRootIu) throws ContradictionException {
		if (!isApplicable(req))
			return;
		List<IInstallableUnit> matches = getApplicableMatches(req);
		if (matches.isEmpty()) {
			return;
		}
		Explanation explanation;
		if (isRootIu) {
			IInstallableUnit reqIu = matches.get(0);
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

	private void expandRequirement(IRequirement req, IInstallableUnit iu, List<AbstractVariable> optionalAbstractRequirements, boolean isRootIu) throws ContradictionException {
		if (req.getMax() == 0) {
			expandNegatedRequirement(req, iu, optionalAbstractRequirements, isRootIu);
			return;
		}
		if (!isApplicable(req))
			return;
		List<IInstallableUnit> matches = getApplicableMatches(req);
		if (isHostRequirement(iu, req)) {
			rememberHostMatches((IInstallableUnitFragment) iu, matches);
		}
		if (req.getMin() > 0) {
			if (matches.isEmpty()) {
				missingRequirement(iu, req);
			} else {
				IInstallableUnit reqIu = matches.get(0);
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

	private void expandRequirements(Collection<IRequirement> reqs, IInstallableUnit iu, boolean isRootIu) throws ContradictionException {
		if (reqs.isEmpty())
			return;
		List<AbstractVariable> optionalAbstractRequirements = new ArrayList<AbstractVariable>();
		for (IRequirement req : reqs) {
			expandRequirement(req, iu, optionalAbstractRequirements, isRootIu);
		}
		createOptionalityExpression(iu, optionalAbstractRequirements);
	}

	public void processIU(IInstallableUnit iu, boolean isRootIU) throws ContradictionException {
		iu = iu.unresolved();
		Map<Version, IInstallableUnit> iuSlice = slice.get(iu.getId());
		if (iuSlice == null) {
			iuSlice = new HashMap<Version, IInstallableUnit>();
			slice.put(iu.getId(), iuSlice);
		}
		iuSlice.put(iu.getVersion(), iu);
		if (!isApplicable(iu)) {
			createNegation(iu, null);
			return;
		}

		IQueryResult<IInstallableUnit> applicablePatches = getApplicablePatches(iu);
		expandLifeCycle(iu, isRootIU);
		//No patches apply, normal code path
		if (applicablePatches.isEmpty()) {
			expandRequirements(getRequiredCapabilities(iu), iu, isRootIU);
		} else {
			//Patches are applicable to the IU
			expandRequirementsWithPatches(iu, applicablePatches, isRootIU);
		}
	}

	private Collection<IRequirement> getRequiredCapabilities(IInstallableUnit iu) {
		Collection<IRequirement> rqs = iu.getRequiredCapabilities();
		if (!considerMetaRequirements)
			return rqs;

		Collection<IRequirement> metaRqs = iu.getMetaRequiredCapabilities();
		if (metaRqs.isEmpty())
			return rqs;

		ArrayList<IRequirement> aggregatedRqs = new ArrayList<IRequirement>(rqs.size() + metaRqs.size());
		aggregatedRqs.addAll(rqs);
		aggregatedRqs.addAll(metaRqs);
		return aggregatedRqs;
	}

	static final class Pending {
		List<IInstallableUnit> matches;
		Explanation explanation;
		Object left;
	}

	private void expandRequirementsWithPatches(IInstallableUnit iu, IQueryResult<IInstallableUnit> applicablePatches, boolean isRootIu) throws ContradictionException {
		//Unmodified dependencies
		Collection<IRequirement> iuRequirements = getRequiredCapabilities(iu);
		Map<IRequirement, List<IInstallableUnitPatch>> unchangedRequirements = new HashMap<IRequirement, List<IInstallableUnitPatch>>(iuRequirements.size());
		Map<IRequirement, Pending> nonPatchedRequirements = new HashMap<IRequirement, Pending>(iuRequirements.size());
		for (Iterator<IInstallableUnit> iterator = applicablePatches.iterator(); iterator.hasNext();) {
			IInstallableUnitPatch patch = (IInstallableUnitPatch) iterator.next();
			IRequirement[][] reqs = mergeRequirements(iu, patch);
			if (reqs.length == 0)
				return;

			// Optional requirements are encoded via:
			// ABS -> (match1(req) or match2(req) or ... or matchN(req))
			// noop(IU)-> ~ABS
			// IU -> (noop(IU) or ABS)
			// Therefore we only need one optional requirement statement per IU
			List<AbstractVariable> optionalAbstractRequirements = new ArrayList<AbstractVariable>();
			for (int i = 0; i < reqs.length; i++) {
				//The requirement is unchanged
				if (reqs[i][0] == reqs[i][1]) {
					if (!isApplicable(reqs[i][0]))
						continue;

					List<IInstallableUnitPatch> patchesAppliedElseWhere = unchangedRequirements.get(reqs[i][0]);
					if (patchesAppliedElseWhere == null) {
						patchesAppliedElseWhere = new ArrayList<IInstallableUnitPatch>();
						unchangedRequirements.put(reqs[i][0], patchesAppliedElseWhere);
					}
					patchesAppliedElseWhere.add(patch);
					continue;
				}

				//Generate dependency when the patch is applied
				//P1 -> (A -> D) equiv. (P1 & A) -> D
				if (isApplicable(reqs[i][1])) {
					IRequirement req = reqs[i][1];
					List<IInstallableUnit> matches = getApplicableMatches(req);
					if (isHostRequirement(iu, req)) {
						rememberHostMatches((IInstallableUnitFragment) iu, matches);
					}
					if (req.getMin() > 0) {
						if (matches.isEmpty()) {
							missingRequirement(patch, req);
						} else {
							IInstallableUnit reqIu = matches.get(0);
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
					Pending pending = nonPatchedRequirements.get(req);
					if (pending != null) {
						pending.matches.add(patch);
						continue;
					}

					List<IInstallableUnit> matches = getApplicableMatches(req);
					if (isHostRequirement(iu, req)) {
						rememberHostMatches((IInstallableUnitFragment) iu, matches);
					}
					if (req.getMin() > 0) {
						if (matches.isEmpty()) {
							dependencyHelper.implication(new Object[] {iu}).implies(patch).named(new Explanation.HardRequirement(iu, null));
						} else {
							matches.add(patch);
							IInstallableUnit reqIu = matches.get(0);///(IInstallableUnit) picker.query(new CapabilityQuery(req), new Collector(), null).iterator().next();

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
		for (Pending pending : nonPatchedRequirements.values()) {
			createImplication(pending.left, pending.matches, pending.explanation);
		}

		List<AbstractVariable> optionalAbstractRequirements = new ArrayList<AbstractVariable>();
		for (Entry<IRequirement, List<IInstallableUnitPatch>> entry : unchangedRequirements.entrySet()) {
			List<IInstallableUnitPatch> patchesApplied = entry.getValue();
			Iterator<IInstallableUnit> allPatches = applicablePatches.iterator();
			List<IInstallableUnitPatch> requiredPatches = new ArrayList<IInstallableUnitPatch>();
			while (allPatches.hasNext()) {
				IInstallableUnitPatch patch = (IInstallableUnitPatch) allPatches.next();
				if (!patchesApplied.contains(patch))
					requiredPatches.add(patch);
			}
			IRequirement req = entry.getKey();
			List<IInstallableUnit> matches = getApplicableMatches(req);
			if (isHostRequirement(iu, req)) {
				rememberHostMatches((IInstallableUnitFragment) iu, matches);
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
					IInstallableUnit reqIu = matches.get(0);//(IInstallableUnit) picker.query(new CapabilityQuery(req), new Collector(), null).iterator().next();
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
		expandRequirement(req, iu, CollectionUtils.<AbstractVariable> emptyList(), isRootIu);
	}

	private void missingRequirement(IInstallableUnit iu, IRequirement req) throws ContradictionException {
		result.add(new Status(IStatus.WARNING, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Unsatisfied_dependency, iu, req)));
		createNegation(iu, req);
	}

	/**
	 * @param req
	 * @return a list of mandatory requirements if any, an empty list if req.isOptional().
	 */
	private List<IInstallableUnit> getApplicableMatches(IRequirement req) {
		List<IInstallableUnit> target = new ArrayList<IInstallableUnit>();
		IQueryResult<IInstallableUnit> matches = picker.query(QueryUtil.createMatchQuery(req.getMatches()), null);
		for (Iterator<IInstallableUnit> iterator = matches.iterator(); iterator.hasNext();) {
			IInstallableUnit match = iterator.next();
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
		List<IRequirementChange> changes = patch.getRequirementsChange();
		Collection<IRequirement> iuRequirements = iu.getRequiredCapabilities();
		IRequirement[] originalRequirements = iuRequirements.toArray(new IRequirement[iuRequirements.size()]);
		List<IRequirement[]> rrr = new ArrayList<IRequirement[]>();
		boolean found = false;
		for (int i = 0; i < changes.size(); i++) {
			IRequirementChange change = changes.get(i);
			for (int j = 0; j < originalRequirements.length; j++) {
				if (originalRequirements[j] != null && change.matches((IRequiredCapability) originalRequirements[j])) {
					found = true;
					if (change.newValue() != null)
						rrr.add(new IRequirement[] {originalRequirements[j], change.newValue()});
					else
						// case where a requirement is removed
						rrr.add(new IRequirement[] {originalRequirements[j], null});
					originalRequirements[j] = null;
				}
				//				break;
			}
			if (!found && change.applyOn() == null && change.newValue() != null) //Case where a new requirement is added
				rrr.add(new IRequirement[] {null, change.newValue()});
		}
		//Add all the unmodified requirements to the result
		for (int i = 0; i < originalRequirements.length; i++) {
			if (originalRequirements[i] != null)
				rrr.add(new IRequirement[] {originalRequirements[i], originalRequirements[i]});
		}
		return rrr.toArray(new IRequirement[rrr.size()][]);
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
	private void createOptionalityExpression(IInstallableUnit iu, List<AbstractVariable> optionalRequirements) throws ContradictionException {
		if (optionalRequirements.isEmpty())
			return;
		AbstractVariable noop = getNoOperationVariable(iu);
		for (AbstractVariable abs : optionalRequirements) {
			createIncompatibleValues(abs, noop);
		}
		optionalRequirements.add(noop);
		createImplication(iu, optionalRequirements, Explanation.OPTIONAL_REQUIREMENT);
	}

	//This will create as many implication as there is element in the right argument
	private void createNegationImplication(Object left, List<?> right, Explanation name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + left + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		for (Object r : right)
			dependencyHelper.implication(new Object[] {left}).impliesNot(r).named(name);
	}

	private void createImplication(Object left, List<?> right, Explanation name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + left + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		dependencyHelper.implication(new Object[] {left}).implies(right.toArray()).named(name);
	}

	private void createImplication(Object[] left, List<?> right, Explanation name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + Arrays.asList(left) + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		dependencyHelper.implication(left).implies(right.toArray()).named(name);
	}

	//Return IUPatches that are applicable for the given iu
	private IQueryResult<IInstallableUnit> getApplicablePatches(IInstallableUnit iu) {
		if (patches == null)
			patches = new QueryableArray(picker.query(QueryUtil.createIUPatchQuery(), null).toArray(IInstallableUnit.class));

		return patches.query(new ApplicablePatchQuery(iu), null);
	}

	//Create constraints to deal with singleton
	//When there is a mix of singleton and non singleton, several constraints are generated
	private void createConstraintsForSingleton() throws ContradictionException {
		Set<Entry<String, Map<Version, IInstallableUnit>>> s = slice.entrySet();
		for (Entry<String, Map<Version, IInstallableUnit>> entry : s) {
			Map<Version, IInstallableUnit> conflictingEntries = entry.getValue();
			if (conflictingEntries.size() < 2)
				continue;

			Collection<IInstallableUnit> conflictingVersions = conflictingEntries.values();
			List<IInstallableUnit> singletons = new ArrayList<IInstallableUnit>();
			List<IInstallableUnit> nonSingletons = new ArrayList<IInstallableUnit>();
			for (IInstallableUnit iu : conflictingVersions) {
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
				singletonArray = singletons.toArray(new IInstallableUnit[singletons.size()]);
				createAtMostOne(singletonArray);
			} else {
				singletonArray = singletons.toArray(new IInstallableUnit[singletons.size() + 1]);
				for (IInstallableUnit nonSingleton : nonSingletons) {
					singletonArray[singletonArray.length - 1] = nonSingleton;
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
		dependencyHelper.atMost(1, (Object[]) ius).named(new Explanation.Singleton(ius));
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
		dependencyHelper.atMost(1, (Object[]) vars).named(Explanation.OPTIONAL_REQUIREMENT);
	}

	private AbstractVariable getAbstractVariable() {
		AbstractVariable abstractVariable = new AbstractVariable();
		abstractVariables.add(abstractVariable);
		return abstractVariable;
	}

	private AbstractVariable getNoOperationVariable(IInstallableUnit iu) {
		AbstractVariable v = noopVariables.get(iu);
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
		solution = new ArrayList<IInstallableUnit>();
		IVec<Object> sat4jSolution = dependencyHelper.getSolution();
		for (Iterator<Object> iter = sat4jSolution.iterator(); iter.hasNext();) {
			Object var = iter.next();
			if (var instanceof IInstallableUnit) {
				IInstallableUnit iu = (IInstallableUnit) var;
				if (iu == entryPoint)
					continue;
				solution.add(iu);
			}
		}
	}

	private void printSolution(Collection<IInstallableUnit> state) {
		ArrayList<IInstallableUnit> l = new ArrayList<IInstallableUnit>(state);
		Collections.sort(l);
		Tracing.debug("Solution:"); //$NON-NLS-1$
		Tracing.debug("Numbers of IUs selected: " + l.size()); //$NON-NLS-1$
		for (IInstallableUnit s : l) {
			Tracing.debug(s.toString());
		}
	}

	public Collection<IInstallableUnit> extractSolution() {
		if (DEBUG)
			printSolution(solution);
		return solution;
	}

	public Set<Explanation> getExplanation(IProgressMonitor monitor) {
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

	public Map<IInstallableUnitFragment, List<IInstallableUnit>> getFragmentAssociation() {
		Map<IInstallableUnitFragment, List<IInstallableUnit>> resolvedFragments = new HashMap<IInstallableUnitFragment, List<IInstallableUnit>>(fragments.size());
		for (Entry<IInstallableUnitFragment, Set<IInstallableUnit>> fragment : fragments.entrySet()) {
			if (!dependencyHelper.getBooleanValueFor(fragment.getKey()))
				continue;
			Set<IInstallableUnit> potentialHosts = fragment.getValue();
			List<IInstallableUnit> resolvedHost = new ArrayList<IInstallableUnit>(potentialHosts.size());
			for (IInstallableUnit host : potentialHosts) {
				if (dependencyHelper.getBooleanValueFor(host))
					resolvedHost.add(host);
			}
			if (resolvedHost.size() != 0)
				resolvedFragments.put(fragment.getKey(), resolvedHost);
		}
		return resolvedFragments;
	}

	private void rememberHostMatches(IInstallableUnitFragment fragment, List<IInstallableUnit> matches) {
		Set<IInstallableUnit> existingMatches = fragments.get(fragment);
		if (existingMatches == null) {
			existingMatches = new HashSet<IInstallableUnit>();
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