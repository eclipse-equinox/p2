/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.sharedinstall;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.migration.MigrationSupport;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.*;

public class NeedsMigration extends AbstractProvisioningTest {
	@IUDescription(content = "package: sdk \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit sdk1;

	@IUDescription(content = "package: sdk \n" + "singleton: true\n" + "version: 2 \n")
	public IInstallableUnit sdk2;

	@IUDescription(content = "package: egit \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit egit1;

	@IUDescription(content = "package: egit \n" + "singleton: true\n" + "version: 2 \n")
	public IInstallableUnit egit2;

	@IUDescription(content = "package: cdt \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit cdt1;

	@IUDescription(content = "package: eppPackage \n" + "singleton: true\n" + "version: 1 \n" + "depends: sdk = 1, egit =1")
	public IInstallableUnit eppPackage;

	private IPlanner planner;
	private IEngine engine;
	private MigrationSupport scheduler;
	private Method needsMigrationMethod;

	@Override
	protected void setUp() throws Exception {
		IULoader.loadIUs(this);
		planner = createPlanner();
		engine = createEngine();
		scheduler = new MigrationSupport();
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, sdk2, egit1, egit2, cdt1, eppPackage});
		needsMigrationMethod = scheduler.getClass().getDeclaredMethod("findUnitstoMigrate", IProfile.class, IProfile.class);
		needsMigrationMethod.setAccessible(true);
	}

	public void testEmptyUserProfile() {
		IProfile previousUserProfile = createProfile("previous" + getName());
		IProfile currentBaseProfile = createProfile("current" + getName());
		assertOK(install(currentBaseProfile, new IInstallableUnit[] {sdk1}, true, planner, engine));

		//The user version and the base version are the same
		assertFalse(needsMigration(previousUserProfile, currentBaseProfile));
	}

	public void testSameVersions() {
		IProfile previousUserProfile = createProfile("previous" + getName());
		IProfile currentBaseProfile = createProfile("current" + getName());
		assertOK(install(previousUserProfile, new IInstallableUnit[] {sdk1}, true, planner, engine));
		assertOK(install(currentBaseProfile, new IInstallableUnit[] {sdk1}, true, planner, engine));

		//The user version and the base version are the same
		assertFalse(needsMigration(previousUserProfile, currentBaseProfile));
	}

	public void testDifferentVersions() {
		IProfile previousUserProfile = createProfile("previous" + getName());
		IProfile currentBaseProfile = createProfile("current" + getName());
		assertOK(installAsRoots(previousUserProfile, new IInstallableUnit[] {sdk1}, true, planner, engine));
		assertOK(installAsRoots(currentBaseProfile, new IInstallableUnit[] {sdk2}, true, planner, engine));

		//The user version is older than the base version
		assertFalse(needsMigration(previousUserProfile, currentBaseProfile));
	}

	public void testUserProfileNewerThanBaseVersion() {
		IProfile previousUserProfile = createProfile("previous" + getName());
		IProfile currentBaseProfile = createProfile("current" + getName());
		assertOK(installAsRoots(previousUserProfile, new IInstallableUnit[] {sdk2, egit2}, true, planner, engine));
		assertOK(installAsRoots(currentBaseProfile, new IInstallableUnit[] {sdk2, egit1}, true, planner, engine));

		//The user version is higher than what the base has
		assertTrue(needsMigration(previousUserProfile, currentBaseProfile));
	}

	public void testBaseEncompassWhatUserHas() {
		IProfile previousUserProfile = createProfile("previous" + getName());
		IProfile currentBaseProfile = createProfile("current" + getName());
		assertOK(installAsRoots(previousUserProfile, new IInstallableUnit[] {sdk1, egit1}, true, planner, engine));
		assertOK(installAsRoots(currentBaseProfile, new IInstallableUnit[] {eppPackage}, true, planner, engine));

		//All the elements that are in the user profile are included in the base
		assertFalse(needsMigration(previousUserProfile, currentBaseProfile));
	}

	public void testBaseEncompassSomePartsOfWhatUserHas() {
		IProfile previousUserProfile = createProfile("previous" + getName());
		IProfile currentBaseProfile = createProfile("current" + getName());
		assertOK(installAsRoots(previousUserProfile, new IInstallableUnit[] {sdk1, egit1, cdt1}, true, planner, engine));
		assertOK(installAsRoots(currentBaseProfile, new IInstallableUnit[] {eppPackage}, true, planner, engine));

		//Not all the elements that are in the user profile are included in the base
		assertTrue(needsMigration(previousUserProfile, currentBaseProfile));
	}

	public void testBaseEncompassSomePartsOfWhatUserHas2() {
		IProfile previousUserProfile = createProfile("previous" + getName());
		IProfile currentBaseProfile = createProfile("current" + getName());
		assertOK(installAsRoots(previousUserProfile, new IInstallableUnit[] {sdk1, egit1, cdt1}, true, planner, engine));
		assertOK(installAsRoots(currentBaseProfile, new IInstallableUnit[] {sdk1, egit1}, true, planner, engine));

		//Not all the elements that are in the user profile are included in the base
		assertTrue(needsMigration(previousUserProfile, currentBaseProfile));
	}

	public void testNoIUsInstalledInUserProfile() {
		IProfile previousUserProfile = createProfile("previous" + getName());
		IProfile currentBaseProfile = createProfile("current" + getName());
		assertOK(installAsRootsAndFlaggedAsBase(previousUserProfile, new IInstallableUnit[] {sdk1}, true, planner, engine));
		assertOK(installAsRoots(currentBaseProfile, new IInstallableUnit[] {sdk2}, true, planner, engine));

		//The elements from the previous base are not proposed for migration
		assertFalse(needsMigration(previousUserProfile, currentBaseProfile));
	}

	public void testOneIUInUserSpaceNotAvailableInBase() {
		IProfile previousUserProfile = createProfile("previous" + getName());
		IProfile currentBaseProfile = createProfile("current" + getName());
		assertOK(installAsRootsAndFlaggedAsBase(previousUserProfile, new IInstallableUnit[] {sdk1}, true, planner, engine));
		assertOK(installAsRoots(previousUserProfile, new IInstallableUnit[] {egit1}, true, planner, engine));
		assertOK(installAsRoots(currentBaseProfile, new IInstallableUnit[] {sdk2}, true, planner, engine));

		//In this case egit1 should be migrated
		assertTrue(needsMigration(previousUserProfile, currentBaseProfile));
	}

	public void testLowerVersionAvailableInBase() {
		IProfile previousUserProfile = createProfile("previous" + getName());
		IProfile currentBaseProfile = createProfile("current" + getName());
		assertOK(installAsRootsAndFlaggedAsBase(previousUserProfile, new IInstallableUnit[] {sdk1}, true, planner, engine));
		assertOK(installAsRoots(previousUserProfile, new IInstallableUnit[] {egit2}, true, planner, engine));
		assertOK(installAsRoots(currentBaseProfile, new IInstallableUnit[] {sdk2, egit1}, true, planner, engine));

		//In this case egit2 should be migrated
		assertTrue(needsMigration(previousUserProfile, currentBaseProfile));
	}

	public void testHigerVersionAvailableInBase() {
		IProfile previousUserProfile = createProfile("previous" + getName());
		IProfile currentBaseProfile = createProfile("current" + getName());
		assertOK(installAsRootsAndFlaggedAsBase(previousUserProfile, new IInstallableUnit[] {sdk1}, true, planner, engine));
		assertOK(installAsRoots(previousUserProfile, new IInstallableUnit[] {egit1}, true, planner, engine));
		assertOK(installAsRoots(currentBaseProfile, new IInstallableUnit[] {sdk2, egit1}, true, planner, engine));

		//Nothing to migrate
		assertFalse(needsMigration(previousUserProfile, currentBaseProfile));
	}

	public void testSameVersionAvailableInBase() {
		IProfile previousUserProfile = createProfile("previous" + getName());
		IProfile currentBaseProfile = createProfile("current" + getName());
		assertOK(installAsRootsAndFlaggedAsBase(previousUserProfile, new IInstallableUnit[] {sdk1}, true, planner, engine));
		assertOK(installAsRoots(previousUserProfile, new IInstallableUnit[] {egit1}, true, planner, engine));
		assertOK(installAsRoots(currentBaseProfile, new IInstallableUnit[] {sdk2, egit1}, true, planner, engine));

		//Nothing to migrate
		assertFalse(needsMigration(previousUserProfile, currentBaseProfile));
	}

	private boolean needsMigration(IProfile previousUserProfile, IProfile currentBaseProfile) {
		try {
			return !((Collection) needsMigrationMethod.invoke(scheduler, previousUserProfile, currentBaseProfile)).isEmpty();
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
