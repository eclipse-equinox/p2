/*******************************************************************************
 * Copyright (c) 2019 Guillaume Dufour and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Guillaume Dufour - test lifecycle requirement overwrite
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import java.util.Arrays;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitPatch;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Black box tests for API of {@link org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnitPatch}.
 */
public class InstallableUnitPatchTest extends AbstractProvisioningTest {
	/**
	 * Tests for {@link org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit#satisfies(org.eclipse.equinox.internal.provisional.p2.metadata.RequiredCapability)}.
	 */
	public void testLifeCycleRequirement() {

		org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitPatchDescription iu = new MetadataFactory.InstallableUnitPatchDescription();
		iu.setId("P");
		iu.setVersion(Version.create("1.0.0"));
		iu.setApplicabilityScope(new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}});
		IRequirement lRequirement = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "L", VersionRange.emptyRange, null, false, false);
		iu.setLifeCycle(lRequirement);
		iu.setRequirements(NO_REQUIRES);
		IInstallableUnitPatch p1 = MetadataFactory.createInstallableUnitPatch(iu);

		assertEquals("patch requirement must contains the lifecycle, so size must be equal to 1", 1, p1.getRequirements().size());
		assertEquals("patch requirement must contains the lifecycle", lRequirement, p1.getRequirements().iterator().next());

		org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitPatchDescription iu2 = new MetadataFactory.InstallableUnitPatchDescription();
		iu2.setId("P2");
		iu2.setVersion(Version.create("1.0.0"));
		iu2.setApplicabilityScope(new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A2", VersionRange.emptyRange, null, false, false)}});
		IRequirement l2Requirement = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "L2", VersionRange.emptyRange, null, false, false);
		iu2.setLifeCycle(l2Requirement);
		IRequirement r = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "R", VersionRange.emptyRange, null, false, false);
		iu2.setRequirements(new IRequirement[] {r});
		IInstallableUnitPatch p2 = MetadataFactory.createInstallableUnitPatch(iu2);

		assertEquals("patch requirement must contains the lifecycle and the requirement, so size must be equal to 2", 2, p2.getRequirements().size());
		assertContains("patch requirement must contains the lifecycle", p2.getRequirements().iterator(), Arrays.asList(l2Requirement, r));
	}
}
