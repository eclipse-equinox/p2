/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.optimizers.jardelta.JarDeltaOptimizerStep;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.artifact.processors.ArtifactRepositoryMock;
import org.osgi.framework.Version;

/**
 * Test the <code>JarDelta</code> processing step.
 */
public class JarDeltaOptimizerTest extends TestCase {

	private static Map getEntries(ZipInputStream input) throws IOException {
		Map result = new HashMap();
		while (true) {
			ZipEntry entry = input.getNextEntry();
			if (entry == null)
				return result;

			ByteArrayOutputStream content = new ByteArrayOutputStream();
			FileUtils.copyStream(input, false, content, true);
			input.closeEntry();
			result.put(entry.getName(), new Object[] {entry, content.toByteArray()});
		}
	}

	public static void compare(ZipInputStream input1, ZipInputStream input2) throws IOException {
		Map jar1 = getEntries(input1);
		Map jar2 = getEntries(input2);
		for (Iterator i = jar1.keySet().iterator(); i.hasNext();) {
			String name = (String) i.next();
			Object[] file1 = (Object[]) jar1.get(name);
			Object[] file2 = (Object[]) jar2.remove(name);
			Assert.assertNotNull(file2);

			ZipEntry entry1 = (ZipEntry) file1[0];
			ZipEntry entry2 = (ZipEntry) file2[0];
			// compare the entries
			Assert.assertTrue(entry1.getName().equals(entry2.getName()));
			Assert.assertTrue(entry1.getSize() == entry2.getSize());
			//	TODO for now skip over the timestamp as they seem to be different
			// assertTrue(entry1.getTime() == entry2.getTime());
			Assert.assertTrue(entry1.isDirectory() == entry2.isDirectory());
			Assert.assertTrue(entry1.getCrc() == entry2.getCrc());
			Assert.assertTrue(entry1.getMethod() == entry2.getMethod());

			// check the content of the entries
			Assert.assertTrue(Arrays.equals((byte[]) file1[1], (byte[]) file2[1]));
		}

		// ensure that we have consumed all of the entries in the second JAR
		Assert.assertTrue(jar2.size() == 0);
	}

	//	public void testPrepare() throws IOException {
	//		IArtifactRepository repoMock = ArtifactRepositoryMock.getMock("testData/optimizers/testdata_1.0.0.1.jar");
	//		ProcessingStep step = new MockableJarDeltaOptimizerStep(repoMock);
	//		ProcessingStepDescriptor stepDescriptor = new ProcessingStepDescriptor("id", "ns,cl,id1,1.0.0.1", true);
	//		IArtifactKey key = new ArtifactKey("ns", "cl", "id1", new Version("1.0.0.2"));
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
		ProcessingStepDescriptor stepDescriptor = new ProcessingStepDescriptor("id", "ns,cl,id1,1.0.0.1", true);
		IArtifactKey key = new ArtifactKey("ns", "cl", "id1", new Version("1.0.0.2"));
		ArtifactDescriptor descriptor = new ArtifactDescriptor(key);
		step.initialize(stepDescriptor, descriptor);
		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		step.link(destination, new NullProgressMonitor());

		InputStream inputStream = TestActivator.getContext().getBundle().getEntry("testData/optimizers/testdata_1.0.0.2.jar").openStream();
		FileUtils.copyStream(inputStream, true, step, true);
		destination.close();

		inputStream = TestActivator.getContext().getBundle().getEntry("testData/optimizers/testdata_1.0.0.1-2.jar").openStream();
		ByteArrayOutputStream expected = new ByteArrayOutputStream();
		FileUtils.copyStream(inputStream, true, expected, true);

		ZipInputStream expectedJar = new ZipInputStream(new ByteArrayInputStream(expected.toByteArray()));
		ZipInputStream testJar = new ZipInputStream(new ByteArrayInputStream(destination.toByteArray()));
		compare(expectedJar, testJar);
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
