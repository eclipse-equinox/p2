/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.CapabilityQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.InvalidSyntaxException;

public class Slicer {
	private static boolean DEBUG = false;
	private IQueryable possibilites;

	private LinkedList toProcess;
	private Set considered; //IUs to add to the slice
	private TwoTierMap slice; //The IUs that have been considered to be part of the problem

	private Dictionary selectionContext;
	private MultiStatus result;

	public Slicer(IQueryable input, Dictionary context) {
		possibilites = input;
		slice = new TwoTierMap();
		selectionContext = context;
		result = new MultiStatus(DirectorActivator.PI_DIRECTOR, IStatus.OK, Messages.Planner_Problems_resolving_plan, null);
	}

	public Slicer(IInstallableUnit[] installRoots, IInstallableUnit[] gatherAvailableInstallableUnits, Dictionary selectionContext) {
		this(new QueryableArray(gatherAvailableInstallableUnits), selectionContext);
	}

	public IQueryable slice(IInstallableUnit[] ius, IProgressMonitor monitor) {
		try {
			long start = 0;
			if (DEBUG) {
				start = System.currentTimeMillis();
				System.out.println("Start slicing: " + start); //$NON-NLS-1$
			}

			validateInput(ius);
			considered = new HashSet(Arrays.asList(ius));
			toProcess = new LinkedList(considered);
			while (!toProcess.isEmpty()) {
				if (monitor.isCanceled()) {
					result.merge(Status.CANCEL_STATUS);
					throw new OperationCanceledException();
				}
				processIU((IInstallableUnit) toProcess.removeFirst());
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
		return new QueryableArray((IInstallableUnit[]) considered.toArray(new IInstallableUnit[considered.size()]));
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

	private void processIU(IInstallableUnit iu) {
		iu = iu.unresolved();

		slice.put(iu.getId(), iu.getVersion(), iu);
		if (!isApplicable(iu)) {
			return;
		}

		IRequiredCapability[] reqs = getRequiredCapabilities(iu);
		if (reqs.length == 0) {
			return;
		}
		for (int i = 0; i < reqs.length; i++) {
			if (!isApplicable(reqs[i]))
				continue;

			if (!reqs[i].isGreedy()) {
				continue;
			}

			expandRequirement(iu, reqs[i]);
		}
	}

	private IRequiredCapability[] getRequiredCapabilities(IInstallableUnit iu) {
		if (!(iu instanceof IInstallableUnitPatch)) {
			return iu.getRequiredCapabilities();
		}
		IRequiredCapability[] aggregatedCapabilities;
		IInstallableUnitPatch patchIU = (IInstallableUnitPatch) iu;
		IRequirementChange[] changes = patchIU.getRequirementsChange();
		int initialRequirementCount = iu.getRequiredCapabilities().length;
		aggregatedCapabilities = new IRequiredCapability[initialRequirementCount + changes.length];
		System.arraycopy(iu.getRequiredCapabilities(), 0, aggregatedCapabilities, 0, initialRequirementCount);
		for (int i = 0; i < changes.length; i++) {
			aggregatedCapabilities[initialRequirementCount++] = changes[i].newValue();
		}
		return aggregatedCapabilities;
	}

	private void expandRequirement(IInstallableUnit iu, IRequiredCapability req) {
		Collector matches = possibilites.query(new CapabilityQuery(req), new Collector(), null);
		int validMatches = 0;
		for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
			IInstallableUnit match = (IInstallableUnit) iterator.next();
			if (!isApplicable(match))
				continue;
			validMatches++;
			if (!slice.containsKey(match.getId(), match.getVersion()))
				consider(match);
		}

		if (validMatches == 0) {
			if (req.isOptional()) {
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
