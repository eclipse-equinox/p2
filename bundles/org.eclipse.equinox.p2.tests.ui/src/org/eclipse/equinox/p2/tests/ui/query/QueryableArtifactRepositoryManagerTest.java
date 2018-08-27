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
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import java.net.URI;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.ui.QueryableArtifactRepositoryManager;
import org.eclipse.equinox.internal.p2.ui.RepositoryLocationQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

public class QueryableArtifactRepositoryManagerTest extends AbstractQueryTest {
	private static final String repositoryOne = "http://one.lan";
	private static final String repositoryTwo = "http://two.lan";
	private int repoCount = 0;

	@Override
	public void setUp() throws Exception {
		super.setUp();

		IArtifactRepositoryManager repoManager = getArtifactRepositoryManager();
		repoManager.addRepository(URIUtil.fromString(repositoryOne));
		repoManager.addRepository(URIUtil.fromString(repositoryTwo));
		// In case other repositories already exist in the manager.
		repoCount = repoManager.getKnownRepositories(0).length;
	}

	@Override
	public void tearDown() throws Exception {
		IArtifactRepositoryManager repoManager = getArtifactRepositoryManager();
		repoManager.removeRepository(URIUtil.fromString(repositoryOne));
		repoManager.removeRepository(URIUtil.fromString(repositoryTwo));

		super.tearDown();
	}

	public void testQuery() {
		QueryableArtifactRepositoryManager manager = getQueryableManager();

		IQueryResult<URI> result = manager.locationsQueriable().query(new RepositoryLocationQuery(), getMonitor());
		assertTrue(queryResultSize(result) == repoCount);
	}

	private QueryableArtifactRepositoryManager getQueryableManager() {
		return new QueryableArtifactRepositoryManager(ProvisioningUI.getDefaultUI(), false);
	}
}
