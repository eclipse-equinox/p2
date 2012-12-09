/*******************************************************************************
 *  Copyright (c) 2010-2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rapicorp, Inc (Pascal Rapicault) - Bug 394156 - Add support for updates from one namespace to another
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.operations;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.operations.IStatusCodes;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

/**
 * Tests various aspects of install operations
 */
public class InstallOperationTests extends AbstractProvisioningUITest {
	public void testInstallerPlan() throws ProvisionException {
		URI uri = getTestData("InstallHandler", "testData/installPlan").toURI();
		Set<IInstallableUnit> ius = getMetadataRepositoryManager().loadRepository(uri, getMonitor()).query(QueryUtil.createIUQuery("A"), getMonitor()).toSet();
		assertTrue("One IU", ius.size() == 1);
		InstallOperation op = new InstallOperation(getSession(), ius);
		op.setProfileId(TESTPROFILE);
		ProvisioningContext pc = new ProvisioningContext(getAgent());
		pc.setArtifactRepositories(new URI[] {uri});
		pc.setMetadataRepositories(new URI[] {uri});
		op.setProvisioningContext(pc);
		assertTrue("Should resolve", op.resolveModal(getMonitor()).isOK());
		assertTrue("Should install", op.getProvisioningJob(null).runModal(getMonitor()).isOK());
		assertFalse("Action1 should have been installed", getProfile(TESTPROFILE).query(QueryUtil.createIUQuery("Action1"), getMonitor()).isEmpty());
	}

	public void testDetectMissingRequirement() throws ProvisionException, OperationCanceledException {
		URI uriA, uriB, uriC;
		IMetadataRepository repoA, repoB, repoC;
		String testDataFileLocation = "testData/provisioningContextTests/";
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

		String id = "TestProfileIDForMissingRequirement";
		createProfile(id);
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {repoA.getLocation()});
		context.setArtifactRepositories(new URI[0]);
		IInstallableUnit[] units = repoA.query(QueryUtil.createIUQuery("A"), getMonitor()).toArray(IInstallableUnit.class);
		assertTrue("should find A in main repo", units.length > 0);

		// NOW WE CAN TEST!
		assertNull("ProvisioningContext does not follow by default", context.getProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES));

		InstallOperation op = new InstallOperation(getSession(), Collections.singleton(units[0]));
		op.setProvisioningContext(context);
		op.setProfileId(id);
		assertTrue("Should resolve", op.resolveModal(getMonitor()).isOK());

		assertNotNull("Context was reset to follow", context.getProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES));

		getArtifactRepositoryManager().removeRepository(uriA);
		getArtifactRepositoryManager().removeRepository(uriB);
		getArtifactRepositoryManager().removeRepository(uriC);
		getMetadataRepositoryManager().removeRepository(uriA);
		getMetadataRepositoryManager().removeRepository(uriB);
		getMetadataRepositoryManager().removeRepository(uriC);

	}

	public void testUpdateWithNamespaceChange() {
		//Create the IU that will be detected as an update
		InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
		iud.setId("NewB");
		iud.setVersion(Version.create("1.0.0"));

		String orExpression = "providedCapabilities.exists(pc | pc.namespace == 'org.eclipse.equinox.p2.iu' && pc.name == 'B')";
		IExpression expr = ExpressionUtil.parse(orExpression);
		IMatchExpression<IInstallableUnit> matchExpression = ExpressionUtil.getFactory().matchExpression(expr);

		Collection<IMatchExpression<IInstallableUnit>> updateExpression = new ArrayList<IMatchExpression<IInstallableUnit>>();
		updateExpression.add(matchExpression);
		iud.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(updateExpression, IUpdateDescriptor.HIGH, (String) null, (URI) null));
		IInstallableUnit newIUB = MetadataFactory.createInstallableUnit(iud);

		//create the IU being updated
		IInstallableUnit installed = createIU("B");

		//Setup the profile
		installAsRoots(profile, new IInstallableUnit[] {installed}, true, createPlanner(), createEngine());

		List<IInstallableUnit> ius = new ArrayList<IInstallableUnit>();
		ius.add(newIUB);
		InstallOperation op = new InstallOperation(getSession(), ius);
		op.setProfileId(TESTPROFILE);

		IStatus resolutionStatus = op.resolveModal(getMonitor());
		assertEquals(IStatusCodes.PROFILE_CHANGE_ALTERED, ((MultiStatus) resolutionStatus).getChildren()[0].getCode());
		assertEquals(IStatusCodes.ALTERED_IMPLIED_UPDATE, ((MultiStatus) (((MultiStatus) resolutionStatus).getChildren()[0])).getChildren()[0].getCode());
	}
}
