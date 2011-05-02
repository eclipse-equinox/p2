/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositories;
import org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * Tests for {@link QueryableMetadataRepositoryManager}.
 */
public class QueryableMetadataRepositoryManagerTest extends AbstractQueryTest {
	/**
	 * Tests querying against a non-existent repository
	 */

	ProvisioningUI ui;
	ProvisioningSession session;

	protected void setUp() throws Exception {
		ui = ProvisioningUI.getDefaultUI();
		session = ui.getSession();
		super.setUp();
	}

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

		ProvisioningJob loadJob = new LoadMetadataRepositoryJob(ui);
		loadJob.runModal(getMonitor());

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

		ProvisioningJob loadJob = new LoadMetadataRepositoryJob(ui);
		loadJob.runModal(new CancelingProgressMonitor());

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

		IQueryResult result = manager.query(QueryUtil.createIUQuery("test.bundle", Version.createOSGi(1, 0, 0)), new CancelingProgressMonitor());
		assertTrue("1.0", result.isEmpty());
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

		ProvisioningJob loadJob = new LoadMetadataRepositoryJob(ui);
		loadJob.runModal(getMonitor());

		// the provisioning job retains references to the repos so they should
		// not get garbage collected.
		assertTrue("1.1", manager.areRepositoriesLoaded());
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
		// not loaded yet
		assertFalse("1.0", manager.areRepositoriesLoaded());

		ProvisioningJob loadJob = new LoadMetadataRepositoryJob(ui);
		loadJob.runModal(getMonitor());

		// the repositories have been loaded.  Because the non-existent 
		// repository has been noticed and recorded as missing, it
		// does not count "not loaded."
		assertTrue("1.1", manager.areRepositoriesLoaded());
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

		IQueryResult result = manager.query(QueryUtil.createIUQuery("test.bundle", Version.createOSGi(1, 0, 0)), getMonitor());
		assertEquals("1.0", 1, queryResultSize(result));
		IInstallableUnit iu = (IInstallableUnit) result.iterator().next();
		assertEquals("1.1", "test.bundle", iu.getId());

		// RepoLocationQuery must cause repository URI's to be collected and no repository
		// loading should occur.
		result = manager.locationsQueriable().query(new RepositoryLocationQuery(), getMonitor());
		assertEquals("2.0", 3, queryResultSize(result));
		assertContains("2.1", result, existing);
		assertContains("2.1", result, nonExisting);
		assertContains("2.1", result, broken);

		// null IUPropertyQuery collects all IUs
		result = manager.query(QueryUtil.createIUQuery((String) null), getMonitor());
		int iuCount = queryResultSize(result);
		result = manager.query(QueryUtil.createIUPropertyQuery(null, QueryUtil.ANY), getMonitor());
		assertEquals("2.2", iuCount, queryResultSize(result));
	}

	public void testNonLatestInMultipleRepositories() {
		URI multipleVersion1, multipleVersion2;
		try {
			multipleVersion1 = TestData.getFile("metadataRepo", "multipleversions1").toURI();
			multipleVersion2 = TestData.getFile("metadataRepo", "multipleversions2").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.addRepository(multipleVersion1);
		metadataRepositoryManager.addRepository(multipleVersion2);
		QueryableMetadataRepositoryManager manager = getQueryableManager();

		IUViewQueryContext context = new IUViewQueryContext(IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		context.setShowLatestVersionsOnly(false);

		MetadataRepositories rootElement = new MetadataRepositories(context, ui, manager);
		QueryProvider queryProvider = new QueryProvider(ui);
		ElementQueryDescriptor queryDescriptor = queryProvider.getQueryDescriptor(rootElement);
		Collection collection = queryDescriptor.performQuery(null);
		assertEquals("1.0", 5, collection.size());
	}

	public void testLatestInMultipleRepositories() {
		URI multipleVersion1, multipleVersion2;
		try {
			multipleVersion1 = TestData.getFile("metadataRepo", "multipleversions1").toURI();
			multipleVersion2 = TestData.getFile("metadataRepo", "multipleversions2").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.addRepository(multipleVersion1);
		metadataRepositoryManager.addRepository(multipleVersion2);
		QueryableMetadataRepositoryManager manager = getQueryableManager();

		IUViewQueryContext context = new IUViewQueryContext(IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		context.setShowLatestVersionsOnly(true);

		MetadataRepositories rootElement = new MetadataRepositories(context, ui, manager);
		QueryProvider queryProvider = new QueryProvider(ui);
		ElementQueryDescriptor queryDescriptor = queryProvider.getQueryDescriptor(rootElement);
		Collection collection = queryDescriptor.performQuery(null);
		assertEquals("1.0", 1, collection.size());
		AvailableIUElement next = (AvailableIUElement) collection.iterator().next();
		assertEquals("1.1", Version.createOSGi(3, 0, 0), next.getIU().getVersion());
	}

	/**
	 * Tests that the repository nickname is set on load.  See bug 274334 for details.
	 */
	public void testNicknameOnLoad() {
		URI location;
		try {
			location = TestData.getFile("metadataRepo", "good").toURI();
		} catch (Exception e) {
			fail("0.98", e);
			return;
		}
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.removeRepository(location);
		metadataRepositoryManager.addRepository(location);
		try {
			ui.loadMetadataRepository(location, false, getMonitor());
		} catch (ProvisionException e) {
			fail("0.99", e);
		}
		assertEquals("1.0", "Good Test Repository", metadataRepositoryManager.getRepositoryProperty(location, IRepository.PROP_NICKNAME));

	}

	private QueryableMetadataRepositoryManager getQueryableManager() {
		return new QueryableMetadataRepositoryManager(ui, false);
	}
}
