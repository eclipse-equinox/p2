/*******************************************************************************
 * Copyright (c) 2007, 2010 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 * IBM - ongoing maintenance
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.*;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository.ArtifactOutputStream;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ArtifactOutputStreamTest extends AbstractProvisioningTest {

	private SimpleArtifactRepository sar = null;
	private ProcessingStep destination = null;
	private IArtifactKey ak = null;
	private IArtifactDescriptor ad = null;
	private ArtifactOutputStream aos = null;
	private File temp = null;
	private File tempWritableLocation = null;

	protected void setUp() throws Exception {
		super.setUp();

		tempWritableLocation = File.createTempFile("artifact", ".repo");
		tempWritableLocation.delete();
		tempWritableLocation.mkdirs();
		sar = new SimpleArtifactRepository(getAgent(), "name", tempWritableLocation.toURI(), null);
		destination = new Destination();
		ak = new ArtifactKey("classifier", "id", Version.create("1.0"));
		ad = new ArtifactDescriptor(ak);
		temp = File.createTempFile("ArtifactOutputStreamTest", ".tmp");
		temp.deleteOnExit();
		aos = sar.new ArtifactOutputStream(destination, ad, temp);
		assertNotNull(aos);
		Destination.ioe = null;
		Destination.baos = null;
	}

	protected void tearDown() throws Exception {
		AbstractProvisioningTest.delete(temp);
		AbstractProvisioningTest.delete(tempWritableLocation);
		super.tearDown();
	}

	public void testStatefullness() {
		assertTrue(aos instanceof IStateful);
		assertTrue(aos instanceof ArtifactOutputStream);
	}

	public void testSingleCloseStreamOkDestinationOk() throws IOException {
		assertTrue(temp.exists());
		aos.write(22);
		aos.close();
		assertTrue(temp.exists());
		assertEquals("1", ad.getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));
	}

	public void testDoubleCloseStreamOkDestinationOk() throws IOException {
		assertTrue(temp.exists());
		aos.write(22);
		aos.close();
		assertTrue(temp.exists());
		assertEquals("1", ad.getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));
		// tests bug 212476
		aos.close();
		assertTrue(temp.exists());
	}

	public void testSingleCloseStreamNotOkDestinationOk() throws IOException {
		assertTrue(temp.exists());
		aos.setStatus(Status.CANCEL_STATUS);
		aos.close();
		assertFalse(temp.exists());
	}

	public void testSingleCloseStreamOkDestinationNotOk() throws IOException {
		assertTrue(temp.exists());
		destination.setStatus(Status.CANCEL_STATUS);
		aos.close();
		assertFalse(temp.exists());
	}

	public void testSingleCloseStreamOkDestinationCloseFails() {
		assertTrue(temp.exists());
		Destination.ioe = new IOException("Expected");
		try {
			aos.close();
			assertTrue(false);
		} catch (IOException ioe) {
			assertTrue(true);
		} finally {
			assertFalse(temp.exists());
		}
	}

	public void testSingleCloseStreamNotOkDestinationCloseFails() {
		assertTrue(temp.exists());
		Destination.ioe = new IOException("Expected");
		aos.setStatus(Status.CANCEL_STATUS);
		try {
			aos.close();
		} catch (IOException ioe) {
			assertTrue(false);
		} finally {
			assertFalse(temp.exists());
		}
	}

	public void testWriteToDestinationStreamOkDestinationOk() throws IOException {
		Destination.baos = new ByteArrayOutputStream();
		assertTrue(temp.exists());
		aos.write(22);
		aos.close();
		assertTrue(temp.exists());
		assertEquals("1", ad.getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));
		aos.close();
		assertTrue(temp.exists());
		assertEquals(1, Destination.baos.toByteArray().length);
		assertEquals(22, Destination.baos.toByteArray()[0]);
	}

	static class Destination extends ProcessingStep {

		static IOException ioe = null;
		static ByteArrayOutputStream baos = null;

		public void close() throws IOException {
			super.close();
			if (ioe != null) {
				throw ioe;
			}
		}

		public void write(int b) {
			if (baos != null)
				baos.write(b);
		}
	}
}
