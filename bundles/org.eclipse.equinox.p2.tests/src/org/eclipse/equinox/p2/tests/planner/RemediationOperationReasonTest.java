/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RemediationOperation;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class RemediationOperationReasonTest extends AbstractProvisioningTest {

	private RemediationOperation operation;
	private Method computeUnmetRequirementReason;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IProfile profile = createProfile("RemediationReasonTestProfile"); //$NON-NLS-1$
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		operation = new RemediationOperation(new ProvisioningSession(getAgent()), request);
		computeUnmetRequirementReason = RemediationOperation.class.getDeclaredMethod(
				"computeUnmetRequirementReason", //$NON-NLS-1$
				IInstallableUnit.class, Collection.class,
				org.eclipse.core.runtime.IProgressMonitor.class);
		computeUnmetRequirementReason.setAccessible(true);
	}


	private String invokeReason(IInstallableUnit iu, Collection<IInstallableUnit> available)
			throws Exception {
		return (String) computeUnmetRequirementReason.invoke(
				operation, iu, available, new NullProgressMonitor());
	}

	// Creates an IU with one mandatory capability requirement on a specific id + version range
	private IInstallableUnit iuRequiring(String id, String requiredId, VersionRange range) {
		IRequirement req = MetadataFactory.createRequirement(
				IInstallableUnit.NAMESPACE_IU_ID, requiredId, range, null, false, true);
		MetadataFactory.InstallableUnitDescription desc = new MetadataFactory.InstallableUnitDescription();
		desc.setId(id);
		desc.setVersion(Version.createOSGi(1, 0, 0));
		desc.setRequirements(new IRequirement[] { req });
		return MetadataFactory.createInstallableUnit(desc);
	}

	// Creates an IU with one optional (min=0) capability requirement
	private IInstallableUnit iuWithOptionalRequirement(String id, String requiredId, VersionRange range) {
		IRequirement req = MetadataFactory.createRequirement(
				IInstallableUnit.NAMESPACE_IU_ID, requiredId, range, null, true, true);
		MetadataFactory.InstallableUnitDescription desc = new MetadataFactory.InstallableUnitDescription();
		desc.setId(id);
		desc.setVersion(Version.createOSGi(1, 0, 0));
		desc.setRequirements(new IRequirement[] { req });
		return MetadataFactory.createInstallableUnit(desc);
	}

	// A satisfied requirement should not produce a reason
	public void testRequirementSatisfiedByCorrectVersionProducesNoReason() throws Exception {
		IInstallableUnit dep = createIU("org.example.dep", Version.createOSGi(1, 5, 0)); //$NON-NLS-1$
		IInstallableUnit main = iuRequiring("org.example.main", "org.example.dep", //$NON-NLS-1$ //$NON-NLS-2$
				new VersionRange("[1.0.0,2.0.0)")); //$NON-NLS-1$

		String reason = invokeReason(main, List.of(dep));

		assertNull("No reason expected when the dep version is within the required range", reason); //$NON-NLS-1$
	}

	// The dep exists but at the wrong version — a reason should still be produced.
	public void testRequirementUnmetDueToVersionMismatchProducesReason() throws Exception {
		// dep 3.0.0 is present but main requires [1.0.0,2.0.0)
		IInstallableUnit dep = createIU("org.example.dep", Version.createOSGi(3, 0, 0)); //$NON-NLS-1$
		IInstallableUnit main = iuRequiring("org.example.main", "org.example.dep", //$NON-NLS-1$ //$NON-NLS-2$
				new VersionRange("[1.0.0,2.0.0)")); //$NON-NLS-1$

		String reason = invokeReason(main, List.of(dep));

		assertNotNull("A reason must be produced when the dep version does not satisfy the range", reason); //$NON-NLS-1$
		assertTrue("Reason must mention the unsatisfied requirement", //$NON-NLS-1$
				reason.contains("org.example.dep")); //$NON-NLS-1$
		// Message must use the RequirementNotFound template (no provider in repos)
		assertTrue("Reason must use the 'not found in repository' message", //$NON-NLS-1$
				reason.contains("could not be found")); //$NON-NLS-1$
	}

	// Optional requirements should never be reported as a blocking cause.
	public void testOptionalRequirementIsNeverReported() throws Exception {
		IInstallableUnit main = iuWithOptionalRequirement("org.example.main", "org.example.optional.dep", //$NON-NLS-1$ //$NON-NLS-2$
				VersionRange.emptyRange);

		String reason = invokeReason(main, Collections.emptyList());

		assertNull("Optional requirements must not produce a reason", reason); //$NON-NLS-1$
	}

	// Only the mandatory unmet requirement should appear when mixed with an
	// optional one.
	public void testMandatoryRequirementReportedWhenMixedWithOptional() throws Exception {
		IRequirement optional = MetadataFactory.createRequirement(
				IInstallableUnit.NAMESPACE_IU_ID, "org.example.optional", //$NON-NLS-1$
				VersionRange.emptyRange, null, true, true); // min=0
		IRequirement mandatory = MetadataFactory.createRequirement(
				IInstallableUnit.NAMESPACE_IU_ID, "org.example.mandatory", //$NON-NLS-1$
				VersionRange.emptyRange, null, false, true); // min=1
		MetadataFactory.InstallableUnitDescription desc = new MetadataFactory.InstallableUnitDescription();
		desc.setId("org.example.main"); //$NON-NLS-1$
		desc.setVersion(Version.createOSGi(1, 0, 0));
		desc.setRequirements(new IRequirement[] { optional, mandatory });
		IInstallableUnit main = MetadataFactory.createInstallableUnit(desc);

		String reason = invokeReason(main, Collections.emptyList());

		assertNotNull("The mandatory requirement must still produce a reason", reason); //$NON-NLS-1$
		assertTrue("Reason must name the mandatory requirement, not the optional one", //$NON-NLS-1$
				reason.contains("org.example.mandatory")); //$NON-NLS-1$
		assertFalse("The optional requirement must not appear in the reason", //$NON-NLS-1$
				reason.contains("org.example.optional")); //$NON-NLS-1$
	}
}
