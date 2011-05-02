/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import java.util.HashMap;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Various automated tests of the {@link IDirector} API.
 */
public class AutomatedDirectorTest extends AbstractProvisioningTest {
	//private static Version version = Version.createOSGi(1, 0, 0);

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
		IMatchExpression<IInstallableUnit> filter = createFilter("FilterKey", "true");
		IRequirement capability = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, requiredIU.getId(), ANY_VERSION, filter, false, false);
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), new IRequirement[] {capability});

		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		createTestMetdataRepository(allUnits);
		IDirector director = createDirector();

		//Install into a profile in which the filter is satisfied
		Map properties = new HashMap();
		properties.put(IProfile.PROP_ENVIRONMENTS, "FilterKey=true");
		IProfile satisfied = createProfile("Satisfied." + getName(), properties);
		ProfileChangeRequest request = new ProfileChangeRequest(satisfied);
		request.add(toInstallIU);
		IStatus result = director.provision(request, null, null);
		assertTrue("1.0", result.isOK());
		assertProfileContains("1.1", satisfied, allUnits);
	}

	/**
	 * Tests installing an IU that has an optional prerequisite that is available.
	 */
	public void testInstallOptionalAvailable() {
		String capabilityId = "test." + getName();
		//The IU that exports the capability
		IInstallableUnit requiredIU = createIU("required." + getName(), new IProvidedCapability[] {MetadataFactory.createProvidedCapability("test.capability", capabilityId, DEFAULT_VERSION)});

		//The IU that optionally requires the capability
		IRequirement required = MetadataFactory.createRequirement("test.capability", capabilityId, ANY_VERSION, null, /* optional=> */true, /* multiple=> */false, /* greedy=>*/false);
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), new IRequirement[] {required});

		IInstallableUnit[] allUnits = new IInstallableUnit[] {toInstallIU, requiredIU};
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		IProfile profile = createProfile("TestProfile." + getName());
		IDirector director = createDirector();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(toInstallArray);
		IStatus result = director.provision(request, null, null);
		if (!result.isOK())
			LogHelper.log(result);
		assertTrue("1.0", result.isOK());
		assertProfileContains("1.1", profile, toInstallArray);
	}

	/**
	 * Tests installing an IU that has an optional prerequisite that is not available.
	 */
	public void testInstallOptionalUnavailable() {
		String capabilityId = "test." + getName();
		//no IU will be available that exports this capability
		IRequirement required = MetadataFactory.createRequirement("test.capability", capabilityId, ANY_VERSION, null, true, false);
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), new IRequirement[] {required});

		IInstallableUnit[] allUnits = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		IProfile profile = createProfile("TestProfile." + getName());
		IDirector director = createDirector();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(allUnits);
		IStatus result = director.provision(request, null, null);
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
		IProvidedCapability[] provides = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("test.capability", capabilityId, DEFAULT_VERSION)};
		IInstallableUnit requiredIU = createIU("required." + getName(), createFilter("osgi.os", "blort"), provides);

		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), createRequiredCapabilities("test.capability", capabilityId, ANY_VERSION, (IMatchExpression<IInstallableUnit>) null));

		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		IProfile profile = createProfile("TestProfile." + getName());
		IDirector director = createDirector();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(toInstallArray);
		IStatus result = director.provision(request, null, null);
		assertTrue("1.0", !result.isOK());

		//try again with the filter satisfied
		Map properties = new HashMap();
		properties.put(IProfile.PROP_ENVIRONMENTS, "osgi.os=blort");
		IProfile profile2 = createProfile("TestProfile2." + getName(), properties);
		request = new ProfileChangeRequest(profile2);
		request.addInstallableUnits(toInstallArray);
		result = director.provision(request, null, null);
		assertTrue("2.0", result.isOK());
	}

	/**
	 * Tests installing an IU that has an unsatisfied platform filter
	 */
	public void testInstallPlatformFilterUnsatisfied() {
		//The IU to install
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), createFilter("osgi.os", "blort"), NO_PROVIDES);
		IInstallableUnit[] allUnits = new IInstallableUnit[] {toInstallIU};
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		IProfile profile = createProfile("TestProfile." + getName());
		IDirector director = createDirector();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(toInstallArray);
		IStatus result = director.provision(request, null, null);
		assertTrue("1.0", !result.isOK());

		//try again with the filter satisfied
		Map properties = new HashMap();
		properties.put(IProfile.PROP_ENVIRONMENTS, "osgi.os=blort");
		IProfile profile2 = createProfile("TestProfile2." + getName(), properties);
		request = new ProfileChangeRequest(profile2);
		request.addInstallableUnits(toInstallArray);
		result = director.provision(request, null, null);
		assertTrue("2.0", result.isOK());
	}

	/**
	 * Simple test of installing an IU that has a required capability, and ensuring
	 * that the IU providing the capability is installed.
	 */
	public void testSimpleInstallRequired() {
		String capabilityId = "test." + getName();
		//The IU that exports the capability
		IInstallableUnit requiredIU = createIU("required." + getName(), new IProvidedCapability[] {MetadataFactory.createProvidedCapability("test.capability", capabilityId, DEFAULT_VERSION)});

		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), createRequiredCapabilities("test.capability", capabilityId, ANY_VERSION, (IMatchExpression<IInstallableUnit>) null));

		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		IProfile profile = createProfile("TestProfile." + getName());

		IDirector director = createDirector();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(toInstallArray);
		IStatus result = director.provision(request, null, null);
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

		IRequirement capability = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, requiredIU.getId(), null, null, false, false);
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), new IRequirement[] {capability});

		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		IProfile profile = createProfile("TestProfile." + getName());

		IDirector director = createDirector();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(toInstallArray);
		IStatus result = director.provision(request, null, null);
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

		IRequirement capability = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, requiredIU.getId(), ANY_VERSION, null, false, false);
		IInstallableUnit toInstallIU = createIU("toInstall." + getName(), new IRequirement[] {capability});

		IInstallableUnit[] allUnits = new IInstallableUnit[] {requiredIU, toInstallIU};
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		IProfile profile = createProfile("TestProfile." + getName());

		IDirector director = createDirector();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(toInstallArray);
		IStatus result = director.provision(request, null, null);
		if (!result.isOK())
			LogHelper.log(result);
		assertTrue("1.0", result.isOK());
		assertProfileContains("1.1", profile, allUnits);
	}

}
