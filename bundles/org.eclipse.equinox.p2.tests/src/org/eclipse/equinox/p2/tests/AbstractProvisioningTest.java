/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
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
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * Base class for provisioning tests with convenience methods used by multiple tests.
 */
public class AbstractProvisioningTest extends TestCase {

	protected static final VersionRange ANY_VERSION = new VersionRange(Version.emptyVersion, true, new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), true);
	private static final ProvidedCapability[] BUNDLE_CAPABILITY = new ProvidedCapability[] {MetadataFactory.createProvidedCapability("eclipse.touchpoint", "bundle", new Version(1, 0, 0))};

	private static final RequiredCapability[] BUNDLE_REQUIREMENT = new RequiredCapability[] {MetadataFactory.createRequiredCapability("eclipse.touchpoint", "bundle", VersionRange.emptyRange, null, false, true)};

	protected static final Version DEFAULT_VERSION = new Version(1, 0, 0);
	public static final TouchpointType TOUCHPOINT_OSGI = MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.osgi", new Version(1, 0, 0));

	protected static final Map NO_PROPERTIES = Collections.EMPTY_MAP;
	protected static final ProvidedCapability[] NO_PROVIDES = new ProvidedCapability[0];
	protected static final RequiredCapability[] NO_REQUIRES = new RequiredCapability[0];

	protected static final TouchpointData NO_TP_DATA = null;

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

	public static void assertEmptyProfile(IProfile profile) {
		assertNotNull("The profile should not be null", profile);
		if (getInstallableUnits(profile).hasNext())
			fail("The profile should be empty,profileId=" + profile);
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
	protected static void assertProfileContains(String message, IProfile profile, IInstallableUnit[] expectedUnits) {
		HashSet expected = new HashSet(Arrays.asList(expectedUnits));
		for (Iterator it = getInstallableUnits(profile); it.hasNext();) {
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
	protected static void assertProfileContainsAll(String message, IProfile profile2, IInstallableUnit[] expectedUnits) {
		HashSet expected = new HashSet(Arrays.asList(expectedUnits));
		for (Iterator it = getInstallableUnits(profile2); it.hasNext();) {
			IInstallableUnit actual = (IInstallableUnit) it.next();
			expected.remove(actual);
		}
		if (!expected.isEmpty())
			fail(message + " profile " + profile2.getProfileId() + " did not contain expected units: " + expected);
	}

	/*
	 * Copy
	 * - if we have a file, then copy the file
	 * - if we have a directory then merge
	 */
	public static void copy(File source, File target) throws IOException {
		if (!source.exists())
			return;
		if (source.isDirectory()) {
			if (target.exists() && target.isFile())
				target.delete();
			if (!target.exists())
				target.mkdirs();
			File[] children = source.listFiles();
			for (int i = 0; i < children.length; i++)
				copy(children[i], new File(target, children[i].getName()));
			return;
		}
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new BufferedInputStream(new FileInputStream(source));
			output = new BufferedOutputStream(new FileOutputStream(target));

			byte[] buffer = new byte[8192];
			int bytesRead = 0;
			while ((bytesRead = input.read(buffer)) != -1)
				output.write(buffer, 0, bytesRead);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					System.err.println("Exception while trying to close input stream on: " + source.getAbsolutePath());
					e.printStackTrace();
				}
			}
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					System.err.println("Exception while trying to close output stream on: " + target.getAbsolutePath());
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 	Create an eclipse InstallableUnitFragment with the given name that is hosted
	 *  by any bundle. The fragment has the default version, and the default self and
	 *  fragment provided capabilities are added to the IU.
	 */
	public static IInstallableUnitFragment createBundleFragment(String name) {
		return createIUFragment(null, name, DEFAULT_VERSION, BUNDLE_REQUIREMENT, TOUCHPOINT_OSGI, NO_TP_DATA);
	}

	/**
	 * 	Create an eclipse InstallableUnitFragment with the given name and version
	 *  that is hosted by any bundle. The default self and fragment provided capabilities
	 *  are added to the IU.
	 */
	public static IInstallableUnitFragment createBundleFragment(String name, Version version, TouchpointData tpData) {
		return createIUFragment(null, name, version, BUNDLE_REQUIREMENT, TOUCHPOINT_OSGI, tpData);
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
		return createIU(name, version, null, NO_REQUIRES, BUNDLE_CAPABILITY, NO_PROPERTIES, TOUCHPOINT_OSGI, NO_TP_DATA, false);
	}

	/**
	 * 	Create an Eclipse InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createEclipseIU(String name, Version version, RequiredCapability[] requires, TouchpointData touchpointData) {
		return createIU(name, version, null, requires, BUNDLE_CAPABILITY, NO_PROPERTIES, TOUCHPOINT_OSGI, touchpointData, false);
	}

	public static IEngine createEngine() {
		return (IEngine) ServiceHelper.getService(TestActivator.getContext(), IEngine.SERVICE_NAME);
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
	public static IInstallableUnit createIU(String name, Version version) {
		return createIU(name, version, null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false);
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
	public static IInstallableUnit createIU(String name, Version version, ProvidedCapability[] additionalProvides) {
		return createIU(name, version, null, NO_REQUIRES, additionalProvides, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false);
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
	public static IInstallableUnit createIU(String name, Version version, String filter, ProvidedCapability[] additionalProvides) {
		return createIU(name, version, filter, NO_REQUIRES, additionalProvides, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version, String filter, RequiredCapability[] required, ProvidedCapability[] additionalProvides, Map properties, TouchpointType tpType, TouchpointData tpData, boolean singleton) {
		return createIU(name, version, filter, required, additionalProvides, properties, tpType, tpData, singleton, null);
	}

	public static IInstallableUnit createIU(String name, Version version, String filter, RequiredCapability[] required, ProvidedCapability[] additionalProvides, Map properties, TouchpointType tpType, TouchpointData tpData, boolean singleton, IUpdateDescriptor update) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(name);
		iu.setVersion(version);
		iu.setFilter(filter);
		ProvidedCapability[] provides = new ProvidedCapability[additionalProvides.length + 1];
		provides[0] = getSelfCapability(name, version);
		for (int i = 0; i < additionalProvides.length; i++) {
			provides[i + 1] = additionalProvides[i];
		}
		for (Iterator iter = properties.keySet().iterator(); iter.hasNext();) {
			String nextKey = (String) iter.next();
			String nextValue = (String) properties.get(nextKey);
			iu.setProperty(nextKey, nextValue);
		}
		iu.setCapabilities(provides);
		iu.setRequiredCapabilities(required);
		iu.setTouchpointType(tpType);
		if (tpData != null)
			iu.addTouchpointData(tpData);
		iu.setSingleton(singleton);
		iu.setUpdateDescriptor(update);
		return MetadataFactory.createInstallableUnit(iu);
	}

	/**
	 * 	Create a basic InstallableUnitFragment with the given attributes. 
	 * The self and fragment provided capabilities are added to the IU.
	 */
	public static IInstallableUnitFragment createIUFragment(IInstallableUnit host, String name, Version version) {
		return createIUFragment(host, name, version, NO_REQUIRES, TouchpointType.NONE, NO_TP_DATA);
	}

	/**
	 * 	Create a basic InstallableUnitFragment with the given attributes. 
	 * The self and fragment provided capabilities are added to the IU.
	 */
	public static IInstallableUnitFragment createIUFragment(IInstallableUnit host, String name, Version version, RequiredCapability[] required, TouchpointType tpType, TouchpointData tpData) {
		InstallableUnitFragmentDescription fragment = new InstallableUnitFragmentDescription();
		fragment.setId(name);
		fragment.setVersion(version);
		fragment.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		fragment.setRequiredCapabilities(required);
		fragment.setTouchpointType(tpType);
		if (tpData != null)
			fragment.addTouchpointData(tpData);
		if (host != null) {
			VersionRange hostRange = new VersionRange(host.getVersion(), true, host.getVersion(), true);
			fragment.setHost(host.getId(), hostRange);
		}
		return MetadataFactory.createInstallableUnitFragment(fragment);
	}

	/**
	 * 	Create a basic InstallableUnitFragment with the given attributes. 
	 * The self and fragment provided capabilities are added to the IU.
	 */
	public static IInstallableUnitFragment createIUFragment(String name) {
		return createIUFragment(null, name, DEFAULT_VERSION, NO_REQUIRES, TouchpointType.NONE, NO_TP_DATA);
	}

	/**
	 * 	Create a basic InstallableUnitFragment with the given attributes. 
	 * The self and fragment provided capabilities are added to the IU.
	 */
	public static IInstallableUnitFragment createIUFragment(String name, Version version) {
		return createIUFragment(null, name, version, NO_REQUIRES, TouchpointType.NONE, NO_TP_DATA);
	}

	public static IPlanner createPlanner() {
		return (IPlanner) ServiceHelper.getService(TestActivator.getContext(), IPlanner.class.getName());
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
		return new RequiredCapability[] {MetadataFactory.createRequiredCapability(namespace, name, range, filter, false, false)};
	}

	public static boolean delete(File file) {
		if (!file.exists())
			return true;
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			for (int i = 0; i < children.length; i++)
				delete(children[i]);
		}
		return file.delete();
	}

	/**
	 * 	Compare two 2-dimensional arrays of strings for equality
	 */
	protected static boolean equal(String[][] tuples0, String[][] tuples1) {
		if (tuples0.length != tuples1.length)
			return false;
		for (int i = 0; i < tuples0.length; i++) {
			String[] tuple0 = tuples0[i];
			String[] tuple1 = tuples1[i];
			if (tuple0.length != tuple1.length)
				return false;
			for (int j = 0; j < tuple0.length; j++) {
				if (!tuple0[j].equals(tuple1[j]))
					return false;
			}
		}
		return true;
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

	public static Iterator getInstallableUnits(IProfile profile2) {
		return profile2.query(InstallableUnitQuery.ANY, new Collector(), null).iterator();
	}

	/**
	 * 	Get the 'self' capability for the given installable unit.
	 */
	protected static ProvidedCapability getSelfCapability(IInstallableUnit iu) {
		return getSelfCapability(iu.getId(), iu.getVersion());
	}

	/**
	 * 	Get the 'self' capability for an installable unit with the give id and version.
	 */
	private static ProvidedCapability getSelfCapability(String installableUnitId, Version installableUnitVersion) {
		return MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, installableUnitId, installableUnitVersion);
	}

	private static void indent(OutputStream output, int indent) {
		for (int i = 0; i < indent; i++)
			try {
				output.write("\t".getBytes());
			} catch (IOException e) {
				// ignore
			}
	}

	public static void printProfile(IProfile profile) {
		boolean containsIU = false;
		for (Iterator iterator = getInstallableUnits(profile); iterator.hasNext();) {
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
	 * Creates a profile with the given name. Ensures that no such profile
	 * already exists.  The returned profile will be removed automatically
	 * in the tearDown method.
	 */
	protected IProfile createProfile(String name) {
		return createProfile(name, null, null);
	}

	/**
	 * Creates a profile with the given name. Ensures that no such profile
	 * already exists.  The returned profile will be removed automatically
	 * in the tearDown method.
	 */
	protected IProfile createProfile(String name, String parentId) {
		return createProfile(name, parentId, null);
	}

	protected IProfile createProfile(String name, String parentId, Map properties) {
		//remove any existing profile with the same name
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		profileRegistry.removeProfile(name);
		//create and return a new profile
		return profileRegistry.addProfile(name, properties, parentId);
	}

	protected IProfile getProfile(String profileId) {
		//remove any existing profile with the same name
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		return profileRegistry.getProfile(profileId);
	}

	/**
	 * Returns a resolved IU corresponding to the given IU, with no attached fragments.
	 */
	protected IInstallableUnit createResolvedIU(IInstallableUnit unit) {
		return MetadataFactory.createResolvedInstallableUnit(unit, new IInstallableUnitFragment[0]);
	}

	/**
	 * Adds a test metadata repository to the system that provides the given units. 
	 * The repository will automatically be removed in the tearDown method.
	 */
	protected void createTestMetdataRepository(IInstallableUnit[] units) {
		IMetadataRepository repo = new TestMetadataRepository(units);
		MetadataRepositoryManager repoMan = (MetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
		assertNotNull(repoMan);
		repoMan.addRepository(repo);
		metadataRepos.add(repo);
	}

	public String getUniqueString() {
		return System.currentTimeMillis() + "-" + Math.random();
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
				repoMan.removeRepository(repo.getLocation());
			}
			metadataRepos.clear();
		}
		//remove all profiles created by this test
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		for (Iterator it = profilesToRemove.iterator(); it.hasNext();) {
			IProfile toRemove = (IProfile) it.next();
			profileRegistry.removeProfile(toRemove.getProfileId());
		}
		profilesToRemove.clear();
		//See bug 209069 - currently no way to persist install registry changes or clear the metadata cache
		//		IMetadataRepository cache = MetadataCache.getCacheInstance((MetadataRepositoryManager) repoMan);
		//		cache.removeAll();
	}
}
