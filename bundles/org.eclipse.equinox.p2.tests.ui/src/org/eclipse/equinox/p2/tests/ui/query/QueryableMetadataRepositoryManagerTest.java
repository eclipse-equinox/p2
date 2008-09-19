/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import org.eclipse.core.tests.harness.CancelingProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.ui.model.MetadataRepositories;
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestData;
import org.osgi.framework.Version;

/**
 * Tests for {@link QueryableMetadataRepositoryManager}.
 */
public class QueryableMetadataRepositoryManagerTest extends AbstractProvisioningTest {
	/**
	 * Tests querying against a non-existent repository
	 */
	public void testBrokenRepository() {
		URL brokenRepo;
		try {
			brokenRepo = TestData.getFile("metadataRepo", "bad").toURL();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		MetadataRepositories repos = new MetadataRepositories(new URL[] {brokenRepo});
		QueryableMetadataRepositoryManager manager = new QueryableMetadataRepositoryManager(repos);
		assertTrue("1.0", !manager.areRepositoriesLoaded());

		manager.loadAll(getMonitor());

		//false because the broken repository is not loaded
		assertTrue("1.1", !manager.areRepositoriesLoaded());
	}

	/**
	 * Tests canceling a load
	 */
	public void testCancelLoad() {
		URL location;
		try {
			location = TestData.getFile("metadataRepo", "good").toURL();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		MetadataRepositories repos = new MetadataRepositories(new URL[] {location});
		QueryableMetadataRepositoryManager manager = new QueryableMetadataRepositoryManager(repos);
		assertTrue("1.0", !manager.areRepositoriesLoaded());

		manager.loadAll(new CancelingProgressMonitor());

		//should not be loaded due to cancelation
		assertTrue("1.1", !manager.areRepositoriesLoaded());
	}

	public void testCancelQuery() {
		URL existing, nonExisting, broken;
		try {
			existing = TestData.getFile("metadataRepo", "good").toURL();
			nonExisting = new File("does/not/exist/testNotFoundRepository").toURL();
			broken = TestData.getFile("metadataRepo", "bad").toURL();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		MetadataRepositories repos = new MetadataRepositories(new URL[] {existing, nonExisting, broken});
		QueryableMetadataRepositoryManager manager = new QueryableMetadataRepositoryManager(repos);

		Collector result = manager.query(new InstallableUnitQuery("test.bundle", new Version(1, 0, 0)), new Collector(), new CancelingProgressMonitor());
		assertEquals("1.0", 0, result.size());

		//null query collects repository URLs
		result = manager.query(null, new Collector(), new CancelingProgressMonitor());
		assertEquals("2.0", 0, result.size());
	}

	public void testExistingRepository() {
		URL location;
		try {
			location = TestData.getFile("metadataRepo", "good").toURL();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		MetadataRepositories repos = new MetadataRepositories(new URL[] {location});
		QueryableMetadataRepositoryManager manager = new QueryableMetadataRepositoryManager(repos);
		assertTrue("1.0", !manager.areRepositoriesLoaded());

		manager.loadAll(getMonitor());

		assertTrue("1.1", manager.areRepositoriesLoaded());
	}

	/**
	 * Tests querying against a non-existent repository
	 */
	public void testNotFoundRepository() {
		URL existing, nonExisting;
		try {
			existing = TestData.getFile("metadataRepo", "good").toURL();
			nonExisting = new File("does/not/exist/testNotFoundRepository").toURL();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		MetadataRepositories repos = new MetadataRepositories(new URL[] {existing, nonExisting});
		QueryableMetadataRepositoryManager manager = new QueryableMetadataRepositoryManager(repos);
		assertTrue("1.0", !manager.areRepositoriesLoaded());

		manager.loadAll(getMonitor());

		//false because the non-existent repository is not loaded
		assertTrue("1.1", !manager.areRepositoriesLoaded());
	}

	/**
	 * When QueryableMetadataRepositoryManager has a null set of repositories, it uses all known repositories
	 */
	public void testNullRepositories() {
		MetadataRepositories repos = new MetadataRepositories();
		repos.setIncludeDisabledRepositories(true);
		QueryableMetadataRepositoryManager manager = new QueryableMetadataRepositoryManager(repos);
		manager.loadAll(getMonitor());
		manager.areRepositoriesLoaded();
	}

	public void testQuery() {
		URL existing, nonExisting, broken;
		try {
			existing = TestData.getFile("metadataRepo", "good").toURL();
			nonExisting = new File("does/not/exist/testNotFoundRepository").toURL();
			broken = TestData.getFile("metadataRepo", "bad").toURL();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		MetadataRepositories repos = new MetadataRepositories(new URL[] {existing, nonExisting, broken});
		QueryableMetadataRepositoryManager manager = new QueryableMetadataRepositoryManager(repos);

		Collector result = manager.query(new InstallableUnitQuery("test.bundle", new Version(1, 0, 0)), new Collector(), getMonitor());
		assertEquals("1.0", 1, result.size());
		IInstallableUnit iu = (IInstallableUnit) result.iterator().next();
		assertEquals("1.1", "test.bundle", iu.getId());

		//null query collects repository URLs
		result = manager.query(null, new Collector(), getMonitor());
		assertEquals("2.0", 3, result.size());
		Collection resultCollection = result.toCollection();
		assertTrue("2.1", resultCollection.contains(existing));
		assertTrue("2.1", resultCollection.contains(nonExisting));
		assertTrue("2.1", resultCollection.contains(broken));
	}
}
