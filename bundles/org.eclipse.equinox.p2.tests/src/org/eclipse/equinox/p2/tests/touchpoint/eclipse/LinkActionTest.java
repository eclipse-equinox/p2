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
import java.util.*;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.LinkAction;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class LinkActionTest extends AbstractProvisioningTest {

	public LinkActionTest(String name) {
		super(name);
	}

	public LinkActionTest() {
		super("");
	}

	public void testExecuteUndo() {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		File zipSource = getTestData("1.0", "/testData/nativeTouchpoint/a.zip");
		File zipTarget = new File(installFolder, "a.zip");
		copy("2.0", zipSource, zipTarget);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);

		parameters.put(ActionConstants.PARM_TARGET_DIR, installFolder.getAbsolutePath());
		parameters.put(ActionConstants.PARM_LINK_NAME, "b.zip");
		parameters.put(ActionConstants.PARM_LINK_TARGET, zipTarget.getAbsolutePath());
		parameters = Collections.unmodifiableMap(parameters);

		// TODO: We need a way to verify
		// one idea is to run an executable here
		// This is currently just going through the paces to check for any runtime exceptions
		LinkAction action = new LinkAction();
		action.execute(parameters);
		// does nothing so should not alter parameters
		action.undo(parameters);
	}

	public void testExecuteUndoWithArtifact() throws Exception {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		profileProperties.setProperty(IProfile.PROP_CACHE, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		IFileArtifactRepository bundlePool = Util.getBundlePoolRepository(getAgent(), profile);
		File dirBundleSource = getTestData("1.0", "/testData/eclipseTouchpoint/bundles/directoryBased_1.0.0");
		File targetPlugins = new File(installFolder, "plugins");
		assertTrue(targetPlugins.mkdir());
		File dirBundleTarget = new File(targetPlugins, "directoryBased_1.0.0");
		copy("2.0", dirBundleSource, dirBundleTarget);

		BundleDescription bundleDescription = BundlesAction.createBundleDescription(dirBundleTarget);
		IArtifactKey key = BundlesAction.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		ArtifactDescriptor descriptor = (ArtifactDescriptor) PublisherHelper.createArtifactDescriptor(key, dirBundleTarget);
		descriptor.setProperty("artifact.folder", Boolean.TRUE.toString());
		IInstallableUnit iu = createBundleIU(bundleDescription, dirBundleTarget.isDirectory(), key);
		bundlePool.addDescriptor(descriptor);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, iu);
		parameters.put("iu", operand.second());
		parameters.put("artifact", key);
		touchpoint.initializeOperand(profile, parameters);

		parameters.put(ActionConstants.PARM_TARGET_DIR, "@artifact");
		parameters.put(ActionConstants.PARM_LINK_NAME, "plugin.xml.link");
		parameters.put(ActionConstants.PARM_LINK_TARGET, "plugin.xml");
		parameters = Collections.unmodifiableMap(parameters);

		// TODO: We need a way to verify
		// one idea is to run an executable here
		// This is currently just going through the paces to check for any runtime exceptions
		LinkAction action = new LinkAction();
		action.execute(parameters);
		// does nothing so should not alter parameters
		action.undo(parameters);
	}

	public void testExecuteUndoWithArtifactLocation() throws Exception {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		profileProperties.setProperty(IProfile.PROP_CACHE, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		IFileArtifactRepository bundlePool = Util.getBundlePoolRepository(getAgent(), profile);
		File dirBundleSource = getTestData("1.0", "/testData/eclipseTouchpoint/bundles/directoryBased_1.0.0");
		File targetPlugins = new File(installFolder, "plugins");
		assertTrue(targetPlugins.mkdir());
		File dirBundleTarget = new File(targetPlugins, "directoryBased_1.0.0");
		copy("2.0", dirBundleSource, dirBundleTarget);

		BundleDescription bundleDescription = BundlesAction.createBundleDescription(dirBundleTarget);
		IArtifactKey key = BundlesAction.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		ArtifactDescriptor descriptor = (ArtifactDescriptor) PublisherHelper.createArtifactDescriptor(key, dirBundleTarget);
		descriptor.setProperty("artifact.folder", Boolean.TRUE.toString());
		IInstallableUnit iu = createBundleIU(bundleDescription, dirBundleTarget.isDirectory(), key);
		bundlePool.addDescriptor(descriptor);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, iu);
		parameters.put("iu", operand.second());
		parameters.put("artifact", key);
		touchpoint.initializeOperand(profile, parameters);

		parameters.put(ActionConstants.PARM_TARGET_DIR, parameters.get("artifact.location"));
		parameters.put(ActionConstants.PARM_LINK_NAME, "plugin.xml.link");
		parameters.put(ActionConstants.PARM_LINK_TARGET, "plugin.xml");
		parameters = Collections.unmodifiableMap(parameters);

		// TODO: We need a way to verify
		// one idea is to run an executable here
		// This is currently just going through the paces to check for any runtime exceptions
		LinkAction action = new LinkAction();
		action.execute(parameters);
		// does nothing so should not alter parameters
		action.undo(parameters);
	}
}
