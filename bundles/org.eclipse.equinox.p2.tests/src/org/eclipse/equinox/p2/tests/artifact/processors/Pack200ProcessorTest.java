/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.processors;

import java.io.*;
import java.util.Arrays;
import junit.framework.TestCase;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.processors.pack200.Pack200ProcessorStep;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.jarprocessor.PackStep;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Bundle;

public class Pack200ProcessorTest extends TestCase {

	public Pack200ProcessorTest(String name) {
		super(name);
	}

	public Pack200ProcessorTest() {
		super("");
	}

	public void testUnpack() throws IOException {
		//this test is only applicable if pack200 is available
		if (!PackStep.canPack())
			return;
		// Setup the processor
		ProcessingStep step = new Pack200ProcessorStep();
		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		step.link(destination, new NullProgressMonitor());

		// drive the source data through the step
		Bundle bundle = TestActivator.getContext().getBundle();
		InputStream inputStream = bundle.getEntry("testData/optimizers/org.eclipse.equinox.app_1.0.100.v20071015.jar.pack.gz").openStream();
		FileUtils.copyStream(inputStream, true, step, true);

		// Get the test data
		inputStream = bundle.getEntry("testData/optimizers/org.eclipse.equinox.app_1.0.100.v20071015.jar").openStream();
		ByteArrayOutputStream expected = new ByteArrayOutputStream();
		FileUtils.copyStream(inputStream, true, expected, true);

		// Compare
		assertTrue(Arrays.equals(expected.toByteArray(), destination.toByteArray()));
	}

	public void testUnpackFailsBecauseOfZeroLengthPackedFile() throws IOException {
		// Setup the processor
		ProcessingStep step = new Pack200ProcessorStep();
		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		step.link(destination, new NullProgressMonitor());

		// drive the source data (zero length) through the step
		InputStream inputStream = new ByteArrayInputStream(new byte[0]);
		FileUtils.copyStream(inputStream, true, step, true);

		// This must fail, i.e. the status is not ok!
		assertFalse(step.getStatus().isOK());
	}

}
