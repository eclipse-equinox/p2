/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.processors;

import java.io.*;
import java.util.Arrays;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.processors.jbdiff.JBPatchZipStep;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Version;

/**
 * Test the <code>JBPatchZipStep</code> processing step.
 */
public class JBPatchZipStepTest extends AbstractProvisioningTest {

	//	/**
	//	 * This is a disabled "unit test" that was used to generate the data needed for real test.
	//	 * @throws IOException
	//	 */
	//	public void testPrepare() throws IOException {
	//		String base = "C:/projekte/rcp/org.eclipse.equinox.p2.tests";
	//		File tonormalize32 = new File(base, TestActivator.getContext().getBundle().getEntry("testData/optimizers/org.eclipse.jdt_3.2.0.v20060605-1400.jar").getFile());
	//		File normalized32 = File.createTempFile("3.2", ".njar");
	//		SarUtil.normalize(tonormalize32, normalized32);
	//		File sar32 = File.createTempFile("3.2", ".sar");
	//		SarUtil.zipToSar(normalized32, sar32);
	//
	//		File tonormalize33 = new File(base, TestActivator.getContext().getBundle().getEntry("testData/optimizers/org.eclipse.jdt_3.3.0.v20070607-1300.jar").getFile());
	//		File normalized33 = File.createTempFile("3.3", ".njar");
	//		SarUtil.normalize(tonormalize33, normalized33);
	//		File sar33 = File.createTempFile("3.3", ".sar");
	//		SarUtil.zipToSar(normalized33, sar33);
	//
	//		File diff = File.createTempFile("diff32-33", ".jbdiff");
	//		JBDiff.bsdiff(sar32, sar33, diff);
	//	}

	/**
	 * Test patching the <b>normalized</b> jars. This is indicated by the extension ".njar".
	 * 
	 * @throws IOException
	 */
	public void testPatchOrgEclipseJdt32to33() throws IOException {

		IArtifactRepository repoMock = ArtifactRepositoryMock.getMock("testData/optimizers/org.eclipse.jdt_3.2.0.v20060605-1400.njar");
		ProcessingStep patcher = new MockableJBPatchZipStep(repoMock);
		ProcessingStepDescriptor descriptor = new ProcessingStepDescriptor("id", "ns,cl,id1,1.0", true);
		IArtifactKey key = new ArtifactKey("ns", "cl", "id1", new Version("1.1"));
		ArtifactDescriptor context = new ArtifactDescriptor(key);
		patcher.initialize(descriptor, context);

		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		patcher.link(destination, new NullProgressMonitor());

		InputStream inputStream = TestActivator.getContext().getBundle().getEntry("testData/optimizers/org.eclipse.jdt_3.2.0-3.3.0.jbdiff").openStream();
		FileUtils.copyStream(inputStream, true, patcher, true);

		inputStream = TestActivator.getContext().getBundle().getEntry("testData/optimizers/org.eclipse.jdt_3.3.0.v20070607-1300.njar").openStream();
		ByteArrayOutputStream expected = new ByteArrayOutputStream();
		FileUtils.copyStream(inputStream, true, expected, true);

		assertTrue("Different resulting njar.", Arrays.equals(expected.toByteArray(), destination.toByteArray()));
	}

	/**
	 * Need to inject a repository!
	 */
	private static class MockableJBPatchZipStep extends JBPatchZipStep {
		public MockableJBPatchZipStep(IArtifactRepository repository) {
			super.repository = repository;
		}
	}

}
