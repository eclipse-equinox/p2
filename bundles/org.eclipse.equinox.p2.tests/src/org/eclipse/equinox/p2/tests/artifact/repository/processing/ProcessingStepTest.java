/*******************************************************************************
 * Copyright (c) 2007, 2008 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository.processing;

import java.io.IOException;
import java.io.OutputStream;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;

public class ProcessingStepTest extends TestCase {

	private ProcessingStep ps;
	private boolean flushed;
	private boolean closed;

	protected void setUp() throws Exception {
		super.setUp();
		ps = new ProcessingStep() {};
		flushed = false;
		closed = false;
	}

	public void testDefaultStatus() {
		assertNotNull(ps.getStatus());
		assertTrue(ps.getStatus().isOK());
		assertTrue(ps.getStatus(false).isOK());
		assertTrue(ps.getStatus(true).isOK());
		assertTrue(ProcessingStepHandler.checkStatus(ps).isOK());
	}

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

	public void testFlush() throws IOException {
		OutputStream destination = new OutputStream() {

			public void write(int b) {
			}

			public void flush() {
				flushed = true;
			}
		};
		ps.link(destination, new NullProgressMonitor());
		ps.flush();
		assertTrue(flushed);
	}

	public void testCloseSimpleOutputStreamAsDestination() throws IOException {
		OutputStream destination = new OutputStream() {

			public void write(int b) {
			}

			public void close() {
				closed = true;
			}
		};
		ps.link(destination, new NullProgressMonitor());
		ps.close();
		assertFalse(closed);
	}

	public void testCloseProcessingStepAsDestination() throws IOException {
		OutputStream destination = new ProcessingStep() {
			public void close() {
				closed = true;
			}
		};
		ps.link(destination, new NullProgressMonitor());
		ps.close();
		assertTrue(closed);
	}

}
