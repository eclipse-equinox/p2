package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug254481dataSet2 extends AbstractProvisioningTest {
	IProfile profile = null;
	IMetadataRepository repo = null;

	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("test data bug 254481", "testData/bug254481/dataSet2/p2/org.eclipse.equinox.p2.engine/profileRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", reporegistry1, tempFolder);
		SimpleProfileRegistry registry = new SimpleProfileRegistry(tempFolder, null, false);
		profile = registry.getProfile("bootProfile");
		assertNotNull(profile);
		repo = getMetadataRepositoryManager().loadRepository(getTestData("test data bug 254481", "testData/bug254481/dataSet2/repo").toURI(), null);
		assertNotNull(repo);
	}

	protected void tearDown() throws Exception {
		getMetadataRepositoryManager().removeRepository(getTestData("test data bug 254481", "testData/bug254481/dataSet2/repo").toURI());
		super.tearDown();
	}

	public void testInstallFeaturePatch() {
		Collector c = repo.query(new InstallableUnitQuery("org.eclipse.jdt.feature.patch.feature.group"), new Collector(), new NullProgressMonitor());
		assertEquals(1, c.size());
		IInstallableUnit patch = (IInstallableUnit) c.iterator().next();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {patch});
		request.setInstallableUnitInclusionRules(patch, PlannerHelper.createOptionalInclusionRule(patch));
		IPlanner planner = createPlanner();
		ProvisioningPlan plan = planner.getProvisioningPlan(request, null, new NullProgressMonitor());
		assertInstallOperand(plan, patch);
		assertEquals(1, plan.getAdditions().query(new InstallableUnitQuery("org.eclipse.jdt.core"), new Collector(), null).size());
		assertEquals(1, plan.getAdditions().query(new InstallableUnitQuery("org.eclipse.jdt.core.manipulation"), new Collector(), null).size());
	}
}
