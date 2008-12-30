/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.resolution.ResolutionHelper;
import org.eclipse.equinox.internal.p2.resolution.UnsatisfiedCapability;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests for {@link ResolutionHelper}.
 */
public class ResolutionHelperTest extends AbstractProvisioningTest {
	private static final String FILTER_KEY = "osgi.os";

	public static Test suite() {
		return new TestSuite(ResolutionHelperTest.class);
	}

	public ResolutionHelperTest() {
		super("");
	}

	public ResolutionHelperTest(String name) {
		super(name);
	}

	/**
	 * Tests resolving an IU that requires a capability, and the available
	 * provided capability is above the required capability's version range.
	 */
	public void testDependencyAboveVersionRange() {
		Version version = new Version(5, 0, 0);

		//The IU that exports the capability
		IProvidedCapability[] provides = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("test.capability", "test", version)};
		IInstallableUnit required = createIU("required", version, provides);

		//an IU whose required capability falls outside available range
		IRequiredCapability[] requires = createRequiredCapabilities("test.capability", "test", new VersionRange("[2.0,5.0)"), null);
		IInstallableUnit toInstall = createIU("match", version, requires);

		ResolutionHelper rh = new ResolutionHelper(null, null);
		HashSet installSet = new HashSet();
		installSet.add(toInstall);
		HashSet available = new HashSet();
		available.add(required);
		UnsatisfiedCapability[] unsatisfied = rh.install(installSet, available);

		assertEquals("1.0", 1, unsatisfied.length);
		assertEquals("1.1", "match", unsatisfied[0].getUnsatisfiedUnit().getId());
		IRequiredCapability capability = unsatisfied[0].getRequiredCapability();
		assertEquals("1.4", "test.capability", capability.getNamespace());
		assertEquals("1.5", "test", capability.getName());
	}

	/**
	 * Tests resolving an IU that requires a capability, and the available
	 * provided capability is below the required capability's version range.
	 */
	public void testDependencyBelowVersionRange() {
		Version version = new Version(2, 0, 0);

		//The IU that exports the capability
		IProvidedCapability[] provides = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("test.capability", "test", version)};
		IInstallableUnit required = createIU("required", version, provides);

		//an IU whose required capability falls outside available range
		IRequiredCapability[] requires = createRequiredCapabilities("test.capability", "test", new VersionRange("(2.0,3.0)"), null);
		IInstallableUnit toInstall = createIU("match", version, requires);

		ResolutionHelper rh = new ResolutionHelper(null, null);
		HashSet installSet = new HashSet();
		installSet.add(toInstall);
		HashSet available = new HashSet();
		available.add(required);
		UnsatisfiedCapability[] unsatisfied = rh.install(installSet, available);

		assertEquals("1.0", 1, unsatisfied.length);
		assertEquals("1.1", "match", unsatisfied[0].getUnsatisfiedUnit().getId());
		IRequiredCapability capability = unsatisfied[0].getRequiredCapability();
		assertEquals("1.4", "test.capability", capability.getNamespace());
		assertEquals("1.5", "test", capability.getName());
	}

	public void testDependencyWithPlatformFilter() {
		Version version = new Version(1, 0, 0);

		//The IU that exports the capability
		IProvidedCapability[] provides = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("test.capability", "test", version)};
		IInstallableUnit required = createIU("required", version, createFilter(FILTER_KEY, "win32"), provides);

		IRequiredCapability[] requires = createRequiredCapabilities("test.capability", "test", ANY_VERSION, null);
		IInstallableUnit toInstall = createIU("toInstall", version, requires);

		//setup context so that platform filter will satisfy dependency
		Hashtable context = new Hashtable();
		context.put(FILTER_KEY, "win32");
		ResolutionHelper rh = new ResolutionHelper(context, null);
		HashSet installSet = new HashSet();
		installSet.add(toInstall);
		HashSet available = new HashSet();
		available.add(required);
		UnsatisfiedCapability[] unsatisfied = rh.install(installSet, available);
		assertEquals("1.0", 0, unsatisfied.length);

		//now try with a null evaluation context
		rh = new ResolutionHelper(null, null);
		unsatisfied = rh.install(installSet, available);
		assertEquals("1.1", 1, unsatisfied.length);
		assertEquals("1.2", "toInstall", unsatisfied[0].getUnsatisfiedUnit().getId());
		IRequiredCapability capability = unsatisfied[0].getRequiredCapability();
		assertEquals("1.3", "test.capability", capability.getNamespace());
		assertEquals("1.4", "test", capability.getName());

		//now use a context where platform filter will not be satisfied
		context.put(FILTER_KEY, "nomatch");
		rh = new ResolutionHelper(context, null);
		unsatisfied = rh.install(installSet, available);
		assertEquals("2.1", 1, unsatisfied.length);
		assertEquals("2.2", "toInstall", unsatisfied[0].getUnsatisfiedUnit().getId());
		capability = unsatisfied[0].getRequiredCapability();
		assertEquals("2.3", "test.capability", capability.getNamespace());
		assertEquals("2.4", "test", capability.getName());

	}

	/**
	 * Tests resolving an IU that has a filter on its required capability.
	 */
	public void testSatisfiedDependencyWithMatchingFilter() {
		//use the same version everywhere because it's not interesting for this test
		Version version = new Version(1, 0, 0);

		//The IU that exports the capability
		IProvidedCapability[] provides = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("test.capability", "test", version)};
		IInstallableUnit required = createIU("required", version, provides);

		//an IU whose filter will match the environment
		IRequiredCapability[] requires = createRequiredCapabilities("test.capability", "test", createFilter(FILTER_KEY, "matchValue"));
		IInstallableUnit toInstall = createIU("match", version, requires);

		Dictionary environment = new Hashtable();
		environment.put(FILTER_KEY, "matchValue");
		ResolutionHelper rh = new ResolutionHelper(environment, null);
		HashSet installSet = new HashSet();
		installSet.add(toInstall);
		HashSet available = new HashSet();
		available.add(required);
		UnsatisfiedCapability[] unsatisfied = rh.install(installSet, available);

		assertEquals("1.0", 0, unsatisfied.length);
	}

	/**
	 * In this test we try to resolve an IU that has a required capability that is
	 * available, but there is a filter on the required capability so it should not be considered.
	 */
	public void testSatisfiedDependencyWithUnmatchingFilter() {
		//use the same version everywhere because it's not interesting for this test
		Version version = new Version(1, 0, 0);

		//The IU that exports the capability
		IProvidedCapability[] provides = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("test.capability", "test", version)};
		IInstallableUnit required = createIU("required", version, provides);

		//an IU whose filter will not match the environment
		IRequiredCapability[] requires = createRequiredCapabilities("test.capability", "test", createFilter(FILTER_KEY, "noMatchValue"));
		IInstallableUnit toInstall = createIU("noMatch", version, requires);

		Dictionary environment = new Hashtable();
		environment.put(FILTER_KEY, "matchValue");
		ResolutionHelper rh = new ResolutionHelper(environment, null);
		HashSet installSet = new HashSet();
		installSet.add(toInstall);
		HashSet available = new HashSet();
		available.add(required);
		UnsatisfiedCapability[] unsatisfied = rh.install(installSet, available);

		assertEquals("1.0", 0, unsatisfied.length);
	}

	public void testSimpleDependency() {
		IRequiredCapability[] requires = new IRequiredCapability[] {MetadataFactory.createRequiredCapability("java.runtime", "JRE", null, null, false, false)};
		IInstallableUnit osgi = createIU("org.eclipse.osgi", new Version(3, 2, 0, null), requires, NO_PROPERTIES, false);

		IProvidedCapability[] provides = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("java.runtime", "JRE", new Version(1, 4, 2, "sr2"))};
		IInstallableUnit jre = createIU("com.ibm.jre", new Version(1, 4, 2, "sr2"), provides);

		ResolutionHelper rh = new ResolutionHelper(null, null);
		HashSet osgiSet = new HashSet(1);
		osgiSet.add(osgi);
		HashSet jreSet = new HashSet(1);
		jreSet.add(jre);
		assertEquals("1.0", 0, rh.install(osgiSet, jreSet).length);
	}

	/**
	 * Tests resolving an IU that has a filter on its required capability, and
	 * the required capability is not available.
	 */
	public void testUnsatisfiedDependencyWithMatchingFilter() {
		//use the same version everywhere because it's not interesting for this test
		Version version = new Version(1, 0, 0);

		//The IU that exports the capability
		IProvidedCapability[] provides = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("test.capability", "test", version)};
		IInstallableUnit required = createIU("required", version, provides);

		//an IU whose filter will match the environment
		IRequiredCapability[] requires = createRequiredCapabilities("test.capability", "does.not.exist", createFilter(FILTER_KEY, "matchValue"));
		IInstallableUnit toInstall = createIU("match", version, requires);

		Dictionary environment = new Hashtable();
		environment.put(FILTER_KEY, "matchValue");
		ResolutionHelper rh = new ResolutionHelper(environment, null);
		HashSet installSet = new HashSet();
		installSet.add(toInstall);
		HashSet available = new HashSet();
		available.add(required);
		UnsatisfiedCapability[] unsatisfied = rh.install(installSet, available);

		assertEquals("1.0", 1, unsatisfied.length);
		assertEquals("1.1", "match", unsatisfied[0].getUnsatisfiedUnit().getId());
		IRequiredCapability capability = unsatisfied[0].getRequiredCapability();
		assertEquals("1.4", "test.capability", capability.getNamespace());
		assertEquals("1.5", "does.not.exist", capability.getName());
	}

	/**
	 * In this test we try to resolve an IU that has an unsatisfied dependency.
	 * However, there is a filter on the unresolved dependency that does not
	 * match the environment, so it should not prevent the IU being resolved.
	 */
	public void testUnsatisfiedDependencyWithUnmatchingFilter() {
		//use the same version everywhere because it's not interesting for this test
		Version version = new Version(1, 0, 0);

		//The IU that exports the capability
		IProvidedCapability[] provides = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("test.capability", "test", version)};
		IInstallableUnit required = createIU("required", version, provides);

		//an IU whose filter will not match the environment
		IRequiredCapability[] requires = createRequiredCapabilities("test.capability", "does.not.exist", createFilter(FILTER_KEY, "noMatchValue"));
		IInstallableUnit toInstall = createIU("noMatch", version, requires);

		Dictionary environment = new Hashtable();
		environment.put(FILTER_KEY, "matchValue");
		ResolutionHelper rh = new ResolutionHelper(environment, null);
		HashSet installSet = new HashSet();
		installSet.add(toInstall);
		HashSet available = new HashSet();
		available.add(required);
		UnsatisfiedCapability[] unsatisfied = rh.install(installSet, available);

		assertEquals("1.0", 0, unsatisfied.length);
	}
}
