/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.service.resolver.ResolutionException;

/**
 * Reproduces a scenario where several consumer bundles import ASM packages
 * with different version ranges. "test.bundle.a" imports
 * <code>org.objectweb.asm</code>, <code>org.objectweb.asm.commons</code> and
 * <code>org.objectweb.asm.util</code> with a wide range, "test.bundle.b"
 * imports only <code>org.objectweb.asm.commons</code> with a narrow range
 * that matches only the older ASM release, and "test.bundle.c" imports only
 * <code>org.objectweb.asm.util</code> with a wide open-ended range (no upper
 * bound).
 * <p>
 * Rather than reaching out to the real Orbit update sites over the network,
 * this test uses local, synthetic IUs that mirror the exact
 * <code>java.package</code> capability/requirement shape published by the
 * real ASM bundles in the Orbit aggregation repositories for 4.40.0 (ASM
 * 9.9.1) and 4.41.0 (ASM 9.10.1) - see
 * https://download.eclipse.org/tools/orbit/simrel/orbit-aggregation/release/4.40.0
 * and .../4.41.0. In particular, each ASM release bundle requires its sibling
 * bundles (e.g. <code>org.objectweb.asm.commons</code> requires
 * <code>org.objectweb.asm</code> and <code>org.objectweb.asm.tree</code>)
 * with a narrow, auto-generated version range of the shape
 * <code>[version,nextMinor.0)</code>, which only matches bundles from the
 * very same ASM release.
 * <p>
 * The test first installs "test.bundle.a" and "test.bundle.b" against a
 * repository that only contains ASM 9.9.1, then installs "test.bundle.c"
 * while making a second repository available that only contains ASM 9.10.1
 * ("test.bundle.a" and "test.bundle.b" are left untouched in this second
 * step). The resulting profile
 * is inspected to determine which ASM bundle versions are present after the
 * upgrade.
 */
public class AsmMixedVersionTest extends AbstractProvisioningTest {

	private static final String JAVA_PACKAGE_NAMESPACE = "java.package";

	private static final String[] ASM_BUNDLE_IDS = { "org.objectweb.asm", "org.objectweb.asm.commons",
			"org.objectweb.asm.tree", "org.objectweb.asm.tree.analysis", "org.objectweb.asm.util" };

	/**
	 * Describes one ASM release: its version, and the exclusive upper bound of
	 * the narrow version range that the release's own bundles use to require
	 * each other (e.g. 9.9.1 -&gt; 9.10.0, 9.10.1 -&gt; 9.11.0).
	 */
	private record AsmRelease(Version version, String nextMinorExclusive) {
		String narrowRange() {
			return "[" + version + "," + nextMinorExclusive + ")";
		}
	}

	private static final AsmRelease ASM_9_9_1 = new AsmRelease(Version.create("9.9.1"), "9.10.0");
	private static final AsmRelease ASM_9_10_1 = new AsmRelease(Version.create("9.10.1"), "9.11.0");

	private static IRequirement packageRequirement(String packageName, String versionRange) {
		return MetadataFactory.createRequirement(JAVA_PACKAGE_NAMESPACE, packageName, new VersionRange(versionRange),
				null, false, false, true);
	}

	private static IProvidedCapability packageCapability(String packageName, Version version) {
		return packageCapability(packageName, version, null);
	}

	/**
	 * Creates a <code>java.package</code> provided capability, optionally
	 * carrying a <code>uses</code> directive - exactly as real OSGi bundles
	 * declare in their <code>Export-Package</code> manifest header (e.g.
	 * <code>Export-Package: org.objectweb.asm.util;uses:="org.objectweb.asm,
	 * org.objectweb.asm.tree,org.objectweb.asm.tree.analysis"</code>), and as
	 * seen in the real Orbit ASM metadata's
	 * <code>java.package.directive.uses</code> property. The <code>uses</code>
	 * directive tells consumers of this package which other packages must be
	 * used in a version-compatible way alongside it - see
	 * https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html#i3127019
	 */
	private static IProvidedCapability packageCapability(String packageName, Version version, String uses) {
		if (uses == null) {
			return MetadataFactory.createProvidedCapability(JAVA_PACKAGE_NAMESPACE, packageName, version);
		}
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(JAVA_PACKAGE_NAMESPACE, packageName);
		properties.put(IProvidedCapability.PROPERTY_VERSION, version);
		properties.put(JAVA_PACKAGE_NAMESPACE + ".directive.uses", uses);
		return MetadataFactory.createProvidedCapability(JAVA_PACKAGE_NAMESPACE, properties);
	}

	/**
	 * Creates the five ASM bundles (org.objectweb.asm, .commons, .tree,
	 * .tree.analysis and .util) for the given release, wired together exactly
	 * like the real Orbit bundles: org.objectweb.asm provides both the
	 * org.objectweb.asm and org.objectweb.asm.signature packages and requires
	 * nothing; org.objectweb.asm.tree requires org.objectweb.asm;
	 * org.objectweb.asm.commons requires org.objectweb.asm and
	 * org.objectweb.asm.tree; org.objectweb.asm.tree.analysis requires
	 * org.objectweb.asm and org.objectweb.asm.tree; org.objectweb.asm.util
	 * requires org.objectweb.asm, org.objectweb.asm.tree and
	 * org.objectweb.asm.tree.analysis. All these internal requirements use the
	 * narrow, auto-generated version range of the release, so they only ever
	 * match bundles from that very same ASM release.
	 */
	private static List<IInstallableUnit> createAsmRelease(AsmRelease release) {
		Version version = release.version();
		String range = release.narrowRange();

		IInstallableUnit asm = createIU("org.objectweb.asm", version,
				new IProvidedCapability[] { packageCapability("org.objectweb.asm", version),
						packageCapability("org.objectweb.asm.signature", version) });

		IInstallableUnit tree = createIU("org.objectweb.asm.tree", version, null,
				new IRequirement[] { packageRequirement("org.objectweb.asm", range),
						packageRequirement("org.objectweb.asm.signature", range) },
				new IProvidedCapability[] { packageCapability("org.objectweb.asm.tree", version,
						"org.objectweb.asm,org.objectweb.asm.signature") },
				NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false);

		IInstallableUnit commons = createIU("org.objectweb.asm.commons", version, null,
				new IRequirement[] { packageRequirement("org.objectweb.asm", range),
						packageRequirement("org.objectweb.asm.signature", range),
						packageRequirement("org.objectweb.asm.tree", range) },
				new IProvidedCapability[] { packageCapability("org.objectweb.asm.commons", version,
						"org.objectweb.asm,org.objectweb.asm.signature,org.objectweb.asm.tree") },
				NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false);

		IInstallableUnit treeAnalysis = createIU("org.objectweb.asm.tree.analysis", version, null,
				new IRequirement[] { packageRequirement("org.objectweb.asm", range),
						packageRequirement("org.objectweb.asm.signature", range),
						packageRequirement("org.objectweb.asm.tree", range) },
				new IProvidedCapability[] { packageCapability("org.objectweb.asm.tree.analysis", version,
						"org.objectweb.asm,org.objectweb.asm.signature,org.objectweb.asm.tree") },
				NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false);

		IInstallableUnit util = createIU("org.objectweb.asm.util", version, null,
				new IRequirement[] { packageRequirement("org.objectweb.asm", range),
						packageRequirement("org.objectweb.asm.signature", range),
						packageRequirement("org.objectweb.asm.tree", range),
						packageRequirement("org.objectweb.asm.tree.analysis", range) },
				new IProvidedCapability[] { packageCapability("org.objectweb.asm.util", version,
						"org.objectweb.asm,org.objectweb.asm.signature,org.objectweb.asm.tree,org.objectweb.asm.tree.analysis") },
				NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false);

		List<IInstallableUnit> ius = new ArrayList<>();
		ius.add(asm);
		ius.add(tree);
		ius.add(commons);
		ius.add(treeAnalysis);
		ius.add(util);
		return ius;
	}

	/**
	 * Bundle that imports org.objectweb.asm, org.objectweb.asm.commons and
	 * org.objectweb.asm.util with a wide version range, similar to a consumer
	 * built against an old ASM baseline but tolerant of newer releases. Importing
	 * org.objectweb.asm.util (in addition to org.objectweb.asm.commons) is what
	 * makes the problem visible: once a newer org.objectweb.asm.util becomes
	 * available and gets pulled in for some other reason, this bundle's own wide
	 * range would happily accept it too, but the planner has no reason to move it
	 * there as long as the older release already satisfies all requirements.
	 */
	/**
	 * test.bundle.a's own Import-Package requirements (package name -&gt; version
	 * range), reused both to build the bundle itself and to later check whether
	 * all of them ultimately resolve to the same ASM release.
	 */
	private static final Map<String, String> BUNDLE_A_REQUIREMENTS = Map.of("org.objectweb.asm", "[9.6.0,10.0.0)",
			"org.objectweb.asm.commons", "[9.6.0,10.0.0)", "org.objectweb.asm.util", "[9.6.0,10.0.0)");

	private static IInstallableUnit createBundleA() {
		IRequirement[] requirements = BUNDLE_A_REQUIREMENTS.entrySet().stream()
				.map(e -> packageRequirement(e.getKey(), e.getValue())).toArray(IRequirement[]::new);
		return createIU("test.bundle.a", Version.create("1.0.0"), requirements);
	}

	/**
	 * Bundle that imports only org.objectweb.asm.commons with a narrow version
	 * range, similar to a consumer that pins tightly to a specific ASM minor
	 * release (as bnd/tycho typically auto-generate for Import-Package ranges).
	 */
	private static IInstallableUnit createBundleB() {
		IRequirement[] requirements = { packageRequirement("org.objectweb.asm.commons", "[9.9.0,9.10.0)") };
		return createIU("test.bundle.b", Version.create("1.0.0"), requirements);
	}

	/**
	 * Bundle that imports only org.objectweb.asm.util with a wide open-ended
	 * version range (no upper bound), similar to a consumer that is tolerant of
	 * any future ASM release. This bundle transitively pulls in
	 * org.objectweb.asm.tree.analysis, org.objectweb.asm.tree and
	 * org.objectweb.asm (all narrowly pinned to the same release as the
	 * org.objectweb.asm.util version that gets picked), and is used to check
	 * whether installing it - once the newer ASM release becomes available -
	 * causes org.objectweb.asm and org.objectweb.asm.tree to end up installed in
	 * both versions side by side, while org.objectweb.asm.commons (which is only
	 * required with a narrow range by test.bundle.b) stays pinned to the older
	 * release.
	 */
	private static IInstallableUnit createBundleC() {
		IRequirement[] requirements = { packageRequirement("org.objectweb.asm.util", "[9.4.0,)") };
		return createIU("test.bundle.c", Version.create("1.0.0"), requirements);
	}

	private static SortedSet<Version> versionsOf(IQueryable<IInstallableUnit> queryable, String id) {
		SortedSet<Version> versions = new TreeSet<>();
		for (IInstallableUnit iu : queryable.query(QueryUtil.createIUQuery(id), new NullProgressMonitor())) {
			versions.add(iu.getVersion());
		}
		return versions;
	}

	private static String printAsmVersions(IQueryable<IInstallableUnit> queryable) {
		StringBuilder sb = new StringBuilder();
		for (String id : ASM_BUNDLE_IDS) {
			sb.append(id).append(" = ").append(versionsOf(queryable, id)).append(System.lineSeparator());
		}
		return sb.toString();
	}

	/**
	 * Hands the given profile's installable units over to the real, standard
	 * OSGi resolver (see {@link OsgiUsesOracle}) to check whether they could
	 * actually be wired together consistently by a real OSGi framework, i.e.
	 * without violating any package <code>uses</code> constraint. Fails the test
	 * with the resolver's own diagnostic message if not.
	 */
	private static void assertOsgiUsesConsistent(String message, IProfile profile) {
		List<IInstallableUnit> ius = profile.query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).toUnmodifiableSet()
				.stream().toList();
		ResolutionException exception = OsgiUsesOracle.validate(ius, TestActivator.getContext());
		if (exception != null) {
			fail(message + " - the real OSGi resolver reports: " + exception.getMessage());
		}
	}

	/**
	 * Plans and executes the given request with a {@link ProvisioningContext}.
	 * All IUs in this test are synthetic (no real artifacts), so there is
	 * nothing to collect, download or trust-check - the test is only interested
	 * in which IU versions the planner/engine settle on.
	 */
	private static IStatus performUnattended(IProfileChangeRequest request, IPlanner planner, IEngine engine,
			IProvisioningAgent agent) {
		ProvisioningContext context = new ProvisioningContext(agent);
		IProvisioningPlan plan = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		if (!plan.getStatus().isOK()) {
			return plan.getStatus();
		}
		return engine.perform(plan, new NullProgressMonitor());
	}

	public void testAsmVersionsMixAfterUpgrade() throws ProvisionException {
		IPlanner planner = getPlanner(getAgent());
		IEngine engine = getEngine();

		// Phase 1: fresh install against a repository that only has ASM 9.9.1
		IInstallableUnit bundleA = createBundleA();
		IInstallableUnit bundleB = createBundleB();
		List<IInstallableUnit> initialRepoContent = new ArrayList<>(createAsmRelease(ASM_9_9_1));
		initialRepoContent.add(bundleA);
		initialRepoContent.add(bundleB);
		createTestMetdataRepository(initialRepoContent.toArray(IInstallableUnit[]::new));

		IProfile profile = createProfile("AsmMixedVersionTestProfile");
		IProfileChangeRequest initialRequest = planner.createChangeRequest(profile);
		for (IInstallableUnit iu : new IInstallableUnit[] { bundleA, bundleB }) {
			initialRequest.add(iu);
			initialRequest.setInstallableUnitInclusionRules(iu, ProfileInclusionRules.createStrictInclusionRule(iu));
			initialRequest.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_ROOT_IU,
					Boolean.TRUE.toString());
		}
		assertOK(performUnattended(initialRequest, planner, engine, getAgent()));

		profile = getProfile(profile.getProfileId());
		System.out.println("=== After initial install ===");
		System.out.println(printAsmVersions(profile));
		for (String id : ASM_BUNDLE_IDS) {
			assertEquals("Unexpected version(s) for " + id + " after initial install",
					new TreeSet<>(List.of(ASM_9_9_1.version())), versionsOf(profile, id));
		}
		assertOsgiUsesConsistent("Profile after initial install should be OSGi-resolvable", profile);

		// Phase 2: install test.bundle.c (which requires the newer ASM release via
		// org.objectweb.asm.util), both ASM releases are now available;
		// test.bundle.a is intentionally left untouched.
		IInstallableUnit bundleC = createBundleC();
		List<IInstallableUnit> secondRepoContent = new ArrayList<>(createAsmRelease(ASM_9_10_1));
		secondRepoContent.add(bundleC);
		createTestMetdataRepository(secondRepoContent.toArray(IInstallableUnit[]::new));

		IProfileChangeRequest request = planner.createChangeRequest(profile);
		request.add(bundleC);
		request.setInstallableUnitInclusionRules(bundleC, ProfileInclusionRules.createStrictInclusionRule(bundleC));
		request.setInstallableUnitProfileProperty(bundleC, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());

		assertOK(performUnattended(request, planner, engine, getAgent()));

		profile = getProfile(profile.getProfileId());
		System.out.println("=== After installing test.bundle.c ===");
		System.out.println(printAsmVersions(profile));

		// What one would actually expect: the resulting profile must be something
		// a real OSGi framework could actually resolve/wire together, i.e. it must
		// not violate any package "uses" constraint
		// (https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html#i3127019).
		// Rather than re-implementing that check ourselves with a simplified,
		// necessarily incomplete heuristic, we hand the profile's IUs over to the
		// real, standard OSGi resolver (see OsgiUsesOracle) and let it tell us
		// authoritatively whether this is the case.
		assertOsgiUsesConsistent("Profile after installing test.bundle.c is not OSGi-resolvable", profile);
	}

	/**
	 * EXPERIMENT: does a completely *fresh* install reproduce the same
	 * inconsistency, with no update/upgrade step involved at all? Both ASM
	 * releases (9.9.1 and 9.10.1) are made available from the very start, and
	 * "test.bundle.a", "test.bundle.b" and "test.bundle.c" are all installed
	 * together, as roots, in a single request against a brand new (empty)
	 * profile.
	 */
	public void testAsmVersionsMixOnFreshInstall() throws ProvisionException {
		IPlanner planner = getPlanner(getAgent());
		IEngine engine = getEngine();

		IInstallableUnit bundleA = createBundleA();
		IInstallableUnit bundleB = createBundleB();
		IInstallableUnit bundleC = createBundleC();
		List<IInstallableUnit> repoContent = new ArrayList<>(createAsmRelease(ASM_9_9_1));
		repoContent.addAll(createAsmRelease(ASM_9_10_1));
		repoContent.add(bundleA);
		repoContent.add(bundleB);
		repoContent.add(bundleC);
		createTestMetdataRepository(repoContent.toArray(IInstallableUnit[]::new));

		IProfile profile = createProfile("AsmFreshInstallTestProfile");
		IProfileChangeRequest request = planner.createChangeRequest(profile);
		for (IInstallableUnit iu : new IInstallableUnit[] { bundleA, bundleB, bundleC }) {
			request.add(iu);
			request.setInstallableUnitInclusionRules(iu, ProfileInclusionRules.createStrictInclusionRule(iu));
			request.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
		}
		assertOK(performUnattended(request, planner, engine, getAgent()));

		profile = getProfile(profile.getProfileId());
		System.out.println("=== After fresh install of A+B+C (both ASM releases available) ===");
		System.out.println(printAsmVersions(profile));

		assertOsgiUsesConsistent("Profile after fresh install of A+B+C is not OSGi-resolvable", profile);
	}
}
