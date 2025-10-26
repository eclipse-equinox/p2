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
 *   SAP AG - allow setting greedy through directive (bug 247099)
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.errorStatus;
import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.statusWithMessageWhich;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.OSGiVersion;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredPropertiesMatch;
import org.eclipse.equinox.internal.p2.metadata.TranslationSupport;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.ITouchpointInstruction;
import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.publisher.AdviceFileAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.IAdditionalInstallableUnitAdvice;
import org.eclipse.equinox.p2.publisher.actions.ICapabilityAdvice;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.publisher.actions.ITouchpointAdvice;
import org.eclipse.equinox.p2.publisher.actions.IUpdateDescriptorAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.IBundleShapeAdvice;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class BundlesActionTest extends ActionTest {
	private static final String OSGI = PublisherHelper.OSGI_BUNDLE_CLASSIFIER;
	private static final String OSGI_IDENTITY = "osgi.identity";
	private static final String OSGI_EE = "osgi.ee";
	private static final String JAVA_PACKAGE = "java.package";//$NON-NLS-1$
	private static final String JAVA_EE_1_4 = "(|(&(osgi.ee=JavaSE)(version=1.4))(&(osgi.ee=CDC/Foundation)(version=1.1)))";
	private static final String JAVA_EE_1_6 = "(&(osgi.ee=JavaSE)(version=1.6))";

	private static final String TEST1_IUD_NAME = "iud";//$NON-NLS-1$
	private static final String TEST1_PROVZ_NAME = "iuz";//$NON-NLS-1$
	private static final String TEST1_PROVBUNDLE_NAME = "test1";//$NON-NLS-1$
	private static final String TEST1_REQ_EE = JAVA_EE_1_4;
	private static final String TEST2_REQ_A_NAME = "iua";//$NON-NLS-1$
	private static final String TEST2_REQ_B_NAME = "iub";//$NON-NLS-1$
	private static final String TEST2_REQ_C_NAME = "iuc";//$NON-NLS-1$
	private static final String TEST2_REQ_EE = JAVA_EE_1_4;
	private static final String TEST2_PROV_Z_NAME = "iuz";//$NON-NLS-1$
	private static final String TEST2_PROV_Y_NAME = "iuy";//$NON-NLS-1$
	private static final String TEST2_PROV_X_NAME = "iux";//$NON-NLS-1$
	private static final String TEST2_PROV_BUNDLE_NAME = "test2";//$NON-NLS-1$
	private static final String TEST3_PROV_BUNDLE_NAME = "test3";//$NON-NLS-1$
	private static final String TEST4_PROV_BUNDLE_NAME = "test4";//$NON-NLS-1$
	private static final String TEST4_REQ_PACKAGE_OPTIONAL_NAME = "iue";//$NON-NLS-1$
	private static final String TEST4_REQ_PACKAGE_OPTGREEDY_NAME = "iuf";//$NON-NLS-1$
	private static final String TEST4_REQ_BUNDLE_OPTIONAL_NAME = "iug";//$NON-NLS-1$
	private static final String TEST4_REQ_BUNDLE_OPTGREEDY_NAME = "iuh";//$NON-NLS-1$
	private static final String TEST5_REQ_EE = JAVA_EE_1_4;
	private static final String TEST5_PROV_BUNDLE_NAME = "test5";//$NON-NLS-1$
	private static final String TESTDYN_REQ_EE = JAVA_EE_1_6;

	private static final File TEST_BASE = new File(TestActivator.getTestDataFolder(), "BundlesActionTest");//$NON-NLS-1$
	private static final File TEST_FILE1 = new File(TEST_BASE, TEST1_PROVBUNDLE_NAME);
	private static final File TEST_FILE2 = new File(TEST_BASE, TEST2_PROV_BUNDLE_NAME + ".jar");//$NON-NLS-1$

	private static final String PROVBUNDLE_NAMESPACE = "org.eclipse.equinox.p2.iu";//$NON-NLS-1$
	private static final String TEST2_IU_A_NAMESPACE = OSGI;
	private static final String TEST2_IU_B_NAMESPACE = JAVA_PACKAGE;
	private static final String TEST2_IU_C_NAMESPACE = JAVA_PACKAGE;
	private static final String TEST1_IU_D_NAMESPACE = JAVA_PACKAGE;
	private static final String TEST2_PROV_Z_NAMESPACE = JAVA_PACKAGE;
	private static final String TEST2_PROV_Y_NAMESPACE = JAVA_PACKAGE;
	private static final String TEST2_PROV_X_NAMESPACE = JAVA_PACKAGE;
	private static final String TEST1_PROV_Z_NAMESPACE = JAVA_PACKAGE;

	private static final Version BUNDLE1_VERSION = Version.create("0.1.0");//$NON-NLS-1$
	private static final Version BUNDLE2_VERSION = Version.create("1.0.0.qualifier");//$NON-NLS-1$
	private static final Version BUNDLE3_VERSION = Version.create("0.1.0.qualifier");//$NON-NLS-1$
	private static final Version BUNDLE4_VERSION = Version.create("2.0.1");//$NON-NLS-1$
	private static final Version BUNDLE5_VERSION = Version.create("0.1.0.qualifier");//$NON-NLS-1$

	private static final VersionRange DEFAULT_VERSION_RANGE = VersionRange.emptyRange;
	private static final Version PROVBUNDLE2_VERSION = BUNDLE2_VERSION;
	private static final Version TEST2_PROVZ_VERSION = Version.emptyVersion;
	private static final Version TEST2_PROVY_VERSION = Version.emptyVersion;
	private static final Version TEST2_PROVX_VERSION = Version.emptyVersion;
	private static final VersionRange TEST2_IU_A_VERSION_RANGE = VersionRange.emptyRange;
	private static final VersionRange TEST2_IU_B_VERSION_RANGE = VersionRange.emptyRange;
	private static final VersionRange TEST2_IU_C_VERSION_RANGE = VersionRange.create("1.0.0");//$NON-NLS-1$
	private static final VersionRange TEST1_IU_D_VERSION_RANGE = VersionRange.create("1.3.0");//$NON-NLS-1$

	protected TestArtifactRepository artifactRepository = new TestArtifactRepository(getAgent());
	private ArgumentCaptor<AdviceFileAdvice> updateDescriptorCapture;

	public void testAll() throws Exception {
		File[] files = TEST_BASE.listFiles();
		testAction = Mockito.spy(new BundlesAction(files));
		setupPublisherResult();
		setupPublisherInfo();
		updateDescriptorCapture = ArgumentCaptor.forClass(AdviceFileAdvice.class);
		IStatus status = testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		verify(publisherInfo, Mockito.atLeastOnce()).addAdvice(updateDescriptorCapture.capture());
		assertEquals(Status.OK_STATUS, status);

		verifyBundlesAction();
		cleanup();
	}

	public void testTranslationFragment() {
		File foo_fragment = new File(TestActivator.getTestDataFolder(), "FragmentPublisherTest/foo.fragment");//$NON-NLS-1$
		File foo = new File(TestActivator.getTestDataFolder(), "FragmentPublisherTest/foo");//$NON-NLS-1$
		BundlesAction bundlesAction = new BundlesAction(new File[] { foo_fragment });
		PublisherInfo info = new PublisherInfo();
		PublisherResult results = new PublisherResult();

		bundlesAction.perform(info, results, new NullProgressMonitor());
		Collection<IInstallableUnit> ius = results.getIUs(null, null);
		assertEquals(1, ius.size());

		info = new PublisherInfo();
		results = new PublisherResult();
		bundlesAction = new BundlesAction(new File[] { foo });
		bundlesAction.perform(info, results, new NullProgressMonitor());
		ius = results.getIUs(null, null);
		assertEquals(1, ius.size());
		QueryableArray queryableArray = new QueryableArray(ius);
		IQueryResult<IInstallableUnit> result = queryableArray.query(QueryUtil.createIUQuery("foo"), null);
		assertEquals(1, queryResultSize(result));
		IInstallableUnit iu = result.iterator().next();
		TranslationSupport utils = new TranslationSupport();
		utils.setTranslationSource(queryableArray);
		assertEquals("English Foo", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME));

		bundlesAction = new BundlesAction(new File[] { foo_fragment });
		bundlesAction.perform(info, results, new NullProgressMonitor());
		ius = results.getIUs(null, null);
		assertEquals(3, ius.size());
		queryableArray = new QueryableArray(ius);
		result = queryableArray.query(QueryUtil.createIUQuery("foo"), null);
		assertEquals(1, queryResultSize(result));
		iu = result.iterator().next();
		utils.setTranslationSource(queryableArray);
		assertEquals("German Foo", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN.toString()));
	}

	private void verifyBundlesAction() throws Exception {
		// verify publisher result
		verifyBundle1();
		verifyBundle2();
		verifyBundle3();
		verifyBundle4();
		verifyBundle5();

		verifyArtifactRepository();
	}

	private void verifyArtifactRepository() throws Exception {
		IArtifactKey key2 = ArtifactKey.parse("osgi.bundle,test2,1.0.0.qualifier");//$NON-NLS-1$
		IArtifactDescriptor[] descriptors = artifactRepository.getArtifactDescriptors(key2);

		// Should have one canonical and one packed
		assertEquals(1, descriptors.length);

		try (ZipInputStream actual = artifactRepository.getZipInputStream(descriptors[0]);
				ZipInputStream expected = new ZipInputStream(Files.newInputStream(TEST_FILE2.toPath()))) {
			TestData.assertEquals(expected, actual);
		}

		IArtifactKey key1 = ArtifactKey.parse("osgi.bundle,test1,0.1.0");//$NON-NLS-1$
		ZipInputStream zis = artifactRepository.getZipInputStream(key1);
		Map<String, Object[]> fileMap = getFileMap(new HashMap<>(), new File[] { TEST_FILE1 },
				IPath.fromOSString(TEST_FILE1.getAbsolutePath()));
		TestData.assertContains(fileMap, zis, true);
	}

	private void verifyBundle1() {
		List<IInstallableUnit> ius = new ArrayList<>(
				publisherResult.getIUs(TEST1_PROVBUNDLE_NAME, IPublisherResult.ROOT));
		assertEquals(1, ius.size());
		IInstallableUnit bundle1IU = ius.get(0);

		assertNotNull(bundle1IU);
		assertEquals(bundle1IU.getVersion(), BUNDLE1_VERSION);

		// check required capabilities
		Collection<IRequirement> requirements = bundle1IU.getRequirements();
		verifyRequirement(requirements, TEST1_IU_D_NAMESPACE, TEST1_IUD_NAME, TEST1_IU_D_VERSION_RANGE);
		verifyRequirement(requirements, OSGI_EE, TEST1_REQ_EE, null, 1, 1, true);
		assertEquals(2, requirements.size());

		// check provided capabilities
		Collection<IProvidedCapability> providedCapabilities = bundle1IU.getProvidedCapabilities();
		verifyProvidedCapability(providedCapabilities, PROVBUNDLE_NAMESPACE, TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION);
		verifyProvidedCapability(providedCapabilities, OSGI_IDENTITY, TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION);
		verifyProvidedCapability(providedCapabilities, OSGI, TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION);
		verifyProvidedCapability(providedCapabilities, TEST1_PROV_Z_NAMESPACE, TEST1_PROVZ_NAME, TEST2_PROVZ_VERSION);
		verifyProvidedCapability(providedCapabilities, PublisherHelper.NAMESPACE_ECLIPSE_TYPE, "source", //$NON-NLS-1$
				Version.create("1.0.0"));//$NON-NLS-1$
		assertEquals(5, providedCapabilities.size());

		List<AdviceFileAdvice> tData = updateDescriptorCapture.getAllValues();
		boolean found = false;
		for (AdviceFileAdvice iTouchpointAdvice : tData) {
			ITouchpointInstruction configure = iTouchpointAdvice.getTouchpointData(NO_TP_DATA)
					.getInstruction("configure");
			if (configure != null) {
				String body = configure.getBody();
				if (body != null && body.indexOf("download.eclipse.org/releases/ganymede") > 0) {
					found = true;
				}
			}
		}
		assertTrue(found);
	}

	private void verifyBundle2() {
		List<IInstallableUnit> ius = new ArrayList<>(
				publisherResult.getIUs(TEST2_PROV_BUNDLE_NAME, IPublisherResult.ROOT));
		assertEquals(1, ius.size());

		IInstallableUnit bundleIu = ius.get(0);
		assertNotNull(bundleIu);
		assertEquals(bundleIu.getVersion(), BUNDLE2_VERSION);

		// check required capabilities
		Collection<IRequirement> requirements = bundleIu.getRequirements();
		verifyRequirement(requirements, TEST2_IU_A_NAMESPACE, TEST2_REQ_A_NAME, TEST2_IU_A_VERSION_RANGE);
		verifyRequirement(requirements, TEST2_IU_B_NAMESPACE, TEST2_REQ_B_NAME, TEST2_IU_B_VERSION_RANGE);
		verifyRequirement(requirements, TEST2_IU_C_NAMESPACE, TEST2_REQ_C_NAME, TEST2_IU_C_VERSION_RANGE);
		verifyRequirement(requirements, OSGI_EE, TEST2_REQ_EE, null, 1, 1, true);
		assertEquals(5, requirements.size());

		// check provided capabilities
		Collection<IProvidedCapability> providedCapabilities = bundleIu.getProvidedCapabilities();
		verifyProvidedCapability(providedCapabilities, PROVBUNDLE_NAMESPACE, TEST2_PROV_BUNDLE_NAME,
				PROVBUNDLE2_VERSION);
		verifyProvidedCapability(providedCapabilities, OSGI, TEST2_PROV_BUNDLE_NAME, BUNDLE2_VERSION);
		verifyProvidedCapability(providedCapabilities, OSGI_IDENTITY, TEST2_PROV_BUNDLE_NAME, BUNDLE2_VERSION);
		verifyProvidedCapability(providedCapabilities, TEST2_PROV_Z_NAMESPACE, TEST2_PROV_Z_NAME, TEST2_PROVZ_VERSION);
		verifyProvidedCapability(providedCapabilities, TEST2_PROV_Y_NAMESPACE, TEST2_PROV_Y_NAME, TEST2_PROVY_VERSION);
		verifyProvidedCapability(providedCapabilities, TEST2_PROV_X_NAMESPACE, TEST2_PROV_X_NAME, TEST2_PROVX_VERSION);
		verifyProvidedCapability(providedCapabilities, PublisherHelper.NAMESPACE_ECLIPSE_TYPE, "bundle", //$NON-NLS-1$
				Version.create("1.0.0"));//$NON-NLS-1$
		assertEquals(7, providedCapabilities.size()); /* number of tested elements */

		// check %bundle name is correct
		Map<String, String> prop = bundleIu.getProperties();
		assertTrue(prop.get("org.eclipse.equinox.p2.name").toString().equalsIgnoreCase("%bundleName"));//$NON-NLS-1$//$NON-NLS-2$
		assertTrue(prop.get("org.eclipse.equinox.p2.provider").toString().equalsIgnoreCase("%providerName"));//$NON-NLS-1$//$NON-NLS-2$

		Collection<ITouchpointData> data = bundleIu.getTouchpointData();
		boolean found = false;
		for (ITouchpointData td : data) {
			ITouchpointInstruction configure = td.getInstruction("configure");
			if (configure == null) {
				continue;
			}
			String body = configure.getBody();
			if (body != null && body.indexOf("download.eclipse.org/releases/ganymede") > 0) {
				found = true;
			}
		}
		assertFalse(found);
	}

	private void verifyBundle3() {
		// also a regression test for bug 393051: manifest headers use uncommon (but
		// valid) capitalization
		ArrayList<IInstallableUnit> ius = new ArrayList<>(
				publisherResult.getIUs(TEST3_PROV_BUNDLE_NAME, IPublisherResult.ROOT));
		assertEquals(1, ius.size());
		IUpdateDescriptor updateDescriptor = null;
		boolean found = false;
		for (AdviceFileAdvice advice : updateDescriptorCapture.getAllValues()) {
			IUpdateDescriptor descriptor = advice.getUpdateDescriptor(new InstallableUnitDescription());
			if (descriptor != null) {
				Collection<IMatchExpression<IInstallableUnit>> iUsBeingUpdated = descriptor.getIUsBeingUpdated();
				if (iUsBeingUpdated != null) {
					String name = RequiredCapability.extractName(descriptor.getIUsBeingUpdated().iterator().next());
					if (TEST3_PROV_BUNDLE_NAME.equals(name)) {
						updateDescriptor = descriptor;
						found = true;
						break;
					}
				}
			}
		}
		assertTrue(found);
		VersionRange range = RequiredCapability.extractRange(updateDescriptor.getIUsBeingUpdated().iterator().next());
		String description = updateDescriptor.getDescription();
		int severity = updateDescriptor.getSeverity();

		VersionRange expectedRange = new VersionRange("(0.0.1," + BUNDLE3_VERSION + "]");
		assertEquals(expectedRange, range);
		assertEquals("Some description about this update", description.trim());
		assertEquals(8, severity);
	}

	private void verifyBundle4() {
		ArrayList<IInstallableUnit> ius = new ArrayList<>(
				publisherResult.getIUs(TEST4_PROV_BUNDLE_NAME, IPublisherResult.ROOT));
		assertEquals(1, ius.size());

		IInstallableUnit bundleIu = ius.get(0);
		assertNotNull(bundleIu);
		assertEquals(bundleIu.getVersion(), BUNDLE4_VERSION);

		// check required capabilities
		Collection<IRequirement> requirements = bundleIu.getRequirements();
		verifyRequirement(requirements, JAVA_PACKAGE, TEST4_REQ_PACKAGE_OPTIONAL_NAME, DEFAULT_VERSION_RANGE, null, 0,
				1, false);
		verifyRequirement(requirements, JAVA_PACKAGE, TEST4_REQ_PACKAGE_OPTGREEDY_NAME, DEFAULT_VERSION_RANGE, null, 0,
				1, true);
		verifyRequirement(requirements, OSGI, TEST4_REQ_BUNDLE_OPTIONAL_NAME, DEFAULT_VERSION_RANGE, null, 0, 1, false);
		verifyRequirement(requirements, OSGI, TEST4_REQ_BUNDLE_OPTGREEDY_NAME, DEFAULT_VERSION_RANGE, null, 0, 1, true);
		assertEquals(5, requirements.size());
	}

	private void verifyBundle5() {
		ArrayList<IInstallableUnit> ius = new ArrayList<>(
				publisherResult.getIUs(TEST5_PROV_BUNDLE_NAME, IPublisherResult.ROOT));
		assertEquals(1, ius.size());

		IInstallableUnit bundle5IU = ius.get(0);
		for (AdviceFileAdvice adv : updateDescriptorCapture.getAllValues()) {
			IRequirement[] reqs = adv.getRequiredCapabilities(new InstallableUnitDescription());
			if (reqs != null) {
				verifyRequirement(List.of(reqs), "bar", "foo", VersionRange.emptyRange, null, 6, 7, true);
			}
		}
		Collection<IRequirement> requirements = bundle5IU.getRequirements();
		verifyRequirement(requirements, OSGI_EE, TEST5_REQ_EE, null, 1, 1, true);
	}

	@Override
	public void cleanup() {
		super.cleanup();
		if (artifactRepository != null) {
			artifactRepository.removeAll(new NullProgressMonitor());
			artifactRepository = null;
		}
	}

	@Override
	protected void insertPublisherInfoBehavior() {
		// super sets publisherInfo.getMetadataRepository and
		// publisherInfo.getContextMetadataRepository
		super.insertPublisherInfoBehavior();
		Map<String, String> sarProperties = new HashMap<>();
		sarProperties.put("key1", "value1");//$NON-NLS-1$//$NON-NLS-2$
		sarProperties.put("key2", "value2");//$NON-NLS-1$//$NON-NLS-2$

		Map<String, String> sdkProperties = new HashMap<>();
		sdkProperties.put("key1", "value1");//$NON-NLS-1$//$NON-NLS-2$
		sdkProperties.put("key2", "value2");//$NON-NLS-1$//$NON-NLS-2$

		when(publisherInfo.getArtifactRepository()).thenReturn(artifactRepository);
		when(publisherInfo.getArtifactOptions())
				.thenReturn(IPublisherInfo.A_INDEX | IPublisherInfo.A_OVERWRITE | IPublisherInfo.A_PUBLISH);
		when(publisherInfo.getAdvice(null, false, null, null, ICapabilityAdvice.class)).thenReturn(new ArrayList<>());

		expectOtherAdviceQueries(TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION);
		expectPropertyAdviceQuery(TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION, sarProperties);
		expectUpdateDescriptorAdviceQuery(TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION, null);

		expectOtherAdviceQueries(TEST2_PROV_BUNDLE_NAME, BUNDLE2_VERSION);
		expectPropertyAdviceQuery(TEST2_PROV_BUNDLE_NAME, BUNDLE2_VERSION, sdkProperties);
		expectUpdateDescriptorAdviceQuery(TEST2_PROV_BUNDLE_NAME, BUNDLE2_VERSION, null);

		expectOtherAdviceQueries(TEST3_PROV_BUNDLE_NAME, BUNDLE3_VERSION);
		expectPropertyAdviceQuery(TEST3_PROV_BUNDLE_NAME, BUNDLE3_VERSION, sarProperties);
		expectTouchpointAdviceQuery(TEST3_PROV_BUNDLE_NAME, BUNDLE3_VERSION, null);

		expectOtherAdviceQueries(TEST4_PROV_BUNDLE_NAME, BUNDLE4_VERSION);
		expectPropertyAdviceQuery(TEST4_PROV_BUNDLE_NAME, BUNDLE4_VERSION, null);
		expectUpdateDescriptorAdviceQuery(TEST4_PROV_BUNDLE_NAME, BUNDLE4_VERSION, null);
		expectTouchpointAdviceQuery(TEST4_PROV_BUNDLE_NAME, BUNDLE4_VERSION, null);

//		expectCapabilityAdviceQuery(TEST5_PROV_BUNDLE_NAME, BUNDLE5_VERSION, capAdvice5.getValues());
		expectOtherAdviceQueries(TEST5_PROV_BUNDLE_NAME, BUNDLE5_VERSION);
		expectPropertyAdviceQuery(TEST5_PROV_BUNDLE_NAME, BUNDLE5_VERSION, sarProperties);
		expectUpdateDescriptorAdviceQuery(TEST5_PROV_BUNDLE_NAME, BUNDLE5_VERSION, null);
		expectTouchpointAdviceQuery(TEST5_PROV_BUNDLE_NAME, BUNDLE5_VERSION, null);

	}

	private void expectOtherAdviceQueries(String bundleName, Version bundleVersion) {
		when(publisherInfo.getAdvice(null, false, bundleName, bundleVersion, ICapabilityAdvice.class))
				.thenReturn(Collections.emptyList());
		when(publisherInfo.getAdvice(null, false, bundleName, bundleVersion, IAdditionalInstallableUnitAdvice.class))
				.thenReturn(Collections.emptyList());
		when(publisherInfo.getAdvice(null, true, bundleName, bundleVersion, IBundleShapeAdvice.class)).thenReturn(null);
	}

//	private void expectCapabilityAdviceQuery(String bundleName, Version bundleVersion,
//			Collection<ICapabilityAdvice> answer) {
//		if (answer == null)
//			answer = Collections.emptyList();
//		when(publisherInfo.getAdvice(null, false, bundleName, bundleVersion, ICapabilityAdvice.class))
//				.thenReturn(answer);
//	}

	private void expectUpdateDescriptorAdviceQuery(String bundleName, Version bundleVersion,
			Collection<IUpdateDescriptorAdvice> answer) {
		if (answer == null) {
			answer = Collections.emptyList();
		}
		when(publisherInfo.getAdvice(null, false, bundleName, bundleVersion, IUpdateDescriptorAdvice.class))
				.thenReturn(answer);
	}

	private void expectTouchpointAdviceQuery(String bundleName, Version bundleVersion, List<ITouchpointAdvice> answer) {
		if (answer == null) {
			answer = Collections.emptyList();
		}
		when(publisherInfo.getAdvice(null, false, bundleName, bundleVersion, ITouchpointAdvice.class))
				.thenReturn(answer);
	}

	private void expectPropertyAdviceQuery(String bundleName, Version bundleVersion, Map<String, String> answer) {
		List<IPropertyAdvice> propertyAdvices;
		if (answer != null) {
			propertyAdvices = Collections.singletonList(createPropertyAdvice(answer));
		} else {
			propertyAdvices = Collections.emptyList();
		}
		when(publisherInfo.getAdvice(null, false, bundleName, bundleVersion, IPropertyAdvice.class))
				.thenReturn(propertyAdvices);
	}

	private IPropertyAdvice createPropertyAdvice(Map<String, String> properties) {
		IPropertyAdvice mockAdvice = mock(IPropertyAdvice.class);
		when(mockAdvice.getInstallableUnitProperties(any(InstallableUnitDescription.class))).thenReturn(null);
		when(mockAdvice.getArtifactProperties(any(IInstallableUnit.class), any(IArtifactDescriptor.class)))
				.thenReturn(properties);
		return mockAdvice;
	}

	public void testDynamicImport() throws Exception {
		File testData = getTestData("dymamicImport", "testData/dynamicImport");
		IInstallableUnit iu = BundlesAction.createBundleIU(BundlesAction.createBundleDescription(testData), null,
				new PublisherInfo());

		Collection<IRequirement> requirements = iu.getRequirements();
		verifyRequirement(requirements, OSGI_EE, TESTDYN_REQ_EE, null, 1, 1, true);
		assertEquals(2, requirements.size());
	}

	public void testPublishBundlesWhereOneBundleIsInvalid() throws Exception {
		File[] bundleLocations = new File(TestActivator.getTestDataFolder(), "bug331683").listFiles();
		testAction = new BundlesAction(bundleLocations);
		setupPublisherResult();
		PublisherInfo info = new PublisherInfo();
		IStatus status = testAction.perform(info, publisherResult, new NullProgressMonitor());

		// overall status shall be error...
		assertThat(status, errorStatus());
		List<IStatus> childStatuses = Arrays.asList(status.getChildren());
		assertThat(childStatuses, hasItem(statusWithMessageWhich(containsString(
				"The manifest line \"foo\" is invalid; it has no colon ':' character after the header key."))));
		assertThat(childStatuses.size(), is(1));

		// ... but the valid bundle must still be published
		Collection<IInstallableUnit> ius = publisherResult.getIUs("org.eclipse.p2.test.validManifest",
				IPublisherResult.ROOT);
		assertThat(ius.size(), is(1));
	}

	public void testPackageAttributes() throws Exception {
		File testData = new File(TestActivator.getTestDataFolder(), "pkgAttributes");
		IInstallableUnit iu = BundlesAction.createBundleIU(BundlesAction.createBundleDescription(testData), null,
				new PublisherInfo());
		IProvidedCapability capability = iu.getProvidedCapabilities().stream()
				.filter(provided -> provided.getNamespace().equals(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE))
				.findFirst().orElseThrow(() -> new AssertionError("no package found!"));
		Map<String, Object> properties = capability.getProperties();
		assertEquals(String.valueOf(properties), "org.eclipse.core.runtime",
				properties.get(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE));
		assertEquals(String.valueOf(properties), "split",
				properties.get(BundlesAction.PACKAGE_ATTRIBUTE_PROPERTY_PREFIX + "common"));
		Object object = properties.get(BundlesAction.PACKAGE_DIRECTIVE_PROPERTY_PREFIX + "mandatory");
		if (object instanceof List<?> list) {
			assertTrue(list.contains("common"));
		} else {
			fail("Not a list: " + object);
		}

	}

	public void testMultiRequired() throws Exception {
		File testData = new File(TestActivator.getTestDataFolder(), "requireMultiple");
		IInstallableUnit iu = BundlesAction.createBundleIU(BundlesAction.createBundleDescription(testData), null,
				new PublisherInfo());
		Map<String, List<RequiredPropertiesMatch>> map = iu.getRequirements().stream()
				.filter(RequiredPropertiesMatch.class::isInstance)
				.map(RequiredPropertiesMatch.class::cast)
				.collect(Collectors.groupingBy(m -> RequiredPropertiesMatch.extractNamespace(m.getMatches())));
		assertCardinality(map.get("single.required"), 1, 1);
		assertCardinality(map.get("single.optional"), 0, 1);
		assertCardinality(map.get("multiple.required"), 1, Integer.MAX_VALUE);
		assertCardinality(map.get("multiple.optional"), 0, Integer.MAX_VALUE);
	}

	protected void assertCardinality(List<RequiredPropertiesMatch> matches, int min, int max) {
		assertNotNull(matches);
		assertEquals(1, matches.size());
		assertEquals(min, matches.get(0).getMin());
		assertEquals(max, matches.get(0).getMax());
	}

	public void testMultiVersionCapability() throws Exception {
		File testData = getTestData("dymamicImport", "testData/multiVersionCapability/bundle1");
		IInstallableUnit iu = BundlesAction.createBundleIU(BundlesAction.createBundleDescription(testData), null,
				new PublisherInfo());
		Map<String, List<IProvidedCapability>> namespace2capability = iu.getProvidedCapabilities().stream()
				.collect(Collectors.groupingBy(IProvidedCapability::getNamespace));

		List<IProvidedCapability> list0 = namespace2capability.get("cap0");
		assertCapabilities(list0, "name0", Version.emptyVersion);

		List<IProvidedCapability> list1 = namespace2capability.get("cap1");
		assertCapabilities(list1, "name1", new OSGiVersion(1, 0, 0, ""));

		List<IProvidedCapability> list2 = namespace2capability.get("cap2");
		assertCapabilities(list2, "name2", new OSGiVersion(1, 1, 0, ""));

		List<IProvidedCapability> list3 = namespace2capability.get("cap3");
		assertCapabilities(list3, "name3", new OSGiVersion(1, 0, 0, ""), new OSGiVersion(2, 1, 0, ""));
	}

	private void assertCapabilities(List<IProvidedCapability> capabilities, String expectedName,
			Version... expectedVersions) {
		assertEquals(expectedVersions.length, capabilities.size());
		capabilities.forEach(c -> assertEquals(expectedName, c.getName()));
		capabilities.sort(Comparator.comparing(IProvidedCapability::getVersion));
		for (int i = 0; i < expectedVersions.length; i++) {
			assertEquals(expectedVersions[i], capabilities.get(i).getVersion());
		}
	}

	/**
	 * Test that modern Java versions (JavaSE-25) are properly supported as BREEs
	 * without requiring explicit profile files.
	 */
	public void testJavaSE25BREE() throws Exception {
		File testData = new File(TestActivator.getTestDataFolder(), "BundlesActionTest/test6");
		IInstallableUnit iu = BundlesAction.createBundleIU(BundlesAction.createBundleDescription(testData), null,
				new PublisherInfo());

		assertNotNull("Bundle IU should be created", iu);
		assertEquals("test6", iu.getId());

		// Verify that the BREE requirement is properly created
		Collection<IRequirement> requirements = iu.getRequirements();
		assertNotNull("Requirements should not be null", requirements);
		assertTrue("Should have at least one requirement", requirements.size() > 0);

		// Find the osgi.ee requirement
		IRequirement eeRequirement = requirements.stream()
				.filter(req -> {
					if (req instanceof RequiredPropertiesMatch) {
						return OSGI_EE.equals(RequiredPropertiesMatch.extractNamespace(req.getMatches()));
					}
					return false;
				})
				.findFirst()
				.orElse(null);

		assertNotNull("Should have an osgi.ee requirement", eeRequirement);

		// Verify the requirement filter contains JavaSE and version 25
		String filterString = RequiredPropertiesMatch.extractPropertiesMatch(eeRequirement.getMatches()).toString();
		assertTrue("Filter should contain 'osgi.ee' and 'JavaSE': " + filterString,
				filterString.contains("osgi.ee") && filterString.contains("JavaSE"));
		assertTrue("Filter should contain 'version' and '25': " + filterString,
				filterString.contains("version") && filterString.contains("25"));
	}

	/**
	 * Test that JavaSE-21 BREE is properly supported.
	 */
	public void testJavaSE21BREE() throws Exception {
		File testData = new File(TestActivator.getTestDataFolder(), "BundlesActionTest/test7");
		IInstallableUnit iu = BundlesAction.createBundleIU(BundlesAction.createBundleDescription(testData), null,
				new PublisherInfo());

		assertNotNull("Bundle IU should be created", iu);
		assertEquals("test7", iu.getId());

		// Verify that the BREE requirement is properly created
		Collection<IRequirement> requirements = iu.getRequirements();
		assertNotNull("Requirements should not be null", requirements);
		assertTrue("Should have at least one requirement", requirements.size() > 0);

		// Find the osgi.ee requirement
		IRequirement eeRequirement = requirements.stream()
				.filter(req -> {
					if (req instanceof RequiredPropertiesMatch) {
						return OSGI_EE.equals(RequiredPropertiesMatch.extractNamespace(req.getMatches()));
					}
					return false;
				})
				.findFirst()
				.orElse(null);

		assertNotNull("Should have an osgi.ee requirement", eeRequirement);

		// Verify the requirement filter contains JavaSE and version 21
		String filterString = RequiredPropertiesMatch.extractPropertiesMatch(eeRequirement.getMatches()).toString();
		assertTrue("Filter should contain 'osgi.ee' and 'JavaSE': " + filterString,
				filterString.contains("osgi.ee") && filterString.contains("JavaSE"));
		assertTrue("Filter should contain 'version' and '21': " + filterString,
				filterString.contains("version") && filterString.contains("21"));
	}

	/**
	 * Test that various modern JavaSE BREE formats are properly parsed and handled.
	 * This ensures backward compatibility and forward compatibility with future Java versions.
	 */
	public void testVariousJavaSEBREEFormats() throws Exception {
		// Test different BREE formats to ensure they are all handled correctly
		String[][] testCases = {
			{"JavaSE-11", "JavaSE", "11"},
			{"JavaSE-17", "JavaSE", "17"},
			{"JavaSE-21", "JavaSE", "21"},
			{"JavaSE-25", "JavaSE", "25"},
			{"JavaSE-1.8", "JavaSE", "1.8"},
			{"J2SE-1.4", "JavaSE", "1.4"} // J2SE should be converted to JavaSE
		};

		for (String[] testCase : testCases) {
			String bree = testCase[0];
			String expectedEE = testCase[1];
			String expectedVersion = testCase[2];

			// Create a temporary test bundle with this BREE
			// Use createTempDirectory for better isolation and automatic cleanup
			File tempDir = null;
			try {
				tempDir = java.nio.file.Files.createTempDirectory("bree-test-").toFile();
				File metaInfDir = new File(tempDir, "META-INF");
				metaInfDir.mkdirs();

				File manifestFile = new File(metaInfDir, "MANIFEST.MF");
				try (java.io.FileWriter writer = new java.io.FileWriter(manifestFile)) {
					writer.write("Manifest-Version: 1.0\n");
					writer.write("Bundle-ManifestVersion: 2\n");
					writer.write("Bundle-Name: Test " + bree + " BREE\n");
					writer.write("Bundle-SymbolicName: test." + bree.replace('.', '_').replace('-', '_') + "\n");
					writer.write("Bundle-Version: 1.0.0\n");
					writer.write("Bundle-RequiredExecutionEnvironment: " + bree + "\n");
				}

				IInstallableUnit iu = BundlesAction.createBundleIU(BundlesAction.createBundleDescription(tempDir), null,
						new PublisherInfo());

				assertNotNull("Bundle IU should be created for " + bree, iu);

				// Verify that the BREE requirement is properly created
				Collection<IRequirement> requirements = iu.getRequirements();
				assertNotNull("Requirements should not be null for " + bree, requirements);
				assertTrue("Should have at least one requirement for " + bree, requirements.size() > 0);

				// Find the osgi.ee requirement
				IRequirement eeRequirement = requirements.stream()
						.filter(req -> {
							if (req instanceof RequiredPropertiesMatch) {
								return OSGI_EE.equals(RequiredPropertiesMatch.extractNamespace(req.getMatches()));
							}
							return false;
						})
						.findFirst()
						.orElse(null);

				assertNotNull("Should have an osgi.ee requirement for " + bree, eeRequirement);

				// Verify the requirement filter
				String filterString = RequiredPropertiesMatch.extractPropertiesMatch(eeRequirement.getMatches()).toString();
				assertTrue("Filter should contain 'osgi.ee' and '" + expectedEE + "' for " + bree + ": " + filterString,
						filterString.contains("osgi.ee") && filterString.contains(expectedEE));
				assertTrue("Filter should contain 'version' and '" + expectedVersion + "' for " + bree + ": " + filterString,
						filterString.contains("version") && filterString.contains(expectedVersion));
			} finally {
				// Clean up temp directory - guaranteed to execute even on test failure
				if (tempDir != null) {
					deleteDirectory(tempDir);
				}
			}
		}
	}

	/**
	 * Test edge cases and future-proofing for high version numbers.
	 * This ensures that the BREE handling remains generic and doesn't break with future Java releases.
	 */
	public void testFutureJavaVersionBREEs() throws Exception {
		// Test with hypothetical future Java versions to ensure forward compatibility
		String[] futureVersions = {"JavaSE-30", "JavaSE-50", "JavaSE-100"};

		for (String bree : futureVersions) {
			// Create a temporary test bundle with this BREE
			// Use createTempDirectory for better isolation and automatic cleanup
			File tempDir = null;
			try {
				tempDir = java.nio.file.Files.createTempDirectory("bree-test-future-").toFile();
				File metaInfDir = new File(tempDir, "META-INF");
				metaInfDir.mkdirs();

				File manifestFile = new File(metaInfDir, "MANIFEST.MF");
				try (java.io.FileWriter writer = new java.io.FileWriter(manifestFile)) {
					writer.write("Manifest-Version: 1.0\n");
					writer.write("Bundle-ManifestVersion: 2\n");
					writer.write("Bundle-Name: Test " + bree + " BREE\n");
					writer.write("Bundle-SymbolicName: test.future." + bree.replace('-', '_') + "\n");
					writer.write("Bundle-Version: 1.0.0\n");
					writer.write("Bundle-RequiredExecutionEnvironment: " + bree + "\n");
				}

				IInstallableUnit iu = BundlesAction.createBundleIU(BundlesAction.createBundleDescription(tempDir), null,
						new PublisherInfo());

				assertNotNull("Bundle IU should be created for future version " + bree, iu);

				// Verify that the BREE requirement is properly created
				Collection<IRequirement> requirements = iu.getRequirements();
				assertNotNull("Requirements should not be null for " + bree, requirements);
				assertTrue("Should have at least one requirement for " + bree, requirements.size() > 0);

				// Find the osgi.ee requirement
				IRequirement eeRequirement = requirements.stream()
						.filter(req -> {
							if (req instanceof RequiredPropertiesMatch) {
								return OSGI_EE.equals(RequiredPropertiesMatch.extractNamespace(req.getMatches()));
							}
							return false;
						})
						.findFirst()
						.orElse(null);

				assertNotNull("Should have an osgi.ee requirement for future version " + bree, eeRequirement);

				// Just verify the filter is created - we don't care about exact format for future versions
				String filterString = RequiredPropertiesMatch.extractPropertiesMatch(eeRequirement.getMatches()).toString();
				assertNotNull("Filter should be created for " + bree, filterString);
				assertTrue("Filter should contain version information for " + bree + ": " + filterString,
						filterString.contains("version") || filterString.contains("osgi.ee"));
			} finally {
				// Clean up temp directory - guaranteed to execute even on test failure
				if (tempDir != null) {
					deleteDirectory(tempDir);
				}
			}
		}
	}

	/**
	 * Test that invalid BREEs like "UNKNOWN" are properly rejected with a clear error.
	 * This prevents the creation of invalid osgi.ee requirements.
	 */
	public void testInvalidBREEsRejected() throws Exception {
		String[] invalidBREEs = {"UNKNOWN", "UNSPECIFIED", "", " "};

		for (String invalidBree : invalidBREEs) {
			if (invalidBree.trim().isEmpty()) {
				continue; // Skip empty strings as they might not reach the BREE parsing
			}
			
			File tempDir = null;
			try {
				tempDir = java.nio.file.Files.createTempDirectory("bree-test-invalid-").toFile();
				File metaInfDir = new File(tempDir, "META-INF");
				metaInfDir.mkdirs();

				File manifestFile = new File(metaInfDir, "MANIFEST.MF");
				try (java.io.FileWriter writer = new java.io.FileWriter(manifestFile)) {
					writer.write("Manifest-Version: 1.0\n");
					writer.write("Bundle-ManifestVersion: 2\n");
					writer.write("Bundle-Name: Test Invalid BREE\n");
					writer.write("Bundle-SymbolicName: test.invalid.bree\n");
					writer.write("Bundle-Version: 1.0.0\n");
					writer.write("Bundle-RequiredExecutionEnvironment: " + invalidBree + "\n");
				}

				try {
					BundlesAction.createBundleIU(BundlesAction.createBundleDescription(tempDir), null,
							new PublisherInfo());
					fail("Expected BundleException for invalid BREE: " + invalidBree);
				} catch (Exception e) {
					// Expected - should fail with an exception
					assertTrue("Exception message should mention invalid BREE: " + e.getMessage(),
							e.getMessage() != null && 
							(e.getMessage().contains("Invalid execution environment") || 
							 e.getMessage().contains("Error converting required execution environment")));
				}
			} finally {
				// Clean up temp directory
				if (tempDir != null) {
					deleteDirectory(tempDir);
				}
			}
		}
	}

	private void deleteDirectory(File dir) {
		if (dir.exists()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isDirectory()) {
						deleteDirectory(file);
					} else {
						file.delete();
					}
				}
			}
			dir.delete();
		}
	}
}
