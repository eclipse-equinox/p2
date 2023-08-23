/*******************************************************************************
 *  Copyright (c) 2005, 2020 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorSelector;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.equinox.p2.repository.spi.AbstractRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.AbstractWrappedArtifactRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MirrorRequestTest extends AbstractProvisioningTest {
	private static final String testDataLocation = "testData/artifactRepo/emptyJarRepo";
	File targetLocation;
	IArtifactRepository targetRepository, sourceRepository;
	URI destination, failedOptimized, pakedRepositoryLocation;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		targetLocation = File.createTempFile("target", ".repo");
		targetLocation.delete();
		targetLocation.mkdirs();
		targetRepository = new SimpleArtifactRepository(getAgent(), "TargetRepo", targetLocation.toURI(), null);

		IArtifactRepositoryManager mgr = getArtifactRepositoryManager();
		sourceRepository = mgr.loadRepository((getTestData("EmptyJar repo", testDataLocation).toURI()), null);
		failedOptimized = URIUtil.toJarURI(getTestData("Error loading test data", "testData/mirror/invalidPackedMissingCanonical.zip").toURI(), null);
		pakedRepositoryLocation = getTestData("Error loading packed repository", "testData/mirror/mirrorPackedRepo").toURI();
		destination = getTempFolder().toURI();
	}

	@Override
	protected void tearDown() throws Exception {
		getArtifactRepositoryManager().removeRepository(destination);
		getArtifactRepositoryManager().removeRepository(failedOptimized);
		getArtifactRepositoryManager().removeRepository(targetLocation.toURI());
		getArtifactRepositoryManager().removeRepository(pakedRepositoryLocation);
		AbstractProvisioningTest.delete(targetLocation);
		delete(new File(destination));
		super.tearDown();
	}

	public void testInvalidZipFileInTheSource() {
		IArtifactKey key = new ArtifactKey("org.eclipse.update.feature", "HelloWorldFeature", Version.createOSGi(1, 0, 0));
		Map<String, String> targetProperties = new HashMap<>();
		targetProperties.put("artifact.folder", "true");
		MirrorRequest request = new MirrorRequest(key, targetRepository, null, targetProperties, getAgent().getService(Transport.class));
		request.perform(sourceRepository, new NullProgressMonitor());

		assertTrue(request.getResult().matches(IStatus.ERROR));
		assertTrue(request.getResult().getException() instanceof IOException);
	}

	public void testMissingArtifact() {
		IArtifactKey key = new ArtifactKey("org.eclipse.update.feature", "Missing", Version.createOSGi(1, 0, 0));
		Map<String, String> targetProperties = new HashMap<>();
		targetProperties.put("artifact.folder", "true");
		MirrorRequest request = new MirrorRequest(key, targetRepository, null, targetProperties, getTransport());
		request.perform(sourceRepository, new NullProgressMonitor());

		assertTrue(request.getResult().matches(IStatus.ERROR));
	}

	// Test that if MirrorRequest fails to download a packed artifact it attempts the canonical version
	public void testFailToCanonical() {
		RemoteRepo src = new RemoteRepo((SimpleArtifactRepository) sourceRepository);

		IArtifactKey key = new ArtifactKey("test.txt", "fail_to_canonical", Version.parseVersion("1.0.0"));
		MirrorRequest request = new MirrorRequest(key, targetRepository, null, null, getTransport());
		request.perform(src, new NullProgressMonitor());

		assertTrue(request.getResult().toString(), request.getResult().isOK());
		assertTrue(String.format("Target does not contain artifact %s", key), targetRepository.contains(key));
		// Pack200 tools are gone in Java14+ thus pack.gz file is not downloaded
		assertEquals("Exact number of downloads", 1, src.downloadCount);
	}

	/**
	 * Same as {@link #testFailToCanonical()} but with 3 mirrors:
	 * <ul>
	 * <li><code>mirror-one</code>, which is unreachable</li>
	 * <li><code>mirror-two</code>, which has an invalid optimized artifact and causes processing step to fail</li>
	 * <li>original repository, which has a valid canonical artifact</li>
	 * </ul>
	 *
	 */
	public void testFailToCanonicalWithMirrors() {
		OrderedMirrorSelector selector = new OrderedMirrorSelector(sourceRepository);
		try {
			RemoteRepo src = new RemoteRepo((SimpleArtifactRepository) sourceRepository);

			IArtifactKey key = new ArtifactKey("test.txt", "fail_to_canonical", Version.parseVersion("1.0.0"));
			MirrorRequest request = new MirrorRequest(key, targetRepository, null, null, getTransport());
			request.perform(src, new NullProgressMonitor());

			assertTrue(request.getResult().toString(), request.getResult().isOK());
			assertTrue(String.format("Target does not contain artifact %s", key), targetRepository.contains(key));
			assertEquals("Exact number of downloads", 3, src.downloadCount);
			assertEquals("All mirrors utilized", selector.mirrors.length, selector.index);
		} finally {
			selector.clearSelector();
		}
	}

	// Test that SimpleArtifactRepository & MirrorRequest use mirrors in the event of a failure.
	public void testMirrorFailOver() {
		OrderedMirrorSelector selector = new OrderedMirrorSelector(sourceRepository);
		try {
			// call test
			IArtifactKey key = new ArtifactKey("test.txt", "HelloWorldText", Version.parseVersion("1.0.0"));
			MirrorRequest request = new MirrorRequest(key, targetRepository, null, null, getTransport());
			request.perform(sourceRepository, new NullProgressMonitor());

			// The download succeeded
			assertTrue(request.getResult().toString(), request.getResult().isOK());
			// All available mirrors used
			assertEquals("All mirrors utilized", selector.mirrors.length, selector.index);
		} finally {
			selector.clearSelector();
		}
	}

	/*
	 * Test that the expected Status level is returned when a mirror fails from packed to canonical
	 */
	public void testStatusFromFailover() {
		StatusSequenceRepository source = null;
		LinkedList<IStatus> seq = new LinkedList<>();
		try {
			source = new StatusSequenceRepository(getArtifactRepositoryManager().loadRepository(pakedRepositoryLocation, new NullProgressMonitor()));

		} catch (ProvisionException e) {
			fail("Failed to load source repository");
		}
		source.setSequence(seq);
		// Grab an ArtifactKey to mirror, doesn't matter which
		IQueryResult<IArtifactKey> keys = source.query(ArtifactKeyQuery.ALL_KEYS, null);
		assertTrue("Unable to obtain artifact keys", keys != null && !keys.isEmpty());

		IArtifactKey key = keys.iterator().next();
		MirrorRequest req = new MirrorRequest(key, targetRepository, null, null, getTransport());

		req.perform(source, new NullProgressMonitor());

		// packed artifact is ignored as Java 14 removed pack200
		assertEquals("Expected OK status", IStatus.OK, req.getResult().getSeverity());

		// Remove key from repo so the same one can be used
		targetRepository.removeDescriptor(key, new NullProgressMonitor());

		req = new MirrorRequest(key, targetRepository, null, null, getTransport());

		req.perform(source, new NullProgressMonitor());

		// packed artifact is ignored as Java 14 removed pack200
		assertEquals("Expected OK status", IStatus.OK, req.getResult().getSeverity());

		// Remove key from repo so the same one can be used
		targetRepository.removeDescriptor(key, new NullProgressMonitor());

		req = new MirrorRequest(key, targetRepository, null, null, getTransport());

		req.perform(source, new NullProgressMonitor());
		// packed artifact is ignored as Java 14 removed pack200
		assertEquals("Expected OK status", IStatus.OK, req.getResult().getSeverity());
	}

	protected static void assertStatusContains(String message, IStatus status, String statusString) {
		if (!statusContains(status, statusString))
			fail(message);
	}

	class StatusSequenceRepository extends AbstractWrappedArtifactRepository {
		Queue<IStatus> sequence;

		public StatusSequenceRepository(IArtifactRepository repo) {
			super(repo);
		}

		@Override
		public URI getLocation() {
			// Lie about the location so packed files are used
			try {
				return new URI("http://somewhere");
			} catch (URISyntaxException e) {
				return null;
			}
		}

		@Override
		public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
			try {
				destination.write(new byte[] {1, 1, 2});
			} catch (Exception e) {
				fail("Failed to write to stream", e);
			}
			if (sequence.isEmpty())
				return Status.OK_STATUS;
			return sequence.remove();
		}

		public void setSequence(Queue<IStatus> queue) {
			sequence = queue;
		}
	}

	private static boolean statusContains(IStatus status, String statusString) {
		if (status.getMessage().indexOf(statusString) != -1)
			return true;
		if (!status.isMultiStatus())
			return false;

		IStatus[] children = status.getChildren();
		for (IStatus child : children) {
			if (statusContains(child, statusString)) {
				return true;
			}
		}

		return false;
	}

	// Repository which misleads about its location
	protected class RemoteRepo extends AbstractArtifactRepository {
		SimpleArtifactRepository delegate;
		int downloadCount = 0;

		RemoteRepo(SimpleArtifactRepository repo) {
			super(getAgent(), repo.getName(), repo.getType(), repo.getVersion(), repo.getLocation(), repo.getDescription(), repo.getProvider(), repo.getProperties());
			delegate = repo;
		}

		@Override
		public synchronized URI getLocation() {
			try {
				return new URI("http://test/");
			} catch (URISyntaxException e) {
				// Should never happen, but we'll fail anyway
				fail("URI creation failed", e);
				return null;
			}
		}

		@Override
		public boolean contains(IArtifactDescriptor descriptor) {
			return delegate.contains(descriptor);
		}

		@Override
		public boolean contains(IArtifactKey key) {
			return delegate.contains(key);
		}

		@Override
		public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
			downloadCount++;
			return delegate.getArtifact(descriptor, destination, monitor);
		}

		@Override
		public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
			return delegate.getArtifactDescriptors(key);
		}

		@Override
		public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
			return delegate.getArtifacts(requests, monitor);
		}

		@Override
		public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
			return delegate.getOutputStream(descriptor);
		}

		@Override
		public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
			return delegate.getRawArtifact(descriptor, destination, monitor);
		}

		@Override
		public IQueryable<IArtifactDescriptor> descriptorQueryable() {
			return delegate.descriptorQueryable();
		}

		@Override
		public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
			return delegate.query(query, monitor);
		}
	}

	/*
	 * Special mirror selector for testing which chooses mirrors in order
	 */
	protected class OrderedMirrorSelector extends MirrorSelector {
		private URI repoLocation;
		int index = 0;
		MirrorInfo[] mirrors;
		IArtifactRepository repo;
		MirrorSelector oldSelector = null;

		OrderedMirrorSelector(IArtifactRepository repo) {
			super(repo, getTransport());
			this.repo = repo;
			// Setting this property forces SimpleArtifactRepository to use mirrors despite being a local repo
			// Alternatively we could use reflect to change "location" of the repo
			repo.setProperty(SimpleArtifactRepository.PROP_FORCE_THREADING, String.valueOf(true));
			setSelector();
			getRepoLocation();
			mirrors = computeMirrors("file:///" + getTestData("Mirror Location", testDataLocation + '/' + repo.getProperties().get(IRepository.PROP_MIRRORS_URL)).toString().replace('\\', '/'));
		}

		// Hijack the source repository's MirrorSelector
		private void setSelector() {
			Field mirrorField = null;
			try {
				mirrorField = SimpleArtifactRepository.class.getDeclaredField("mirrors");
				mirrorField.setAccessible(true);
				oldSelector = (MirrorSelector) mirrorField.get(repo); // Store the old value so we can restore it
				mirrorField.set(repo, this);
			} catch (Exception e) {
				fail("0.2", e);
			}
		}

		// Clear the mirror selector we place on the repository
		public void clearSelector() {
			if (repo == null) {
				return;
			}
			repo.setProperty(SimpleArtifactRepository.PROP_FORCE_THREADING, String.valueOf(false));
			Field mirrorField = null;
			try {
				mirrorField = SimpleArtifactRepository.class.getDeclaredField("mirrors");
				mirrorField.setAccessible(true);
				mirrorField.set(repo, oldSelector);
			} catch (Exception e) {
				fail("0.2", e);
			}
		}

		// Overridden to prevent mirror sorting
		@Override
		public synchronized void reportResult(String toDownload, IStatus result) {
			return;
		}

		// We want to test each mirror once.
		@Override
		public synchronized boolean hasValidMirror() {
			return mirrors != null && index < mirrors.length;
		}

		@Override
		public synchronized URI getMirrorLocation(URI inputLocation, IProgressMonitor monitor) {
			return URIUtil.append(nextMirror(), repoLocation.relativize(inputLocation).getPath());
		}

		private URI nextMirror() {
			Field mirrorLocation = null;
			try {
				mirrorLocation = MirrorInfo.class.getDeclaredField("locationString");
				mirrorLocation.setAccessible(true);

				return URIUtil.makeAbsolute(new URI((String) mirrorLocation.get(mirrors[index++])), repoLocation);
			} catch (Exception e) {
				fail(Double.toString(0.4 + index), e);
				return null;
			}
		}

		private synchronized void getRepoLocation() {
			Field locationField = null;
			try {
				locationField = AbstractRepository.class.getDeclaredField("location");
				locationField.setAccessible(true);
				repoLocation = (URI) locationField.get(repo);
			} catch (Exception e) {
				fail("0.3", e);
			}
		}

		private MirrorInfo[] computeMirrors(String mirrorsURL) {
			// Copied & modified from MirrorSelector
			try {
				DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = domFactory.newDocumentBuilder();
				Document document = builder.parse(mirrorsURL);
				if (document == null)
					return null;
				NodeList mirrorNodes = document.getElementsByTagName("mirror"); //$NON-NLS-1$
				int mirrorCount = mirrorNodes.getLength();
				MirrorInfo[] infos = new MirrorInfo[mirrorCount + 1];
				for (int i = 0; i < mirrorCount; i++) {
					Element mirrorNode = (Element) mirrorNodes.item(i);
					String infoURL = mirrorNode.getAttribute("url"); //$NON-NLS-1$
					infos[i] = new MirrorInfo(infoURL, i);
				}
				//p2: add the base site as the last resort mirror so we can track download speed and failure rate
				infos[mirrorCount] = new MirrorInfo(repoLocation.toString(), mirrorCount);
				return infos;
			} catch (Exception e) {
				// log if absolute url
				if (mirrorsURL != null && (mirrorsURL.startsWith("http://") //$NON-NLS-1$
						|| mirrorsURL.startsWith("https://") //$NON-NLS-1$
						|| mirrorsURL.startsWith("file://") //$NON-NLS-1$
						|| mirrorsURL.startsWith("ftp://") //$NON-NLS-1$
						|| mirrorsURL.startsWith("jar://"))) //$NON-NLS-1$
					fail("Error processing mirrors URL: " + mirrorsURL, e); //$NON-NLS-1$
				return null;
			}
		}
	}
}
