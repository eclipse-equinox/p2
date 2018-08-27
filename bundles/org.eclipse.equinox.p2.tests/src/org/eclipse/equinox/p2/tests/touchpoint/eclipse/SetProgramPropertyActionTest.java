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
import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.SetProgramPropertyAction;
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

public class SetProgramPropertyActionTest extends AbstractProvisioningTest {

	public SetProgramPropertyActionTest(String name) {
		super(name);
	}

	public SetProgramPropertyActionTest() {
		super("");
	}

	public void testExecuteUndo() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		Map<String, String> profileProperties = new HashMap<>();
		profileProperties.put(IProfile.PROP_INSTALL_FOLDER, getTempFolder().toString());
		IProfile profile = createProfile("test", profileProperties);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, createIU("test"));
		touchpoint.initializePhase(null, profile, "test", parameters);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, parameters);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);

		String frameworkDependentPropertyName = "test";
		String frameworkDependentPropertyValue = "true";
		assertFalse(manipulator.getConfigData().getProperties().containsKey(frameworkDependentPropertyName));
		parameters.put(ActionConstants.PARM_PROP_NAME, frameworkDependentPropertyName);
		parameters.put(ActionConstants.PARM_PROP_VALUE, frameworkDependentPropertyValue);
		parameters = Collections.unmodifiableMap(parameters);

		SetProgramPropertyAction action = new SetProgramPropertyAction();
		action.execute(parameters);
		assertEquals("true", manipulator.getConfigData().getProperty(frameworkDependentPropertyName));
		action.undo(parameters);
		assertFalse(manipulator.getConfigData().getProperties().containsKey(frameworkDependentPropertyName));
	}

	public void testExecuteUndoWithArtifact() throws Exception {
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
		parameters.put("artifact", key);
		touchpoint.initializeOperand(profile, parameters);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);

		String resolvedArtifact = osgiTarget.getAbsolutePath();
		assertFalse(Arrays.asList(manipulator.getLauncherData().getProgramArgs()).contains(resolvedArtifact));
		parameters.put(ActionConstants.PARM_PROP_NAME, "test");
		parameters.put(ActionConstants.PARM_PROP_VALUE, "@artifact");

		parameters = Collections.unmodifiableMap(parameters);

		SetProgramPropertyAction action = new SetProgramPropertyAction();

		action.execute(parameters);
		assertTrue(manipulator.getConfigData().getProperty("test").equals(resolvedArtifact));
		action.undo(parameters);
		assertFalse(manipulator.getConfigData().getProperties().containsKey("test"));

		action.execute(parameters);
		assertTrue(manipulator.getConfigData().getProperty("test").equals(resolvedArtifact));
		parameters = new HashMap<>(parameters);
		parameters.remove(ActionConstants.PARM_PROP_VALUE);
		parameters = Collections.unmodifiableMap(parameters);
		action.execute(parameters);
		assertFalse(manipulator.getConfigData().getProperties().containsKey("test"));
		action.undo(parameters);
		assertTrue(manipulator.getConfigData().getProperty("test").equals(resolvedArtifact));
	}

	public void testExecuteUndoWithArtifactLocation() throws Exception {
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
		parameters.put("artifact", key);
		touchpoint.initializeOperand(profile, parameters);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);

		String resolvedArtifact = osgiTarget.getAbsolutePath();
		assertFalse(Arrays.asList(manipulator.getLauncherData().getProgramArgs()).contains(resolvedArtifact));
		parameters.put(ActionConstants.PARM_PROP_NAME, "test");
		parameters.put(ActionConstants.PARM_PROP_VALUE, parameters.get("artifact.location"));

		parameters = Collections.unmodifiableMap(parameters);

		SetProgramPropertyAction action = new SetProgramPropertyAction();

		action.execute(parameters);
		assertTrue(manipulator.getConfigData().getProperty("test").equals(resolvedArtifact));
		action.undo(parameters);
		assertFalse(manipulator.getConfigData().getProperties().containsKey("test"));

		action.execute(parameters);
		assertTrue(manipulator.getConfigData().getProperty("test").equals(resolvedArtifact));
		parameters = new HashMap<>(parameters);
		parameters.remove(ActionConstants.PARM_PROP_VALUE);
		parameters = Collections.unmodifiableMap(parameters);
		action.execute(parameters);
		assertFalse(manipulator.getConfigData().getProperties().containsKey("test"));
		action.undo(parameters);
		assertTrue(manipulator.getConfigData().getProperty("test").equals(resolvedArtifact));
	}

}