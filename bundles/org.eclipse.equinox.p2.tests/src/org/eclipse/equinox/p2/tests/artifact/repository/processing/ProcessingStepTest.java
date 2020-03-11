/*******************************************************************************
 * Copyright (c) 2007, 2017 compeople AG and others.
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
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.junit.Before;
import org.junit.Test;

public class ProcessingStepTest {

	private ProcessingStep ps;
	boolean flushed;
	boolean closed;

	@Before
	public void setUp() throws Exception {
		ps = new ProcessingStep() {};
		flushed = false;
		closed = false;
	}

	@Test
	public void testDefaultStatus() {
		assertNotNull(ps.getStatus());
		assertTrue(ps.getStatus().isOK());
		assertTrue(ps.getStatus(false).isOK());
		assertTrue(ps.getStatus(true).isOK());
		assertTrue(ProcessingStepHandler.checkStatus(ps).isOK());
	}

	@Test
	public void testGetDeepStatus() {
		ProcessingStep ps2 = new ProcessingStep() {};
		ps.link(ps2, new NullProgressMonitor());
		assertTrue(ps.getStatus(true).isOK());

		ps2.setStatus(new Status(IStatus.ERROR, "plugin id", "message"));
		assertFalse(ps.getStatus(true).isOK());
		assertTrue(ps.getStatus(true).isMultiStatus());
		assertEquals(IStatus.ERROR, ps.getStatus(true).getSeverity());
		MultiStatus multi = (MultiStatus) ps.getStatus(true);
		assertEquals(2, multi.getChildren().length);

		ProcessingStep ps3 = new ProcessingStep() {};
		ps2.link(ps3, new NullProgressMonitor());
		assertFalse(ps.getStatus(true).isOK());
		assertTrue(ps.getStatus(true).isMultiStatus());
		assertEquals(IStatus.ERROR, ps.getStatus(true).getSeverity());
		multi = (MultiStatus) ps.getStatus(true);
		assertEquals(3, multi.getChildren().length);

		ps3.setStatus(Status.CANCEL_STATUS);
		assertFalse(ps.getStatus(true).isOK());
		assertTrue(ps.getStatus(true).isMultiStatus());
		assertEquals(IStatus.CANCEL, ps.getStatus(true).getSeverity());
	}

	@Test
	public void testFlush() throws IOException {
		OutputStream destination = new OutputStream() {

			@Override
			public void write(int b) {
			}

			@Override
			public void flush() {
				flushed = true;
			}
		};
		ps.link(destination, new NullProgressMonitor());
		ps.flush();
		assertTrue(flushed);
	}

	@Test
	public void testCloseSimpleOutputStreamAsDestination() throws IOException {
		OutputStream destination = new OutputStream() {

			@Override
			public void write(int b) {
			}

			@Override
			public void close() {
				closed = true;
			}
		};
		ps.link(destination, new NullProgressMonitor());
		ps.close();
		assertFalse(closed);
	}

	@Test
	public void testCloseProcessingStepAsDestination() throws IOException {
		OutputStream destination = new ProcessingStep() {
			@Override
			public void close() {
				closed = true;
			}
		};
		ps.link(destination, new NullProgressMonitor());
		ps.close();
		assertTrue(closed);
	}

}
