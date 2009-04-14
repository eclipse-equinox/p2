package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import java.lang.reflect.Field;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

public class Bug271067 extends AbstractProvisioningTest {
	private IProfile profile;
	private File previousStoreValue = null;
	String profileLoadedId = "bootProfile";

	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("test data bug 271067", "testData/bug271067/p2/org.eclipse.equinox.p2.engine/profileRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", reporegistry1, tempFolder);
		SimpleProfileRegistry realProfileRegistry = (SimpleProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		//Tweak the running profile registry
		Field profilesMapField = SimpleProfileRegistry.class.getDeclaredField("profiles"); //$NON-NLS-1$
		profilesMapField.setAccessible(true);
		profilesMapField.set(realProfileRegistry, null);

		Field profileStore = SimpleProfileRegistry.class.getDeclaredField("store");
		profileStore.setAccessible(true);
		previousStoreValue = (File) profileStore.get(realProfileRegistry);
		profileStore.set(realProfileRegistry, tempFolder);
		//End of tweaking the profile registry

		profile = realProfileRegistry.getProfile(profileLoadedId);
		assertNotNull(profile);
		loadMetadataRepository(getTestData("Repository for 271067", "testData/bug271067/").toURI());
	}

	@Override
	protected void tearDown() throws Exception {
		SimpleProfileRegistry realProfileRegistry = (SimpleProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());

		Field profilesMapField = SimpleProfileRegistry.class.getDeclaredField("profiles"); //$NON-NLS-1$
		profilesMapField.setAccessible(true);
		profilesMapField.set(realProfileRegistry, null);

		Field profileStore = SimpleProfileRegistry.class.getDeclaredField("store");
		profileStore.setAccessible(true);
		previousStoreValue = (File) profileStore.get(realProfileRegistry);
		profileStore.set(realProfileRegistry, previousStoreValue);
		super.tearDown();
	}

	public void testInstallFeaturePatch() {
		Collector c = getMetadataRepositoryManager().query(new InstallableUnitQuery("hello.feature.1.feature.group"), new Collector(), new NullProgressMonitor());
		assertEquals(1, c.size());
		Collector c2 = getMetadataRepositoryManager().query(new InstallableUnitQuery("hello.patch.feature.group"), new Collector(), new NullProgressMonitor());
		assertEquals(1, c2.size());
		ProfileChangeRequest installFeature1 = new ProfileChangeRequest(profile);
		installFeature1.addInstallableUnits(new IInstallableUnit[] {(IInstallableUnit) c.iterator().next(), (IInstallableUnit) c2.iterator().next()});
		installFeature1.setInstallableUnitInclusionRules((IInstallableUnit) c.iterator().next(), PlannerHelper.createOptionalInclusionRule((IInstallableUnit) c.iterator().next()));
		installFeature1.setInstallableUnitInclusionRules((IInstallableUnit) c2.iterator().next(), PlannerHelper.createOptionalInclusionRule((IInstallableUnit) c2.iterator().next()));
		ProvisioningPlan feature1Plan = createPlanner().getProvisioningPlan(installFeature1, new ProvisioningContext(), null);
		assertOK("installation of feature1 and patch", createEngine().perform(getProfile(profileLoadedId), new DefaultPhaseSet(), feature1Plan.getOperands(), new ProvisioningContext(), new NullProgressMonitor()));
		assertEquals(1, getProfile(profileLoadedId).query(new InstallableUnitQuery("hello", new Version("1.0.0.1")), new Collector(), new NullProgressMonitor()).size());

		Collector c3 = getMetadataRepositoryManager().query(new InstallableUnitQuery("hello.feature.2.feature.group"), new Collector(), new NullProgressMonitor());
		assertEquals(1, c3.size());
		ProfileChangeRequest installFeature2 = new ProfileChangeRequest(profile);
		installFeature2.addInstallableUnits(new IInstallableUnit[] {(IInstallableUnit) c3.iterator().next()});
		installFeature2.setInstallableUnitInclusionRules((IInstallableUnit) c3.iterator().next(), PlannerHelper.createOptionalInclusionRule((IInstallableUnit) c.iterator().next()));
		ProvisioningPlan feature2Plan = createPlanner().getProvisioningPlan(installFeature2, new ProvisioningContext(), null);
		assertOK("installation of feature1 and patch", createEngine().perform(getProfile(profileLoadedId), new DefaultPhaseSet(), feature2Plan.getOperands(), new ProvisioningContext(), new NullProgressMonitor()));
		assertEquals(1, getProfile(profileLoadedId).query(new InstallableUnitQuery("hello", new Version("1.0.0.1")), new Collector(), new NullProgressMonitor()).size());
	}
}
