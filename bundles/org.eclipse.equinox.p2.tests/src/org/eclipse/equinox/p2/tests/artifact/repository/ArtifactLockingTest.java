/*******************************************************************************
 * Copyright (c) 2011 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ArtifactLockingTest extends AbstractProvisioningTest {

	private File targetLocation;
	private SimpleArtifactRepository repo1 = null;
	private SimpleArtifactRepository repo2 = null;
	private boolean lockingValue = false;

	public void setUp() throws Exception {
		super.setUp();
		lockingValue = Activator.getInstance().enableArtifactLocking();
		System.setProperty(Activator.ENABLE_ARTIFACT_LOCKING, "true");

		targetLocation = File.createTempFile("bundlepool", ".repo");
		targetLocation.delete();
		targetLocation.mkdirs();
		repo1 = new SimpleArtifactRepository(getAgent(), "TargetRepo", targetLocation.toURI(), null);
		Thread.sleep(1000);
		repo2 = new SimpleArtifactRepository(getAgent(), "TargetRepo", targetLocation.toURI(), null);
	}

	@Override
	protected void tearDown() throws Exception {
		System.setProperty(Activator.ENABLE_ARTIFACT_LOCKING, "" + lockingValue);
		super.tearDown();
	}

	boolean canContinue = false;

	public void testCancelLoad() throws InterruptedException, ProvisionException {
		this.canContinue = false;
		final IProgressMonitor progressMonitor = new NullProgressMonitor();
		new Thread(new Runnable() {
			public void run() {
				status = repo1.executeBatch(new IRunnableWithProgress() {

					public void run(IProgressMonitor monitor) throws OperationCanceledException {
						try {
							canContinue = true;
							Thread.sleep(3 * 1000);
							repo1.addDescriptor(new SimpleArtifactDescriptor(new ArtifactKey("org.eclipse.test", "test2", Version.create("1.0.0"))), new NullProgressMonitor());
							progressMonitor.setCanceled(true);
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// Do nothing
						}
					}
				}, new NullProgressMonitor());
			}
		}).start();

		while (!canContinue) {
			Thread.sleep(100);
		}

		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		boolean expected = false;
		try {
			artifactManager.loadRepository(targetLocation.toURI(), progressMonitor);
		} catch (OperationCanceledException e) {
			expected = true;
		}
		assertTrue("Expected an Operation Cancled Exception", expected);

	}

	public void testWaitForLoad() throws InterruptedException, ProvisionException {
		this.canContinue = false;
		new Thread(new Runnable() {
			public void run() {
				status = repo1.executeBatch(new IRunnableWithProgress() {

					public void run(IProgressMonitor monitor) throws OperationCanceledException {
						try {
							canContinue = true;
							Thread.sleep(6 * 1000);
							repo1.addDescriptor(new SimpleArtifactDescriptor(new ArtifactKey("org.eclipse.test", "test2", Version.create("1.0.0"))), new NullProgressMonitor());

						} catch (InterruptedException e) {
							// Do nothing
						}
					}
				}, new NullProgressMonitor());
			}
		}).start();

		while (!canContinue) {
			Thread.sleep(100);
		}
		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		SimpleArtifactRepository artifactRepository = (SimpleArtifactRepository) artifactManager.loadRepository(targetLocation.toURI(), new NullProgressMonitor());
		assertEquals(1, artifactRepository.getDescriptors().size());

	}

	// Helper variables for the following test case
	private boolean keepRunning = true;
	IStatus status = null;

	/**
	 * Creates 1 thread that does a long running execute batch. We then try to add descriptors to another
	 * instance of the same repository. This will block.  We then cancel the progress monitor to test
	 * that the block terminates.
	 * @throws InterruptedException
	 */
	public void testCancel() throws InterruptedException {
		final IProgressMonitor progressMonitor = new NullProgressMonitor();
		this.keepRunning = true;

		new Thread(new Runnable() {
			public void run() {
				status = repo1.executeBatch(new IRunnableWithProgress() {

					public void run(IProgressMonitor monitor) throws OperationCanceledException {
						long start = System.currentTimeMillis();
						while (keepRunning) {
							long current = System.currentTimeMillis();
							if (current - start > 1000 * 10) {
								fail("Test case never finished. Likely keep running was never set to false.");
								return;
							}
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// Do nothing
							}
						}
					}
				}, new NullProgressMonitor());
			}
		}).start();

		// Give the execute batch thread a chance to start
		Thread.sleep(1000);

		// Create a thread that will stop our progress monitor
		Thread t = new Thread(new Runnable() {

			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// Do nothing
				}
				progressMonitor.setCanceled(true);
			}
		});
		t.start();

		// This should block
		repo2.addDescriptor(new SimpleArtifactDescriptor(new ArtifactKey("org.eclipse.test", "test2", Version.create("1.0.0"))), progressMonitor);
		keepRunning = false;
		if (status != null && !status.isOK()) {
			fail(status.getMessage());
		}
		assertEquals(0, repo2.getDescriptors().size()); // The descriptor should not have been added
	}

	public void testCancelRead() throws InterruptedException {
		final IProgressMonitor progressMonitor = new NullProgressMonitor();
		this.keepRunning = true;

		new Thread(new Runnable() {
			public void run() {
				status = repo1.executeBatch(new IRunnableWithProgress() {

					public void run(IProgressMonitor monitor) throws OperationCanceledException {
						long start = System.currentTimeMillis();
						while (keepRunning) {
							long current = System.currentTimeMillis();
							if (current - start > 1000 * 10) {
								fail("Test case never finished. Likely keep running was never set to false.");
								return;
							}
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// Do nothing
							}
						}
					}
				}, new NullProgressMonitor());
			}
		}).start();

		// Give the execute batch thread a chance to start
		Thread.sleep(1000);

		// Create a thread that will stop our progress monitor
		Thread t = new Thread(new Runnable() {

			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// Do nothing
				}
				progressMonitor.setCanceled(true);
			}
		});
		t.start();

		// This should block
		repo2.getDescriptors();
		keepRunning = false;
		if (status != null && !status.isOK()) {
			fail(status.getMessage());
		}
	}

	// The following variables are used in the following test case

	IStatus status1 = null;
	IStatus status2 = null;
	boolean lockAcquired = false;

	/**
	 * This tests that two 'executeBatch' operations are not executed in 
	 * parallel, but rather, the second one waits for the first to complete.
	 * @throws InterruptedException
	 */
	public void testMultipleExecuteBatch() throws InterruptedException {
		this.lockAcquired = false;
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				status1 = repo1.executeBatch(new IRunnableWithProgress() {

					public void run(IProgressMonitor monitor) throws OperationCanceledException {
						try {
							if (lockAcquired)
								throw new RuntimeException("Lock already acquired");
							lockAcquired = true;
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// Do nothing
							}
						} finally {
							lockAcquired = false;
						}
					}
				}, new NullProgressMonitor());
			}
		});
		t1.start();

		Thread t2 = new Thread(new Runnable() {
			public void run() {
				status2 = repo2.executeBatch(new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws OperationCanceledException {
						try {
							if (lockAcquired)
								throw new RuntimeException("Lock already acquired");
							lockAcquired = true;
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// Do nothing
							}
						} finally {
							lockAcquired = false;
						}
					}
				}, new NullProgressMonitor());
			}
		});
		t2.start();

		t1.join();
		t2.join();

		if (!status1.isOK() || !status2.isOK())
			fail("Test failed, a lock acquired simultaneously by both execute batch operations");
	}

	/**
	 * This tests that we wait for the lock to come off, we then add a descriptor
	 * @throws InterruptedException
	 */
	public void testWait() throws InterruptedException {

		new Thread(new Runnable() {
			public void run() {
				status = repo1.executeBatch(new IRunnableWithProgress() {

					public void run(IProgressMonitor monitor) throws OperationCanceledException {
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							// Do nothing
						}

					}
				}, new NullProgressMonitor());
			}
		}).start();

		// Give the execute batch thread a chance to start
		Thread.sleep(1000);

		// This should block for 5 seconds, and then add the descriptor
		repo2.addDescriptor(new SimpleArtifactDescriptor(new ArtifactKey("org.eclipse.test", "test2", Version.create("1.0.0"))), new NullProgressMonitor());
		assertEquals(1, repo2.getDescriptors().size()); // The descriptor should not have been added
	}

	public void testMultipleAddDescriptors() throws InterruptedException {
		Thread.sleep(1000);
		repo1.addDescriptor(new SimpleArtifactDescriptor(new ArtifactKey("org.eclipse.test", "test1", Version.create("1.0.0"))), new NullProgressMonitor());
		assertEquals(1, repo1.getDescriptors().size());
		Thread.sleep(1000);
		repo2.addDescriptor(new SimpleArtifactDescriptor(new ArtifactKey("org.eclipse.test", "test2", Version.create("1.0.0"))), new NullProgressMonitor());
		assertEquals(2, repo2.getDescriptors().size());
	}

	public void testContainsDescriptor() throws InterruptedException {
		ArtifactKey k = new ArtifactKey("org.eclipse.test", "test1", Version.create("1.0.0"));
		SimpleArtifactDescriptor d = new SimpleArtifactDescriptor(k);
		Thread.sleep(1000);
		repo1.addDescriptor(d, new NullProgressMonitor());
		Thread.sleep(1000);
		assertTrue(repo2.contains(d));
	}

	public void testContainsKey() throws InterruptedException {
		ArtifactKey k = new ArtifactKey("org.eclipse.test", "test1", Version.create("1.0.0"));
		SimpleArtifactDescriptor d = new SimpleArtifactDescriptor(k);
		Thread.sleep(1000);
		repo1.addDescriptor(d, new NullProgressMonitor());
		Thread.sleep(1000);
		assertTrue(repo2.contains(k));
	}

	public void testMultipleRemoveDescriptors() throws InterruptedException {
		SimpleArtifactDescriptor d1 = new SimpleArtifactDescriptor(new ArtifactKey("org.eclipse.test", "test1", Version.create("1.0.0")));
		SimpleArtifactDescriptor d2 = new SimpleArtifactDescriptor(new ArtifactKey("org.eclipse.test", "test2", Version.create("1.0.0")));

		Thread.sleep(1000);
		repo1.addDescriptor(d1, new NullProgressMonitor());
		repo1.addDescriptor(d2, new NullProgressMonitor());
		assertEquals(2, repo1.getDescriptors().size());
		Thread.sleep(1000);
		repo1.removeDescriptor(d1, new NullProgressMonitor());
		assertEquals(1, repo1.getDescriptors().size());
		Thread.sleep(1000);
		repo2.removeDescriptor(d2, new NullProgressMonitor());
		assertEquals(0, repo2.getDescriptors().size());
	}

	public void testMultipleRemoveKeys() throws InterruptedException {
		ArtifactKey k1 = new ArtifactKey("org.eclipse.test", "test1", Version.create("1.0.0"));
		ArtifactKey k2 = new ArtifactKey("org.eclipse.test", "test2", Version.create("1.0.0"));
		SimpleArtifactDescriptor d1 = new SimpleArtifactDescriptor(k1);
		SimpleArtifactDescriptor d2 = new SimpleArtifactDescriptor(k2);

		Thread.sleep(1000);
		repo1.addDescriptor(d1, new NullProgressMonitor());
		repo1.addDescriptor(d2, new NullProgressMonitor());
		assertEquals(2, repo1.getDescriptors().size());
		Thread.sleep(1000);
		repo1.removeDescriptor(k1, new NullProgressMonitor());
		assertEquals(1, repo1.getDescriptors().size());
		Thread.sleep(1000);
		assertEquals(1, repo2.getDescriptors().size());
		repo2.removeDescriptor(k2, new NullProgressMonitor());
		assertEquals(0, repo2.getDescriptors().size());
	}

	public void testRemoveBulkKeys() throws InterruptedException {
		ArtifactKey k1 = new ArtifactKey("org.eclipse.test", "test1", Version.create("1.0.0"));
		ArtifactKey k2 = new ArtifactKey("org.eclipse.test", "test2", Version.create("1.0.0"));
		ArtifactKey k3 = new ArtifactKey("org.eclipse.test", "test3", Version.create("1.0.0"));
		SimpleArtifactDescriptor d1 = new SimpleArtifactDescriptor(k1);
		SimpleArtifactDescriptor d2 = new SimpleArtifactDescriptor(k2);
		SimpleArtifactDescriptor d3 = new SimpleArtifactDescriptor(k3);

		Thread.sleep(1000);
		repo1.addDescriptor(d1, new NullProgressMonitor());
		repo1.addDescriptor(d2, new NullProgressMonitor());
		repo1.addDescriptor(d3, new NullProgressMonitor());
		assertEquals(3, repo1.getDescriptors().size());
		Thread.sleep(1000);
		repo2.removeDescriptors(new IArtifactKey[] {k1, k2}, new NullProgressMonitor());
		Thread.sleep(1000);
		assertEquals(1, repo2.getDescriptors().size());
		assertEquals(1, repo1.getDescriptors().size());
	}

	public void testRemoveBulkDescriptors() throws InterruptedException {
		ArtifactKey k1 = new ArtifactKey("org.eclipse.test", "test1", Version.create("1.0.0"));
		ArtifactKey k2 = new ArtifactKey("org.eclipse.test", "test2", Version.create("1.0.0"));
		ArtifactKey k3 = new ArtifactKey("org.eclipse.test", "test3", Version.create("1.0.0"));
		SimpleArtifactDescriptor d1 = new SimpleArtifactDescriptor(k1);
		SimpleArtifactDescriptor d2 = new SimpleArtifactDescriptor(k2);
		SimpleArtifactDescriptor d3 = new SimpleArtifactDescriptor(k3);

		Thread.sleep(1000);
		repo1.addDescriptor(d1, new NullProgressMonitor());
		repo1.addDescriptor(d2, new NullProgressMonitor());
		repo1.addDescriptor(d3, new NullProgressMonitor());
		assertEquals(3, repo1.getDescriptors().size());
		Thread.sleep(1000);
		repo2.removeDescriptors(new IArtifactDescriptor[] {d1, d2}, new NullProgressMonitor());
		Thread.sleep(1000);
		assertEquals(1, repo2.getDescriptors().size());
		assertEquals(1, repo1.getDescriptors().size());
	}

	public void testRemoveAll() throws InterruptedException {
		ArtifactKey k1 = new ArtifactKey("org.eclipse.test", "test1", Version.create("1.0.0"));
		ArtifactKey k2 = new ArtifactKey("org.eclipse.test", "test2", Version.create("1.0.0"));
		SimpleArtifactDescriptor d1 = new SimpleArtifactDescriptor(k1);
		SimpleArtifactDescriptor d2 = new SimpleArtifactDescriptor(k2);

		Thread.sleep(1000);
		repo1.addDescriptor(d1, new NullProgressMonitor());
		repo1.addDescriptor(d2, new NullProgressMonitor());
		assertEquals(2, repo1.getDescriptors().size());
		Thread.sleep(1000);
		repo2.removeAll(new NullProgressMonitor());
		Thread.sleep(1000);
		assertEquals(0, repo2.getDescriptors().size());
		assertEquals(0, repo1.getDescriptors().size());
	}

	public void testReloadAdds() throws InterruptedException {
		// Delay 1 second because some operating systems only give 1 second precision
		Thread.sleep(1000);
		repo1.addDescriptor(new SimpleArtifactDescriptor(new ArtifactKey("org.eclipse.test", "test1", Version.create("1.0.0"))), new NullProgressMonitor());
		Thread.sleep(1000);
		repo2.addDescriptor(new SimpleArtifactDescriptor(new ArtifactKey("org.eclipse.test", "test2", Version.create("1.0.0"))), new NullProgressMonitor());

		assertEquals(2, repo2.getDescriptors().size());
		// Does the fist repo get reloaded when reading the descriptors
		assertEquals(2, repo1.getDescriptors().size());
	}

	public void _testSetProperty() throws InterruptedException {
		// Delay 1 second because some operating systems only give 1 second precision
		Thread.sleep(1000);
		repo1.setProperty("foo", "bar", new NullProgressMonitor());
		Thread.sleep(1000);
		assertEquals("bar", repo1.getProperty("foo"));
		assertEquals("bar", repo2.getProperty("foo"));
	}

	public void _testGetProperties() throws InterruptedException {
		// Delay 1 second because some operating systems only give 1 second precision
		Thread.sleep(1000);
		repo1.setProperty("foo", "bar", new NullProgressMonitor());
		Thread.sleep(1000);
		assertEquals("bar", repo1.getProperties().get("foo"));
		assertEquals("bar", repo2.getProperties().get("foo"));
	}

	public void _testSetName() throws InterruptedException {
		// Delay 1 second because some operating systems only give 1 second precision
		Thread.sleep(1000);
		repo1.setName("Foo");
		Thread.sleep(1000);
		assertEquals("Foo", repo1.getName());
		assertEquals("Foo", repo2.getName());
	}

	public void _testSetDescription() throws InterruptedException {
		// Delay 1 second because some operating systems only give 1 second precision
		Thread.sleep(1000);
		repo1.setDescription("Foo Bar");
		Thread.sleep(1000);
		assertEquals("Foo Bar", repo1.getDescription());
		assertEquals("Foo Bar", repo2.getDescription());
	}

}
