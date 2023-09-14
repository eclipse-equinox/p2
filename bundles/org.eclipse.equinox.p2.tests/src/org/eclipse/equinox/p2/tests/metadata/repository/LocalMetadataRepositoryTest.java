/*******************************************************************************
 *  Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import static org.junit.Assert.assertThrows;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Test API of the local metadata repository implementation.
 */
public class LocalMetadataRepositoryTest extends AbstractProvisioningTest {
	private static final String TEST_KEY = "TestKey";
	private static final String TEST_VALUE = "TestValue";
	protected File repoLocation;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		String tempDir = System.getProperty("java.io.tmpdir");
		repoLocation = new File(tempDir, "LocalMetadataRepositoryTest");
		AbstractProvisioningTest.delete(repoLocation);
		repoLocation.mkdir();
	}

	@Override
	protected void tearDown() throws Exception {
		getMetadataRepositoryManager().removeRepository(repoLocation.toURI());
		delete(repoLocation);
		super.tearDown();
	}

	private IMetadataRepository createTestRepository(IMetadataRepositoryManager manager, Map<String, String> properties)
			throws ProvisionException {
		return manager.createRepository(repoLocation.toURI(), "TestRepo",
				IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
	}

	private IMetadataRepository createTestRepository(Map<String, String> properties) throws ProvisionException {
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		return createTestRepository(manager, properties);
	}

	public void testCompressedRepository() throws ProvisionException {
		IMetadataRepository repo = createTestRepository(Map.of(IRepository.PROP_COMPRESSED, "true"));

		InstallableUnitDescription descriptor = new MetadataFactory.InstallableUnitDescription();
		descriptor.setId("testIuId");
		descriptor.setVersion(Version.create("3.2.1"));
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(descriptor);
		repo.addInstallableUnits(Arrays.asList(iu));

		File[] files = repoLocation.listFiles();
		boolean jarFilePresent = false;
		boolean xmlFilePresent = false;
		// one of the files in the repository should be the content.xml.jar
		for (File file : files) {
			if ("content.jar".equalsIgnoreCase(file.getName())) {
				jarFilePresent = true;
			}
			if ("content.xml".equalsIgnoreCase(file.getName())) {
				xmlFilePresent = true;
			}
		}
		assertTrue("Repository did not create JAR for content.xml", jarFilePresent);
		assertFalse("Repository should not create content.xml", xmlFilePresent);
	}

	public void testGetProperties() throws ProvisionException {
		IMetadataRepository repo = createTestRepository(null);
		Map<String, String> properties = repo.getProperties();
		//attempting to modify the properties should fail
		assertThrows(RuntimeException.class, () -> properties.put(TEST_KEY, TEST_VALUE));
	}

	public void testSetProperty() throws ProvisionException {
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		IMetadataRepository repo = createTestRepository(manager, null);
		Map<String, String> properties = repo.getProperties();
		assertTrue("1.0", !properties.containsKey(TEST_KEY));
		repo.setProperty(TEST_KEY, TEST_VALUE);

		//the previously obtained properties should not be affected by subsequent changes
		assertTrue("1.1", !properties.containsKey(TEST_KEY));
		properties = repo.getProperties();
		assertTrue("1.2", properties.containsKey(TEST_KEY));

		//going back to repo manager, should still get the new property
		repo = manager.loadRepository(repoLocation.toURI(), null);
		properties = repo.getProperties();
		assertTrue("1.3", properties.containsKey(TEST_KEY));

		//setting a null value should remove the key
		repo.setProperty(TEST_KEY, null);
		properties = repo.getProperties();
		assertTrue("1.4", !properties.containsKey(TEST_KEY));
	}

	public void testAddRemoveIUs() throws ProvisionException {
		IMetadataRepository repo = createTestRepository(null);
		IInstallableUnit iu = createIU("foo");
		repo.addInstallableUnits(Arrays.asList(iu));
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createIUQuery((String) null), getMonitor());
		assertEquals("1.0", 1, queryResultSize(result));
		repo.removeAll();
		result = repo.query(QueryUtil.createIUQuery((String) null), getMonitor());
		assertTrue("1.1", result.isEmpty());
	}

	public void testRemoveByQuery() throws ProvisionException {
		IMetadataRepository repo = createTestRepository(null);
		IInstallableUnit iu = createIU("foo");
		IInstallableUnit iu2 = createIU("bar");
		repo.addInstallableUnits(Arrays.asList(iu, iu2));
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createIUQuery((String) null), getMonitor());
		assertEquals("1.0", 2, queryResultSize(result));
		repo.removeInstallableUnits(Arrays.asList(iu));
		result = repo.query(QueryUtil.createIUQuery((String) null), getMonitor());
		assertEquals("1.1", 1, queryResultSize(result));
		repo.removeInstallableUnits(Arrays.asList(iu2));
		result = repo.query(QueryUtil.createIUQuery((String) null), getMonitor());
		assertTrue("1.2", result.isEmpty());

	}

	public void testUncompressedRepository() throws ProvisionException {
		IMetadataRepository repo = createTestRepository(Map.of(IRepository.PROP_COMPRESSED, "false"));

		InstallableUnitDescription descriptor = new MetadataFactory.InstallableUnitDescription();
		descriptor.setId("testIuId");
		descriptor.setVersion(Version.create("3.2.1"));
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(descriptor);
		repo.addInstallableUnits(Arrays.asList(iu));

		File[] files = repoLocation.listFiles();
		// none of the files in the repository should be the content.xml.jar
		for (File file : files) {
			if ("content.jar".equalsIgnoreCase(file.getName())) {
				fail("Repository should not create JAR for content.xml");
			}
		}
	}

	/**
	 * Tests loading a repository that has a reference to itself as a disabled repository.
	 * @throws MalformedURLException
	 * @throws ProvisionException
	 */
	public void testLoadSelfReference() throws ProvisionException {
		//setup a repository that has a reference to itself in disabled state
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		final URI repoURI = repoLocation.toURI();
		IMetadataRepository repo = createTestRepository(manager, Map.of(IRepository.PROP_COMPRESSED, "false"));
		repo.addReferences(
				List.of(new RepositoryReference(repoURI, "testNick", IRepository.TYPE_METADATA, IRepository.NONE)));

		final int[] callCount = new int[] {0};
		final boolean[] wasEnabled = new boolean[] {false};
		//add a listener to ensure we receive add events with the repository enabled
		SynchronousProvisioningListener listener = o -> {
			if (!(o instanceof RepositoryEvent))
				return;
			RepositoryEvent event = (RepositoryEvent) o;
			if (event.getKind() != RepositoryEvent.ADDED)
				return;
			if (!event.getRepositoryLocation().equals(repoURI))
				return;
			wasEnabled[0] = event.isRepositoryEnabled();
			callCount[0]++;
		};
		final IProvisioningEventBus eventBus = getEventBus();
		eventBus.addListener(listener);
		try {
			//now remove and reload the repository
			manager.removeRepository(repoURI);
			repo = manager.loadRepository(repoURI, null);
			assertTrue("1.0", manager.isEnabled(repoURI));
			assertTrue("1.1", wasEnabled[0]);
			assertEquals("1.2", 1, callCount[0]);
		} finally {
			eventBus.removeListener(listener);
		}
	}

	public void testRefreshSelfReference() throws ProvisionException {
		//setup a repository that has a reference to itself in disabled state
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		final URI repoURL = repoLocation.toURI();
		IMetadataRepository repo = createTestRepository(manager, Map.of(IRepository.PROP_COMPRESSED, "false"));
		repo.addReferences(
				List.of(new RepositoryReference(repoURL, "testNick", IRepository.TYPE_METADATA, IRepository.NONE)));

		if (!manager.isEnabled(repoURL)) {
			// Enable the repo if it's not enabled.
			manager.setEnabled(repoURL, true);
		}
		final int[] callCount = new int[] {0};
		final boolean[] wasEnabled = new boolean[] {false};
		//add a listener to ensure we receive add events with the repository enabled
		SynchronousProvisioningListener listener = o -> {
			if (!(o instanceof RepositoryEvent))
				return;
			RepositoryEvent event = (RepositoryEvent) o;
			if (event.getKind() != RepositoryEvent.ADDED)
				return;
			if (!event.getRepositoryLocation().equals(repoURL))
				return;
			wasEnabled[0] = event.isRepositoryEnabled();
			callCount[0]++;
		};
		getEventBus().addListener(listener);
		try {
			//ensure refreshing the repository doesn't disable it
			manager.refreshRepository(repoURL, null);
			assertTrue("1.0", manager.isEnabled(repoURL));
			assertTrue("1.1", wasEnabled[0]);
			assertEquals("1.2", 1, callCount[0]);
		} finally {
			getEventBus().removeListener(listener);
		}
	}

	public void testAddReference() throws ProvisionException {
		IMetadataRepository repo = createTestRepository(null);
		IRepositoryReference reference = new RepositoryReference(URI.create("https://foo.bar.org"), "aName",
				IRepository.TYPE_METADATA, IRepository.NONE);
		repo.addReferences(List.of(reference));
		assertEquals(Set.of(reference), repo.getReferences());
	}

	public void testRemoveReference() throws ProvisionException {
		IMetadataRepository repo = createTestRepository(null);
		IRepositoryReference ref1 = new RepositoryReference(URI.create("https://foo.bar1.org"), "aName1",
				IRepository.TYPE_METADATA, IRepository.NONE);
		IRepositoryReference ref2 = new RepositoryReference(URI.create("https://foo.bar2.org"), "aName2",
				IRepository.TYPE_METADATA, IRepository.NONE);

		repo.addReferences(List.of(ref1, ref2));
		assertEquals(Set.of(ref1, ref2), repo.getReferences());
		repo.removeReferences(List.of(ref1));
		assertEquals(Set.of(ref2), repo.getReferences());
	}

	public void testUniqueURIs() throws ProvisionException, OperationCanceledException {
		// The test data bug 278668 has multiple installable units with the same license uri
		IMetadataRepository repo = getMetadataRepositoryManager().loadRepository(getTestData("test data bug 278668", "testData/bug278668").toURI(), null);
		URI last = null;
		for (IInstallableUnit iu : repo.query(QueryUtil.ALL_UNITS, null)) {
			Collection<ILicense> licenses = iu.getLicenses();
			for (ILicense license : licenses) {
				URI uri = license.getLocation();
				if (last == null) {
					last = uri;
				} else {
					assertSame("License URIs must be the same object", last, uri);
				}
			}
		}
	}
}
