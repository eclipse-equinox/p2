package org.eclipse.equinox.p2.tests.artifact.repository.optimizer;

import java.io.*;
import java.net.URL;
import junit.framework.TestCase;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.repositoryoptimizer.Optimizer;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;

public class PackRepositoryTest extends TestCase {
	private ServiceTracker managerTracker;
	private File workDir;

	public PackRepositoryTest(String name) {
		super(name);
	}

	public PackRepositoryTest() {
		super("");
	}

	protected void setUp() throws Exception {
		managerTracker = new ServiceTracker(TestActivator.getContext(), IArtifactRepositoryManager.class.getName(), null);
		managerTracker.open();
		workDir = File.createTempFile("work", "");
		if (!workDir.delete())
			throw new IOException("Could not delete file for creating temporary working dir.");
		if (!workDir.mkdirs())
			throw new IOException("Could not create temporary working dir.");

	}

	protected void tearDown() throws Exception {
		managerTracker.close();
	}

	public void testJarURLRepository() {
		URL repositoryJar = TestActivator.getContext().getBundle().getEntry("/testData/enginerepo");
		URL repositoryURL = extractRepositoryJAR(repositoryJar, workDir);
		assertNotNull("Could not extract repository", repositoryURL);
		IArtifactRepository repository = ((IArtifactRepositoryManager) managerTracker.getService()).loadRepository(repositoryURL, null);
		IArtifactKey key = new ArtifactKey("eclipse", "plugin", "org.eclipse.equinox.prov.engine", new Version("0.1.0.200709241631"));
		IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
		assertTrue("Artifact Descriptor for engine missing", descriptors.length == 1);

		new Optimizer(repository).run();
		descriptors = repository.getArtifactDescriptors(key);
		assertTrue("Optimization was a no-op", descriptors.length == 2);

		IArtifactDescriptor canonical = null;
		IArtifactDescriptor optimized = null;
		for (int i = 0; i < descriptors.length; i++) {
			if (descriptors[i].getProcessingSteps().length == 0)
				canonical = descriptors[i];
			else
				optimized = descriptors[i];
		}

		assertTrue("Optmized descriptor not found", optimized != null);
		assertTrue("Canonical descriptor not found", canonical != null);
		long optimizedSize = Long.parseLong(optimized.getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));
		long canonicalSize = Long.parseLong(canonical.getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));
		assertTrue("Optimzed not smaller than canonical", optimizedSize < canonicalSize);

		File canonicalFile = fetchArtifact("canonical", canonical);
		File optimizedFile = fetchArtifact("optimized", canonical);
		compareFiles(canonicalFile, optimizedFile);
	}

	private URL extractRepositoryJAR(URL source, File destination) {
		String filter = "(protocol=" + source.getProtocol() + ")";
		URLConverter converter = (URLConverter) ServiceHelper.getService(TestActivator.getContext(), URLConverter.class.getName(), filter);
		try {
			return converter == null ? null : converter.toFileURL(source);
		} catch (IOException e) {
			return null;
		}
	}

	private void compareFiles(File canonicalFile, File optimizedFile) {
		assertTrue("Canonical file does not exist", canonicalFile.exists());
		assertTrue("Optimized file does not exist", optimizedFile.exists());
		assertEquals(canonicalFile.length(), optimizedFile.length());
		// TODO compare the actual content
	}

	private File fetchArtifact(String name, IArtifactDescriptor descriptor) {
		try {
			File result = new File(workDir, name);
			OutputStream destination = new BufferedOutputStream(new FileOutputStream(result));
			try {
				descriptor.getRepository().getArtifact(descriptor, destination, new NullProgressMonitor());
				return result;
			} finally {
				if (destination != null)
					destination.close();
			}
		} catch (IOException e) {
			fail("Could not fetch artifact " + descriptor);
		}
		return null;
	}
}
