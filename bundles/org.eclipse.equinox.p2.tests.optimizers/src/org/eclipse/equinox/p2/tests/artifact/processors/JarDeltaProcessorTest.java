/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *		compeople AG (Stefan Liebig)
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.processors;

import java.io.*;
import java.util.zip.ZipInputStream;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.processors.jardelta.JarDeltaProcessorStep;
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

public class JarDeltaProcessorTest extends OptimizerTest {

	public JarDeltaProcessorTest(String name) {
		super(name);
	}

	public JarDeltaProcessorTest() {
		super("");
	}

	public void testProcessing() throws IOException {
		IArtifactRepository repoMock = ArtifactRepositoryMock.getMock("testData/optimizers/testdata_1.0.0.1.jar");
		ProcessingStep step = new MockableJarDeltaProcessorStep(repoMock);
		IProcessingStepDescriptor stepDescriptor = new ProcessingStepDescriptor("id", "ns,cl,id1,1.0.0.1", true);
		IArtifactKey key = new ArtifactKey("cl", "id1", Version.create("1.0.0.2"));
		ArtifactDescriptor descriptor = new ArtifactDescriptor(key);
		step.initialize(getAgent(), stepDescriptor, descriptor);
		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		step.link(destination, new NullProgressMonitor());

		InputStream inputStream = TestData.get("optimizers", "testdata_1.0.0.1-2.jar");
		FileUtils.copyStream(inputStream, true, step, true);
		destination.close();

		inputStream = TestData.get("optimizers", "testdata_1.0.0.2.jar");
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
	private static class MockableJarDeltaProcessorStep extends JarDeltaProcessorStep {
		public MockableJarDeltaProcessorStep(IArtifactRepository repository) {
			super();
			this.repository = repository;
		}
	}
}
