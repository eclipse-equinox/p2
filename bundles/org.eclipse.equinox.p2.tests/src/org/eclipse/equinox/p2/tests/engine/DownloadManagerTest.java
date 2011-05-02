/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.net.URI;
import java.net.URISyntaxException;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.DownloadManager;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Simple tests of {@link DownloadManager} API.
 */
public class DownloadManagerTest extends AbstractProvisioningTest {
	private static final String testDataFileLocation = "testData/artifactRepo/simple/artifacts.xml";
	private static final String testDataFileZipLocation = "testData/artifactRepo/jarfiles/artifacts.jar";

	public static Test suite() {
		return new TestSuite(DownloadManagerTest.class);
	}

	/**
	 * Tests invocation of DownloadManager when there is nothing to download.
	 */
	public void testEmpty() {
		DownloadManager manager = createDownloadManager(null);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());
	}

	/**
	 * Tests invocation of DownloadManager when there is nothing to download.
	 */
	public void testEmptyWithContext() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		DownloadManager manager = createDownloadManager(context);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());
	}

	/**
	 * Tests invocation of DownloadManager when there is nothing to download.
	 */
	public void testAddNullArtifactRequest() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		DownloadManager manager = createDownloadManager(context);
		try {
			manager.add((IArtifactRequest) null);
		} catch (RuntimeException e) {
			return;
		}
		fail("1.0");
	}

	public void testAddNullArtifactRequestArray() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		DownloadManager manager = createDownloadManager(context);
		try {
			manager.add((IArtifactRequest[]) null);
		} catch (RuntimeException e) {
			return;
		}
		fail("1.0");
	}

	public void testAddEmptyArtifactRequestArray() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		DownloadManager manager = createDownloadManager(context);
		manager.add(new IArtifactRequest[0]);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());
	}

	public void testAddArtifactRequestArrayContainingNull() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		DownloadManager manager = createDownloadManager(context);
		try {
			IArtifactRequest[] requests = new IArtifactRequest[] {null};
			manager.add(requests);
		} catch (RuntimeException e) {
			return;
		}
		fail("1.0");
	}

	public void testAddArtifactRequest() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		DownloadManager manager = createDownloadManager(context);

		IArtifactRequest request = createArtifactRequest();
		manager.add(request);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());

	}

	public void testContext() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setArtifactRepositories(new URI[0]);
		DownloadManager manager = createDownloadManager(context);

		IArtifactRequest request = createArtifactRequest();
		manager.add(request);
		IStatus result = manager.start(null);
		assertFalse("1.0", result.isOK());
		assertNotNull(result.getException());
	}

	public void testAddArtifactRequestArray() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		DownloadManager manager = createDownloadManager(context);

		IArtifactRequest[] requests = new IArtifactRequest[] {createArtifactRequest()};
		manager.add(requests);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());

	}

	private IArtifactRequest createArtifactRequest() {
		IArtifactRequest request = new IArtifactRequest() {
			public IArtifactKey getArtifactKey() {
				return null;
			}

			public void perform(IArtifactRepository sourceRepository, IProgressMonitor monitor) {
				//do nothing
			}

			public IStatus getResult() {
				return Status.OK_STATUS;
			}
		};
		return request;
	}

	public void testEmptyArtifactRepositoryListContext() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setArtifactRepositories(new URI[0]);
		DownloadManager manager = createDownloadManager(context);

		IArtifactRequest[] requests = new IArtifactRequest[] {createArtifactRequest()};
		manager.add(requests);
		IStatus result = manager.start(null);
		assertEquals("1.0", IStatus.ERROR, result.getSeverity());
	}

	public void testFileFirstArtifactRepositoryListContext() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		URI[] artifactRepos = new URI[2];
		artifactRepos[0] = getTestData("Simple Artifact Repo", testDataFileLocation).toURI();
		artifactRepos[1] = URIUtil.toJarURI(getTestData("Simple Artifact Repo Zip", testDataFileZipLocation).toURI(), null);

		context.setArtifactRepositories(artifactRepos);
		DownloadManager manager = createDownloadManager(context);

		IArtifactRequest[] requests = new IArtifactRequest[] {createArtifactRequest()};
		manager.add(requests);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());

		// Right now the provisioning context adds these to the manager so
		// we have to remove them so as not to affect other tests.
		getArtifactRepositoryManager().removeRepository(artifactRepos[0]);
		getArtifactRepositoryManager().removeRepository(artifactRepos[1]);
	}

	public void testFileLastArtifactRepositoryListContext() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		URI[] artifactRepos = new URI[2];
		artifactRepos[0] = URIUtil.toJarURI(getTestData("Simple Artifact Repo Zip", testDataFileZipLocation).toURI(), null);
		artifactRepos[1] = getTestData("Simple Artifact Repo", testDataFileLocation).toURI();

		context.setArtifactRepositories(artifactRepos);
		DownloadManager manager = createDownloadManager(context);

		IArtifactRequest[] requests = new IArtifactRequest[] {createArtifactRequest()};
		manager.add(requests);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());

		// Right now the provisioning context adds these to the manager so
		// we have to remove them so as not to affect other tests.
		getArtifactRepositoryManager().removeRepository(artifactRepos[0]);
		getArtifactRepositoryManager().removeRepository(artifactRepos[1]);

	}

	public void testNoFileArtifactRepositoryListContext() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		URI[] artifactRepos = new URI[2];
		try {
			artifactRepos[0] = URIUtil.toJarURI(getTestData("Simple Artifact Repo Zip", testDataFileZipLocation).toURI(), null);
			// This file doesn't exist, but we need an absolute path anyway so we can remove the URI later
			String absolute = getTempFolder() + "/test2";
			artifactRepos[1] = URIUtil.toJarURI(URIUtil.fromString(absolute), null);
		} catch (URISyntaxException e) {
			fail(e.getMessage());
		}

		context.setArtifactRepositories(artifactRepos);
		DownloadManager manager = createDownloadManager(context);

		IArtifactRequest[] requests = new IArtifactRequest[] {createArtifactRequest()};
		manager.add(requests);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());

		// Right now the provisioning context adds these to the manager so
		// we have to remove them so as not to affect other tests.
		getArtifactRepositoryManager().removeRepository(artifactRepos[0]);
		getArtifactRepositoryManager().removeRepository(artifactRepos[1]);

	}

	private DownloadManager createDownloadManager(ProvisioningContext context) {
		return new DownloadManager(context, getAgent());
	}
}
