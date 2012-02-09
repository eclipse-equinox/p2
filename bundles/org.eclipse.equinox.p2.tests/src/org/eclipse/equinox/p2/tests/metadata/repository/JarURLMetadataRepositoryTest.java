/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestData;
import org.junit.Test;

public class JarURLMetadataRepositoryTest extends AbstractProvisioningTest {

	private IMetadataRepositoryManager manager;
	private File testRepoJar;

	public JarURLMetadataRepositoryTest(String name) {
		super(name);
	}

	public JarURLMetadataRepositoryTest() {
		this("");
	}

	protected void setUp() throws Exception {
		manager = getMetadataRepositoryManager();

	}

	protected void tearDown() throws Exception {
		manager = null;
		if (testRepoJar != null)
			FileUtils.deleteAll(testRepoJar.getParentFile());
	}

	/**
	 * Tests loading a repository in a zip file.
	 * This test case comes from bug 369834.
	 */
	@Test
	public void testZipFileRepository() throws IOException, ProvisionException, OperationCanceledException {
		//ensure a random agent doesn't cause it to fail
		File zip = TestData.getFile("bug369834", "f-TestBuild-group.group.group.zip");
		URI location = URIUtil.toJarURI(zip.toURI(), new Path(""));
		IMetadataRepository repo = manager.loadRepository(location, null);
		assertTrue(!repo.query(QueryUtil.createIUAnyQuery(), null).isEmpty());
	}

	/**
	 * Tests loading a repository in a jar file.
	 */
	public void testJarURLRepository() throws ProvisionException {
		//create a repository
		String tempDir = System.getProperty("java.io.tmpdir");
		File testRepo = new File(tempDir, "testRepo");
		FileUtils.deleteAll(testRepo);
		testRepo.mkdir();
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, "true");
		IMetadataRepository repo = manager.createRepository(testRepo.toURI(), "TestRepo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		//populate with some basic metadata
		InstallableUnitDescription descriptor = new MetadataFactory.InstallableUnitDescription();
		descriptor.setId("testIuId");
		descriptor.setVersion(Version.create("3.2.1"));
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(descriptor);
		repo.addInstallableUnits(Arrays.asList(iu));

		testRepoJar = new File(testRepo, "content.jar");
		assertTrue(testRepoJar.exists());
		testRepoJar.deleteOnExit();

		URI jarRepoLocation = null;
		try {
			jarRepoLocation = new URI("jar:" + testRepoJar.toURI() + "!/");
		} catch (URISyntaxException e) {
			fail(e.getMessage());
		}

		repo = manager.loadRepository(jarRepoLocation, null);
		assertTrue(!repo.query(QueryUtil.createIUAnyQuery(), null).isEmpty());

		URI[] local = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_LOCAL);
		boolean found = false;
		for (int i = 0; i < local.length; i++)
			if (local[i].equals(jarRepoLocation))
				found = true;
		assertTrue(found);
		manager.removeRepository(jarRepoLocation);
	}
}
