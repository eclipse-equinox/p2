package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import java.net.URI;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ExplanationLargeConflict extends AbstractProvisioningTest {
	IMetadataRepository repo1;
	IMetadataRepository repo2;
	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		repo1 = loadMetadataRepository(getTestData("repo1", "testData/testLargeConflict/repo1").toURI());
		repo2 = loadMetadataRepository(getTestData("repo2", "testData/testLargeConflict/repo2").toURI());

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testExplanation() {
		Collector c = repo1.query(new InstallableUnitQuery("org.eclipse.jdt.feature.group"), new Collector(), null);
		assertEquals(1, c.size());
		IInstallableUnit jdt1 = (IInstallableUnit) c.iterator().next();

		Collector c2 = repo2.query(new InstallableUnitQuery("org.eclipse.jdt.feature.group"), new Collector(), null);
		assertEquals(1, c2.size());
		IInstallableUnit jdt2 = (IInstallableUnit) c2.iterator().next();

		assertNotSame(jdt1, jdt2);

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {jdt1, jdt2});
		ProvisioningPlan plan = planner.getProvisioningPlan(pcr, new ProvisioningContext(new URI[] {getTestData("repo1", "testData/testLargeConflict/repo1").toURI(), getTestData("repo2", "testData/testLargeConflict/repo2").toURI()}), null);
		assertNotOK(plan.getStatus());
		System.out.println(plan.getRequestStatus().getExplanations());
	}

	public void testExplanationOverLargeInstall() {
		File reporegistry1 = getTestData("test data explanation large conflict", "testData/testLargeConflict/p2/org.eclipse.equinox.p2.engine/profileRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", reporegistry1, tempFolder);
		SimpleProfileRegistry registry = new SimpleProfileRegistry(tempFolder, null, false);
		IProfile SDKprofile = registry.getProfile("SDKProfile");
		assertNotNull(profile);

		Collector c = repo2.query(new InstallableUnitQuery("org.eclipse.cvs.feature.group"), new Collector(), null);
		assertEquals(1, c.size());
		IInstallableUnit cvs = (IInstallableUnit) c.iterator().next();

		ProfileChangeRequest pcr = new ProfileChangeRequest(SDKprofile);
		pcr.addInstallableUnits(new IInstallableUnit[] {cvs});
		ProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, null);
		assertNotOK(plan.getStatus());
		System.out.println(plan.getRequestStatus().getExplanations());
	}
}
