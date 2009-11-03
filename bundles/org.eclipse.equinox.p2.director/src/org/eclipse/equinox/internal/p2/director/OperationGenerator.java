/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;

public class OperationGenerator {
	private static final IInstallableUnit NULL_IU = MetadataFactory.createResolvedInstallableUnit(MetadataFactory.createInstallableUnit(new InstallableUnitDescription()), new IInstallableUnitFragment[0]);

	public InstallableUnitOperand[] generateOperation(Collection from_, Collection to_) {
		Collection intersection = new HashSet(from_);
		intersection.retainAll(to_);

		HashSet tmpFrom = new HashSet(from_);
		HashSet tmpTo = new HashSet(to_);
		tmpFrom.removeAll(intersection);
		tmpTo.removeAll(intersection);

		List from = new ArrayList(tmpFrom);
		Collections.sort(from);

		List to = new ArrayList(tmpTo);
		Collections.sort(to);

		ArrayList operations = new ArrayList();
		generateUpdates(from, to, operations);
		generateInstallUninstall(from, to, operations);
		generateConfigurationChanges(to_, intersection, operations);
		InstallableUnitOperand[] ops = (InstallableUnitOperand[]) operations.toArray(new InstallableUnitOperand[operations.size()]);
		return ops;
	}

	//This generates operations that are causing the IUs to be reconfigured.
	private void generateConfigurationChanges(Collection to_, Collection intersection, ArrayList operations) {
		if (intersection.size() == 0)
			return;
		//We retain from each set the things that are the same.
		//Note that despite the fact that they are the same, a different CU can be attached.
		//The objects contained in the intersection are the one that were originally in the from collection.
		TreeSet to = new TreeSet(to_);
		for (Iterator iterator = intersection.iterator(); iterator.hasNext();) {
			IInstallableUnit fromIU = (IInstallableUnit) iterator.next();
			IInstallableUnit toIU = (IInstallableUnit) to.tailSet(fromIU).first();
			generateConfigurationOperation(fromIU, toIU, operations);
		}

	}

	private void generateConfigurationOperation(IInstallableUnit fromIU, IInstallableUnit toIU, ArrayList operations) {
		IInstallableUnitFragment[] fromFragments = fromIU.getFragments();
		IInstallableUnitFragment[] toFragments = toIU.getFragments();
		if (fromFragments == toFragments)
			return;
		//Check to see if the two arrays are equals independently of the order of the fragments
		boolean different = false;
		if (fromFragments != null && toFragments != null) {
			if (fromFragments.length != toFragments.length)
				different = true;
			else {
				for (int i = 0; i < fromFragments.length; i++) {
					boolean found = false;
					for (int j = 0; j < toFragments.length; j++) {
						if (fromFragments[i].equals(toFragments[j]))
							found = true;
					}
					if (!found) {
						different = true;
						break;
					}
				}
			}
		} else {
			//One of the two array is null
			different = true;
		}
		if (!different)
			return;
		operations.add(new InstallableUnitOperand(fromIU, toIU));
	}

	private void generateInstallUninstall(List from, List to, ArrayList operations) {
		int toIdx = 0;
		int fromIdx = 0;
		while (fromIdx != from.size() && toIdx != to.size()) {
			IInstallableUnit fromIU = (IInstallableUnit) from.get(fromIdx);
			IInstallableUnit toIU = (IInstallableUnit) to.get(toIdx);
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
				operations.add(createUninstallOperation((IInstallableUnit) from.get(i)));
			}
		}
		if (toIdx != to.size()) {
			for (int i = toIdx; i < to.size(); i++) {
				operations.add(createInstallOperation((IInstallableUnit) to.get(i)));
			}
		}
	}

	private void generateUpdates(List from, List to, ArrayList operations) {
		Set processed = new HashSet();
		Set removedFromTo = new HashSet();

		if (to.isEmpty() || from.isEmpty())
			return;

		Map fromById = new HashMap();
		for (Iterator iterator = from.iterator(); iterator.hasNext();) {
			IInstallableUnit iuFrom = (IInstallableUnit) iterator.next();
			List ius = (List) fromById.get(iuFrom.getId());
			if (ius == null) {
				ius = new ArrayList();
				fromById.put(iuFrom.getId(), ius);
			}
			ius.add(iuFrom);
		}

		for (int toIdx = 0; toIdx < to.size(); toIdx++) {
			IInstallableUnit iuTo = (IInstallableUnit) to.get(toIdx);
			if (iuTo.getId().equals(next(to, toIdx).getId())) { //This handle the case where there are multiple versions of the same IU in the target. Eg we are trying to update from A 1.0.0 to A 1.1.1 and A 1.2.2
				toIdx = skip(to, iuTo, toIdx) - 1;
				//System.out.println("Can't update " + iuTo + " because another iu with same id is in the target state");
				continue;
			}
			if (iuTo.getUpdateDescriptor() == null)
				continue;

			List fromIdIndexList = (List) fromById.get(iuTo.getUpdateDescriptor().getId());
			if (fromIdIndexList == null)
				continue;

			//when the ui we update from is in the new state, skip (for example FROM is A, C, B & TO is C (update of
			InstallableUnitQuery updateQuery = new InstallableUnitQuery(iuTo.getUpdateDescriptor().getId(), iuTo.getUpdateDescriptor().getRange());
			Iterator updates = updateQuery.perform(fromIdIndexList.iterator(), new Collector()).iterator();

			if (!updates.hasNext()) { //Nothing to update from.
				continue;
			}
			IInstallableUnit iuFrom = (IInstallableUnit) updates.next();
			if (updates.hasNext()) { //There are multiple IUs to update from
				//System.out.println("Can't update  " + iuTo + " because there are multiple IUs to update from (" + toString(iusFrom) + ')');
				continue;
			}
			if (iuTo.equals(iuFrom)) {
				from.remove(iuFrom);
				fromIdIndexList.remove(iuFrom);
				removedFromTo.add(iuTo);
				continue;
			}
			operations.add(createUpdateOperation(iuFrom, iuTo));
			from.remove(iuFrom);
			fromIdIndexList.remove(iuFrom);
			processed.add(iuTo);
		}
		to.removeAll(processed);
		to.removeAll(removedFromTo);
	}

	private InstallableUnitOperand createUninstallOperation(IInstallableUnit iu) {
		return new InstallableUnitOperand(iu, null);
	}

	private InstallableUnitOperand createInstallOperation(IInstallableUnit iu) {
		return new InstallableUnitOperand(null, iu);
	}

	private InstallableUnitOperand createUpdateOperation(IInstallableUnit from, IInstallableUnit to) {
		return new InstallableUnitOperand(from, to);
	}

	private IInstallableUnit next(List l, int i) {
		i++;
		if (i >= l.size())
			return NULL_IU;
		return (IInstallableUnit) l.get(i);
	}

	private int skip(List c, IInstallableUnit id, int idx) {
		int i = idx;
		for (; i < c.size(); i++) {
			if (!id.getId().equals(((IInstallableUnit) c.get(i)).getId()))
				return i;
		}
		return i;
	}
}
