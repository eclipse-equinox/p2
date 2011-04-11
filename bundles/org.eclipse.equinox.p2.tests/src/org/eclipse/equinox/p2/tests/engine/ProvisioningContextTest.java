/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.query.ExpressionMatchQuery;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Test API of the provisioning context
 */
public class ProvisioningContextTest extends AbstractProvisioningTest {
	protected File repoLocation;
	protected File referredRepoLocation1;
	protected File referredRepoLocation2;
	protected Set<IRepositoryReference> metadataRepoSnapshot;
	protected Set<IRepositoryReference> artifactRepoSnapshot;
	protected int metadataRepoCount, artifactRepoCount;

	protected void setUp() throws Exception {
		super.setUp();
		repoLocation = getTempFolder();
		referredRepoLocation1 = getTempFolder();
		referredRepoLocation2 = getTempFolder();

		// we add one metadata and one artifact repository at repoLocation
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, "false");
		URI repoURL = repoLocation.toURI();
		IMetadataRepository repo = getMetadataRepositoryManager().createRepository(repoURL, "testContextAllReposWithFollow", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=305565
		getArtifactRepositoryManager().createRepository(repoURL, "testContextAllReposWithFollow", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);

		// now create one metadata and one artifact repository at referredRepoLocation1.
		// remove them (so they manager doesn't know them) but refer to them from the first repo as disabled repos
		URI repoFollowed1 = referredRepoLocation1.toURI();
		IMetadataRepository repo1 = getMetadataRepositoryManager().createRepository(repoFollowed1, "referred1", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		getMetadataRepositoryManager().removeRepository(repoFollowed1);
		getArtifactRepositoryManager().createRepository(repoFollowed1, "referred1", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		getArtifactRepositoryManager().removeRepository(repoFollowed1);
		repo.addReferences(Collections.singletonList(new RepositoryReference(repoFollowed1, "follow1", IRepository.TYPE_METADATA, IRepository.NONE)));
		repo.addReferences(Collections.singletonList(new RepositoryReference(repoFollowed1, "follow1", IRepository.TYPE_ARTIFACT, IRepository.NONE)));

		// do the same at referredRepoLocation2 (create repos and remove them).
		// add them as disabled references in repo 1.
		URI repoFollowed2 = referredRepoLocation2.toURI();
		getMetadataRepositoryManager().createRepository(repoFollowed2, "referred2", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		getMetadataRepositoryManager().removeRepository(repoFollowed2);
		getArtifactRepositoryManager().createRepository(repoFollowed2, "referred2", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		getArtifactRepositoryManager().removeRepository(repoFollowed2);
		repo1.addReferences(Collections.singletonList(new RepositoryReference(repoFollowed2, "follow2", IRepository.TYPE_METADATA, IRepository.NONE)));
		repo1.addReferences(Collections.singletonList(new RepositoryReference(repoFollowed2, "follow2", IRepository.TYPE_ARTIFACT, IRepository.NONE)));
	}

	protected void tearDown() throws Exception {
		getMetadataRepositoryManager().removeRepository(repoLocation.toURI());
		getArtifactRepositoryManager().removeRepository(repoLocation.toURI());
		getMetadataRepositoryManager().removeRepository(referredRepoLocation1.toURI());
		getArtifactRepositoryManager().removeRepository(referredRepoLocation1.toURI());
		getMetadataRepositoryManager().removeRepository(referredRepoLocation2.toURI());
		getArtifactRepositoryManager().removeRepository(referredRepoLocation2.toURI());
		delete(repoLocation);
		delete(referredRepoLocation1);
		delete(referredRepoLocation2);

		super.tearDown();
	}

	protected void snapShot() throws ProvisionException {
		// First load all metadata repositories that are enabled so that any enabled references they have will get added.
		// This ensures our snapshot is a valid count and that differences are only the result of the provisioning context (vs. loading).
		URI[] locations = getMetadataRepositoryManager().getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (int i = 0; i < locations.length; i++)
			getMetadataRepositoryManager().loadRepository(locations[i], getMonitor());

		metadataRepoCount = 0;
		metadataRepoSnapshot = new HashSet<IRepositoryReference>();
		List<URI> all = new ArrayList<URI>();
		all.addAll(Arrays.asList(getMetadataRepositoryManager().getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL)));
		all.addAll(Arrays.asList(getMetadataRepositoryManager().getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED)));

		for (URI location : all) {
			int options = getMetadataRepositoryManager().isEnabled(location) ? IRepository.ENABLED : IRepository.NONE;
			if (options == IRepository.ENABLED)
				metadataRepoCount++;
			metadataRepoSnapshot.add(new RepositoryReference(location, getMetadataRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NICKNAME), IRepository.TYPE_METADATA, options));
		}
		artifactRepoCount = 0;
		artifactRepoSnapshot = new HashSet<IRepositoryReference>();
		all = new ArrayList<URI>();
		all.addAll(Arrays.asList(getArtifactRepositoryManager().getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL)));
		all.addAll(Arrays.asList(getArtifactRepositoryManager().getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED)));
		for (URI location : all) {
			int options = getArtifactRepositoryManager().isEnabled(location) ? IRepository.ENABLED : IRepository.NONE;
			if (options == IRepository.ENABLED)
				artifactRepoCount++;
			artifactRepoSnapshot.add(new RepositoryReference(location, getArtifactRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NICKNAME), IRepository.TYPE_ARTIFACT, options));
		}
	}

	protected void checkSnapShots() {
		List<URI> all = new ArrayList<URI>();
		all.addAll(Arrays.asList(getMetadataRepositoryManager().getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL)));
		all.addAll(Arrays.asList(getMetadataRepositoryManager().getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED)));
		assertEquals("snapshot check 1.0", all.size(), metadataRepoSnapshot.size());
		for (URI location : all) {
			IRepositoryReference reference = snapshotFor(location, metadataRepoSnapshot);
			assertNotNull("snapshot check 1.1", reference);
			assertEquals("snapshot check 1.2", getMetadataRepositoryManager().isEnabled(location), (reference.getOptions() & IRepository.ENABLED) == IRepository.ENABLED);
			assertEquals("snapshot check 1.3", getMetadataRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NICKNAME), reference.getNickname());
		}
		all = new ArrayList<URI>();
		all.addAll(Arrays.asList(getArtifactRepositoryManager().getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL)));
		all.addAll(Arrays.asList(getArtifactRepositoryManager().getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED)));
		assertEquals("snapshot check 1.4", all.size(), artifactRepoSnapshot.size());
		for (URI location : all) {
			IRepositoryReference reference = snapshotFor(location, artifactRepoSnapshot);
			assertNotNull("snapshot check 1.5", reference);
			assertEquals("snapshot check 1.6", getArtifactRepositoryManager().isEnabled(location), (reference.getOptions() & IRepository.ENABLED) == IRepository.ENABLED);
			assertEquals("snapshot check 1.7", getArtifactRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NICKNAME), reference.getNickname());
		}
	}

	protected IRepositoryReference snapshotFor(URI location, Set<IRepositoryReference> refs) {
		for (IRepositoryReference ref : refs) {
			if (URIUtil.sameURI(location, ref.getLocation())) {
				return ref;
			}
		}
		return null;
	}

	protected int getRepositoryCount(IRepositoryManager manager) {
		URI[] enabled = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		URI[] disabled = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED);
		return enabled.length + disabled.length;
	}

	public void testContextAllReposNoFollow() throws ProvisionException {
		snapShot();
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.getMetadata(getMonitor());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);
		// we only followed what was snapshot and enabled
		// It is possible that we didn't follow everything because we don't know for sure that every repo was
		// reachable.  The best we can test is that we followed no more than the enabled, but possibly less
		assertTrue("1.1", artifactRepoCount >= followed.length);
		// nothing should have changed
		checkSnapShots();
	}

	public void testContextAllReposWithFollow() throws ProvisionException {
		snapShot();
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.getMetadata(getMonitor());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);

		int allThatLoaded = followed.length;
		// Now try again following references
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, "true");
		context.getMetadata(getMonitor());
		followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);

		// We should have followed at least two additional repositories (we don't know what disabled references might be in other repos)
		assertTrue("1.1", followed.length >= allThatLoaded + 2);
		checkSnapShots();
	}

	public void testContextOneRepoNoFollow() throws ProvisionException {
		snapShot();
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoLocation.toURI()});
		context.setArtifactRepositories(new URI[] {repoLocation.toURI()});
		context.getMetadata(getMonitor());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);
		assertEquals("1.1", 1, followed.length);
		// No repos should have been added since we didn't follow references and used only our repo
		checkSnapShots();
	}

	public void testContextOneRepoWithFollow() throws ProvisionException {
		snapShot();
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoLocation.toURI()});
		context.setArtifactRepositories(new URI[] {repoLocation.toURI()});
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, "true");
		context.getMetadata(getMonitor());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);
		// three repositories, not just one
		assertEquals("1.1", 3, followed.length);
		checkSnapShots();
	}

	public void testContextTwoRepoNoFollow() throws ProvisionException {
		snapShot();
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoLocation.toURI(), referredRepoLocation1.toURI()});
		context.setArtifactRepositories(new URI[] {repoLocation.toURI(), referredRepoLocation1.toURI()});
		assertFalse("1.1", getArtifactRepositoryManager().contains(referredRepoLocation1.toURI()));
		assertFalse("1.2", getMetadataRepositoryManager().contains(referredRepoLocation1.toURI()));
		assertFalse("1.3", getArtifactRepositoryManager().contains(referredRepoLocation2.toURI()));
		assertFalse("1.4", getMetadataRepositoryManager().contains(referredRepoLocation2.toURI()));

		context.getMetadata(getMonitor());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);

		// With no following, we keep the legacy behavior.  Putting a repo in the context will cause it to be loaded (and added)
		// The snapshot is not the same.  We should see referredRepoLocation1 in the manager because it was explicitly added to the context
		assertEquals("1.5", 2, followed.length);
		assertTrue("1.6", getArtifactRepositoryManager().isEnabled(referredRepoLocation1.toURI()));
		assertTrue("1.7", getMetadataRepositoryManager().isEnabled(referredRepoLocation1.toURI()));

		// referredRepoLocation2 was added by reference due to loading of referredRepoLocation1, but should not be enabled
		assertFalse("1.8", getArtifactRepositoryManager().isEnabled(referredRepoLocation2.toURI()));
		assertFalse("1.9", getMetadataRepositoryManager().isEnabled(referredRepoLocation2.toURI()));
	}

	public void testContextTwoRepoWithFollow() throws ProvisionException {
		snapShot();
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoLocation.toURI(), referredRepoLocation1.toURI()});
		context.setArtifactRepositories(new URI[] {repoLocation.toURI(), referredRepoLocation1.toURI()});
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, "true");

		assertFalse("1.1", getArtifactRepositoryManager().contains(referredRepoLocation1.toURI()));
		assertFalse("1.2", getMetadataRepositoryManager().contains(referredRepoLocation1.toURI()));
		assertFalse("1.3", getArtifactRepositoryManager().contains(referredRepoLocation2.toURI()));
		assertFalse("1.4", getMetadataRepositoryManager().contains(referredRepoLocation2.toURI()));

		context.getMetadata(getMonitor());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);

		// three repositories, not just two
		assertEquals("1.5", 3, followed.length);

		// however putting the referred location in the provisioning context caused it to get added.
		assertTrue("1.6", getArtifactRepositoryManager().isEnabled(referredRepoLocation1.toURI()));
		assertTrue("1.7", getMetadataRepositoryManager().isEnabled(referredRepoLocation1.toURI()));

		// referredRepoLocation2 was added by reference due to loading of referredRepoLocation1, but should not be enabled
		assertFalse("1.8", getArtifactRepositoryManager().isEnabled(referredRepoLocation2.toURI()));
		assertFalse("1.9", getMetadataRepositoryManager().isEnabled(referredRepoLocation2.toURI()));
	}

	public void testContextThreeRepoNoFollow() throws ProvisionException {
		snapShot();
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoLocation.toURI(), referredRepoLocation1.toURI(), referredRepoLocation2.toURI()});
		context.setArtifactRepositories(new URI[] {repoLocation.toURI(), referredRepoLocation1.toURI(), referredRepoLocation2.toURI()});
		assertFalse("1.1", getArtifactRepositoryManager().contains(referredRepoLocation1.toURI()));
		assertFalse("1.2", getMetadataRepositoryManager().contains(referredRepoLocation1.toURI()));
		assertFalse("1.3", getArtifactRepositoryManager().contains(referredRepoLocation2.toURI()));
		assertFalse("1.4", getMetadataRepositoryManager().contains(referredRepoLocation2.toURI()));

		context.getMetadata(getMonitor());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);

		// With no following, we keep the legacy behavior.  Putting a repo in the context will cause it to be loaded (and added)
		// The snapshot is not the same.  We should see referredRepoLocation1 and referredRepoLocaiton2 in the manager because both were 
		// explicitly added to the context
		assertEquals("1.5", 3, followed.length);
		assertTrue("1.6", getArtifactRepositoryManager().isEnabled(referredRepoLocation1.toURI()));
		assertTrue("1.7", getMetadataRepositoryManager().isEnabled(referredRepoLocation1.toURI()));
		assertFalse("1.8", getArtifactRepositoryManager().isEnabled(referredRepoLocation2.toURI()));
		assertFalse("1.9", getMetadataRepositoryManager().isEnabled(referredRepoLocation2.toURI()));
	}

	public void testContextThreeRepoWithFollow() throws ProvisionException {
		snapShot();
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoLocation.toURI(), referredRepoLocation1.toURI(), referredRepoLocation2.toURI()});
		context.setArtifactRepositories(new URI[] {repoLocation.toURI(), referredRepoLocation1.toURI(), referredRepoLocation2.toURI()});
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, "true");

		assertFalse("1.1", getArtifactRepositoryManager().contains(referredRepoLocation1.toURI()));
		assertFalse("1.2", getMetadataRepositoryManager().contains(referredRepoLocation1.toURI()));
		assertFalse("1.3", getArtifactRepositoryManager().contains(referredRepoLocation2.toURI()));
		assertFalse("1.4", getMetadataRepositoryManager().contains(referredRepoLocation2.toURI()));

		context.getMetadata(getMonitor());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);

		// three repositories followed
		assertEquals("1.5", 3, followed.length);

		// however putting all referred locations in the provisioning context caused them to get added.
		assertTrue("1.6", getArtifactRepositoryManager().isEnabled(referredRepoLocation1.toURI()));
		assertTrue("1.7", getMetadataRepositoryManager().isEnabled(referredRepoLocation1.toURI()));
		assertFalse("1.8", getArtifactRepositoryManager().isEnabled(referredRepoLocation2.toURI()));
		assertFalse("1.9", getMetadataRepositoryManager().isEnabled(referredRepoLocation2.toURI()));
	}

	public void testContextNoReposNoFollow() throws ProvisionException {
		snapShot();
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setArtifactRepositories(new URI[0]);
		context.setMetadataRepositories(new URI[0]);
		context.getMetadata(getMonitor());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);
		assertEquals("1.1", 0, followed.length);
		checkSnapShots();
	}

	public void testContextNoReposWithFollow() throws ProvisionException {
		snapShot();
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, "true");
		context.setArtifactRepositories(new URI[0]);
		context.setMetadataRepositories(new URI[0]);
		context.getMetadata(getMonitor());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);
		assertEquals("1.1", 0, followed.length);
		checkSnapShots();
	}
}
