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
package org.eclipse.equinox.p2.tests.artifact.optimizers;

import java.io.*;
import java.util.Arrays;
import junit.framework.TestCase;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.optimizers.jbdiff.JBDiffStep;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.artifact.processors.ArtifactRepositoryMock;
import org.osgi.framework.Version;

/**
 * Test the <code>JBDiffStepTest</code> processing step.
 */
public class JBDiffStepTest extends TestCase {

	/**
	 * Test diffing the launcher.
	 * 
	 * @throws IOException
	 */
	public void testDiffEclipseExe32to33() throws IOException {

		IArtifactRepository repoMock = ArtifactRepositoryMock.getMock("testData/optimizers/eclipse-3.2.exe");
		MockableJBDiffStep differ = new MockableJBDiffStep(repoMock);
		ProcessingStepDescriptor stepDescriptor = new ProcessingStepDescriptor("id", "ns,cl,id1,1.0", true);
		IArtifactKey key = new ArtifactKey("ns", "cl", "id1", new Version("1.1"));
		ArtifactDescriptor descriptor = new ArtifactDescriptor(key);
		differ.initialize(stepDescriptor, descriptor);

		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		differ.link(destination, new NullProgressMonitor());

		InputStream inputStream = TestActivator.getContext().getBundle().getEntry("testData/optimizers/eclipse-3.3.exe").openStream();
		FileUtils.copyStream(inputStream, true, differ, true);

		inputStream = TestActivator.getContext().getBundle().getEntry("testData/optimizers/eclipse-3.2-3.3.jbdiff").openStream();
		ByteArrayOutputStream expected = new ByteArrayOutputStream();
		FileUtils.copyStream(inputStream, true, expected, true);
		assertTrue(Arrays.equals(expected.toByteArray(), destination.toByteArray()));
	}

	/**
	 * Need to inject a repository!
	 */
	private static class MockableJBDiffStep extends JBDiffStep {
		public MockableJBDiffStep(IArtifactRepository repository) {
			super(repository);
		}
	}

}
