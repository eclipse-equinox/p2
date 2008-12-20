/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
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

import java.io.*;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.tests.*;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

@SuppressWarnings( {"restriction", "unchecked"})
public class JREActionTest extends ActionTest {

	private File J14 = new File(TestActivator.getTestDataFolder(), "JREActionTest/1.4/"); //$NON-NLS-1$
	private File J15 = new File(TestActivator.getTestDataFolder(), "JREActionTest/1.5/"); //$NON-NLS-1$
	private File J16 = new File(TestActivator.getTestDataFolder(), "JREActionTest/1.6/"); //$NON-NLS-1$

	protected TestArtifactRepository artifactRepository = new TestArtifactRepository();
	protected TestMetadataRepository metadataRepository;

	public void setUp() throws Exception {
		setupPublisherInfo();
		setupPublisherResult();
	}

	public void test14() throws Exception {
		// TODO currently failing so comment it out
		if (DISABLED)
			return;
		testAction = new JREAction(publisherInfo, J14);
		testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		verifyResults(92, new Version("1.4.0")); //$NON-NLS-1$
		verifyArtifactRepository(ArtifactKey.parse("binary,a.jre,1.4.0"), J14, "J2SE-1.4.profile"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void test15() throws Exception {
		// TODO currently failing so comment it out
		if (DISABLED)
			return;
		testAction = new JREAction(publisherInfo, J15);
		testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		verifyResults(119, new Version("1.5.0")); //$NON-NLS-1$
		verifyArtifactRepository(ArtifactKey.parse("binary,a.jre,1.5.0"), J15, "J2SE-1.5.profile"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void test16() throws Exception {
		testAction = new JREAction(publisherInfo, J16);
		testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		verifyResults(117, new Version("1.6.0")); //$NON-NLS-1$
		verifyArtifactRepository(ArtifactKey.parse("binary,a.jre,1.6.0"), J16, "JavaSE-1.6.profile"); //$NON-NLS-1$//$NON-NLS-2$
	}

	private void verifyResults(int numProvidedCapabilities, Version JREVersion) {
		ArrayList fooIUs = new ArrayList(publisherResult.getIUs("a.jre", IPublisherResult.ROOT)); //$NON-NLS-1$
		assertTrue(fooIUs.size() == 1);
		IInstallableUnit foo = (IInstallableUnit) fooIUs.get(0);

		// check version
		assertTrue(foo.getVersion().equals(JREVersion));

		// check touchpointType
		assertTrue(foo.getTouchpointType().getId().equalsIgnoreCase("org.eclipse.equinox.p2.native")); //$NON-NLS-1$
		assertTrue(foo.getTouchpointType().getVersion().equals(new Version("1.0.0"))); //$NON-NLS-1$

		// check provided capabilities
		ProvidedCapability[] fooProvidedCapabilities = foo.getProvidedCapabilities();
		assertTrue(fooProvidedCapabilities.length == numProvidedCapabilities);

		ArrayList barIUs = new ArrayList(publisherResult.getIUs("config.a.jre", IPublisherResult.ROOT)); //$NON-NLS-1$
		assertTrue(barIUs.size() == 1);
		IInstallableUnit bar = (IInstallableUnit) barIUs.get(0);

		Map instructions = bar.getTouchpointData()[0].getInstructions();
		assertTrue(((TouchpointInstruction) instructions.get("install")).getBody().equals("unzip(source:@artifact, target:${installFolder});")); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue(((TouchpointInstruction) instructions.get("uninstall")).getBody().equals("cleanupzip(source:@artifact, target:${installFolder});")); //$NON-NLS-1$ //$NON-NLS-2$

		RequiredCapability[] requiredCapability = bar.getRequiredCapabilities();
		verifyRequiredCapability(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, "a.jre", new VersionRange(JREVersion, true, new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), true)); //$NON-NLS-1$ 
		assertTrue(requiredCapability.length == 1);

		ProvidedCapability[] providedCapability = bar.getProvidedCapabilities();
		verifyProvidedCapability(providedCapability, IInstallableUnit.NAMESPACE_IU_ID, "config.a.jre", JREVersion); //$NON-NLS-1$ 
		assertTrue(providedCapability.length == 1);

		assertTrue(bar.getProperty("org.eclipse.equinox.p2.type.fragment").equals("true")); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue(bar.getVersion().equals(JREVersion));
	}

	private void verifyArtifactRepository(IArtifactKey key, File JRELocation, final String fileName) throws IOException {
		assertTrue(artifactRepository.contains(key));
		ByteArrayOutputStream content = new ByteArrayOutputStream();
		FileFilter fileFilter = new FileFilter() {
			public boolean accept(File file) {
				return file.getName().endsWith(fileName);
			}
		};
		File[] contentBytes = JRELocation.listFiles(fileFilter);
		FileUtils.copyStream(new FileInputStream(contentBytes[0]), false, content, true);
		ZipInputStream zipInputStream = artifactRepository.getZipInputStream(key);

		Map fileMap = new HashMap();
		fileMap.put(fileName, new Object[] {contentBytes[0], content.toByteArray()});
		TestData.assertContains(fileMap, zipInputStream, true);
	}

	protected void insertPublisherInfoBehavior() {
		expect(publisherInfo.getArtifactRepository()).andReturn(artifactRepository).anyTimes();
		expect(publisherInfo.getArtifactOptions()).andReturn(IPublisherInfo.A_PUBLISH).anyTimes();
	}

}
