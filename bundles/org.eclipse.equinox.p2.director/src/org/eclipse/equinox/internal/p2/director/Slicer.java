/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.osgi.util.NLS;

public class Slicer {
	private static boolean DEBUG = false;
	private final IQueryable<IInstallableUnit> possibilites;
	private final boolean considerMetaRequirements;
	protected final IInstallableUnit selectionContext;
	private final Map<String, Map<Version, IInstallableUnit>> slice; //The IUs that have been considered to be part of the problem
	private final MultiStatus result;

	private LinkedList<IInstallableUnit> toProcess;
	private Set<IInstallableUnit> considered; //IUs to add to the slice

	public Slicer(IQueryable<IInstallableUnit> input, Map<String, String> context, boolean considerMetaRequirements) {
		this(input, InstallableUnit.contextIU(context), considerMetaRequirements);
	}

	public Slicer(IQueryable<IInstallableUnit> possibilites, IInstallableUnit selectionContext, boolean considerMetaRequirements) {
		this.possibilites = possibilites;
		this.selectionContext = selectionContext;
		this.considerMetaRequirements = considerMetaRequirements;
		slice = new HashMap<String, Map<Version, IInstallableUnit>>();
		result = new MultiStatus(DirectorActivator.PI_DIRECTOR, IStatus.OK, Messages.Planner_Problems_resolving_plan, null);
	}

	public IQueryable<IInstallableUnit> slice(IInstallableUnit[] ius, IProgressMonitor monitor) {
		try {
			long start = 0;
			if (DEBUG) {
				start = System.currentTimeMillis();
				System.out.println("Start slicing: " + start); //$NON-NLS-1$
			}

			validateInput(ius);
			considered = new HashSet<IInstallableUnit>(Arrays.asList(ius));
			toProcess = new LinkedList<IInstallableUnit>(considered);
			while (!toProcess.isEmpty()) {
				if (monitor.isCanceled()) {
					result.merge(Status.CANCEL_STATUS);
					throw new OperationCanceledException();
				}
				processIU(toProcess.removeFirst());
			}
			if (DEBUG) {
				long stop = System.currentTimeMillis();
				System.out.println("Slicing complete: " + (stop - start)); //$NON-NLS-1$
			}
		} catch (IllegalStateException e) {
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, e.getMessage(), e));
		}
		if (Tracing.DEBUG && result.getSeverity() != IStatus.OK)
			LogHelper.log(result);
		if (result.getSeverity() == IStatus.ERROR)
			return null;
		return new QueryableArray(considered.toArray(new IInstallableUnit[considered.size()]));
	}

	public MultiStatus getStatus() {
		return result;
	}

	//This is a shortcut to simplify the error reporting when the filter of the ius we are being asked to install does not pass
	private void validateInput(IInstallableUnit[] ius) {
		for (int i = 0; i < ius.length; i++) {
			if (!isApplicable(ius[i]))
				throw new IllegalStateException("The IU " + ius[i] + " can't be installed in this environment because its filter does not match."); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	// Check whether the requirement is applicable
	protected boolean isApplicable(IRequirement req) {
		IMatchExpression<IInstallableUnit> filter = req.getFilter();
		return filter == null || filter.isMatch(selectionContext);
	}

	protected boolean isApplicable(IInstallableUnit iu) {
		IMatchExpression<IInstallableUnit> filter = iu.getFilter();
		return filter == null || filter.isMatch(selectionContext);
	}

	protected void processIU(IInstallableUnit iu) {
		iu = iu.unresolved();

		Map<Version, IInstallableUnit> iuSlice = slice.get(iu.getId());
		if (iuSlice == null) {
			iuSlice = new HashMap<Version, IInstallableUnit>();
			slice.put(iu.getId(), iuSlice);
		}
		iuSlice.put(iu.getVersion(), iu);
		if (!isApplicable(iu)) {
			return;
		}

		Collection<IRequirement> reqs = getRequiredCapabilities(iu);
		if (reqs.isEmpty())
			return;
		for (IRequirement req : reqs) {
			if (!isApplicable(req))
				continue;

			if (!isGreedy(req)) {
				continue;
			}

			expandRequirement(iu, req);
		}
	}

	protected boolean isGreedy(IRequirement req) {
		return req.isGreedy();
	}

	private Collection<IRequirement> getRequiredCapabilities(IInstallableUnit iu) {
		Collection<IRequirement> iuRequirements = iu.getRequirements();
		int initialRequirementCount = iuRequirements.size();
		if (!(iu instanceof IInstallableUnitPatch)) {
			if (!considerMetaRequirements)
				return iuRequirements;

			Collection<IRequirement> iuMetaRequirements = iu.getMetaRequirements();
			int metaSize = iuMetaRequirements.size();
			if (metaSize == 0)
				return iuRequirements;

			ArrayList<IRequirement> aggregatedCapabilities = new ArrayList<IRequirement>(initialRequirementCount + metaSize);
			aggregatedCapabilities.addAll(iuRequirements);
			aggregatedCapabilities.addAll(iuMetaRequirements);
			return aggregatedCapabilities;
		}

		IInstallableUnitPatch patchIU = (IInstallableUnitPatch) iu;
		List<IRequirementChange> changes = patchIU.getRequirementsChange();
		ArrayList<IRequirement> aggregatedCapabilities = new ArrayList<IRequirement>(initialRequirementCount + changes.size());
		aggregatedCapabilities.addAll(iuRequirements);
		for (int i = 0; i < changes.size(); i++)
			aggregatedCapabilities.add(changes.get(i).newValue());
		return aggregatedCapabilities;
	}

	private void expandRequirement(IInstallableUnit iu, IRequirement req) {
		if (req.getMax() == 0)
			return;
		IQueryResult<IInstallableUnit> matches = possibilites.query(QueryUtil.createMatchQuery(req.getMatches()), null);
		int validMatches = 0;
		for (Iterator<IInstallableUnit> iterator = matches.iterator(); iterator.hasNext();) {
			IInstallableUnit match = iterator.next();
			if (!isApplicable(match))
				continue;
			validMatches++;
			Map<Version, IInstallableUnit> iuSlice = slice.get(match.getId());
			if (iuSlice == null || !iuSlice.containsKey(match.getVersion()))
				consider(match);
		}

		if (validMatches == 0) {
			if (req.getMin() == 0) {
				if (DEBUG)
					System.out.println("No IU found to satisfy optional dependency of " + iu + " on req " + req); //$NON-NLS-1$//$NON-NLS-2$
			} else {
				result.add(new Status(IStatus.WARNING, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Unsatisfied_dependency, iu, req)));
			}
		}
	}

	private void consider(IInstallableUnit match) {
		if (considered.add(match))
			toProcess.addLast(match);
	}
}
