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
import org.eclipse.equinox.internal.p2.artifact.processors.jbdiff.JBPatchStep;
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
 * Test the <code>JBPatchStep</code>
 *
 */
public class JBPatchStepTest extends OptimizerTest {

	public void testPatchEclipseExe32to33() throws IOException {
		IArtifactRepository repoMock = ArtifactRepositoryMock.getMock("testData/optimizers/eclipse-3.2.exe");
		ProcessingStep patcher = new MockableJBPatchStep(repoMock);
		IProcessingStepDescriptor descriptor = new ProcessingStepDescriptor("id", "ns,cl,id1,1.0", true);
		IArtifactKey key = new ArtifactKey("cl", "id1", Version.create("1.1"));
		ArtifactDescriptor context = new ArtifactDescriptor(key);
		patcher.initialize(getAgent(), descriptor, context);

		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		patcher.link(destination, new NullProgressMonitor());

		InputStream inputStream = TestData.get("optimizers", "eclipse-3.2-3.3.jbdiff");
		FileUtils.copyStream(inputStream, true, patcher, true);

		inputStream = TestData.get("optimizers", "eclipse-3.3.exe");
		ByteArrayOutputStream expected = new ByteArrayOutputStream();
		FileUtils.copyStream(inputStream, true, expected, true);
		assertTrue(Arrays.equals(expected.toByteArray(), destination.toByteArray()));
	}

	/**
	 * Need to inject a repository!
	 */
	private static class MockableJBPatchStep extends JBPatchStep {
		public MockableJBPatchStep(IArtifactRepository repository) {
			super.repository = repository;
		}
	}

}
