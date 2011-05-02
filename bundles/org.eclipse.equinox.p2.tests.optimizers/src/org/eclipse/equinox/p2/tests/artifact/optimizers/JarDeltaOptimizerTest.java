/*******************************************************************************
 * Copyright (c) 2007, 2010 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 * 	IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.optimizers;

import java.io.*;
import java.util.zip.ZipInputStream;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.optimizers.jardelta.JarDeltaOptimizerStep;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.equinox.p2.tests.artifact.processors.ArtifactRepositoryMock;
import org.eclipse.equinox.p2.tests.optimizers.TestData;

/**
 * Test the <code>JarDelta</code> processing step.
 */
public class JarDeltaOptimizerTest extends OptimizerTest {

	//	public void testPrepare() throws IOException {
	//		IArtifactRepository repoMock = ArtifactRepositoryMock.getMock("testData/optimizers/testdata_1.0.0.1.jar");
	//		ProcessingStep step = new MockableJarDeltaOptimizerStep(repoMock);
	//		ProcessingStepDescriptor stepDescriptor = new ProcessingStepDescriptor("id", "ns,cl,id1,1.0.0.1", true);
	//		IArtifactKey key = new ArtifactKey("ns", "cl", "id1", Version.create("1.0.0.2"));
	//		ArtifactDescriptor descriptor = new ArtifactDescriptor(key);
	//		step.initialize(stepDescriptor, descriptor);
	//		ByteArrayOutputStream destination = new ByteArrayOutputStream();
	//		step.link(destination, new NullProgressMonitor());
	//
	//		InputStream inputStream = TestActivator.getContext().getBundle().getEntry("testData/optimizers/testdata_1.0.0.2.jar").openStream();
	//		FileUtils.copyStream(inputStream, true, step, true);
	//		destination.close();
	//
	//		inputStream = new ByteArrayInputStream(destination.toByteArray());
	//		FileOutputStream file = new FileOutputStream("d:/jardelta.jar");
	//		FileUtils.copyStream(inputStream, true, file, true);
	//	}

	public void testOptimization() throws IOException {
		IArtifactRepository repoMock = ArtifactRepositoryMock.getMock("testData/optimizers/testdata_1.0.0.1.jar");
		ProcessingStep step = new MockableJarDeltaOptimizerStep(repoMock);
		IProcessingStepDescriptor stepDescriptor = new ProcessingStepDescriptor("id", "ns,cl,id1,1.0.0.1", true);
		IArtifactKey key = new ArtifactKey("cl", "id1", Version.create("1.0.0.2"));
		ArtifactDescriptor descriptor = new ArtifactDescriptor(key);
		step.initialize(getAgent(), stepDescriptor, descriptor);
		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		step.link(destination, new NullProgressMonitor());

		InputStream inputStream = TestData.get("optimizers", "testdata_1.0.0.2.jar");
		FileUtils.copyStream(inputStream, true, step, true);
		destination.close();

		inputStream = TestData.get("optimizers", "testdata_1.0.0.1-2.jar");
		ByteArrayOutputStream expected = new ByteArrayOutputStream();
		FileUtils.copyStream(inputStream, true, expected, true);

		ZipInputStream expectedJar = new ZipInputStream(new ByteArrayInputStream(expected.toByteArray()));
		ZipInputStream testJar = new ZipInputStream(new ByteArrayInputStream(destination.toByteArray()));
		TestData.assertEquals(expectedJar, testJar);
		expectedJar.close();
		testJar.close();
	}

	/**
	 * Need to inject a repository!
	 */
	private static class MockableJarDeltaOptimizerStep extends JarDeltaOptimizerStep {
		public MockableJarDeltaOptimizerStep(IArtifactRepository repository) {
			super(repository);
		}
	}
}
