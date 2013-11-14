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

import static org.easymock.EasyMock.expect;

import java.io.File;
import java.util.*;
import org.easymock.EasyMock;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.publisher.eclipse.DataLoader;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.*;

@SuppressWarnings({"unchecked"})
public class ConfigCUsActionTest extends ActionTest {
	private static File configLocation = new File(TestActivator.getTestDataFolder(), "ConfigCUsActionTest/level1/level2/config.ini"); //$NON-NLS-1$
	private static File executableLocation = new File(TestActivator.getTestDataFolder(), "ConfigCUsActionTest/level1/run.exe"); //$NON-NLS-1$
	private static Version version = Version.create("1.0.0"); //$NON-NLS-1$
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
		testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		verifyAction();
		debug("Completed ConfigCUsAction test."); //$NON-NLS-1$
	}

	private void verifyAction() {
		ArrayList IUs = new ArrayList(publisherResult.getIUs(null, IPublisherResult.ROOT));
		assertTrue(IUs.size() == 1);
		InstallableUnit iu = (InstallableUnit) IUs.get(0);
		assertTrue(iu.getId().equalsIgnoreCase(flavor + id + ".configuration")); //$NON-NLS-1$

		//verify ProvidedCapabilities
		Collection<IProvidedCapability> providedCapabilities = iu.getProvidedCapabilities();
		verifyProvidedCapability(providedCapabilities, "org.eclipse.equinox.p2.iu", iu.getId(), version); //$NON-NLS-1$
		//		verifyProvidedCapability(providedCapabilities, flavor + id, id + ".config", version); //$NON-NLS-1$
		assertTrue(providedCapabilities.size() == 1);

		//verify RequiredCapabilities
		List<IRequirement> requiredCapability = iu.getRequirements();
		verifyRequiredCapability(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, flavor + id + ".config." + configSpec, new VersionRange(version, true, version, true)); //$NON-NLS-1$
		verifyRequiredCapability(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, flavor + id + ".ini." + configSpec, new VersionRange(version, true, version, true)); //$NON-NLS-1$
		assertTrue(requiredCapability.size() == 2);

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
				assertTrue(iu.getFilter().equals(InstallableUnit.parseFilter("(& (osgi.ws=win32)(osgi.os=win32)(osgi.arch=x86))"))); //$NON-NLS-1$
				assertTrue(iu.getVersion().equals(version));
				assertFalse(iu.isSingleton());
				Collection<IProvidedCapability> providedCapabilities = iu.getProvidedCapabilities();
				verifyProvidedCapability(providedCapabilities, IInstallableUnit.NAMESPACE_IU_ID, flavor + id + "." + cuType + "." + configSpec, version); //$NON-NLS-1$//$NON-NLS-2$
				verifyProvidedCapability(providedCapabilities, flavor + id, id + "." + cuType, version); //$NON-NLS-1$
				assertTrue(providedCapabilities.size() == 2);
				assertTrue(iu.getRequirements().size() == 0);
				if (cuType.equals("ini"))
					verifyLauncherArgs(iu);
				if (cuType.equals("config"))
					verifyConfigProperties(iu);
				return; //pass
			}
		}
		fail();

	}

	private void verifyLauncherArgs(IInstallableUnit iu) {
		Collection<ITouchpointData> touchpointData = iu.getTouchpointData();
		assertEquals(1, touchpointData.size());
		ITouchpointData data = touchpointData.iterator().next();
		ITouchpointInstruction instruction = data.getInstruction("configure");
		String body = instruction.getBody();
		assertTrue("arg -foo bar", body.indexOf("addProgramArg(programArg:-foo bar);") > -1);
		assertTrue("vmarg -agentlib", body.indexOf("addJvmArg(jvmArg:-agentlib${#58}jdwp=transport=dt_socket${#44}server=y${#44}suspend=n${#44}address=8272);") > -1);
		assertTrue("arg -product com,ma", body.indexOf("addProgramArg(programArg:-product);addProgramArg(programArg:com${#44}ma);") > -1);
	}

	private void verifyConfigProperties(IInstallableUnit iu) {
		Collection<ITouchpointData> touchpointData = iu.getTouchpointData();
		assertEquals(1, touchpointData.size());
		ITouchpointData data = touchpointData.iterator().next();
		ITouchpointInstruction instruction = data.getInstruction("configure");
		String body = instruction.getBody();
		assertTrue("eclipse.product", body.indexOf("setProgramProperty(propName:eclipse.product,propValue:org.eclipse.platform.ide);") > -1);
		assertTrue("eclipse.buildId", body.indexOf("setProgramProperty(propName:eclipse.buildId,propValue:TEST-ID);") > -1);
		assertTrue("my.property", body.indexOf("setProgramProperty(propName:my.property,propValue:${#123}a${#44}b${#58}c${#59}${#36}d${#125});") > -1);
	}

	protected void insertPublisherInfoBehavior() {
		loader = new DataLoader(configLocation, executableLocation);

		//configure IConfigAdvice
		ConfigData configData = loader.getConfigData();
		ConfigAdvice configAdvice = new ConfigAdvice(configData, configSpec);
		ArrayList configList = new ArrayList();
		configList.add(configAdvice);
		expect(publisherInfo.getAdvice(EasyMock.matches(configSpec), EasyMock.eq(false), (String) EasyMock.anyObject(), (Version) EasyMock.anyObject(), EasyMock.eq(IConfigAdvice.class))).andReturn(configList).anyTimes();

		//configure IExecutableAdvice
		LauncherData launcherData = loader.getLauncherData();
		LaunchingAdvice launchingAdvice = new LaunchingAdvice(launcherData, configSpec);

		ArrayList launchingList = new ArrayList();
		launchingList.add(launchingAdvice);

		try {
			String productFileLocation = TestData.getFile("ProductActionTest", "productFileActionTest.product").toString();
			ProductFileAdvice productAdvice = new ProductFileAdvice(new ProductFile(productFileLocation), configSpec);
			launchingList.add(productAdvice);
		} catch (Exception e) {
			// TODO Auto-generated catch block
		}

		expect(publisherInfo.getAdvice(EasyMock.matches(configSpec), EasyMock.eq(false), (String) EasyMock.anyObject(), (Version) EasyMock.anyObject(), EasyMock.eq(IExecutableAdvice.class))).andReturn(launchingList).anyTimes();

		//setup metadata repository
		IInstallableUnit[] ius = {mockIU("foo", null), mockIU("bar", null)}; //$NON-NLS-1$ //$NON-NLS-2$

		metadataRepo = new TestMetadataRepository(getAgent(), ius);
		expect(publisherInfo.getMetadataRepository()).andReturn(metadataRepo).anyTimes();
		expect(publisherInfo.getContextMetadataRepository()).andReturn(null).anyTimes();

	}
}
