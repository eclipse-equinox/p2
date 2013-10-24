/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.mirror;

import static org.eclipse.equinox.p2.tests.AbstractProvisioningTest.getTestData;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.util.*;
import junit.framework.AssertionFailedError;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.StringBufferStream;
import org.eclipse.equinox.p2.tests.TestAgentProvider;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for joint mirroring of units and artifacts. See other test classes for unit-only and artifact-only mirroring tests. 
 */
public class MirrorApplicationTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	@Rule
	public TestAgentProvider agentProvider = new TestAgentProvider();

	private File sourceRepoLocation;
	private File destRepoLocation;

	private MirrorApplication subject;

	@Before
	public void initRepositories() throws Exception {
		sourceRepoLocation = getTestData("0.0", "/testData/mirror/mirrorSourceRepo1 with space");
		destRepoLocation = tempFolder.newFolder("destRepo");
	}

	@Test
	public void testMirrorUnitWithArtifact() throws Exception {
		IInstallableUnit unit = getUnitFromSourceRepo("helloworld");

		subject = createApplication(sourceRepoLocation, destRepoLocation, true);
		subject.setSourceIUs(Arrays.asList(unit));
		runApplication(subject);

		assertThat(artifactsIn(destRepoLocation), is(Collections.singleton("helloworld")));
	}

	@Test
	public void testMirrorUnitWithoutArtifacts() throws Exception {
		IInstallableUnit unitWithoutArtifact = getUnitFromSourceRepo("a.jre");
		assertThat(unitWithoutArtifact.getArtifacts(), not(hasItem(any(IArtifactKey.class)))); // self-test // TODO use is(empty()) once Hamcrest 1.3 is available

		subject = createApplication(sourceRepoLocation, destRepoLocation, true);
		subject.setSourceIUs(Arrays.asList(unitWithoutArtifact));
		runApplication(subject);

		assertThat(artifactsIn(destRepoLocation), not(hasItem(any(String.class)))); // TODO use is(empty()) once Hamcrest 1.3 is available
	}

	private IInstallableUnit getUnitFromSourceRepo(String id) throws Exception {
		IMetadataRepository repository = agentProvider.getService(IMetadataRepositoryManager.class).loadRepository(sourceRepoLocation.toURI(), null);
		IQueryResult<IInstallableUnit> queryResult = repository.query(QueryUtil.createIUQuery(id), null);
		if (queryResult.isEmpty())
			throw new AssertionFailedError("No unit with ID '" + id + "' found in repository " + sourceRepoLocation);
		return queryResult.iterator().next();
	}

	private Set<String> artifactsIn(File repositoryLocation) throws Exception {
		IArtifactRepository repository = agentProvider.getService(IArtifactRepositoryManager.class).loadRepository(repositoryLocation.toURI(), null);
		return artifactsIn(repository);
	}

	private static Set<String> artifactsIn(IArtifactRepository repository) {
		Set<String> result = new HashSet<String>();
		for (IArtifactKey artifactKey : repository.query(QueryUtil.createMatchQuery(IArtifactKey.class, "true"), null).toUnmodifiableSet()) {
			result.add(artifactKey.getId());
		}
		return result;
	}

	private static MirrorApplication createApplication(File sourceLocation, File destLocation, Boolean append) {
		MirrorApplication app = new MirrorApplication();

		if (destLocation != null) {
			RepositoryDescriptor dest = createRepositoryDescriptor(destLocation.toURI(), append);
			app.addDestination(dest);
		}

		if (sourceLocation != null) {
			RepositoryDescriptor src = createRepositoryDescriptor(sourceLocation.toURI(), null);
			app.addSource(src);
		}
		return app;
	}

	private static RepositoryDescriptor createRepositoryDescriptor(URI location, Boolean append) {
		RepositoryDescriptor descriptor = new RepositoryDescriptor();
		descriptor.setLocation(location);
		if (append != null)
			descriptor.setAppend(append);
		return descriptor;
	}

	private static StringBuffer runApplication(MirrorApplication app) throws ProvisionException {
		StringBuffer buffer = new StringBuffer();
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream(buffer)));
			app.run(null);
		} finally {
			System.setOut(out);
		}
		return buffer;
	}

}
