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
import java.net.URI;
import java.net.URL;
import java.util.*;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * Base class for provisioning tests with convenience methods used by multiple tests.
 */
public abstract class AbstractProvisioningTest extends TestCase {

	protected static final VersionRange ANY_VERSION = new VersionRange(Version.emptyVersion, true, new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), true);
	private static final ProvidedCapability[] BUNDLE_CAPABILITY = new ProvidedCapability[] {MetadataFactory.createProvidedCapability("eclipse.touchpoint", "bundle", new Version(1, 0, 0))};

	private static final RequiredCapability[] BUNDLE_REQUIREMENT = new RequiredCapability[] {MetadataFactory.createRequiredCapability("eclipse.touchpoint", "bundle", VersionRange.emptyRange, null, false, true)};

	protected static final Version DEFAULT_VERSION = new Version(1, 0, 0);
	public static final TouchpointType TOUCHPOINT_OSGI = MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.osgi", new Version(1, 0, 0));

	protected static final Map NO_PROPERTIES = Collections.EMPTY_MAP;
	protected static final ProvidedCapability[] NO_PROVIDES = new ProvidedCapability[0];
	protected static final RequiredCapability[] NO_REQUIRES = new RequiredCapability[0];

	protected static final TouchpointData NO_TP_DATA = MetadataFactory.createTouchpointData(new HashMap());

	//flag used to disable currently failing (invalid) tests. This should always be set to true
	protected boolean DISABLED = true;

	/**
	 * Tracks the metadata repositories created by this test instance. The repositories
	 * will be removed automatically at the end of the test.
	 */
	private List metadataRepos = new ArrayList();
	/**
	 * Tracks the profile ids created by this test instance. The profiles
	 * will be removed automatically at the end of the test.
	 */
	protected List profilesToRemove = new ArrayList();

	private File testFolder = null;

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

	protected static void assertOK(String message, IStatus status) {
		if (status.isOK())
			return;

		// print out the children if we have any
		IStatus children[] = status.getChildren();
		for (int i = 0; i < children.length; i++) {
			IStatus child = children[i];
			if (!child.isOK())
				new CoreException(child).printStackTrace();
		}

		fail(message + ' ' + status.getMessage(), status.getException());
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
	public static void copy(String message, File source, File target) {
		if (!source.exists())
			return;
		if (source.isDirectory()) {
			if (target.exists() && target.isFile())
				target.delete();
			if (!target.exists())
				target.mkdirs();
			File[] children = source.listFiles();
			for (int i = 0; i < children.length; i++)
				copy(message, children[i], new File(target, children[i].getName()));
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
		} catch (IOException e) {
			fail(message, e);
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
		InstallableUnitFragmentDescription fragment = new InstallableUnitFragmentDescription();
		fragment.setId(name);
		fragment.setVersion(DEFAULT_VERSION);
		fragment.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		fragment.setTouchpointType(TOUCHPOINT_OSGI);
		fragment.addTouchpointData(NO_TP_DATA);
		fragment.setHost(BUNDLE_REQUIREMENT);
		return MetadataFactory.createInstallableUnitFragment(fragment);

		//		return createIUFragment(null, name, DEFAULT_VERSION, BUNDLE_REQUIREMENT, TOUCHPOINT_OSGI, NO_TP_DATA);
	}

	/**
	 * 	Create an eclipse InstallableUnitFragment with the given name and version
	 *  that is hosted by any bundle. The default self and fragment provided capabilities
	 *  are added to the IU.
	 */
	public static IInstallableUnitFragment createBundleFragment(String name, Version version, TouchpointData tpData) {
		return createIUFragment(null, name, version, BUNDLE_REQUIREMENT, TOUCHPOINT_OSGI, tpData);
	}

	public IInstallableUnit createBundleIU(BundleDescription bd, boolean isFolder, IArtifactKey key) {
		PublisherInfo info = new PublisherInfo();
		String shape = isFolder ? IBundleShapeAdvice.DIR : IBundleShapeAdvice.JAR;
		info.addAdvice(new BundleShapeAdvice(bd.getSymbolicName(), bd.getVersion(), shape));
		return BundlesAction.createBundleIU(bd, key, info);
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

	public static IInstallableUnitPatch createIUPatch(String name, Version version, boolean singleton, RequirementChange[] changes, RequiredCapability[][] scope, RequiredCapability lifeCycle) {
		return createIUPatch(name, version, null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, singleton, null, changes, scope, lifeCycle);
	}

	public static IInstallableUnitPatch createIUPatch(String name, Version version, String filter, RequiredCapability[] required, ProvidedCapability[] additionalProvides, Map properties, TouchpointType tpType, TouchpointData tpData, boolean singleton, IUpdateDescriptor update, RequirementChange[] reqChanges, RequiredCapability[][] scope, RequiredCapability lifeCycle) {
		InstallableUnitPatchDescription iu = new MetadataFactory.InstallableUnitPatchDescription();
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
		iu.setRequirementChanges(reqChanges);
		iu.setApplicabilityScope(scope);
		iu.setLifeCycle(lifeCycle);
		return MetadataFactory.createInstallableUnitPatch(iu);
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
			fragment.setHost(new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, host.getId(), hostRange, null, false, false)});
		}
		fragment.setCapabilities(new ProvidedCapability[] {getSelfCapability(name, version)});
		return MetadataFactory.createInstallableUnitFragment(fragment);
	}

	public static void changeVersion(InstallableUnitDescription desc, Version newVersion) {
		ProvidedCapability[] capabilities = desc.getProvidedCapabilities();
		for (int i = 0; i < capabilities.length; i++) {
			if (desc.getVersion().equals(capabilities[i].getVersion()))
				capabilities[i] = MetadataFactory.createProvidedCapability(capabilities[i].getNamespace(), capabilities[i].getName(), newVersion);
		}
		desc.setVersion(newVersion);
	}

	public static MetadataFactory.InstallableUnitDescription createIUDescriptor(IInstallableUnit prototype) {
		InstallableUnitDescription desc = new MetadataFactory.InstallableUnitDescription();
		desc.setArtifacts(prototype.getArtifacts());
		ProvidedCapability originalCapabilities[] = prototype.getProvidedCapabilities();
		ProvidedCapability newCapabilities[] = new ProvidedCapability[originalCapabilities.length];
		for (int i = 0; i < originalCapabilities.length; i++) {
			newCapabilities[i] = MetadataFactory.createProvidedCapability(originalCapabilities[i].getNamespace(), originalCapabilities[i].getName(), originalCapabilities[i].getVersion());
		}
		desc.setCapabilities(newCapabilities);
		desc.setCopyright(prototype.getCopyright());
		desc.setFilter(prototype.getFilter());
		desc.setId(prototype.getId());
		desc.setLicense(prototype.getLicense());
		RequiredCapability[] originalRequirements = prototype.getRequiredCapabilities();
		RequiredCapability[] newRequirements = new RequiredCapability[originalRequirements.length];
		for (int i = 0; i < newRequirements.length; i++) {
			newRequirements[i] = MetadataFactory.createRequiredCapability(originalRequirements[i].getNamespace(), originalRequirements[i].getName(), originalRequirements[i].getRange(), originalRequirements[i].getFilter(), originalRequirements[i].isOptional(), originalRequirements[i].isMultiple(), originalRequirements[i].isGreedy());
		}
		desc.setRequiredCapabilities(prototype.getRequiredCapabilities());
		desc.setSingleton(prototype.isSingleton());
		desc.setTouchpointType(MetadataFactory.createTouchpointType(prototype.getTouchpointType().getId(), prototype.getTouchpointType().getVersion()));
		desc.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(prototype.getUpdateDescriptor().getId(), prototype.getUpdateDescriptor().getRange(), prototype.getUpdateDescriptor().getSeverity(), prototype.getUpdateDescriptor().getDescription()));
		desc.setVersion(prototype.getVersion());
		Map prototypeProperties = prototype.getProperties();
		Set entries = prototypeProperties.entrySet();
		for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			desc.setProperty((String) entry.getKey(), (String) entry.getValue());
		}
		return desc;
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
			write(status, 0, System.err);
		} else {
			if (e != null)
				e.printStackTrace();
		}
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

	private static void write(IStatus status, int indent, PrintStream output) {
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
				write(children[i], indent + 1, output);
		}
	}

	public AbstractProvisioningTest() {
		super("");
	}

	public AbstractProvisioningTest(String name) {
		super(name);
	}

	protected static void assertEquals(String message, byte[] expected, byte[] actual) {
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

	protected static void assertEquals(String message, Object[] expected, Object[] actual) {
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
		profilesToRemove.add(name);
		//create and return a new profile
		try {
			return profileRegistry.addProfile(name, properties, parentId);
		} catch (ProvisionException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	protected IProgressMonitor getMonitor() {
		return new NullProgressMonitor();
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

	protected IArtifactRepository createArtifactRepository(URI location, Map properties) throws ProvisionException {
		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		IArtifactRepository repo = artifactRepositoryManager.createRepository(location, "artifact", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		artifactRepositoryManager.removeRepository(repo.getLocation());
		return repo;
	}

	protected static IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	protected IMetadataRepository createMetadataRepository(URI location, Map properties) throws ProvisionException {
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		IMetadataRepository repo = metadataRepositoryManager.createRepository(location, "metadata", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		metadataRepos.add(repo);
		return repo;
	}

	protected static IMetadataRepositoryManager getMetadataRepositoryManager() {
		return (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
	}

	public static String getUniqueString() {
		return System.currentTimeMillis() + "-" + Math.random();
	}

	public File getTempFolder() {
		return getTestFolder(getUniqueString());
	}

	protected File getTestFolder(String name) {
		Location instanceLocation = (Location) ServiceHelper.getService(TestActivator.getContext(), Location.class.getName(), Location.INSTANCE_FILTER);
		URL url = instanceLocation != null ? instanceLocation.getURL() : null;
		if (instanceLocation == null || !instanceLocation.isSet() || url == null) {
			String tempDir = System.getProperty("java.io.tmpdir");
			testFolder = new File(tempDir, name);
		} else {
			File instance = URLUtil.toFile(url);
			testFolder = new File(instance, name);
		}

		if (testFolder.exists())
			delete(testFolder);
		testFolder.mkdirs();
		return testFolder;
	}

	protected void runTest() throws Throwable {
		super.runTest();

		//clean up after success
		if (testFolder != null && testFolder.exists()) {
			delete(testFolder);
			testFolder = null;
		}
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
		URI[] urls = repoMan.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (int i = 0; i < urls.length; i++) {
			try {
				if (urls[i].toString().indexOf("cache") != -1 || urls[i].toString().indexOf("rollback") != -1)
					repoMan.loadRepository(urls[i], null).removeAll();
			} catch (ProvisionException e) {
				//if the repository didn't load, then it doesn't exist and we don't need to clear it up
			}
		}
		//remove all profiles created by this test
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		for (Iterator it = profilesToRemove.iterator(); it.hasNext();) {
			String toRemove = (String) it.next();
			profileRegistry.removeProfile(toRemove);
		}
		profilesToRemove.clear();
		//See bug 209069 - currently no way to persist install registry changes or clear the metadata cache
		//		IMetadataRepository cache = MetadataCache.getCacheInstance((MetadataRepositoryManager) repoMan);
		//		cache.removeAll();
	}

	/*
	 * Look up and return a file handle to the given entry in the bundle.
	 */
	protected File getTestData(String message, String entry) {
		if (entry == null)
			fail(message + " entry is null.");
		URL base = TestActivator.getContext().getBundle().getEntry(entry);
		if (base == null)
			fail(message + " entry not found in bundle: " + entry);
		try {
			String osPath = new Path(FileLocator.toFileURL(base).getPath()).toOSString();
			File result = new File(osPath);
			if (!result.getCanonicalPath().equals(result.getPath()))
				fail(message + " result path: " + result.getPath() + " does not match canonical path: " + result.getCanonicalFile().getPath());
			return result;
		} catch (IOException e) {
			fail(message, e);
		}
		// avoid compile error... should never reach this code
		return null;
	}

	protected static void assertInstallOperand(ProvisioningPlan plan, IInstallableUnit iu) {
		Operand[] ops = plan.getOperands();
		for (int i = 0; i < ops.length; i++) {
			if (ops[i] instanceof InstallableUnitOperand) {
				InstallableUnitOperand iuOp = (InstallableUnitOperand) ops[i];
				if (iu.equals(iuOp.second()))
					return;
			}
		}
		fail("Can't find " + iu + " in the plan");
	}

	protected static void assertUninstallOperand(ProvisioningPlan plan, IInstallableUnit iu) {
		Operand[] ops = plan.getOperands();
		for (int i = 0; i < ops.length; i++) {
			if (ops[i] instanceof InstallableUnitOperand) {
				InstallableUnitOperand iuOp = (InstallableUnitOperand) ops[i];
				if (iu.equals(iuOp.first()))
					return;
			}
		}
		fail("Can't find " + iu + " in the plan");
	}

	protected static void assertNoOperand(ProvisioningPlan plan, IInstallableUnit iu) {
		Operand[] ops = plan.getOperands();
		for (int i = 0; i < ops.length; i++) {
			if (ops[i] instanceof InstallableUnitOperand) {
				InstallableUnitOperand iuOp = (InstallableUnitOperand) ops[i];
				if (iuOp.second() != null && iuOp.second().equals(iu))
					fail(iu + " should not be present in this plan.");
				if (iuOp.first() != null && iuOp.first().equals(iu))
					fail(iu + " should not be present in this plan.");
			}
		}
	}

	protected void setUp() throws Exception {
		super.setUp();
		MetadataRepositoryManager repoMan = (MetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
		URI[] repos = repoMan.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (int i = 0; i < repos.length; i++) {
			repoMan.removeRepository(repos[i]);
		}
	}

	protected IStatus install(IProfile profile, IInstallableUnit[] ius, boolean strict, IPlanner planner, IEngine engine) {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(ius);
		for (int i = 0; i < ius.length; i++) {
			req.setInstallableUnitInclusionRules(ius[i], strict ? PlannerHelper.createStrictInclusionRule(ius[i]) : PlannerHelper.createOptionalInclusionRule(ius[i]));
		}

		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		if (plan.getStatus().getSeverity() == IStatus.ERROR || plan.getStatus().getSeverity() == IStatus.CANCEL)
			return plan.getStatus();
		return engine.perform(profile, new DefaultPhaseSet(), plan.getOperands(), null, null);
	}

	protected IStatus uninstall(IProfile profile, IInstallableUnit[] ius, IPlanner planner, IEngine engine) {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.removeInstallableUnits(ius);

		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		return engine.perform(profile, new DefaultPhaseSet(), plan.getOperands(), null, null);
	}

	protected static void assertEquals(String message, Object[] expected, Object[] actual, boolean orderImportant) {
		// if the order in the array must match exactly, then call the other method
		if (orderImportant) {
			assertEquals(message, expected, actual);
			return;
		}
		// otherwise use this method and check that the arrays are equal in any order
		if (expected == null && actual == null)
			return;
		if (expected == actual)
			return;
		if (expected == null || actual == null)
			assertTrue(message + ".1", false);
		if (expected.length != actual.length)
			assertTrue(message + ".2", false);
		boolean[] found = new boolean[expected.length];
		for (int i = 0; i < expected.length; i++) {
			for (int j = 0; j < expected.length; j++) {
				if (!found[j] && expected[i].equals(actual[j]))
					found[j] = true;
			}
		}
		for (int i = 0; i < found.length; i++)
			if (!found[i])
				assertTrue(message + ".3." + i, false);
	}

	protected IProvisioningEventBus getEventBus() {
		IProvisioningEventBus bus = (IProvisioningEventBus) ServiceHelper.getService(TestActivator.getContext(), IProvisioningEventBus.SERVICE_NAME);
		assertNotNull(bus);
		return bus;
	}

	protected static void assertEquals(String message, IInstallableUnit iu1, IInstallableUnit iu2) throws AssertionFailedError {
		if (iu1 == iu2)
			return;
		if (iu1 == null || iu2 == null) {
			fail(message);
		}

		if (!iu1.equals(iu2))
			fail(message + " " + iu1 + " is not equal to " + iu2);

		if (iu1.isFragment()) {
			if (!iu2.isFragment())
				fail(message + " " + iu1 + " is not a fragment.");
			try {
				assertEquals(message, ((IInstallableUnitFragment) iu1).getHost(), ((IInstallableUnitFragment) iu2).getHost());
			} catch (AssertionFailedError failure) {
				fail(message + " Unequal hosts: " + failure.getMessage());
			}
		} else if (iu2.isFragment()) {
			fail(message + " " + iu2 + " is a fragment.");
		}

		if (iu1.isSingleton()) {
			if (!iu2.isSingleton())
				fail(message + " " + iu2 + " is not a singleton.");
		} else if (iu2.isSingleton()) {
			fail(message + " " + iu2 + " is a singleton.");
		}

		assertEquals(message, iu1.getProvidedCapabilities(), iu2.getProvidedCapabilities());
		assertEquals(message, iu1.getRequiredCapabilities(), iu2.getRequiredCapabilities());
		assertEquals(message, iu1.getArtifacts(), iu2.getArtifacts());
		assertEquals(message, iu1.getTouchpointType(), iu2.getTouchpointType());
		assertEquals(message, iu1.getTouchpointData(), iu2.getTouchpointData());
		assertEquals(message, iu1.getProperties(), iu2.getProperties());
		assertEquals(message, iu1.getLicense(), iu2.getLicense());
		assertEquals(message, iu1.getCopyright(), iu2.getCopyright());
		assertEquals(message, iu1.getUpdateDescriptor(), iu2.getUpdateDescriptor());
		assertEquals(message, iu1.getFilter(), iu2.getFilter());

		if (iu1.isResolved() && iu2.isResolved())
			assertEquals(message, iu1.getFragments(), iu2.getFragments());
	}

	/*
	 * Return a boolean value indicating whether or not the given arrays of installable
	 * units representing fragments are considered to be equal.
	 */
	protected static void assertEquals(String message, IInstallableUnitFragment[] fragments1, IInstallableUnitFragment[] fragments2) throws AssertionFailedError {
		Map map = new HashMap(fragments2.length);
		for (int i = 0; i < fragments2.length; i++) {
			map.put(fragments2[i], fragments2[i]);
		}

		for (int i = 0; i < fragments1.length; i++) {
			if (!map.containsKey(fragments1))
				fail(message + " Expected fragment '" + fragments1[i] + "' not present.");
			else {
				assertEquals(message, fragments1[i], map.remove(fragments1[i]));
			}
		}

		if (map.size() > 0)
			fail(message + " Unexpected fragment '" + map.entrySet().iterator().next() + "'");
	}

	/*
	 * Return a boolean value indicating whether or not the given update descriptor
	 * objects are considered to be equal.
	 */
	protected static void assertEquals(String message, IUpdateDescriptor desc1, IUpdateDescriptor desc2) throws AssertionFailedError {
		if (desc1 == desc2)
			return;
		if (desc1 == null || desc2 == null)
			fail();

		try {
			assertEquals(message, desc1.getId(), desc2.getId());
			assertEquals(message, desc1.getSeverity(), desc2.getSeverity());
			assertEquals(message, desc1.getRange(), desc2.getRange());

			String d1 = desc1.getDescription();
			String d2 = desc2.getDescription();
			if (d1 == null)
				d1 = "";
			if (d2 == null)
				d2 = "";
			assertEquals(message, d1, d2);
		} catch (AssertionFailedError e) {
			fail(message + " Unequal Update Descriptors: " + e.getMessage());
		}
	}

	/**
	 * Assumes each array does not contain more than one IU with a given name and version.
	 */
	public static void assertEquals(String message, IInstallableUnit[] ius1, IInstallableUnit[] ius2) {
		TreeSet set = new TreeSet(new Comparator() {
			public int compare(Object o1, Object o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});
		set.addAll(Arrays.asList(ius2));

		for (int i = 0; i < ius1.length; i++) {
			// + "\0" is a successor for strings
			SortedSet subset = set.subSet(ius1[i], ius1[i].toString() + "\0");
			if (subset.size() == 1) {
				IInstallableUnit candidate = (IInstallableUnit) subset.first();
				try {
					assertEquals(message, ius1[i], candidate);
				} catch (AssertionFailedError e) {
					fail(message + " IUs '" + ius1[i] + "' are unequal : " + e.getMessage());
				}
				subset.remove(candidate);
			} else if (subset.size() > 1) {
				//should not happen
				fail(message + " ERROR: Unexpected failure.");
			} else {
				fail(message + " Expected IU " + ius1[i] + " not found.");
			}
		}
		if (set.size() > 0)
			fail(message + " Unexpected IU " + set.first() + ".");
	}

	/*
	 * Compare 2 copyright objects and fail if they are not considered equal.
	 */
	protected static void assertEquals(String message, Copyright cpyrt1, Copyright cpyrt2) {
		if (cpyrt1 == cpyrt2)
			return;
		if (cpyrt1 == null || cpyrt2 == null) {
			fail(message);
		}
		assertEquals(message, cpyrt1.getBody(), cpyrt2.getBody());
		assertEquals(message, cpyrt1.getLocation().toString(), cpyrt2.getLocation().toString());
	}

	/**
	 * matches all descriptors from source in the destination
	 * Note: NOT BICONDITIONAL! assertContains(A, B) is NOT the same as assertContains(B, A)
	 */
	protected static void assertContains(String message, IArtifactRepository sourceRepo, IArtifactRepository destinationRepo) {
		IArtifactKey[] sourceKeys = sourceRepo.getArtifactKeys();

		for (int i = 0; i < sourceKeys.length; i++) {
			IArtifactDescriptor[] destinationDescriptors = destinationRepo.getArtifactDescriptors(sourceKeys[i]);
			if (destinationDescriptors == null || destinationDescriptors.length == 0)
				fail(message + ": unmatched key: " + sourceKeys[i].toString());
			//this implicitly verifies the keys are present

			IArtifactDescriptor[] sourceDescriptors = sourceRepo.getArtifactDescriptors(sourceKeys[i]);

			assertEquals(message, sourceDescriptors, destinationDescriptors, false); //order doesn't matter
		}
	}

	/**
	 * Ensures 2 repositories are equal by ensure all items in repo1 are contained
	 * in repo2 and all items in repo2 are in repo1
	 */
	protected static void assertContentEquals(String message, IArtifactRepository repo1, IArtifactRepository repo2) {
		assertContains(message, repo1, repo2);
		assertContains(message, repo2, repo1);
	}

	/**
	 * matches all metadata from source in the destination
	 * Note: NOT BICONDITIONAL! assertContains(A, B) is NOT the same as assertContains(B, A)
	 */
	protected static void assertContains(String message, IMetadataRepository sourceRepo, IMetadataRepository destinationRepo) {
		Collector sourceCollector = sourceRepo.query(InstallableUnitQuery.ANY, new Collector(), null);
		Iterator it = sourceCollector.iterator();

		while (it.hasNext()) {
			IInstallableUnit sourceIU = (IInstallableUnit) it.next();
			Collector destinationCollector = destinationRepo.query(new InstallableUnitQuery(sourceIU.getId(), sourceIU.getVersion()), new Collector(), null);
			assertEquals(message, 1, destinationCollector.size());
			assertEquals(message, sourceIU, (IInstallableUnit) destinationCollector.iterator().next());
		}
	}

	/**
	 * Ensures 2 repositories are equal by ensure all items in repo1 are contained
	 * in repo2 and all items in repo2 are in repo1
	 */
	protected static void assertContentEquals(String message, IMetadataRepository repo1, IMetadataRepository repo2) {
		assertContains(message, repo1, repo2);
		assertContains(message, repo2, repo1);
	}

	/*
	 * Return a boolean value indicating whether or not the given installable units
	 * are considered to be equal.
	 */
	protected static boolean isEqual(IInstallableUnit iu1, IInstallableUnit iu2) {
		try {
			assertEquals("IUs not equal", iu1, iu2);
		} catch (AssertionFailedError e) {
			return false;
		}
		return true;
	}

	/**
	 * Returns true if running on Windows, and false otherwise.
	 */
	protected boolean isWindows() {
		return Platform.getOS().equals(Platform.OS_WIN32);
	}

	protected IUpdateDescriptor createUpdateDescriptor(String id, Version version) {
		return MetadataFactory.createUpdateDescriptor(id, new VersionRange(Version.emptyVersion, true, version, false), IUpdateDescriptor.HIGH, "desc");
	}

	/**
	 * Ensures 2 inputed Maps representing repository properties are equivalent
	 * A special assert is needed as the time stamp is expected to change
	 */
	protected static void assertRepositoryProperties(String message, Map expected, Map actual) {
		if (expected == null && actual == null)
			return;
		if (expected == null || actual == null)
			fail(message);
		Object[] expectedArray = expected.keySet().toArray();
		for (int i = 0; i < expectedArray.length; i++) {
			assertTrue(message, actual.containsKey(expectedArray[i])); //Ensure the key exists
			if (!expectedArray[i].equals("p2.timestamp")) //time stamp value is expected to change
				assertEquals(message, expected.get(expectedArray[i]), actual.get(expectedArray[i]));
		}
	}

	/**
	 * Assert that the given log file contains the given message
	 * The message is expected to be contained on a single line
	 * @param log
	 * @param msg
	 * @throws Exception
	 */
	public static void assertLogContainsLine(File log, String msg) throws Exception {
		assertLogContainsLines(log, new String[] {msg});
	}

	/**
	 * Assert that the given log file contains the given lines
	 * Lines are expected to appear in order
	 * @param log
	 * @param lines
	 * @throws Exception
	 */
	public static void assertLogContainsLines(File log, String[] lines) throws Exception {
		assertNotNull(log);
		assertTrue(log.exists());
		assertTrue(log.length() > 0);

		int idx = 0;
		BufferedReader reader = new BufferedReader(new FileReader(log));
		while (reader.ready()) {
			String line = reader.readLine();
			if (line.indexOf(lines[idx]) >= 0) {
				if (++idx >= lines.length) {
					reader.close();
					return;
				}
			}
		}
		reader.close();
		assertTrue(false);
	}
}
