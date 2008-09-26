/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.easymock.EasyMock.expect;

import java.io.File;
import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.publisher.eclipse.DataLoader;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.TestMetadataRepository;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

@SuppressWarnings( {"unchecked", "restriction"})
public class ConfigCUsActionTest extends ActionTest {
	private static File configLocation = new File(TestActivator.getTestDataFolder(), "ConfigCUsActionTest/level1/level2/config.ini"); //$NON-NLS-1$
	private static File executableLocation = new File(TestActivator.getTestDataFolder(), "ConfigCUsActionTest/level1/run.exe"); //$NON-NLS-1$
	private static Version version = new Version("1.0.0"); //$NON-NLS-1$
	private static String id = "id"; //$NON-NLS-1$
	private static String flavor = "tooling"; //$NON-NLS-1$
	private IMetadataRepository metadataRepo;
	private DataLoader loader;

	public void setUp() throws Exception {
		setupPublisherInfo();
		setupPublisherResult();
		testAction = new ConfigCUsAction(publisherInfo, flavor, id, version);
	}

	public void testAction() throws Exception {
		testAction.perform(publisherInfo, publisherResult);
		verifyAction();
		debug("Completed ConfigCUsAction test."); //$NON-NLS-1$
	}

	private void verifyAction() {
		ArrayList IUs = new ArrayList(publisherResult.getIUs(null, IPublisherResult.ROOT));
		assertTrue(IUs.size() == 1);
		InstallableUnit iu = (InstallableUnit) IUs.get(0);
		assertTrue(iu.getId().equalsIgnoreCase(flavor + id + ".configuration")); //$NON-NLS-1$

		//verify ProvidedCapabilities
		ProvidedCapability[] providedCapabilities = iu.getProvidedCapabilities();
		verifyProvidedCapability(providedCapabilities, "org.eclipse.equinox.p2.iu", iu.getId(), version); //$NON-NLS-1$
		//		verifyProvidedCapability(providedCapabilities, flavor + id, id + ".config", version); //$NON-NLS-1$
		assertTrue(providedCapabilities.length == 1);

		//verify RequiredCapabilities
		RequiredCapability[] requiredCapability = iu.getRequiredCapabilities();
		verifyRequiredCapability(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, flavor + id + ".config." + configSpec, new VersionRange(version, true, version, true)); //$NON-NLS-1$
		verifyRequiredCapability(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, flavor + id + ".ini." + configSpec, new VersionRange(version, true, version, true)); //$NON-NLS-1$
		assertTrue(requiredCapability.length == 2);

		//verify non root IUs
		verifyFragment("ini"); //$NON-NLS-1$
		verifyFragment("config"); //$NON-NLS-1$
	}

	private void verifyFragment(String cuType) {
		ArrayList IUs = new ArrayList(publisherResult.getIUs(null, IPublisherResult.NON_ROOT));
		assertTrue(IUs.size() == 2);
		for (int i = 0; i < IUs.size(); i++) {
			InstallableUnit iu = (InstallableUnit) IUs.get(i);
			if (iu.getId().equals(flavor + id + "." + cuType + "." + configSpec)) { //$NON-NLS-1$ //$NON-NLS-2$
				assertTrue(iu.getFilter().equals("(& (osgi.ws=win32)(osgi.os=win32)(osgi.arch=x86))")); //$NON-NLS-1$
				assertTrue(iu.getVersion().equals(version));
				assertTrue(iu.getProperty("org.eclipse.equinox.p2.type.fragment").equals("true")); //$NON-NLS-1$//$NON-NLS-2$
				assertFalse(iu.isSingleton());
				ProvidedCapability[] providedCapabilities = iu.getProvidedCapabilities();
				verifyProvidedCapability(providedCapabilities, IInstallableUnit.NAMESPACE_IU_ID, flavor + id + "." + cuType + "." + configSpec, version); //$NON-NLS-1$//$NON-NLS-2$
				verifyProvidedCapability(providedCapabilities, flavor + id, id + "." + cuType, version); //$NON-NLS-1$
				assertTrue(providedCapabilities.length == 2);
				assertTrue(iu.getRequiredCapabilities().length == 0);
				assertFalse(iu.isFragment());
				return; //pass
			}
		}
		fail();

	}

	protected void insertPublisherInfoBehavior() {
		loader = new DataLoader(configLocation, executableLocation);

		//configure IConfigAdvice
		ConfigData configData = loader.getConfigData();
		ConfigAdvice configAdvice = new ConfigAdvice(configData, configSpec);
		ArrayList configList = new ArrayList();
		configList.add(configAdvice);
		expect(publisherInfo.getAdvice(configSpec, false, null, null, IConfigAdvice.class)).andReturn(configList);

		//configure ILaunchingAdvice
		LauncherData launcherData = loader.getLauncherData();
		LaunchingAdvice launchingAdvice = new LaunchingAdvice(launcherData, configSpec);
		ArrayList launchingList = new ArrayList();
		launchingList.add(launchingAdvice);
		expect(publisherInfo.getAdvice(configSpec, false, null, null, ILaunchingAdvice.class)).andReturn(launchingList);

		//setup metadata repository
		IInstallableUnit[] ius = {mockIU("foo", null), mockIU("bar", null)}; //$NON-NLS-1$ //$NON-NLS-2$

		metadataRepo = new TestMetadataRepository(ius);
		expect(publisherInfo.getMetadataRepository()).andReturn(metadataRepo).anyTimes();

	}
}
