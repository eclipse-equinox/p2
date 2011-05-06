/*******************************************************************************
 *  Copyright (c) 2009, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     SAP AG - repository atomic loading
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ant;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.ICompositeRepository;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractAntProvisioningTest;

public class CompositeRepositoryTaskTest extends AbstractAntProvisioningTest {
	private static final String ADD_ELEMENT = "add";
	private static final String REMOVE_ELEMENT = "remove";
	private URI compositeSite;
	private URI childSite, childSite2;

	public void setUp() throws Exception {
		super.setUp();
		// Get a random location to create a repository
		compositeSite = (new File(getTempFolder(), getUniqueString())).toURI();
		childSite = getTestData("Loading test data", "testData/testRepos/simple.1").toURI();
		childSite2 = new URI("memory:/in/memory");
	}

	public void tearDown() throws Exception {
		// Remove repository manager references
		getArtifactRepositoryManager().removeRepository(compositeSite);
		getMetadataRepositoryManager().removeRepository(compositeSite);

		getArtifactRepositoryManager().removeRepository(childSite);
		getMetadataRepositoryManager().removeRepository(childSite);
		getArtifactRepositoryManager().removeRepository(childSite2);
		getMetadataRepositoryManager().removeRepository(childSite2);

		// Cleanup disk
		delete(new File(compositeSite));
		super.tearDown();
	}

	/*
	 * Test adding a child to an existing artifact repository
	 */
	public void testAddChildToExistingArtifactRepository() throws Exception {
		// Create repository
		createCompositeRepository(TYPE_ARTIFACT);
		// Create the modify repository task
		AntTaskElement modify = createCompositeRepositoryTaskElement(TYPE_ARTIFACT);
		addTask(modify);

		// Create the Add element
		AntTaskElement add = new AntTaskElement(ADD_ELEMENT);
		add.addElement(getRepositoryElement(childSite, TYPE_ARTIFACT));
		modify.addElement(add);

		// Run the task
		runAntTask();

		CompositeArtifactRepository repo = (CompositeArtifactRepository) getCompositeRepository(TYPE_ARTIFACT);
		assertTrue("Repository does not contain child", repo.getChildren().contains(childSite));
	}

	/*
	 * Test what occurs when no children are added to the newly created Composite repository
	 */
	public void testCreateNoChidlren() {
		AntTaskElement modify = createCompositeRepositoryTaskElement(TYPE_BOTH);
		addTask(modify);
		runAntTask();

		if (getArtifactRepositoryManager().contains(compositeSite))
			getArtifactRepositoryManager().removeRepository(compositeSite);
		if (getMetadataRepositoryManager().contains(compositeSite))
			getMetadataRepositoryManager().removeRepository(compositeSite);

		ICompositeRepository artifact = null;
		ICompositeRepository metadata = null;
		try {
			artifact = (ICompositeRepository) getArtifactRepositoryManager().loadRepository(compositeSite, null);
			metadata = (ICompositeRepository) getMetadataRepositoryManager().loadRepository(compositeSite, null);
		} catch (ProvisionException e) {
			fail("Failed to load repositories", e);
		}
		assertTrue("Artifact Repository contains children", artifact.getChildren().isEmpty());
		assertTrue("Metadata Repository contains children", metadata.getChildren().isEmpty());
	}

	/*
	 * Test adding a child to an existing metadata repository 
	 */
	public void testAddChildToExistingMetadataRepository() {
		// Create repository
		createCompositeRepository(TYPE_METADATA);
		// Create the modify repository task
		AntTaskElement modify = createCompositeRepositoryTaskElement(TYPE_METADATA);
		addTask(modify);

		// Create the Add element
		AntTaskElement add = new AntTaskElement(ADD_ELEMENT);
		add.addElement(getRepositoryElement(childSite, TYPE_METADATA));
		modify.addElement(add);

		// Run the task
		runAntTask();

		CompositeMetadataRepository repo = (CompositeMetadataRepository) getCompositeRepository(TYPE_METADATA);
		assertTrue("Repository does not contain child", repo.getChildren().contains(childSite));
	}

	public void testAddChild() throws URISyntaxException {
		// Create repository
		createCompositeRepository(TYPE_METADATA);
		// Create the modify repository task
		AntTaskElement modify = createCompositeRepositoryTaskElement(TYPE_METADATA);
		addTask(modify);

		// Create the Add element
		AntTaskElement add = new AntTaskElement(ADD_ELEMENT);
		add.addAttribute("location", "childSite");
		add.addAttribute("kind", TYPE_METADATA);
		modify.addElement(add);

		// Run the task
		runAntTask();

		CompositeMetadataRepository repo = (CompositeMetadataRepository) getCompositeRepository(TYPE_METADATA);
		URI child = URIUtil.fromString("childSite");
		child = URIUtil.makeAbsolute(child, repo.getLocation());
		assertTrue("Repository does not contain child", repo.getChildren().contains(child));
	}

	/*
	 * Test adding a child to both types of repositories (which already exist)
	 */
	public void testAddChildToExistingRepositories() {
		// Create repository
		createCompositeRepository(null);
		// Create the modify repository task
		AntTaskElement modify = createCompositeRepositoryTaskElement(null);
		addTask(modify);

		// Create the Add element
		modify.addElement(createAddElement(null, new URI[] {childSite}));

		// Run the task
		runAntTask();

		CompositeArtifactRepository artifactRepo = (CompositeArtifactRepository) getCompositeRepository(TYPE_ARTIFACT);
		assertTrue("Repository does not contain child", artifactRepo.getChildren().contains(childSite));

		CompositeMetadataRepository metadataRepo = (CompositeMetadataRepository) getCompositeRepository(TYPE_METADATA);
		assertTrue("Repository does not contain child", metadataRepo.getChildren().contains(childSite));
	}

	/*
	 * Test the ability to remove all children
	 */
	public void testRemoveAllChildren() {
		// Create repository
		ICompositeRepository parent = createCompositeRepository(TYPE_ARTIFACT);
		parent.addChild(childSite);

		// Create the modify repository task
		AntTaskElement modify = new AntTaskElement("p2.composite.repository");
		AntTaskElement destination = getRepositoryElement(compositeSite, TYPE_ARTIFACT);
		destination.addAttribute("append", String.valueOf(false));
		modify.addElement(destination);
		addTask(modify);

		// Run the task
		runAntTask();

		CompositeArtifactRepository artifactRepo = (CompositeArtifactRepository) getCompositeRepository(TYPE_ARTIFACT);
		assertTrue("Children not removed", artifactRepo.getChildren().isEmpty());

	}

	/*
	 * Test the removal of specified children
	 */
	public void testRemoveChild() {
		ICompositeRepository repo = createCompositeRepository(TYPE_ARTIFACT);
		try {
			getArtifactRepositoryManager().loadRepository(childSite, null);
			getArtifactRepositoryManager().createRepository(childSite2, "Child site", IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);

			repo.addChild(childSite);
			repo.addChild(childSite2);
		} catch (ProvisionException e) {
			fail("Failed to create child repositories");
		}
		getArtifactRepositoryManager().removeRepository(compositeSite);

		AntTaskElement modify = createCompositeRepositoryTaskElement(TYPE_ARTIFACT);
		modify.addElement(createRemoveElement(TYPE_ARTIFACT, new URI[] {childSite}));
		addTask(modify);

		runAntTask();

		repo = getCompositeRepository(TYPE_ARTIFACT);

		assertFalse(repo.getChildren().contains(childSite));
		assertTrue(repo.getChildren().contains(childSite2));
	}

	/*
	 * Test creating a CompositeArtifactRepository
	 */
	public void testCreateCompositeArtifactRepository() throws Exception {
		// Create Composite Repository Task
		AntTaskElement createCompositeTask = createCompositeRepositoryTaskElement(TYPE_ARTIFACT);
		addTask(createCompositeTask);

		runAntTask();

		assertTrue(getArtifactRepositoryManager().contains(compositeSite));
		assertTrue(getArtifactRepositoryManager().loadRepository(compositeSite, null) instanceof CompositeArtifactRepository);
		assertFalse("Metadata repository does not exists", getMetadataRepositoryManager().contains(compositeSite));
	}

	/*
	 * Test creating a CompositeMetadataRepository
	 */
	public void testCreateCompositeMetadataRepository() throws Exception {
		// Create Composite Repository Task
		AntTaskElement createCompositeTask = createCompositeRepositoryTaskElement(TYPE_METADATA);
		addTask(createCompositeTask);

		runAntTask();

		assertTrue("Metadata repository does not exists", getMetadataRepositoryManager().contains(compositeSite));
		assertTrue("Metadata repository is not a CompositeRepository", getMetadataRepositoryManager().loadRepository(compositeSite, null) instanceof CompositeMetadataRepository);
		assertFalse("Artifact repository also exists", getArtifactRepositoryManager().contains(compositeSite));
	}

	/*
	 * Tests the ability to create both Artifact & Metadata repositories at once.
	 */
	public void testCreateCombinedCompositeRepository() throws Exception {
		// Create Composite Repository Task
		AntTaskElement createCompositeTask = createCompositeRepositoryTaskElement(null);
		addTask(createCompositeTask);

		runAntTask();

		assertTrue("Metadata repository does not exists", getMetadataRepositoryManager().contains(compositeSite));
		assertTrue("Artifact repository does not exists", getArtifactRepositoryManager().contains(compositeSite));
		assertTrue("Metadata repository is not a CompositeRepository", getMetadataRepositoryManager().loadRepository(compositeSite, null) instanceof CompositeMetadataRepository);
		assertTrue("Artifact repository is not a CompositeRepository", getArtifactRepositoryManager().loadRepository(compositeSite, null) instanceof CompositeArtifactRepository);
	}

	/*
	 * Test that failOnExists attribute is honoured
	 */
	public void testFailOnExists() throws Exception {
		// Create Composite Repository Task
		AntTaskElement createCompositeTask = createCompositeRepositoryTaskElement(TYPE_ARTIFACT);
		addTask(createCompositeTask);
		runAntTask();

		// Set failOnExists
		createCompositeTask.addAttributes(new String[] {"failOnExists", String.valueOf(true)});

		Throwable exception = null;
		try {
			runAntTaskWithExceptions();
		} catch (CoreException e) {
			exception = rootCause(e);
		}
		if (!exception.getMessage().contains("exists"))
			fail("Unexpected exception: ", exception);
	}

	/*
	 * Test that not-compressed attribute is honoured
	 */
	public void testNotCompressed() throws Exception {
		// Create Composite Repository Task
		AntTaskElement createCompositeTask = createCompositeRepositoryTaskElement(TYPE_ARTIFACT);
		addTask(createCompositeTask);
		// Set the compressed attribute to false
		((AntTaskElement) createCompositeTask.elements.get(0)).addAttributes(new String[] {"compressed", String.valueOf(false)});
		runAntTask();

		ICompositeRepository repo = getCompositeRepository(TYPE_ARTIFACT);
		assertTrue(repo instanceof CompositeArtifactRepository);
		assertFalse("The repository is compressed", Boolean.valueOf((String) repo.getProperties().get(IRepository.PROP_COMPRESSED)));
	}

	/*
	 * Test that atomic attribute of an artifact repo is honoured
	 */
	public void testAtomicArtifactRepository() throws Exception {
		// Create Composite Artifact Repository Task
		AntTaskElement createCompositeArtifactRepositoryTask = new AntTaskElement("p2.composite.artifact.repository.create");
		// Set the atomic attribute to true
		createCompositeArtifactRepositoryTask.addAttributes(new String[] {"atomic", String.valueOf(true)});
		createCompositeArtifactRepositoryTask.addAttributes(new String[] {"location", URIUtil.toUnencodedString(compositeSite)});
		addTask(createCompositeArtifactRepositoryTask);
		runAntTask();

		ICompositeRepository repo = getCompositeRepository(TYPE_ARTIFACT);
		assertTrue(repo instanceof CompositeArtifactRepository);
		assertTrue("The repository is not atomic", Boolean.valueOf((String) repo.getProperties().get(CompositeArtifactRepository.PROP_ATOMIC_LOADING)));
	}

	/*
	 * Test that atomic attribute of a metadata repo is honoured
	 */
	public void testAtomicMetadataRepository() throws Exception {
		// Create Composite Metadata Repository Task
		AntTaskElement createCompositeMetadataRepositoryTask = new AntTaskElement("p2.composite.metadata.repository.create");
		// Set the atomic attribute to true
		createCompositeMetadataRepositoryTask.addAttributes(new String[] {"atomic", String.valueOf(true)});
		createCompositeMetadataRepositoryTask.addAttributes(new String[] {"location", URIUtil.toUnencodedString(compositeSite)});
		addTask(createCompositeMetadataRepositoryTask);
		runAntTask();

		ICompositeRepository repo = getCompositeRepository(TYPE_METADATA);
		assertTrue(repo instanceof CompositeMetadataRepository);
		assertTrue("The repository is not atomic", Boolean.valueOf((String) repo.getProperties().get(CompositeArtifactRepository.PROP_ATOMIC_LOADING)));
	}

	/*
	 * Test that the name is properly set on a newly created repository
	 */
	public void testName() {
		String repoName = "My Test Repository";
		// Create Composite Repository Task
		AntTaskElement createCompositeTask = createCompositeRepositoryTaskElement(TYPE_ARTIFACT);
		addTask(createCompositeTask);
		// Set the repository name
		((AntTaskElement) createCompositeTask.elements.get(0)).addAttributes(new String[] {"name", repoName});

		runAntTask();

		try {
			IArtifactRepository repo = getArtifactRepositoryManager().loadRepository(compositeSite, null);
			assertTrue(repo instanceof CompositeArtifactRepository);
			assertEquals(repoName, repo.getName());
		} catch (ProvisionException e) {
			fail("Failed to load repository", e);
		}
	}

	/*
	 * Test adding a child to a new artifact repository
	 */
	public void testAddChildToNewArtifactRepository() {
		// Create Composite Repository Task
		AntTaskElement createCompositeTask = createCompositeRepositoryTaskElement(TYPE_ARTIFACT);
		addTask(createCompositeTask);

		// Create add element
		AntTaskElement addElement = new AntTaskElement("add");
		// Add a repository
		addElement.addElement(getRepositoryElement(childSite, TYPE_ARTIFACT));
		createCompositeTask.addElement(addElement);

		runAntTask();

		try {
			CompositeArtifactRepository repo = (CompositeArtifactRepository) getArtifactRepositoryManager().loadRepository(compositeSite, null);
			assertTrue(repo.getChildren().contains(childSite));
			assertEquals("More than one child present", 1, repo.getChildren().size());
		} catch (ProvisionException e) {
			fail("Failed to load repository", e);
		}
	}

	/*
	 * Test adding a child to a new metadata repository
	 */
	public void testAddChildToNewMetadataRepository() {
		// Create Composite Repository Task
		AntTaskElement createCompositeTask = createCompositeRepositoryTaskElement(TYPE_METADATA);
		addTask(createCompositeTask);

		// Create add element
		AntTaskElement addElement = new AntTaskElement("add");
		// Add a repository
		addElement.addElement(getRepositoryElement(childSite, TYPE_METADATA));
		createCompositeTask.addElement(addElement);

		runAntTask();

		try {
			ICompositeRepository repo = (ICompositeRepository) getMetadataRepositoryManager().loadRepository(compositeSite, null);
			assertTrue(repo.getChildren().contains(childSite));
			assertEquals("More than one child present", 1, repo.getChildren().size());
		} catch (ProvisionException e) {
			fail("Failed to load repository", e);
		}
	}

	/*
	 * Test how the task behaves with an invalid location
	 */
	public void testInvalidLocation() throws Exception {
		URI location = URIUtil.fromString("scheme:/location");
		AntTaskElement createCompositeTask = new AntTaskElement("p2.composite.repository");
		createCompositeTask.addElement(getRepositoryElement(location, TYPE_ARTIFACT));
		addTask(createCompositeTask);

		Exception exception = null;
		try {
			runAntTaskWithExceptions();
		} catch (CoreException e) {
			exception = e;
			try {
				getArtifactRepositoryManager().loadRepository(location, null);
				fail("Repository with invalid location loaded.");
			} catch (ProvisionException e2) {
				// This is a success
			}
		}
		if (exception == null)
			fail("No exception thrown");
	}

	/*
	 * Get the composite repository at the default location
	 */
	protected ICompositeRepository getCompositeRepository(String type) {
		try {
			if (type == TYPE_ARTIFACT) {
				return (ICompositeRepository) getArtifactRepositoryManager().loadRepository(compositeSite, null);
			} else if (type == TYPE_METADATA)
				return (ICompositeRepository) getMetadataRepositoryManager().loadRepository(compositeSite, null);
			else
				fail("No type specified");
		} catch (ProvisionException e) {
			fail("Failed to load repository", e);
		} catch (ClassCastException e) {
			fail("Repository is not composite", e);
		}
		// Will not occur
		return null;
	}

	/*
	 *  Create an "remove" AntTaskElement for the specified addresses
	 */
	protected AntTaskElement createRemoveElement(String type, URI[] addresses) {
		AntTaskElement add = new AntTaskElement(REMOVE_ELEMENT);
		for (int i = 0; i < addresses.length; i++)
			add.addElement(getRepositoryElement(addresses[i], type));
		return add;
	}

	/*
	 * Create an "add" AntTaskElement for the specified addresses
	 */
	protected AntTaskElement createAddElement(String type, URI[] addresses) {
		AntTaskElement add = new AntTaskElement(ADD_ELEMENT);
		for (int i = 0; i < addresses.length; i++)
			add.addElement(getRepositoryElement(addresses[i], type));
		return add;
	}

	/*
	 * Create an AntTaskElement representing a p2 composite repository task with the default repo location specified
	 */
	protected AntTaskElement createCompositeRepositoryTaskElement(String type) {
		AntTaskElement compositeTask = new AntTaskElement("p2.composite.repository");
		compositeTask.addElement(getRepositoryElement(compositeSite, type));

		return compositeTask;
	}

	/*
	 * Create a composite repository at the default location of the specified type(s)
	 */
	protected ICompositeRepository createCompositeRepository(String type) {
		ICompositeRepository repo = null;
		try {
			if (TYPE_ARTIFACT.equals(type) || type == null) {
				repo = (ICompositeRepository) RepositoryHelper.validDestinationRepository(getArtifactRepositoryManager().createRepository(compositeSite, "Test Composite Repo", IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null));
			}
			if (TYPE_METADATA.equals(type) || type == null) {
				repo = (ICompositeRepository) RepositoryHelper.validDestinationRepository(getMetadataRepositoryManager().createRepository(compositeSite, "Test Composite Repo", IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null));
			}
		} catch (ProvisionException e) {
			fail("Failed to create composite repository", e);
		} catch (IllegalStateException e) {
			fail("failed to create writeable composite repository", e);
		}
		return repo;
	}
}
