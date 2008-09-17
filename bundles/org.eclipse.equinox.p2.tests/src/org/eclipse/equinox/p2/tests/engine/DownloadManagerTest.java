/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.engine.DownloadManager;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Simple tests of {@link DownloadManager} API.
 */
public class DownloadManagerTest extends AbstractProvisioningTest {
	public static Test suite() {
		return new TestSuite(DownloadManagerTest.class);
	}

	/**
	 * Tests invocation of DownloadManager when there is nothing to download.
	 */
	public void testEmpty() {
		DownloadManager manager = new DownloadManager(null);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());
	}

	/**
	 * Tests invocation of DownloadManager when there is nothing to download.
	 */
	public void testEmptyWithContext() {
		ProvisioningContext context = new ProvisioningContext();
		DownloadManager manager = new DownloadManager(context);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());
	}

	/**
	 * Tests invocation of DownloadManager when there is nothing to download.
	 */
	public void testAddNullArtifactRequest() {
		ProvisioningContext context = new ProvisioningContext();
		DownloadManager manager = new DownloadManager(context);
		try {
			manager.add((IArtifactRequest) null);
		} catch (RuntimeException e) {
			return;
		}
		fail("1.0");
	}

	public void testAddNullArtifactRequestArray() {
		ProvisioningContext context = new ProvisioningContext();
		DownloadManager manager = new DownloadManager(context);
		try {
			manager.add((IArtifactRequest[]) null);
		} catch (RuntimeException e) {
			return;
		}
		fail("1.0");
	}

	public void testAddEmptyArtifactRequestArray() {
		ProvisioningContext context = new ProvisioningContext();
		DownloadManager manager = new DownloadManager(context);
		manager.add(new IArtifactRequest[0]);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());
	}

	public void testAddArtifactRequestArrayContainingNull() {
		ProvisioningContext context = new ProvisioningContext();
		DownloadManager manager = new DownloadManager(context);
		try {
			IArtifactRequest[] requests = new IArtifactRequest[] {null};
			manager.add(requests);
		} catch (RuntimeException e) {
			return;
		}
		fail("1.0");
	}

	public void testAddArtifactRequest() {
		ProvisioningContext context = new ProvisioningContext();
		DownloadManager manager = new DownloadManager(context);

		IArtifactRequest request = createArtifactRequest();
		manager.add(request);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());

	}

	public void testAddArtifactRequestArray() {
		ProvisioningContext context = new ProvisioningContext();
		DownloadManager manager = new DownloadManager(context);

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

			public IStatus getResult() {
				return Status.OK_STATUS;
			}
		};
		return request;
	}

	public void testEmptyArtifactRepositoryListContext() {
		ProvisioningContext context = new ProvisioningContext();
		context.setArtifactRepositories(new URL[0]);
		DownloadManager manager = new DownloadManager(context);

		IArtifactRequest[] requests = new IArtifactRequest[] {createArtifactRequest()};
		manager.add(requests);
		IStatus result = manager.start(null);
		assertEquals("1.0", IStatus.ERROR, result.getSeverity());
	}

	public void testFileFirstArtifactRepositoryListContext() {
		ProvisioningContext context = new ProvisioningContext();
		URL[] artifactRepos = new URL[2];
		try {
			artifactRepos[0] = new URL("file:/test");
			artifactRepos[1] = new URL("jar:file:/test!/");
		} catch (MalformedURLException e) {
			fail(e.getMessage());
		}

		context.setArtifactRepositories(artifactRepos);
		DownloadManager manager = new DownloadManager(context);

		IArtifactRequest[] requests = new IArtifactRequest[] {createArtifactRequest()};
		manager.add(requests);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());
	}

	public void testFileLastArtifactRepositoryListContext() {
		ProvisioningContext context = new ProvisioningContext();
		URL[] artifactRepos = new URL[2];
		try {
			artifactRepos[0] = new URL("jar:file:/test!/");
			artifactRepos[1] = new URL("file:/test");
		} catch (MalformedURLException e) {
			fail(e.getMessage());
		}

		context.setArtifactRepositories(artifactRepos);
		DownloadManager manager = new DownloadManager(context);

		IArtifactRequest[] requests = new IArtifactRequest[] {createArtifactRequest()};
		manager.add(requests);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());
	}

	public void testNoFileArtifactRepositoryListContext() {
		ProvisioningContext context = new ProvisioningContext();
		URL[] artifactRepos = new URL[2];
		try {
			artifactRepos[0] = new URL("jar:file:/test1!/");
			artifactRepos[1] = new URL("jar:file:/test2!/");
		} catch (MalformedURLException e) {
			fail(e.getMessage());
		}

		context.setArtifactRepositories(artifactRepos);
		DownloadManager manager = new DownloadManager(context);

		IArtifactRequest[] requests = new IArtifactRequest[] {createArtifactRequest()};
		manager.add(requests);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());
	}
}
