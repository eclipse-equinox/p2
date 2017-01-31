/******************************************************************************* 
* Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * This tests Batch Execution for Metadata Repositories
 */
public class BatchExecuteMetadataRepositoryTest extends AbstractProvisioningTest {

	//metadata repository to remove on tear down
	private File repositoryFile = null;
	private URI repositoryURI = null;

	protected Collection<IInstallableUnit> createIUInCollection(String id, Version version) {
		IInstallableUnit iu = createIU(id, version);
		Collection<IInstallableUnit> result = new ArrayList<IInstallableUnit>(1);
		result.add(iu);
		return result;
	}

	boolean fileContainsString(URI location, String string) throws IOException {
		StringBuffer buffer = new StringBuffer();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(location)));
			while (reader.ready())
				buffer.append(reader.readLine());
			return buffer.toString().contains(string);
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	/*
	 * This tests that in the normal case, the batch process runs fine
	 */
	public void testAdd() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			Map properties = new HashMap();
			final LocalMetadataRepository repo = (LocalMetadataRepository) getMetadataRepositoryManager().createRepository(repositoryURI, "My Repo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			final URI contentXML = new URI(repositoryFile.toURI().toString() + "/content.xml");
			IStatus status = repo.executeBatch(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					repo.addInstallableUnits(createIUInCollection("foo", Version.emptyVersion));
					try {
						assertFalse("1.0", fileContainsString(contentXML, "foo"));
					} catch (IOException e) {
						fail("0.99");
					}
				}
			}, new NullProgressMonitor());
			assertTrue(status.isOK());
			assertTrue("1.0", fileContainsString(contentXML, "foo"));
			assertEquals("2.0", 1, repo.query(QueryUtil.createIUQuery("foo"), new NullProgressMonitor()).toSet().size());
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
			Map properties = new HashMap();
			final LocalMetadataRepository repo = (LocalMetadataRepository) getMetadataRepositoryManager().createRepository(repositoryURI, "My Repo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			final URI contentXML = new URI(repositoryFile.toURI().toString() + "/content.xml");
			IStatus status = repo.executeBatch(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					repo.addInstallableUnits(createIUInCollection("foo", Version.emptyVersion));
					repo.addInstallableUnits(createIUInCollection("bar", Version.emptyVersion));
					repo.addInstallableUnits(createIUInCollection("baz", Version.emptyVersion));
					try {
						assertFalse("1.0", fileContainsString(contentXML, "foo"));
						assertFalse("1.0", fileContainsString(contentXML, "bar"));
						assertFalse("1.0", fileContainsString(contentXML, "baz"));
					} catch (IOException e) {
						fail("0.99");
					}
				}
			}, new NullProgressMonitor());
			assertTrue(status.isOK());
			assertEquals("1.0", 1, repo.query(QueryUtil.createIUQuery("foo"), new NullProgressMonitor()).toSet().size());
			assertEquals("1.1", 1, repo.query(QueryUtil.createIUQuery("bar"), new NullProgressMonitor()).toSet().size());
			assertEquals("1.2", 1, repo.query(QueryUtil.createIUQuery("baz"), new NullProgressMonitor()).toSet().size());
			assertTrue("2.0", fileContainsString(contentXML, "foo"));
			assertTrue("2.1", fileContainsString(contentXML, "bar"));
			assertTrue("2.2", fileContainsString(contentXML, "baz"));
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
			Map properties = new HashMap();
			final LocalMetadataRepository repo = (LocalMetadataRepository) getMetadataRepositoryManager().createRepository(repositoryURI, "My Repo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			final URI contentXML = new URI(repositoryFile.toURI().toString() + "/content.xml");
			IStatus status = repo.executeBatch(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					repo.addInstallableUnits(createIUInCollection("foo", Version.emptyVersion));
					repo.addInstallableUnits(createIUInCollection("bar", Version.emptyVersion));
					throw new RuntimeException();
				}
			}, new NullProgressMonitor());
			assertFalse(status.isOK());
			assertEquals("1.0", 1, repo.query(QueryUtil.createIUQuery("foo"), new NullProgressMonitor()).toSet().size());
			assertEquals("1.1", 1, repo.query(QueryUtil.createIUQuery("bar"), new NullProgressMonitor()).toSet().size());
			assertTrue("2.0", fileContainsString(contentXML, "foo"));
			assertTrue("2.1", fileContainsString(contentXML, "bar"));
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
			Map properties = new HashMap();
			final LocalMetadataRepository repo = (LocalMetadataRepository) getMetadataRepositoryManager().createRepository(repositoryURI, "My Repo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			final URI contentXML = new URI(repositoryFile.toURI().toString() + "/content.xml");
			IStatus status = repo.executeBatch(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					Collection<IInstallableUnit> foo = createIUInCollection("foo", Version.emptyVersion);
					repo.addInstallableUnits(foo);
					repo.addInstallableUnits(createIUInCollection("bar", Version.emptyVersion));
					repo.removeInstallableUnits(foo);
				}
			}, new NullProgressMonitor());
			assertTrue(status.isOK());
			assertEquals("1.0", 0, repo.query(QueryUtil.createIUQuery("foo"), new NullProgressMonitor()).toSet().size());
			assertEquals("1.1", 1, repo.query(QueryUtil.createIUQuery("bar"), new NullProgressMonitor()).toSet().size());
			assertFalse("2.0", fileContainsString(contentXML, "foo"));
			assertTrue("2.1", fileContainsString(contentXML, "bar"));
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
			Map properties = new HashMap();
			final LocalMetadataRepository repo = (LocalMetadataRepository) getMetadataRepositoryManager().createRepository(repositoryURI, "My Repo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			final URI contentXML = new URI(repositoryFile.toURI().toString() + "/content.xml");
			IStatus status = repo.executeBatch(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					repo.addInstallableUnits(createIUInCollection("1", Version.emptyVersion));
					repo.addInstallableUnits(createIUInCollection("2", Version.emptyVersion));
					repo.addInstallableUnits(createIUInCollection("3", Version.emptyVersion));
					Collection<IInstallableUnit> foo = createIUInCollection("foo", Version.emptyVersion);
					repo.addInstallableUnits(foo);
					repo.addInstallableUnits(createIUInCollection("bar", Version.emptyVersion));
					repo.removeInstallableUnits(foo);
				}
			}, new NullProgressMonitor());
			assertTrue(status.isOK());
			assertEquals("1.0", 4, repo.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()).toSet().size());
			assertFalse("2.0", fileContainsString(contentXML, "foo"));
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This tests that in the normal case, the batch process runs fine
	 */
	public void testBatchProcessingOK() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			Map properties = new HashMap();
			final LocalMetadataRepository repo = (LocalMetadataRepository) getMetadataRepositoryManager().createRepository(repositoryURI, "My Repo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			IStatus status = repo.executeBatch(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					// empty
				}
			}, new NullProgressMonitor());
			assertTrue(status.isOK());
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	class FailingSimpleMetadataRepository extends LocalMetadataRepository {
		boolean executeBatch = false;

		/**
		 * @param repositoryName
		 * @param location
		 * @param properties
		 */
		public FailingSimpleMetadataRepository() {
			super(getAgent());
		}

		public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
			executeBatch = true;
			return super.executeBatch(runnable, monitor);
		}

		public void save() {
			if (executeBatch)
				throw new RuntimeException("foo");
		}
	}

	/*
	 * This tests that exceptions are properly propagated for a SimpleMetadataRepository 
	 */
	public void testBatchProcessingExceptionsSimple() {
		try {
			LocalMetadataRepository simpleMetadataRepository = new FailingSimpleMetadataRepository();

			IStatus status = simpleMetadataRepository.executeBatch(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					throw new RuntimeException("bar");
				}
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
	 * This tests that exceptions are properly propagated for a SimpleMetadataRepository
	 */
	public void testBatchProcessingSaveExceptionSimple() {
		try {
			LocalMetadataRepository simpleMetadataRepository = new FailingSimpleMetadataRepository();

			IStatus status = simpleMetadataRepository.executeBatch(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					// empty
				}
			}, new NullProgressMonitor());
			assertFalse(status.isOK());
			assertEquals("foo", status.getException().getMessage());
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	class TrackSavignSimpleMetadataRepository extends LocalMetadataRepository {
		boolean executeBatch = false;
		public boolean didSave = false;

		/**
		 * @param repositoryName
		 * @param location
		 * @param properties
		 */
		public TrackSavignSimpleMetadataRepository() {
			super(getAgent());
		}

		public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
			executeBatch = true;
			return super.executeBatch(runnable, monitor);
		}

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
			TrackSavignSimpleMetadataRepository simpleMetadataRepository = new TrackSavignSimpleMetadataRepository();
			simpleMetadataRepository.executeBatch(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					//do nothing;
				}
			}, new NullProgressMonitor());
			assertTrue(simpleMetadataRepository.didSave);
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
			TrackSavignSimpleMetadataRepository simpleMetadataRepository = new TrackSavignSimpleMetadataRepository();
			simpleMetadataRepository.executeBatch(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					throw new RuntimeException();
				}
			}, new NullProgressMonitor());
			assertTrue(simpleMetadataRepository.didSave);
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
			Map properties = new HashMap();
			final LocalMetadataRepository repo = (LocalMetadataRepository) getMetadataRepositoryManager().createRepository(repositoryURI, "My Repo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			repo.executeBatch(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					throw new RuntimeException();
				}
			}, new NullProgressMonitor());
			Field field = LocalMetadataRepository.class.getDeclaredField("disableSave");
			field.setAccessible(true);
			boolean disableSave = field.getBoolean(repo);
			assertFalse("1.0", disableSave);

		} catch (Exception e) {
			fail("Test failed", e);
		}
	}

	/*
	 * This test ensure that the simple metadata repository disables the 
	 * save flag during the batch process
	 */
	public void testDisableSaveFlagDuringExecutionSimple() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();
			Map properties = new HashMap();
			final LocalMetadataRepository repo = (LocalMetadataRepository) getMetadataRepositoryManager().createRepository(repositoryURI, "My Repo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			repo.executeBatch(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					Field field;
					try {
						field = LocalMetadataRepository.class.getDeclaredField("disableSave");
						field.setAccessible(true);
						boolean disableSave = field.getBoolean(repo);
						assertTrue("1.0", disableSave);
					} catch (SecurityException e) {
						fail("1.1" + e.getMessage());
					} catch (NoSuchFieldException e) {
						// TODO Auto-generated catch block
						fail("1.2" + e.getMessage());
					} catch (IllegalArgumentException e) {
						fail("1.2" + e.getMessage());
					} catch (IllegalAccessException e) {
						fail("1.2" + e.getMessage());
					}
				}
			}, new NullProgressMonitor());

		} catch (Exception e) {
			fail("Test failed", e);
		}
	}
}
