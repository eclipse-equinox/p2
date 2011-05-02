/*******************************************************************************
 * Copyright (c) 2007, 2010 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Test code that is affected by bug 252308 within {@code MirrorRequest}.
 */
public class Bug252308 extends AbstractProvisioningTest {

	private Method transferSingle;
	private Method extractRootCause;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		extractRootCause = MirrorRequest.class.getDeclaredMethod("extractRootCause", new Class[] {IStatus.class});
		extractRootCause.setAccessible(true);
		transferSingle = MirrorRequest.class.getDeclaredMethod("transferSingle", new Class[] {IArtifactDescriptor.class, IArtifactDescriptor.class, IProgressMonitor.class});
		transferSingle.setAccessible(true);
	}

	private IStatus extractRootCause(IStatus status) throws Exception {
		return (IStatus) extractRootCause.invoke(null, new Object[] {status});
	}

	private IStatus transferSingle(MirrorRequest request, IArtifactDescriptor destinationDescriptor, IArtifactDescriptor sourceDescriptor, IProgressMonitor monitor) throws Exception {
		return (IStatus) transferSingle.invoke(request, new Object[] {destinationDescriptor, sourceDescriptor, monitor});
	}

	public void testExtractRootCauseNullStatus() throws Exception {
		IStatus status = extractRootCause(null);
		assertNull(status);
	}

	public void testExtractRootCauseWarningStatus() throws Exception {
		IStatus status = extractRootCause(new Status(IStatus.WARNING, "id1", "Test", new IOException("IO")));
		assertNull(status);
	}

	public void testExtractRootCauseErrorStatusWithException() throws Exception {
		IStatus status = extractRootCause(new Status(IStatus.ERROR, "id1", "Test", new IOException("IO")));
		assertNotNull(status);
		assertEquals("id1", status.getPlugin());
		assertEquals("Test", status.getMessage());
		assertEquals("IO", status.getException().getMessage());
	}

	public void testExtractRootCauseFromFlatMultiStatus() throws Exception {
		MultiStatus multiStatus = new MultiStatus("id1", 0, "Message", new FileNotFoundException("FNFE"));
		IStatus status = extractRootCause(multiStatus);
		assertNull(status);
	}

	public void testExtractRootCauseFromNestedMultiStatus() throws Exception {
		MultiStatus multiStatus = new MultiStatus("id0", 0, "Message", new FileNotFoundException("FNFE"));
		Status status1 = new Status(IStatus.WARNING, "id1", "Test", new IOException("IO"));
		Status status2 = new Status(IStatus.ERROR, "id2", "Test", new IOException("IO"));
		Status status3 = new Status(IStatus.ERROR, "id3", "Test", null);
		multiStatus.add(status1);
		multiStatus.add(status2);
		multiStatus.add(status3);

		IStatus status = extractRootCause(multiStatus);
		assertNotNull(status);
		assertEquals("id2", status.getPlugin());
	}

	public void testExtractRootCauseFromNestedNestedMultiStatus() throws Exception {
		MultiStatus multiStatus = new MultiStatus("id0", 0, "Message", new FileNotFoundException("FNFE"));
		Status status1 = new Status(IStatus.WARNING, "id1", "Test", new IOException("IO"));
		MultiStatus status2 = new MultiStatus("id2", 0, "Test", null);
		Status status3 = new Status(IStatus.ERROR, "id3", "Test", new NullPointerException("NPE"));
		multiStatus.add(status1);
		multiStatus.add(status2);
		multiStatus.add(status3);

		Status status21 = new Status(IStatus.WARNING, "id21", "Test", new IOException("IO"));
		Status status22 = new Status(IStatus.ERROR, "id22", "Test", new IOException("IO"));
		Status status23 = new Status(IStatus.ERROR, "id23", "Test", new ProvisionException("PE"));
		status2.add(status21);
		status2.add(status22);
		status2.add(status23);

		IStatus status = extractRootCause(multiStatus);
		assertNotNull(status);
		assertEquals("id22", status.getPlugin());
	}

	public void testTransferError() throws Exception {
		File simpleRepo = getTestData("simple repository", "testData/artifactRepo/transferTestRepo");
		IArtifactRepository source = null;
		IArtifactRepository target = null;
		try {
			source = getArtifactRepositoryManager().loadRepository(simpleRepo.toURI(), new NullProgressMonitor());
			target = createArtifactRepository(new File(getTempFolder(), getName()).toURI(), null);
		} catch (ProvisionException e) {
			fail("failing setting up the tests", e);
		}

		IArtifactDescriptor sourceDescriptor = getArtifactKeyFor(source, "osgi.bundle", "missingFromFileSystem", Version.createOSGi(1, 0, 0))[0];
		SimpleArtifactDescriptor targetDescriptor = new SimpleArtifactDescriptor(sourceDescriptor);
		targetDescriptor.setRepositoryProperty("artifact.folder", "true");
		class TestRequest extends MirrorRequest {
			public TestRequest(IArtifactKey key, IArtifactRepository targetRepository, Map<String, String> targetDescriptorProperties, Map<String, String> targetRepositoryProperties) {
				super(key, targetRepository, targetDescriptorProperties, targetRepositoryProperties, getTransport());
			}

			public void setSource(IArtifactRepository source) {
				super.setSourceRepository(source);
			}
		}
		TestRequest request = new TestRequest(new ArtifactKey("osgi.bundle", "missingFromFileSystem", Version.createOSGi(1, 0, 0)), target, null, null);
		request.setSource(source);
		IStatus s = transferSingle(request, targetDescriptor, sourceDescriptor, new NullProgressMonitor());
		assertTrue(s.toString(), s.getException().getClass() == FileNotFoundException.class);
	}

	private IArtifactDescriptor[] getArtifactKeyFor(IArtifactRepository repo, String classifier, String id, Version version) {
		return repo.getArtifactDescriptors(new ArtifactKey(classifier, id, version));
	}

}
