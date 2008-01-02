/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Various automated tests of the {@link IDirector} API.
 */
public class AutomatedDirectorTest extends AbstractProvisioningTest {
	//private static Version version = new Version(1, 0, 0);

	public static Test suite() {
		return new TestSuite(AutomatedDirectorTest.class);
	}

	public AutomatedDirectorTest() {
		super("");
	}

	public AutomatedDirectorTest(String name) {
		super(name);
	}

	/**
	 * Tests installing an IU that has a filtered dependency on another IU. When
	 * the filter is satisfied, the dependency is active and the required IU should
	 * be installed. When the filter is not satisfied, the dependency is inactive
	 * and the second IU should not be installed.
	 */
	public void testInstallFilteredCapability() {
		//The IU that is required
		IInstallableUnit requiredIU = createIU("required." + getName());

		// The IU to be installed
		String filter = createFilter("FilterKey", "true");
		RequiredCapability capability = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU, requiredIU.getId(), ANY_VERSION, filter, false, false);
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), new RequiredCapability[] {capability});

		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);
		IDirector director = createDirector();

		//Install into a profile in which the filter is satisfied
		Profile satisfied = createProfile("Satisfied." + getName());
		satisfied.setValue(Profile.PROP_ENVIRONMENTS, "FilterKey=true");
		IStatus result = director.install(toInstallArray, satisfied, null, null);
		assertTrue("1.0", result.isOK());
		assertProfileContains("1.1", satisfied, allUnits);
	}

	/**
	 * Tests installing an IU that has an optional prerequisite that is available.
	 */
	public void testInstallOptionalAvailable() {
		String capabilityId = "test." + getName();
		//The IU that exports the capability
		IInstallableUnit requiredIU = createIU("required." + getName(), new ProvidedCapability[] {MetadataFactory.createProvidedCapability("test.capability", capabilityId, DEFAULT_VERSION)});

		//The IU that optionally requires the capability
		RequiredCapability required = MetadataFactory.createRequiredCapability("test.capability", capabilityId, ANY_VERSION, null, /* optional=> */true, /* multiple=> */false);
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), new RequiredCapability[] {required});

		IInstallableUnit[] allUnits = new IInstallableUnit[] {toInstallIU, requiredIU};
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		Profile profile = createProfile("TestProfile." + getName());
		IDirector director = createDirector();
		IStatus result = director.install(toInstallArray, profile, null, null);
		if (!result.isOK())
			LogHelper.log(result);
		assertTrue("1.0", result.isOK());
		assertProfileContains("1.1", profile, allUnits);
	}

	/**
	 * Tests installing an IU that has an optional prerequisite that is not available.
	 */
	public void testInstallOptionalUnavailable() {
		String capabilityId = "test." + getName();
		//no IU will be available that exports this capability
		RequiredCapability required = MetadataFactory.createRequiredCapability("test.capability", capabilityId, ANY_VERSION, null, true, false);
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), new RequiredCapability[] {required});

		IInstallableUnit[] allUnits = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		Profile profile = createProfile("TestProfile." + getName());
		IDirector director = createDirector();
		IStatus result = director.install(allUnits, profile, null, null);
		if (!result.isOK())
			LogHelper.log(result);
		assertTrue("1.0", result.isOK());
		assertProfileContains("1.1", profile, allUnits);
	}

	/**
	 * Tests installing an IU that has a required capability, and the IU providing
	 * the capability has a platform filter that is not satisfied.
	 */
	public void testInstallPlatformFilter() {
		//The IU that exports the capability
		String capabilityId = "test." + getName();
		ProvidedCapability[] provides = new ProvidedCapability[] {MetadataFactory.createProvidedCapability("test.capability", capabilityId, DEFAULT_VERSION)};
		IInstallableUnit requiredIU = createIU("required." + getName(), createFilter("osgi.os", "blort"), provides);

		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), createRequiredCapabilities("test.capability", capabilityId, ANY_VERSION, null));

		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		Profile profile = createProfile("TestProfile." + getName());
		IDirector director = createDirector();
		IStatus result = director.install(toInstallArray, profile, null, null);
		assertTrue("1.0", !result.isOK());

		//try again with the filter satisfied
		profile.setValue(Profile.PROP_ENVIRONMENTS, "osgi.os=blort");
		result = director.install(toInstallArray, profile, null, null);
		assertTrue("2.0", result.isOK());
	}

	/**
	 * Simple test of installing an IU that has a required capability, and ensuring
	 * that the IU providing the capability is installed.
	 */
	public void testSimpleInstallRequired() {
		String capabilityId = "test." + getName();
		//The IU that exports the capability
		IInstallableUnit requiredIU = createIU("required." + getName(), new ProvidedCapability[] {MetadataFactory.createProvidedCapability("test.capability", capabilityId, DEFAULT_VERSION)});

		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), createRequiredCapabilities("test.capability", capabilityId, ANY_VERSION, null));

		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		Profile profile = createProfile("TestProfile." + getName());

		IDirector director = createDirector();
		IStatus result = director.install(toInstallArray, profile, null, null);
		if (!result.isOK())
			LogHelper.log(result);
		assertTrue("1.0", result.isOK());
		assertProfileContains("1.1", profile, allUnits);
	}

	/**
	 * Simple test of installing an IU that has a required capability, but without
	 * specifying a version range (any version will do).
	 */
	public void testInstallRequiredNoVersion() {
		//The IU that is needed
		IInstallableUnit requiredIU = createIU("required." + getName());

		RequiredCapability capability = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU, requiredIU.getId(), null, null, false, false);
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), new RequiredCapability[] {capability});

		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		Profile profile = createProfile("TestProfile." + getName());

		IDirector director = createDirector();
		IStatus result = director.install(toInstallArray, profile, null, null);
		if (!result.isOK())
			LogHelper.log(result);
		assertTrue("1.0", result.isOK());
		assertProfileContains("1.1", profile, allUnits);
	}

	/**
	 * Simple test of installing an IU that has a required IU, and ensuring
	 * that the required IU is installed. In other words, the IU to install has a required
	 * capability on the IU namespace.
	 */
	public void testSimpleInstallRequiredIU() {
		//The IU that exports the capability
		IInstallableUnit requiredIU = createIU("required." + getName());

		RequiredCapability capability = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU, requiredIU.getId(), ANY_VERSION, null, false, false);
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), new RequiredCapability[] {capability});

		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		Profile profile = createProfile("TestProfile." + getName());

		IDirector director = createDirector();
		IStatus result = director.install(toInstallArray, profile, null, null);
		if (!result.isOK())
			LogHelper.log(result);
		assertTrue("1.0", result.isOK());
		assertProfileContains("1.1", profile, allUnits);
	}

}
