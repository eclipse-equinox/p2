/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.mockito.Mockito.verify;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.AccumulateConfigDataAction;
import org.eclipse.equinox.p2.publisher.eclipse.ConfigAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.LaunchingAdvice;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

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

	ArgumentCaptor<IPublisherAdvice> capture;

	@Override
	public void setUp() throws Exception {
		capture = ArgumentCaptor.forClass(IPublisherAdvice.class);
		setupPublisherInfo();
		setupPublisherResult();
		testAction = new AccumulateConfigDataAction(publisherInfo, configSpec, configLocation, executableLocation);
	}

	public void testAccumulateConfigDataAction() throws Exception {
		testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		verify(publisherInfo, Mockito.atLeastOnce()).addAdvice(capture.capture());
		verifyConfigAdvice();
		verifyLaunchAdvice();
		debug("Completed AccumulateConfigDataActionTest."); //$NON-NLS-1$
	}

	private void verifyLaunchAdvice() throws URISyntaxException {
		LaunchingAdvice captured = (LaunchingAdvice) capture.getAllValues().stream()
				.filter(LaunchingAdvice.class::isInstance).collect(Collectors.toList()).get(0);
		String[] programArgs = captured.getProgramArguments();
		assertTrue(programArgs.length == 4);
		assertTrue(programArgs[0].equalsIgnoreCase("-startup")); //$NON-NLS-1$

		IPath path1 = IPath.fromOSString(TestActivator.getTestDataFolder().getPath() + FOO);
		assertTrue(path1.toFile().toURI().equals(new URI(programArgs[1])));
		assertTrue(programArgs[2].equalsIgnoreCase("--launcher.library"));//$NON-NLS-1$

		IPath path2 = IPath.fromOSString(TestActivator.getTestDataFolder().getPath() + BAR);
		assertTrue(path2.toFile().toURI().equals(new URI(programArgs[3])));

		String[] vmArgs = captured.getVMArguments();
		assertTrue(vmArgs.length == 0);
		//		assertTrue(captured.getExecutableName().equalsIgnoreCase(EXECUTABLE_NAME)); TODO: use executable name from params, not framework admin
	}

	private void verifyConfigAdvice() throws Exception {
		ConfigAdvice captured = (ConfigAdvice) capture.getAllValues().stream().filter(ConfigAdvice.class::isInstance)
				.collect(Collectors.toList()).get(0);
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
		for (BundleInfo bundle : bundles) {
			if (bundle.getSymbolicName().equals(symbolicName)) {
				return;
			}
		}
		fail();
	}

	@Override
	protected void insertPublisherInfoBehavior() {
		ConfigData configData = new ConfigData(fwName, fwVersion, launcherName, launcherVersion);
		ConfigAdvice configAdvice = new ConfigAdvice(configData, configSpec);
		ArrayList<ConfigAdvice> configList = new ArrayList<>();
		configList.add(configAdvice);
	}
}
