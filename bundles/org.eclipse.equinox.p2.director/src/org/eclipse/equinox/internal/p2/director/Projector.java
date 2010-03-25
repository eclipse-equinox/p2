/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 * 	Daniel Le Berre - Fix in the encoding and the optimization function
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.io.*;
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
import org.sat4j.pb.reader.OPBEclipseReader2007;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.*;

/**
 * This class is the interface between SAT4J and the planner. It produces a
 * boolean satisfiability problem, invokes the solver, and converts the solver result
 * back into information understandable by the planner.
 */
public class Projector implements IProjector {
	private static boolean DEBUG = Tracing.DEBUG_PLANNER_PROJECTOR;
	private IQueryable picker;

	private Map variables; //key IU, value corresponding variable in the problem
	private Map noopVariables; //key IU, value corresponding no optionality variable in the problem, 
	private List abstractVariables;

	private TwoTierMap slice; //The IUs that have been considered to be part of the problem

	private Dictionary selectionContext;

	private int varCount = 1;

	private ArrayList constraints;
	private ArrayList dependencies;
	private ArrayList tautologies;
	private StringBuffer objective;
	private StringBuffer explanation = new StringBuffer("explain: "); //$NON-NLS-1$
	private Collection solution;

	private File problemFile;
	private MultiStatus result;

	private int commentsCount = 0;

	public Projector(IQueryable q, Dictionary context) {
		picker = q;
		variables = new HashMap();
		noopVariables = new HashMap();
		slice = new TwoTierMap();
		constraints = new ArrayList();
		tautologies = new ArrayList();
		dependencies = new ArrayList();
		selectionContext = context;
		abstractVariables = new ArrayList();
		result = new MultiStatus(DirectorActivator.PI_DIRECTOR, IStatus.OK, Messages.Planner_Problems_resolving_plan, null);
	}

	public void encode(IInstallableUnit[] ius, IProgressMonitor monitor) {
		try {
			long start = 0;
			if (DEBUG) {
				start = System.currentTimeMillis();
				System.out.println("Start projection: " + start); //$NON-NLS-1$
			}

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
				processIU((IInstallableUnit) iusToEncode.next());
			}
			createConstraintsForSingleton();
			for (int i = 0; i < ius.length; i++) {
				createMustHaves(ius[i]);
			}
			createOptimizationFunction(ius);
			persist();
			if (DEBUG) {
				long stop = System.currentTimeMillis();
				System.out.println("Projection complete: " + (stop - start)); //$NON-NLS-1$
			}
		} catch (IllegalStateException e) {
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, e.getMessage(), e));
		}
	}

	// translates a -> -b into pseudo boolean
	private String impliesNo(String a, String b) {
		return "-1 " + a + " -1 " + b + ">= -1 ;"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private String implies(String a, String b) {
		return "-1 " + a + " +1 " + b + ">= -1 ;"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	//Create an optimization function favoring the highest version of each IU  
	private void createOptimizationFunction(IInstallableUnit[] ius) {
		final String MIN_STR = "min:"; //$NON-NLS-1$

		objective = new StringBuffer(MIN_STR);
		Set s = slice.entrySet();
		final int POWER = 2;

		long maxWeight = POWER;
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			HashMap conflictingEntries = (HashMap) entry.getValue();
			if (conflictingEntries.size() == 1) {
				continue;
			}
			List toSort = new ArrayList(conflictingEntries.values());
			Collections.sort(toSort, Collections.reverseOrder());
			long weight = POWER;
			int count = toSort.size();
			for (int i = 1; i < count; i++) {
				objective.append(' ').append(weight).append(' ').append(getVariable((IInstallableUnit) toSort.get(i)));
				weight *= POWER;
			}
			if (weight > maxWeight)
				maxWeight = weight;
		}

		maxWeight *= POWER;

		for (Iterator iterator = noopVariables.values().iterator(); iterator.hasNext();) {
			objective.append(' ').append(maxWeight).append(' ').append(iterator.next().toString());
		}

		maxWeight *= POWER;

		//Add the abstract variables
		for (Iterator iterator = abstractVariables.iterator(); iterator.hasNext();) {
			objective.append(" -").append(maxWeight).append(" ").append((String) iterator.next()); //$NON-NLS-1$ //$NON-NLS-2$
		}

		maxWeight *= POWER;
		objective.append(' ').append(getPatchesWeight(ius, maxWeight));

		if (MIN_STR.equals(objective.toString().trim())) {
			objective = new StringBuffer();
		} else {
			objective.append(" ;"); //$NON-NLS-1$
		}
	}

	protected StringBuffer getPatchesWeight(IInstallableUnit ius[], long optionalWeight) {
		StringBuffer weights = new StringBuffer();
		List requestedPatches = new ArrayList(ius.length);
		int optionalCount = 1;
		for (int i = 0; i < ius.length; i++) {
			RequiredCapability[] reqs = ius[i].getRequiredCapabilities();
			for (int j = 0; j < reqs.length; j++) {
				if (!reqs[j].isOptional())
					continue;

				Collector matches = picker.query(new CapabilityQuery(reqs[j]), new Collector(), null);
				for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
					IInstallableUnit match = (IInstallableUnit) iterator.next();
					if (match instanceof IInstallableUnitPatch)
						requestedPatches.add(match);
					else {
						weights.append('-').append(optionalWeight).append(' ').append(getVariable(match)).append(' ');
						optionalCount++;
					}
				}
			}
		}
		long patchesWeight = optionalWeight * 2 * optionalCount;
		for (Iterator iterator = requestedPatches.iterator(); iterator.hasNext();) {
			weights.append('-').append(patchesWeight).append(' ').append(getVariable((IInstallableUnit) iterator.next())).append(' ');
		}
		return weights;
	}

	private void createMustHaves(IInstallableUnit iu) {
		tautologies.add(" +1 " + getVariable(iu) + " = 1;"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void createNegation(IInstallableUnit iu) {
		createNegation(getVariable(iu));
	}

	private void createNegation(String var) {
		tautologies.add(" +1" + var + " = 0;"); //$NON-NLS-1$//$NON-NLS-2$
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

	//Write the problem generated into a temporary file
	private void persist() {
		try {
			problemFile = File.createTempFile("p2Encoding", ".opb"); //$NON-NLS-1$//$NON-NLS-2$
			BufferedWriter w = new BufferedWriter(new FileWriter(problemFile));
			int clauseCount = tautologies.size() + dependencies.size() + constraints.size() - commentsCount;

			w.write("* #variable= " + varCount + " #constraint= " + clauseCount + "  "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			w.newLine();
			w.write("*"); //$NON-NLS-1$
			w.newLine();
			displayMappingInComments(w);
			if (clauseCount == 0) {
				w.close();
				return;
			}
			w.write(objective.toString());
			w.newLine();
			w.newLine();
			w.write(explanation + " ;"); //$NON-NLS-1$
			w.newLine();
			w.newLine();

			for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
				w.write((String) iterator.next());
				w.newLine();
			}
			for (Iterator iterator = constraints.iterator(); iterator.hasNext();) {
				w.write((String) iterator.next());
				w.newLine();
			}
			for (Iterator iterator = tautologies.iterator(); iterator.hasNext();) {
				w.write((String) iterator.next());
				w.newLine();
			}
			w.close();
		} catch (IOException e) {
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Error_saving_opbfile, problemFile), e));
		}
	}

	private void displayMappingInComments(BufferedWriter w) throws IOException {
		if (!DEBUG)
			return;
		List vars = new ArrayList(variables.keySet());
		Collections.sort(vars);
		w.write("* IUs variables"); //$NON-NLS-1$
		w.newLine();
		w.write("* "); //$NON-NLS-1$
		w.newLine();
		Iterator iterator = vars.iterator();
		while (iterator.hasNext()) {
			w.write("* "); //$NON-NLS-1$
			Object key = iterator.next();
			w.write(key.toString());
			w.write("=>"); //$NON-NLS-1$
			w.write(variables.get(key).toString());
			w.newLine();
		}
		w.write("* "); //$NON-NLS-1$
		w.newLine();
		w.write("* Abstract variables"); //$NON-NLS-1$
		w.newLine();
		w.write("* "); //$NON-NLS-1$
		w.newLine();
		iterator = abstractVariables.iterator();
		w.write("* "); //$NON-NLS-1$
		while (iterator.hasNext()) {
			w.write(iterator.next().toString());
			w.write(' ');
		}
		w.newLine();
		w.write("* "); //$NON-NLS-1$
		w.newLine();
		w.write("* NoOp variables"); //$NON-NLS-1$
		w.newLine();
		w.write("* "); //$NON-NLS-1$
		w.newLine();
		iterator = noopVariables.keySet().iterator();
		while (iterator.hasNext()) {
			w.write("* "); //$NON-NLS-1$
			Object key = iterator.next();
			w.write(key.toString());
			w.write("=>"); //$NON-NLS-1$
			w.write(noopVariables.get(key).toString());
			w.newLine();
		}
		w.write("* "); //$NON-NLS-1$
		w.newLine();
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

	public void processIU(IInstallableUnit iu) {
		iu = iu.unresolved();

		slice.put(iu.getId(), iu.getVersion(), iu);
		explanation.append(" ").append(getVariable(iu)); //$NON-NLS-1$
		if (!isApplicable(iu)) {
			createNegation(iu);
			return;
		}

		Collector patches = getApplicablePatches(iu);
		expandLifeCycle(iu);
		//No patches apply, normal code path
		if (patches.size() == 0) {
			RequiredCapability[] reqs = iu.getRequiredCapabilities();
			if (reqs.length == 0) {
				return;
			}
			for (int i = 0; i < reqs.length; i++) {
				if (!isApplicable(reqs[i]))
					continue;

				expandRequirement(null, iu, reqs[i]);
			}
			addOptionalityExpression();
		} else {
			//Patches are applicable to the IU

			//Unmodified dependencies
			Map unchangedRequirements = new HashMap(iu.getRequiredCapabilities().length);
			for (Iterator iterator = patches.iterator(); iterator.hasNext();) {
				IInstallableUnitPatch patch = (IInstallableUnitPatch) iterator.next();
				RequiredCapability[][] reqs = mergeRequirements(iu, patch);
				if (reqs.length == 0)
					return;

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
					//P1 -> A -> D (equiv P1 & A -> D equiv -1 P1 -1 A + 1 B >= -1)
					if (isApplicable(reqs[i][1])) {
						genericExpandRequirement(" -1 " + getVariable(patch) + " -1 " + getVariable(iu), iu, reqs[i][1], " >= -1", " 1 " + getVariable(patch) + "=0;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
					}
					//Generate dependency when the patch is not applied
					//-P1 -> A -> B ( equiv. -P1 & A -> B equiv 1 P1 - 1 A + 1 B >= 0)
					if (isApplicable(reqs[i][0]))
						genericExpandRequirement(" 1 " + getVariable(patch) + " -1 " + getVariable(iu), iu, reqs[i][0], " >= 0", implies(getVariable(iu), getVariable(patch))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				addOptionalityExpression();
			}
			for (Iterator iterator = unchangedRequirements.entrySet().iterator(); iterator.hasNext();) {
				Entry entry = (Entry) iterator.next();
				StringBuffer expression = new StringBuffer();
				List patchesApplied = (List) entry.getValue();
				List allPatches = new ArrayList(patches.toCollection());
				allPatches.removeAll(patchesApplied);
				for (Iterator iterator2 = allPatches.iterator(); iterator2.hasNext();) {
					IInstallableUnitPatch patch = (IInstallableUnitPatch) iterator2.next();
					expression.append(" 1 " + getVariable(patch)); //$NON-NLS-1$
				}
				if (allPatches.size() != 0)
					genericExpandRequirement(expression.toString(), iu, (RequiredCapability) entry.getKey(), " >= 0", " 1 " + getVariable(iu) + "=0;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				else
					expandRequirement(null, iu, (RequiredCapability) entry.getKey());
			}
		}
	}

	private void expandLifeCycle(IInstallableUnit iu) {
		if (!(iu instanceof IInstallableUnitPatch))
			return;
		IInstallableUnitPatch patch = (IInstallableUnitPatch) iu;
		if (patch.getLifeCycle() == null)
			return;
		expandNormalRequirement(null, iu, patch.getLifeCycle());
	}

	private void genericExpandRequirement(String var, IInstallableUnit iu, RequiredCapability req, String value, String negationExpression) {
		if (req.isOptional())
			genericOptionalRequirementExpansion(var, iu, req, value);
		else
			genericRequirementExpansion(var, iu, req, value, negationExpression);
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

	private void addOptionalityExpression() {
		if (optionalityExpression != null && countOptionalIUs > 0)
			dependencies.add(optionalityExpression + " >= 0;"); //$NON-NLS-1$
		optionalityExpression = null;
		countOptionalIUs = 0;
	}

	private String optionalityExpression = null;
	private int countOptionalIUs = 0;
	private QueryableArray patches;

	private void expandOptionalRequirement(String iuVar, IInstallableUnit iu, RequiredCapability req) {
		if (iuVar == null)
			iuVar = getVariable(iu);
		String abstractVar = getAbstractVariable();
		String expression = " -1 " + abstractVar; //$NON-NLS-1$
		Collector matches = picker.query(new CapabilityQuery(req), new Collector(), null);
		if (optionalityExpression == null)
			optionalityExpression = " -1 " + iuVar + " 1 " + getNoOperationVariable(iu); //$NON-NLS-1$ //$NON-NLS-2$ 
		StringBuffer comment = new StringBuffer();
		if (DEBUG) {
			comment.append("* "); //$NON-NLS-1$
			comment.append(iu.toString());
			comment.append(" requires optionaly either "); //$NON-NLS-1$
		}
		int countMatches = 0;
		for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
			IInstallableUnit match = (IInstallableUnit) iterator.next();
			if (isApplicable(match)) {
				countMatches++;
				expression += " 1 " + getVariable(match); //$NON-NLS-1$
				if (DEBUG) {
					comment.append(match.toString());
					comment.append(' ');
				}
			}
		}
		countOptionalIUs += countMatches;
		if (countMatches > 0) {
			if (DEBUG) {
				dependencies.add(comment.toString());
				commentsCount++;
			}
			dependencies.add(impliesNo(getNoOperationVariable(iu), abstractVar));
			dependencies.add(expression + " >= 0;"); //$NON-NLS-1$
			optionalityExpression += " 1 " + abstractVar; //$NON-NLS-1$
		}

		if (DEBUG)
			System.out.println("No IU found to satisfy optional dependency of " + iu + " req " + req); //$NON-NLS-1$//$NON-NLS-2$
	}

	private void genericOptionalRequirementExpansion(String iuVar, IInstallableUnit iu, RequiredCapability req, String value) {
		String abstractVar = getAbstractVariable();
		String expression = iuVar;
		Collector matches = picker.query(new CapabilityQuery(req), new Collector(), null);
		if (optionalityExpression == null)
			optionalityExpression = " -1 " + getVariable(iu) + " 1 " + getNoOperationVariable(iu); //$NON-NLS-1$ //$NON-NLS-2$ 
		StringBuffer comment = new StringBuffer();
		if (DEBUG) {
			comment.append("* "); //$NON-NLS-1$
			comment.append(iu.toString());
			comment.append(" requires optionaly either "); //$NON-NLS-1$
		}
		int countMatches = 0;
		for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
			IInstallableUnit match = (IInstallableUnit) iterator.next();
			if (isApplicable(match)) {
				countMatches++;
				expression += " 1 " + getVariable(match); //$NON-NLS-1$
				if (DEBUG) {
					comment.append(match.toString());
					comment.append(' ');
				}
			}
		}
		countOptionalIUs += countMatches;
		if (countMatches > 0) {
			if (DEBUG) {
				dependencies.add(comment.toString());
				commentsCount++;
			}
			dependencies.add(impliesNo(getNoOperationVariable(iu), abstractVar));
			dependencies.add(expression + " " + value + ";"); //$NON-NLS-1$ //$NON-NLS-2$
			optionalityExpression += " 1 " + abstractVar; //$NON-NLS-1$
		}

		if (DEBUG)
			System.out.println("No IU found to satisfy optional dependency of " + iu + " req " + req); //$NON-NLS-1$//$NON-NLS-2$
	}

	private void genericRequirementExpansion(String varIu, IInstallableUnit iu, RequiredCapability req, String value, String negationExpression) {
		String expression = varIu;
		Collector matches = picker.query(new CapabilityQuery(req), new Collector(), null);
		StringBuffer comment = new StringBuffer();
		if (DEBUG) {
			comment.append("* "); //$NON-NLS-1$
			comment.append(iu.toString());
			comment.append(" requires either "); //$NON-NLS-1$
		}
		int countMatches = 0;
		for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
			IInstallableUnit match = (IInstallableUnit) iterator.next();
			if (isApplicable(match)) {
				countMatches++;
				expression += " +1 " + getVariable(match); //$NON-NLS-1$
				if (DEBUG) {
					comment.append(match.toString());
					comment.append(' ');
				}
			}
		}

		if (countMatches > 0) {
			if (DEBUG) {
				dependencies.add(comment.toString());
				commentsCount++;
			}
			dependencies.add(expression + " " + value + ";"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			result.add(new Status(IStatus.WARNING, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Unsatisfied_dependency, iu, req)));
			dependencies.add(negationExpression);
		}
	}

	private void expandNormalRequirement(String varIu, IInstallableUnit iu, RequiredCapability req) {
		//Generate the regular requirement
		if (varIu == null)
			varIu = getVariable(iu);
		String expression = "-1 " + varIu; //$NON-NLS-1$
		Collector matches = picker.query(new CapabilityQuery(req), new Collector(), null);
		StringBuffer comment = new StringBuffer();
		if (DEBUG) {
			comment.append("* "); //$NON-NLS-1$
			comment.append(iu.toString());
			comment.append(" requires either "); //$NON-NLS-1$
		}
		int countMatches = 0;
		for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
			IInstallableUnit match = (IInstallableUnit) iterator.next();
			if (isApplicable(match)) {
				countMatches++;
				expression += " +1 " + getVariable(match); //$NON-NLS-1$
				if (DEBUG) {
					comment.append(match.toString());
					comment.append(' ');
				}
			}
		}

		if (countMatches > 0) {
			if (DEBUG) {
				dependencies.add(comment.toString());
				commentsCount++;
			}
			dependencies.add(expression + " >= 0;"); //$NON-NLS-1$
		} else {
			result.add(new Status(IStatus.WARNING, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Unsatisfied_dependency, iu, req)));
			createNegation(varIu);
		}
	}

	//Return IUPatches that are applicable for the given iu
	private Collector getApplicablePatches(IInstallableUnit iu) {
		if (patches == null)
			patches = new QueryableArray((IInstallableUnit[]) picker.query(ApplicablePatchQuery.ANY, new Collector(), null).toArray(IInstallableUnit.class));

		return patches.query(new ApplicablePatchQuery(iu), new Collector(), null);
	}

	private void expandRequirement(String var, IInstallableUnit iu, RequiredCapability req) {
		if (req.isOptional())
			expandOptionalRequirement(var, iu, req);
		else
			expandNormalRequirement(var, iu, req);
	}

	//Create constraints to deal with singleton
	//When there is a mix of singleton and non singleton, several constraints are generated 
	private void createConstraintsForSingleton() {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			HashMap conflictingEntries = (HashMap) entry.getValue();
			if (conflictingEntries.size() < 2)
				continue;

			Collection conflictingVersions = conflictingEntries.values();
			String singletonRule = ""; //$NON-NLS-1$
			ArrayList nonSingleton = new ArrayList();
			int countSingleton = 0;
			for (Iterator conflictIterator = conflictingVersions.iterator(); conflictIterator.hasNext();) {
				IInstallableUnit conflictElt = (IInstallableUnit) conflictIterator.next();
				if (conflictElt.isSingleton()) {
					singletonRule += " -1 " + getVariable(conflictElt); //$NON-NLS-1$
					countSingleton++;
				} else {
					nonSingleton.add(conflictElt);
				}
			}
			if (countSingleton == 0)
				continue;

			for (Iterator iterator2 = nonSingleton.iterator(); iterator2.hasNext();) {
				constraints.add(singletonRule + " -1 " + getVariable((IInstallableUnit) iterator2.next()) + " >= -1;"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			singletonRule += " >= -1;"; //$NON-NLS-1$
			constraints.add(singletonRule);
		}
	}

	//Return the corresponding variable 
	private String getVariable(IInstallableUnit iu) {
		String v = (String) variables.get(iu);
		if (v == null) {
			v = new String("x" + varCount++); //$NON-NLS-1$
			variables.put(iu, v);
		}
		return v;
	}

	private String getAbstractVariable() {
		String newVar = new String("x" + varCount++); //$NON-NLS-1$
		abstractVariables.add(newVar);
		return newVar;
	}

	private String getNoOperationVariable(IInstallableUnit iu) {
		String v = (String) noopVariables.get(iu);
		if (v == null) {
			v = new String("x" + varCount++); //$NON-NLS-1$
			noopVariables.put(iu, v);
		}
		return v;
	}

	public IStatus invokeSolver(IProgressMonitor monitor) {
		if (result.getSeverity() == IStatus.ERROR)
			return result;
		boolean delete = true;
		IPBSolver solver = SolverFactory.newEclipseP2();
		solver.setTimeoutOnConflicts(1000);
		OPBEclipseReader2007 reader = new OPBEclipseReader2007(solver);
		// CNF filename is given on the command line 
		long start = System.currentTimeMillis();
		if (DEBUG)
			System.out.println("Invoking solver: " + start); //$NON-NLS-1$
		FileReader fr = null;
		try {
			fr = new FileReader(problemFile);
			IProblem problem = reader.parseInstance(fr);
			if (problem.isSatisfiable()) {
				//				problem.model();
				if (DEBUG) {
					System.out.println("Satisfiable !"); //$NON-NLS-1$
					System.out.println(reader.decode(problem.model()));
				}
				backToIU(problem);
				long stop = System.currentTimeMillis();
				if (DEBUG)
					System.out.println("Solver solution found: " + (stop - start)); //$NON-NLS-1$
			} else {
				if (DEBUG)
					System.out.println("Unsatisfiable !"); //$NON-NLS-1$
				result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Unsatisfiable_problem, problemFile)));
			}
		} catch (FileNotFoundException e) {
			delete = false;
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Missing_opb_file, problemFile)));
		} catch (ParseFormatException e) {
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Format_error, problemFile)));
		} catch (ContradictionException e) {
			result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Trivial_exception, problemFile)));
		} catch (TimeoutException e) {
			result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Timeout, problemFile)));
		} catch (Exception e) {
			delete = false;
			result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Unexpected_problem, problemFile), e));
		} finally {
			try {
				if (fr != null)
					fr.close();
			} catch (IOException e) {
				//ignore
			}
			if (delete)
				problemFile.delete();
		}
		return result;
	}

	private void backToIU(IProblem problem) {
		solution = new ArrayList();
		for (Iterator allIUs = variables.entrySet().iterator(); allIUs.hasNext();) {
			Entry entry = (Entry) allIUs.next();
			int match = Integer.parseInt(((String) entry.getValue()).substring(1));
			if (problem.model(match)) {
				solution.add(((IInstallableUnit) entry.getKey()).unresolved());
			}
		}
	}

	private void printSolution(Collection state) {
		ArrayList l = new ArrayList(state);
		Collections.sort(l);
		System.out.println("Numbers of IUs selected:" + l.size()); //$NON-NLS-1$
		for (Iterator iterator = l.iterator(); iterator.hasNext();) {
			System.out.println(iterator.next());
		}
	}

	public Collection extractSolution() {
		if (DEBUG)
			printSolution(solution);
		return solution;
	}
}
