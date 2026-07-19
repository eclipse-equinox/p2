/*******************************************************************************
 * Copyright (c) 2026 Eclipse contributors and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.io.File;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredPropertiesMatch;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

/**
 * Tests that {@code Require-Capability} requirements published by
 * {@link BundlesAction} carry a namespace-specific environment filter that is
 * active by default and can be disabled by setting the profile property
 * {@code org.eclipse.equinox.p2.disable.require.capability.<namespace>} to
 * {@code "true"}.
 */
public class BundlesActionRequireCapabilityFilterTest extends AbstractProvisioningTest {

	private static final String TEST_BUNDLE = "testData/BundlesActionRequireCapabilityFilterTest/testRC";

	private final IInstallableUnit bundleIU;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		File testData = getTestData("testRC", TEST_BUNDLE);
		bundleIU = BundlesAction.createBundleIU(BundlesAction.createBundleDescription(testData), null,
				new PublisherInfo());
	}

	/**
	 * A {@code Require-Capability} requirement must carry an env filter whose
	 * property name is the namespace-specific property.
	 */
	public void testRequireCapabilityHasEnvFilter() {
		IRequirement req = findRequiredPropertiesMatchByNamespace(bundleIU.getRequirements(), "my.cap");
		assertNotNull("my.cap Require-Capability requirement missing", req);

		IMatchExpression<IInstallableUnit> filter = req.getFilter();
		assertNotNull("Require-Capability requirement should have an env filter", filter);
		// filter.toString() renders the parameter as "$0"; the LDAP filter string is in getParameters()[0]
		assertTrue("Env filter should contain the namespace-specific property name",
				filter.getParameters()[0].toString()
						.contains(BundlesAction.getFilterPropertyForNamespace("my.cap")));
	}

	/**
	 * The env filter must evaluate to {@code true} when the disable property is
	 * absent, so that the requirement is enforced by default.
	 */
	public void testRequireCapabilityFilterActiveByDefault() {
		IRequirement req = findRequiredPropertiesMatchByNamespace(bundleIU.getRequirements(), "my.cap");
		assertNotNull("my.cap Require-Capability requirement missing", req);

		IInstallableUnit profileIU = createProfileIU();
		assertTrue("Require-Capability requirement should be active when property is absent",
				req.getFilter().isMatch(profileIU));
	}

	/**
	 * Setting the namespace-specific property to {@code "true"} must disable only
	 * requirements in that namespace and leave other namespaces active.
	 */
	public void testRequireCapabilityFilterDisabledByNamespaceProperty() {
		IRequirement myCapReq = findRequiredPropertiesMatchByNamespace(bundleIU.getRequirements(), "my.cap");
		IRequirement osgiEeReq = findRequiredPropertiesMatchByNamespace(bundleIU.getRequirements(), "osgi.ee");
		assertNotNull("my.cap Require-Capability requirement missing", myCapReq);
		assertNotNull("osgi.ee requirement missing", osgiEeReq);

		IInstallableUnit profileIU = createProfileIU("my.cap");
		assertFalse("my.cap requirement should be disabled when its property is set to true",
				myCapReq.getFilter().isMatch(profileIU));
		assertTrue("osgi.ee requirement must remain active when only my.cap property is set",
				osgiEeReq.getFilter().isMatch(profileIU));
	}

	/**
	 * Setting the old global property (without namespace suffix) must NOT disable
	 * any requirement, since the filter is now namespace-specific.
	 */
	public void testOldGlobalPropertyHasNoEffect() {
		IRequirement myCapReq = findRequiredPropertiesMatchByNamespace(bundleIU.getRequirements(), "my.cap");
		IRequirement osgiEeReq = findRequiredPropertiesMatchByNamespace(bundleIU.getRequirements(), "osgi.ee");
		assertNotNull("my.cap Require-Capability requirement missing", myCapReq);
		assertNotNull("osgi.ee requirement missing", osgiEeReq);

		InstallableUnitDescription desc = new InstallableUnitDescription();
		desc.setId("test.profile.global");
		desc.setVersion(Version.create("1.0.0"));
		// Set only the unsuffixed (old) global property — this should have no effect
		desc.setProperty(BundlesAction.FILTER_PROPERTY_DISABLE_REQUIRE_CAPABILITY, "true");
		IInstallableUnit profileIU = MetadataFactory.createInstallableUnit(desc);

		assertTrue("my.cap requirement must remain active when only global property is set (no namespace suffix)",
				myCapReq.getFilter().isMatch(profileIU));
		assertTrue("osgi.ee requirement must remain active when only global property is set (no namespace suffix)",
				osgiEeReq.getFilter().isMatch(profileIU));
	}

	/**
	 * Requirements synthesised from {@code Bundle-RequiredExecutionEnvironment}
	 * are emitted as {@code osgi.ee} {@code Require-Capability} entries and must
	 * carry the namespace-specific env filter.
	 */
	public void testBREERequirementHasEnvFilter() {
		IRequirement req = findRequiredPropertiesMatchByNamespace(bundleIU.getRequirements(), "osgi.ee");
		assertNotNull("osgi.ee requirement (from BREE) missing", req);

		IMatchExpression<IInstallableUnit> filter = req.getFilter();
		assertNotNull("osgi.ee requirement should have an env filter", filter);
		// filter.toString() renders the parameter as "$0"; the LDAP filter string is in getParameters()[0]
		assertTrue("Env filter of osgi.ee requirement should contain the namespace-specific property name",
				filter.getParameters()[0].toString()
						.contains(BundlesAction.getFilterPropertyForNamespace("osgi.ee")));
	}

	/**
	 * Setting the {@code osgi.ee}-specific property to {@code "true"} must disable
	 * the BREE-derived {@code osgi.ee} requirement.
	 */
	public void testBREERequirementFilterDisabledByNamespaceProperty() {
		IRequirement req = findRequiredPropertiesMatchByNamespace(bundleIU.getRequirements(), "osgi.ee");
		assertNotNull("osgi.ee requirement (from BREE) missing", req);

		IInstallableUnit profileIU = createProfileIU("osgi.ee");
		assertFalse("osgi.ee requirement should be disabled when its property is set to true",
				req.getFilter().isMatch(profileIU));
	}

	/**
	 * {@code Import-Package} requirements must NOT carry the env filter because
	 * they are not emitted via the {@code Require-Capability} code path.
	 */
	public void testImportPackageHasNoEnvFilter() {
		IRequiredCapability req = findRequiredCapabilityByNamespace(bundleIU.getRequirements(),
				PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE);
		assertNotNull("java.package (Import-Package) requirement missing", req);
		assertNull("Import-Package requirement must not have an env filter", req.getFilter());
	}

	/**
	 * {@code Require-Bundle} requirements must NOT carry the env filter because
	 * they are not emitted via the {@code Require-Capability} code path.
	 */
	public void testRequireBundleHasNoEnvFilter() {
		IRequiredCapability req = findRequiredCapabilityByNamespace(bundleIU.getRequirements(),
				BundlesAction.CAPABILITY_NS_OSGI_BUNDLE);
		assertNotNull("osgi.bundle (Require-Bundle) requirement missing", req);
		assertNull("Require-Bundle requirement must not have an env filter", req.getFilter());
	}

	// --- helpers ---

	/**
	 * Creates a profile IU with the given namespaces disabled. At least one
	 * property is always set so that {@code InstallableUnit.getMember("properties")}
	 * returns a non-null map (required for {@code IMatchExpression.isMatch()} to
	 * work correctly).
	 */
	private static IInstallableUnit createProfileIU(String... disabledNamespaces) {
		InstallableUnitDescription desc = new InstallableUnitDescription();
		desc.setId("test.profile");
		desc.setVersion(Version.create("1.0.0"));
		// Always set a dummy property to initialize the properties map; without it
		// InstallableUnit.getMember("properties") returns null and filter.isMatch()
		// short-circuits to false regardless of the filter expression.
		desc.setProperty("p2.test.profile", "active");
		for (String namespace : disabledNamespaces) {
			desc.setProperty(BundlesAction.getFilterPropertyForNamespace(namespace), "true");
		}
		return MetadataFactory.createInstallableUnit(desc);
	}

	private static IRequirement findRequiredPropertiesMatchByNamespace(Collection<IRequirement> requirements,
			String namespace) {
		for (IRequirement req : requirements) {
			if (req instanceof RequiredPropertiesMatch rpm
					&& namespace.equals(RequiredPropertiesMatch.extractNamespace(rpm.getMatches()))) {
				return req;
			}
		}
		return null;
	}

	private static IRequiredCapability findRequiredCapabilityByNamespace(Collection<IRequirement> requirements,
			String namespace) {
		for (IRequirement req : requirements) {
			if (req instanceof IRequiredCapability rc && namespace.equals(rc.getNamespace())) {
				return rc;
			}
		}
		return null;
	}

}
