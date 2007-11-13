/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.*;
import java.util.*;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.director.IPlanner;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.installregistry.IInstallRegistry;
import org.eclipse.equinox.p2.installregistry.IProfileInstallRegistry;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * Base class for provisioning tests with convenience methods used by multiple tests.
 */
public class AbstractProvisioningTest extends TestCase {

	protected static final VersionRange ANY_VERSION = new VersionRange(Version.emptyVersion, true, new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), true);
	private static final ProvidedCapability[] BUNDLE_CAPABILITY = new ProvidedCapability[] {new ProvidedCapability("eclipse.touchpoint", "bundle", new Version(1, 0, 0))};

	private static final RequiredCapability[] BUNDLE_REQUIREMENT = new RequiredCapability[] {new RequiredCapability("eclipse.touchpoint", "bundle", VersionRange.emptyRange, null, false, true)};

	protected static final Version DEFAULT_VERSION = new Version(1, 0, 0);
	protected static final TouchpointType ECLIPSE_TOUCHPOINT = new TouchpointType("eclipse", new Version(1, 0, 0));

	protected static final ProvidedCapability[] NO_PROVIDES = new ProvidedCapability[0];
	protected static final Map NO_PROPERTIES = Collections.EMPTY_MAP;
	protected static final TouchpointData NO_TP_DATA = null;

	protected static final RequiredCapability[] NO_REQUIRES = new RequiredCapability[0];

	/**
	 * Tracks the metadata repositories created by this test instance. The repositories
	 * will be removed automatically at the end of the test.
	 */
	private List metadataRepos = new ArrayList();
	/**
	 * Tracks the profiles created by this test instance. The profiles
	 * will be removed automatically at the end of the test.
	 */
	private List profilesToRemove = new ArrayList();

	public static void assertEmptyProfile(Profile p) {
		assertNotNull("The profile should not be null", p);
		boolean containsIU = false;
		for (Iterator iterator = p.getInstallableUnits(); iterator.hasNext();) {
			containsIU = true;
		}
		if (containsIU)
			fail("The profile should be empty,profileId=" + p);
	}

	protected static void assertNotIUs(IInstallableUnit[] ius, Iterator installableUnits) {
		Set notexpected = new HashSet();
		notexpected.addAll(Arrays.asList(ius));

		while (installableUnits.hasNext()) {
			IInstallableUnit next = (IInstallableUnit) installableUnits.next();
			if (notexpected.contains(next)) {
				fail("not expected [" + next + "]");
			}
		}
	}

	protected static void assertNotOK(IStatus result) {
		assertTrue("The status should not have been OK", !result.isOK());
	}

	protected static void assertOK(IStatus result) {
		if (result.isOK())
			return;

		if (result instanceof MultiStatus) {
			MultiStatus ms = (MultiStatus) result;
			IStatus children[] = ms.getChildren();
			for (int i = 0; i < children.length; i++) {
				System.err.println(children[i]);
			}
		}

		Throwable t = result.getException();
		if (t != null)
			t.printStackTrace();

		fail(result.toString());
	}

	/**
	 * Asserts that the given profile contains *only* the given IUs.
	 */
	protected static void assertProfileContains(String message, Profile profile, IInstallableUnit[] expectedUnits) {
		HashSet expected = new HashSet(Arrays.asList(expectedUnits));
		for (Iterator it = profile.getInstallableUnits(); it.hasNext();) {
			IInstallableUnit actual = (IInstallableUnit) it.next();
			if (!expected.remove(actual))
				fail(message + " profile " + profile.getProfileId() + " contained an unexpected unit: " + actual);
		}
		if (!expected.isEmpty())
			fail(message + " profile " + profile.getProfileId() + " did not contain expected units: " + expected);
	}

	/**
	 * Asserts that the given profile contains all the given IUs.
	 */
	protected static void assertProfileContainsAll(String message, Profile profile, IInstallableUnit[] expectedUnits) {
		HashSet expected = new HashSet(Arrays.asList(expectedUnits));
		for (Iterator it = profile.getInstallableUnits(); it.hasNext();) {
			IInstallableUnit actual = (IInstallableUnit) it.next();
			expected.remove(actual);
		}
		if (!expected.isEmpty())
			fail(message + " profile " + profile.getProfileId() + " did not contain expected units: " + expected);
	}

	/**
	 * 	Create an eclipse InstallableUnitFragment with the given name that is hosted
	 *  by any bundle. The fragment has the default version, and the default self and
	 *  fragment provided capabilities are added to the IU.
	 */
	public static IInstallableUnitFragment createBundleFragment(String name) {
		return createIUFragment(name, DEFAULT_VERSION, BUNDLE_REQUIREMENT, ECLIPSE_TOUCHPOINT, NO_TP_DATA);
	}

	/**
	 * 	Create an eclipse InstallableUnitFragment with the given name and version
	 *  that is hosted by any bundle. The default self and fragment provided capabilities
	 *  are added to the IU.
	 */
	public static IInstallableUnitFragment createBundleFragment(String name, Version version, TouchpointData tpData) {
		return createIUFragment(name, version, BUNDLE_REQUIREMENT, ECLIPSE_TOUCHPOINT, tpData);
	}

	public static IDirector createDirector() {
		return (IDirector) ServiceHelper.getService(TestActivator.getContext(), IDirector.class.getName());
	}

	/**
	 * 	Create an Eclipse InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createEclipseIU(String name) {
		return createEclipseIU(name, DEFAULT_VERSION);
	}

	/**
	 * 	Create an Eclipse InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createEclipseIU(String name, Version version) {
		return createIU(name, version, null, NO_REQUIRES, BUNDLE_CAPABILITY, NO_PROPERTIES, ECLIPSE_TOUCHPOINT, NO_TP_DATA, false);
	}

	/**
	 * 	Create an Eclipse InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createEclipseIU(String name, Version version, RequiredCapability[] requires, TouchpointData touchpointData) {
		return createIU(name, version, null, requires, BUNDLE_CAPABILITY, NO_PROPERTIES, ECLIPSE_TOUCHPOINT, touchpointData, false);
	}

	/**
	 * Creates and returns a correctly formatted LDAP filter with the given key and value.
	 */
	protected static String createFilter(String filterKey, String filterValue) {
		return "(" + filterKey + '=' + filterValue + ')';
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name) {
		return createIU(name, DEFAULT_VERSION);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, ProvidedCapability[] additionalProvides) {
		return createIU(name, DEFAULT_VERSION, null, NO_REQUIRES, additionalProvides, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, RequiredCapability[] requires) {
		return createIU(name, DEFAULT_VERSION, null, requires, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, RequiredCapability[] requires, boolean singleton) {
		return createIU(name, DEFAULT_VERSION, null, requires, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, singleton);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, String filter, ProvidedCapability[] additionalProvides) {
		return createIU(name, DEFAULT_VERSION, filter, NO_REQUIRES, additionalProvides, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version, String filter, ProvidedCapability[] additionalProvides) {
		return createIU(name, version, filter, NO_REQUIRES, additionalProvides, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version, ProvidedCapability[] additionalProvides) {
		return createIU(name, version, null, NO_REQUIRES, additionalProvides, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version) {
		return createIU(name, version, null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version, RequiredCapability[] required) {
		return createIU(name, version, null, required, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version, RequiredCapability[] required, Map properties, boolean singleton) {
		return createIU(name, version, null, required, NO_PROVIDES, properties, TouchpointType.NONE, NO_TP_DATA, singleton);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version, boolean singleton) {
		return createIU(name, version, null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, singleton);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version, String filter, RequiredCapability[] required, ProvidedCapability[] additionalProvides, Map properties, TouchpointType tpType, TouchpointData tpData, boolean singleton) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(name);
		iu.setVersion(version);
		iu.setFilter(filter);
		ProvidedCapability[] provides = new ProvidedCapability[additionalProvides.length + 1];
		provides[0] = getSelfCapability(name, version);
		for (int i = 0; i < additionalProvides.length; i++) {
			provides[i + 1] = additionalProvides[i];
		}
		iu.setCapabilities(provides);
		iu.setRequiredCapabilities(required);
		iu.setTouchpointType(tpType);
		if (tpData != null)
			iu.addTouchpointData(tpData);
		iu.setSingleton(singleton);
		return MetadataFactory.createInstallableUnit(iu);
	}

	/**
	 * 	Create a basic InstallableUnitFragment with the given attributes. 
	 * The self and fragment provided capabilities are added to the IU.
	 */
	public static IInstallableUnitFragment createIUFragment(String name) {
		return createIUFragment(name, DEFAULT_VERSION, NO_REQUIRES, TouchpointType.NONE, NO_TP_DATA);
	}

	/**
	 * 	Create a basic InstallableUnitFragment with the given attributes. 
	 * The self and fragment provided capabilities are added to the IU.
	 */
	public static IInstallableUnitFragment createIUFragment(String name, Version version) {
		return createIUFragment(name, version, NO_REQUIRES, TouchpointType.NONE, NO_TP_DATA);
	}

	/**
	 * 	Create a basic InstallableUnitFragment with the given attributes. 
	 * The self and fragment provided capabilities are added to the IU.
	 */
	public static IInstallableUnitFragment createIUFragment(String name, Version version, RequiredCapability[] required, TouchpointType tpType, TouchpointData tpData) {
		InstallableUnitFragmentDescription iu = new InstallableUnitFragmentDescription();
		iu.setId(name);
		iu.setVersion(version);
		ProvidedCapability[] cap = new ProvidedCapability[] {getSelfCapability(name, version), IInstallableUnitFragment.FRAGMENT_CAPABILITY};
		iu.setCapabilities(cap);
		iu.setRequiredCapabilities(required);
		iu.setTouchpointType(tpType);
		if (tpData != null)
			iu.addTouchpointData(tpData);
		return MetadataFactory.createInstallableUnitFragment(iu);
	}

	public static IPlanner createPlanner() {
		return (IPlanner) ServiceHelper.getService(TestActivator.getContext(), IPlanner.class.getName());
	}

	/**
	 * Creates a profile with the given name. Ensures that no such profile
	 * already exists.  The returned profile will be removed automatically
	 * in the tearDown method.
	 */
	protected Profile createProfile(String name) {
		return createProfile(name, null);
	}

	/**
	 * Creates a profile with the given name. Ensures that no such profile
	 * already exists.  The returned profile will be removed automatically
	 * in the tearDown method.
	 */
	protected Profile createProfile(String name, Profile parent) {
		//remove any existing profile with the same name
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		Profile profile = profileRegistry.getProfile(name);
		if (profile != null)
			profileRegistry.removeProfile(profile);
		//create and return a new profile
		profile = new Profile(name, parent);
		profilesToRemove.add(profile);
		return profile;
	}

	/**
	 * Creates and returns a required capability with the provided attributes.
	 */
	protected static RequiredCapability[] createRequiredCapabilities(String namespace, String name, String filter) {
		return createRequiredCapabilities(namespace, name, ANY_VERSION, filter);
	}

	/**
	 * Creates and returns a required capability with the provided attributes.
	 */
	protected static RequiredCapability[] createRequiredCapabilities(String namespace, String name, VersionRange range, String filter) {
		return new RequiredCapability[] {new RequiredCapability(namespace, name, range, filter, false, false)};
	}

	/**
	 * Fails the test due to the given throwable.
	 */
	public static void fail(String message, Throwable e) {
		// If the exception is a CoreException with a multistatus
		// then print out the multistatus so we can see all the info.
		if (e instanceof CoreException) {
			IStatus status = ((CoreException) e).getStatus();
			//if the status does not have an exception, print the stack for this one
			if (status.getException() == null)
				e.printStackTrace();
			write(status, 0);
		} else
			e.printStackTrace();
		fail(message + ": " + e);
	}

	/**
	 * 	Get the 'self' capability for the given installable unit.
	 */
	private static ProvidedCapability getSelfCapability(String installableUnitId, Version installableUnitVersion) {
		return new ProvidedCapability(IInstallableUnit.NAMESPACE_IU, installableUnitId, installableUnitVersion);
	}

	private static void indent(OutputStream output, int indent) {
		for (int i = 0; i < indent; i++)
			try {
				output.write("\t".getBytes());
			} catch (IOException e) {
				// ignore
			}
	}

	// TODO: The following group of utilities are (somewhat) specific to eclipse test cases
	//		 so could be moved to a separate base class (e.g. AbstractEclipseProvisioningTestCase)
	//		 that extends AbstractProvisioningTestCase.

	public static void printProfile(Profile toPrint) {
		boolean containsIU = false;
		for (Iterator iterator = toPrint.getInstallableUnits(); iterator.hasNext();) {
			System.out.println(iterator.next());
			containsIU = true;
		}
		if (!containsIU)
			System.out.println("No iu");
	}

	private static void write(IStatus status, int indent) {
		PrintStream output = System.out;
		indent(output, indent);
		output.println("Severity: " + status.getSeverity());

		indent(output, indent);
		output.println("Plugin ID: " + status.getPlugin());

		indent(output, indent);
		output.println("Code: " + status.getCode());

		indent(output, indent);
		output.println("Message: " + status.getMessage());

		if (status.getException() != null) {
			indent(output, indent);
			output.print("Exception: ");
			status.getException().printStackTrace(output);
		}

		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (int i = 0; i < children.length; i++)
				write(children[i], indent + 1);
		}
	}

	public AbstractProvisioningTest() {
		super("");
	}

	public AbstractProvisioningTest(String name) {
		super(name);
	}

	protected void assertEquals(String message, byte[] expected, byte[] actual) {
		if (expected == null && actual == null)
			return;
		if (expected == null)
			fail(message + " expected null but was: " + actual);
		if (actual == null)
			fail(message + " array is unexpectedly null");
		if (expected.length != actual.length)
			fail(message + " expected array length:<" + expected.length + "> but was:<" + actual.length + ">");
		for (int i = 0; i < expected.length; i++)
			assertEquals(message + " arrays differ at position:<" + i + ">", expected[i], actual[i]);
	}

	// End of eclipse specific utilities.

	protected void assertEquals(String message, Object[] expected, Object[] actual) {
		if (expected == null && actual == null)
			return;
		if (expected == null)
			fail(message + " expected null but was: " + actual);
		if (actual == null)
			fail(message + " array is unexpectedly null");
		if (expected.length != actual.length)
			fail(message + " expected array length:<" + expected.length + "> but was:<" + actual.length + ">");
		for (int i = 0; i < expected.length; i++)
			assertEquals(message + " arrays differ at position:<" + i + ">", expected[i], actual[i]);
	}

	/**
	 * Adds a test metadata repository to the system that provides the given units. 
	 * The repository will automatically be removed in the tearDown method.
	 */
	protected void createTestMetdataRepository(IInstallableUnit[] units) {
		IMetadataRepository repo = new TestMetadataRepository(units);
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
		assertNotNull(repoMan);
		repoMan.addRepository(repo);
		metadataRepos.add(repo);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		//remove all metadata repositories created by this test
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (!metadataRepos.isEmpty()) {
			for (Iterator it = metadataRepos.iterator(); it.hasNext();) {
				IMetadataRepository repo = (IMetadataRepository) it.next();
				repoMan.removeRepository(repo);
			}
			metadataRepos.clear();
		}
		//remove all profiles created by this test
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		IInstallRegistry installRegistry = (IInstallRegistry) ServiceHelper.getService(TestActivator.getContext(), IInstallRegistry.class.getName());
		for (Iterator it = profilesToRemove.iterator(); it.hasNext();) {
			Profile toRemove = (Profile) it.next();
			profileRegistry.removeProfile(toRemove);
			IProfileInstallRegistry profileInstallRegistry = installRegistry.getProfileInstallRegistry(toRemove);
			IInstallableUnit[] units = profileInstallRegistry.getInstallableUnits();
			for (int i = 0; i < units.length; i++) {
				profileInstallRegistry.removeInstallableUnits(units[i]);
			}
		}
		profilesToRemove.clear();
		//See bug 209069 - currently no way to persist install registry changes or clear the metadata cache
		//		((InstallRegistry) installRegistry).persist();
		//		IMetadataRepository cache = MetadataCache.getCacheInstance((MetadataRepositoryManager) repoMan);
		//		cache.removeAll();
	}

	/**
	 * Returns a resolved IU corresponding to the given IU, with no attached fragments.
	 */
	protected IInstallableUnit createResolvedIU(IInstallableUnit unit) {
		return MetadataFactory.createResolvedInstallableUnit(unit, new IInstallableUnitFragment[0]);
	}
}
