/*******************************************************************************
 *  Copyright (c) 2015 SAP SE and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.natives;

import static org.eclipse.equinox.p2.tests.AbstractProvisioningTest.assertOK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;

import java.io.File;
import java.util.*;
import java.util.prefs.Preferences;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.touchpoint.natives.NativeTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.CheckAndPromptNativePackageWindowsRegistry;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest.ProvisioningTestRuleAdapter;
import org.junit.*;

public class CheckAndPromptNativePackageWindowsRegistryTest {

	@BeforeClass
	public static void classSetUp() throws Exception {
		assumeThat("Windows only: uses Windows registry", Platform.getOS(), equalTo(Platform.OS_WIN32));
	}

	@Rule
	public final ProvisioningTestRuleAdapter testHelper = new ProvisioningTestRuleAdapter();

	private Preferences prefsNode;

	@Before
	public void setUp() throws Exception {
		prefsNode = Preferences.userNodeForPackage(getClass());
		prefsNode.clear();
		prefsNode.flush();
	}

	@Test
	public void execute_StringAttribute() throws Exception {
		String attributeName = "attribute";
		String attributeValue = "value";
		prefsNode.put(attributeName, attributeValue);
		prefsNode.flush();

		Map<String, Object> parameters = new HashMap<String, Object>();
		NativeTouchpoint touchpoint = createTouchpoint(parameters);
		parameters.put(ActionConstants.PARM_LINUX_DISTRO, CheckAndPromptNativePackageWindowsRegistry.WINDOWS_DISTRO);
		parameters.put(ActionConstants.PARM_LINUX_PACKAGE_NAME, "windows package");
		parameters.put(ActionConstants.PARM_LINUX_PACKAGE_VERSION, "1.0");
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_KEY, registryPath(prefsNode));
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_ATTRIBUTE_NAME, attributeName);
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_ATTRIBUTE_VALUE, attributeValue);
		parameters = Collections.unmodifiableMap(parameters);

		ProvisioningAction action = new CheckAndPromptNativePackageWindowsRegistry();
		action.setTouchpoint(touchpoint);

		assertOK(action.execute(parameters));
		assertEquals(Collections.emptyList(), touchpoint.getPackagesToInstall());
	}

	@Test
	public void execute_StringAttribute_DifferentValues() throws Exception {
		String attributeName = "attribute";
		String attributeValue = "value";
		prefsNode.put(attributeName, attributeValue);
		prefsNode.flush();

		Map<String, Object> parameters = new HashMap<String, Object>();
		NativeTouchpoint touchpoint = createTouchpoint(parameters);
		parameters.put(ActionConstants.PARM_LINUX_DISTRO, CheckAndPromptNativePackageWindowsRegistry.WINDOWS_DISTRO);
		parameters.put(ActionConstants.PARM_LINUX_PACKAGE_NAME, "windows package");
		parameters.put(ActionConstants.PARM_LINUX_PACKAGE_VERSION, "1.0");
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_KEY, registryPath(prefsNode));
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_ATTRIBUTE_NAME, attributeName);
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_ATTRIBUTE_VALUE, attributeValue + "_DIFF");
		parameters = Collections.unmodifiableMap(parameters);

		ProvisioningAction action = new CheckAndPromptNativePackageWindowsRegistry();
		action.setTouchpoint(touchpoint);

		assertOK(action.execute(parameters));
		assertEquals(touchpoint.getPackagesToInstall().toString(), 1, touchpoint.getPackagesToInstall().size());
	}

	@Test
	public void execute_IntAttribute() throws Exception {
		String attributeName = "attribute";
		int attributeValue = 1;
		prefsNode.putInt(attributeName, attributeValue);
		prefsNode.flush();

		Map<String, Object> parameters = new HashMap<String, Object>();
		NativeTouchpoint touchpoint = createTouchpoint(parameters);
		parameters.put(ActionConstants.PARM_LINUX_DISTRO, CheckAndPromptNativePackageWindowsRegistry.WINDOWS_DISTRO);
		parameters.put(ActionConstants.PARM_LINUX_PACKAGE_NAME, "windows package");
		parameters.put(ActionConstants.PARM_LINUX_PACKAGE_VERSION, "1.0");
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_KEY, registryPath(prefsNode));
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_ATTRIBUTE_NAME, attributeName);
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_ATTRIBUTE_VALUE, String.valueOf(attributeValue));
		parameters = Collections.unmodifiableMap(parameters);

		ProvisioningAction action = new CheckAndPromptNativePackageWindowsRegistry();
		action.setTouchpoint(touchpoint);

		assertOK(action.execute(parameters));
		assertEquals(Collections.emptyList(), touchpoint.getPackagesToInstall());
	}

	@Test
	public void execute_IntAttribute_DifferentLiteralValue() throws Exception {
		String attributeName = "attribute";
		int attributeValue = 1;
		prefsNode.putInt(attributeName, attributeValue);
		prefsNode.flush();

		Map<String, Object> parameters = new HashMap<String, Object>();
		NativeTouchpoint touchpoint = createTouchpoint(parameters);
		parameters.put(ActionConstants.PARM_LINUX_DISTRO, CheckAndPromptNativePackageWindowsRegistry.WINDOWS_DISTRO);
		parameters.put(ActionConstants.PARM_LINUX_PACKAGE_NAME, "windows package");
		parameters.put(ActionConstants.PARM_LINUX_PACKAGE_VERSION, "1.0");
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_KEY, registryPath(prefsNode));
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_ATTRIBUTE_NAME, attributeName);
		// we simulate a different literal value
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_ATTRIBUTE_VALUE, "0" + String.valueOf(attributeValue));
		parameters = Collections.unmodifiableMap(parameters);

		ProvisioningAction action = new CheckAndPromptNativePackageWindowsRegistry();
		action.setTouchpoint(touchpoint);

		assertOK(action.execute(parameters));
		assertEquals(Collections.emptyList(), touchpoint.getPackagesToInstall());
	}

	@Test
	public void execute_KeyExistence() throws Exception {
		Map<String, Object> parameters = new HashMap<String, Object>();
		NativeTouchpoint touchpoint = createTouchpoint(parameters);
		parameters.put(ActionConstants.PARM_LINUX_DISTRO, CheckAndPromptNativePackageWindowsRegistry.WINDOWS_DISTRO);
		parameters.put(ActionConstants.PARM_LINUX_PACKAGE_NAME, "windows package");
		parameters.put(ActionConstants.PARM_LINUX_PACKAGE_VERSION, "1.0");
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_KEY, registryPath(prefsNode));
		parameters = Collections.unmodifiableMap(parameters);

		ProvisioningAction action = new CheckAndPromptNativePackageWindowsRegistry();
		action.setTouchpoint(touchpoint);

		assertOK(action.execute(parameters));
		assertEquals(Collections.emptyList(), touchpoint.getPackagesToInstall());
	}

	@Test
	public void execute_KeyExistence_DifferentKeys() throws Exception {
		Map<String, Object> parameters = new HashMap<String, Object>();
		NativeTouchpoint touchpoint = createTouchpoint(parameters);
		parameters.put(ActionConstants.PARM_LINUX_DISTRO, CheckAndPromptNativePackageWindowsRegistry.WINDOWS_DISTRO);
		parameters.put(ActionConstants.PARM_LINUX_PACKAGE_NAME, "windows package");
		parameters.put(ActionConstants.PARM_LINUX_PACKAGE_VERSION, "1.0");
		parameters.put(ActionConstants.PARM_WINDOWS_REGISTRY_KEY, registryPath(prefsNode) + "\\node");
		parameters = Collections.unmodifiableMap(parameters);

		ProvisioningAction action = new CheckAndPromptNativePackageWindowsRegistry();
		action.setTouchpoint(touchpoint);

		assertOK(action.execute(parameters));
		assertEquals(touchpoint.getPackagesToInstall().toString(), 1, touchpoint.getPackagesToInstall().size());
	}

	private NativeTouchpoint createTouchpoint(Map<String, Object> parameters) {
		Properties profileProperties = new Properties();
		File installFolder = testHelper.getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = testHelper.createProfile("test", profileProperties);

		parameters.put(ActionConstants.PARM_PROFILE, profile);
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);
		return touchpoint;
	}

	private static String registryPath(Preferences prefs) {
		String path = prefs.absolutePath().replace('/', '\\');
		String root = prefs.isUserNode() ? "HKCU" : "HKLM";
		return root + "\\Software\\JavaSoft\\Prefs" + path;
	}

}