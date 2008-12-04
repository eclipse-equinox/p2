package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import java.util.ArrayList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class SDKPatchingTest2 extends AbstractProvisioningTest {
	IProfile profile = null;
	ArrayList newIUs = new ArrayList();
	IInstallableUnit patchInstallingJDTLaunching = null;
	IInstallableUnit patchInstallingDebugUI = null;

	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("test data for sdkpatching test", "testData/sdkpatchingtest/p2/org.eclipse.equinox.p2.engine/profileRegistry");
		SimpleProfileRegistry registry = new SimpleProfileRegistry(reporegistry1, null, false);
		profile = registry.getProfile("SDKProfile");
		assertNotNull(profile);

		//create a patch to install a new version of jdt.launching
		MetadataFactory.InstallableUnitDescription newJDTLaunching = createIUDescriptor((IInstallableUnit) profile.query(new InstallableUnitQuery("org.eclipse.jdt.launching"), new Collector(), new NullProgressMonitor()).iterator().next());
		Version newJDTLaunchingVersion = new Version(3, 5, 0, "zeNewVersion");
		changeVersion(newJDTLaunching, newJDTLaunchingVersion);
		newIUs.add(MetadataFactory.createInstallableUnit(newJDTLaunching));

		RequirementChange change = new RequirementChange(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.jdt.launching", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.jdt.launching", new VersionRange(newJDTLaunchingVersion, true, newJDTLaunchingVersion, true), null, false, false, true));
		RequiredCapability lifeCycle = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.jdt.feature.group", new VersionRange("[3.5.0.v20081202-0800-7p83FGDFHmHuj2mNpJBSKZe, 3.5.0.v20081202-0800-7p83FGDFHmHuj2mNpJBSKZe]"), null, false, false, true);
		patchInstallingJDTLaunching = createIUPatch("P", new Version("1.0.0"), true, new RequirementChange[] {change}, new RequiredCapability[0][0], lifeCycle);

		newIUs.add(patchInstallingJDTLaunching);

		//create a patch to install a new version of jdt.debug.ui
		MetadataFactory.InstallableUnitDescription newDebugUI = createIUDescriptor((IInstallableUnit) profile.query(new InstallableUnitQuery("org.eclipse.jdt.debug.ui"), new Collector(), new NullProgressMonitor()).iterator().next());
		Version newDebugVersion = new Version(3, 3, 0, "zeNewVersion");
		changeVersion(newDebugUI, newDebugVersion);
		newIUs.add(MetadataFactory.createInstallableUnit(newDebugUI));

		RequirementChange change2 = new RequirementChange(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.jdt.debug.ui", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.jdt.debug.ui", new VersionRange(newDebugVersion, true, newDebugVersion, true), null, false, false, true));
		RequiredCapability lifeCycle2 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.jdt.feature.group", new VersionRange("[3.5.0.v20081202-0800-7p83FGDFHmHuj2mNpJBSKZe, 3.5.0.v20081202-0800-7p83FGDFHmHuj2mNpJBSKZe]"), null, false, false, true);
		patchInstallingDebugUI = createIUPatch("P2", new Version("1.0.0"), true, new RequirementChange[] {change2}, new RequiredCapability[0][0], lifeCycle2);

		newIUs.add(patchInstallingDebugUI);

	}

	public void testInstallFeaturePatch() {
		ProvisioningContext ctx = new ProvisioningContext();
		ctx.setExtraIUs(newIUs);
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {patchInstallingJDTLaunching, patchInstallingDebugUI});
		request.setInstallableUnitInclusionRules(patchInstallingJDTLaunching, PlannerHelper.createOptionalInclusionRule(patchInstallingJDTLaunching));
		request.setInstallableUnitInclusionRules(patchInstallingDebugUI, PlannerHelper.createOptionalInclusionRule(patchInstallingDebugUI));
		IPlanner planner = createPlanner();
		ProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertOK("Installation plan", plan.getStatus());
		assertEquals(8, plan.getOperands().length);
	}
}
