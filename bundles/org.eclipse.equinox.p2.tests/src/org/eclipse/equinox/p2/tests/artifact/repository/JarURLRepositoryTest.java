package org.eclipse.equinox.p2.tests.artifact.repository;

import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class JarURLRepositoryTest extends TestCase {

	private ServiceReference managerRef;
	private IArtifactRepositoryManager manager;

	public JarURLRepositoryTest(String name) {
		super(name);
	}

	public JarURLRepositoryTest() {
		super("");
	}

	protected void setUp() throws Exception {
		managerRef = TestActivator.getContext().getServiceReference(IArtifactRepositoryManager.class.getName());
		manager = (IArtifactRepositoryManager) TestActivator.getContext().getService(managerRef);
	}

	protected void tearDown() throws Exception {
		manager = null;
		TestActivator.getContext().ungetService(managerRef);
	}

	public void testJarURLRepository() {
		URL engineJar = TestActivator.getContext().getBundle().getEntry("/testData/enginerepo.jar");
		URL jarRepoURL = null;
		try {
			jarRepoURL = new URL("jar:" + engineJar.toString() + "!/");
		} catch (MalformedURLException e) {
			fail(e.getMessage());
		}
		IArtifactRepository repo = manager.loadRepository(jarRepoURL, null);
		assertTrue(repo.contains(new ArtifactKey("eclipse", "plugin", "org.eclipse.equinox.p2.engine", new Version("0.1.0.200709241631"))));
	}
}
