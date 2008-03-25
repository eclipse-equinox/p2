/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.CapabilityQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.core.PBSolverCP;
import org.sat4j.pb.orders.VarOrderHeapObjective;
import org.sat4j.pb.reader.OPBEclipseReader2007;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.*;

/**
 * This class is the interface between SAT4J and the planner. It produces a
 * boolean satisfiability problem, invokes the solver, and converts the solver result
 * back into information understandable by the planner.
 */
public class Projector {
	private static boolean DEBUG = false;
	private IQueryable picker;

	private Map variables; //key IU, value corresponding variable in the problem
	private Map variableForSyntheticIUs; //key IU, value corresponding variable in the problem.

	private TwoTierMap slice; //The IUs that have been considered to be part of the problem

	private Dictionary selectionContext;

	private final static int shift = 1;

	private ArrayList constraints;
	private ArrayList dependencies;
	private ArrayList tautologies;
	private String objective;

	private Collection solution;

	private File problemFile;
	private MultiStatus result;

	public Projector(IQueryable q, Dictionary context) {
		picker = q;
		variables = new HashMap();
		variableForSyntheticIUs = new HashMap();
		slice = new TwoTierMap();
		constraints = new ArrayList();
		tautologies = new ArrayList();
		dependencies = new ArrayList();
		selectionContext = context;
		result = new MultiStatus(DirectorActivator.PI_DIRECTOR, IStatus.OK, "Problems resolving provisioning plan.", null);
	}

	public void encode(IInstallableUnit[] ius, IProgressMonitor monitor) {
		try {
			long start = 0;
			if (DEBUG) {
				start = System.currentTimeMillis();
				System.out.println("Start projection: " + start); //$NON-NLS-1$
			}

			Iterator iusToEncode = picker.query(InstallableUnitQuery.ANY, new Collector(), null).iterator();
			while (iusToEncode.hasNext()) {
				processIU((IInstallableUnit) iusToEncode.next(), true);
			}
			createConstraintsForSingleton();
			for (int i = 0; i < ius.length; i++) {
				createMustHaves(ius[i]);
			}
			createOptimizationFunction();
			persist();
			if (DEBUG) {
				long stop = System.currentTimeMillis();
				System.out.println("Projection complete: " + (stop - start)); //$NON-NLS-1$
			}
		} catch (IllegalStateException e) {
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, e.getMessage(), e));
		}
	}

	//Create an optimization function favoring the highest version of each IU  
	private void createOptimizationFunction() {
		objective = "min:"; //$NON-NLS-1$
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			HashMap conflictingEntries = (HashMap) entry.getValue();
			if (conflictingEntries.size() <= 1) {
				objective += " 1 " + getVariable((IInstallableUnit) conflictingEntries.values().iterator().next()); //$NON-NLS-1$
				continue;
			}
			List toSort = new ArrayList(conflictingEntries.values());
			Collections.sort(toSort);
			int weight = toSort.size();
			for (Iterator iterator2 = toSort.iterator(); iterator2.hasNext();) {
				objective += " " + weight-- + " " + getVariable((IInstallableUnit) iterator2.next()); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		objective += " ;"; //$NON-NLS-1$
	}

	private void createMustHaves(IInstallableUnit iu) {
		tautologies.add(" +1 " + getVariable(iu) + " = 1;"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void createNegation(IInstallableUnit iu) {
		tautologies.add(" +1" + getVariable(iu) + " = 0;"); //$NON-NLS-1$//$NON-NLS-2$
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
			int clauseCount = tautologies.size() + dependencies.size() + constraints.size();

			int variableCount = variables.size();
			w.write("* #variable= " + variableCount + " #constraint= " + clauseCount + "  "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			w.newLine();
			w.write("*"); //$NON-NLS-1$
			w.newLine();

			if (variableCount == 0 && clauseCount == 0) {
				w.close();
				return;
			}
			w.write(objective);
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
			e.printStackTrace();
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

	String explanation = "explain: "; //$NON-NLS-1$

	public void processIU(IInstallableUnit iu, boolean expandOptionalRequirements) {
		slice.put(iu.getId(), iu.getVersion(), iu);
		explanation += " " + getVariable(iu); //$NON-NLS-1$
		if (!isApplicable(iu)) {
			createNegation(iu);
			return;
		}

		RequiredCapability[] reqs = iu.getRequiredCapabilities();
		if (expandOptionalRequirements) {
			if (sortDependencies(iu)[OPT].size() != 0) {
				expandIUs(iu, sortDependencies(iu));
				return;
			}
		}
		if (reqs.length == 0) {
			return;
		}
		for (int i = 0; i < reqs.length; i++) {
			if (!isApplicable(reqs[i]))
				continue;

			try {
				expandRequirement(iu, reqs[i]);
			} catch (IllegalStateException ise) {
				result.add(new Status(IStatus.WARNING, DirectorActivator.PI_DIRECTOR, ise.getMessage(), ise));
				createNegation(iu);
			}
		}
	}

	private void expandIUs(IInstallableUnit iu, List[] list) {
		List combinationsOfRequirements = new ArrayList();
		getCombinations(list[CORE], list[OPT], combinationsOfRequirements);
		combinationsOfRequirements.add(list[CORE]);
		String expression = "-1 " + getVariable(iu); //$NON-NLS-1$
		int count = combinationsOfRequirements.size();
		String generatedIUId = iu.getId() + '-' + System.currentTimeMillis();
		for (Iterator iterator = combinationsOfRequirements.iterator(); iterator.hasNext();) {
			RequiredCapability[] reqs = (RequiredCapability[]) ((ArrayList) iterator.next()).toArray(new RequiredCapability[0]);

			InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
			iud.setId(generatedIUId);
			iud.setVersion(new Version(0, reqs.length, count--));
			iud.setRequiredCapabilities(reqs);
			iud.setSingleton(true);
			IInstallableUnit generated = MetadataFactory.createInstallableUnit(iud);

			processIU(generated, false);
			expression += " +1 " + getVariable(generated); //$NON-NLS-1$
			variableForSyntheticIUs.put(generated, getVariable(generated));
		}
		expression += ">= 0;"; //$NON-NLS-1$
		dependencies.add(expression);
	}

	private void getCombinations(List seed, List elts, List solutions) {
		if (elts.isEmpty()) {
			solutions.add(seed);
			return;
		}

		Object head = elts.get(0);
		ArrayList solutionElt = new ArrayList(seed);
		solutionElt.add(head);
		solutions.add(solutionElt);

		List tail = elts.subList(1, elts.size());
		if (!tail.isEmpty()) {
			getCombinations(seed, tail, solutions);
			ArrayList nextSeed = new ArrayList(seed);
			nextSeed.add(head);
			getCombinations(nextSeed, tail, solutions);
		}
	}

	final byte OPT = 0;
	final byte CORE = 1;

	private ArrayList[] sortDependencies(IInstallableUnit iu) {
		RequiredCapability[] reqs = iu.getRequiredCapabilities();
		ArrayList opt = new ArrayList(reqs.length);
		ArrayList nonOpt = new ArrayList(reqs.length);
		for (int i = 0; i < reqs.length; i++) {
			if (reqs[i].isOptional())
				opt.add(reqs[i]);
			else
				nonOpt.add(reqs[i]);
		}
		ArrayList[] sorted = new ArrayList[2];
		sorted[CORE] = nonOpt;
		sorted[OPT] = opt;
		return sorted;
	}

	private void expandRequirement(IInstallableUnit iu, RequiredCapability req) {
		String expression = "-1 " + getVariable(iu); //$NON-NLS-1$
		Collector matches = picker.query(new CapabilityQuery(req), new Collector(), null);

		int countMatches = 0;
		for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
			IInstallableUnit match = (IInstallableUnit) iterator.next();
			if (!isApplicable(match))
				continue;
			countMatches++;
			expression += " +1 " + getVariable(match); //$NON-NLS-1$
		}
		if (countMatches > 0) {
			dependencies.add(expression + (countMatches == 1 ? " >= 0;" : " >= 0;")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			if (req.isOptional()) {
				if (DEBUG)
					System.out.println("No IU found to satisfy optional dependency of " + iu + " req " + req); //$NON-NLS-1$//$NON-NLS-2$
			} else {
				throw new IllegalStateException("No IU found to satisfy dependency of " + iu + " req " + req); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
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
			//			v = new String("x" + (variables.size() + shift) + iu.toString()); //$NON-NLS-1$
			v = new String("x" + (variables.size() + shift)); //$NON-NLS-1$
			variables.put(iu, v);
		}
		return v;
	}

	public IStatus invokeSolver(IProgressMonitor monitor) {
		if (result.getSeverity() == IStatus.ERROR)
			return result;
		IPBSolver solver = SolverFactory.newEclipseP2();
		solver.setTimeout(60);
		OPBEclipseReader2007 reader = new OPBEclipseReader2007(solver);
		// CNF filename is given on the command line 
		long start = System.currentTimeMillis();
		if (DEBUG)
			System.out.println("Invoking solver: " + start); //$NON-NLS-1$
		FileReader fr = null;
		try {
			fr = new FileReader(problemFile);
			PBSolverCP problem = (PBSolverCP) reader.parseInstance(fr);
			if (problem.getOrder() instanceof VarOrderHeapObjective) {
				((VarOrderHeapObjective) problem.getOrder()).setObjectiveFunction(reader.getObjectiveFunction());
			}
			if (problem.isSatisfiable()) {
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
				result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, 1, "No solution found", null));
			}
		} catch (FileNotFoundException e) {
			//Ignore we are producing the input file
			if (DEBUG)
				e.printStackTrace();
		} catch (ParseFormatException e) {
			//Ignore we are producing the input file
			if (DEBUG)
				e.printStackTrace();
		} catch (ContradictionException e) {
			result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, 1, "No solution found because of a trivial contradiction", e));
		} catch (TimeoutException e) {
			result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, 1, "No solution found.", e));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (fr != null)
					fr.close();
			} catch (IOException e) {
				//ignore
			}
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
				if (variableForSyntheticIUs.get(entry.getKey()) == null)
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
