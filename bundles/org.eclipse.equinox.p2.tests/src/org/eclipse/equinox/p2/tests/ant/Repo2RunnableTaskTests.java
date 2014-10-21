/*******************************************************************************
 *  Copyright (c) 2009, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *      Red Hat, Inc. - fragments support added.
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ant;

import java.io.*;
import java.net.URI;
import java.util.Iterator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractAntProvisioningTest;

public class Repo2RunnableTaskTests extends AbstractAntProvisioningTest {

	private URI destination, source;

	public void setUp() throws Exception {
		source = getTestData("Error loading data", "testData/mirror/mirrorSourceRepo1 with space").toURI();
		destination = getTestFolder(getName()).toURI();
		super.setUp();
	}

	public void tearDown() throws Exception {
		getArtifactRepositoryManager().removeRepository(source);
		getMetadataRepositoryManager().removeRepository(source);
		getArtifactRepositoryManager().removeRepository(destination);
		getMetadataRepositoryManager().removeRepository(destination);
		delete(new File(destination));
		super.tearDown();
	}

	/*
	 * Test the Repo2RunnableTask functions as expected on a simple repository
	 */
	public void testRepo2Runnable() {
		createRepo2RunnableTaskElement(TYPE_BOTH);

		runAntTask();
		assertEquals("Number of artifact keys differs", getArtifactKeyCount(source), getArtifactKeyCount(destination));
		assertTrue("Unexpected format", expectedFormat(destination));
	}

	public void testRepo2RunnableFragments() throws IOException {
		createRepo2RunnableTaskElementFragments(TYPE_BOTH);

		runAntTask();
		assertEquals("Number of artifact keys differs", getArtifactKeyCount(source), getArtifactKeyCount(destination));
		assertTrue("Unexpected format", expectedFormat(destination));
		File f = new File(destination);
		assertTrue("Missing content.jar", new File(f, "content.jar").exists());
		assertTrue("Missing artifacts.jar", new File(f, "artifacts.jar").exists());
		assertTrue("Missing fragment.info", new File(f, "fragment.info").exists());
		BufferedReader br = new BufferedReader(new FileReader(new File(f, "fragment.info")));
		while (br.ready())
			System.out.println(br.readLine());
		br.close();
	}

	/*
	 * Test that when an IU is specified that it is used
	 */
	public void testRepo2RunnableSpecifiedIU() {
		IInstallableUnit iu = null;
		try {
			IMetadataRepository repo = getMetadataRepositoryManager().loadRepository(source, new NullProgressMonitor());
			IQueryResult ius = repo.query(QueryUtil.createIUQuery("helloworldfeature.feature.jar"), new NullProgressMonitor());
			assertEquals("Expected number of IUs", 1, queryResultSize(ius));
			iu = (IInstallableUnit) ius.iterator().next();
		} catch (ProvisionException e) {
			fail("Failed to obtain iu", e);
		}
		AntTaskElement task = createRepo2RunnableTaskElement(TYPE_BOTH);
		task.addElement(createIUElement(iu));

		runAntTask();
		assertEquals("Number of artifact keys differs", iu.getArtifacts().size(), getArtifactKeyCount(destination));
		assertTrue("Unexpected format", expectedFormat(destination));
	}

	public void testRepo2RunnableFailOnError() {
		source = getTestData("Error loading data", "testData/mirror/mirrorSourceRepo3").toURI();
		URI binary = getTestData("Error loading binary data", "testData/testRepos/binary.repo").toURI();

		AntTaskElement mirror = new AntTaskElement("p2.mirror");
		AntTaskElement sourceRepo = new AntTaskElement("source");
		sourceRepo.addElement(getRepositoryElement(source, TYPE_BOTH));
		sourceRepo.addElement(getRepositoryElement(binary, TYPE_BOTH));
		mirror.addElement(sourceRepo);
		mirror.addElement(getRepositoryElement(destination, TYPE_BOTH));
		addTask(mirror);

		AntTaskElement delete = new AntTaskElement("delete");
		delete.addAttribute("file", getTestFolder(getName()) + "/plugins/helloworld_1.0.0.jar");
		addTask(delete);

		getArtifactRepositoryManager().removeRepository(binary);
		getMetadataRepositoryManager().removeRepository(binary);
		getArtifactRepositoryManager().removeRepository(source);
		getMetadataRepositoryManager().removeRepository(source);
		source = destination;

		File destinationFile = new File(getTestFolder(getName()), "repo2");
		destination = destinationFile.toURI();

		AntTaskElement task = createRepo2RunnableTaskElement(TYPE_BOTH);
		task.addAttribute("failOnError", "false");

		runAntTask();
		assertTrue(new File(destinationFile, "binary/f_root_1.0.0").exists());
	}

	/*
	 * Ensure that the output repository is of the expected type
	 */
	protected boolean expectedFormat(URI location) {
		IArtifactRepository repo = null;
		try {
			repo = getArtifactRepositoryManager().loadRepository(location, new NullProgressMonitor());
		} catch (ProvisionException e) {
			fail("Failed to load repository", e);
		}
		IQueryResult keys = repo.query(ArtifactKeyQuery.ALL_KEYS, null);
		for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
			IArtifactKey key = (IArtifactKey) iterator.next();
			IArtifactDescriptor[] descriptors = repo.getArtifactDescriptors(key);
			for (int n = 0; n < descriptors.length; n++) {
				IArtifactDescriptor desc = descriptors[n];
				// Features should be unzipped, others should not be.
				boolean isFolder = desc.getProperty("artifact.folder") != null ? Boolean.valueOf(desc.getProperty("artifact.folder")) : false;
				if (key.getClassifier().equals(""))
					assertTrue(desc + " is not a folder", isFolder);
				else
					assertFalse(desc + " is a folder", isFolder);
				// Artifacts should not be packed
				assertTrue("Artifact is still packed", !IArtifactDescriptor.FORMAT_PACKED.equals(desc.getProperty(IArtifactDescriptor.FORMAT)));
			}
		}
		return true;
	}

	/*
	 * Create a simple AntTaskElement for the Repo2RunnableTask
	 */
	protected AntTaskElement createRepo2RunnableTaskElement() {
		AntTaskElement task = new AntTaskElement("p2.repo2runnable");
		addTask(task);
		return task;
	}

	/*
	 * Create an AntTaskElement for the Repo2Runnabletask populated with the default source & destination 
	 */
	protected AntTaskElement createRepo2RunnableTaskElement(String type) {
		AntTaskElement task = createRepo2RunnableTaskElement();
		task.addElement(getRepositoryElement(destination, type));

		AntTaskElement sourceElement = new AntTaskElement("source");
		sourceElement.addElement(getRepositoryElement(source, type));
		task.addElement(sourceElement);

		return task;
	}

	protected AntTaskElement createRepo2RunnableTaskElementFragments(String type) {
		AntTaskElement task = createRepo2RunnableTaskElement();
		task.addElement(getRepositoryElement(destination, type));

		AntTaskElement sourceElement = new AntTaskElement("source");
		sourceElement.addElement(getRepositoryElement(source, type));
		task.addElement(sourceElement);

		//		AntTaskElement fragmentsElement = new AntTaskElement("createFragments");
		//		fragmentsElement.a
		//		task.addElement(fragmentsElement);

		task.addAttribute("createFragments", "true");
		return task;
	}
}
