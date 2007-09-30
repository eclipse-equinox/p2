/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.director;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.p2.director.Messages;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.resolution.ResolutionHelper;
import org.eclipse.osgi.service.resolver.VersionRange;

//TODO The "extends" relationship with the director is a hack to get stuffs working
public class Oracle extends NewSimpleDirector {
	public Object canInstall(IInstallableUnit[] toAdd, Profile profile, IProgressMonitor monitor) {
		MultiStatus result = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, "oracle", null);
		SubMonitor sub = SubMonitor.convert(monitor, ExpandWork + OperationWork);
		try {
			IInstallableUnit[] alreadyInstalled = toArray(profile.getInstallableUnits());
			if (alreadyIn(alreadyInstalled, toAdd[0])) {
				return Boolean.FALSE;
			}
			//Compute the complete closure of things to install to successfully install the installRoots.
			NewDependencyExpander expander = new NewDependencyExpander(toAdd, alreadyInstalled, gatherAvailableInstallableUnits(toAdd), profile, true);
			IStatus expanderResult = expander.expand(sub);
			sub.worked(ExpandWork);
			if (expanderResult.isOK()) {
				return Boolean.TRUE;
			}
			Collection resolved = expander.getAllInstallableUnits();
			Collection entryPoints = new HashSet();
			for (Iterator iterator = resolved.iterator(); iterator.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) iterator.next();
				//				if ("true".equals(iu.getProperty("entryPoint"))) {
				entryPoints.add(iu);
				//				}
			}
			Collection initialEntryPoints = new HashSet();
			for (int i = 0; i < alreadyInstalled.length; i++) {
				//				if ("true".equals(alreadyInstalled[i].getProperty("entryPoint"))) {
				initialEntryPoints.add(alreadyInstalled[i]);
				//				}
			}
			initialEntryPoints.removeAll(entryPoints);
			if (initialEntryPoints.size() != 0) {
				return initialEntryPoints;
			}
			return Boolean.FALSE;
		} finally {
			sub.done();
		}
	}

	public boolean canInstall(IInstallableUnit[] toAdd, IInstallableUnit[] toUpdate, Profile profile, IProgressMonitor monitor) {
		IInstallableUnit[] replacements = new IInstallableUnit[toUpdate.length + toAdd.length];
		for (int i = 0; i < toUpdate.length; i++) {
			replacements[i] = containsUpdate(toUpdate[i]);
			//TODO We need to be able to deal with multiple updates and even where is none
			if (replacements[i] == null)
				return false;
		}
		System.arraycopy(toAdd, 0, replacements, toUpdate.length, toAdd.length);

		MultiStatus result = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, Messages.Director_Uninstall_Problems, null);
		SubMonitor sub = SubMonitor.convert(monitor, OperationWork);

		IInstallableUnit[] alreadyInstalled = toArray(profile.getInstallableUnits());
		Collection oldState = new ResolutionHelper(profile.getSelectionContext(), null).attachCUs(Arrays.asList(alreadyInstalled));

		NewDependencyExpander expander = new NewDependencyExpander(toUpdate, new IInstallableUnit[0], alreadyInstalled, profile, true);
		expander.expand(sub);
		Collection toUninstallClosure = new ResolutionHelper(profile.getSelectionContext(), null).attachCUs(expander.getAllInstallableUnits());

		Collection remainingIUs = new HashSet(oldState);
		remainingIUs.removeAll(toUninstallClosure);
		NewDependencyExpander finalExpander = new NewDependencyExpander(replacements, (IInstallableUnit[]) remainingIUs.toArray(new IInstallableUnit[remainingIUs.size()]), gatherAvailableInstallableUnits(toAdd), profile, true);
		finalExpander.expand(sub);
		Collection newState = new ResolutionHelper(profile.getSelectionContext(), null).attachCUs(finalExpander.getAllInstallableUnits());

		return true;
	}

	private IInstallableUnit containsUpdate(IInstallableUnit iu) {
		IInstallableUnit[] candidates = gatherAvailableInstallableUnits(null);
		for (int i = 0; i < candidates.length; i++) {
			if (iu.getId().equals(candidates[i].getProperty(IInstallableUnitConstants.UPDATE_FROM))) {
				if (iu.equals(candidates[i]))
					continue;
				VersionRange range = new VersionRange(candidates[i].getProperty(IInstallableUnitConstants.UPDATE_RANGE));
				if (range.isIncluded(iu.getVersion()) && candidates[i].getVersion().compareTo(iu.getVersion()) > 0)
					return candidates[i];
			}
		}
		return null;
	}

	private boolean alreadyIn(IInstallableUnit[] ius, IInstallableUnit id) {
		for (int i = 0; i < ius.length; i++) {
			if (ius[i].equals(id))
				return true;
		}
		return false;
	}

	private IInstallableUnit[] toArray(Iterator it) {
		ArrayList result = new ArrayList();
		while (it.hasNext()) {
			result.add(it.next());
		}
		return (IInstallableUnit[]) result.toArray(new IInstallableUnit[result.size()]);
	}

	public Collection hasUpdate(IInstallableUnit toUpdate) {
		if (toUpdate.getProperty(IInstallableUnitConstants.ENTRYPOINT_IU_KEY) != null)
			return entryPointProcessing(toUpdate);
		IInstallableUnit[] allius = gatherAvailableInstallableUnits(null);
		Set updates = new HashSet();
		for (int i = 0; i < allius.length; i++) {
			if (toUpdate.getId().equals(allius[i].getProperty(IInstallableUnitConstants.UPDATE_FROM))) {
				if (toUpdate.getVersion().compareTo(allius[i].getVersion()) < 0 && new VersionRange(allius[i].getProperty(IInstallableUnitConstants.UPDATE_RANGE)).isIncluded(toUpdate.getVersion()))
					updates.add(allius[i]);
			}
		}
		return updates;
	}

	private Collection entryPointProcessing(IInstallableUnit entryPoint) {
		ArrayList updates = new ArrayList();
		RequiredCapability[] entries = entryPoint.getRequiredCapabilities();
		for (int i = 0; i < entries.length; i++) {
			if (!IInstallableUnit.IU_NAMESPACE.equals(entries[i].getNamespace()))
				continue;
			IInstallableUnit[] allius = gatherAvailableInstallableUnits(null);
			IInstallableUnit match = null;
			for (int j = 0; j < allius.length; j++) {
				if (entries[i].getName().equals(allius[j].getProperty(IInstallableUnitConstants.UPDATE_FROM))) {
					if (new VersionRange(allius[j].getProperty(IInstallableUnitConstants.UPDATE_RANGE)).isIncluded(entries[i].getRange().getMinimum()) && allius[j].getVersion().compareTo(entries[i].getRange().getMinimum()) > 0) {
						if (match == null || allius[j].getVersion().compareTo(match.getVersion()) > 0)
							match = allius[j];
					}
				}
			}
			if (match != null)
				updates.add(match);
		}
		if (updates.size() == 0)
			return updates;
		String entryPointName = entryPoint.getProperty(IInstallableUnitConstants.NAME);
		if (entryPointName == null)
			entryPointName = entryPoint.getId();
		InstallableUnit newEntryPoint = createEntryPoint(entryPointName, (IInstallableUnit[]) updates.toArray(new IInstallableUnit[updates.size()]));
		newEntryPoint.setProperty(IInstallableUnitConstants.UPDATE_FROM, entryPoint.getId());
		ArrayList result = new ArrayList();
		result.add(newEntryPoint);
		return result;
	}
}
