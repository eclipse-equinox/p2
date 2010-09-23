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

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import java.net.URI;
import java.util.Collections;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Test API of the provisioning context
 */
public class ProvisioningContextTest extends AbstractProvisioningTest {

	private static final String testDataFileLocation = "testData/provisioningContextTests/";
	private static final int A_UNITCOUNT = 37;
	private static final String TEST = "TestProvisioningContextFollow";
	protected IMetadataRepository repoA, repoB, repoC;
	URI uriA, uriB, uriC;

	protected void setUp() throws Exception {
		super.setUp();
		uriA = getTestData("A", testDataFileLocation + "A").toURI();
		uriB = getTestData("B", testDataFileLocation + "B").toURI();
		uriC = getTestData("C", testDataFileLocation + "C").toURI();

		repoA = getMetadataRepositoryManager().loadRepository(uriA, getMonitor());
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=305565
		repoA.addReferences(Collections.singletonList(new RepositoryReference(uriA, null, IRepository.TYPE_ARTIFACT, IRepository.ENABLED)));

		// now create a second set of repos and refer from the first
		repoB = getMetadataRepositoryManager().loadRepository(uriB, getMonitor());
		repoB.addReferences(Collections.singletonList(new RepositoryReference(uriB, null, IRepository.TYPE_ARTIFACT, IRepository.ENABLED)));
		repoA.addReferences(Collections.singletonList(new RepositoryReference(repoB.getLocation(), null, IRepository.TYPE_METADATA, IRepository.ENABLED)));

		// this repo is referred by the previous one
		repoC = getMetadataRepositoryManager().loadRepository(uriC, getMonitor());
		repoC.addReferences(Collections.singletonList(new RepositoryReference(uriC, null, IRepository.TYPE_ARTIFACT, IRepository.ENABLED)));
		repoB.addReferences(Collections.singletonList(new RepositoryReference(repoC.getLocation(), null, IRepository.TYPE_METADATA, IRepository.ENABLED)));
	}

	protected void tearDown() throws Exception {
		getArtifactRepositoryManager().removeRepository(uriA);
		getArtifactRepositoryManager().removeRepository(uriB);
		getArtifactRepositoryManager().removeRepository(uriC);
		getMetadataRepositoryManager().removeRepository(uriA);
		getMetadataRepositoryManager().removeRepository(uriB);
		getMetadataRepositoryManager().removeRepository(uriC);
	}

	public void testContextOneRepoNoFollow() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoA.getLocation()});
		context.setArtifactRepositories(new URI[0]);
		IQueryable<IInstallableUnit> queryable = context.getMetadata(getMonitor());
		assertEquals("Only IUs from A", A_UNITCOUNT, queryable.query(QueryUtil.ALL_UNITS, getMonitor()).toUnmodifiableSet().size());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);
		// The immediate artifact repo reference was followed
		assertEquals("1 separately located artifact repos", 1, followed.length);
	}

	public void testContextOneRepoWithFollow() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoA.getLocation()});
		context.setArtifactRepositories(new URI[0]);
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, "true");
		IQueryable<IInstallableUnit> queryable = context.getMetadata(getMonitor());
		assertTrue("More IUs", queryable.query(QueryUtil.ALL_UNITS, getMonitor()).toUnmodifiableSet().size() >= A_UNITCOUNT + 2);
		IInstallableUnit[] units = queryable.query(QueryUtil.createIUQuery("B"), getMonitor()).toArray(IInstallableUnit.class);
		assertTrue("should find B", units.length > 0);
		units = queryable.query(QueryUtil.createIUQuery("C"), getMonitor()).toArray(IInstallableUnit.class);
		assertTrue("should find C", units.length > 0);
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);
		// The artifact repo reference was followed
		assertEquals("3 artifact repos", 3, followed.length);
	}

	public void testContextTwoRepoNoFollow() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoA.getLocation(), repoB.getLocation()});
		context.setArtifactRepositories(new URI[0]);
		IQueryable<IInstallableUnit> queryable = context.getMetadata(getMonitor());
		assertTrue("IUs from A and B", queryable.query(QueryUtil.ALL_UNITS, getMonitor()).toUnmodifiableSet().size() > A_UNITCOUNT);
		IInstallableUnit[] units = queryable.query(QueryUtil.createIUQuery("B"), getMonitor()).toArray(IInstallableUnit.class);
		assertTrue("should find B", units.length > 0);
		units = queryable.query(QueryUtil.createIUQuery("C"), getMonitor()).toArray(IInstallableUnit.class);
		assertFalse("should not find C", units.length > 0);
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);

		assertEquals("two artifact repos", 2, followed.length);
	}

	public void testContextTwoRepoWithFollow() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoA.getLocation(), repoB.getLocation()});
		context.setArtifactRepositories(new URI[0]);
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, "true");
		IQueryable<IInstallableUnit> queryable = context.getMetadata(getMonitor());
		assertTrue("More IUs", queryable.query(QueryUtil.ALL_UNITS, getMonitor()).toUnmodifiableSet().size() >= A_UNITCOUNT + 2);
		IInstallableUnit[] units = queryable.query(QueryUtil.createIUQuery("B"), getMonitor()).toArray(IInstallableUnit.class);
		assertTrue("should find B", units.length > 0);
		units = queryable.query(QueryUtil.createIUQuery("C"), getMonitor()).toArray(IInstallableUnit.class);
		assertTrue("should find C", units.length > 0);
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);

		assertEquals("three artifact repos", 3, followed.length);
	}

	public void testContextThreeRepoNoFollow() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoA.getLocation(), repoB.getLocation(), repoC.getLocation()});
		context.setArtifactRepositories(new URI[0]);
		IQueryable<IInstallableUnit> queryable = context.getMetadata(getMonitor());
		assertTrue("More IUs", queryable.query(QueryUtil.ALL_UNITS, getMonitor()).toUnmodifiableSet().size() >= A_UNITCOUNT + 2);
		IInstallableUnit[] units = queryable.query(QueryUtil.createIUQuery("B"), getMonitor()).toArray(IInstallableUnit.class);
		assertTrue("should find B", units.length > 0);
		units = queryable.query(QueryUtil.createIUQuery("C"), getMonitor()).toArray(IInstallableUnit.class);
		assertTrue("should find C", units.length > 0);
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);

		assertEquals("three artifact repos", 3, followed.length);
	}

	public void testContextThreeRepoWithFollow() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoA.getLocation(), repoB.getLocation(), repoC.getLocation()});
		context.setArtifactRepositories(new URI[0]);
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, "true");
		IQueryable<IInstallableUnit> queryable = context.getMetadata(getMonitor());
		assertTrue("More IUs", queryable.query(QueryUtil.ALL_UNITS, getMonitor()).toUnmodifiableSet().size() >= A_UNITCOUNT + 2);
		IInstallableUnit[] units = queryable.query(QueryUtil.createIUQuery("B"), getMonitor()).toArray(IInstallableUnit.class);
		assertTrue("should find B", units.length > 0);
		units = queryable.query(QueryUtil.createIUQuery("C"), getMonitor()).toArray(IInstallableUnit.class);
		assertTrue("should find C", units.length > 0);
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);

		assertEquals("three artifact repos", 3, followed.length);
	}

	public void testContextNoReposNoFollow() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setArtifactRepositories(new URI[0]);
		context.setMetadataRepositories(new URI[0]);
		context.getMetadata(getMonitor());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);
		assertEquals("1.1", 0, followed.length);
	}

	public void testContextNoReposWithFollow() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, "true");
		context.setArtifactRepositories(new URI[0]);
		context.setMetadataRepositories(new URI[0]);
		context.getMetadata(getMonitor());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] followed = context.getArtifactRepositories(getMonitor()).query(all, getMonitor()).toArray(IArtifactRepository.class);
		assertEquals("1.1", 0, followed.length);
	}

	public void testFollowHelpsResolve() {
		createProfile(TEST);
		IProfileChangeRequest request = ProfileChangeRequest.createByProfileId(getAgent(), TEST);
		IInstallableUnit[] units = repoA.query(QueryUtil.createIUQuery("A"), getMonitor()).toArray(IInstallableUnit.class);
		assertTrue("should find A in main repo", units.length > 0);
		request.add(units[0]);
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoA.getLocation()});
		context.setArtifactRepositories(new URI[0]);
		IProvisioningPlan plan = getPlanner(getAgent()).getProvisioningPlan(request, context, getMonitor());
		assertFalse("resolve should fail with missing requirements", plan.getStatus().isOK());
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, "true");
		plan = getPlanner(getAgent()).getProvisioningPlan(request, context, getMonitor());
		assertTrue("resolve should pass", plan.getStatus().isOK());
	}
}
