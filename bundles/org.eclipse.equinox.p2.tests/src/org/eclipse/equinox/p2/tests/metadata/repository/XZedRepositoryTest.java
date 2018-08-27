/*******************************************************************************
 * Copyright (c) 2015, 2017 Rapicorp, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp, Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.junit.Test;

public class XZedRepositoryTest extends AbstractProvisioningTest {

	@Test
	public void testLoadContentJarAndXZ() throws ProvisionException, OperationCanceledException {
		IMetadataRepository repo = getMetadataRepositoryManager().loadRepository(getTestData("xzedRepo", "testData/xzRepoTests/metadata/contentJarAndXZ").toURI(), null);
		IQueryResult<IInstallableUnit> units = repo.query(QueryUtil.createIUQuery("testIU", Version.create("2.0.0")), null);
		assertEquals(1, units.toSet().size());
	}

	@Test
	public void testLoadXzAndContentJar() throws ProvisionException, OperationCanceledException {
		IMetadataRepository repo = getMetadataRepositoryManager().loadRepository(getTestData("xzedRepo", "testData/xzRepoTests/metadata/xzAndContentJar").toURI(), null);
		IQueryResult<IInstallableUnit> units = repo.query(QueryUtil.createIUQuery("iuFromXZ", Version.create("2.0.0")), null);
		assertEquals(1, units.toSet().size());
	}

	@Test
	public void testLoadXzAndContentXML() throws ProvisionException, OperationCanceledException {
		IMetadataRepository repo = getMetadataRepositoryManager().loadRepository(getTestData("xzedRepo", "testData/xzRepoTests/metadata/xzAndContentXML").toURI(), null);
		IQueryResult<IInstallableUnit> units = repo.query(QueryUtil.createIUQuery("iuFromXZ", Version.create("2.0.0")), null);
		assertEquals(1, units.toSet().size());
	}

	@Test
	public void testLoadXzBusted() throws OperationCanceledException {
		boolean repoCanLoad = true;
		try {
			getMetadataRepositoryManager().loadRepository(getTestData("xzedRepo", "testData/xzRepoTests/metadata/xzBusted").toURI(), null);
		} catch (ProvisionException e) {
			repoCanLoad = false;
		}
		assertFalse(repoCanLoad);
	}

	@Test
	public void testLoadXzOnly() throws ProvisionException, OperationCanceledException {
		IMetadataRepository repo = getMetadataRepositoryManager().loadRepository(getTestData("xzedRepo", "testData/xzRepoTests/metadata/xzOnly").toURI(), null);
		IQueryResult<IInstallableUnit> units = repo.query(QueryUtil.createIUQuery("iuFromXZ", Version.create("2.0.0")), null);
		assertEquals(1, units.toSet().size());
	}

	@Test
	public void testArtifactsJarAndXZ() throws ProvisionException, OperationCanceledException {
		IArtifactRepository repo = getArtifactRepositoryManager().loadRepository(getTestData("xzedRepo", "testData/xzRepoTests/artifacts/artifactsJarAndXZ").toURI(), null);
		IQueryResult<IArtifactKey> units = repo.query(new ArtifactKeyQuery("osgi.bundle", "aaPlugin", new VersionRange("[1.0.0, 1.0.0]")), null);

		assertEquals(1, units.toSet().size());
	}

	@Test
	public void testxzAndArtifactsJar() throws ProvisionException, OperationCanceledException {
		IArtifactRepository repo = getArtifactRepositoryManager().loadRepository(getTestData("xzedRepo", "testData/xzRepoTests/artifacts/xzAndArtifactsJar").toURI(), null);
		IQueryResult<IArtifactKey> units = repo.query(new ArtifactKeyQuery("osgi.bundle", "aaPluginFromXZ", new VersionRange("[1.0.0, 1.0.0]")), null);

		assertEquals(1, units.toSet().size());
	}

	@Test
	public void testxzAndArtifactsXML() throws ProvisionException, OperationCanceledException {
		IArtifactRepository repo = getArtifactRepositoryManager().loadRepository(getTestData("xzedRepo", "testData/xzRepoTests/artifacts/xzAndArtifactsXML").toURI(), null);
		IQueryResult<IArtifactKey> units = repo.query(new ArtifactKeyQuery("osgi.bundle", "aaPluginFromXZ", new VersionRange("[1.0.0, 1.0.0]")), null);

		assertEquals(1, units.toSet().size());
	}

	@Test
	public void testxzOnly() throws ProvisionException, OperationCanceledException {
		IArtifactRepository repo = getArtifactRepositoryManager().loadRepository(getTestData("xzedRepo", "testData/xzRepoTests/artifacts/xzOnly").toURI(), null);
		IQueryResult<IArtifactKey> units = repo.query(new ArtifactKeyQuery("osgi.bundle", "aaPluginFromXZ", new VersionRange("[1.0.0, 1.0.0]")), null);

		assertEquals(1, units.toSet().size());
	}

	@Test
	public void testLoadArtifactsXzBusted() throws OperationCanceledException {
		boolean repoCanLoad = true;
		try {
			getMetadataRepositoryManager().loadRepository(getTestData("xzedRepo", "testData/xzRepoTests/artifacts/xzBusted").toURI(), null);
		} catch (ProvisionException e) {
			repoCanLoad = false;
		}
		assertFalse(repoCanLoad);
	}
}
