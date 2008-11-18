package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class EclipseTouchpointTest extends AbstractProvisioningTest {

	public EclipseTouchpointTest(String name) {
		super(name);
	}

	public EclipseTouchpointTest() {
		super("");
	}

	public void testInitializeCompletePhaseCommit() {
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();

		Map parameters = new HashMap();
		IProfile profile = createProfile("test");

		touchpoint.initializePhase(null, profile, "test", parameters);
		assertNull(parameters.get(EclipseTouchpoint.PARM_INSTALL_FOLDER));
		Object manipulator = parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);
		assertNotNull(parameters.get(EclipseTouchpoint.PARM_SOURCE_BUNDLES));
		assertNotNull(parameters.get(EclipseTouchpoint.PARM_PLATFORM_CONFIGURATION));
		touchpoint.completePhase(null, profile, "test", parameters);

		// checking that the manipulator is carried from phases to phase
		parameters.clear();
		touchpoint.initializePhase(null, profile, "test2", parameters);
		Object testManipulator = parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertEquals(manipulator, testManipulator);
		touchpoint.completePhase(null, profile, "test2", parameters);

		// re: "uninstall" this is necessary for now for coverage until we have formal commit and rollback events
		// this test should be revisited then
		parameters.clear();
		touchpoint.initializePhase(null, profile, "uninstall", parameters);
		testManipulator = parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertEquals(manipulator, testManipulator);
		touchpoint.completePhase(null, profile, "uninstall", parameters);

		// this will save the manipulator and remove it from the set of tracked manipulators
		touchpoint.commit(profile);

		touchpoint.initializePhase(null, profile, "test2", parameters);
		testManipulator = parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotSame(manipulator, testManipulator);
	}

	public void testQualifyAction() {
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		assertEquals("org.eclipse.equinox.p2.touchpoint.eclipse.installBundle", touchpoint.qualifyAction("installBundle"));
	}

	public void testInitializeCompleteOperand() {
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		Map parameters = new HashMap();
		IProfile profile = createProfile("test");
		Operand operand = new InstallableUnitOperand(null, createIU("test"));

		// need a partial iu test here
		touchpoint.initializeOperand(profile, operand, parameters);
		touchpoint.completeOperand(profile, operand, parameters);
	}

	public void testPrepareIU() {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		profileProperties.setProperty(IProfile.PROP_CACHE, installFolder.toString());
		IProfile profile = createProfile("test", null, profileProperties);

		IFileArtifactRepository bundlePool = Util.getBundlePoolRepository(profile);
		File osgiSource = getTestData("1.0", "/testData/eclipseTouchpoint/bundles/org.eclipse.osgi_3.4.2.R34x_v20080826-1230.jar");
		File targetPlugins = new File(installFolder, "plugins");
		assertTrue(targetPlugins.mkdir());
		File osgiTarget = new File(targetPlugins, "org.eclipse.osgi_3.4.2.R34x_v20080826-1230.jar");
		copy("2.0", osgiSource, osgiTarget);

		BundleDescription bundleDescription = BundlesAction.createBundleDescription(osgiTarget);
		IArtifactKey key = BundlesAction.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, osgiTarget);
		bundlePool.addDescriptor(descriptor);

		Properties extraProperties = new Properties();
		extraProperties.put(IInstallableUnit.PROP_PARTIAL_IU, Boolean.TRUE.toString());

		Dictionary mockManifest = new Properties();
		mockManifest.put("Manifest-Version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
		mockManifest.put("Bundle-ManifestVersion", "2"); //$NON-NLS-1$ //$NON-NLS-2$
		mockManifest.put("Bundle-SymbolicName", key.getId()); //$NON-NLS-1$
		mockManifest.put("Bundle-Version", key.getVersion().toString()); //$NON-NLS-1$

		BundleDescription partialIUbundleDescription = BundlesAction.createBundleDescription(mockManifest, null);
		IInstallableUnit[] bundleIUs = PublisherHelper.createEclipseIU(partialIUbundleDescription, null, false, key, extraProperties);
		assertTrue(bundleIUs != null && bundleIUs.length != 0);
		IInstallableUnit iu = bundleIUs[0];
		assertTrue(Boolean.valueOf(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU)).booleanValue());
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		IInstallableUnit fullIU = touchpoint.prepareIU(iu, profile);
		assertFalse(Boolean.valueOf(fullIU.getProperty(IInstallableUnit.PROP_PARTIAL_IU)).booleanValue());
	}

}
