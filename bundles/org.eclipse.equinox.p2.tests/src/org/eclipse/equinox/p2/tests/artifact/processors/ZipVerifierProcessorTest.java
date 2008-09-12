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
import junit.framework.TestCase;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ZipVerifierStep;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Bundle;

public class ZipVerifierProcessorTest extends TestCase {

	public ZipVerifierProcessorTest(String name) {
		super(name);
	}

	public ZipVerifierProcessorTest() {
		super("");
	}

	public void testGoodZip() throws IOException {
		// Setup the processor
		ProcessingStep step = new ZipVerifierStep();
		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		step.link(destination, new NullProgressMonitor());

		// drive the source data through the step
		Bundle bundle = TestActivator.getContext().getBundle();
		InputStream inputStream = bundle.getEntry("testData/zipValidation/a.zip").openStream();
		FileUtils.copyStream(inputStream, true, step, true);

		assertEquals(step.getStatus().getSeverity(), IStatus.OK);
	}

	public void testBogusFile() throws IOException {
		// Setup the processor
		ProcessingStep step = new ZipVerifierStep();
		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		step.link(destination, new NullProgressMonitor());

		// drive the source data through the step
		Bundle bundle = TestActivator.getContext().getBundle();
		InputStream inputStream = bundle.getEntry("testData/zipValidation/a.txt").openStream();
		FileUtils.copyStream(inputStream, true, step, true);

		assertEquals(step.getStatus().getSeverity(), IStatus.ERROR);
	}

	public void testBogusFile2() throws IOException {
		// Setup the processor
		ProcessingStep step = new ZipVerifierStep();
		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		step.link(destination, new NullProgressMonitor());

		// drive the source data through the step
		Bundle bundle = TestActivator.getContext().getBundle();
		InputStream inputStream = bundle.getEntry("testData/zipValidation/org.eclipse.mylyn.bugzilla.core_2.3.2.v20080402-2100.jar").openStream();
		FileUtils.copyStream(inputStream, true, step, true);

		assertEquals(step.getStatus().getSeverity(), IStatus.ERROR);
	}

	public void testBogusFile3() throws IOException {
		// Setup the processor
		ProcessingStep step = new ZipVerifierStep();
		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		step.link(destination, new NullProgressMonitor());

		// drive the source data through the step
		Bundle bundle = TestActivator.getContext().getBundle();
		InputStream inputStream = bundle.getEntry("testData/zipValidation/bogusa.zip").openStream();
		FileUtils.copyStream(inputStream, true, step, true);

		assertEquals(step.getStatus().getSeverity(), IStatus.ERROR);
	}

	public void testPackGZFile() throws IOException {

		// Setup the processor
		ProcessingStep step = new ZipVerifierStep();
		ByteArrayOutputStream destination = new ByteArrayOutputStream();
		step.link(destination, new NullProgressMonitor());

		// drive the source data through the step
		Bundle bundle = TestActivator.getContext().getBundle();
		InputStream inputStream = bundle.getEntry("testData/zipValidation/org.eclipse.equinox.p2.updatechecker.source_1.0.0.v20080427-2136.jar.pack.gz").openStream();
		FileUtils.copyStream(inputStream, true, step, true);

		assertEquals(step.getStatus().getSeverity(), IStatus.ERROR);

	}
}
