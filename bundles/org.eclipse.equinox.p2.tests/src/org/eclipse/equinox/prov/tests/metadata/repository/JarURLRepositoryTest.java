package org.eclipse.equinox.prov.tests.metadata.repository;

import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.TestCase;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.prov.tests.TestActivator;
import org.osgi.framework.ServiceReference;

public class JarURLRepositoryTest extends TestCase {

	private ServiceReference managerRef;
	private IMetadataRepositoryManager manager;

	public JarURLRepositoryTest(String name) {
		super(name);
	}

	public JarURLRepositoryTest() {
		super("");
	}

	protected void setUp() throws Exception {
		managerRef = TestActivator.getContext().getServiceReference(IMetadataRepositoryManager.class.getName());
		manager = (IMetadataRepositoryManager) TestActivator.getContext().getService(managerRef);
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
		IMetadataRepository repo = manager.loadRepository(jarRepoURL, null);
		assertTrue(repo.getInstallableUnits(null).length > 0);
	}
}
