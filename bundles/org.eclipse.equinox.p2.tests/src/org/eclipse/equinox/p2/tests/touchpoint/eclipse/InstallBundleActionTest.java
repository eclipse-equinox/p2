/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.InstallBundleAction;
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

public class InstallBundleActionTest extends AbstractProvisioningTest {

	public InstallBundleActionTest(String name) {
		super(name);
	}

	public InstallBundleActionTest() {
		super("");
	}

	public void testExecuteUndo() throws Exception {
		Map<String, String> profileProperties = new HashMap<>();
		File installFolder = getTempFolder();
		profileProperties.put(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		profileProperties.put(IProfile.PROP_CACHE, installFolder.toString());
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
		bundlePool.addDescriptor(descriptor, new NullProgressMonitor());

		Map<String, Object> parameters = new HashMap<>();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, iu);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, parameters);

		parameters.put(ActionConstants.PARM_BUNDLE, key.toString());
		parameters = Collections.unmodifiableMap(parameters);

		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);

		assertFalse(inBundles(manipulator, osgiTarget));
		InstallBundleAction action = new InstallBundleAction();
		action.execute(parameters);
		assertTrue(inBundles(manipulator, osgiTarget));
		action.undo(parameters);
		assertFalse(inBundles(manipulator, osgiTarget));
	}

	private boolean inBundles(Manipulator manipulator, File osgiTarget) {
		URI location = osgiTarget.toURI();
		BundleInfo[] bundles = manipulator.getConfigData().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (location.equals(bundles[i].getLocation()))
				return true;
		}
		return false;
	}

}