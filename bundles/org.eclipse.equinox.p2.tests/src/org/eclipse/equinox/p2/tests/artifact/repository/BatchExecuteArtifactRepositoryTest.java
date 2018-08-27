/*******************************************************************************
* Copyright (c) 2009, 2017 EclipseSource and others.
 *
 * This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
* available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * This tests Batch Execution for Artifact Repositories
 */
public class BatchExecuteArtifactRepositoryTest extends AbstractProvisioningTest {

	//artifact repository to remove on tear down
	private File repositoryFile = null;
	private URI repositoryURI = null;

	private IArtifactDescriptor createDescriptor(String classifier, String id, Version version) {
		return new SimpleArtifactDescriptor(new ArtifactKey(classifier, id, version));
	}

	/*
	 * This tests that in the normal case, the batch process runs fine
	 */
	public void testAdd() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			Map<String, String> properties = new HashMap<>();
			final SimpleArtifactRepository repo = (SimpleArtifactRepository) getArtifactRepositoryManager().createRepository(repositoryURI, "My Repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			final URI artifactXML = new URI(repositoryFile.toURI().toString() + "/artifacts.xml");
			IStatus status = repo.executeBatch(monitor -> {
				repo.addDescriptor(createDescriptor("foo", "foo", Version.emptyVersion), monitor);
				try {
					assertFalse("1.0", fileContainsString(artifactXML, "foo"));
				} catch (IOException e) {
					fail("0.99");
				}
			}, new NullProgressMonitor());
			assertTrue(status.isOK());
			assertTrue("1.0", fileContainsString(artifactXML, "foo"));
			assertEquals("2.0", 1, repo.query(new ArtifactKeyQuery("foo", "foo", null), new NullProgressMonitor()).toSet().size());
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This tests 3 adds
	 */
	public void testMultiAdd() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			Map<String, String> properties = new HashMap<>();
			final SimpleArtifactRepository repo = (SimpleArtifactRepository) getArtifactRepositoryManager().createRepository(repositoryURI, "My Repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			final URI artifactXML = new URI(repositoryFile.toURI().toString() + "/artifacts.xml");
			IStatus status = repo.executeBatch(monitor -> {
				repo.addDescriptor(createDescriptor("foo", "foo", Version.emptyVersion), monitor);
				repo.addDescriptor(createDescriptor("bar", "bar", Version.emptyVersion), monitor);
				repo.addDescriptor(createDescriptor("baz", "baz", Version.emptyVersion), monitor);
				try {
					assertFalse("1.0", fileContainsString(artifactXML, "foo"));
					assertFalse("1.0", fileContainsString(artifactXML, "bar"));
					assertFalse("1.0", fileContainsString(artifactXML, "baz"));
				} catch (IOException e) {
					fail("0.99");
				}
			}, new NullProgressMonitor());
			assertTrue(status.isOK());
			assertEquals("1.0", 1, repo.query(new ArtifactKeyQuery("foo", "foo", null), new NullProgressMonitor()).toSet().size());
			assertEquals("1.1", 1, repo.query(new ArtifactKeyQuery("bar", "bar", null), new NullProgressMonitor()).toSet().size());
			assertEquals("1.2", 1, repo.query(new ArtifactKeyQuery("baz", "baz", null), new NullProgressMonitor()).toSet().size());
			assertTrue("2.0", fileContainsString(artifactXML, "foo"));
			assertTrue("2.1", fileContainsString(artifactXML, "bar"));
			assertTrue("2.2", fileContainsString(artifactXML, "baz"));
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This tests multiple adds with an exception thrown. Makes sure that the descriptors are added
	 */
	public void testMultiAddWithException() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			Map<String, String> properties = new HashMap<>();
			final SimpleArtifactRepository repo = (SimpleArtifactRepository) getArtifactRepositoryManager().createRepository(repositoryURI, "My Repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			final URI artifactXML = new URI(repositoryFile.toURI().toString() + "/artifacts.xml");
			IStatus status = repo.executeBatch(monitor -> {
				repo.addDescriptor(createDescriptor("foo", "foo", Version.emptyVersion), monitor);
				repo.addDescriptor(createDescriptor("bar", "bar", Version.emptyVersion), monitor);
				throw new RuntimeException();
			}, new NullProgressMonitor());
			assertFalse(status.isOK());
			assertEquals("1.0", 1, repo.query(new ArtifactKeyQuery("foo", "foo", null), new NullProgressMonitor()).toSet().size());
			assertEquals("1.1", 1, repo.query(new ArtifactKeyQuery("bar", "bar", null), new NullProgressMonitor()).toSet().size());
			assertTrue("2.0", fileContainsString(artifactXML, "foo"));
			assertTrue("2.1", fileContainsString(artifactXML, "bar"));
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This tests multiple adds with an exception thrown. Makes sure that the descriptors are added
	 */
	public void testAddAndRemove() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			Map<String, String> properties = new HashMap<>();
			final SimpleArtifactRepository repo = (SimpleArtifactRepository) getArtifactRepositoryManager().createRepository(repositoryURI, "My Repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			final URI artifactXML = new URI(repositoryFile.toURI().toString() + "/artifacts.xml");
			IStatus status = repo.executeBatch(monitor -> {
				IArtifactDescriptor foo = createDescriptor("foo", "foo", Version.emptyVersion);
				repo.addDescriptor(foo, monitor);
				repo.addDescriptor(createDescriptor("bar", "bar", Version.emptyVersion), monitor);
				repo.removeDescriptor(foo, monitor);
			}, new NullProgressMonitor());
			assertTrue(status.isOK());
			assertEquals("1.0", 1, repo.query(new ArtifactKeyQuery("bar", "bar", null), new NullProgressMonitor()).toSet().size());
			assertEquals("1.1", 0, repo.query(new ArtifactKeyQuery("foo", "foo", null), new NullProgressMonitor()).toSet().size());
			assertFalse("2.0", fileContainsString(artifactXML, "foo"));
			assertTrue("2.1", fileContainsString(artifactXML, "bar"));
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This tests multiple adds with an exception thrown. Makes sure that the descriptors are added
	 */
	public void testMultiAddAndRemove() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			Map<String, String> properties = new HashMap<>();
			final SimpleArtifactRepository repo = (SimpleArtifactRepository) getArtifactRepositoryManager().createRepository(repositoryURI, "My Repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			final URI artifactXML = new URI(repositoryFile.toURI().toString() + "/artifacts.xml");
			IStatus status = repo.executeBatch(monitor -> {
				repo.addDescriptor(createDescriptor("1", "1", Version.emptyVersion), monitor);
				repo.addDescriptor(createDescriptor("2", "2", Version.emptyVersion), monitor);
				repo.addDescriptor(createDescriptor("3", "3", Version.emptyVersion), monitor);
				IArtifactDescriptor foo = createDescriptor("foo", "foo", Version.emptyVersion);
				repo.addDescriptor(foo, monitor);
				repo.addDescriptor(createDescriptor("bar", "bar", Version.emptyVersion), monitor);
				repo.removeDescriptor(foo, monitor);
			}, new NullProgressMonitor());
			assertTrue(status.isOK());
			assertEquals("1.0", 4, repo.query(new ArtifactKeyQuery(null, null, null), new NullProgressMonitor()).toSet().size());
			assertFalse("2.0", fileContainsString(artifactXML, "foo"));
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	boolean fileContainsString(URI location, String string) throws IOException {
		StringBuffer buffer = new StringBuffer();
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(location)))) {

			while (reader.ready())
				buffer.append(reader.readLine());
			return buffer.toString().contains(string);
		}
	}

	/*
	 * This tests that in the normal case, the batch process runs fine
	 */
	public void testBatchProcessingOK() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			Map<String, String> properties = new HashMap<>();
			SimpleArtifactRepository repo = (SimpleArtifactRepository) getArtifactRepositoryManager().createRepository(repositoryURI, "My Repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			IStatus status = repo.executeBatch(monitor -> {
				// empty
			}, new NullProgressMonitor());
			assertTrue(status.isOK());
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	class FailingSimpleArtifactRepository extends SimpleArtifactRepository {
		boolean executeBatch = false;

		/**
		 * @param repositoryName
		 * @param location
		 * @param properties
		 */
		public FailingSimpleArtifactRepository(String repositoryName, URI location, Map<String, String> properties) {
			super(getAgent(), repositoryName, location, properties);
		}

		@Override
		public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
			executeBatch = true;
			return super.executeBatch(runnable, monitor);
		}

		@Override
		public void save() {
			if (executeBatch)
				throw new RuntimeException("foo");
		}
	}

	class FailingCompositeArtifactRepository extends CompositeArtifactRepository {
		boolean executeBatch = false;

		/**
		 * @param repositoryName
		 * @param location
		 * @param properties
		 */
		public FailingCompositeArtifactRepository(IArtifactRepositoryManager manager, String repositoryName, URI location, Map<String, String> properties) {
			super(manager, location, repositoryName, properties);
		}

		@Override
		public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
			executeBatch = true;
			return super.executeBatch(runnable, monitor);
		}

		@Override
		public void save() {
			if (executeBatch)
				throw new RuntimeException("foo");
		}
	}

	/*
	 * This tests that exceptions are properly propagated for a SimpleArtifactRepository
	 */
	public void testBatchProcessingExceptionsSimple() {
		try {
			SimpleArtifactRepository simpleArtifactRepository = new FailingSimpleArtifactRepository("foo", new URI("http://foo.bar"), null);

			IStatus status = simpleArtifactRepository.executeBatch(monitor -> {
				throw new RuntimeException("bar");
			}, new NullProgressMonitor());
			assertFalse(status.isOK());
			assertEquals("foo", status.getException().getMessage());
			assertEquals(1, status.getChildren().length);
			assertEquals("bar", status.getChildren()[0].getMessage());
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This tests that exceptions are properly propagated for a SimpleArtifactRepository
	 */
	public void testBatchProcessingSaveExceptionSimple() {
		try {
			SimpleArtifactRepository simpleArtifactRepository = new FailingSimpleArtifactRepository("foo", new URI("http://foo.bar"), null);

			IStatus status = simpleArtifactRepository.executeBatch(monitor -> {
				// empty
			}, new NullProgressMonitor());
			assertFalse(status.isOK());
			assertEquals("foo", status.getException().getMessage());
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This tests that exceptions are properly propagated for a CompositeRepository
	 */
	public void testBatchProcessingCancelled() {
		try {
			Map<String, String> properties = new HashMap<>();
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			SimpleArtifactRepository repo = (SimpleArtifactRepository) getArtifactRepositoryManager().createRepository(repositoryURI, "My Repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			IProgressMonitor monitor = new NullProgressMonitor();
			monitor.setCanceled(true);

			IStatus status = repo.executeBatch(monitor1 -> {
				if (monitor1.isCanceled())
					throw new OperationCanceledException();
			}, monitor);

			assertTrue(status.getSeverity() == IStatus.CANCEL);
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This tests that exceptions are properly propagated for a CompositeRepository
	 */
	public void testBatchProcessingExceptionsComposite() {
		try {
			FailingCompositeArtifactRepository compositeArtifactRepository = new FailingCompositeArtifactRepository(getArtifactRepositoryManager(), "foo", new URI("http://foo.bar"), null);

			IStatus status = compositeArtifactRepository.executeBatch(monitor -> {
				throw new RuntimeException("bar");
			}, new NullProgressMonitor());
			assertFalse(status.isOK());
			assertEquals("foo", status.getException().getMessage());
			assertEquals(1, status.getChildren().length);
			assertEquals("bar", status.getChildren()[0].getMessage());
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This tests that exceptions are properly propagated for a CompositeRepository
	 */
	public void testBatchProcessingSaveExceptionComposite() {
		try {
			FailingCompositeArtifactRepository compositeArtifactRepository = new FailingCompositeArtifactRepository(getArtifactRepositoryManager(), "foo", new URI("http://foo.bar"), null);

			IStatus status = compositeArtifactRepository.executeBatch(monitor -> {
				// empty
			}, new NullProgressMonitor());
			assertFalse(status.isOK());
			assertEquals("foo", status.getException().getMessage());
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	class TrackSavignSimpleArtifactRepository extends SimpleArtifactRepository {
		boolean executeBatch = false;
		public boolean didSave = false;

		/**
		 * @param repositoryName
		 * @param location
		 * @param properties
		 */
		public TrackSavignSimpleArtifactRepository(String repositoryName, URI location, Map<String, String> properties) {
			super(getAgent(), repositoryName, location, properties);
		}

		@Override
		public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
			executeBatch = true;
			return super.executeBatch(runnable, monitor);
		}

		@Override
		public void save() {
			if (executeBatch)
				didSave = true;
		}
	}

	/*
	 * This test ensure that a repository did in fact save the results,
	 * if there was no exception
	 */
	public void testBatchProcessingTrackSaving() {
		try {
			TrackSavignSimpleArtifactRepository simpleArtifactRepository = new TrackSavignSimpleArtifactRepository("foo", new URI("http://foo.bar"), null);
			simpleArtifactRepository.executeBatch(monitor -> {
				//do nothing;
			}, new NullProgressMonitor());
			assertTrue(simpleArtifactRepository.didSave);
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This test ensures that a repository did in fact save the results, even
	 * if there was an exception
	 */
	public void testBatchProcessingTrackSavingException() {
		try {
			TrackSavignSimpleArtifactRepository simpleArtifactRepository = new TrackSavignSimpleArtifactRepository("foo", new URI("http://foo.bar"), null);
			simpleArtifactRepository.executeBatch(monitor -> {
				throw new RuntimeException();
			}, new NullProgressMonitor());
			assertTrue(simpleArtifactRepository.didSave);
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This test ensures that the simple repository resets the disableSave flag
	 * even if there is an exception
	 */
	public void testDisableSaveFlagResetSimple() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			Map<String, String> properties = new HashMap<>();
			SimpleArtifactRepository repo = (SimpleArtifactRepository) getArtifactRepositoryManager().createRepository(repositoryURI, "My Repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			repo.executeBatch(monitor -> {
				throw new RuntimeException();
			}, new NullProgressMonitor());
			Field field = SimpleArtifactRepository.class.getDeclaredField("disableSave");
			field.setAccessible(true);
			boolean disableSave = field.getBoolean(repo);
			assertFalse("1.0", disableSave);

		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This test ensure that the simple artifact repository disables the
	 * save flag during the batch process
	 */
	public void testDisableSaveFlagDuringExecutionSimple() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			Map<String, String> properties = new HashMap<>();
			final SimpleArtifactRepository repo = (SimpleArtifactRepository) getArtifactRepositoryManager().createRepository(repositoryURI, "My Repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			repo.executeBatch(monitor -> {
				Field field;
				try {
					field = SimpleArtifactRepository.class.getDeclaredField("disableSave");
					field.setAccessible(true);
					boolean disableSave = field.getBoolean(repo);
					assertTrue("1.0", disableSave);
				} catch (SecurityException e1) {
					fail("1.1" + e1.getMessage());
				} catch (NoSuchFieldException e2) {
					// TODO Auto-generated catch block
					fail("1.2" + e2.getMessage());
				} catch (IllegalArgumentException e3) {
					fail("1.2" + e3.getMessage());
				} catch (IllegalAccessException e4) {
					fail("1.2" + e4.getMessage());
				}
			}, new NullProgressMonitor());

		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/**
	 * This tests ensure that the composite repository sets the disableSave flag
	 * back, even if there is an exception
	 */
	public void testDisableSaveFlagResetComposite() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			Map<String, String> properties = new HashMap<>();
			CompositeArtifactRepository repo = (CompositeArtifactRepository) getArtifactRepositoryManager().createRepository(repositoryURI, "My Repo", IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, properties);
			repo.executeBatch(monitor -> {
				throw new RuntimeException();
			}, new NullProgressMonitor());
			Field field = CompositeArtifactRepository.class.getDeclaredField("disableSave");
			field.setAccessible(true);
			boolean disableSave = field.getBoolean(repo);
			assertFalse("1.0", disableSave);

		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This test makes sure that the composite repository save is disabled during write
	 */
	public void testDisableSaveFlagDuringExecutionComposite() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			Map<String, String> properties = new HashMap<>();
			final CompositeArtifactRepository repo = (CompositeArtifactRepository) getArtifactRepositoryManager().createRepository(repositoryURI, "My Repo", IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, properties);
			repo.executeBatch(monitor -> {
				Field field;
				try {
					field = CompositeArtifactRepository.class.getDeclaredField("disableSave");
					field.setAccessible(true);
					boolean disableSave = field.getBoolean(repo);
					assertTrue("1.0", disableSave);
				} catch (SecurityException e1) {
					fail("1.1" + e1.getMessage());
				} catch (NoSuchFieldException e2) {
					// TODO Auto-generated catch block
					fail("1.2" + e2.getMessage());
				} catch (IllegalArgumentException e3) {
					fail("1.2" + e3.getMessage());
				} catch (IllegalAccessException e4) {
					fail("1.2" + e4.getMessage());
				}
			}, new NullProgressMonitor());

		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

}
