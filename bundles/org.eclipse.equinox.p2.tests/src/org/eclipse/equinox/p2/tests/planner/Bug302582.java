/*******************************************************************************
 *  Copyright (c) 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Iterator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerHelper;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug302582 extends AbstractProvisioningTest {
	private IProfile profile;
	private File previousStoreValue = null;
	String profileLoadedId = "bootProfile";
	String directory = "testData/bug302582";
	IMetadataRepository repo = null;

	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("test data bug 302582", directory);
		File tempFolder = getTempFolder();
		copy("0.2", reporegistry1, tempFolder);
		IProfileRegistry realProfileRegistry = getProfileRegistry();
		//Tweak the running profile registry
		Field profileStore = SimpleProfileRegistry.class.getDeclaredField("store");
		profileStore.setAccessible(true);
		previousStoreValue = (File) profileStore.get(realProfileRegistry);
		profileStore.set(realProfileRegistry, new File(tempFolder, "p2/org.eclipse.equinox.p2.engine/profileRegistry"));

		Field profilesMapField = SimpleProfileRegistry.class.getDeclaredField("profiles"); //$NON-NLS-1$
		profilesMapField.setAccessible(true);
		profilesMapField.set(realProfileRegistry, null);
		//End of tweaking the profile registry

		profile = realProfileRegistry.getProfile(profileLoadedId);
		assertNotNull(profile);
		repo = loadMetadataRepository(getTestData("Repository for 302582", directory + "/repo").toURI());
	}

	protected IMetadataRepository loadMetadataRepository(URI location) throws ProvisionException {
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		return metadataRepositoryManager.loadRepository(location, null);
	}

	protected void tearDown() throws Exception {
		SimpleProfileRegistry realProfileRegistry = (SimpleProfileRegistry) getProfileRegistry();

		Field profilesMapField = SimpleProfileRegistry.class.getDeclaredField("profiles"); //$NON-NLS-1$
		profilesMapField.setAccessible(true);
		profilesMapField.set(realProfileRegistry, null);

		Field profileStore = SimpleProfileRegistry.class.getDeclaredField("store");
		profileStore.setAccessible(true);
		profileStore.set(realProfileRegistry, previousStoreValue);
		super.tearDown();
	}

	public void testInstall() {
		try {
			repo = loadMetadataRepository(getTestData("test data bug 302582 repo", directory + "/repo").toURI());
		} catch (ProvisionException e) {
			assertNull(e); //This guarantees that the error does not go unnoticed
		}
		IQueryResult ius = repo.query(InstallableUnitQuery.ANY, new NullProgressMonitor());
		IPlanner planner = createPlanner();
		IProvisioningPlan actualPlan, expectedPlan = null;

		actualPlan = planner.getProvisioningPlan(createActualRequest(ius), null, new NullProgressMonitor());
		expectedPlan = planner.getProvisioningPlan(createExpectedRequest(ius), null, new NullProgressMonitor());

		Operand[] actualOperands = actualPlan.getOperands();
		Operand[] expectedOperands = expectedPlan.getOperands();
		assertFalse("0.9 Plan is empty.", expectedOperands.length == 0);

		for (int outer = 0; outer < expectedOperands.length; outer++) {
			if (!(expectedOperands[outer] instanceof InstallableUnitOperand))
				continue;
			IInstallableUnit first = ((InstallableUnitOperand) expectedOperands[outer]).first();
			IInstallableUnit second = ((InstallableUnitOperand) expectedOperands[outer]).second();
			// we are upgrading from v1.0.0.x to v1.0.2.x
			assertNotNull("1.0 " + first, first);
			assertNotNull("1.1 " + second, second);

			// see if there is an operand in the actual plan which involved this IU.
			boolean found = false;
			for (int inner = 0; inner < actualOperands.length; inner++) {
				if (!(actualOperands[inner] instanceof InstallableUnitOperand))
					continue;
				InstallableUnitOperand actual = (InstallableUnitOperand) actualOperands[inner];
				assertNotNull("1.2 " + actual, actual.second());
				if (!actual.second().getId().equals(second.getId()))
					continue;
				// we have IUs with the same id... do they have the same version too?
				assertEquals("2.0", second, actual.second());
				found = true;
			}
			if (!found)
				fail("3.0 Plan is missing install operand for: " + second);
		}
	}

	/*
	 * Create and return a profile change request which says we want to 
	 * optionally install all of the given IUs.
	 */
	private ProfileChangeRequest createActualRequest(IQueryResult ius) {
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits((IInstallableUnit[]) ius.toArray(IInstallableUnit.class));
		Iterator it = ius.iterator();
		while (it.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) it.next();
			pcr.setInstallableUnitInclusionRules(iu, PlannerHelper.createOptionalInclusionRule(iu));
		}
		return pcr;
	}

	/*
	 * Create and return a profile change request for what we expect... we want to install
	 * the highest version of the HelloWorld bundle.
	 */
	private ProfileChangeRequest createExpectedRequest(IQueryResult ius) {
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		Iterator it = ius.iterator();
		while (it.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) it.next();
			if ((iu.getId().equals("com.dcns.rsm.hello") && iu.getVersion().equals(Version.createOSGi(1, 0, 2, "v20100103")))) {
				pcr.addInstallableUnits(new IInstallableUnit[] {iu});
				pcr.setInstallableUnitInclusionRules(iu, PlannerHelper.createOptionalInclusionRule(iu));
			}
		}
		return pcr;
	}
}
