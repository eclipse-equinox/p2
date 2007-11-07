package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.TestCase;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.metadata.generator.EclipseInstallGeneratorInfoProvider;
import org.eclipse.equinox.p2.metadata.generator.Generator;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;

public class JarURLRepositoryTest extends TestCase {

	private ServiceReference managerRef;
	private IMetadataRepositoryManager manager;
	private File testRepoJar;

	public JarURLRepositoryTest(String name) {
		super(name);
	}

	public JarURLRepositoryTest() {
		this("");
	}

	private static boolean deleteDirectory(File directory) {
		if (directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return directory.delete();
	}

	protected void setUp() throws Exception {
		managerRef = TestActivator.getContext().getServiceReference(IMetadataRepositoryManager.class.getName());
		manager = (IMetadataRepositoryManager) TestActivator.getContext().getService(managerRef);

		EclipseInstallGeneratorInfoProvider provider = new EclipseInstallGeneratorInfoProvider();
		URL base = TestActivator.getContext().getBundle().getEntry("/testData/generator/eclipse3.3");
		provider.initialize(new File(FileLocator.toFileURL(base).getPath()));
		String tempDir = System.getProperty("java.io.tmpdir");
		File testRepo = new File(tempDir, "testRepo");
		deleteDirectory(testRepo);
		testRepo.mkdir();
		provider.setFlavor("jartest");
		provider.setMetadataRepository(manager.createRepository(testRepo.toURL(), "testRepo", "org.eclipse.equinox.p2.metadata.repository.simpleRepository"));
		IStatus result = new Generator(provider).generate();
		FileUtils.zip(new File[] {testRepo}, new File(tempDir, "testRepo.jar"));
		testRepoJar = new File(tempDir, "testRepo.jar");
		assertTrue(testRepoJar.exists());
		testRepoJar.deleteOnExit();
		deleteDirectory(testRepo);
	}

	protected void tearDown() throws Exception {
		manager = null;
		TestActivator.getContext().ungetService(managerRef);
	}

	public void testJarURLRepository() {
		URL jarRepoURL = null;
		try {
			jarRepoURL = new URL("jar:" + testRepoJar.toURL().toString() + "!/testRepo/");
		} catch (MalformedURLException e) {
			fail(e.getMessage());
		}
		IMetadataRepository repo = manager.loadRepository(jarRepoURL, null);
		assertTrue(repo.getInstallableUnits(null).length > 0);
		manager.removeRepository(repo);
	}
}
