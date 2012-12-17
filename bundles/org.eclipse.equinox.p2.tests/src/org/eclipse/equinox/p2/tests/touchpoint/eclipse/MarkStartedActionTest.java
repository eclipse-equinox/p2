/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
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
import java.net.URI;
import java.util.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.MarkStartedAction;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class MarkStartedActionTest extends AbstractProvisioningTest {

	public MarkStartedActionTest(String name) {
		super(name);
	}

	public MarkStartedActionTest() {
		super("");
	}

	public void testExecuteUndo() throws Exception {
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
		IInstallableUnit iu = createBundleIU(bundleDescription, osgiTarget.isDirectory(), key);
		bundlePool.addDescriptor(descriptor);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, iu);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, parameters);

		parameters.put(ActionConstants.PARM_STARTED, Boolean.TRUE.toString());
		parameters = Collections.unmodifiableMap(parameters);

		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);

		BundleInfo bundleInfo = Util.createBundleInfo(osgiTarget, iu);
		manipulator.getConfigData().addBundle(bundleInfo);
		assertTrue(isMarkedStarted(manipulator, osgiTarget, false));

		MarkStartedAction action = new MarkStartedAction();
		action.execute(parameters);
		assertTrue(isMarkedStarted(manipulator, osgiTarget, true));
		action.undo(parameters);
		assertTrue(isMarkedStarted(manipulator, osgiTarget, false));
	}

	public void testExecuteUndoWithMissingArtifact() throws Exception {
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
		IInstallableUnit iu = createBundleIU(bundleDescription, osgiTarget.isDirectory(), key);
		bundlePool.addDescriptor(descriptor);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, iu);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, parameters);

		parameters.put(ActionConstants.PARM_STARTED, Boolean.TRUE.toString());
		parameters = Collections.unmodifiableMap(parameters);

		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);

		BundleInfo bundleInfo = Util.createBundleInfo(osgiTarget, iu);
		manipulator.getConfigData().addBundle(bundleInfo);
		assertTrue(isMarkedStarted(manipulator, osgiTarget, false));

		// let's remove the artifact now
		bundlePool.removeDescriptor(descriptor);

		MarkStartedAction action = new MarkStartedAction();
		action.execute(parameters);
		assertTrue(isMarkedStarted(manipulator, osgiTarget, true));
		action.undo(parameters);
		assertTrue(isMarkedStarted(manipulator, osgiTarget, false));
	}

	public void testExecuteOnFragmentBundleResultsInBundleNotBeingMarkedStarted() throws Exception {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		profileProperties.setProperty(IProfile.PROP_CACHE, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		IFileArtifactRepository bundlePool = Util.getBundlePoolRepository(getAgent(), profile);
		File osgiSource = getTestData("1.0", "/testData/eclipseTouchpoint/bundles/org.eclipse.osgi.fragment_1.0.0.jar");
		File targetPlugins = new File(installFolder, "plugins");
		assertTrue(targetPlugins.mkdir());
		File osgiTarget = new File(targetPlugins, "org.eclipse.osgi.fragment_1.0.0.jar");
		copy("2.0", osgiSource, osgiTarget);

		BundleDescription bundleDescription = BundlesAction.createBundleDescription(osgiTarget);
		IArtifactKey key = BundlesAction.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, osgiTarget);
		IInstallableUnit iu = createBundleIU(bundleDescription, osgiTarget.isDirectory(), key);
		bundlePool.addDescriptor(descriptor);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, iu);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, parameters);

		parameters.put(ActionConstants.PARM_STARTED, Boolean.TRUE.toString());
		parameters = Collections.unmodifiableMap(parameters);

		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);

		BundleInfo bundleInfo = Util.createBundleInfo(osgiTarget, iu);
		manipulator.getConfigData().addBundle(bundleInfo);
		assertTrue(isMarkedStarted(manipulator, osgiTarget, false));

		MarkStartedAction action = new MarkStartedAction();
		action.execute(parameters);
		assertTrue(isMarkedStarted(manipulator, osgiTarget, false));
		action.undo(parameters);
		assertTrue(isMarkedStarted(manipulator, osgiTarget, false));
	}

	private boolean isMarkedStarted(Manipulator manipulator, File osgiTarget, boolean started) {
		URI location = osgiTarget.toURI();
		BundleInfo[] bundles = manipulator.getConfigData().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (location.equals(bundles[i].getLocation()) && (started == bundles[i].isMarkedAsStarted()))
				return true;
		}
		return false;
	}
}