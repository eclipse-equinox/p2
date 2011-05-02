/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.easymock.EasyMock.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.*;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.tests.*;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

@SuppressWarnings({"unchecked"})
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
	private Capture<ITouchpointAdvice> tpAdvice;

	public void setUp() throws Exception {
		testAction = new FeaturesAction(new File[] {root});
		tpAdvice = new Capture<ITouchpointAdvice>();
		setupPublisherInfo();
		setupPublisherResult();
	}

	/**
	 * Tests publishing two simple features.
	 */
	public void testSimple() throws Exception {
		testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		verifyRepositoryContents();
		debug("Completed FeaturesAction."); //$NON-NLS-1$
	}

	public void testFeaturePatch() throws Exception {
		File testFolder = getTestFolder("FeaturesAction.testFilters");
		StringBuffer buffer = new StringBuffer();
		buffer.append("<feature id=\"test.feature\" version=\"1.0.0\" >                                       \n");
		buffer.append("   <requires>                                                                          \n");
		buffer.append("      <import feature=\"org.foo\" version=\"[1.0.0,2.0.0)\" match=\"versionRange\" patch=\"true\"/>  \n");
		buffer.append("   </requires>                                                                         \n");
		buffer.append("</feature>                                                                             \n");
		File featureXML = new File(testFolder, "feature.xml");
		writeBuffer(featureXML, buffer);

		publisherInfo = new PublisherInfo();
		FeaturesAction action = new FeaturesAction(new File[] {testFolder});
		action.perform(publisherInfo, publisherResult, new NullProgressMonitor());

		IInstallableUnitPatch iu = (IInstallableUnitPatch) publisherResult.getIU("test.feature.feature.group", Version.parseVersion("1.0.0"), null);
		IRequirement[][] applicabilityScope = iu.getApplicabilityScope();
		assertEquals(1, applicabilityScope.length);
		IRequiredCapability require = (IRequiredCapability) applicabilityScope[0][0];
		assertEquals("org.foo.feature.group", require.getName());
		IMatchExpression<IInstallableUnit> matches = require.getMatches();
		assertEquals("providedCapabilities.exists(x | x.name == $0 && x.namespace == $1 && x.version >= $2 && x.version < $3)", matches.toString());
		assertEquals(Version.parseVersion("1.0.0"), matches.getParameters()[2]);
		assertEquals(Version.parseVersion("2.0.0"), matches.getParameters()[3]);
	}

	public void testMatchRange() throws Exception {
		File testFolder = getTestFolder("FeaturesAction.testFilters");
		StringBuffer buffer = new StringBuffer();
		buffer.append("<feature id=\"test.feature\" version=\"1.0.0\" >                                       \n");
		buffer.append("   <requires>                                                                          \n");
		buffer.append("      <import plugin=\"org.plug\" version=\"[1.0.0,2.0.0)\" match=\"versionRange\" />  \n");
		buffer.append("      <import feature=\"org.foo\" version=\"[1.0.0,2.0.0)\" match=\"versionRange\" />  \n");
		buffer.append("   </requires>                                                                         \n");
		buffer.append("</feature>                                                                             \n");
		File featureXML = new File(testFolder, "feature.xml");
		writeBuffer(featureXML, buffer);

		publisherInfo = new PublisherInfo();
		FeaturesAction action = new FeaturesAction(new File[] {testFolder});
		action.perform(publisherInfo, publisherResult, new NullProgressMonitor());

		IInstallableUnit iu = publisherResult.getIU("test.feature.feature.group", Version.parseVersion("1.0.0"), null);
		Collection<IRequirement> requires = iu.getRequirements();
		assertEquals(3, requires.size());
		for (IRequirement require : requires) {
			if (((IRequiredCapability) require).getName().equals("org.foo.feature.group")) {
				IMatchExpression<IInstallableUnit> matches = require.getMatches();
				assertEquals("providedCapabilities.exists(x | x.name == $0 && x.namespace == $1 && x.version >= $2 && x.version < $3)", matches.toString());
				assertEquals(Version.parseVersion("1.0.0"), matches.getParameters()[2]);
				assertEquals(Version.parseVersion("2.0.0"), matches.getParameters()[3]);
			} else if (((IRequiredCapability) require).getName().equals("org.plug")) {
				IMatchExpression<IInstallableUnit> matches = require.getMatches();
				assertEquals("providedCapabilities.exists(x | x.name == $0 && x.namespace == $1 && x.version >= $2 && x.version < $3)", matches.toString());
				assertEquals(Version.parseVersion("1.0.0"), matches.getParameters()[2]);
				assertEquals(Version.parseVersion("2.0.0"), matches.getParameters()[3]);
			}
		}
	}

	public void testFilters() throws Exception {
		File testFolder = getTestFolder("FeaturesAction.testFilters");
		StringBuffer buffer = new StringBuffer();
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
		FeaturesAction action = new FeaturesAction(new File[] {testFolder});
		action.perform(publisherInfo, publisherResult, new NullProgressMonitor());

		IInstallableUnit iu = publisherResult.getIU("test.feature.feature.group", Version.parseVersion("1.0.0"), null);
		Collection<IRequirement> requires = iu.getRequirements();
		assertEquals(5, requires.size());
		for (IRequirement require : requires) {
			if (((IRequiredCapability) require).getName().equals("org.foo.feature.group")) {
				assertEquals(ExpressionUtil.parseLDAP("(osgi.os=win32)"), require.getFilter().getParameters()[0]);
			} else if (((IRequiredCapability) require).getName().equals("org.plug")) {
				assertEquals(ExpressionUtil.parseLDAP("(&(my.prop=foo)(osgi.os=win32))"), require.getFilter().getParameters()[0]);
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
		//{foo.feature.jar=[foo.feature.jar 1.0.0], bar.feature.jar=[bar.feature.jar 1.1.1], foo.feature.group=[foo.feature.group 1.0.0], bar.feature.group=[bar.feature.group 1.1.1]}
		ArrayList fooIUs = new ArrayList(publisherResult.getIUs("foo.feature.jar", IPublisherResult.NON_ROOT)); //$NON-NLS-1$
		assertTrue(fooIUs.size() == 1);
		IInstallableUnit foo = (IInstallableUnit) fooIUs.get(0);
		assertTrue(foo.getId().equalsIgnoreCase("foo.feature.jar")); //$NON-NLS-1$
		assertTrue(foo.getVersion().equals(fooVersion));
		assertEquals("Foo Feature", foo.getProperty(IInstallableUnit.PROP_NAME));
		assertEquals("Foo Description", foo.getProperty(IInstallableUnit.PROP_DESCRIPTION));
		assertEquals("Foo License", foo.getLicenses().iterator().next().getBody());
		assertEquals("Foo Copyright", foo.getCopyright().getBody());
		assertTrue(foo.getProperty("key1").equals("value1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(foo.getProperty("key2").equals("value2")); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue(foo.getArtifacts().iterator().next().equals(FOO_KEY));
		assertEquals(foo.getFilter().getParameters()[0], ExpressionUtil.parseLDAP("(org.eclipse.update.install.features=true)")); //$NON-NLS-1$

		//check touchpointType
		assertTrue(foo.getTouchpointType().getId().equalsIgnoreCase("org.eclipse.equinox.p2.osgi")); //$NON-NLS-1$
		assertTrue(foo.getTouchpointType().getVersion().equals(fooVersion));

		//zipped=true
		Collection<ITouchpointData> tpData = foo.getTouchpointData();
		String fooValue = tpData.iterator().next().getInstructions().get("zipped").getBody(); //$NON-NLS-1$
		assertTrue(fooValue.equalsIgnoreCase("true")); //$NON-NLS-1$

		Collection<IRequirement> fooRequiredCapabilities = foo.getRequirements();
		assertTrue(fooRequiredCapabilities.size() == 0);

		Collection<IProvidedCapability> fooProvidedCapabilities = foo.getProvidedCapabilities();
		contains(fooProvidedCapabilities, IInstallableUnit.NAMESPACE_IU_ID, "foo.feature.jar", fooVersion); //$NON-NLS-1$
		contains(fooProvidedCapabilities, PublisherHelper.NAMESPACE_ECLIPSE_TYPE, "feature", fooVersion); //$NON-NLS-1$ 
		contains(fooProvidedCapabilities, "org.eclipse.update.feature", FOO, fooVersion); //$NON-NLS-1$
		assertTrue(fooProvidedCapabilities.size() == 3);

		//feature group IU for foo
		fooIUs = new ArrayList(publisherResult.getIUs("foo.feature.group", IPublisherResult.ROOT)); //$NON-NLS-1$
		assertTrue(fooIUs.size() == 1);
		IInstallableUnit fooGroup = (IInstallableUnit) fooIUs.get(0);
		tpData = fooGroup.getTouchpointData();
		assertEquals(1, tpData.size());
		ITouchpointInstruction instruction = tpData.iterator().next().getInstruction("install");
		assertNotNull(instruction);
		assertEquals("ln(targetDir:@artifact,linkTarget:foo/lib.1.so,linkName:lib.so);chmod(targetDir:@artifact,targetFile:lib/lib.so,permissions:755);", instruction.getBody());
		assertNull(fooGroup.getFilter());

		/*verify bar*/
		ArrayList barIUs = new ArrayList(publisherResult.getIUs("bar.feature.jar", IPublisherResult.NON_ROOT)); //$NON-NLS-1$
		assertTrue(barIUs.size() == 1);
		IInstallableUnit bar = (IInstallableUnit) barIUs.get(0);
		assertTrue(bar.getId().equals("bar.feature.jar")); //$NON-NLS-1$
		assertTrue(bar.getVersion().equals(barVersion));
		assertTrue(bar.getProperty("key1").equals("value1")); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue(bar.getProperty("key2").equals("value2")); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue(bar.getProperties().containsKey("org.eclipse.update.installHandler")); //$NON-NLS-1$
		assertTrue(bar.getProperties().containsValue("handler=bar handler")); //$NON-NLS-1$
		assertTrue(bar.getArtifacts().iterator().next().equals(BAR_KEY));
		assertEquals(bar.getFilter().getParameters()[0], ExpressionUtil.parseLDAP("(org.eclipse.update.install.features=true)")); //$NON-NLS-1$
		assertTrue(bar.isSingleton());

		barIUs = new ArrayList(publisherResult.getIUs("bar.feature.group", IPublisherResult.ROOT)); //$NON-NLS-1$
		assertTrue(fooIUs.size() == 1);
		IInstallableUnit barGroup = (IInstallableUnit) barIUs.get(0);
		Collection<IRequirement> barRequiredCapabilities = barGroup.getRequirements();
		//contains(barRequiredCapabilities, IInstallableUnit.NAMESPACE_IU_ID, "bar_root", new VersionRange(barVersion, true, barVersion, true), null, false /*multiple*/, false /*optional*/); //$NON-NLS-1$//$NON-NLS-2$
		contains(barRequiredCapabilities, IInstallableUnit.NAMESPACE_IU_ID, "bar.feature.jar", new VersionRange(barVersion, true, barVersion, true), "(org.eclipse.update.install.features=true)", false /*multiple*/, false /*optional*/); //$NON-NLS-1$//$NON-NLS-2$
		contains(barRequiredCapabilities, IInstallableUnit.NAMESPACE_IU_ID, "org.bar.feature.feature.group", VersionRange.emptyRange, "(&(|(osgi.nl=de)(osgi.nl=en)(osgi.nl=fr)))", false /*multiple*/, false /*optional*/); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals(barGroup.getFilter().getParameters()[0], ExpressionUtil.parseLDAP("(&(|(osgi.os=macosx)(osgi.os=win32))(|(osgi.ws=carbon)(osgi.ws=win32))(|(osgi.arch=ppc)(osgi.arch=x86))(osgi.nl=en))"));

		//check zipped=true in touchpointData
		String barValue = bar.getTouchpointData().iterator().next().getInstructions().get("zipped").getBody(); //$NON-NLS-1$
		assertTrue(barValue.equalsIgnoreCase("true")); //$NON-NLS-1$

		//check touchpointType
		assertTrue(bar.getTouchpointType().getId().equalsIgnoreCase("org.eclipse.equinox.p2.osgi")); //$NON-NLS-1$
		assertTrue(bar.getTouchpointType().getVersion().equals(fooVersion));
		//String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple, boolean greedy)
		barRequiredCapabilities = bar.getRequirements();

		assertTrue(barRequiredCapabilities.size() == 0);

		Collection<IProvidedCapability> barProvidedCapabilities = bar.getProvidedCapabilities();
		contains(barProvidedCapabilities, IInstallableUnit.NAMESPACE_IU_ID, "bar.feature.jar", barVersion); //$NON-NLS-1$ 
		contains(barProvidedCapabilities, PublisherHelper.NAMESPACE_ECLIPSE_TYPE, "feature", fooVersion); //$NON-NLS-1$ 
		contains(barProvidedCapabilities, "org.eclipse.update.feature", BAR, barVersion); //$NON-NLS-1$
		assertTrue(barProvidedCapabilities.size() == 3);
	}

	private void verifyArtifacts() throws IOException {
		ZipInputStream actualStream = artifactRepository.getZipInputStream(FOO_KEY);
		Map expected = getFileMap(new HashMap(), new File[] {new File(root, FOO)}, new Path(new File(root, FOO).getAbsolutePath()));
		TestData.assertContains(expected, actualStream, true);

		expected = getFileMap(new HashMap(), new File[] {new File(root, BAR)}, new Path(new File(root, BAR).getAbsolutePath()));
		actualStream = artifactRepository.getZipInputStream(BAR_KEY);
		TestData.assertContains(expected, actualStream, true);
	}

	protected void insertPublisherInfoBehavior() {
		//setup metadataRepository with barIU
		metadataRepository = new TestMetadataRepository(getAgent(), new IInstallableUnit[] {mockIU(BAR, null)});

		ArrayList adviceCollection = fillAdvice(new ArrayList());
		expect(publisherInfo.getAdvice(null, false, "bar.feature.jar", barVersion, IPropertyAdvice.class)).andReturn(adviceCollection).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "bar", barVersion, IPropertyAdvice.class)).andReturn(adviceCollection).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "bar", barVersion, IFeatureRootAdvice.class)).andReturn(Collections.EMPTY_LIST).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "bar.feature.group", barVersion, IPropertyAdvice.class)).andReturn(adviceCollection).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "bar.feature.group", barVersion, ITouchpointAdvice.class)).andReturn(Collections.EMPTY_LIST).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "bar.feature.group", barVersion, ICapabilityAdvice.class)).andReturn(Collections.EMPTY_LIST).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "bar.feature.group", barVersion, IAdditionalInstallableUnitAdvice.class)).andReturn(Collections.EMPTY_LIST).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "foo.feature.jar", fooVersion, IPropertyAdvice.class)).andReturn(adviceCollection).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "bar.feature.group", barVersion, IUpdateDescriptorAdvice.class)).andReturn(Collections.EMPTY_LIST).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "foo", fooVersion, IPropertyAdvice.class)).andReturn(adviceCollection).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "foo", fooVersion, IFeatureRootAdvice.class)).andReturn(Collections.EMPTY_LIST).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "foo.feature.group", fooVersion, IPropertyAdvice.class)).andReturn(adviceCollection).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "foo.feature.group", fooVersion, ICapabilityAdvice.class)).andReturn(Collections.EMPTY_LIST).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "foo.feature.group", fooVersion, IAdditionalInstallableUnitAdvice.class)).andReturn(Collections.EMPTY_LIST).anyTimes();
		expect(publisherInfo.getAdvice(null, false, "foo.feature.group", fooVersion, IUpdateDescriptorAdvice.class)).andReturn(Collections.EMPTY_LIST).anyTimes();
		expect(publisherInfo.getArtifactOptions()).andReturn(IPublisherInfo.A_INDEX | IPublisherInfo.A_OVERWRITE | IPublisherInfo.A_PUBLISH).anyTimes();
		expect(publisherInfo.getArtifactRepository()).andReturn(artifactRepository).anyTimes();
		expect(publisherInfo.getMetadataRepository()).andReturn(metadataRepository).anyTimes();
		expect(publisherInfo.getContextMetadataRepository()).andReturn(null).anyTimes();

		//capture any touchpoint advice, and return the captured advice when the action asks for it
		publisherInfo.addAdvice(and(isA(ITouchpointAdvice.class), capture(tpAdvice)));
		EasyMock.expectLastCall().anyTimes();
		expect(publisherInfo.getAdvice(null, false, "foo.feature.group", fooVersion, ITouchpointAdvice.class)).andReturn(new CaptureList(tpAdvice)).anyTimes();
	}

	private ArrayList fillAdvice(ArrayList adviceCollection) {
		Map<String, String> prop = new HashMap<String, String>();
		prop.put("key1", "value1"); //$NON-NLS-1$//$NON-NLS-2$
		prop.put("key2", "value2"); //$NON-NLS-1$//$NON-NLS-2$
		IPropertyAdvice propertyAdvice = EasyMock.createMock(IPropertyAdvice.class);
		expect(propertyAdvice.getInstallableUnitProperties((InstallableUnitDescription) EasyMock.anyObject())).andReturn(prop).anyTimes();
		expect(propertyAdvice.getArtifactProperties((IInstallableUnit) EasyMock.anyObject(), (IArtifactDescriptor) EasyMock.anyObject())).andReturn(null).anyTimes();
		EasyMock.replay(propertyAdvice);
		adviceCollection.add(propertyAdvice);
		return adviceCollection;
	}
}
