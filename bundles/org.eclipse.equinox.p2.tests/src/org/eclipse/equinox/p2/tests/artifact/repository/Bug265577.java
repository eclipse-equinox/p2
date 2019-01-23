/*******************************************************************************
 *  Copyright (c) 2009, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug265577 extends AbstractProvisioningTest {
	IProfile profile;
	IMetadataRepository metadataRepo;
	IArtifactRepository artifactRepo;
	ProvisioningContext context;
	IEngine engine;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		profile = createProfile(Bug265577.class.getName());

		engine = getAgent().getService(IEngine.class);
		// Load repositories
		File repoLocation = getTestData("Repository location", "/testData/bug265577/zipRepo.zip");
		if (repoLocation == null)
			fail("unable to load test data");
		URI location = URIUtil.toJarURI(repoLocation.toURI(), null);
		initializeArtifactRepo(location);
		initializeMetadataRepo(location);
	}

	private void initializeArtifactRepo(URI location) throws ProvisionException {
		IArtifactRepositoryManager mgr = getArtifactRepositoryManager();
		artifactRepo = mgr.loadRepository(location, new NullProgressMonitor());
	}

	private void initializeMetadataRepo(URI location) throws ProvisionException {
		IMetadataRepositoryManager mgr = getMetadataRepositoryManager();
		metadataRepo = mgr.loadRepository(location, new NullProgressMonitor());
		context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(mgr.getKnownRepositories(0));
	}

	// Tests the response to a feature folder inside a jar
	public void testZippedRepoWithFolderFeature() {
		IQueryResult<IInstallableUnit> queryResult = metadataRepo.query(QueryUtil.createIUQuery("Field_Assist_Example.feature.jar"), null);
		IInstallableUnit[] ius = queryResult.toArray(IInstallableUnit.class);
		IArtifactKey key = (ius[0].getArtifacts()).iterator().next();

		IArtifactDescriptor[] descriptors = artifactRepo.getArtifactDescriptors(key);
		ArtifactDescriptor desc = (ArtifactDescriptor) descriptors[0];
		// Set folder property
		desc.setProperty("artifact.folder", String.valueOf(true));

		IStatus status = null;
		try {
			OutputStream destination = new BufferedOutputStream(new FileOutputStream(new File(getTempFolder(), "FieldAssist")));
			status = artifactRepo.getArtifact(desc, destination, new NullProgressMonitor());
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
		}

		if (status.isOK())
			fail("OK Status on download");
		assertTrue(status.getMessage().equals(getJarFolderMessage(key)));
	}

	// Test to retrieve a file from a zipped metadata & artifact repository
	public void testZippedRepo() {
		IQueryResult<IInstallableUnit> queryResult = metadataRepo.query(QueryUtil.createIUQuery("valid.feature.jar"), null);
		IInstallableUnit[] ius = queryResult.toArray(IInstallableUnit.class);
		IArtifactKey key = (ius[0].getArtifacts()).iterator().next();

		IArtifactDescriptor[] descriptors = artifactRepo.getArtifactDescriptors(key);
		IArtifactDescriptor desc = descriptors[0];

		IStatus status = null;
		try {
			OutputStream destination = new BufferedOutputStream(new FileOutputStream(new File(getTempFolder(), "valid")));
			status = artifactRepo.getArtifact(desc, destination, new NullProgressMonitor());
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
		}

		assertTrue(status.getMessage(), status.isOK());
	}

	// Return expected error message for the attempt to retrieve an artifact if it is a folder from inside a jar
	private String getJarFolderMessage(IArtifactKey key) {
		return "Artifact " + key.toString() + " is a folder but the repository is an archive or remote location.";
	}
}