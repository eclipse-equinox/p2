/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.optimizers;

import java.io.*;
import java.util.Arrays;
import junit.framework.TestCase;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.optimizers.pack200.Pack200OptimizerStep;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Bundle;

public class Pack200OptimizerTest extends TestCase {

	public Pack200OptimizerTest(String name) {
		super(name);
	}

	public Pack200OptimizerTest() {
		super("");
	}

	public void testPrepare() throws IOException {
		// Setup the step
		ProcessingStep step = new Pack200OptimizerStep();
		FileOutputStream destination = new FileOutputStream("d:/packed.pack.gz");
		step.link(destination, new NullProgressMonitor());

		// drive the source data through the step
		Bundle bundle = TestActivator.getContext().getBundle();
		InputStream inputStream = bundle.getEntry("testData/optimizers/org.eclipse.equinox.app_1.0.100.v20071015.jar").openStream();
		FileUtils.copyStream(inputStream, true, step, true);

		destination.close();
	}

	public void testPack() throws IOException {
		// Setup the step
		ProcessingStep step = new Pack200OptimizerStep();
		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		step.link(destination, new NullProgressMonitor());

		// drive the source data through the step
		Bundle bundle = TestActivator.getContext().getBundle();
		InputStream inputStream = bundle.getEntry("testData/optimizers/org.eclipse.equinox.app_1.0.100.v20071015.jar").openStream();
		FileUtils.copyStream(inputStream, true, step, true);

		// Get the test data
		inputStream = bundle.getEntry("testData/optimizers/org.eclipse.equinox.app_1.0.100.v20071015.jar.pack.gz").openStream();
		ByteArrayOutputStream expected = new ByteArrayOutputStream();
		FileUtils.copyStream(inputStream, true, expected, true);

		// Compare
		assertTrue(Arrays.equals(expected.toByteArray(), destination.toByteArray()));
	}
}
