/*******************************************************************************
 *  Copyright (c) 2011, 2023 Wind River and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Wind River - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug351944 extends AbstractProvisioningTest {

	File artifactRepoFile = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		File testData = AbstractProvisioningTest.getTestData("artifact repository", "testData/bug351944");
		artifactRepoFile = getTempFolder();
		copy("Copy to temporary folder", testData, artifactRepoFile);
		changeWritePermission(artifactRepoFile, false);
	}

	/**
	 * it doesn't work on Windows to make a folder read-only then can't create
	 * new file in it
	 */
	private void changeWritePermission(File target, boolean canWrite) {
		if (target.exists()) {
			target.setWritable(canWrite);
			if (target.isDirectory()) {
				for (File child : target.listFiles()) {
					child.setWritable(canWrite);
				}
			}
		}
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		changeWritePermission(artifactRepoFile, true);
		delete(artifactRepoFile);
	}

	public void testSimpleRepositoryPerformanceOnLoadReadonlyLocalRepository() throws ProvisionException {
		if (!Platform.OS_WIN32.equals(Platform.getOS())) {
			final URI testRepo = artifactRepoFile.toURI();
			IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
			IArtifactRepository repo = artifactRepositoryManager.loadRepository(testRepo, new NullProgressMonitor());
			IQueryResult<IArtifactKey> allArtifactKeys = repo.query(ArtifactKeyQuery.ALL_KEYS, new NullProgressMonitor());
			Set<IArtifactKey> keySet = allArtifactKeys.toUnmodifiableSet();

			Collection<IArtifactRequest> requests = new ArrayList<>();
			for (IArtifactKey key : keySet)
				requests.add(artifactRepositoryManager.createMirrorRequest(key, repo, null, null));

			long start = System.currentTimeMillis();
			Collection<IArtifactRequest> toBeRequests = getRequestsForRepository(repo,
					requests.toArray(new IArtifactRequest[requests.size()]));
			long end = System.currentTimeMillis();
			long queryArtifactOneByOne = end - start;

			start = System.currentTimeMillis();
			Collection<IArtifactRequest> toBeRequests2 = getRequestsForRepository2(repo,
					requests.toArray(new IArtifactRequest[requests.size()]));
			end = System.currentTimeMillis();
			long queryAllArtifacts = end - start;

			assertEquals("Test case has problem, not find same requests.", toBeRequests.size(), toBeRequests2.size());
			assertEquals("Querying artifact key from simple repository has performance issue.", queryAllArtifacts, queryArtifactOneByOne, 10);
		}
	}

	/**
	 * copy from {@link org.eclipse.equinox.internal.p2.engine.DownloadManager}
	 * @param repository
	 * @param requestsToProcess
	 * @return
	 */
	private Collection<IArtifactRequest> getRequestsForRepository(IArtifactRepository repository,
			IArtifactRequest[] requestsToProcess) {
		List<IArtifactRequest> applicable = new ArrayList<>();
		for (IArtifactRequest request : requestsToProcess) {
			if (repository.contains(request.getArtifactKey()))
				applicable.add(request);
		}
		return applicable;
	}

	private Collection<IArtifactRequest> getRequestsForRepository2(IArtifactRepository repository,
			IArtifactRequest[] requestsToProcess) {
		Set<IArtifactKey> keys = repository.query(ArtifactKeyQuery.ALL_KEYS, new NullProgressMonitor()).toSet();
		ArrayList<IArtifactRequest> applicable = new ArrayList<>();
		for (IArtifactRequest request : requestsToProcess) {
			if (keys.contains(request.getArtifactKey()))
				applicable.add(request);
		}
		return applicable;
	}
}
