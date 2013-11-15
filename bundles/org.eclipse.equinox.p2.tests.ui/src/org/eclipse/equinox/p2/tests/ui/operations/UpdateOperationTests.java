/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.operations;

import java.util.ArrayList;
import java.util.HashSet;
import org.eclipse.equinox.internal.p2.operations.SearchForUpdatesResolutionJob;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

public class UpdateOperationTests extends AbstractProvisioningUITest {
	IInstallableUnit a1, b1;
	IInstallableUnit b12;
	IInstallableUnit a120WithDifferentId;
	IInstallableUnit a130;
	IInstallableUnit a140WithDifferentId;
	IInstallableUnitPatch firstPatchForA1, secondPatchForA1, thirdPatchForA1, patchFora2;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"));
		IUpdateDescriptor update = MetadataFactory.createUpdateDescriptor("A", new VersionRange("[1.0.0, 1.0.0]"), 0, "update description");
		a120WithDifferentId = createIU("UpdateA", Version.createOSGi(1, 2, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, false, update, NO_REQUIRES);
		a130 = createIU("A", Version.createOSGi(1, 3, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, false, update, NO_REQUIRES);
		a140WithDifferentId = createIU("UpdateForA", Version.createOSGi(1, 4, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, false, update, NO_REQUIRES);
		IRequirementChange change = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequirement lifeCycle = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		firstPatchForA1 = createIUPatch("P", Version.create("1.0.0"), true, new IRequirementChange[] {change}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, lifeCycle);
		secondPatchForA1 = createIUPatch("P", Version.create("2.0.0"), true, new IRequirementChange[] {change}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, lifeCycle);
		thirdPatchForA1 = createIUPatch("P2", Version.create("1.0.0"), true, new IRequirementChange[] {change}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, lifeCycle);

		IRequirementChange change2 = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequirement lifeCycle2 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 3.2.0]"), null, false, false);
		patchFora2 = createIUPatch("P", Version.create("1.0.0"), true, new IRequirementChange[] {change2}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, lifeCycle2);

		b1 = createIU("B", Version.create("1.0.0"));
		update = MetadataFactory.createUpdateDescriptor("B", new VersionRange("[1.0.0, 1.0.0]"), 0, "update description");
		b12 = createIU("B", Version.createOSGi(1, 2, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, false, update, NO_REQUIRES);
		// Ensure that all versions, not just the latest, are considered by the UI
		getPolicy().setShowLatestVersionsOnly(false);
	}

	public void testChooseUpdateOverPatch() {
		createTestMetdataRepository(new IInstallableUnit[] {a1, a120WithDifferentId, a130, firstPatchForA1, patchFora2});
		install(a1, true, false);
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(a1);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(iusInvolved, null);
		op.resolveModal(getMonitor());
		IProfileChangeRequest request = op.getProfileChangeRequest();
		assertTrue("1.0", request.getAdditions().size() == 1);
		assertTrue("1.1", request.getAdditions().iterator().next().equals(a130));
		assertTrue("1.2", request.getRemovals().size() == 1);
		assertTrue("1.3", request.getRemovals().iterator().next().equals(a1));
	}

	public void testForcePatchOverUpdate() {
		createTestMetdataRepository(new IInstallableUnit[] {a1, a120WithDifferentId, a130, firstPatchForA1, patchFora2});
		install(a1, true, false);
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(a1);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(iusInvolved, null);
		op.resolveModal(getMonitor());
		Update[] updates = op.getPossibleUpdates();
		Update firstPatch = null;
		for (int i = 0; i < updates.length; i++) {
			if (updates[i].replacement.equals(firstPatchForA1)) {
				firstPatch = updates[i];
				break;
			}
		}
		assertNotNull(".99", firstPatch);
		op.setSelectedUpdates(new Update[] {firstPatch});
		op.resolveModal(getMonitor());
		IProfileChangeRequest request = op.getProfileChangeRequest();
		assertTrue("1.0", request.getAdditions().size() == 1);
		assertTrue("1.1", request.getAdditions().iterator().next().equals(firstPatchForA1));
		assertTrue("1.2", request.getRemovals().size() == 0);
	}

	public void testRecognizePatchIsInstalled() {
		createTestMetdataRepository(new IInstallableUnit[] {a1, a120WithDifferentId, a130, firstPatchForA1, patchFora2});
		install(a1, true, false);
		install(firstPatchForA1, true, false);
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(a1);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(iusInvolved, null);
		op.resolveModal(getMonitor());
		IProfileChangeRequest request = op.getProfileChangeRequest();
		// update was favored, that would happen even if patch was not installed
		assertTrue("1.0", request.getAdditions().size() == 1);
		assertTrue("1.1", request.getAdditions().iterator().next().equals(a130));
		// the patch is not being shown to the user because we figured out it was already installed
		// The elements showing are a130 and a120WithDifferentId
		assertEquals("1.2", 2, op.getPossibleUpdates().length);
	}

	public void testChooseNotTheNewest() {
		createTestMetdataRepository(new IInstallableUnit[] {a1, a120WithDifferentId, a130, firstPatchForA1, patchFora2});
		install(a1, true, false);
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(a1);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(iusInvolved, null);
		op.resolveModal(getMonitor());
		Update[] updates = op.getPossibleUpdates();
		Update notNewest = null;
		for (int i = 0; i < updates.length; i++) {
			if (updates[i].replacement.equals(a120WithDifferentId)) {
				notNewest = updates[i];
				break;
			}
		}
		assertNotNull(".99", notNewest);
		op.setSelectedUpdates(new Update[] {notNewest});
		op.resolveModal(getMonitor());
		IProfileChangeRequest request = op.getProfileChangeRequest();
		// selected was favored
		assertTrue("1.0", request.getAdditions().size() == 1);
		assertTrue("1.1", request.getAdditions().iterator().next().equals(a120WithDifferentId));
		// The two updates and the patch were recognized
		assertEquals("1.2", 3, op.getPossibleUpdates().length);
	}

	public void testChooseLatestPatches() {
		createTestMetdataRepository(new IInstallableUnit[] {a1, firstPatchForA1, secondPatchForA1, thirdPatchForA1});
		install(a1, true, false);
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(a1);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(iusInvolved, null);
		op.resolveModal(getMonitor());
		IProfileChangeRequest request = op.getProfileChangeRequest();
		// the latest two patches were selected
		HashSet chosen = new HashSet();
		assertTrue("1.0", request.getAdditions().size() == 2);
		chosen.addAll(request.getAdditions());
		assertTrue("1.1", chosen.contains(secondPatchForA1));
		assertTrue("1.2", chosen.contains(thirdPatchForA1));

		assertEquals("1.2", 3, op.getPossibleUpdates().length);
	}

	public void testLatestHasDifferentId() {
		createTestMetdataRepository(new IInstallableUnit[] {a1, firstPatchForA1, secondPatchForA1, thirdPatchForA1, a120WithDifferentId, a130, a140WithDifferentId});
		install(a1, true, false);
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(a1);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(iusInvolved, null);
		op.resolveModal(getMonitor());
		IProfileChangeRequest request = op.getProfileChangeRequest();
		// update 140 was recognized as the latest even though it had a different id
		assertTrue("1.0", request.getAdditions().size() == 1);
		assertTrue("1.1", request.getAdditions().iterator().next().equals(a140WithDifferentId));
		// All three patches and all three updates can be chosen
		assertEquals("1.2", 6, op.getPossibleUpdates().length);
	}

	// bug 300445
	public void testRemoveSelectionAfterResolve() {
		createTestMetdataRepository(new IInstallableUnit[] {a1, a130, b1, b12});
		install(a1, true, false);
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(a1);
		iusInvolved.add(b1);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(iusInvolved, null);
		op.resolveModal(getMonitor());
		Update[] updates = op.getSelectedUpdates();
		assertEquals("1.0", 2, updates.length);
		// choose just one
		op.setSelectedUpdates(new Update[] {updates[0]});
		op.resolveModal(getMonitor());
		assertEquals("1.1", 1, op.getSelectedUpdates().length);
	}

	// bug 290858
	public void testSearchForUpdatesInJob() {
		createTestMetdataRepository(new IInstallableUnit[] {a1, a130, b1, b12});
		install(a1, true, false);
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(a1);
		iusInvolved.add(b1);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(iusInvolved, null);
		ProvisioningJob job = op.getResolveJob(getMonitor());
		assertTrue("1.0", job instanceof SearchForUpdatesResolutionJob);
		// getting the job should not compute the request.
		assertNull("1.1", ((SearchForUpdatesResolutionJob) job).getProfileChangeRequest());
		job.runModal(getMonitor());
		assertNotNull("1.2", ((SearchForUpdatesResolutionJob) job).getProfileChangeRequest());

	}
}
