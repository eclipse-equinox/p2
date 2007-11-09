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
import org.eclipse.equinox.p2.engine.Operand;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.query.CompoundIterator;
import org.eclipse.osgi.service.resolver.VersionRange;

public class OperationGenerator {
	static IResolvedInstallableUnit NULL_IU = new ResolvedInstallableUnit(MetadataFactory.createInstallableUnit(new InstallableUnitDescription()));

	public Operand[] generateOperation(Collection from_, Collection to_) {
		List from = new ArrayList(from_);
		Collections.sort(from);

		List to = new ArrayList(to_);
		Collections.sort(to);

		ArrayList operations = new ArrayList();
		generateUpdates(from, to, operations);
		generateInstallUninstall(from, to, operations);
		Operand[] ops = (Operand[]) operations.toArray(new Operand[operations.size()]);
		return ops;
	}

	private void generateInstallUninstall(List from, List to, ArrayList operations) {
		int toIdx = 0;
		int fromIdx = 0;
		while (fromIdx != from.size() && toIdx != to.size()) {
			IResolvedInstallableUnit fromIU = (IResolvedInstallableUnit) from.get(fromIdx);
			IResolvedInstallableUnit toIU = (IResolvedInstallableUnit) to.get(toIdx);
			int comparison = toIU.compareTo(fromIU);
			if (comparison < 0) {
				operations.add(createInstallOperation(toIU));
				toIdx++;
			} else if (comparison == 0) {
				toIdx++;
				fromIdx++;
				//				System.out.println("same " + fromIU);
			} else {
				operations.add(createUninstallOperation(fromIU));
				fromIdx++;
			}
		}
		if (fromIdx != from.size()) {
			for (int i = fromIdx; i < from.size(); i++) {
				operations.add(createUninstallOperation((IResolvedInstallableUnit) from.get(i)));
			}
		}
		if (toIdx != to.size()) {
			for (int i = toIdx; i < to.size(); i++) {
				operations.add(createInstallOperation((IResolvedInstallableUnit) to.get(i)));
			}
		}
	}

	private void generateUpdates(List from, List to, ArrayList operations) {
		Set processed = new HashSet();
		for (int toIdx = 0; toIdx < to.size(); toIdx++) {
			IResolvedInstallableUnit iuTo = (IResolvedInstallableUnit) to.get(toIdx);
			if (iuTo.getId().equals(next(from, toIdx).getId())) {
				toIdx = skip(to, iuTo, toIdx) - 1;
				//System.out.println("Can't update " + iuTo + " because another iu with same id is in the target state");
				continue;
			}
			if (iuTo.getProperty(IInstallableUnitConstants.UPDATE_FROM) == null)
				continue;
			//when the ui we update from is in the new state, skip (for example FROM is A, C, B & TO is C (update of 
			Iterator updates = new CompoundIterator(new Iterator[] {from.iterator()}, iuTo.getProperty(IInstallableUnitConstants.UPDATE_FROM), new VersionRange(iuTo.getProperty(IInstallableUnitConstants.UPDATE_RANGE)), null, false);
			IResolvedInstallableUnit iuFrom;
			if (!updates.hasNext()) { //Nothing to udpate from.
				continue;
			}
			iuFrom = (IResolvedInstallableUnit) updates.next();
			if (updates.hasNext()) { //There are multiple IUs to update from
				//System.out.println("Can't update  " + iuTo + " because there are multiple IUs to update from (" + toString(iusFrom) + ')');
				continue;
			}
			if (iuTo.equals(iuFrom)) {
				from.remove(iuFrom);
				to.remove(iuTo);
				continue;
			}
			operations.add(createUpdateOperation(iuFrom, iuTo));
			from.remove(iuFrom);
			processed.add(iuTo);
		}
		to.removeAll(processed);
	}

	private Operand createUninstallOperation(IResolvedInstallableUnit iu) {
		return new Operand(iu, null);
	}

	private Operand createInstallOperation(IResolvedInstallableUnit iu) {
		return new Operand(null, iu);
	}

	private Operand createUpdateOperation(IResolvedInstallableUnit from, IResolvedInstallableUnit to) {
		return new Operand(from, to);
	}

	private IResolvedInstallableUnit next(List l, int i) {
		i++;
		if (i >= l.size())
			return NULL_IU;
		return (IResolvedInstallableUnit) l.get(i);
	}

	private int skip(List c, IResolvedInstallableUnit id, int idx) {
		int i = idx;
		for (; i < c.size(); i++) {
			if (!id.getId().equals(((IInstallableUnit) c.get(idx)).getId()))
				return i;
		}
		return i;
	}
}
