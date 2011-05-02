/*******************************************************************************
 * Copyright (c) 2007, 2010 compeople AG and others.
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
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.equinox.p2.tests.artifact.optimizers.OptimizerTest;
import org.eclipse.equinox.p2.tests.optimizers.TestData;

/**
 * Test the <code>JBPatchZipStep</code> processing step.
 */
public class JBPatchZipStepTest extends OptimizerTest {

	//	/**
	//	 * This is a disabled "unit test" that was used to generate the data needed for real test.
	//	 * @throws IOException
	//	 */
	//	public void testGenerateTestData() throws IOException {
	//
	//		File sar32 = TestData.getTempFile("sar", "org.eclipse.jdt_3.2.0.v20060605-1400.sar");
	//		File sar33 = TestData.getTempFile("sar", "org.eclipse.jdt_3.3.0.v20070607-1300.sar");
	//
	//		File diff = File.createTempFile("org.eclipse.jdt_3.2.0-3.3.0", ".jbdiff");
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
		IProcessingStepDescriptor descriptor = new ProcessingStepDescriptor("id", "ns,cl,id1,1.0", true);
		IArtifactKey key = new ArtifactKey("cl", "id1", Version.create("1.1"));
		ArtifactDescriptor context = new ArtifactDescriptor(key);
		patcher.initialize(getAgent(), descriptor, context);

		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		patcher.link(destination, new NullProgressMonitor());

		InputStream inputStream = TestData.get("optimizers", "org.eclipse.jdt_3.2.0-3.3.0.jbdiff");
		FileUtils.copyStream(inputStream, true, patcher, true);

		inputStream = TestData.get("optimizers", "org.eclipse.jdt_3.3.0.v20070607-1300.njar");
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
