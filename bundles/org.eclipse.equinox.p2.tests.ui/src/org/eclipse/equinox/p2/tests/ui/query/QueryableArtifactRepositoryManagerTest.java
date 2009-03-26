package org.eclipse.equinox.p2.tests.ui.query;

import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.ui.QueryableArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.RepositoryLocationQuery;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;

public class QueryableArtifactRepositoryManagerTest extends AbstractQueryTest {
	private static final String repositoryOne = "http://one.lan";
	private static final String repositoryTwo = "http://two.lan";
	private int repoCount = 0;

	public void setUp() throws Exception {
		super.setUp();

		IArtifactRepositoryManager repoManager = getArtifactRepositoryManager();
		repoManager.addRepository(URIUtil.fromString(repositoryOne));
		repoManager.addRepository(URIUtil.fromString(repositoryTwo));
		// In case other repositories already exist in the manager.
		repoCount = repoManager.getKnownRepositories(0).length;
	}

	public void tearDown() throws Exception {
		IArtifactRepositoryManager repoManager = getArtifactRepositoryManager();
		repoManager.removeRepository(URIUtil.fromString(repositoryOne));
		repoManager.removeRepository(URIUtil.fromString(repositoryTwo));

		super.tearDown();
	}

	public void testQuery() {
		QueryableArtifactRepositoryManager manager = getQueryableManager();

		Collector result = new Collector();
		manager.query(new RepositoryLocationQuery(), result, getMonitor());
		assertTrue(result.size() == repoCount);
	}

	private QueryableArtifactRepositoryManager getQueryableManager() {
		return new QueryableArtifactRepositoryManager(Policy.getDefault().getQueryContext(), false);
	}
}
