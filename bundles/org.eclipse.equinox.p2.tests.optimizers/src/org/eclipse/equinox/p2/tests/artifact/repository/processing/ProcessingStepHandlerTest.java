/*******************************************************************************
* Copyright (c) 2007 compeople AG and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	compeople AG (Stefan Liebig) - initial API and implementation
*  IBM - continuing development
*******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository.processing;

import java.io.*;
import java.util.Arrays;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.processors.pack200.Pack200ProcessorStep;
import org.eclipse.equinox.internal.p2.artifact.processors.verifier.MD5Verifier;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.*;
import org.eclipse.equinox.p2.tests.optimizers.TestActivator;

public class ProcessingStepHandlerTest extends TestCase {

	//	private static final int BUFFER_SIZE = 8 * 1024;

	ProcessingStepHandler handler = new ProcessingStepHandler();
	IProgressMonitor monitor = new NullProgressMonitor();

	public void testExecuteNoPSs() throws IOException {
		ProcessingStepDescriptor[] descriptors = new ProcessingStepDescriptor[0];
		OutputStream result = new ByteArrayOutputStream(10);
		OutputStream testStream = handler.createAndLink(descriptors, null, result, monitor);
		testStream.write("Test".getBytes());
		testStream.close();
		assertEquals("Test", result.toString());
	}

	public void testExecuteOneByteShifterPS() throws IOException {
		ProcessingStep[] steps = new ProcessingStep[] {new ByteShifter(1)};
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		OutputStream testStream = handler.link(steps, result, monitor);
		testStream.write(new byte[] {1});
		testStream.close();
		assertTrue(Arrays.equals(new byte[] {2}, result.toByteArray()));
	}

	public void testExecuteTwoByteShifterPSs() throws IOException {
		ProcessingStep[] steps = new ProcessingStep[] {new ByteShifter(1), new ByteShifter(2)};
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		OutputStream testStream = handler.link(steps, result, monitor);
		testStream.write(new byte[] {1});
		testStream.close();
		assertTrue(Arrays.equals(new byte[] {8}, result.toByteArray()));
	}

	public void testExecuteOneMD5VerifierPSOk() throws IOException {
		ProcessingStep[] steps = new ProcessingStep[] {new MD5Verifier("0cbc6611f5540bd0809a388dc95a615b")};
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		OutputStream testStream = handler.link(steps, result, monitor);
		testStream.write("Test".getBytes());
		testStream.close();
		assertEquals("Test", result.toString());
	}

	public void testExecuteOneMD5VerifierPSFails() throws IOException {
		ProcessingStep[] steps = new ProcessingStep[] {new MD5Verifier("9cbc6611f5540bd0809a388dc95a615b")};
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		OutputStream testStream = handler.link(steps, result, monitor);
		testStream.write("Test".getBytes());
		try {
			testStream.close();
			assertEquals("Test", result.toString());
			assertTrue((ProcessingStepHandler.checkStatus(testStream).getSeverity() == IStatus.ERROR));
		} catch (IOException e) {
			assertTrue(true);
		}
	}

	public void testExecuteOneByteShifterAndOneMD5VerifierPSOk() throws IOException {
		// Order of PSs is important!!
		ProcessingStep[] steps = new ProcessingStep[] {new ByteShifter(1), new MD5Verifier("ceeee507e8db83294600218b4e198897")};
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		OutputStream testStream = handler.link(steps, result, monitor);
		testStream.write(new byte[] {1, 2, 3, 4, 5});
		testStream.close();
		assertTrue(Arrays.equals(new byte[] {2, 4, 6, 8, 10}, result.toByteArray()));
	}

	public void testExecuteOneByteShifterAndOneMD5VerifierPSFailWrongOrder() throws IOException {
		// Order of PSs is important - here it wrong!!
		ProcessingStep[] steps = new ProcessingStep[] {new MD5Verifier("af476bbaf152a4c39ca4e5c498a88aa0"), new ByteShifter(1)};
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		OutputStream testStream = handler.link(steps, result, monitor);
		testStream.write(new byte[] {1, 2, 3, 4, 5});
		try {
			testStream.close();
			assertTrue(Arrays.equals(new byte[] {2, 4, 6, 8, 10}, result.toByteArray()));
			assertTrue((ProcessingStepHandler.checkStatus(testStream).getSeverity() == IStatus.ERROR));
		} catch (IOException e) {
			assertTrue(true);
		}
	}

	public void testAssureOrderingOfPSs1() throws IOException {
		ProcessingStep[] steps = new ProcessingStep[] {new Adder(1), new Multiplier(2)};
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		OutputStream testStream = handler.link(steps, result, monitor);
		testStream.write(new byte[] {1, 2, 3, 4, 5});
		testStream.close();
		assertTrue(Arrays.equals(new byte[] {4, 6, 8, 10, 12}, result.toByteArray()));
	}

	public void testAssureOrderingOfPSs2() throws IOException {
		ProcessingStep[] steps = new ProcessingStep[] {new Multiplier(2), new Adder(1)};
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		OutputStream testStream = handler.link(steps, result, monitor);
		testStream.write(new byte[] {1, 2, 3, 4, 5});
		testStream.close();
		assertTrue(Arrays.equals(new byte[] {3, 5, 7, 9, 11}, result.toByteArray()));
	}

	public void testExecuteOnePack200UnpackerPS() throws IOException {
		ProcessingStepDescriptor[] descriptors = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)};
		ProcessingStep[] steps = handler.create(descriptors, null);
		ByteArrayOutputStream result = new ByteArrayOutputStream(100000);
		OutputStream testStream = handler.link(steps, result, monitor);
		IStatus status = ProcessingStepHandler.checkStatus(testStream);
		assertTrue("Step is not ready.", status.isOK());
		InputStream inputStream = TestActivator.getContext().getBundle().getEntry("testData/jarprocessor.jar.pack.gz").openStream();
		FileUtils.copyStream(inputStream, true, testStream, true);
		assertEquals(35062, result.size());
	}

	public void testCreateByteShifterPS() {
		ProcessingStepDescriptor[] descriptors = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.ByteShifter", "1", true)};
		ProcessingStep[] steps = handler.create(descriptors, null);
		assertNotNull(steps);
		assertEquals(1, steps.length);
		assertEquals(ByteShifter.class, steps[0].getClass());
	}

	public void testCreateMD5VerifierPS() {
		ProcessingStepDescriptor[] descriptors = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.MD5Verifier", "1", true)};
		ProcessingStep[] steps = handler.create(descriptors, null);
		assertNotNull(steps);
		assertEquals(1, steps.length);
		assertEquals(MD5Verifier.class, steps[0].getClass());
	}

	public void testCreateAdderPS() {
		ProcessingStepDescriptor[] descriptors = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Adder", "1", true)};
		ProcessingStep[] steps = handler.create(descriptors, null);
		assertNotNull(steps);
		assertEquals(1, steps.length);
		assertEquals(Adder.class, steps[0].getClass());
	}

	public void testCreateMultiplierPS() {
		ProcessingStepDescriptor[] descriptors = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Multiplier", "2", true)};
		ProcessingStep[] steps = handler.create(descriptors, null);
		assertNotNull(steps);
		assertEquals(1, steps.length);
		assertEquals(Multiplier.class, steps[0].getClass());
	}

	public void testCreatePack200UnpackerPS() {
		ProcessingStepDescriptor[] descriptors = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)};
		ProcessingStep[] steps = handler.create(descriptors, null);
		assertNotNull(steps);
		assertEquals(1, steps.length);
		assertEquals(Pack200ProcessorStep.class, steps[0].getClass());
	}

	public void testCreatePSsAndAssureOrderingOfPSs1() throws IOException {
		ProcessingStepDescriptor adder = new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Adder", "1", true);
		ProcessingStepDescriptor multiplier = new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Multiplier", "2", true);
		ProcessingStepDescriptor[] descriptors = new ProcessingStepDescriptor[] {adder, multiplier};
		ProcessingStep[] steps = handler.create(descriptors, null);
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		OutputStream testStream = handler.link(steps, result, monitor);
		testStream.write(new byte[] {1, 2, 3, 4, 5});
		testStream.close();
		assertTrue(Arrays.equals(new byte[] {4, 6, 8, 10, 12}, result.toByteArray()));
	}

	public void testCreatePSsAndAssureOrderingOfPSs2() throws IOException {
		ProcessingStepDescriptor adder = new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Adder", "1", true);
		ProcessingStepDescriptor multiplier = new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Multiplier", "2", true);
		ProcessingStepDescriptor[] descriptors = new ProcessingStepDescriptor[] {multiplier, adder};
		ProcessingStep[] steps = handler.create(descriptors, null);
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		OutputStream testStream = handler.link(steps, result, monitor);
		testStream.write(new byte[] {1, 2, 3, 4, 5});
		testStream.close();
		assertTrue(Arrays.equals(new byte[] {3, 5, 7, 9, 11}, result.toByteArray()));
	}

	public void testLinkPSs() throws IOException {
		ProcessingStepDescriptor adder = new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Adder", "1", true);
		ProcessingStepDescriptor multiplier = new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Multiplier", "2", true);
		ProcessingStepDescriptor[] descriptors = new ProcessingStepDescriptor[] {adder, multiplier};
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		OutputStream testStream = handler.createAndLink(descriptors, null, result, monitor);
		testStream.write(new byte[] {1, 2, 3, 4, 5});
		testStream.close();
		assertTrue(Arrays.equals(new byte[] {4, 6, 8, 10, 12}, result.toByteArray()));
	}

}
