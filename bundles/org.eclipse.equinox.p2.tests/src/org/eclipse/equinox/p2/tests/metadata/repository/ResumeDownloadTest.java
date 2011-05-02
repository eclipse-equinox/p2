/*******************************************************************************
 * Copyright (c) 2006, 2010, Cloudsmith Inc.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 ******************************************************************************/

package org.eclipse.equinox.p2.tests.metadata.repository;

import java.net.URI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ecf.filetransfer.IFileRangeSpecification;
import org.eclipse.ecf.filetransfer.IIncomingFileTransfer;
import org.eclipse.equinox.internal.p2.transport.ecf.FileReader;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ResumeDownloadTest extends AbstractProvisioningTest {
	private static String UPDATE_SITE = "http://download.eclipse.org/eclipse/updates/3.4";
	private IMetadataRepositoryManager mgr;
	private URI repoLoc;
	private String originalResumeProp;

	protected void setUp() throws Exception {
		super.setUp();
		repoLoc = new URI(UPDATE_SITE);
		originalResumeProp = System.getProperty("org.eclipse.equinox.p2.metadata.repository.resumable", "true");

		mgr = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (mgr == null) {
			throw new RuntimeException("Repository manager could not be loaded");
		}
		mgr.removeRepository(repoLoc);
		if (mgr.contains(repoLoc))
			throw new RuntimeException("Error - An earlier test did not leave a clean state - could not remove repo");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		FileReader.setTestProbe(null);
		// reset the resume property to what it was before the test.
		System.setProperty("org.eclipse.equinox.p2.metadata.repository.resumable", originalResumeProp);
		mgr.removeRepository(repoLoc);
	}

	public void testResume() throws ProvisionException {
		boolean caught = false;
		try {
			FileReader.setTestProbe(new CancelSimulator());
			mgr.loadRepository(repoLoc, null);
		} catch (OperationCanceledException e) {
			/* ignore - the operation is supposed to be canceled */
			caught = true;
		}
		assertTrue("Cancel should have been caught (1)", caught);
		caught = false;
		FileReader.setTestProbe(null);

		FileReader.setTestProbe(new ResumeCheck());
		// Try again - this time it should resume
		mgr.loadRepository(repoLoc, null);

		assertTrue("Cancelation was made before entire file was downloaded", bytesReceived < entireLength);
		assertEquals("First+remaining size equals entire size", bytesReceived + remainingLength, entireLength);
		assertTrue("Remaining length smaller than entire length", entireLength > remainingLength);

	}

	public void testResumeTwice() throws ProvisionException {
		boolean caught = false;
		try {
			FileReader.setTestProbe(new CancelSimulator());
			mgr.loadRepository(repoLoc, null);
		} catch (OperationCanceledException e) {
			/* ignore - the operation is supposed to be canceled */
			caught = true;
		}
		assertTrue("Cancel should have been caught (1)", caught);
		caught = false;

		try {
			FileReader.setTestProbe(new CancelSimulator());
			mgr.loadRepository(repoLoc, null);
		} catch (OperationCanceledException e) {
			/* ignore - the operation is supposed to be canceled */
			caught = true;
		}
		assertTrue("Cancel should have been caught (2)", caught);
		caught = false;
		FileReader.setTestProbe(null);

		FileReader.setTestProbe(new ResumeCheck());
		mgr.loadRepository(repoLoc, null);

		assertTrue("Cancelation was made before entire file was downloaded", bytesReceived < entireLength);
		assertEquals("First+remaining size equals entire size", bytesReceived + remainingLength, entireLength);
		assertTrue("Remaining length smaller than entire length", entireLength > remainingLength);

	}

	public void testBlockedResume() throws ProvisionException {
		// block the resume functionality
		System.setProperty("org.eclipse.equinox.p2.metadata.repository.resumable", "false");

		boolean caught = false;
		try {
			FileReader.setTestProbe(new CancelSimulator());
			mgr.loadRepository(repoLoc, null);
		} catch (OperationCanceledException e) {
			/* ignore - the operation is supposed to be canceled */
			caught = true;
		}
		assertTrue("Cancel should have been caught (1)", caught);
		caught = false;
		FileReader.setTestProbe(null);

		FileReader.setTestProbe(new ResumeCheck());
		// Try again - this time it should NOT resume
		mgr.loadRepository(repoLoc, null);

		assertTrue("Cancelation was made before entire file was downloaded", bytesReceived < entireLength);
		assertEquals("Resume starts at 0", 0L, resumeStart);
		assertEquals("Remaining length == original length", entireLength, remainingLength);

	}

	protected long bytesReceived = 0L;
	protected long entireLength = 0L;
	protected long resumeStart = 0L;
	protected long remainingLength = 0L;
	protected long resumedReceived = 0L;

	private class CancelSimulator implements FileReader.IFileReaderProbe {

		public CancelSimulator() {
			//
		}

		public void onData(FileReader reader, IIncomingFileTransfer source, IProgressMonitor monitor) {
			bytesReceived = source.getBytesReceived();
			if (bytesReceived > 1000)
				monitor.setCanceled(true);
		}

		public void onDone(FileReader reader, IIncomingFileTransfer source, IProgressMonitor monitor) {
			bytesReceived = source.getBytesReceived();
		}

		public void onStart(FileReader reader, IIncomingFileTransfer source, IProgressMonitor monitor) {
			entireLength = source.getFileLength();
		}

	}

	private class ResumeCheck implements FileReader.IFileReaderProbe {

		public ResumeCheck() {
			//
		}

		public void onData(FileReader reader, IIncomingFileTransfer source, IProgressMonitor monitor) {
			resumedReceived = source.getBytesReceived();
		}

		public void onDone(FileReader reader, IIncomingFileTransfer source, IProgressMonitor monitor) {
			/* ignore */
		}

		public void onStart(FileReader reader, IIncomingFileTransfer source, IProgressMonitor monitor) {
			IFileRangeSpecification spec = source.getFileRangeSpecification();
			resumeStart = spec == null ? 0L : spec.getStartPosition();
			remainingLength = source.getFileLength();

		}

	}
}
