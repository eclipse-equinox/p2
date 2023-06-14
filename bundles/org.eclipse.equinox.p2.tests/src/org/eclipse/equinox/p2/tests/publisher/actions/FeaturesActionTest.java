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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitPatch;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.ITouchpointInstruction;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.IAdditionalInstallableUnitAdvice;
import org.eclipse.equinox.p2.publisher.actions.ICapabilityAdvice;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.publisher.actions.ITouchpointAdvice;
import org.eclipse.equinox.p2.publisher.actions.IUpdateDescriptorAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.TestMetadataRepository;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class FeaturesActionTest extends ActionTest {

	public static IArtifactKey FOO_KEY = ArtifactKey.parse("org.eclipse.update.feature,foo,1.0.0"); //$NON-NLS-1$
	public static IArtifactKey BAR_KEY = ArtifactKey.parse("org.eclipse.update.feature,bar,1.1.1"); //$NON-NLS-1$

	private static File root = new File(TestActivator.getTestDataFolder().toString(), "FeaturesActionTest"); //$NON-NLS-1$
	protected TestArtifactRepository artifactRepository = new TestArtifactRepository(getAgent());
	protected TestMetadataRepository metadataRepository;
	private Version fooVersion = Version.create("1.0.0"); //$NON-NLS-1$
	private Version barVersion = Version.create("1.1.1"); //$NON-NLS-1$
	private String BAR = "bar"; //$NON-NLS-1$
	private String FOO = "foo"; //$NON-NLS-1$
	private ArgumentCaptor<ITouchpointAdvice> capture = ArgumentCaptor.forClass(ITouchpointAdvice.class);

	@Override
	public void setUp() throws Exception {
		super.setUp();
		testAction = Mockito.spy(new FeaturesAction(new File[] { root }));
		setupPublisherInfo();
		setupPublisherResult();
	}

	/**
	 * Tests publishing two simple features.
	 */
	public void testSimple() throws Exception {
		testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		verify(publisherInfo, Mockito.atLeastOnce()).addAdvice(capture.capture());
		verifyRepositoryContents();
		debug("Completed FeaturesAction."); //$NON-NLS-1$
	}

	public void testFeaturePatch() throws Exception {
		File testFolder = getTestFolder("FeaturesAction.testFilters");
		StringBuilder buffer = new StringBuilder();
		buffer.append("<feature id=\"test.feature\" version=\"1.0.0\" >                                       \n");
		buffer.append("   <requires>                                                                          \n");
		buffer.append(
				"      <import feature=\"org.foo\" version=\"[1.0.0,2.0.0)\" match=\"versionRange\" patch=\"true\"/>  \n");
		buffer.append("   </requires>                                                                         \n");
		buffer.append("</feature>                                                                             \n");
		File featureXML = new File(testFolder, "feature.xml");
		writeBuffer(featureXML, buffer);

		publisherInfo = new PublisherInfo();
		FeaturesAction action = new FeaturesAction(new File[] { testFolder });
		action.perform(publisherInfo, publisherResult, new NullProgressMonitor());

		IInstallableUnitPatch iu = (IInstallableUnitPatch) publisherResult.getIU("test.feature.feature.group",
				Version.parseVersion("1.0.0"), null);
		IRequirement[][] applicabilityScope = iu.getApplicabilityScope();
		assertEquals(1, applicabilityScope.length);
		IRequiredCapability require = (IRequiredCapability) applicabilityScope[0][0];

		IRequirement expected = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID,
				"org.foo.feature.group", VersionRange.create("[1.0.0, 2.0.0)"), null, false, false, true);
		verifyRequirement(Collections.singleton(expected), require);
	}

	public void testMatchRange() throws Exception {
		File testFolder = getTestFolder("FeaturesAction.testFilters");
		StringBuilder buffer = new StringBuilder();
		buffer.append("<feature id=\"test.feature\" version=\"1.0.0\" >                                       \n");
		buffer.append("   <requires>                                                                          \n");
		buffer.append("      <import plugin=\"org.plug\" version=\"[1.0.0,2.0.0)\" match=\"versionRange\" />  \n");
		buffer.append("      <import feature=\"org.foo\" version=\"[1.0.0,2.0.0)\" match=\"versionRange\" />  \n");
		buffer.append("   </requires>                                                                         \n");
		buffer.append("</feature>                                                                             \n");
		File featureXML = new File(testFolder, "feature.xml");
		writeBuffer(featureXML, buffer);

		publisherInfo = new PublisherInfo();
		FeaturesAction action = new FeaturesAction(new File[] { testFolder });
		action.perform(publisherInfo, publisherResult, new NullProgressMonitor());

		IInstallableUnit iu = publisherResult.getIU("test.feature.feature.group", Version.parseVersion("1.0.0"), null);
		Collection<IRequirement> requires = iu.getRequirements();
		assertEquals(3, requires.size());
		for (IRequirement require : requires) {
			String requireName = ((IRequiredCapability) require).getName();

			if (requireName.equals("org.foo.feature.group")) {
				IRequirement expected = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID,
						"org.foo.feature.group", VersionRange.create("[1.0.0, 2.0.0)"), null, false, false, true);
				verifyRequirement(Collections.singleton(expected), require);
			} else if (requireName.equals("org.plug")) {
				IRequirement expected = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "org.plug",
						VersionRange.create("[1.0.0, 2.0.0)"), null, false, false, true);
				verifyRequirement(Collections.singleton(expected), require);
			}
		}
	}

	public void testMatchGreaterOrEqual() throws Exception {
		File testFolder = getTestFolder("FeaturesAction.testFilters");
		StringBuilder buffer = new StringBuilder();
		buffer.append("<feature id=\"test.feature\" version=\"1.0.0\" >                                       \n");
		buffer.append("   <requires>                                                                          \n");
		buffer.append("      <import plugin=\"org.plug\" version=\"1.0.0\" match=\"greaterOrEqual\" />        \n");
		buffer.append("      <import feature=\"org.foo\" version=\"1.0.0\" match=\"greaterOrEqual\" />        \n");
		buffer.append("   </requires>                                                                         \n");
		buffer.append("</feature>                                                                             \n");
		File featureXML = new File(testFolder, "feature.xml");
		writeBuffer(featureXML, buffer);

		publisherInfo = new PublisherInfo();
		FeaturesAction action = new FeaturesAction(new File[] { testFolder });
		action.perform(publisherInfo, publisherResult, new NullProgressMonitor());

		IInstallableUnit iu = publisherResult.getIU("test.feature.feature.group", Version.parseVersion("1.0.0"), null);
		Collection<IRequirement> requires = iu.getRequirements();
		assertEquals(3, requires.size());
		for (IRequirement require : requires) {
			String requireName = ((IRequiredCapability) require).getName();

			if (requireName.equals("org.foo.feature.group")) {
				IRequirement expected = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID,
						"org.foo.feature.group", VersionRange.create("1.0.0"), null, false, false, true);
				verifyRequirement(Collections.singleton(expected), require);
			} else if (requireName.equals("org.plug")) {
				IRequirement expected = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "org.plug",
						VersionRange.create("1.0.0"), null, false, false, true);
				verifyRequirement(Collections.singleton(expected), require);
			}
		}
	}

	public void testFilters() throws Exception {
		File testFolder = getTestFolder("FeaturesAction.testFilters");
		StringBuilder buffer = new StringBuilder();
		buffer.append("<feature id=\"test.feature\" version=\"1.0.0\" >                                       \n");
		buffer.append("   <includes id=\"org.foo\" version=\"1.0.0\" filter=\"(osgi.os=win32)\"/>             \n");
		buffer.append("   <plugin id=\"org.plug\" version=\"1.0.0\" filter=\"(my.prop=foo)\" os=\"win32\" />  \n");
		buffer.append("   <requires>                                                                          \n");
		buffer.append("      <import plugin=\"org.plug2\" version=\"1.0.0\" filter=\"(my.prop=foo)\" />       \n");
		buffer.append("      <import feature=\"org.foo2\" version=\"1.0.0\" filter=\"(my.prop=foo)\" />       \n");
		buffer.append("   </requires>                                                                         \n");
		buffer.append("</feature>                                                                             \n");
		File featureXML = new File(testFolder, "feature.xml");
		writeBuffer(featureXML, buffer);

		publisherInfo = new PublisherInfo();
		FeaturesAction action = new FeaturesAction(new File[] { testFolder });
		action.perform(publisherInfo, publisherResult, new NullProgressMonitor());

		IInstallableUnit iu = publisherResult.getIU("test.feature.feature.group", Version.parseVersion("1.0.0"), null);
		Collection<IRequirement> requires = iu.getRequirements();
		assertEquals(5, requires.size());
		for (IRequirement require : requires) {
			if (((IRequiredCapability) require).getName().equals("org.foo.feature.group")) {
				assertEquals(ExpressionUtil.parseLDAP("(osgi.os=win32)"), require.getFilter().getParameters()[0]);
			} else if (((IRequiredCapability) require).getName().equals("org.plug")) {
				assertEquals(ExpressionUtil.parseLDAP("(&(my.prop=foo)(osgi.os=win32))"),
						require.getFilter().getParameters()[0]);
			} else if (((IRequiredCapability) require).getName().equals("org.plug2")) {
				assertEquals(ExpressionUtil.parseLDAP("(my.prop=foo)"), require.getFilter().getParameters()[0]);
			} else if (((IRequiredCapability) require).getName().equals("org.foo2.feature.group")) {
				assertEquals(ExpressionUtil.parseLDAP("(my.prop=foo)"), require.getFilter().getParameters()[0]);
			}
		}
	}

	private void verifyRepositoryContents() throws Exception {
		verifyArtifacts();
		verifyMetadata();
	}

	private void verifyMetadata() {
		// {foo.feature.jar=[foo.feature.jar 1.0.0], bar.feature.jar=[bar.feature.jar
		// 1.1.1], foo.feature.group=[foo.feature.group 1.0.0],
		// bar.feature.group=[bar.feature.group 1.1.1]}
		ArrayList<IInstallableUnit> fooIUs = new ArrayList<>(
				publisherResult.getIUs("foo.feature.jar", IPublisherResult.NON_ROOT)); //$NON-NLS-1$
		assertEquals(1, fooIUs.size());
		IInstallableUnit foo = fooIUs.get(0);
		assertTrue(foo.getId().equalsIgnoreCase("foo.feature.jar")); //$NON-NLS-1$
		assertEquals(fooVersion, foo.getVersion());
		assertEquals("Foo Feature", foo.getProperty(IInstallableUnit.PROP_NAME));
		assertEquals("Foo Description", foo.getProperty(IInstallableUnit.PROP_DESCRIPTION));
		assertEquals("Foo License", foo.getLicenses().iterator().next().getBody());
		assertEquals("Foo Copyright", foo.getCopyright().getBody());
		assertEquals("value1", foo.getProperty("key1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("value2", foo.getProperty("key2")); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals(FOO_KEY, foo.getArtifacts().iterator().next());
		assertEquals(foo.getFilter().getParameters()[0],
				ExpressionUtil.parseLDAP("(org.eclipse.update.install.features=true)")); //$NON-NLS-1$

		// check touchpointType
		assertTrue(foo.getTouchpointType().getId().equalsIgnoreCase("org.eclipse.equinox.p2.osgi")); //$NON-NLS-1$
		assertEquals(fooVersion, foo.getTouchpointType().getVersion());

		// zipped=true
		Collection<ITouchpointData> tpData = foo.getTouchpointData();
		String fooValue = tpData.iterator().next().getInstructions().get("zipped").getBody(); //$NON-NLS-1$
		assertTrue(fooValue.equalsIgnoreCase("true")); //$NON-NLS-1$

		Collection<IRequirement> fooRequiredCapabilities = foo.getRequirements();
		assertTrue(fooRequiredCapabilities.isEmpty());

		Collection<IProvidedCapability> fooProvidedCapabilities = foo.getProvidedCapabilities();
		verifyProvidedCapability(fooProvidedCapabilities, IInstallableUnit.NAMESPACE_IU_ID, "foo.feature.jar", //$NON-NLS-1$
				fooVersion);
		verifyProvidedCapability(fooProvidedCapabilities, PublisherHelper.NAMESPACE_ECLIPSE_TYPE, "feature", //$NON-NLS-1$
				fooVersion);
		verifyProvidedCapability(fooProvidedCapabilities, "org.eclipse.update.feature", FOO, fooVersion); //$NON-NLS-1$
		assertEquals(3, fooProvidedCapabilities.size());

		// feature group IU for foo
		fooIUs = new ArrayList<>(publisherResult.getIUs("foo.feature.group", IPublisherResult.ROOT)); //$NON-NLS-1$
		assertEquals(1, fooIUs.size());
		IInstallableUnit fooGroup = fooIUs.get(0);
		ITouchpointAdvice tData = capture.getValue();
		ITouchpointInstruction instruction = tData.getTouchpointData(NO_TP_DATA).getInstruction("install");
		assertNotNull(instruction);
		assertEquals(
				"ln(targetDir:@artifact,linkTarget:foo/lib.1.so,linkName:lib.so);chmod(targetDir:@artifact,targetFile:lib/lib.so,permissions:755);",
				instruction.getBody());
		assertNull(fooGroup.getFilter());

		/* verify bar */
		ArrayList<IInstallableUnit> barIUs = new ArrayList<>(
				publisherResult.getIUs("bar.feature.jar", IPublisherResult.NON_ROOT)); //$NON-NLS-1$
		assertEquals(1, barIUs.size());
		IInstallableUnit bar = barIUs.get(0);
		assertEquals("bar.feature.jar", bar.getId()); //$NON-NLS-1$
		assertEquals(barVersion, bar.getVersion());
		assertEquals("value1", bar.getProperty("key1")); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("value2", bar.getProperty("key2")); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue(bar.getProperties().containsKey("org.eclipse.update.installHandler")); //$NON-NLS-1$
		assertTrue(bar.getProperties().containsValue("handler=bar handler")); //$NON-NLS-1$
		assertEquals(BAR_KEY, bar.getArtifacts().iterator().next());
		assertEquals(bar.getFilter().getParameters()[0],
				ExpressionUtil.parseLDAP("(org.eclipse.update.install.features=true)")); //$NON-NLS-1$
		assertTrue(bar.isSingleton());

		barIUs = new ArrayList<>(publisherResult.getIUs("bar.feature.group", IPublisherResult.ROOT)); //$NON-NLS-1$
		assertEquals(1, fooIUs.size());
		IInstallableUnit barGroup = barIUs.get(0);
		Collection<IRequirement> barRequiredCapabilities = barGroup.getRequirements();
		// contains(barRequiredCapabilities, IInstallableUnit.NAMESPACE_IU_ID,
		// "bar_root", new VersionRange(barVersion, true, barVersion, true), null, false
		// /*multiple*/, false /*optional*/); //$NON-NLS-1$//$NON-NLS-2$
		verifyRequirement(barRequiredCapabilities, IInstallableUnit.NAMESPACE_IU_ID, "bar.feature.jar", //$NON-NLS-1$
				new VersionRange(barVersion, true, barVersion, true), "(org.eclipse.update.install.features=true)", 1, //$NON-NLS-1$
				1, true);
		verifyRequirement(barRequiredCapabilities, IInstallableUnit.NAMESPACE_IU_ID, "org.bar.feature.feature.group", //$NON-NLS-1$
				VersionRange.emptyRange, "(&(|(osgi.nl=de)(osgi.nl=en)(osgi.nl=fr)))", 1, 1, true); //$NON-NLS-1$
		assertEquals(barGroup.getFilter().getParameters()[0], ExpressionUtil.parseLDAP(
				"(&(|(osgi.os=macosx)(osgi.os=win32))(|(osgi.ws=carbon)(osgi.ws=win32))(|(osgi.arch=ppc)(osgi.arch=x86))(osgi.nl=en))"));

		// check zipped=true in touchpointData
		String barValue = bar.getTouchpointData().iterator().next().getInstructions().get("zipped").getBody(); //$NON-NLS-1$
		assertTrue(barValue.equalsIgnoreCase("true")); //$NON-NLS-1$

		// check touchpointType
		assertTrue(bar.getTouchpointType().getId().equalsIgnoreCase("org.eclipse.equinox.p2.osgi")); //$NON-NLS-1$
		assertEquals(fooVersion, bar.getTouchpointType().getVersion());
		// String namespace, String name, VersionRange range, String filter, boolean
		// optional, boolean multiple, boolean greedy)
		barRequiredCapabilities = bar.getRequirements();

		assertTrue(barRequiredCapabilities.isEmpty());

		Collection<IProvidedCapability> barProvidedCapabilities = bar.getProvidedCapabilities();
		verifyProvidedCapability(barProvidedCapabilities, IInstallableUnit.NAMESPACE_IU_ID, "bar.feature.jar", //$NON-NLS-1$
				barVersion);
		verifyProvidedCapability(barProvidedCapabilities, PublisherHelper.NAMESPACE_ECLIPSE_TYPE, "feature", //$NON-NLS-1$
				fooVersion);
		verifyProvidedCapability(barProvidedCapabilities, "org.eclipse.update.feature", BAR, barVersion); //$NON-NLS-1$
		assertEquals(3, barProvidedCapabilities.size());
	}

	private void verifyArtifacts() throws IOException {
		ZipInputStream actualStream = artifactRepository.getZipInputStream(FOO_KEY);
		Map<String, Object[]> expected = getFileMap(new HashMap<>(), new File[] { new File(root, FOO) },
				IPath.fromFile(new File(root, FOO)));
		TestData.assertContains(expected, actualStream, true);

		expected = getFileMap(new HashMap<>(), new File[] { new File(root, BAR) },
				IPath.fromFile(new File(root, BAR)));
		actualStream = artifactRepository.getZipInputStream(BAR_KEY);
		TestData.assertContains(expected, actualStream, true);
	}

	@Override
	protected void insertPublisherInfoBehavior() {
		// setup metadataRepository with barIU
		metadataRepository = new TestMetadataRepository(getAgent(), new IInstallableUnit[] { mockIU(BAR, null) });

		List<IPropertyAdvice> adviceCollection = fillAdvice(new ArrayList<>());
		when(publisherInfo.getAdvice(null, false, "bar.feature.jar", barVersion, IPropertyAdvice.class))
				.thenReturn(adviceCollection);
		when(publisherInfo.getAdvice(null, false, "bar", barVersion, IPropertyAdvice.class))
				.thenReturn(adviceCollection);
		when(publisherInfo.getAdvice(null, false, "bar", barVersion, IFeatureRootAdvice.class))
				.thenReturn(Collections.emptyList());
		when(publisherInfo.getAdvice(null, false, "bar.feature.group", barVersion, IPropertyAdvice.class))
				.thenReturn(adviceCollection);
		when(publisherInfo.getAdvice(null, false, "bar.feature.group", barVersion, ITouchpointAdvice.class))
				.thenReturn(Collections.emptyList());
		when(publisherInfo.getAdvice(null, false, "bar.feature.group", barVersion, ICapabilityAdvice.class))
				.thenReturn(Collections.emptyList());
		when(publisherInfo.getAdvice(null, false, "bar.feature.group", barVersion,
				IAdditionalInstallableUnitAdvice.class)).thenReturn(Collections.emptyList());
		when(publisherInfo.getAdvice(null, false, "foo.feature.jar", fooVersion, IPropertyAdvice.class))
				.thenReturn(adviceCollection);
		when(publisherInfo.getAdvice(null, false, "bar.feature.group", barVersion, IUpdateDescriptorAdvice.class))
				.thenReturn(Collections.emptyList());
		when(publisherInfo.getAdvice(null, false, "foo", fooVersion, IPropertyAdvice.class))
				.thenReturn(adviceCollection);
		when(publisherInfo.getAdvice(null, false, "foo", fooVersion, IFeatureRootAdvice.class))
				.thenReturn(Collections.emptyList());
		when(publisherInfo.getAdvice(null, false, "foo.feature.group", fooVersion, IPropertyAdvice.class))
				.thenReturn(adviceCollection);
		when(publisherInfo.getAdvice(null, false, "foo.feature.group", fooVersion, ICapabilityAdvice.class))
				.thenReturn(Collections.emptyList());
		when(publisherInfo.getAdvice(null, false, "foo.feature.group", fooVersion,
				IAdditionalInstallableUnitAdvice.class)).thenReturn(Collections.emptyList());
		when(publisherInfo.getAdvice(null, false, "foo.feature.group", fooVersion, IUpdateDescriptorAdvice.class))
				.thenReturn(Collections.emptyList());
		when(publisherInfo.getArtifactOptions())
				.thenReturn(IPublisherInfo.A_INDEX | IPublisherInfo.A_OVERWRITE | IPublisherInfo.A_PUBLISH);
		when(publisherInfo.getArtifactRepository()).thenReturn(artifactRepository);
		when(publisherInfo.getMetadataRepository()).thenReturn(metadataRepository);
		when(publisherInfo.getContextMetadataRepository()).thenReturn(null);
	}

	private List<IPropertyAdvice> fillAdvice(ArrayList<IPropertyAdvice> adviceCollection) {
		Map<String, String> prop = new HashMap<>();
		prop.put("key1", "value1"); //$NON-NLS-1$//$NON-NLS-2$
		prop.put("key2", "value2"); //$NON-NLS-1$//$NON-NLS-2$
		IPropertyAdvice propertyAdvice = mock(IPropertyAdvice.class);
		when(propertyAdvice.getInstallableUnitProperties(any(InstallableUnitDescription.class))).thenReturn(prop);
		when(propertyAdvice.getArtifactProperties(any(IInstallableUnit.class), any(IArtifactDescriptor.class)))
				.thenReturn(null);
		adviceCollection.add(propertyAdvice);
		return adviceCollection;
	}
}
