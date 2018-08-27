/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Various automated tests of the {@link IDirector} API.
 */
public class AutomatedDirectorTest extends AbstractProvisioningTest {
	/**
	 * Tests installing an IU that has a filtered dependency on another IU. When
	 * the filter is satisfied, the dependency is active and the required IU should
	 * be installed. When the filter is not satisfied, the dependency is inactive
	 * and the second IU should not be installed.
	 */
	public void testInstallFilteredCapability() {
		final String envKey = "filterKey";
		final String envVal = "true";

		// The IU that is required
		IInstallableUnit requiredIU = createIU("required." + getName());

		// The IU to be installed
		IMatchExpression<IInstallableUnit> requirementFilter = createFilter(envKey, envVal);
		IRequirement[] requires = new IRequirement[] {
				MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, requiredIU.getId(), ANY_VERSION, requirementFilter, false, false)
		};
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), requires);

		// Metadata repository
		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		createTestMetdataRepository(allUnits);

		// Install into a profile in which the requirement filter is satisfied
		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_ENVIRONMENTS, envKey + "=" + envVal);

		// Profile
		IProfile satisfied = createProfile("Satisfied." + getName(), properties);

		// Profile change request
		ProfileChangeRequest request = new ProfileChangeRequest(satisfied);
		request.add(toInstallIU);

		// Provision
		IDirector director = createDirector();
		IStatus result = director.provision(request, null, null);

		assertTrue(result.isOK());
		assertProfileContains("1.1", satisfied, allUnits);
	}

	/**
	 * Tests installing an IU that has an optional prerequisite that is available.
	 */
	public void testInstallOptionalAvailable() {
		final String capabilityNamespace = "test.capability";
		final String capabilityName = "test." + getName();
		final Version capabilityVersion = DEFAULT_VERSION;

		//The IU that exports the capability
		IProvidedCapability[] provides = new IProvidedCapability[] {
				MetadataFactory.createProvidedCapability(capabilityNamespace, capabilityName, capabilityVersion)
		};
		IInstallableUnit requiredIU = createIU("required." + getName(), provides);

		//The IU that optionally requires the capability
		IRequirement[] requires = new IRequirement[] {
				MetadataFactory.createRequirement(capabilityNamespace, capabilityName, ANY_VERSION, null, /* optional=> */true, /* multiple=> */false, /* greedy=>*/false)
		};
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), requires);

		// Metadata repository
		IInstallableUnit[] allUnits = new IInstallableUnit[] {toInstallIU, requiredIU};
		createTestMetdataRepository(allUnits);

		IProfile profile = createProfile("TestProfile." + getName());

		// Change request
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		request.addInstallableUnits(toInstallArray);

		// Provision
		IDirector director = createDirector();
		IStatus result = director.provision(request, null, null);
		if (!result.isOK()) {
			LogHelper.log(result);
		}

		// The requiredIu is not installed, because the optional requirement is not greedy
		assertTrue("1.0", result.isOK());
		assertProfileContains("1.1", profile, toInstallArray);
	}

	/**
	 * Tests installing an IU that has an optional prerequisite that is not available.
	 */
	public void testInstallOptionalUnavailable() {
		final String capabilityNamespace = "test.capability";
		final String capabilityName = "test." + getName();

		// no IU will be available that exports this capability
		IRequirement[] requires = new IRequirement[] {
				MetadataFactory.createRequirement(capabilityNamespace, capabilityName, ANY_VERSION, null, /* optional=> */true, /* multiple=> */false)
		};
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), requires);

		// Metadata repo
		IInstallableUnit[] allUnits = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		// Profile
		IProfile profile = createProfile("TestProfile." + getName());

		// Change request
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(allUnits);

		// Provision
		IDirector director = createDirector();
		IStatus result = director.provision(request, null, null);
		if (!result.isOK()) {
			LogHelper.log(result);
		}

		// The UI is installed because the requirement is optional
		assertTrue("1.0", result.isOK());
		assertProfileContains("1.1", profile, allUnits);
	}

	/**
	 * Tests installing an IU that has a required capability, and the IU providing
	 * the capability has a platform filter that is not satisfied.
	 */
	public void testInstallPlatformFilter() {
		// Profile environment
		final String osKey = "osgi.os";
		final String osVal = "blort";

		// Test capability
		final String capabilityNamespace = "test.capability";
		final String capabilityName = "test." + getName();
		final Version capabilityVersion = DEFAULT_VERSION;

		// The IU that exports the capability
		IProvidedCapability[] provides = new IProvidedCapability[] {MetadataFactory.createProvidedCapability(capabilityNamespace, capabilityName, capabilityVersion)};
		IInstallableUnit requiredIU = createIU("required." + getName(), createFilter(osKey, osVal), provides);

		// Installed IU
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), createRequiredCapabilities(capabilityNamespace, capabilityName, ANY_VERSION, (IMatchExpression<IInstallableUnit>) null));

		// Metadata repo
		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		createTestMetdataRepository(allUnits);

		IDirector director = createDirector();

		// Profile that does not satisfy the OS requirement
		IProfile profile = createProfile("TestProfile." + getName());

		// Request
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		request.addInstallableUnits(toInstallArray);

		// Provision - should fail since requireIU can't be installed on the current environment
		IStatus result = director.provision(request, null, null);
		assertTrue("1.0", !result.isOK());

		// New profile that satisfies the OS requirement
		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_ENVIRONMENTS, osKey + "=" + osVal);
		profile = createProfile("TestProfile2." + getName(), properties);

		// New request
		request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(toInstallArray);

		// New provisioning - should succeed
		result = director.provision(request, null, null);
		assertTrue("2.0", result.isOK());
	}

	/**
	 * Tests installing an IU that has an unsatisfied platform filter
	 */
	public void testInstallPlatformFilterUnsatisfied() {
		// Profile environment
		final String osKey = "osgi.os";
		final String osVal = "blort";

		// The IU to install that needs a concrete environment
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), createFilter(osKey, osVal), NO_PROVIDES);

		// Metadata repo
		IInstallableUnit[] allUnits = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		// Profile without a matching environment
		IProfile profile = createProfile("TestProfile." + getName());

		// Change request
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		request.addInstallableUnits(toInstallArray);

		// Provisioning failure: incompatible environment
		IDirector director = createDirector();
		IStatus result = director.provision(request, null, null);
		assertTrue(!result.isOK());

		// Profile with matching environment
		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_ENVIRONMENTS, osKey + "=" + osVal);
		profile = createProfile("TestProfile2." + getName(), properties);

		// New change request
		request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(toInstallArray);

		// Provisioning success
		result = director.provision(request, null, null);
		assertTrue(result.isOK());
	}

	/**
	 * Simple test of installing an IU that has a required capability, and ensuring
	 * that the IU providing the capability is installed.
	 */
	public void testSimpleInstallRequired() {
		// Test capability
		String capabilityNamespace = "test.capability";
		String capabilityName = "test." + getName();

		//The IU that exports the capability
		IProvidedCapability[] provides = new IProvidedCapability[] {
				MetadataFactory.createProvidedCapability(capabilityNamespace, capabilityName, DEFAULT_VERSION)
		};
		IInstallableUnit requiredIU = createIU("required." + getName(), provides);

		// The IU that requires the capability
		IRequirement[] requires = createRequiredCapabilities(capabilityNamespace, capabilityName, ANY_VERSION, (IMatchExpression<IInstallableUnit>) null);
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), requires);

		// Crate the metadata repo
		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		createTestMetdataRepository(allUnits);

		// Provision
		IProfile profile = createProfile("TestProfile." + getName());

		// Create the profile change request
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		request.addInstallableUnits(toInstallArray);

		// Provision
		IDirector director = createDirector();
		IStatus result = director.provision(request, null, null);
		if (!result.isOK()) {
			LogHelper.log(result);
		}

		assertTrue("1.0", result.isOK());
		assertProfileContains("1.1", profile, allUnits);
	}

	/**
	 * Simple test of installing an IU that has a required capability, but without
	 * specifying a version range (any version will do).
	 */
	public void testInstallRequiredNoVersion() {
		// The IU that is needed
		IInstallableUnit requiredIU = createIU("required." + getName());

		// The IU that is installed
		IRequirement[] requires = new IRequirement[] {
				MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, requiredIU.getId(), null, null, false, false)
		};
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), requires);

		// Metadata repo
		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		createTestMetdataRepository(allUnits);

		// Profile
		IProfile profile = createProfile("TestProfile." + getName());

		// Profile change request
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		request.addInstallableUnits(toInstallArray);

		// Provision
		IDirector director = createDirector();
		IStatus result = director.provision(request, null, null);
		if (!result.isOK()) {
			LogHelper.log(result);
		}

		assertTrue("1.0", result.isOK());
		assertProfileContains("1.1", profile, allUnits);
	}

	/**
	 * Simple test of installing an IU that has a required IU, and ensuring
	 * that the required IU is installed. In other words, the IU to install has a required
	 * capability on the IU namespace.
	 */
	public void testSimpleInstallRequiredIU() {
		// The IU that is required. It exports it's identity capability by default.
		IInstallableUnit requiredIU = createIU("required." + getName());

		// The IU that is installed
		IRequirement[] requires = new IRequirement[] {
				MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, requiredIU.getId(), ANY_VERSION, null, false, false)
		};
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), requires);

		// Create the metadata repo
		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		createTestMetdataRepository(allUnits);

		IProfile profile = createProfile("TestProfile." + getName());

		// Create the profile change request
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		request.addInstallableUnits(toInstallArray);

		// Provision
		IDirector director = createDirector();
		IStatus result = director.provision(request, null, null);
		if (!result.isOK()) {
			LogHelper.log(result);
		}

		assertTrue(result.isOK());
		assertProfileContains("1.1", profile, allUnits);
	}

}
