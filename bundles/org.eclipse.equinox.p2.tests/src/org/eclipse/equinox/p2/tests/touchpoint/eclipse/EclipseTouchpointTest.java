/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.io.File;
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.*;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.EclipsePublisherHelper;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
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
		parameters.put(ActionConstants.PARM_AGENT, getAgent());

		touchpoint.initializePhase(null, profile, "test", parameters);
		Object manipulator = parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);
		assertNotNull(parameters.get(EclipseTouchpoint.PARM_SOURCE_BUNDLES));
		assertNotNull(parameters.get(EclipseTouchpoint.PARM_PLATFORM_CONFIGURATION));
		touchpoint.completePhase(null, profile, "test", parameters);

		// checking that the manipulator is carried from phases to phase
		parameters.clear();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		touchpoint.initializePhase(null, profile, "test2", parameters);
		Object testManipulator = parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertEquals(manipulator, testManipulator);
		touchpoint.completePhase(null, profile, "test2", parameters);

		// re: "uninstall" this is necessary for now for coverage until we have formal commit and rollback events
		// this test should be revisited then
		parameters.clear();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
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

	/**
	 * Tests loading cache extensions from a profile whose install directory contains spaces
	 */
	public void testBug262073() throws MalformedURLException {
		Map properties = new HashMap();
		File site = getTestData("Repository", "/testData/artifactRepo/simple with spaces/");
		//use URL here so spaces are not encoded
		URL spacesLocation = site.toURL();
		site = getTestData("Repositoy", "/testData/artifactRepo/simple/");
		URL location = site.toURL();

		properties.put("org.eclipse.equinox.p2.cache.extensions", location.toString() + "|" + spacesLocation.toString());
		IProfile profile = createProfile("testBug262073", properties);
		AggregatedBundleRepository repo = (AggregatedBundleRepository) Util.getAggregatedBundleRepository(getAgent(), profile);
		Collection repos = repo.testGetBundleRepositories();
		assertEquals("1.0", 3, repos.size());
	}

	public void testInitializeCompleteOperand() {
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		Map parameters = new HashMap();
		IProfile profile = createProfile("test");

		// need a partial iu test here
		touchpoint.initializeOperand(profile, parameters);
		touchpoint.completeOperand(profile, parameters);
	}

	public void testPrepareIU() throws Exception {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		profileProperties.setProperty(IProfile.PROP_CACHE, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		IFileArtifactRepository bundlePool = Util.getBundlePoolRepository(getAgent(), profile);
		File osgiSource = getTestData("1.0", "/testData/eclipseTouchpoint/bundles/org.eclipse.osgi_3.4.2.R34x_v20080826-1230.jar");
		File targetPlugins = new File(installFolder, "plugins");
		assertTrue(targetPlugins.mkdir());
		File osgiTarget = new File(targetPlugins, "org.eclipse.osgi_3.4.2.R34x_v20080826-1230.jar");
		copy("2.0", osgiSource, osgiTarget);

		BundleDescription bundleDescription = BundlesAction.createBundleDescription(osgiTarget);
		IArtifactKey key = BundlesAction.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, osgiTarget);
		bundlePool.addDescriptor(descriptor);

		Map<String, String> extraProperties = new HashMap<String, String>();
		extraProperties.put(IInstallableUnit.PROP_PARTIAL_IU, Boolean.TRUE.toString());

		Dictionary mockManifest = new Properties();
		mockManifest.put("Manifest-Version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
		mockManifest.put("Bundle-ManifestVersion", "2"); //$NON-NLS-1$ //$NON-NLS-2$
		mockManifest.put("Bundle-SymbolicName", key.getId()); //$NON-NLS-1$
		mockManifest.put("Bundle-Version", key.getVersion().toString()); //$NON-NLS-1$

		BundleDescription partialIUBundleDescription = BundlesAction.createBundleDescription(mockManifest, null);
		IInstallableUnit[] bundleIUs = EclipsePublisherHelper.createEclipseIU(partialIUBundleDescription, false, key, extraProperties);
		assertTrue(bundleIUs != null && bundleIUs.length != 0);
		IInstallableUnit iu = bundleIUs[0];
		assertTrue(Boolean.parseBoolean(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU)));
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		IInstallableUnit fullIU = touchpoint.prepareIU(getAgent(), profile, iu, key);
		assertFalse(Boolean.parseBoolean(fullIU.getProperty(IInstallableUnit.PROP_PARTIAL_IU)));
	}

	public void testInstallPartialIU() throws Exception {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		profileProperties.setProperty(IProfile.PROP_CACHE, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		IFileArtifactRepository bundlePool = Util.getBundlePoolRepository(getAgent(), profile);
		File osgiSource = getTestData("1.0", "/testData/eclipseTouchpoint/bundles/org.eclipse.osgi_3.4.2.R34x_v20080826-1230.jar");
		File targetPlugins = new File(installFolder, "plugins");
		assertTrue(targetPlugins.mkdir());
		File osgiTarget = new File(targetPlugins, "org.eclipse.osgi_3.4.2.R34x_v20080826-1230.jar");
		copy("2.0", osgiSource, osgiTarget);

		BundleDescription bundleDescription = BundlesAction.createBundleDescription(osgiTarget);
		IArtifactKey key = BundlesAction.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, osgiTarget);
		bundlePool.addDescriptor(descriptor);

		Map<String, String> extraProperties = new HashMap<String, String>();
		extraProperties.put(IInstallableUnit.PROP_PARTIAL_IU, Boolean.TRUE.toString());

		Dictionary mockManifest = new Properties();
		mockManifest.put("Manifest-Version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
		mockManifest.put("Bundle-ManifestVersion", "2"); //$NON-NLS-1$ //$NON-NLS-2$
		mockManifest.put("Bundle-SymbolicName", key.getId()); //$NON-NLS-1$
		mockManifest.put("Bundle-Version", key.getVersion().toString()); //$NON-NLS-1$

		BundleDescription partialIUBundleDescription = BundlesAction.createBundleDescription(mockManifest, null);
		IInstallableUnit[] bundleIUs = EclipsePublisherHelper.createEclipseIU(partialIUBundleDescription, false, key, extraProperties);
		assertTrue(bundleIUs != null && bundleIUs.length != 0);
		IInstallableUnit iu = bundleIUs[0];
		assertTrue(Boolean.parseBoolean(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU)));

		Iterator iterator = profile.query(QueryUtil.createIUQuery(iu.getId()), null).iterator();
		assertFalse(iterator.hasNext());

		IEngine engine = getEngine();
		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(iu);

		IStatus result = engine.perform(plan, new NullProgressMonitor());
		assertTrue(result.isOK());
		engine = null;

		iterator = profile.query(QueryUtil.createIUQuery(iu.getId()), null).iterator();
		assertTrue(iterator.hasNext());
		IInstallableUnit installedIU = (IInstallableUnit) iterator.next();
		assertTrue(installedIU.getId().equals(iu.getId()));
		assertFalse(Boolean.parseBoolean(installedIU.getProperty(IInstallableUnit.PROP_PARTIAL_IU)));
	}

	public void testInstallPartialIUValidationFailure() throws ProvisionException {

		File installFolder = getTempFolder();
		Properties profileProperties = new Properties();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		profileProperties.setProperty(IProfile.PROP_CACHE, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		URI site = getTestData("0.1", "/testData/updatesite/site").toURI();
		getMetadataRepositoryManager().addRepository(site);
		getArtifactRepositoryManager().addRepository(site);

		IMetadataRepository repo = getMetadataRepositoryManager().loadRepository(site, getMonitor());
		IInstallableUnit iu = repo.query(QueryUtil.createIUQuery("test.bundle"), getMonitor()).iterator().next();
		assertNotNull(iu);
		profile = createProfile("test", profileProperties);
		ProfileChangeRequest request = new ProfileChangeRequest(profile);

		final IInstallableUnit[] newIUs = new IInstallableUnit[] {iu};
		request.addInstallableUnits(newIUs);

		IPlanner planner = createPlanner();
		IProvisioningPlan plan = planner.getProvisioningPlan(request, new ProvisioningContext(getAgent()), new NullProgressMonitor());
		assertTrue("1.0", plan.getStatus().isOK());
		IStatus result = createEngine().perform(plan, getMonitor());
		assertFalse("2.0", result.isOK());
	}

}
