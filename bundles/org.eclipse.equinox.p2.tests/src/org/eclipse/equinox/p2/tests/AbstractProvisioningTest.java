/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 ******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.tests.harness.FussyProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.ICopyright;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IInstallableUnitPatch;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.IRequirementChange;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.BundleShapeAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.IBundleShapeAdvice;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.ArtifactDescriptorQuery;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Base class for provisioning tests with convenience methods used by multiple tests.
 */
public abstract class AbstractProvisioningTest extends TestCase {

	protected static final VersionRange ANY_VERSION = VersionRange.emptyRange;
	protected static final IProvidedCapability[] BUNDLE_CAPABILITY = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("eclipse.touchpoint", "bundle", Version.createOSGi(1, 0, 0))};

	private static final IRequirement[] BUNDLE_REQUIREMENT = new IRequirement[] {MetadataFactory.createRequirement("eclipse.touchpoint", "bundle", VersionRange.emptyRange, null, false, true)};

	protected static final Version DEFAULT_VERSION = Version.createOSGi(1, 0, 0);
	public static final ITouchpointType TOUCHPOINT_OSGI = MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.osgi", Version.createOSGi(1, 0, 0));

	protected static final Map<String, String> NO_PROPERTIES = Collections.emptyMap();
	protected static final IProvidedCapability[] NO_PROVIDES = new IProvidedCapability[0];
	protected static final IRequiredCapability[] NO_REQUIRES = new IRequiredCapability[0];

	protected static final ITouchpointData NO_TP_DATA = MetadataFactory.createTouchpointData(new HashMap<>());

	//flag used to disable currently failing (invalid) tests. This should always be set to true
	protected boolean DISABLED = true;

	/**
	 * Tracks the metadata repositories created by this test instance. The repositories
	 * will be removed automatically at the end of the test.
	 */
	private List<IMetadataRepository> metadataRepos = new ArrayList<>();
	/**
	 * Tracks the profile ids created by this test instance. The profiles
	 * will be removed automatically at the end of the test.
	 */
	protected List<String> profilesToRemove = new ArrayList<>();

	private File testFolder = null;
	protected Object previousSelfValue = null;

	public static void assertEmptyProfile(IProfile profile) {
		assertNotNull("The profile should not be null", profile);
		if (getInstallableUnits(profile).hasNext())
			fail("The profile should be empty,profileId=" + profile);
	}

	protected static void assertNotIUs(IInstallableUnit[] ius, Iterator<IInstallableUnit> installableUnits) {
		Set<IInstallableUnit> notexpected = new HashSet<>();
		notexpected.addAll(Arrays.asList(ius));

		while (installableUnits.hasNext()) {
			IInstallableUnit next = installableUnits.next();
			if (notexpected.contains(next)) {
				fail("not expected [" + next + "]");
			}
		}
	}

	protected static void assertNotOK(IStatus result) {
		assertNotOK("The status should not have been OK", result);
	}

	protected static void assertNotOK(String message, IStatus result) {
		assertTrue(message, !result.isOK());
	}

	public static void assertOK(IStatus status) {
		assertOK("The status should have been OK.", status);
	}

	protected static void assertOK(String message, IStatus status) {
		if (status.isOK())
			return;

		// print out the children if we have any
		IStatus children[] = status.getChildren();
		for (IStatus child : children) {
			if (!child.isOK())
				new CoreException(child).printStackTrace();
		}

		fail(message + ' ' + status.getMessage(), status.getException());
	}

	/**
	 * Asserts that the given profile contains *only* the given IUs.
	 */
	protected static void assertProfileContains(String message, IProfile profile, IInstallableUnit[] expectedUnits) {
		HashSet<IInstallableUnit> expected = new HashSet<>(Arrays.asList(expectedUnits));
		for (Iterator<IInstallableUnit> it = getInstallableUnits(profile); it.hasNext();) {
			IInstallableUnit actual = it.next();
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
		HashSet<IInstallableUnit> expected = new HashSet<>(Arrays.asList(expectedUnits));
		for (Iterator<IInstallableUnit> it = getInstallableUnits(profile2); it.hasNext();) {
			IInstallableUnit actual = it.next();
			expected.remove(actual);
		}
		if (!expected.isEmpty())
			fail(message + " profile " + profile2.getProfileId() + " did not contain expected units: " + expected);
	}

	public static void copy(String message, File source, File target) {
		copy(message, source, target, null);
	}

	/*
	 * Copy
	 * - if we have a file, then copy the file
	 * - if we have a directory then merge
	 */
	public static void copy(String message, File source, File target, FileFilter filter) {
		if (!source.exists())
			return;
		if (source.isDirectory()) {
			if (target.exists() && target.isFile())
				target.delete();
			if (!target.exists())
				target.mkdirs();
			File[] children = source.listFiles(filter);
			for (File child : children) {
				copy(message, child, new File(target, child.getName()));
			}
			return;
		}
		try (InputStream input = new BufferedInputStream(new FileInputStream(source)); OutputStream output = new BufferedOutputStream(new FileOutputStream(target));) {

			byte[] buffer = new byte[8192];
			int bytesRead = 0;
			while ((bytesRead = input.read(buffer)) != -1)
				output.write(buffer, 0, bytesRead);
		} catch (IOException e) {
			fail(message, e);
		}
	}

	public static void move(String message, File source, File target) {
		move(message, source, target, null);
	}

	public static void move(String message, File source, File target, FileFilter filter) {
		// no work to do
		if (!source.exists())
			return;

		// short circuit... if a basic rename just works then there is less work to do
		if (filter == null && source.renameTo(target))
			return;

		// folder move
		if (source.isDirectory()) {
			if (target.exists() && target.isFile())
				target.delete();
			if (!target.exists())
				target.mkdirs();
			File[] children = source.listFiles(filter);
			for (File child : children) {
				move(message, child, new File(target, child.getName()), filter);
			}
			return;
		}

		// delete destination folder if there is one. we are copying a file
		if (target.isDirectory())
			delete(target);

		// both source and target are files at this point
		if (source.renameTo(target))
			return;

		// if the rename didn't work then try a copy/delete
		copy(message, source, target);
		if (target.exists())
			delete(source);
	}

	/**
	 * 	Create an eclipse InstallableUnitFragment with the given name that is hosted
	 *  by any bundle. The fragment has the default version, and the default self and
	 *  fragment provided capabilities are added to the IU.
	 */
	public static IInstallableUnitFragment createBundleFragment(String name) {
		org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription fragment = new org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription();
		fragment.setId(name);
		fragment.setVersion(DEFAULT_VERSION);
		fragment.setProperty(org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		fragment.setTouchpointType(TOUCHPOINT_OSGI);
		fragment.addTouchpointData(NO_TP_DATA);
		fragment.setHost(BUNDLE_REQUIREMENT);
		fragment.setCapabilities(new IProvidedCapability[] {getSelfCapability(name, fragment.getVersion())});
		return MetadataFactory.createInstallableUnitFragment(fragment);

		//		return createIUFragment(null, name, DEFAULT_VERSION, BUNDLE_REQUIREMENT, TOUCHPOINT_OSGI, NO_TP_DATA);
	}

	/**
	 * 	Create an eclipse InstallableUnitFragment with the given name and version
	 *  that is hosted by any bundle. The default self and fragment provided capabilities
	 *  are added to the IU.
	 */
	public static IInstallableUnitFragment createBundleFragment(String name, Version version, ITouchpointData tpData) {
		return createIUFragment(null, name, version, BUNDLE_REQUIREMENT, TOUCHPOINT_OSGI, tpData);
	}

	public IInstallableUnit createBundleIU(BundleDescription bd, boolean isFolder, IArtifactKey key) {
		PublisherInfo info = new PublisherInfo();
		String shape = isFolder ? IBundleShapeAdvice.DIR : IBundleShapeAdvice.JAR;
		info.addAdvice(new BundleShapeAdvice(bd.getSymbolicName(), PublisherHelper.fromOSGiVersion(bd.getVersion()), shape));
		return BundlesAction.createBundleIU(bd, key, info);
	}

	public static IDirector createDirector() {
		return getAgent().getService(IDirector.class);
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

	public static IInstallableUnit createEclipseIUSingleton(String name, Version version) {
		return createIU(name, version, null, NO_REQUIRES, BUNDLE_CAPABILITY, NO_PROPERTIES, TOUCHPOINT_OSGI, NO_TP_DATA, true);
	}

	/**
	 * 	Create an Eclipse InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createEclipseIU(String name, Version version, IRequirement[] requires, ITouchpointData touchpointData) {
		return createIU(name, version, null, requires, BUNDLE_CAPABILITY, NO_PROPERTIES, TOUCHPOINT_OSGI, touchpointData, false);
	}

	public static IEngine createEngine() {
		return getAgent().getService(IEngine.class);
	}

	/**
	 * Creates and returns a correctly formatted LDAP filter with the given key and value.
	 */
	protected static IMatchExpression<IInstallableUnit> createFilter(String filterKey, String filterValue) {
		return InstallableUnit.parseFilter("(" + filterKey + '=' + filterValue + ')');
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
	public static IInstallableUnit createIU(String name, IProvidedCapability[] additionalProvides) {
		return createIU(name, DEFAULT_VERSION, null, NO_REQUIRES, additionalProvides, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, IRequirement[] requires) {
		return createIU(name, DEFAULT_VERSION, null, requires, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, IRequirement[] requires, boolean singleton) {
		return createIU(name, DEFAULT_VERSION, null, requires, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, singleton);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, String filter, IProvidedCapability[] additionalProvides) {
		return createIU(name, InstallableUnit.parseFilter(filter), additionalProvides);
	}

	public static IInstallableUnit createIU(String name, IMatchExpression<IInstallableUnit> filter, IProvidedCapability[] additionalProvides) {
		return createIU(name, DEFAULT_VERSION, filter, NO_REQUIRES, additionalProvides, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version) {
		return createIU(name, version, null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version, boolean singleton) {
		return createIU(name, version, null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, singleton);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version, IProvidedCapability[] additionalProvides) {
		return createIU(name, version, null, NO_REQUIRES, additionalProvides, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version, IRequirement[] required) {
		return createIU(name, version, null, required, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false);
	}

	public static IInstallableUnit createIUWithMetaRequirement(String name, Version version, boolean singleton, IRequirement[] requirements, IRequirement[] metaRequirements) {
		return createIU(name, version, null, requirements, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, singleton, null, metaRequirements);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version, IRequirement[] required, Map<String, String> properties, boolean singleton) {
		return createIU(name, version, null, required, NO_PROVIDES, properties, ITouchpointType.NONE, NO_TP_DATA, singleton);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version, String filter, IProvidedCapability[] additionalProvides) {
		return createIU(name, version, InstallableUnit.parseFilter(filter), additionalProvides);
	}

	public static IInstallableUnit createIU(String name, Version version, IMatchExpression<IInstallableUnit> filter, IProvidedCapability[] additionalProvides) {
		return createIU(name, version, filter, NO_REQUIRES, additionalProvides, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false);
	}

	/**
	 * 	Create a basic InstallableUnit with the given attributes. All other attributes
	 * assume default values, and the default self capability is also added to the IU.
	 */
	public static IInstallableUnit createIU(String name, Version version, IMatchExpression<IInstallableUnit> filter, IRequirement[] required, IProvidedCapability[] additionalProvides, Map<String, String> properties, ITouchpointType tpType, ITouchpointData tpData, boolean singleton) {
		return createIU(name, version, filter, required, additionalProvides, properties, tpType, tpData, singleton, null, null);
	}

	public static IInstallableUnitPatch createIUPatch(String name, Version version, boolean singleton, IRequirementChange[] changes, IRequirement[][] scope, IRequirement lifeCycle) {
		return createIUPatch(name, version, null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, singleton, null, changes, scope, lifeCycle, NO_REQUIRES);
	}

	public static IInstallableUnitPatch createIUPatch(String name, Version version, IMatchExpression<IInstallableUnit> filter, IRequirement[] required, IProvidedCapability[] additionalProvides, Map<String, String> properties, ITouchpointType tpType, ITouchpointData tpData, boolean singleton, IUpdateDescriptor update, IRequirementChange[] reqChanges, IRequirement[][] scope, IRequirement lifeCycle, IRequirement[] metaRequirements) {
		org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitPatchDescription iu = new MetadataFactory.InstallableUnitPatchDescription();
		iu.setId(name);
		iu.setVersion(version);
		iu.setFilter(filter);
		IProvidedCapability[] provides = new IProvidedCapability[additionalProvides.length + 1];
		provides[0] = getSelfCapability(name, version);
		for (int i = 0; i < additionalProvides.length; i++) {
			provides[i + 1] = additionalProvides[i];
		}
		for (Entry<String, String> entry : properties.entrySet()) {
			iu.setProperty(entry.getKey(), entry.getValue());
		}
		iu.setCapabilities(provides);
		iu.setRequirements(required);
		iu.setTouchpointType(tpType);
		if (tpData != null)
			iu.addTouchpointData(tpData);
		iu.setSingleton(singleton);
		iu.setUpdateDescriptor(update);
		iu.setRequirementChanges(reqChanges);
		iu.setApplicabilityScope(scope);
		iu.setLifeCycle(lifeCycle);
		iu.setMetaRequirements(metaRequirements);
		return MetadataFactory.createInstallableUnitPatch(iu);
	}

	public static IInstallableUnit createIU(String name, Version version, IMatchExpression<IInstallableUnit> filter, IRequirement[] required, IProvidedCapability[] additionalProvides, Map<String, String> properties, ITouchpointType tpType, ITouchpointData tpData, boolean singleton, IUpdateDescriptor update, IRequirement[] metaRequirements) {
		org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(name);
		iu.setVersion(version);
		iu.setFilter(filter);
		IProvidedCapability[] provides = new IProvidedCapability[additionalProvides.length + 1];
		provides[0] = getSelfCapability(name, version);
		for (int i = 0; i < additionalProvides.length; i++) {
			provides[i + 1] = additionalProvides[i];
		}
		for (String nextKey : properties.keySet()) {
			String nextValue = properties.get(nextKey);
			iu.setProperty(nextKey, nextValue);
		}
		iu.setCapabilities(provides);
		iu.setRequirements(required);
		iu.setTouchpointType(tpType);
		if (tpData != null)
			iu.addTouchpointData(tpData);
		iu.setSingleton(singleton);
		iu.setUpdateDescriptor(update);
		if (metaRequirements == null)
			metaRequirements = NO_REQUIRES;
		iu.setMetaRequirements(metaRequirements);
		return MetadataFactory.createInstallableUnit(iu);
	}

	/**
	 * 	Create a basic InstallableUnitFragment with the given attributes.
	 * The self and fragment provided capabilities are added to the IU.
	 */
	public static IInstallableUnitFragment createIUFragment(IInstallableUnit host, String name, Version version) {
		return createIUFragment(host, name, version, NO_REQUIRES, ITouchpointType.NONE, NO_TP_DATA);
	}

	/**
	 * 	Create a basic InstallableUnitFragment with the given attributes.
	 * The self and fragment provided capabilities are added to the IU.
	 */
	public static IInstallableUnitFragment createIUFragment(IInstallableUnit host, String name, Version version, IRequirement[] required, ITouchpointType tpType, ITouchpointData tpData) {
		org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription fragment = new org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription();
		fragment.setId(name);
		fragment.setVersion(version);
		fragment.setProperty(org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		fragment.setRequirements(required);
		fragment.setTouchpointType(tpType);
		if (tpData != null)
			fragment.addTouchpointData(tpData);
		if (host != null) {
			VersionRange hostRange = new VersionRange(host.getVersion(), true, host.getVersion(), true);
			fragment.setHost(new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, host.getId(), hostRange, null, false, false)});
		}
		fragment.setCapabilities(new IProvidedCapability[] {getSelfCapability(name, version)});
		return MetadataFactory.createInstallableUnitFragment(fragment);
	}

	public static void changeVersion(org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription desc, Version newVersion) {
		List<IProvidedCapability> capabilities = new ArrayList<>(desc.getProvidedCapabilities());
		for (int i = 0; i < capabilities.size(); i++) {
			IProvidedCapability pc = capabilities.get(i);
			if (desc.getVersion().equals(pc.getVersion()))
				capabilities.set(i, MetadataFactory.createProvidedCapability(pc.getNamespace(), pc.getName(), newVersion));
		}
		desc.setVersion(newVersion);
		desc.setCapabilities(capabilities.toArray(new IProvidedCapability[capabilities.size()]));
	}

	public static MetadataFactory.InstallableUnitDescription createIUDescriptor(IInstallableUnit prototype) {
		org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription desc = new MetadataFactory.InstallableUnitDescription();
		Collection<IArtifactKey> originalArtifacts = prototype.getArtifacts();
		desc.setArtifacts(originalArtifacts.toArray(new IArtifactKey[originalArtifacts.size()]));
		Collection<IProvidedCapability> originalCapabilities = prototype.getProvidedCapabilities();
		desc.setCapabilities(originalCapabilities.toArray(new IProvidedCapability[originalCapabilities.size()]));
		desc.setCopyright(prototype.getCopyright());
		desc.setFilter(prototype.getFilter());
		desc.setId(prototype.getId());
		Collection<ILicense> originalLicenses = prototype.getLicenses();
		desc.setLicenses(originalLicenses.toArray(new ILicense[originalLicenses.size()]));
		Collection<IRequirement> originalRequirements = prototype.getRequirements();
		desc.setRequirements(originalRequirements.toArray(new IRequirement[originalRequirements.size()]));
		originalRequirements = prototype.getMetaRequirements();
		desc.setMetaRequirements(originalRequirements.toArray(new IRequirement[originalRequirements.size()]));
		desc.setSingleton(prototype.isSingleton());
		desc.setTouchpointType(MetadataFactory.createTouchpointType(prototype.getTouchpointType().getId(), prototype.getTouchpointType().getVersion()));
		desc.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(prototype.getUpdateDescriptor().getIUsBeingUpdated(), prototype.getUpdateDescriptor().getSeverity(), prototype.getUpdateDescriptor().getDescription(), null));
		desc.setVersion(prototype.getVersion());
		Map<String, String> prototypeProperties = prototype.getProperties();
		Set<Entry<String, String>> entries = prototypeProperties.entrySet();
		for (Entry<String, String> entry : entries) {
			desc.setProperty(entry.getKey(), entry.getValue());
		}
		return desc;
	}

	public static IPlanner createPlanner() {
		return getAgent().getService(IPlanner.class);
	}

	/**
	 * Creates and returns a required capability with the provided attributes.
	 */
	protected static IRequirement[] createRequiredCapabilities(String namespace, String name) {
		return createRequiredCapabilities(namespace, name, ANY_VERSION, (IMatchExpression<IInstallableUnit>) null);
	}

	protected static IRequirement[] createRequiredCapabilities(String namespace, String name, String filter) {
		return createRequiredCapabilities(namespace, name, ANY_VERSION, filter);
	}

	protected static IRequirement[] createRequiredCapabilities(String namespace, String name, VersionRange range) {
		return createRequiredCapabilities(namespace, name, range, (IMatchExpression<IInstallableUnit>) null);
	}

	protected static IRequirement[] createRequiredCapabilities(String namespace, String name, VersionRange range, String filter) {
		return createRequiredCapabilities(namespace, name, range, InstallableUnit.parseFilter(filter));
	}

	protected static IRequirement[] createRequiredCapabilities(String namespace, String name, VersionRange range, IMatchExpression<IInstallableUnit> filter) {
		return new IRequirement[] {MetadataFactory.createRequirement(namespace, name, range, filter, false, false)};
	}

	public static IRequirement createIURequirement(String name, VersionRange range) {
		return MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, name, range, null, false, false);
	}

	public static IRequirement createIURequirement(String name, VersionRange range, String filter) {
		return MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, name, range, InstallableUnit.parseFilter(filter), false, false);
	}

	public static VersionRange createStrictVersionRange(String version) {
		Version parsedVersion = Version.create(version);
		return new VersionRange(parsedVersion, true, parsedVersion, true);
	}

	public static boolean delete(File file) {
		if (!file.exists())
			return true;
		if (file.isDirectory()) {
			file.setWritable(true);
			file.setReadable(true);
			file.setExecutable(true);
			File[] children = file.listFiles();
			for (File child : children) {
				delete(child);
			}
		}
		if (!file.delete()) {
			file.setWritable(true);
			file.setReadable(true);
			file.delete();
		}
		return !file.exists();
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
		if (e != null)
			message = message + ": " + e;
		fail(message);
	}

	public static Iterator<IInstallableUnit> getInstallableUnits(IProfile profile2) {
		return profile2.query(QueryUtil.createIUAnyQuery(), null).iterator();
	}

	/**
	 * 	Get the 'self' capability for the given installable unit.
	 */
	protected static IProvidedCapability getSelfCapability(IInstallableUnit iu) {
		return getSelfCapability(iu.getId(), iu.getVersion());
	}

	/**
	 * 	Get the 'self' capability for an installable unit with the give id and version.
	 */
	protected static IProvidedCapability getSelfCapability(String installableUnitId, Version installableUnitVersion) {
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

	public static void writeBuffer(File outputFile, StringBuilder buffer) throws IOException {
		outputFile.getParentFile().mkdirs();
		try (FileOutputStream stream = new FileOutputStream(outputFile)) {
			stream.write(buffer.toString().getBytes());
		}
	}

	public static void writeProperties(File outputFile, Properties properties) throws IOException {
		outputFile.getParentFile().mkdirs();
		try (FileOutputStream stream = new FileOutputStream(outputFile)) {
			properties.store(stream, "");
		}
	}

	public static int queryResultSize(IQueryResult queryResult) {
		return queryResult.toUnmodifiableSet().size();
	}

	public static int queryResultUniqueSize(IQueryResult queryResult) {
		int cnt = 0;
		Iterator itor = queryResult.iterator();
		HashSet uniqueTracker = new HashSet();
		while (itor.hasNext()) {
			if (uniqueTracker.add(itor.next()))
				++cnt;
		}
		return cnt;
	}

	public static void restartBundle(final Bundle bundle) throws BundleException {
		bundle.stop(Bundle.STOP_TRANSIENT);
		startBundle(bundle);
	}

	public static void startBundle(final Bundle bundle) throws BundleException {
		//see http://dev.eclipse.org/mhonarc/lists/equinox-dev/msg05917.html
		bundle.start(Bundle.START_ACTIVATION_POLICY);
		bundle.start(Bundle.START_TRANSIENT);
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
			for (IStatus child : children) {
				write(child, indent + 1, output);
			}
		}
	}

	public AbstractProvisioningTest() {
		super("");
	}

	public AbstractProvisioningTest(String name) {
		super(name);
	}

	/**
	 * Creates a profile with the given name. Ensures that no such profile
	 * already exists.  The returned profile will be removed automatically
	 * in the tearDown method.
	 */
	protected IProfile createProfile(String name) {
		return createProfile(name, null);
	}

	/**
	 * Creates a profile with the given name. Ensures that no such profile
	 * already exists.  The returned profile will be removed automatically
	 * in the tearDown method.
	 */
	public IProfile createProfile(String name, Map<String, String> properties) {
		//remove any existing profile with the same name
		IProfileRegistry profileRegistry = getProfileRegistry();
		profileRegistry.removeProfile(name);
		profilesToRemove.add(name);
		//create and return a new profile
		try {
			return profileRegistry.addProfile(name, properties);
		} catch (ProvisionException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	protected IProgressMonitor getMonitor() {
		return new FussyProgressMonitor();
	}

	protected IProfile getProfile(String profileId) {
		return getProfileRegistry().getProfile(profileId);
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
	protected IMetadataRepository createTestMetdataRepository(IInstallableUnit[] units) {
		IMetadataRepository repo = new TestMetadataRepository(getAgent(), units);
		MetadataRepositoryManager repoMan = (MetadataRepositoryManager) getMetadataRepositoryManager();
		assertNotNull(repoMan);
		repoMan.addRepository(repo);
		metadataRepos.add(repo);
		return repo;
	}

	protected IArtifactRepository createArtifactRepository(URI location, Map<String, String> properties) throws ProvisionException {
		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		IArtifactRepository repo = artifactRepositoryManager.createRepository(location, "artifact", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		artifactRepositoryManager.removeRepository(repo.getLocation());
		return repo;
	}

	public static IProvisioningAgent getAgent() {
		//get the global agent for the currently running system
		return ServiceHelper.getService(TestActivator.getContext(), IProvisioningAgent.class);
	}

	protected static IProvisioningAgentProvider getAgentProvider() {
		//get the global agent for the currently running system
		return ServiceHelper.getService(TestActivator.getContext(), IProvisioningAgentProvider.class);
	}

	protected static IAgentLocation getAgentLocation() {
		//get the location of the currently running system
		return getAgent().getService(IAgentLocation.class);
	}

	protected static IArtifactRepositoryManager getArtifactRepositoryManager() {
		return getAgent().getService(IArtifactRepositoryManager.class);
	}

	protected IProfileRegistry getProfileRegistry() {
		return getAgent().getService(IProfileRegistry.class);
	}

	public Transport getTransport() {
		return getAgent().getService(Transport.class);
	}

	protected IMetadataRepository createMetadataRepository(URI location, Map<String, String> properties) throws ProvisionException {
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		IMetadataRepository repo = metadataRepositoryManager.createRepository(location, "metadata", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		metadataRepos.add(repo);
		return repo;
	}

	protected IMetadataRepository loadMetadataRepository(URI location) throws ProvisionException {
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		IMetadataRepository repo = metadataRepositoryManager.loadRepository(location, null);
		metadataRepos.add(repo);
		return repo;
	}

	protected IArtifactRepository loadArtifactRepository(URI location) throws ProvisionException {
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		IArtifactRepository repo = manager.loadRepository(location, null);
		manager.removeRepository(location);
		return repo;
	}

	protected IInstallableUnit getIU(IMetadataRepository repository, String name) {
		IQueryResult queryResult = repository.query(QueryUtil.createIUQuery(name), null);

		IInstallableUnit unit = null;
		if (!queryResult.isEmpty())
			unit = (IInstallableUnit) queryResult.iterator().next();

		return unit;
	}

	protected static IMetadataRepositoryManager getMetadataRepositoryManager() {
		return getAgent().getService(IMetadataRepositoryManager.class);
	}

	public static String getUniqueString() {
		return System.currentTimeMillis() + "-" + Math.random();
	}

	public File getTempFolder() {
		return getTestFolder(getUniqueString());
	}

	protected File getTestFolder(String name) {
		Location instanceLocation = ServiceHelper.getService(TestActivator.getContext(), Location.class, Location.INSTANCE_FILTER);
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

	@Override
	protected void runTest() throws Throwable {
		super.runTest();

		//clean up after success
		if (testFolder != null && testFolder.exists()) {
			delete(testFolder);
			testFolder = null;
		}
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		//remove all metadata repositories created by this test
		IMetadataRepositoryManager repoMan = getMetadataRepositoryManager();
		if (!metadataRepos.isEmpty()) {
			for (IMetadataRepository repo : metadataRepos) {
				repoMan.removeRepository(repo.getLocation());
			}
			metadataRepos.clear();
		}
		URI[] urls = repoMan.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (URI url : urls) {
			try {
				if (url.toString().contains("cache") || url.toString().contains("rollback")) {
					repoMan.loadRepository(url, null).removeAll();
				}
			}catch (ProvisionException e) {
				//if the repository didn't load, then it doesn't exist and we don't need to clear it up
			}
		}
		//remove all profiles created by this test
		IProfileRegistry profileRegistry = getProfileRegistry();
		for (String toRemove : profilesToRemove) {
			profileRegistry.removeProfile(toRemove);
		}
		profilesToRemove.clear();
	}

	/*
	 * Look up and return a file handle to the given entry in the bundle.
	 */
	public static File getTestData(String message, String entry) {
		if (entry == null)
			fail(message + " entry is null.");
		URL base = TestActivator.getContext().getBundle().getEntry(entry);
		if (base == null)
			fail(message + " entry not found in bundle: " + entry);
		try {
			String osPath = IPath.fromOSString(FileLocator.toFileURL(base).getPath()).toOSString();
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

	protected static void assertInstallOperand(IProvisioningPlan plan, IInstallableUnit iu) {
		if (plan.getAdditions().query(QueryUtil.createIUQuery(iu), null).isEmpty())
			fail("Can't find " + iu + " in the plan");
	}

	protected static void assertUninstallOperand(IProvisioningPlan plan, IInstallableUnit iu) {
		if (plan.getRemovals().query(QueryUtil.createIUQuery(iu), null).isEmpty())
			fail("Can't find " + iu + " in the plan");
	}

	protected static void assertNoOperand(IProvisioningPlan plan, IInstallableUnit iu) {
		if (!(plan.getRemovals().query(QueryUtil.createIUQuery(iu), null).isEmpty() && plan.getAdditions().query(QueryUtil.createIUQuery(iu), null).isEmpty()))
			fail(iu + " should not be present in this plan.");
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IMetadataRepositoryManager repoMan = getMetadataRepositoryManager();
		URI[] repos = repoMan.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (URI repo : repos) {
			repoMan.removeRepository(repo);
		}
	}

	protected IStatus installAsRoots(IProfile profile, IInstallableUnit[] ius, boolean strict, IPlanner planner, IEngine engine) {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		for (IInstallableUnit iu : ius) {
			req.add(iu);
			req.setInstallableUnitInclusionRules(iu, strict ? ProfileInclusionRules.createStrictInclusionRule(iu) : ProfileInclusionRules.createOptionalInclusionRule(iu));
			req.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
		}

		return install(req, planner, engine);
	}

	protected IStatus installAsRootsAndFlaggedAsBase(IProfile profile, IInstallableUnit[] ius, boolean strict, IPlanner planner, IEngine engine) {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		for (IInstallableUnit iu : ius) {
			req.add(iu);
			req.setInstallableUnitInclusionRules(iu, strict ? ProfileInclusionRules.createStrictInclusionRule(iu) : ProfileInclusionRules.createOptionalInclusionRule(iu));
			req.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
			req.setInstallableUnitProfileProperty(iu, "org.eclipse.equinox.p2.base", Boolean.TRUE.toString());
		}

		return install(req, planner, engine);
	}

	protected IStatus install(IProfile profile, IInstallableUnit[] ius, boolean strict, IPlanner planner, IEngine engine) {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		for (IInstallableUnit iu : ius) {
			req.add(iu);
			req.setInstallableUnitInclusionRules(iu, strict ? ProfileInclusionRules.createStrictInclusionRule(iu) : ProfileInclusionRules.createOptionalInclusionRule(iu));
		}

		return install(req, planner, engine);
	}

	protected IStatus install(IProfileChangeRequest req, IPlanner planner, IEngine engine) {
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		if (plan.getStatus().getSeverity() == IStatus.ERROR || plan.getStatus().getSeverity() == IStatus.CANCEL)
			return plan.getStatus();
		return engine.perform(plan, null);
	}

	protected IStatus uninstall(IProfile profile, IInstallableUnit[] ius, IPlanner planner, IEngine engine) {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.removeInstallableUnits(ius);

		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		return engine.perform(plan, null);
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
		for (Object expectedelement : expected) {
			for (int j = 0; j < expected.length; j++) {
				if (!found[j] && expectedelement.equals(actual[j])) {
					found[j] = true;
				}
			}
		}
		for (int i = 0; i < found.length; i++)
			if (!found[i])
				assertTrue(message + ".3." + i, false);
	}

	/**
	 * Asserts that the first line of text in f equals the content.
	 * @param f
	 * @param content
	 */
	public static void assertFileContent(String message, File f, String content) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
			String line = reader.readLine();
			assertEquals(message, content, line);
		} catch (FileNotFoundException e) {
			fail("Getting copy target", e);
		} catch (IOException e) {
			fail("reading copy target", e);
		}
	}

	protected IProvisioningEventBus getEventBus() {
		IProvisioningEventBus bus = getAgent().getService(IProvisioningEventBus.class);
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

		if (QueryUtil.isFragment(iu1)) {
			if (!QueryUtil.isFragment(iu2))
				fail(message + " " + iu1 + " is not a fragment.");
			try {
				assertEquals(message, ((IInstallableUnitFragment) iu1).getHost(), ((IInstallableUnitFragment) iu2).getHost());
			} catch (AssertionFailedError failure) {
				fail(message + " Unequal hosts: " + failure.getMessage());
			}
		} else if (QueryUtil.isFragment(iu2)) {
			fail(message + " " + iu2 + " is a fragment.");
		}

		if (iu1.isSingleton()) {
			if (!iu2.isSingleton())
				fail(message + " " + iu2 + " is not a singleton.");
		} else if (iu2.isSingleton()) {
			fail(message + " " + iu2 + " is a singleton.");
		}

		assertEquals(message, iu1.getProvidedCapabilities(), iu2.getProvidedCapabilities());
		assertEquals(message, iu1.getRequirements(), iu2.getRequirements());
		assertEquals(message, iu1.getArtifacts(), iu2.getArtifacts());
		assertEquals(message, iu1.getTouchpointType(), iu2.getTouchpointType());
		assertEquals(message, iu1.getTouchpointData(), iu2.getTouchpointData());
		assertEquals(message, iu1.getProperties(), iu2.getProperties());
		assertEquals(message, iu1.getLicenses(), iu2.getLicenses());
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
		for (IInstallableUnitFragment fragments2element : fragments2) {
			map.put(fragments2element, fragments2element);
		}
		for (IInstallableUnitFragment fragments1element : fragments1) {
			if (!map.containsKey(fragments1)) {
				fail(message + " Expected fragment '" + fragments1element + "' not present.");
			} else {
				assertEquals(message, fragments1element, map.remove(fragments1element));
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
			assertEquals(message, desc1.getIUsBeingUpdated(), desc2.getIUsBeingUpdated());
			assertEquals(message, desc1.getSeverity(), desc2.getSeverity());
			assertEquals(message, desc1.getDescription(), desc2.getDescription());

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
		TreeSet set = new TreeSet((o1, o2) -> o1.toString().compareTo(o2.toString()));
		set.addAll(Arrays.asList(ius2));

		for (IInstallableUnit ius1unit : ius1) {
			// + "\0" is a successor for strings
			SortedSet subset = set.subSet(ius1unit, ius1unit.toString() + "\0");
			if (subset.size() == 1) {
				IInstallableUnit candidate = (IInstallableUnit) subset.first();
				try {
					assertEquals(message, ius1unit, candidate);
				} catch (AssertionFailedError e) {
					fail(message + " IUs '" + ius1unit + "' are unequal : " + e.getMessage());
				}
				subset.remove(candidate);
			} else if (subset.size() > 1) {
				//should not happen
				fail(message + " ERROR: Unexpected failure.");
			} else {
				fail(message + " Expected IU " + ius1unit + " not found.");
			}
		}
		if (set.size() > 0)
			fail(message + " Unexpected IU " + set.first() + ".");
	}

	/*
	 * Compare 2 copyright objects and fail if they are not considered equal.
	 */
	protected static void assertEquals(String message, ICopyright cpyrt1, ICopyright cpyrt2) {
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
		for (IArtifactKey key : sourceRepo.query(ArtifactKeyQuery.ALL_KEYS, null)) {
			IArtifactDescriptor[] destinationDescriptors = destinationRepo.getArtifactDescriptors(key);
			if (destinationDescriptors == null || destinationDescriptors.length == 0)
				fail(message + ": unmatched key: " + key.toString());
			//this implicitly verifies the keys are present

			IArtifactDescriptor[] sourceDescriptors = sourceRepo.getArtifactDescriptors(key);

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
		IQueryResult<IInstallableUnit> sourceCollector = sourceRepo.query(QueryUtil.createIUAnyQuery(), null);
		Iterator<IInstallableUnit> it = sourceCollector.iterator();

		while (it.hasNext()) {
			IInstallableUnit sourceIU = it.next();
			IQueryResult<IInstallableUnit> destinationCollector = destinationRepo.query(QueryUtil.createIUQuery(sourceIU), null);
			assertEquals(message, 1, queryResultSize(destinationCollector));
			assertEquals(message, sourceIU, destinationCollector.iterator().next());
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

	public static void assertContains(String message, IQueryable source, IQueryable destination) {
		IQueryResult<IInstallableUnit> sourceCollector = source.query(QueryUtil.createIUAnyQuery(), null);
		Iterator<IInstallableUnit> it = sourceCollector.iterator();

		while (it.hasNext()) {
			IInstallableUnit sourceIU = it.next();
			IQueryResult destinationCollector = destination.query(QueryUtil.createIUQuery(sourceIU), null);
			assertEquals(message, 1, queryResultSize(destinationCollector));
			assertEquals(message, sourceIU, destinationCollector.iterator().next());
		}
	}

	public static void assertContains(String message, IQueryResult result, IQueryResult mustHave) {
		assertContains(message, result.iterator(), mustHave.iterator());
	}

	public static void assertContains(String message, Iterator result, Iterator mustHave) {
		HashSet repoSet = new HashSet();
		while (mustHave.hasNext())
			repoSet.add(mustHave.next());
		assertContains(message, result, repoSet);
	}

	public static void assertContains(String message, Iterator result, Collection mustHave) {
		while (result.hasNext())
			assertTrue(message, mustHave.contains(result.next()));
	}

	public static void assertContains(IQueryResult result, Object value) {
		assertContains(null, result, value);
	}

	public static void assertNotContains(IQueryResult result, Object value) {
		assertNotContains(null, result, value);
	}

	//	public void assertContains(IQueryable<IInstallableUnit> queryable, IInstallableUnit iu) {
	//		assertFalse("Missing IU " + iu.toString(), queryable.query(QueryUtil.ALL_UNITS, null).isEmpty());
	//	}

	public static void assertContains(String message, IQueryResult result, Object value) {
		Iterator itor = result.iterator();
		while (itor.hasNext())
			if (itor.next().equals(value))
				return;
		fail(message);
	}

	public static void assertNotContains(String message, IQueryResult result, Object value) {
		Iterator itor = result.iterator();
		while (itor.hasNext())
			if (itor.next().equals(value))
				fail(message);
	}

	public static void assertContains(String message, Collection fromIUs, Iterator fromRepo) {
		assertContains(message, fromIUs.iterator(), fromRepo);
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
		for (Object expectedelement : expected.keySet()) {
			assertTrue(message, actual.containsKey(expectedelement)); //Ensure the key exists
			if (!expectedelement.equals("p2.timestamp")) {
				assertEquals(message, expected.get(expectedelement), actual.get(expectedelement));
			}
		}
	}

	/*
	 * Search for partial matches in the log file. Assert that all of the specified strings
	 * are found on a single line in the log file.
	 */
	public static void assertLogContainsLine(File log, String[] parts) throws IOException {
		assertNotNull(log);
		assertTrue(log.exists());
		assertTrue(log.length() > 0);
		assertNotNull(parts);
		try (BufferedReader reader = new BufferedReader(new FileReader(log))) {
			while (reader.ready()) {
				String line = reader.readLine();
				boolean found = true;
				for (String part : parts) {
					found = found && line.contains(part);
				}
				if (found)
					return;
			}
		}
		assertTrue(false);
	}

	/**
	 * Assert that the given log file contains the given lines
	 * Lines are expected to appear in order
	 */
	public static void assertLogContainsLines(File log, String... lines) throws Exception {
		assertNotNull(log);
		assertTrue(log.exists());
		assertTrue(log.length() > 0);

		int idx = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(log))) {
			while (reader.ready()) {
				String line = reader.readLine();
				if (line.contains(lines[idx])) {
					if (++idx >= lines.length) {
						return;
					}
				}
			}
		}
		fail(String.format("Log file %s doesn't contain lines %s", log.getCanonicalPath(), Arrays.toString(lines)));
	}

	/**
	 * Assert that the given log file doesn't contain the given message
	 * The message is expected to be contained on a single line
	 */
	public static void assertLogDoesNotContainLine(File log, String msg) throws Exception {
		assertLogDoesNotContainLines(log, new String[] {msg});
	}

	/**
	 * Assert that the given log file does not contain the given lines
	 * Lines are expected to appear in order
	 */
	public static void assertLogDoesNotContainLines(File log, String[] lines) throws Exception {
		assertNotNull(log);
		assertTrue(log.exists());
		assertTrue(log.length() > 0);

		int idx = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(log))) {
			while (reader.ready()) {
				String line = reader.readLine();
				if (line.contains(lines[idx])) {
					if (++idx >= lines.length) {
						fail(String.format("Log file %s contains lines %s", log.getCanonicalPath(), Arrays.toString(lines)));
					}
				}
			}
		}
	}

	public void clearProfileMap(SimpleProfileRegistry profileRegistry) {
		try {
			Field profilesMap = SimpleProfileRegistry.class.getDeclaredField("profiles");
			profilesMap.setAccessible(true);
			profilesMap.set(profileRegistry, null);
		} catch (Throwable t) {
			fail();
		}
	}

	protected int getArtifactKeyCount(URI location) {
		try {
			return getArtifactKeyCount(getArtifactRepositoryManager().loadRepository(location, null));
		} catch (ProvisionException e) {
			fail("Failed to load repository " + URIUtil.toUnencodedString(location) + " for ArtifactDescriptor count");
			return -1;
		}
	}

	protected int getArtifactKeyCount(IArtifactRepository repo) {
		return queryResultSize(repo.query(ArtifactKeyQuery.ALL_KEYS, null));
	}

	protected int getArtifactDescriptorCount(URI location) {
		int count = 0;
		try {
			IArtifactRepository repo = getArtifactRepositoryManager().loadRepository(location, null);
			IQueryResult<IArtifactDescriptor> descriptors = repo.descriptorQueryable().query(ArtifactDescriptorQuery.ALL_DESCRIPTORS, null);
			return queryResultSize(descriptors);
		} catch (ProvisionException e) {
			fail("Failed to load repository " + URIUtil.toUnencodedString(location) + " for ArtifactDescriptor count");
		}
		return count;
	}

	public int countPlanElements(IProvisioningPlan plan) {
		return queryResultSize(QueryUtil.compoundQueryable(plan.getAdditions(), plan.getRemovals()).query(QueryUtil.createIUAnyQuery(), null));
	}

	/**
	 * This method is used by tests that require access to the "self" profile. It spoofs
	 * up a fake self profile is none is already available. Tests should invoke this method
	 * from their {@link #setUp()} method, and invoke {@link #tearDownSelfProfile()}
	 * from their {@link #tearDown()} method.
	 */
	protected void setUpSelfProfile() {
		if (System.getProperty("eclipse.p2.profile") == null) {
			SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) getProfileRegistry();
			try {
				Field selfField = SimpleProfileRegistry.class.getDeclaredField("self"); //$NON-NLS-1$
				selfField.setAccessible(true);
				previousSelfValue = selfField.get(profileRegistry);
				if (previousSelfValue == null)
					selfField.set(profileRegistry, "agent");
			} catch (Throwable t) {
				fail();
			}
		}
		createProfile("agent");
	}

	/**
	 * This method is used by tests that require access to the "self" profile. It cleans up
	 * a fake self profile is none is already available. Tests should invoke this method
	 * from their {@link #tearDown()} method, and invoke {@link #setUpSelfProfile()}
	 * from their {@link #setUp()} method.
	 */
	protected void tearDownSelfProfile() {
		if (System.getProperty("eclipse.p2.profile") == null) {
			SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) getProfileRegistry();
			try {
				Field selfField = SimpleProfileRegistry.class.getDeclaredField("self"); //$NON-NLS-1$
				selfField.setAccessible(true);
				Object self = selfField.get(profileRegistry);
				if (self.equals("agent"))
					selfField.set(profileRegistry, previousSelfValue);
			} catch (Throwable t) {
				// ignore as we still want to continue tidying up
			}
		}
	}

	public IEngine getEngine() {
		return getAgent().getService(IEngine.class);
	}

	public IPlanner getPlanner(IProvisioningAgent agent) {
		return agent.getService(IPlanner.class);
	}

	public void assertNoContents(File file, String[] lines) {
		if (!file.exists())
			fail("File: " + file.toString() + " can't be found.");
		int idx = 0;
		try {
			try (BufferedReader reader = new BufferedReader(new FileReader(file));) {
				while (reader.ready()) {
					String line = reader.readLine();
					if (line.indexOf(lines[idx]) > 0) {
						fail("String: " + lines[idx] + " should not be in " + file.getAbsolutePath());
					}
				}
			}
		} catch (FileNotFoundException e) {
			//ignore, caught before
		} catch (IOException e) {
			fail("Exception while reading: " + file.getAbsolutePath());
		}
	}

	public void assertContents(File file, String[] lines) {
		if (!file.exists())
			fail("File: " + file.toString() + " can't be found.");
		int idx = 0;
		try {
			try (BufferedReader reader = new BufferedReader(new FileReader(file));) {
				while (reader.ready()) {
					String line = reader.readLine();
					if (line.contains(lines[idx])) {
						if (++idx >= lines.length)
							return;
					}
				}
			}
		} catch (FileNotFoundException e) {
			//ignore, caught before
		} catch (IOException e) {
			fail("String: " + lines[idx] + " not found in " + file.getAbsolutePath());
		}
		fail("String:" + lines[idx] + " not found");
	}

	protected void assertEqualArtifacts(String message, SimpleArtifactRepository expected, SimpleArtifactRepository actual) {
		for (IArtifactKey key : expected.query(ArtifactKeyQuery.ALL_KEYS, null)) {
			IArtifactDescriptor[] expectedDescriptors = expected.getArtifactDescriptors(key);
			IArtifactDescriptor[] actualDescriptors = actual.getArtifactDescriptors(key);

			if (expectedDescriptors == null || actualDescriptors == null)
				if (!(expectedDescriptors == null && actualDescriptors == null))
					fail(message + " missing key " + key);

			top: for (IArtifactDescriptor expectedDescriptor : expectedDescriptors) {
				for (IArtifactDescriptor actualDescriptor : actualDescriptors) {
					if (Arrays.equals(expectedDescriptor.getProcessingSteps(), actualDescriptor.getProcessingSteps())) {
						File expectedFile = expected.getArtifactFile(expectedDescriptor);
						File actualFile = actual.getArtifactFile(actualDescriptor);
						if (expectedFile == null || actualFile == null)
							fail(message + " descriptor mismatch");
						if (!(expectedFile.exists() && actualFile.exists()))
							fail(message + " file does not exist");
						if ("jar".equals(IPath.fromOSString(expectedFile.getName()).getFileExtension())) {
							//compare jar contents
							assertEqualJars(expectedFile, actualFile);
						} else {
							//otherwise just compare file size
							assertEquals("Different file: " + expectedFile.getName(), expectedFile.length(), actualFile.length());
						}
						continue top;
					}
				}
				fail(message + "Missing expected descriptor" + expectedDescriptor);
			}
		}
	}

	protected void assertEqualJars(File expectedFile, File actualFile) {
		try (JarFile expectedJar = new JarFile(expectedFile); JarFile actualJar = new JarFile(actualFile)) {
			int expectedEntryCount = 0, actualEntryCount = 0;
			for (Enumeration<JarEntry> en = expectedJar.entries(); en.hasMoreElements();) {
				expectedEntryCount++;
				JarEntry expectedEntry = en.nextElement();
				JarEntry actualEntry = actualJar.getJarEntry(expectedEntry.getName());
				assertNotNull(actualEntry);
				assertEquals("Unmatched entry size: " + expectedEntry.getName(), expectedEntry.getSize(), actualEntry.getSize());
			}
			for (Enumeration<JarEntry> en = expectedJar.entries(); en.hasMoreElements();) {
				actualEntryCount++;
				en.nextElement();
			}
			assertEquals("Unexpected difference in entries for " + expectedFile.getName(), expectedEntryCount, actualEntryCount);
		} catch (IOException e) {
			fail("Unexpected error comparing jars", e);
		}
	}

	public void assertResolve(IProfileChangeRequest request, IPlanner planner) {
		IProvisioningPlan plan = planner.getProvisioningPlan(request, null, null);
		assertOK(plan.getStatus());
	}

	/**
	 * Exposes {@link AbstractProvisioningTest}'s helper methods as JUnit {@link TestRule}.
	 * To be used by JUnit4 tests which must not extend {@link TestCase}.
	 */
	public static final class ProvisioningTestRuleAdapter extends AbstractProvisioningTest implements TestRule {

		@Override
		public Statement apply(final Statement base, Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					setUp();
					try {
						base.evaluate();
					} finally {
						tearDown();
					}
				}
			};
		}
	}

}
