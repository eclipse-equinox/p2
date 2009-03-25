package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorSelector;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.spi.p2.core.repository.AbstractRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.w3c.dom.*;

public class MirrorRequestTest extends AbstractProvisioningTest {
	private static final String testDataLocation = "testData/artifactRepo/emptyJarRepo";
	File targetLocation;
	IArtifactRepository targetRepository, sourceRepository;

	public void setUp() throws Exception {
		super.setUp();
		targetLocation = File.createTempFile("target", ".repo");
		targetLocation.delete();
		targetLocation.mkdirs();
		targetRepository = new SimpleArtifactRepository("TargetRepo", targetLocation.toURI(), null);

		IArtifactRepositoryManager mgr = getArtifactRepositoryManager();
		sourceRepository = mgr.loadRepository((getTestData("EmptyJar repo", testDataLocation).toURI()), null);

	}

	protected void tearDown() throws Exception {
		AbstractProvisioningTest.delete(targetLocation);
		super.tearDown();
	}

	public void testInvalidZipFileInTheSource() {
		IArtifactKey key = new ArtifactKey("org.eclipse.update.feature", "HelloWorldFeature", new Version(1, 0, 0));
		Properties targetProperties = new Properties();
		targetProperties.put("artifact.folder", "true");
		MirrorRequest request = new MirrorRequest(key, targetRepository, null, targetProperties);
		request.setSourceRepository(sourceRepository);

		request.perform(new NullProgressMonitor());

		assertTrue(request.getResult().matches(IStatus.ERROR));
		assertTrue(request.getResult().getException() instanceof IOException);
	}

	public void testMissingArtifact() {
		IArtifactKey key = new ArtifactKey("org.eclipse.update.feature", "Missing", new Version(1, 0, 0));
		Properties targetProperties = new Properties();
		targetProperties.put("artifact.folder", "true");
		MirrorRequest request = new MirrorRequest(key, targetRepository, null, targetProperties);
		request.setSourceRepository(sourceRepository);

		request.perform(new NullProgressMonitor());

		assertTrue(request.getResult().matches(IStatus.ERROR));
	}

	// Test that SimpleArtifactRepository & MirrorRequest use mirrors in the event of a failure. 
	public void testMirrorFailOver() {
		OrderedMirrorSelector selector = new OrderedMirrorSelector(sourceRepository);
		// Hijack the source repository's MirrorSelector with ours which provides mirrors in order.
		Field mirrors = null;
		try {
			mirrors = SimpleArtifactRepository.class.getDeclaredField("mirrors");
			mirrors.setAccessible(true);
			mirrors.set(sourceRepository, selector);
		} catch (Exception e) {
			fail("0.2", e);
		}
		// call test
		IArtifactKey key = new ArtifactKey("test.txt", "HelloWorldText", new Version("1.0.0"));
		MirrorRequest request = new MirrorRequest(key, targetRepository, null, null);
		request.setSourceRepository(sourceRepository);
		request.perform(new NullProgressMonitor());

		// The download succeeded
		assertTrue(request.getResult().toString(), request.getResult().isOK());
		// All available mirrors used
		assertTrue("All mirrors utilized", selector.index == selector.mirrors.length);
	}

	/*
	 * Special mirror selector for testing which chooses mirrors in order
	 */
	protected class OrderedMirrorSelector extends MirrorSelector {
		private URI repoLocation;
		int index = 0;
		MirrorInfo[] mirrors;
		IArtifactRepository repo;

		OrderedMirrorSelector(IArtifactRepository repo) {
			super(repo);
			this.repo = repo;
			// Setting this property forces SimpleArtifactRepository to use mirrors despite being a local repo
			// Alternatively we could use reflect to change "location" of the repo
			repo.setProperty(SimpleArtifactRepository.PROP_FORCE_THREADING, String.valueOf(true));
			getRepoLocation();
			mirrors = computeMirrors("file:///" + getTestData("Mirror Location", testDataLocation + '/' + repo.getProperties().get(IRepository.PROP_MIRRORS_URL)).toString().replace('\\', '/'));
		}

		// Overridden to prevent mirror sorting
		public synchronized void reportResult(String toDownload, IStatus result) {
			return;
		}

		// We want to test each mirror once.
		public synchronized boolean hasValidMirror() {
			return mirrors != null && index < mirrors.length;
		}

		public synchronized URI getMirrorLocation(URI inputLocation) {
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
