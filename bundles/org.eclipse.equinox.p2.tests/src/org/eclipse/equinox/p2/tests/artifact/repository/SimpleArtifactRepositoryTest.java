/*******************************************************************************
 * Copyright (c) 2007, 2017 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *		compeople AG (Stefan Liebig) - initial API and implementation
 *		Code 9 - ongoing development
 *		IBM - ongoing development
 *		Sonatype Inc - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.ArtifactDescriptorQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class SimpleArtifactRepositoryTest extends AbstractProvisioningTest {
	//artifact repository to remove on tear down
	private File repositoryFile = null;
	private URI repositoryURI = null;

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		//repository location is not used by all tests
		if (repositoryURI != null) {
			getArtifactRepositoryManager().removeRepository(repositoryURI);
			repositoryURI = null;
		}
		if (repositoryFile != null) {
			delete(repositoryFile);
			repositoryFile = null;
		}
	}

	public void testGetActualLocation1() throws Exception {
		URI base = new URI("http://localhost/artifactRepository");
		assertEquals(new URI(base + "/artifacts.xml"), SimpleArtifactRepository.getActualLocation(base, false));
	}

	public void testGetActualLocation2() throws Exception {
		URI base = new URI("http://localhost/artifactRepository/");
		assertEquals(new URI(base + "artifacts.xml"), SimpleArtifactRepository.getActualLocation(base, false));
	}

	public void testGetActualLocationCompressed1() throws Exception {
		URI base = new URI("http://localhost/artifactRepository");
		assertEquals(new URI(base + "/artifacts.jar"), SimpleArtifactRepository.getActualLocation(base, true));
	}

	public void testGetActualLocationCompressed2() throws Exception {
		URI base = new URI("http://localhost/artifactRepository/");
		assertEquals(new URI(base + "artifacts.jar"), SimpleArtifactRepository.getActualLocation(base, true));
	}

	public void testCompressedRepository() throws ProvisionException {
		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		String tempDir = System.getProperty("java.io.tmpdir");
		repositoryFile = new File(tempDir, "SimpleArtifactRepositoryTest");
		delete(repositoryFile);
		repositoryURI = repositoryFile.toURI();
		Map<String, String> properties = new HashMap<>();
		properties.put(IRepository.PROP_COMPRESSED, "true");
		IArtifactRepository repo = artifactRepositoryManager.createRepository(repositoryURI, "artifact name", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);

		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, null);
		repo.addDescriptor(descriptor, new NullProgressMonitor());

		File files[] = repositoryFile.listFiles();
		boolean jarFilePresent = false;
		boolean artifactFilePresent = false;
		for (File file : files) {
			if ("artifacts.jar".equalsIgnoreCase(file.getName())) {
				jarFilePresent = true;
			}
			if ("artifacts.xml".equalsIgnoreCase(file.getName())) {
				artifactFilePresent = false;
			}
		}
		delete(repositoryFile);

		if (!jarFilePresent)
			fail("Repository should create JAR for artifact.xml");
		if (artifactFilePresent)
			fail("Repository should not create artifact.xml");
	}

	public void testUncompressedRepository() throws ProvisionException {
		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		String tempDir = System.getProperty("java.io.tmpdir");
		repositoryFile = new File(tempDir, "SimpleArtifactRepositoryTest");
		delete(repositoryFile);
		repositoryURI = repositoryFile.toURI();
		Map<String, String> properties = new HashMap<>();
		properties.put(IRepository.PROP_COMPRESSED, "false");
		IArtifactRepository repo = artifactRepositoryManager.createRepository(repositoryURI, "artifact name", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);

		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, null);
		repo.addDescriptor(descriptor, new NullProgressMonitor());

		File files[] = repositoryFile.listFiles();
		boolean jarFilePresent = false;
		boolean artifactFilePresent = false;
		for (File file : files) {
			if ("artifacts.jar".equalsIgnoreCase(file.getName())) {
				jarFilePresent = true;
			}
			if ("artifacts.xml".equalsIgnoreCase(file.getName())) {
				artifactFilePresent = true;
			}
		}
		delete(repositoryFile);

		if (jarFilePresent)
			fail("Repository should not create JAR for artifact.xml");
		if (!artifactFilePresent)
			fail("Repository should create artifact.xml");
	}

	public void testLoadInvalidLocation() {
		try {
			getArtifactRepositoryManager().loadRepository(new URI("file:d:/foo"), getMonitor());
		} catch (ProvisionException e) {
			//expected
		} catch (URISyntaxException e) {
			fail("4.99", e);
		}
	}

	public void test_248772() {
		SimpleArtifactRepositoryFactory factory = new SimpleArtifactRepositoryFactory();
		factory.setAgent(getAgent());
		URI location = null;
		location = new File(getTempFolder(), getUniqueString()).toURI();
		factory.create(location, "test type", null, null);
		try {
			//bug 248951, ask for a modifiable repo
			IArtifactRepository repo = factory.load(location, IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, new NullProgressMonitor());
			assertNotNull(repo);
			assertTrue(repo.isModifiable());
		} catch (ProvisionException e) {
			fail("2.0", e);
		}
	}

	public void testErrorStatus() {
		class TestStep extends ProcessingStep {
			IStatus myStatus;

			public TestStep(IStatus status) {
				this.myStatus = status;
			}

			@Override
			public void close() throws IOException {
				setStatus(myStatus);
				super.close();
			}
		}
		repositoryURI = getTestData("Loading test data", "testData/artifactRepo/simple").toURI();
		repositoryFile = new File(getTempFolder(), getUniqueString());

		IArtifactRepository repo = null;
		try {
			repo = getArtifactRepositoryManager().loadRepository(repositoryURI, new NullProgressMonitor());
		} catch (ProvisionException e) {
			fail("Failed to create repository", e);
		}
		IArtifactDescriptor descriptor = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "aaPlugin", Version.create("1.0.0")));

		OutputStream out = null;
		try {
			TestStep errStep = new TestStep(new Status(IStatus.ERROR, "plugin", "Error Step Message"));
			TestStep warnStep = new TestStep(new Status(IStatus.WARNING, "plugin", "Warning Step Message"));
			TestStep okStep = new TestStep(Status.OK_STATUS);
			out = new FileOutputStream(repositoryFile);
			(new ProcessingStepHandler()).link(new ProcessingStep[] {okStep, errStep, warnStep}, out, new NullProgressMonitor());
			IStatus status = repo.getRawArtifact(descriptor, okStep, new NullProgressMonitor());
			out.close();

			// Only the error step should be collected
			assertFalse(status.isOK());
			assertTrue("Unexpected Severity", status.matches(IStatus.ERROR));
			assertEquals(1, status.getChildren().length);

			errStep = new TestStep(new Status(IStatus.ERROR, "plugin", "Error Step Message"));
			warnStep = new TestStep(new Status(IStatus.WARNING, "plugin", "Warning Step Message"));
			TestStep warnStep2 = new TestStep(new Status(IStatus.WARNING, "plugin", "2 - Warning Step Message"));
			okStep = new TestStep(Status.OK_STATUS);
			out = new FileOutputStream(repositoryFile);
			(new ProcessingStepHandler()).link(new ProcessingStep[] {okStep, warnStep, errStep, warnStep2}, out, new NullProgressMonitor());
			status = repo.getRawArtifact(descriptor, okStep, new NullProgressMonitor());
			out.close();

			// The first warning step and the error step should be collected
			assertFalse(status.isOK());
			assertTrue("Unexpected Severity", status.matches(IStatus.ERROR));
			assertEquals(2, status.getChildren().length);

		} catch (IOException e) {
			fail("Failed to create ouptut stream", e);
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					// don't care
				}
		}

	}

	public void testRelativeRepositoryLocation() throws ProvisionException {
		IArtifactDescriptor descriptor = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "helloworld", Version.createOSGi(1, 0, 0)));
		URI repo = getTestData("CorruptedJar repo", "testData/artifactRepo/jarfiles").toURI();

		SimpleArtifactRepository absRepo = (SimpleArtifactRepository) getArtifactRepositoryManager().loadRepository(repo, new NullProgressMonitor());
		URI absLocation = absRepo.getLocation(descriptor);
		getArtifactRepositoryManager().removeRepository(repo);

		repo = new File(System.getProperty("user.dir")).toURI().relativize(repo);
		// handle case where we had file:file: URIs (https://bugs.eclipse.org/334904)
		if (!"file".equals(repo.getScheme()))
			repo = URI.create("file:" + repo.toString());

		SimpleArtifactRepository repository = (SimpleArtifactRepository) getArtifactRepositoryManager().loadRepository(repo, new NullProgressMonitor());

		URI location = repository.getLocation(descriptor);
		assertNotNull("NULL Scheme", location.getScheme());
		assertTrue("File location is relative", location.isAbsolute());
		assertEquals("Path from relative & absolute repos differ", absLocation, location);
	}

	private static class TestDescriptor implements IArtifactDescriptor {
		private static final IProcessingStepDescriptor[] steps = new IProcessingStepDescriptor[0];
		private IArtifactKey artifactKey;
		private Map<String, String> properties = new HashMap<>();

		public TestDescriptor(IArtifactKey key) {
			this.artifactKey = key;
		}

		@Override
		public IArtifactKey getArtifactKey() {
			return artifactKey;
		}

		@Override
		public IProcessingStepDescriptor[] getProcessingSteps() {
			return steps;
		}

		@Override
		public Map<String, String> getProperties() {
			return properties;
		}

		@Override
		public String getProperty(String key) {
			return properties.get(key);
		}

		@Override
		public IArtifactRepository getRepository() {
			return null;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof IArtifactDescriptor))
				return false;

			IArtifactDescriptor other = (IArtifactDescriptor) obj;
			if (!artifactKey.equals(other.getArtifactKey()))
				return false;

			if (!Arrays.equals(steps, other.getProcessingSteps()))
				return false;

			String format = getProperty(FORMAT);
			String otherFormat = other.getProperty(FORMAT);
			if (format != null ? !format.equals(otherFormat) : otherFormat != null)
				return false;

			return true;
		}

		@Override
		public int hashCode() {
			String format = getProperty(FORMAT);

			final int prime = 31;
			int result = 1;
			result = prime * result + ((artifactKey == null) ? 0 : artifactKey.hashCode());
			result = prime * result + Arrays.asList(steps).hashCode();
			result = prime * result + (format != null ? format.hashCode() : 0);
			return result;
		}
	}

	public void testAddContains() throws Exception {
		File folder = getTestFolder("simple_AddContains");
		repositoryURI = folder.toURI();

		Map<String, String> properties = new HashMap<>();
		SimpleArtifactRepository repo = (SimpleArtifactRepository) getArtifactRepositoryManager().createRepository(repositoryURI, "My Repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);

		TestDescriptor descriptor = new TestDescriptor(new ArtifactKey("osgi.bundle", "aaPlugin", Version.create("1.0.0")));
		try (OutputStream stream = repo.getOutputStream(descriptor)) {
			stream.write("I am an artifact\n".getBytes());
		}

		assertTrue(repo.contains(descriptor));

		assertTrue(repo.contains(new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "aaPlugin", Version.create("1.0.0")))));

	}

	public void _testAddDescriptorPerformance() throws Exception {
		File folder = getTestFolder("ArtifactRepository_testAddDescriptorPerformance");
		repositoryURI = folder.toURI();

		IArtifactRepository repo = getArtifactRepositoryManager().createRepository(repositoryURI, "test", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, new HashMap<>());

		long start = System.currentTimeMillis();
		IProgressMonitor monitor = new NullProgressMonitor();
		for (int i = 0; i < 10000; i++) {
			ArtifactDescriptor d = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "a" + i, Version.create("1.0.0")));
			repo.addDescriptor(d, monitor);
		}
		long end = System.currentTimeMillis();
		System.out.println("Total time: " + (end - start));
	}

	public void _testAddDescriptorPerformanceExecuteBatch() throws Exception {
		File folder = getTestFolder("ArtifactRepository_testAddDescriptorPerformanceExectuteBatch");
		repositoryURI = folder.toURI();

		final IArtifactRepository repo = getArtifactRepositoryManager().createRepository(repositoryURI, "test", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, new HashMap<>());

		long start = System.currentTimeMillis();
		repo.executeBatch(monitor -> {
			for (int i = 0; i < 10000; i++) {
				ArtifactDescriptor d = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "a" + i, Version.create("1.0.0")));
				repo.addDescriptor(d, monitor);
			}
		}, new NullProgressMonitor());

		long end = System.currentTimeMillis();
		System.out.println("Total time: " + (end - start));
	}

	@SuppressWarnings("removal")
	public void testQuery() throws Exception {
		File folder = getTestFolder("ArtifactRepository_testQuery");
		repositoryURI = folder.toURI();

		IArtifactRepository repo = getArtifactRepositoryManager().createRepository(repositoryURI, "test", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, new HashMap<>());

		ArtifactDescriptor d1 = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "a", Version.create("1.0.0")));
		ArtifactDescriptor d2 = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "a", Version.create("2.0.0")));
		ArtifactDescriptor d3 = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "a", Version.create("2.0.0")));
		d3.setProperty(IArtifactDescriptor.FORMAT, IArtifactDescriptor.FORMAT_PACKED);
		IProgressMonitor monitor = new NullProgressMonitor();
		repo.addDescriptor(d1, monitor);
		repo.addDescriptor(d2, monitor);
		repo.addDescriptor(d3, monitor);

		IQueryable<IArtifactDescriptor> descQueryable = repo.descriptorQueryable();
		IQueryResult<IArtifactDescriptor> result = descQueryable.query(new ArtifactDescriptorQuery("a", null, null), null);
		assertEquals(3, queryResultSize(result));

		result = descQueryable.query(new ArtifactDescriptorQuery("a", new VersionRange("[2.0.0, 3.0.0)"), null), null);
		assertEquals(2, queryResultSize(result));
		assertNotContains(result, d1);

		result = descQueryable.query(new ArtifactDescriptorQuery("a", null, IArtifactDescriptor.FORMAT_PACKED), null);
		assertEquals(1, queryResultSize(result));
		IArtifactDescriptor resultDescriptor = result.iterator().next();
		assertEquals(d3.getArtifactKey(), resultDescriptor.getArtifactKey());
	}

	/*
	 * Tests the number of threads allowed
	 */
	public void testMaximumThreads() throws Exception {
		File folder = getTestFolder("ArtifactRepository_testQuery");
		repositoryURI = folder.toURI();
		IArtifactRepository repo = getArtifactRepositoryManager().createRepository(repositoryURI, "test", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, new HashMap<>());

		Method getMaximumThreads = SimpleArtifactRepository.class.getDeclaredMethod("getMaximumThreads");
		getMaximumThreads.setAccessible(true);

		Field defaultMaxThreadsField = SimpleArtifactRepository.class.getDeclaredField("DEFAULT_MAX_THREADS");
		defaultMaxThreadsField.setAccessible(true);

		int defaultMaxThreads = defaultMaxThreadsField.getInt(repo);

		assertEquals("Default setting", defaultMaxThreads, getIntVal(getMaximumThreads, repo));

		// Legitimate user value
		System.setProperty(SimpleArtifactRepository.PROP_MAX_THREADS, "2");
		assertEquals("Valid User setting", 2, getIntVal(getMaximumThreads, repo));

		// User value is high, but repo specifies no limit, so user value is used
		// directly.
		System.setProperty(SimpleArtifactRepository.PROP_MAX_THREADS, "22");
		assertEquals("Invalid User setting", 22, getIntVal(getMaximumThreads, repo));
		System.clearProperty(SimpleArtifactRepository.PROP_MAX_THREADS);

		// Legitimate repo value
		repo.setProperty(SimpleArtifactRepository.PROP_MAX_THREADS, "4");
		assertEquals("Valid repository specified setting", 4, getIntVal(getMaximumThreads, repo));

		// Legitimate big repo value, but user default when not specified at all is 4.
		repo.setProperty(SimpleArtifactRepository.PROP_MAX_THREADS, "8");
		assertEquals("Valid repository specified setting", 4, getIntVal(getMaximumThreads, repo));

		// User value is lower should take precedence
		repo.setProperty(SimpleArtifactRepository.PROP_MAX_THREADS, "3");
		System.setProperty(SimpleArtifactRepository.PROP_MAX_THREADS, "2");
		assertEquals("User setting should take precedence", 2, getIntVal(getMaximumThreads, repo));

		// User value is big but lower should take precedence
		repo.setProperty(SimpleArtifactRepository.PROP_MAX_THREADS, "10");
		System.setProperty(SimpleArtifactRepository.PROP_MAX_THREADS, "8");
		assertEquals("User setting should take precedence", 8, getIntVal(getMaximumThreads, repo));

		// Repo value is lower should take precedence
		repo.setProperty(SimpleArtifactRepository.PROP_MAX_THREADS, "2");
		System.setProperty(SimpleArtifactRepository.PROP_MAX_THREADS, "3");
		assertEquals("User setting should take precedence", 2, getIntVal(getMaximumThreads, repo));

		// Repo value is big but lower should take precedence
		repo.setProperty(SimpleArtifactRepository.PROP_MAX_THREADS, "8");
		System.setProperty(SimpleArtifactRepository.PROP_MAX_THREADS, "10");
		assertEquals("User setting should take precedence", 8, getIntVal(getMaximumThreads, repo));
	}

	private int getIntVal(Method m, Object repo) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		return ((Integer) m.invoke(repo)).intValue();
	}
}
