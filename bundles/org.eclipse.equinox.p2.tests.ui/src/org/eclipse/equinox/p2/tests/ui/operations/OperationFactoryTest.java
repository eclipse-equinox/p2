/*******************************************************************************
 *  Copyright (c) 2011 Sonatype, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.tests.ui.operations;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class OperationFactoryTest extends AbstractProvisioningTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testCreateInstallOperation() {
		OperationFactory of = new OperationFactory();
		Collection<IVersionedId> versions = new ArrayList<IVersionedId>();
		versions.add(new VersionedId("aBundle", "1.0.0"));
		Collection<URI> coll = new ArrayList<URI>();
		coll.add(getTestData("Simple repository", "testData/testRepos/simple.1").toURI());
		try {
			of.createInstallOperation(versions, coll, new NullProgressMonitor());
		} catch (ProvisionException e) {
			fail("Creation of the install operation failed", e);
		}
	}

	public void testCreateInstallOperationWithUnspecifiedVersion() {
		OperationFactory of = new OperationFactory();
		Collection<IVersionedId> versions = new ArrayList<IVersionedId>();
		versions.add(new VersionedId("aBundle", (Version) null));
		Collection<URI> coll = new ArrayList<URI>();
		coll.add(getTestData("Simple repository", "testData/testRepos/simple.1").toURI());
		try {
			of.createInstallOperation(versions, coll, new NullProgressMonitor());
		} catch (ProvisionException e) {
			fail("Creation of the install operation failed", e);
		}
	}

	public void testMissingVersionedIdInInstallOperation() {
		OperationFactory of = new OperationFactory();
		Collection<IVersionedId> versions = new ArrayList<IVersionedId>();
		versions.add(new VersionedId("aBundle", "1.0.0"));
		Exception exceptionMet = null;
		try {
			of.createInstallOperation(versions, Collections.<URI> emptyList(), new NullProgressMonitor());
		} catch (ProvisionException e) {
			exceptionMet = e;
		}
		assertNotNull("An exception was expected", exceptionMet);
	}

	public void testMissingElementToUninstall() {
		createProfileWithOneIU(getUniqueString());

		//While we are at it, test the installedElements
		OperationFactory of = new OperationFactory();
		IQueryResult<IInstallableUnit> installedElements = of.listInstalledElements(true, new NullProgressMonitor());
		assertEquals(1, installedElements.toSet().size());

		//Now test various removal scenarios
		testUninstall(new VersionedId("aBundle", (Version) null), false);
		testUninstall(new VersionedId("aBundle", Version.create("1.0.0")), false);
		testUninstall(new VersionedId("aBundle", Version.create("2.0.0")), true);
		testUninstall(new VersionedId("missingBundle", (Version) null), true);
		testUninstall(installedElements.toSet().iterator().next(), false);
		testUninstall(createEclipseIU("doesNotExist"), true);
	}

	private void createProfileWithOneIU(String profileName) {
		//create a profile and set it as self
		try {
			IProfileRegistry profileRegistry = getProfileRegistry();
			profileRegistry.addProfile(profileName);
			Field selfField = SimpleProfileRegistry.class.getDeclaredField("self"); //$NON-NLS-1$
			selfField.setAccessible(true);
			previousSelfValue = selfField.get(profileRegistry);
			selfField.set(profileRegistry, profileName);
		} catch (Exception e) {
			fail("Error while setting up uninstall test", e);
		}

		//install something using the install operation
		OperationFactory of = new OperationFactory();
		Collection<IVersionedId> versions = new ArrayList<IVersionedId>();
		versions.add(new VersionedId("aBundle", (Version) null));
		Collection<URI> coll = new ArrayList<URI>();
		coll.add(getTestData("Simple repository", "testData/testRepos/simple.1").toURI());
		try {
			InstallOperation iop = of.createInstallOperation(versions, coll, new NullProgressMonitor());
			IStatus result = iop.resolveModal(new NullProgressMonitor());
			if (result.isOK()) {
				iop.getProvisioningJob(new NullProgressMonitor()).runModal(new NullProgressMonitor());
			}
		} catch (ProvisionException e) {
			fail("Creation of the install operation failed", e);
		}

	}

	private void testUninstall(IVersionedId vid, boolean shouldFail) {
		OperationFactory of = new OperationFactory();
		Collection<IVersionedId> toRemove = new ArrayList<IVersionedId>();
		toRemove.add(vid);
		Exception expectedException = null;
		try {
			of.createUninstallOperation(toRemove, Collections.<URI> emptyList(), new NullProgressMonitor());
		} catch (ProvisionException e) {
			expectedException = e;
		}
		if (shouldFail)
			assertNotNull(expectedException);
		else
			assertNull(expectedException);
	}

	public void testUpdateOperation() {
		createProfileWithOneIU(getUniqueString());
		OperationFactory of = new OperationFactory();
		{
			Collection<IVersionedId> toUpdate = new ArrayList<IVersionedId>();
			toUpdate.add(new VersionedId("doesNotExist", (Version) null));
			Collection<URI> repos = new ArrayList<URI>();
			repos.add(getTestData("second repository", "testData/testRepos/simple.2").toURI());
			Exception expectedException = null;
			try {
				of.createUpdateOperation(toUpdate, repos, new NullProgressMonitor());
			} catch (ProvisionException e) {
				expectedException = e;
			}
			assertNotNull(expectedException);
		}

		{
			Collection<IVersionedId> toUpdate = new ArrayList<IVersionedId>();
			toUpdate.add(new VersionedId("aBundle", Version.parseVersion("1.0.0")));
			Collection<URI> repos = new ArrayList<URI>();
			repos.add(getTestData("second repository", "testData/testRepos/simple.2").toURI());
			try {
				UpdateOperation op = of.createUpdateOperation(toUpdate, repos, new NullProgressMonitor());
				op.resolveModal(new NullProgressMonitor());
				assertEquals(1, op.getPossibleUpdates().length);
			} catch (ProvisionException e) {
				fail("Exception not expected", e);
			}
		}

		{
			Collection<URI> repos = new ArrayList<URI>();
			repos.add(getTestData("second repository", "testData/testRepos/simple.2").toURI());
			try {
				UpdateOperation op = of.createUpdateOperation(null, repos, new NullProgressMonitor());
				op.resolveModal(new NullProgressMonitor());
				assertEquals(1, op.getPossibleUpdates().length);
			} catch (ProvisionException e) {
				fail("Exception not expected", e);
			}
		}
	}
}