package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.p2.engine.IProvisioningPlan;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ORTesting extends AbstractProvisioningTest {
	//A[1.0] v A[1.1]
	//!A[1.0.2]

	//A v C v !D[1.0.0, 1.1.0]   <-- What is the semantics of negation on a range?

	private static final String NS = "theNamespace";
	private static final String N = "theName";

	//A v C v C[1.0.0]
	public void testOR() {
		RequiredCapability req1 = new RequiredCapability(NS, N, new VersionRange("[1.0.0, 1.2.0)"), null, false, false);
		//		RequiredCapability req2 = new RequiredCapability(NS, N, new VersionRange("[1.5.0, 2.0.0)"), null, false, false);
		ORRequirement req = new ORRequirement(new IRequiredCapability[] {req1});
		ProvidedCapability prov = new ProvidedCapability(NS, N, Version.createOSGi(1, 5, 0));
		assertFalse(prov.satisfies(req));
	}

	public void testOR2() {
		RequiredCapability req1 = new RequiredCapability(NS, N, new VersionRange("[1.0.0, 1.2.0)"), null, false, false);
		RequiredCapability req2 = new RequiredCapability(NS, N, new VersionRange("[1.5.0, 2.0.0)"), null, false, false);
		ORRequirement req = new ORRequirement(new IRequiredCapability[] {req1, req2});
		ProvidedCapability prov = new ProvidedCapability(NS, N, Version.createOSGi(1, 5, 0));
		assertTrue(prov.satisfies(req));
	}

	public void testOR3() {
		MetadataFactory.InstallableUnitDescription iud1 = new MetadataFactory.InstallableUnitDescription();
		iud1.setId("TestOr");
		iud1.setVersion(Version.create("1.0.0"));
		RequiredCapability req1 = new RequiredCapability(NS, N, new VersionRange("[1.1.0, 1.2.0)"), null, false, false);
		RequiredCapability req2 = new RequiredCapability(NS, N, new VersionRange("[1.3.0, 1.4.0)"), null, false, false);
		Collection requirements = new ArrayList();
		requirements.add(new ORRequirement(new IRequiredCapability[] {req1, req2}));
		iud1.addRequiredCapabilities(requirements);
		Collection capabilities = new ArrayList();
		capabilities.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "TestOr", Version.create("1.0.0")));
		iud1.addProvidedCapabilities(capabilities);
		IInstallableUnit iu1 = MetadataFactory.createInstallableUnit(iud1);

		MetadataFactory.InstallableUnitDescription iud2 = new MetadataFactory.InstallableUnitDescription();
		iud2.setId("ProviderOf1_0_0");
		iud2.setVersion(Version.create("1.0.0"));
		Collection capabilities2 = new ArrayList();
		capabilities2.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.0.0")));
		iud2.addProvidedCapabilities(capabilities2);
		IInstallableUnit iu2 = MetadataFactory.createInstallableUnit(iud2);

		MetadataFactory.InstallableUnitDescription iud3 = new MetadataFactory.InstallableUnitDescription();
		iud3.setId("ProviderOf1_1_1");
		iud3.setVersion(Version.create("1.0.0"));
		Collection capabilities3 = new ArrayList();
		capabilities3.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.1.1")));
		capabilities3.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "ProviderOf1_1_1", Version.create("1.0.0")));
		iud3.addProvidedCapabilities(capabilities3);
		IInstallableUnit iu3 = MetadataFactory.createInstallableUnit(iud3);

		MetadataFactory.InstallableUnitDescription iud4 = new MetadataFactory.InstallableUnitDescription();
		iud4.setId("ProviderOf1_3_0");
		iud4.setVersion(Version.create("1.0.0"));
		Collection capabilities4 = new ArrayList();
		capabilities4.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.3.0")));
		capabilities4.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "ProviderOf1_3_0", Version.create("1.0.0")));
		iud4.addProvidedCapabilities(capabilities4);
		IInstallableUnit iu4 = MetadataFactory.createInstallableUnit(iud4);

		IMetadataRepository repo = createTestMetdataRepository(new IInstallableUnit[] {iu1, iu2, iu3, iu4});

		Slicer slicer = new Slicer(repo, null, false);
		IQueryable slice = slicer.slice(new IInstallableUnit[] {iu1}, new NullProgressMonitor());
		Collector c = slice.query(InstallableUnitQuery.ANY, new Collector(), null);
		assertEquals(3, c.size());

		IPlanner planner = createPlanner();
		IProfile profile = createProfile("testOR3");
		ProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);
		changeRequest.addInstallableUnits(new IInstallableUnit[] {iu1});
		IProvisioningPlan plan = planner.getProvisioningPlan(changeRequest, null, null);
		assertOK("Plan OK", plan.getStatus());
		//		assertEquals(1, plan.getCompleteState().query(new InstallableUnitQuery(id), collector, monitor))
	}

	public void testOR4() {
		IInstallableUnit a1 = createIU("A", Version.create("1.0.0"), true);
		IInstallableUnit a2 = createIU("A", Version.create("2.0.0"), true);

		MetadataFactory.InstallableUnitDescription iud1 = new MetadataFactory.InstallableUnitDescription();
		iud1.setId("TestOr");
		iud1.setVersion(Version.create("1.0.0"));
		RequiredCapability req1 = new RequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.2.0)"), null, false, false);
		RequiredCapability req2 = new RequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 2.2.0)"), null, false, false);

		Collection requirements = new ArrayList();
		requirements.add(new ORRequirement(new IRequiredCapability[] {req1, req2}));
		iud1.addRequiredCapabilities(requirements);
		Collection capabilities = new ArrayList();
		capabilities.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "TestOr", Version.create("1.0.0")));
		iud1.addProvidedCapabilities(capabilities);
		IInstallableUnit iu1 = MetadataFactory.createInstallableUnit(iud1);

		createTestMetdataRepository(new IInstallableUnit[] {iu1, a2, a1});

		IPlanner planner = createPlanner();
		IProfile profile = createProfile("TestOr4");
		ProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);
		changeRequest.addInstallableUnits(new IInstallableUnit[] {iu1});
		IProvisioningPlan plan = planner.getProvisioningPlan(changeRequest, null, null);
		assertOK("Plan OK", plan.getStatus());
		//		assertEquals(1, plan.getCompleteState().query(new InstallableUnitQuery(id), collector, monitor))
	}

	public void testOR5() {
		MetadataFactory.InstallableUnitDescription iud1 = new MetadataFactory.InstallableUnitDescription();
		iud1.setId("TestOr");
		iud1.setVersion(Version.create("1.0.0"));
		RequiredCapability req1 = new RequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.2.0)"), null, false, false);
		RequiredCapability req2 = new RequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 2.2.0)"), null, false, false);
		Collection requirements = new ArrayList();
		requirements.add(new ORRequirement(new IRequiredCapability[] {req1, req2}));
		iud1.addRequiredCapabilities(requirements);
		Collection capabilities = new ArrayList();
		capabilities.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "TestOr", Version.create("1.0.0")));
		iud1.addProvidedCapabilities(capabilities);
		IInstallableUnit iu1 = MetadataFactory.createInstallableUnit(iud1);

		createTestMetdataRepository(new IInstallableUnit[] {iu1});

		IPlanner planner = createPlanner();
		IProfile profile = createProfile("TestOr4");
		ProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);
		changeRequest.addInstallableUnits(new IInstallableUnit[] {iu1});
		IProvisioningPlan plan = planner.getProvisioningPlan(changeRequest, null, null);
		assertNotOK("Plan Not OK", plan.getStatus());
		//		assertEquals(1, plan.getCompleteState().query(new InstallableUnitQuery(id), collector, monitor))
	}
}
