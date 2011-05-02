/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ant;

import java.io.File;
import java.net.URI;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractAntProvisioningTest;

public class RepoTasksTests extends AbstractAntProvisioningTest {
	private static final String MIRROR_TASK = "p2.mirror";
	private static final String REMOVE_IU_TASK = "p2.remove.iu";

	private URI destinationRepo;
	private URI sourceRepo;

	public void setUp() throws Exception {
		super.setUp();
		// Get a random location to create a repository
		destinationRepo = getTestFolder(getName()).toURI();
		sourceRepo = getTestData("error loading data", "testData/mirror/mirrorSourceRepo2").toURI();
	}

	public void tearDown() throws Exception {
		// Remove repository manager references
		getArtifactRepositoryManager().removeRepository(destinationRepo);
		getMetadataRepositoryManager().removeRepository(destinationRepo);
		getArtifactRepositoryManager().removeRepository(sourceRepo);
		getMetadataRepositoryManager().removeRepository(sourceRepo);
		// Cleanup disk
		delete(new File(destinationRepo).getParentFile());
		super.tearDown();
	}

	public void testRemoveIU() throws Exception {
		AntTaskElement mirror = new AntTaskElement(MIRROR_TASK);
		AntTaskElement source = new AntTaskElement("source");
		source.addElement(getRepositoryElement(sourceRepo, TYPE_BOTH));
		mirror.addElement(source);
		mirror.addElement(getRepositoryElement(destinationRepo, TYPE_BOTH));
		addTask(mirror);

		AntTaskElement removeIU = new AntTaskElement(REMOVE_IU_TASK);
		removeIU.addElement(getRepositoryElement(destinationRepo, TYPE_BOTH));
		removeIU.addElement(getIUElement("anotherplugin", null));
		AntTaskElement iuElement = new AntTaskElement("iu");
		iuElement.addAttribute("query", "");
		iuElement.addAttribute("artifacts", "(format=packed)");
		removeIU.addElement(iuElement);
		addTask(removeIU);

		runAntTask();

		IMetadataRepository metadata = loadMetadataRepository(destinationRepo);
		IInstallableUnit iu = getIU(metadata, "anotherplugin");
		assertNull(iu);
		assertNotNull(getIU(metadata, "anotherfeature.feature.group"));

		IArtifactRepository artifacts = getArtifactRepositoryManager().loadRepository(destinationRepo, null);
		IQueryResult keys = artifacts.query(new ArtifactKeyQuery(null, "anotherplugin", null), null);
		assertTrue(keys.isEmpty());
		assertFalse(new File(getTestFolder(getName()), "plugins/anotherplugin_1.0.0.jar").exists());
	}
}
