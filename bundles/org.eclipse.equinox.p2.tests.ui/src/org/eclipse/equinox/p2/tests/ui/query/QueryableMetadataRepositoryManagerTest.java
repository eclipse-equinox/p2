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
import java.net.URI;
import java.util.Collection;
import org.eclipse.core.tests.harness.CancelingProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IUPropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.ui.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.RepositoryLocationQuery;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.p2.tests.TestData;

/**
 * Tests for {@link QueryableMetadataRepositoryManager}.
 */
public class QueryableMetadataRepositoryManagerTest extends AbstractQueryTest {
	/**
	 * Tests querying against a non-existent repository
	 */
	public void testBrokenRepository() {
		URI brokenRepo;
		try {
			brokenRepo = TestData.getFile("metadataRepo", "bad").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.addRepository(brokenRepo);
		QueryableMetadataRepositoryManager manager = getQueryableManager();
		assertTrue("1.0", !manager.areRepositoriesLoaded());

		manager.loadAll(getMonitor());

		//false because the broken repository is not loaded
		assertTrue("1.1", !manager.areRepositoriesLoaded());
	}

	/**
	 * Tests canceling a load
	 */
	public void testCancelLoad() {
		URI location;
		try {
			location = TestData.getFile("metadataRepo", "good").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.addRepository(location);
		QueryableMetadataRepositoryManager manager = getQueryableManager();
		assertTrue("1.0", !manager.areRepositoriesLoaded());

		manager.loadAll(new CancelingProgressMonitor());

		//should not be loaded due to cancelation
		assertTrue("1.1", !manager.areRepositoriesLoaded());
	}

	public void testCancelQuery() {
		URI existing, nonExisting, broken;
		try {
			existing = TestData.getFile("metadataRepo", "good").toURI();
			nonExisting = new File("does/not/exist/testNotFoundRepository").toURI();
			broken = TestData.getFile("metadataRepo", "bad").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.addRepository(existing);
		metadataRepositoryManager.addRepository(nonExisting);
		metadataRepositoryManager.addRepository(broken);
		QueryableMetadataRepositoryManager manager = getQueryableManager();

		Collector result = manager.query(new InstallableUnitQuery("test.bundle", new Version(1, 0, 0)), new Collector(), new CancelingProgressMonitor());
		assertEquals("1.0", 0, result.size());

		//null query collects repository URLs
		result = manager.query(null, new Collector(), new CancelingProgressMonitor());
		assertEquals("2.0", 0, result.size());
	}

	public void testExistingRepository() {
		URI location;
		try {
			location = TestData.getFile("metadataRepo", "good").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.addRepository(location);
		QueryableMetadataRepositoryManager manager = getQueryableManager();
		assertTrue("1.0", !manager.areRepositoriesLoaded());

		manager.loadAll(getMonitor());

		//we can never be sure that repositories are loaded because the repository manager cache can be flushed at any time
		//		assertTrue("1.1", manager.areRepositoriesLoaded());
	}

	/**
	 * Tests querying against a non-existent repository
	 */
	public void testNotFoundRepository() {
		URI existing, nonExisting;
		try {
			existing = TestData.getFile("metadataRepo", "good").toURI();
			nonExisting = new File("does/not/exist/testNotFoundRepository").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.addRepository(existing);
		metadataRepositoryManager.addRepository(nonExisting);
		QueryableMetadataRepositoryManager manager = getQueryableManager();
		assertTrue("1.0", !manager.areRepositoriesLoaded());

		manager.loadAll(getMonitor());

		//false because the non-existent repository is not loaded
		assertTrue("1.1", !manager.areRepositoriesLoaded());
	}

	public void testQuery() {
		URI existing, nonExisting, broken;
		try {
			existing = TestData.getFile("metadataRepo", "good").toURI();
			nonExisting = new File("does/not/exist/testNotFoundRepository").toURI();
			broken = TestData.getFile("metadataRepo", "bad").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.addRepository(existing);
		metadataRepositoryManager.addRepository(nonExisting);
		metadataRepositoryManager.addRepository(broken);
		QueryableMetadataRepositoryManager manager = getQueryableManager();

		Collector result = manager.query(new InstallableUnitQuery("test.bundle", new Version(1, 0, 0)), new Collector(), getMonitor());
		assertEquals("1.0", 1, result.size());
		IInstallableUnit iu = (IInstallableUnit) result.iterator().next();
		assertEquals("1.1", "test.bundle", iu.getId());

		//RepoLocationQuery collects repository URLs
		result = manager.query(new RepositoryLocationQuery(), new Collector(), getMonitor());
		assertEquals("2.0", 3, result.size());
		Collection resultCollection = result.toCollection();
		assertTrue("2.1", resultCollection.contains(existing));
		assertTrue("2.1", resultCollection.contains(nonExisting));
		assertTrue("2.1", resultCollection.contains(broken));

		// null IUPropertyQuery collects all IUs
		result = manager.query(new InstallableUnitQuery(null), new Collector(), getMonitor());
		int iuCount = result.size();
		result = manager.query(new IUPropertyQuery(null, null), new Collector(), getMonitor());
		assertEquals("2.2", iuCount, result.size());
	}

	private QueryableMetadataRepositoryManager getQueryableManager() {
		return new QueryableMetadataRepositoryManager(Policy.getDefault().getQueryContext(), false);
	}
}
