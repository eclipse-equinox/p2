/*******************************************************************************
* Copyright (c) 2007, 2018 compeople AG and others.
*
* This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
* 	compeople AG (Stefan Liebig) - initial API and implementation
*  IBM - continuing development
*  Mykola Nikishov - continuing development
*******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository.processing;

import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.processors.md5.MD5Verifier;
import org.eclipse.equinox.internal.p2.artifact.processors.pack200.Pack200ProcessorStep;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.jarprocessor.PackStep;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

public class ProcessingStepHandlerTest extends AbstractProvisioningTest {

	//	private static final int BUFFER_SIZE = 8 * 1024;

	ProcessingStepHandler handler = new ProcessingStepHandler();
	IProgressMonitor monitor = new NullProgressMonitor();

	public void testCanProcess_Ok() {
		ArtifactDescriptor descriptor = new ArtifactDescriptor(ArtifactKey.parse("classifier,id,1.0.0"));
		descriptor.setProcessingSteps(new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)});

		assertTrue(ProcessingStepHandler.canProcess(descriptor));
	}

	public void testCanProcess_FailNoSuchStep() {
		String noSuchStepId = "org.eclipse.equinox.p2.processing.test.Dummy";
		ArtifactDescriptor descriptor = new ArtifactDescriptor(ArtifactKey.parse("classifier,id,1.0.0"));
		descriptor.setProcessingSteps(new ProcessingStepDescriptor[] {new ProcessingStepDescriptor(noSuchStepId, null, true), new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.ByteShifter", "1", true)});

		assertFalse(ProcessingStepHandler.canProcess(descriptor));
	}

	public void testCanProcess_FailAlwaysDisabled() {
		ArtifactDescriptor descriptor = new ArtifactDescriptor(ArtifactKey.parse("classifier,id,1.0.0"));
		String processorId = "org.eclipse.equinox.p2.processing.AlwaysDisabled";
		descriptor.setProcessingSteps(new ProcessingStepDescriptor[] {new ProcessingStepDescriptor(processorId, null, true), new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.ByteShifter", "1", true)});

		assertFalse(ProcessingStepHandler.canProcess(descriptor));
	}

	public void testCanProcess_OkOptionalAlwaysDisabled() {
		ArtifactDescriptor descriptor = new ArtifactDescriptor(ArtifactKey.parse("classifier,id,1.0.0"));
		String processorId = "org.eclipse.equinox.p2.processing.AlwaysDisabled";
		descriptor.setProcessingSteps(new ProcessingStepDescriptor[] {new ProcessingStepDescriptor(processorId, null, false), new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.ByteShifter", "1", true)});

		assertTrue("Not enabled optional step should not block processing", ProcessingStepHandler.canProcess(descriptor));
	}

	public void testCanProcess_FailMultipleSteps() {
		ArtifactDescriptor descriptor = new ArtifactDescriptor(ArtifactKey.parse("classifier,id,1.0.0"));
		String processorId = "org.eclipse.equinox.p2.processing.MultipleSteps";
		descriptor.setProcessingSteps(new ProcessingStepDescriptor[] {new ProcessingStepDescriptor(processorId, null, true), new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.ByteShifter", "1", true)});

		assertFalse(String.format("Multiple step attributes in %s are not supported", processorId), ProcessingStepHandler.canProcess(descriptor));
	}

	public void testExecuteNoPSs() throws IOException {
		IProcessingStepDescriptor[] descriptors = new IProcessingStepDescriptor[0];
		OutputStream result = new ByteArrayOutputStream(10);
		try (OutputStream testStream = handler.createAndLink(getAgent(), descriptors, null, result, monitor)) {
			testStream.write("Test".getBytes());
		}
		assertEquals("Test", result.toString());
	}

	public void testExecuteOneByteShifterPS() throws IOException {
		ProcessingStep[] steps = new ProcessingStep[] {new ByteShifter(1)};
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		try (OutputStream testStream = handler.link(steps, result, monitor)) {
			testStream.write(new byte[] {1});
		}
		assertTrue(Arrays.equals(new byte[] {2}, result.toByteArray()));
	}

	public void testExecuteTwoByteShifterPSs() throws IOException {
		ProcessingStep[] steps = new ProcessingStep[] {new ByteShifter(1), new ByteShifter(2)};
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		try (OutputStream testStream = handler.link(steps, result, monitor)) {
			testStream.write(new byte[] {1});
		}
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
		try (OutputStream testStream = handler.link(steps, result, monitor)) {
			testStream.write(new byte[] {1, 2, 3, 4, 5});
		}
		assertTrue(Arrays.equals(new byte[] {4, 6, 8, 10, 12}, result.toByteArray()));
	}

	public void testAssureOrderingOfPSs2() throws IOException {
		ProcessingStep[] steps = new ProcessingStep[] {new Multiplier(2), new Adder(1)};
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		try (OutputStream testStream = handler.link(steps, result, monitor)) {
			testStream.write(new byte[] {1, 2, 3, 4, 5});
		}

		assertTrue(Arrays.equals(new byte[] {3, 5, 7, 9, 11}, result.toByteArray()));

	}

	public void testExecuteOnePack200UnpackerPS() throws IOException {
		//this test is only applicable if pack200 is available
		if (!PackStep.canPack())
			return;
		IProcessingStepDescriptor[] descriptors = new IProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)};
		ProcessingStep[] steps = handler.create(getAgent(), descriptors, null);
		ByteArrayOutputStream result = new ByteArrayOutputStream(100000);
		OutputStream testStream = handler.link(steps, result, monitor);
		IStatus status = ProcessingStepHandler.checkStatus(testStream);
		assertTrue("Step is not ready.", status.isOK());
		InputStream inputStream = TestActivator.getContext().getBundle().getEntry("testData/jarprocessor.jar.pack.gz").openStream();
		FileUtils.copyStream(inputStream, true, testStream, true);
		//the value 35062 obtained by manually unpacking the test artifact using unpack200.exe from sun 7u9 JRE
		assertEquals(35062, result.size());
	}

	public void testCreateByteShifterPS() {
		IProcessingStepDescriptor[] descriptors = new IProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.ByteShifter", "1", true)};
		ProcessingStep[] steps = handler.create(getAgent(), descriptors, null);
		assertNotNull(steps);
		assertEquals(1, steps.length);
		assertEquals(ByteShifter.class, steps[0].getClass());
	}

	@SuppressWarnings("deprecation")
	public void testCreateMD5VerifierPS() {
		String processorId = "org.eclipse.equinox.p2.processing.MD5Verifier";
		ProcessingStepDescriptor[] descriptors = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor(processorId, "1", true)};
		ProcessingStep[] steps = handler.create(getAgent(), descriptors, null);
		assertNotNull(steps);
		assertEquals(1, steps.length);
		assertNotEquals(String.format("Step '%s' is not available anymore", processorId), MD5Verifier.class, steps[0].getClass());
		assertEquals(IStatus.ERROR, steps[0].getStatus().getSeverity());
	}

	public void testCreateChecksumVerifierPS() {
		ProcessingStepDescriptor processingStepDescriptor = new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.ChecksumVerifier", "1", true);
		ProcessingStepDescriptor[] descriptors = new ProcessingStepDescriptor[] {processingStepDescriptor};
		ProcessingStep[] steps = handler.create(getAgent(), descriptors, null);
		assertNotNull(steps);
		assertEquals(1, steps.length);
		assertEquals(IStatus.ERROR, steps[0].getStatus().getSeverity());
	}

	public void testCreateAdderPS() {
		IProcessingStepDescriptor[] descriptors = new IProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Adder", "1", true)};
		ProcessingStep[] steps = handler.create(getAgent(), descriptors, null);
		assertNotNull(steps);
		assertEquals(1, steps.length);
		assertEquals(Adder.class, steps[0].getClass());
	}

	public void testCreateMultiplierPS() {
		IProcessingStepDescriptor[] descriptors = new IProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Multiplier", "2", true)};
		ProcessingStep[] steps = handler.create(getAgent(), descriptors, null);
		assertNotNull(steps);
		assertEquals(1, steps.length);
		assertEquals(Multiplier.class, steps[0].getClass());
	}

	public void testCreatePack200UnpackerPS() {
		IProcessingStepDescriptor[] descriptors = new IProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)};
		ProcessingStep[] steps = handler.create(getAgent(), descriptors, null);
		assertNotNull(steps);
		assertEquals(1, steps.length);
		assertEquals(Pack200ProcessorStep.class, steps[0].getClass());
	}

	public void testCreatePSsAndAssureOrderingOfPSs1() throws IOException {
		IProcessingStepDescriptor adder = new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Adder", "1", true);
		IProcessingStepDescriptor multiplier = new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Multiplier", "2", true);
		IProcessingStepDescriptor[] descriptors = new IProcessingStepDescriptor[] {adder, multiplier};
		ProcessingStep[] steps = handler.create(getAgent(), descriptors, null);
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		try (OutputStream testStream = handler.link(steps, result, monitor)) {
			testStream.write(new byte[] {1, 2, 3, 4, 5});
		}
		assertTrue(Arrays.equals(new byte[] {4, 6, 8, 10, 12}, result.toByteArray()));
	}

	public void testCreatePSsAndAssureOrderingOfPSs2() throws IOException {
		IProcessingStepDescriptor adder = new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Adder", "1", true);
		IProcessingStepDescriptor multiplier = new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Multiplier", "2", true);
		IProcessingStepDescriptor[] descriptors = new IProcessingStepDescriptor[] {multiplier, adder};
		ProcessingStep[] steps = handler.create(getAgent(), descriptors, null);
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		try (OutputStream testStream = handler.link(steps, result, monitor)) {
			testStream.write(new byte[] {1, 2, 3, 4, 5});
		}
		assertTrue(Arrays.equals(new byte[] {3, 5, 7, 9, 11}, result.toByteArray()));
	}

	public void testLinkPSs() throws IOException {
		IProcessingStepDescriptor adder = new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Adder", "1", true);
		IProcessingStepDescriptor multiplier = new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Multiplier", "2", true);
		IProcessingStepDescriptor[] descriptors = new IProcessingStepDescriptor[] {adder, multiplier};
		ByteArrayOutputStream result = new ByteArrayOutputStream(10);
		try (OutputStream testStream = handler.createAndLink(getAgent(), descriptors, null, result, monitor)) {
			testStream.write(new byte[] {1, 2, 3, 4, 5});
		}
		assertTrue(Arrays.equals(new byte[] {4, 6, 8, 10, 12}, result.toByteArray()));
	}

	public void testPSHgetStatusOK() {
		ProcessingStep ok1, ok2;
		ok1 = new ProcessingStep() {
			@Override
			public IStatus getStatus() {
				return Status.OK_STATUS;
			}
		};
		ok2 = new ProcessingStep() {
			@Override
			public IStatus getStatus() {
				return Status.OK_STATUS;
			}
		};

		OutputStream testStream = handler.link(new ProcessingStep[] {ok1, ok2}, null, monitor);
		IStatus status = ProcessingStepHandler.getStatus(testStream);
		IStatus errStatus = ProcessingStepHandler.getStatus(testStream);
		assertTrue(status.isOK() && errStatus.isOK());
		assertTrue(!status.isMultiStatus());
		assertTrue(!errStatus.isMultiStatus());
	}

	public void testPSHgetStatus() {
		ProcessingStep ok, info, warning, error;
		ok = new ProcessingStep() {
			@Override
			public IStatus getStatus() {
				return Status.OK_STATUS;
			}
		};

		info = new ProcessingStep() {
			@Override
			public IStatus getStatus() {
				return new Status(IStatus.INFO, "ID", "INFO");
			}
		};

		warning = new ProcessingStep() {
			@Override
			public IStatus getStatus() {
				return new Status(IStatus.WARNING, "ID", "WARNING");
			}
		};

		error = new ProcessingStep() {
			@Override
			public IStatus getStatus() {
				return new Status(IStatus.ERROR, "ID", "ERROR");
			}
		};

		OutputStream testStream = handler.link(new ProcessingStep[] {info, ok, error, warning}, null, monitor);
		assertTrue(ProcessingStepHandler.getErrorStatus(testStream).getChildren().length == 2);
		assertTrue(ProcessingStepHandler.getStatus(testStream, true).getChildren().length == 4);
	}
}
