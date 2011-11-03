/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * @since 1.0
 */
public abstract class AbstractPlannerTest extends AbstractProvisioningTest {
	private IProfile profile;
	private File previousStoreValue = null;
	IMetadataRepository repo = null;

	/*
	 * Return the root of the data for this test. e.g. "testData/bug302582"
	 */
	protected abstract String getTestDataPath();

	/*
	 * Return the profileID to be used for this test.
	 */
	protected abstract String getProfileId();

	protected IProfile getProfile() {
		return profile;
	}

	/*
	 * Take the given plan and compress additons/removals so they look like updates. 
	 * Good for viewing while debugging.
	 */
	protected Collection compress(IProvisioningPlan plan) {
		Map<String, InstallableUnitOperand> result = new HashMap<String, InstallableUnitOperand>();
		Operand[] operands = ((ProvisioningPlan) plan).getOperands();
		for (int i = 0; i < operands.length; i++) {
			if (!(operands[i] instanceof InstallableUnitOperand))
				continue;
			InstallableUnitOperand operand = (InstallableUnitOperand) operands[i];
			String id = operand.first() == null ? operand.second().getId() : operand.first().getId();
			InstallableUnitOperand existing = result.get(id);
			if (existing == null) {
				result.put(id, operand);
			} else {
				IInstallableUnit first = existing.first() == null ? operand.first() : existing.first();
				IInstallableUnit second = existing.second() == null ? operand.second() : existing.second();
				result.put(id, new InstallableUnitOperand(first, second));
			}
		}
		return result.values();
	}

	protected ProvisioningContext getContext(Collection<URI> repoLocations) {
		ProvisioningContext result = new ProvisioningContext(getAgent());
		result.setMetadataRepositories(repoLocations == null ? new URI[0] : repoLocations.toArray(new URI[repoLocations.size()]));
		result.setArtifactRepositories(new URI[0]);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.tests.AbstractProvisioningTest#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("loading planner test data", getTestDataPath());
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

		profile = realProfileRegistry.getProfile(getProfileId());
		assertNotNull(profile);
		repo = loadMetadataRepository(getTestData("planner test repo", getTestDataPath() + "/repo").toURI());
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

	/*
	 * Create and return a new profile change request with the given additions and removals.
	 */
	protected IProfileChangeRequest createProfileChangeRequest(Collection optionalAdds, Collection strictAdds, Collection toRemove) {
		IProfileChangeRequest result = new ProfileChangeRequest(profile);

		// add optional IUs
		if (optionalAdds != null) {
			for (Iterator iter = optionalAdds.iterator(); iter.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) iter.next();
				result.add(iu);
				result.setInstallableUnitInclusionRules(iu, ProfileInclusionRules.createOptionalInclusionRule(iu));
				result.setInstallableUnitProfileProperty(iu, "org.eclipse.equinox.p2.type.lock", "1");
				result.setInstallableUnitProfileProperty(iu, "org.eclipse.equinox.p2.reconciler.dropins", "true");
			}
		}

		// add strict IUs
		if (strictAdds != null) {
			for (Iterator iter = strictAdds.iterator(); iter.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) iter.next();
				result.add(iu);
				result.setInstallableUnitInclusionRules(iu, ProfileInclusionRules.createStrictInclusionRule(iu));
			}
		}

		// include removals
		if (toRemove != null) {
			for (Iterator iter = toRemove.iterator(); iter.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) iter.next();
				result.remove(iu);
			}
		}

		return result;
	}

	/*
	 * Assert that all the IU operands in the expected plan are contained in the actual plan.
	 */
	protected void assertContains(String message, IProvisioningPlan expectedPlan, IProvisioningPlan actualPlan) {
		Operand[] expectedOperands = ((ProvisioningPlan) expectedPlan).getOperands();
		Operand[] actualOperands = ((ProvisioningPlan) actualPlan).getOperands();

		// make sure the expected plan isn't empty
		assertFalse("0.9 Plan is empty.", expectedOperands.length == 0);
		for (int outer = 0; outer < expectedOperands.length; outer++) {
			if (!(expectedOperands[outer] instanceof InstallableUnitOperand))
				continue;
			IInstallableUnit first = ((InstallableUnitOperand) expectedOperands[outer]).first();
			IInstallableUnit second = ((InstallableUnitOperand) expectedOperands[outer]).second();

			// see if there is an operand in the actual plan which involved this IU.
			boolean found = false;
			for (int inner = 0; inner < actualOperands.length; inner++) {
				if (!(actualOperands[inner] instanceof InstallableUnitOperand))
					continue;
				InstallableUnitOperand actual = (InstallableUnitOperand) actualOperands[inner];

				// handle removals
				if (second == null) {
					if (actual.second() != null)
						continue;
					if (!actual.first().getId().equals(first.getId()))
						continue;
					// we are doing a removal and we have IUs with the same id... do they have the same version too?
					assertEquals("0.5", first, actual.first());
				}

				// treat additions and updates the same as long as we end up with the same IU in the end
				assertNotNull("1.2 " + actual, actual.second());
				if (!actual.second().getId().equals(second.getId()))
					continue;
				// we are doing an install or upgrade and we have IUs with the same id... do they have the same version too?
				assertEquals("2.0", second, actual.second());
				found = true;

			}
			if (!found)
				fail("3.0 Plan is missing install operand for: " + second);
		}
	}

}
