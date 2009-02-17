/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
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
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.equinox.p2.tests.*;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

@SuppressWarnings( {"unchecked", "restriction"})
public class FeaturesActionTest extends ActionTest {

	public static IArtifactKey FOO_KEY = ArtifactKey.parse("org.eclipse.update.feature,foo,1.0.0"); //$NON-NLS-1$
	public static IArtifactKey BAR_KEY = ArtifactKey.parse("org.eclipse.update.feature,bar,1.1.1"); //$NON-NLS-1$

	private static File root = new File(TestActivator.getTestDataFolder().toString(), "FeaturesActionTest"); //$NON-NLS-1$
	protected TestArtifactRepository artifactRepository = new TestArtifactRepository();
	protected TestMetadataRepository metadataRepository;
	private Version fooVersion = new Version("1.0.0"); //$NON-NLS-1$
	private Version barVersion = new Version("1.1.1"); //$NON-NLS-1$
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

	public void testFeaturePatch() {
		//TODO add a test for generating a feature patch
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
		assertTrue(foo.getProperty("key1").equals("value1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(foo.getProperty("key2").equals("value2")); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue(foo.getArtifacts()[0].equals(FOO_KEY));
		assertTrue(foo.getFilter().equalsIgnoreCase("(org.eclipse.update.install.features=true)")); //$NON-NLS-1$

		//check touchpointType
		assertTrue(foo.getTouchpointType().getId().equalsIgnoreCase("org.eclipse.equinox.p2.osgi")); //$NON-NLS-1$
		assertTrue(foo.getTouchpointType().getVersion().equals(fooVersion));

		//zipped=true
		ITouchpointData[] tpData = foo.getTouchpointData();
		String fooValue = ((ITouchpointInstruction) tpData[0].getInstructions().get("zipped")).getBody(); //$NON-NLS-1$
		assertTrue(fooValue.equalsIgnoreCase("true")); //$NON-NLS-1$

		IRequiredCapability[] fooRequiredCapabilities = foo.getRequiredCapabilities();
		assertTrue(fooRequiredCapabilities.length == 0);

		IProvidedCapability[] fooProvidedCapabilities = foo.getProvidedCapabilities();
		contains(fooProvidedCapabilities, IInstallableUnit.NAMESPACE_IU_ID, "foo.feature.jar", fooVersion); //$NON-NLS-1$
		contains(fooProvidedCapabilities, PublisherHelper.NAMESPACE_ECLIPSE_TYPE, "feature", fooVersion); //$NON-NLS-1$ 
		contains(fooProvidedCapabilities, "org.eclipse.update.feature", FOO, fooVersion); //$NON-NLS-1$
		assertTrue(fooProvidedCapabilities.length == 3);

		//feature group IU for foo
		fooIUs = new ArrayList(publisherResult.getIUs("foo.feature.group", IPublisherResult.ROOT)); //$NON-NLS-1$
		assertTrue(fooIUs.size() == 1);
		IInstallableUnit fooGroup = (IInstallableUnit) fooIUs.get(0);
		tpData = fooGroup.getTouchpointData();
		assertEquals(1, tpData.length);
		ITouchpointInstruction instruction = tpData[0].getInstruction("install");
		assertNotNull(instruction);
		assertEquals("ln(targetDir:@artifact,linkTarget:foo/lib.1.so,linkName:lib.so);chmod(targetDir:@artifact,targetFile:lib/lib.so,permissions:755);", instruction.getBody());

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
		assertTrue(bar.getArtifacts()[0].equals(BAR_KEY));
		assertTrue(bar.getFilter().equalsIgnoreCase("(org.eclipse.update.install.features=true)")); //$NON-NLS-1$
		assertTrue(bar.isSingleton());

		barIUs = new ArrayList(publisherResult.getIUs("bar.feature.group", IPublisherResult.ROOT)); //$NON-NLS-1$
		assertTrue(fooIUs.size() == 1);
		IInstallableUnit barGroup = (IInstallableUnit) barIUs.get(0);
		IRequiredCapability[] barRequiredCapabilities = barGroup.getRequiredCapabilities();
		contains(barRequiredCapabilities, IInstallableUnit.NAMESPACE_IU_ID, "bar_root", new VersionRange(barVersion, true, barVersion, true), null, false /*multiple*/, false /*optional*/); //$NON-NLS-1$//$NON-NLS-2$
		contains(barRequiredCapabilities, IInstallableUnit.NAMESPACE_IU_ID, "bar.feature.jar", new VersionRange(barVersion, true, barVersion, true), "(org.eclipse.update.install.features=true)", false /*multiple*/, false /*optional*/); //$NON-NLS-1$//$NON-NLS-2$

		//check zipped=true in touchpointData
		String barValue = ((ITouchpointInstruction) bar.getTouchpointData()[0].getInstructions().get("zipped")).getBody(); //$NON-NLS-1$
		assertTrue(barValue.equalsIgnoreCase("true")); //$NON-NLS-1$

		//check touchpointType
		assertTrue(bar.getTouchpointType().getId().equalsIgnoreCase("org.eclipse.equinox.p2.osgi")); //$NON-NLS-1$
		assertTrue(bar.getTouchpointType().getVersion().equals(fooVersion));
		//String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple, boolean greedy)
		barRequiredCapabilities = bar.getRequiredCapabilities();

		assertTrue(barRequiredCapabilities.length == 0);

		IProvidedCapability[] barProvidedCapabilities = bar.getProvidedCapabilities();
		contains(barProvidedCapabilities, IInstallableUnit.NAMESPACE_IU_ID, "bar.feature.jar", barVersion); //$NON-NLS-1$ 
		contains(barProvidedCapabilities, PublisherHelper.NAMESPACE_ECLIPSE_TYPE, "feature", fooVersion); //$NON-NLS-1$ 
		contains(barProvidedCapabilities, "org.eclipse.update.feature", BAR, barVersion); //$NON-NLS-1$
		assertTrue(barProvidedCapabilities.length == 3);
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
		metadataRepository = new TestMetadataRepository(new IInstallableUnit[] {mockIU(BAR, null)});

		ArrayList adviceCollection = fillAdvice(new ArrayList());
		expect(publisherInfo.getAdvice(null, false, null, null, IFeatureAdvice.class)).andReturn(adviceCollection).anyTimes();
		expect(publisherInfo.getArtifactOptions()).andReturn(IPublisherInfo.A_INDEX | IPublisherInfo.A_OVERWRITE | IPublisherInfo.A_PUBLISH).anyTimes();
		expect(publisherInfo.getArtifactRepository()).andReturn(artifactRepository).anyTimes();
		expect(publisherInfo.getMetadataRepository()).andReturn(metadataRepository).anyTimes();

		//capture any touchpoint advice, and return the captured advice when the action asks for it
		publisherInfo.addAdvice(and(isA(ITouchpointAdvice.class), capture(tpAdvice)));
		EasyMock.expectLastCall().anyTimes();
		expect(publisherInfo.getAdvice(null, false, null, null, ITouchpointAdvice.class)).andReturn(new CaptureList(tpAdvice)).anyTimes();
	}

	private ArrayList fillAdvice(ArrayList adviceCollection) {
		Properties prop = new Properties();
		prop.setProperty("key1", "value1"); //$NON-NLS-1$//$NON-NLS-2$
		prop.setProperty("key2", "value2"); //$NON-NLS-1$//$NON-NLS-2$
		IFeatureAdvice featureAdvice = EasyMock.createMock(IFeatureAdvice.class);
		expect(featureAdvice.getIUProperties((Feature) EasyMock.anyObject())).andReturn(prop).anyTimes();
		expect(featureAdvice.getArtifactProperties((Feature) EasyMock.anyObject())).andReturn(null).anyTimes();
		EasyMock.replay(featureAdvice);
		adviceCollection.add(featureAdvice);
		return adviceCollection;
	}
}
