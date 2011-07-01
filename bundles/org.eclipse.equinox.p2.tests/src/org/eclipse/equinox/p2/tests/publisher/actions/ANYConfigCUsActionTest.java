/*******************************************************************************
 * Copyright (c) 2011 SAP and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   SAP - initial API and implementation
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
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.*;

@SuppressWarnings({"unchecked"})
public class ANYConfigCUsActionTest extends ActionTest {
	private static final String BUNDLE_VERSION = "5.0.0"; //$NON-NLS-1$
	private static final String ORG_ECLIPSE_CORE_COMMANDS = "org.eclipse.core.commands"; //$NON-NLS-1$
	private static File configLocation = new File(TestActivator.getTestDataFolder(), "ConfigCUsActionTest/level1/level2/config.ini"); //$NON-NLS-1$
	private static File executableLocation = new File(TestActivator.getTestDataFolder(), "ConfigCUsActionTest/level1/run.exe"); //$NON-NLS-1$
	private static Version version = Version.create("1.0.0"); //$NON-NLS-1$
	private static String id = "id"; //$NON-NLS-1$
	private static String flavor = "tooling"; //$NON-NLS-1$
	private IMetadataRepository metadataRepo;
	private DataLoader loader;

	public void setUp() throws Exception {

		// configuration spec for creation of filterless CUs
		String[] cfgSpecs = AbstractPublisherAction.parseConfigSpec("ANY"); //$NON-NLS-1$
		configSpec = AbstractPublisherAction.createConfigSpec(cfgSpecs[0], cfgSpecs[1], cfgSpecs[2]);
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
		assertTrue(providedCapabilities.size() == 1);

		//verify RequiredCapabilities
		List<IRequirement> requiredCapability = iu.getRequirements();
		assertTrue(requiredCapability.size() == 3);
		verifyRequiredCapability(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, flavor + id + ".config." + configSpec, new VersionRange(version, true, version, true)); //$NON-NLS-1$
		verifyRequiredCapability(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, flavor + id + ".ini." + configSpec, new VersionRange(version, true, version, true)); //$NON-NLS-1$
		verifyRequiredCapability(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, flavor + configSpec + ORG_ECLIPSE_CORE_COMMANDS, new VersionRange(version, true, version, true));

		//verify non root IUs
		verifyFragment("ini"); //$NON-NLS-1$
		verifyFragment("config"); //$NON-NLS-1$
		verifyBundleCU();
	}

	private void verifyFragment(String cuType) {
		ArrayList IUs = new ArrayList(publisherResult.getIUs(null, IPublisherResult.NON_ROOT));
		for (int i = 0; i < IUs.size(); i++) {
			InstallableUnit iu = (InstallableUnit) IUs.get(i);
			if (iu.getId().equals(flavor + id + "." + cuType + "." + configSpec)) { //$NON-NLS-1$ //$NON-NLS-2$

				assertNull(iu.getFilter()); // no filter if config spec is ANY

				assertTrue(iu.getVersion().equals(version));

				assertFalse(iu.isSingleton());

				Collection<IProvidedCapability> providedCapabilities = iu.getProvidedCapabilities();
				verifyProvidedCapability(providedCapabilities, IInstallableUnit.NAMESPACE_IU_ID, flavor + id + "." + cuType + "." + configSpec, version); //$NON-NLS-1$//$NON-NLS-2$
				verifyProvidedCapability(providedCapabilities, flavor + id, id + "." + cuType, version); //$NON-NLS-1$
				assertTrue(providedCapabilities.size() == 2);

				assertTrue(iu.getRequirements().size() == 0);

				if (cuType.equals("ini")) //$NON-NLS-1$
					verifyLauncherArgs(iu);
				if (cuType.equals("config")) //$NON-NLS-1$
					verifyConfigProperties(iu);
				return; //pass
			}
		}
		fail("Configuration unit of type " + cuType + " was not enocuntered among fragments"); //$NON-NLS-1$
	}

	private void verifyBundleCU() {

		final String bundleCUId = flavor + configSpec + ORG_ECLIPSE_CORE_COMMANDS;
		IQueryResult queryResult = publisherResult.query(QueryUtil.createIUQuery(bundleCUId), new NullProgressMonitor());
		assertEquals(1, queryResultSize(queryResult));
		IInstallableUnitFragment fragment = (IInstallableUnitFragment) queryResult.iterator().next();

		assertNull(fragment.getFilter()); // no filter if config spec is ANY

		assertTrue(fragment.getVersion().equals(version));

		assertFalse(fragment.isSingleton());

		final Collection<IProvidedCapability> providedCapabilities = fragment.getProvidedCapabilities();
		verifyProvidedCapability(providedCapabilities, IInstallableUnit.NAMESPACE_IU_ID, bundleCUId, version);
		verifyProvidedCapability(providedCapabilities, "org.eclipse.equinox.p2.flavor", flavor + configSpec, version); //$NON-NLS-1$
		assertEquals(2, providedCapabilities.size());

		assertEquals(0, fragment.getRequirements().size());

		final Collection<IRequirement> hostRequirements = fragment.getHost();
		verifyRequiredCapability(hostRequirements, "osgi.bundle", ORG_ECLIPSE_CORE_COMMANDS, new VersionRange(BUNDLE_VERSION)); //$NON-NLS-1$
		verifyRequiredCapability(hostRequirements, "org.eclipse.equinox.p2.eclipse.type", "bundle", new VersionRange(Version.create("1.0.0"), true, Version.create("2.0.0"), false), 1, 1, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue(hostRequirements.size() == 2);

		final Collection<ITouchpointData> touchpointData = fragment.getTouchpointData();
		assertEquals(1, touchpointData.size());
		ITouchpointData data = touchpointData.iterator().next();
		ITouchpointInstruction instruction = data.getInstruction("install"); //$NON-NLS-1$
		assertEquals("installBundle(bundle:${artifact})", instruction.getBody()); //$NON-NLS-1$
		instruction = data.getInstruction("uninstall"); //$NON-NLS-1$
		assertEquals("uninstallBundle(bundle:${artifact})", instruction.getBody()); //$NON-NLS-1$
		instruction = data.getInstruction("configure"); //$NON-NLS-1$
		assertEquals("setStartLevel(startLevel:2);", instruction.getBody()); //$NON-NLS-1$
		instruction = data.getInstruction("unconfigure"); //$NON-NLS-1$
		assertEquals("setStartLevel(startLevel:-1);", instruction.getBody()); //$NON-NLS-1$
	}

	private void verifyLauncherArgs(IInstallableUnit iu) {
		Collection<ITouchpointData> touchpointData = iu.getTouchpointData();
		assertEquals(1, touchpointData.size());
		ITouchpointData data = touchpointData.iterator().next();
		ITouchpointInstruction instruction = data.getInstruction("configure"); //$NON-NLS-1$
		String body = instruction.getBody();
		assertTrue("arg -foo bar", body.indexOf("addProgramArg(programArg:-foo bar);") > -1); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("vmarg -agentlib", body.indexOf("addJvmArg(jvmArg:-agentlib${#58}jdwp=transport=dt_socket${#44}server=y${#44}suspend=n${#44}address=8272);") > -1); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("arg -product com,ma", body.indexOf("addProgramArg(programArg:-product);addProgramArg(programArg:com${#44}ma);") > -1); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void verifyConfigProperties(IInstallableUnit iu) {
		Collection<ITouchpointData> touchpointData = iu.getTouchpointData();
		assertEquals(1, touchpointData.size());
		ITouchpointData data = touchpointData.iterator().next();
		ITouchpointInstruction instruction = data.getInstruction("configure"); //$NON-NLS-1$
		String body = instruction.getBody();
		assertTrue("eclipse.product", body.indexOf("setProgramProperty(propName:eclipse.product,propValue:org.eclipse.platform.ide);") > -1); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("eclipse.buildId", body.indexOf("setProgramProperty(propName:eclipse.buildId,propValue:TEST-ID);") > -1); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("my.property", body.indexOf("setProgramProperty(propName:my.property,propValue:${#123}a${#44}b${#58}c${#59}${#36}d${#125});") > -1); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void insertPublisherInfoBehavior() {
		loader = new DataLoader(configLocation, executableLocation);

		//configure IExecutableAdvice
		LauncherData launcherData = loader.getLauncherData();
		LaunchingAdvice launchingAdvice = new LaunchingAdvice(launcherData, configSpec);

		ArrayList launchingList = new ArrayList();
		launchingList.add(launchingAdvice);

		ProductFileAdvice productAdvice = null;

		try {
			String productFileLocation = TestData.getFile("ProductActionTest", "productFileActionTest.product").toString(); //$NON-NLS-1$ //$NON-NLS-2$
			productAdvice = new ProductFileAdvice(new ProductFile(productFileLocation), configSpec);
			launchingList.add(productAdvice);
		} catch (Exception e) {
			fail("Unable to create product file advice", e); //$NON-NLS-1$
		}

		expect(publisherInfo.getAdvice(EasyMock.matches(configSpec), EasyMock.eq(false), (String) EasyMock.anyObject(), (Version) EasyMock.anyObject(), EasyMock.eq(IExecutableAdvice.class))).andReturn(launchingList).anyTimes();

		//configure IConfigAdvice
		ConfigData configData = loader.getConfigData();
		ConfigAdvice configAdvice = new ConfigAdvice(configData, configSpec);
		ArrayList configList = new ArrayList();
		configList.add(configAdvice);
		configList.add(productAdvice);
		expect(publisherInfo.getAdvice(EasyMock.matches(configSpec), EasyMock.eq(false), (String) EasyMock.anyObject(), (Version) EasyMock.anyObject(), EasyMock.eq(IConfigAdvice.class))).andReturn(configList).anyTimes();

		//setup metadata repository
		IInstallableUnit[] ius = {mockIU("foo", null), mockIU("bar", null)}; //$NON-NLS-1$ //$NON-NLS-2$

		metadataRepo = new TestMetadataRepository(getAgent(), ius);
		expect(publisherInfo.getMetadataRepository()).andReturn(metadataRepo).anyTimes();
		expect(publisherInfo.getContextMetadataRepository()).andReturn(null).anyTimes();

	}

	@Override
	public void setupPublisherResult() {
		super.setupPublisherResult();

		InstallableUnitDescription iuDescription = new InstallableUnitDescription();
		iuDescription.setId(ORG_ECLIPSE_CORE_COMMANDS);
		iuDescription.setVersion(Version.create(BUNDLE_VERSION));
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDescription);

		publisherResult.addIU(iu, IPublisherResult.NON_ROOT);
	}

}
