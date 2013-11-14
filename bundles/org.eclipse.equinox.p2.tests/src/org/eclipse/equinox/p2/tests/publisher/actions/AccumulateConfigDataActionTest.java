/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.equinox.p2.tests.TestActivator;

@SuppressWarnings({"unchecked"})
public class AccumulateConfigDataActionTest extends ActionTest {

	private static String EXECUTABLE_NAME = "run.exe"; //$NON-NLS-1$
	private static String FOO = "/AccumulateConfigDataActionTest/level1/plugins/foo_1.0.100.v20080509-1800.jar"; //$NON-NLS-1$
	private static String BAR = "/AccumulateConfigDataActionTest/level1/plugins/bar_1.0.100.v20080509-1800"; //$NON-NLS-1$
	private static File configLocation = new File(TestActivator.getTestDataFolder(), "AccumulateConfigDataActionTest/level1/level2/config.ini"); //$NON-NLS-1$
	private static File executableLocation = new File(TestActivator.getTestDataFolder(), "AccumulateConfigDataActionTest/level1/" + EXECUTABLE_NAME); //$NON-NLS-1$
	private static String fwName = "osgi"; //$NON-NLS-1$
	private static String fwVersion = "3.4.0.qualifier"; //$NON-NLS-1$
	private static String launcherName = "launcherName"; //$NON-NLS-1$
	private static String launcherVersion = "0.0.0"; //$NON-NLS-1$

	Capture<ConfigAdvice> configAdviceCapture;
	Capture<LaunchingAdvice> launchingAdviceCapture;

	public void setUp() throws Exception {
		configAdviceCapture = new Capture<ConfigAdvice>();
		launchingAdviceCapture = new Capture<LaunchingAdvice>();
		setupPublisherInfo();
		setupPublisherResult();
		testAction = new AccumulateConfigDataAction(publisherInfo, configSpec, configLocation, executableLocation);
	}

	public void testAccumulateConfigDataAction() throws Exception {
		testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		verifyConfigAdvice();
		verifyLaunchAdvice();
		debug("Completed AccumulateConfigDataActionTest."); //$NON-NLS-1$
	}

	private void verifyLaunchAdvice() throws URISyntaxException {
		LaunchingAdvice captured = launchingAdviceCapture.getValue();
		String[] programArgs = captured.getProgramArguments();
		assertTrue(programArgs.length == 4);
		assertTrue(programArgs[0].equalsIgnoreCase("-startup")); //$NON-NLS-1$

		Path path1 = new Path(TestActivator.getTestDataFolder().getPath() + FOO);
		assertTrue(path1.toFile().toURI().equals(new URI(programArgs[1])));
		assertTrue(programArgs[2].equalsIgnoreCase("--launcher.library"));//$NON-NLS-1$

		Path path2 = new Path(TestActivator.getTestDataFolder().getPath() + BAR);
		assertTrue(path2.toFile().toURI().equals(new URI(programArgs[3])));

		String[] vmArgs = captured.getVMArguments();
		assertTrue(vmArgs.length == 0);
		//		assertTrue(captured.getExecutableName().equalsIgnoreCase(EXECUTABLE_NAME)); TODO: use executable name from params, not framework admin
	}

	private void verifyConfigAdvice() throws Exception {
		ConfigAdvice captured = configAdviceCapture.getValue();
		Map<String, String> prop = captured.getProperties();
		assertTrue(prop.get("eclipse.buildId").equalsIgnoreCase("TEST-ID")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(prop.get("eclipse.p2.profile").equalsIgnoreCase("PlatformProfile")); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue(prop.get("org.eclipse.update.reconcile").equalsIgnoreCase("false")); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue(prop.get("eclipse.product").equalsIgnoreCase("org.eclipse.platform.ide")); //$NON-NLS-1$//$NON-NLS-2$

		assertContainsSymbolicName(captured.getBundles(), "org.eclipse.swt"); //$NON-NLS-1$
		assertContainsSymbolicName(captured.getBundles(), "org.eclipse.swt.win32.win32.x86"); //$NON-NLS-1$
		assertContainsSymbolicName(captured.getBundles(), "org.eclipse.swt.gtk.linux.x86"); //$NON-NLS-1$
		assertContainsSymbolicName(captured.getBundles(), "org.eclipse.swt.carbon.macosx"); //$NON-NLS-1$
	}

	private void assertContainsSymbolicName(BundleInfo[] bundles, String symbolicName) {
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].getSymbolicName().equals(symbolicName))
				return;
		}
		fail();
	}

	protected void insertPublisherInfoBehavior() {
		ConfigData configData = new ConfigData(fwName, fwVersion, launcherName, launcherVersion);
		ConfigAdvice configAdvice = new ConfigAdvice(configData, configSpec);
		ArrayList configList = new ArrayList();
		configList.add(configAdvice);

		publisherInfo.addAdvice(EasyMock.and(EasyMock.isA(ConfigAdvice.class), EasyMock.capture(configAdviceCapture)));
		publisherInfo.addAdvice(EasyMock.and(EasyMock.isA(LaunchingAdvice.class), EasyMock.capture(launchingAdviceCapture)));
	}
}
