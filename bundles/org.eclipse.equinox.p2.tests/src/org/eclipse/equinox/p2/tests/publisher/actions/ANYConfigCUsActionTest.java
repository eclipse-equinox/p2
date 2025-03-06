/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP and others.
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
 *   SAP - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.publisher.eclipse.DataLoader;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.ITouchpointInstruction;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.ConfigAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.ConfigCUsAction;
import org.eclipse.equinox.p2.publisher.eclipse.IConfigAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.IExecutableAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.LaunchingAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.TestMetadataRepository;

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

	@Override
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
		ArrayList<IInstallableUnit> IUs = new ArrayList<>(publisherResult.getIUs(null, IPublisherResult.ROOT));
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
		verifyRequirement(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, flavor + id + ".config." + configSpec, new VersionRange(version, true, version, true)); //$NON-NLS-1$
		verifyRequirement(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, flavor + id + ".ini." + configSpec, new VersionRange(version, true, version, true)); //$NON-NLS-1$
		verifyRequirement(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, flavor + configSpec + ORG_ECLIPSE_CORE_COMMANDS, new VersionRange(version, true, version, true));

		//verify non root IUs
		verifyFragment("ini"); //$NON-NLS-1$
		verifyFragment("config"); //$NON-NLS-1$
		verifyBundleCU();
	}

	private void verifyFragment(String cuType) {
		for (IInstallableUnit iu : new ArrayList<>(publisherResult.getIUs(null, IPublisherResult.NON_ROOT))) {
			if (iu.getId().equals(flavor + id + "." + cuType + "." + configSpec)) { //$NON-NLS-1$ //$NON-NLS-2$

				assertNull(iu.getFilter()); // no filter if config spec is ANY

				assertTrue(iu.getVersion().equals(version));

				assertFalse(iu.isSingleton());

				Collection<IProvidedCapability> providedCapabilities = iu.getProvidedCapabilities();
				verifyProvidedCapability(providedCapabilities, IInstallableUnit.NAMESPACE_IU_ID, flavor + id + "." + cuType + "." + configSpec, version); //$NON-NLS-1$//$NON-NLS-2$
				verifyProvidedCapability(providedCapabilities, flavor + id, id + "." + cuType, version); //$NON-NLS-1$
				assertTrue(providedCapabilities.size() == 2);

				assertTrue(iu.getRequirements().size() == 0);

				if (cuType.equals("ini")) { //$NON-NLS-1$
					verifyLauncherArgs(iu);
				}
				if (cuType.equals("config")) { //$NON-NLS-1$
					verifyConfigProperties(iu);
				}
				return; //pass
			}
		}
		fail("Configuration unit of type " + cuType + " was not enocuntered among fragments"); //$NON-NLS-1$
	}

	private void verifyBundleCU() {

		final String bundleCUId = flavor + configSpec + ORG_ECLIPSE_CORE_COMMANDS;
		IQueryResult<IInstallableUnit> queryResult = publisherResult.query(QueryUtil.createIUQuery(bundleCUId), new NullProgressMonitor());
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
		verifyRequirement(hostRequirements, "osgi.bundle", ORG_ECLIPSE_CORE_COMMANDS, new VersionRange(BUNDLE_VERSION)); //$NON-NLS-1$
		verifyRequirement(hostRequirements, "org.eclipse.equinox.p2.eclipse.type", "bundle", new VersionRange(Version.create("1.0.0"), true, Version.create("2.0.0"), false), null, 1, 1, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
		assertTrue("arg -foo bar", body.contains("addProgramArg(programArg:-foo bar);")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("vmarg -agentlib", body.contains("addJvmArg(jvmArg:-agentlib${#58}jdwp=transport=dt_socket${#44}server=y${#44}suspend=n${#44}address=8272);")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("arg -product com,ma", body.contains("addProgramArg(programArg:-product);addProgramArg(programArg:com${#44}ma);")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void verifyConfigProperties(IInstallableUnit iu) {
		Collection<ITouchpointData> touchpointData = iu.getTouchpointData();
		assertEquals(1, touchpointData.size());
		ITouchpointData data = touchpointData.iterator().next();
		ITouchpointInstruction instruction = data.getInstruction("configure"); //$NON-NLS-1$
		String body = instruction.getBody();
		assertTrue("eclipse.product", body.contains("setProgramProperty(propName:eclipse.product,propValue:org.eclipse.platform.ide);")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("eclipse.buildId", body.contains("setProgramProperty(propName:eclipse.buildId,propValue:TEST-ID);")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("my.property", body.contains("setProgramProperty(propName:my.property,propValue:${#123}a${#44}b${#58}c${#59}${#36}d${#125});")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected void insertPublisherInfoBehavior() {
		loader = new DataLoader(configLocation, executableLocation);

		//configure IExecutableAdvice
		LauncherData launcherData = loader.getLauncherData();
		LaunchingAdvice launchingAdvice = new LaunchingAdvice(launcherData, configSpec);

		ArrayList<IExecutableAdvice> launchingList = new ArrayList<>();
		launchingList.add(launchingAdvice);

		ProductFileAdvice productAdvice = null;

		try {
			String productFileLocation = TestData.getFile("ProductActionTest", "productFileActionTest.product").toString(); //$NON-NLS-1$ //$NON-NLS-2$
			productAdvice = new ProductFileAdvice(new ProductFile(productFileLocation), configSpec);
			launchingList.add(productAdvice);
		} catch (Exception e) {
			fail("Unable to create product file advice", e); //$NON-NLS-1$
		}

		when(publisherInfo.getAdvice(matches(configSpec), eq(false), anyString(), any(Version.class),
				eq(IExecutableAdvice.class))).thenReturn(launchingList);

		//configure IConfigAdvice
		ConfigData configData = loader.getConfigData();
		ConfigAdvice configAdvice = new ConfigAdvice(configData, configSpec);
		ArrayList<IConfigAdvice> configList = new ArrayList<>();
		configList.add(configAdvice);
		configList.add(productAdvice);
		when(publisherInfo.getAdvice(matches(configSpec), eq(false), anyString(), any(Version.class),
				eq(IConfigAdvice.class))).thenReturn(configList);

		//setup metadata repository
		IInstallableUnit[] ius = {mockIU("foo", null), mockIU("bar", null)}; //$NON-NLS-1$ //$NON-NLS-2$

		metadataRepo = new TestMetadataRepository(getAgent(), ius);
		when(publisherInfo.getMetadataRepository()).thenReturn(metadataRepo);
		when(publisherInfo.getContextMetadataRepository()).thenReturn(null);

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
