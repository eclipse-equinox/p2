/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import static org.eclipse.core.runtime.IStatus.ERROR;
import static org.eclipse.core.runtime.IStatus.OK;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.equinox.internal.p2.director.Explanation.MissingIU;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerStatus;
import org.eclipse.equinox.internal.provisional.p2.director.RequestStatus;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PropertyMatchRequirement extends AbstractProvisioningTest {
	private IInstallableUnit providerIu;
	private IInstallableUnit consumerIu;

	private IProfile profile;
	private IPlanner planner;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// A standard OSGi service representation
		String osgiService = "osgi.service";
		String objectClass = "objectClass";
		List<String> objectClassList = Arrays.asList("org.example.A", "org.example.B", "org.example.C");

		// Provider.
		// TODO Check if p2 really needs a name. IProvidedCapability.equals() can differentiate by properties only.
		Map<String, Object> capability = new HashMap<>();
		capability.put(osgiService, "ignored-1");
		capability.put(objectClass, objectClassList);

		IProvidedCapability[] provides = new IProvidedCapability[] {
				MetadataFactory.createProvidedCapability(osgiService, capability)
		};
		providerIu = createIU("provider", DEFAULT_VERSION, provides);

		// Consumer
		String requirement = String.format("(%s=%s)", objectClass, objectClassList.get(0));
		IRequirement[] requires = new IRequirement[] {
				MetadataFactory.createRequirement(osgiService, requirement, null, 1, 1, true)
		};
		consumerIu = createIU("consumer", DEFAULT_VERSION, requires);

		// Planner
		profile = createProfile("test." + getName());
		planner = createPlanner();
	}

	public void testMandatoryPresent() {
		createTestMetdataRepository(new IInstallableUnit[] {providerIu, consumerIu});

		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.add(consumerIu);

		// Must pass
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(OK, plan.getStatus().getSeverity());

		// And both consumer and provider must be installed
		assertInstallOperand(plan, consumerIu);
		assertInstallOperand(plan, providerIu);
	}

	public void testMandatoryAbsent() {
		createTestMetdataRepository(new IInstallableUnit[] {consumerIu});

		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.add(consumerIu);

		// Must fail
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(ERROR, plan.getStatus().getSeverity());

		// With a good explanation
		RequestStatus requestStatus = ((PlannerStatus) plan.getStatus()).getRequestStatus();
		IRequirement consumerReq = consumerIu.getRequirements().iterator().next();
		requestStatus.getExplanations()
				.stream()
				.filter(e -> e instanceof MissingIU)
				.filter(e -> (((MissingIU) e).req.toString()).equals(consumerReq.toString()))
				.findFirst()
				.orElseGet(() -> {
					fail("Did not find explanation for missing requirement: " + consumerReq);
					return null;
				});

		// And the consumer must not be installed
		assertNoOperand(plan, consumerIu);
	}
}
