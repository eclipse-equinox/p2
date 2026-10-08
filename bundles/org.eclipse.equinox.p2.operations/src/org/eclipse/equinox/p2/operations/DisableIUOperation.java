/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
 ******************************************************************************/
package org.eclipse.equinox.p2.operations;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.p2.operations.IFailedStatusEvaluator;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;

/**
 * A {@link DisableIUOperation} disables {@link IInstallableUnit}s in a profile
 * without removing them from disk. Disabling removes the
 * {@link IProfile#PROP_PROFILE_ROOT_IU} property so the p2 resolver no longer
 * treats the unit as an active root. The unit can be re-enabled cheaply via
 * {@link EnableIUOperation}.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.8
 */
public class DisableIUOperation extends ProfileChangeOperation {

	/** Relative path of bundles.info inside the configuration folder. */
	static final String BUNDLES_INFO_PATH = "org.eclipse.equinox.simpleconfigurator/bundles.info"; //$NON-NLS-1$

	/**
	 * Bundle symbolic names that are critical to the OSGi framework and must
	 * never be removed from bundles.info, regardless of what was selected.
	 * Removing any of them makes the install unbootable until manually repaired.
	 */
	private static final Set<String> PROTECTED_BUNDLES = Set.of(
			"org.eclipse.osgi", //$NON-NLS-1$
			"org.eclipse.core.runtime", //$NON-NLS-1$
			"org.eclipse.equinox.common", //$NON-NLS-1$
			"org.eclipse.equinox.registry", //$NON-NLS-1$
			"org.eclipse.equinox.preferences", //$NON-NLS-1$
			"org.eclipse.equinox.app", //$NON-NLS-1$
			"org.eclipse.equinox.simpleconfigurator", //$NON-NLS-1$
			"org.eclipse.update.configurator"); //$NON-NLS-1$

	private final Collection<IInstallableUnit> toDisable;

	public DisableIUOperation(ProvisioningSession session, Collection<IInstallableUnit> toDisable) {
		super(session);
		this.toDisable = toDisable;
	}

	/**
	 * Profile property used to tag IUs that were explicitly disabled by the user.
	 * This distinguishes them from transitive dependencies (which also have no root property).
	 */
	public static final String PROP_DISABLED = "org.eclipse.p2.disabled"; //$NON-NLS-1$

	@Override
	protected void computeProfileChangeRequest(MultiStatus status, IProgressMonitor monitor) {
		request = ProfileChangeRequest.createByProfileId(session.getProvisioningAgent(), profileId);
		for (IInstallableUnit iu : toDisable) {
			request.removeInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_ROOT_IU);
			request.setInstallableUnitProfileProperty(iu, PROP_DISABLED, Boolean.TRUE.toString());
		}
	}

	private static Collection<IInstallableUnit> expandToBundleIUs(
			Collection<IInstallableUnit> ius, IProfile profile) {
		List<IInstallableUnit> bundles = new ArrayList<>();
		for (IInstallableUnit iu : ius) {
			if (getOsgiBundleName(iu) != null) {
				// Already a bundle IU — use it directly, unless it's protected.
				if (PROTECTED_BUNDLES.contains(iu.getId()) || PROTECTED_BUNDLES.contains(getOsgiBundleName(iu))) {
					LogHelper.log(new Status(IStatus.WARNING, "org.eclipse.equinox.p2.operations", //$NON-NLS-1$
							"expandToBundleIUs: refusing to disable protected bundle " + iu.getId())); //$NON-NLS-1$
				} else {
					bundles.add(iu);
				}
			} else {
				// Feature group or category: walk owned plugin requirements only.
				//
				// p2 features have two kinds of requirements:
				//   1. <plugin> — a component the feature OWNS, always encoded with an
				//      exact version range [v, v] in either the org.eclipse.equinox.p2.iu
				//      or osgi.bundle namespace (e.g. egit uses osgi.bundle). Safe to disable.
				//   2. <import> — a dependency the feature does NOT own, encoded with an
				//      open range like [3.208.0, 4.0.0). Must never be removed from
				//      bundles.info — doing so disables platform bundles like org.eclipse.ui.
				//
				// We distinguish them by version range: exact min==max → owned plugin,
				// open range → import dependency. This is more reliable than the
				// exclude.import filter, which is not consistently present across all
				// feature publishers (e.g. egit's <import> entries have no filter at all).
				for (IRequirement req : iu.getRequirements()) {
					IMatchExpression<IInstallableUnit> matchExpr = req.getMatches();
					if (matchExpr == null || !isOwnedPluginRequirement(matchExpr)) {
						continue;
					}
					IQueryResult<IInstallableUnit> matches =
							profile.query(QueryUtil.createMatchQuery(matchExpr), null);
					for (IInstallableUnit match : matches) {
						if (getOsgiBundleName(match) != null) {
							if (PROTECTED_BUNDLES.contains(match.getId())
									|| PROTECTED_BUNDLES.contains(getOsgiBundleName(match))) {
								LogHelper.log(new Status(IStatus.WARNING, "org.eclipse.equinox.p2.operations", //$NON-NLS-1$
										"expandToBundleIUs: refusing to disable protected bundle " //$NON-NLS-1$
										+ getOsgiBundleName(match) + " (matched via requirement of " //$NON-NLS-1$
										+ iu.getId() + ")")); //$NON-NLS-1$
								continue;
							}
							bundles.add(match);
						}
					}
				}
			}
		}
		return bundles;
	}

	private static boolean isOwnedPluginRequirement(IMatchExpression<IInstallableUnit> matchExpr) {
		// Must be in the org.eclipse.equinox.p2.iu or osgi.bundle namespace.
		boolean knownNamespace = false;
		for (Object param : matchExpr.getParameters()) {
			if (IInstallableUnit.NAMESPACE_IU_ID.equals(param) || "osgi.bundle".equals(param)) { //$NON-NLS-1$
				knownNamespace = true;
				break;
			}
		}
		if (!knownNamespace) {
			return false;
		}
		try {
			VersionRange range = RequiredCapability.extractRange(matchExpr);
			return range.getMinimum().equals(range.getMaximum());
		} catch (Exception e) {
			return false;
		}
	}

	public static void updateBundlesInfo(Collection<IInstallableUnit> ius, boolean markedAsStarted,
			ProvisioningSession session, String profileId) {
		IProfile profile = session.getProfileRegistry().getProfile(profileId);
		Path bundlesInfo = resolveBundlesInfo(profile);
		if (bundlesInfo == null || !Files.exists(bundlesInfo)) {
			return;
		}
		try {
			if (markedAsStarted) {
				restoreBundleLines(bundlesInfo, ius);
			} else {
				Collection<IInstallableUnit> bundleIUs = expandToBundleIUs(ius, profile);
				if (bundleIUs.isEmpty()) {
					return;
				}
				removeBundleLines(bundlesInfo, ius, bundleIUs);
			}
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.WARNING, "org.eclipse.equinox.p2.operations", //$NON-NLS-1$
					"Failed to update bundles.info", e)); //$NON-NLS-1$
		}
	}

	private static void removeBundleLines(Path bundlesInfo, Collection<IInstallableUnit> rootIUs,
			Collection<IInstallableUnit> bundleIUs) throws IOException {
		List<String> lines = Files.readAllLines(bundlesInfo, StandardCharsets.UTF_8);
		StringBuilder removed = new StringBuilder();
		StringBuilder kept = new StringBuilder();
		for (String line : lines) {
			if (isMatchedBundle(line, bundleIUs)) {
				removed.append(line).append('\n');
			} else {
				kept.append(line).append('\n');
			}
		}
		if (removed.length() == 0) {
			return; // no matching bundles found — nothing to do
		}
		Path store = bundlesInfo.getParent().resolve("disabled-bundles.info"); //$NON-NLS-1$
		StringBuilder tag = new StringBuilder();
		for (IInstallableUnit iu : rootIUs) {
			tag.append("#disabled:").append(iu.getId()).append('\n'); //$NON-NLS-1$
		}
		String existing = Files.exists(store) ? Files.readString(store, StandardCharsets.UTF_8) : ""; //$NON-NLS-1$
		Files.writeString(store, existing + tag + removed, StandardCharsets.UTF_8);
		Files.writeString(bundlesInfo, kept.toString(), StandardCharsets.UTF_8);
	}

	private static void restoreBundleLines(Path bundlesInfo, Collection<IInstallableUnit> ius) throws IOException {
		Path store = bundlesInfo.getParent().resolve("disabled-bundles.info"); //$NON-NLS-1$
		if (!Files.exists(store)) {
			return;
		}

		java.util.Set<String> iuIds = new java.util.HashSet<>();
		for (IInstallableUnit iu : ius) {
			iuIds.add(iu.getId());
			String bundleName = getOsgiBundleName(iu);
			if (bundleName != null) {
				iuIds.add(bundleName);
			}
		}

		List<String> storeLines = Files.readAllLines(store, StandardCharsets.UTF_8);
		StringBuilder toRestore = new StringBuilder();
		StringBuilder remaining = new StringBuilder();
		boolean inMatchingBlock = false;
		for (String line : storeLines) {
			if (line.startsWith("#disabled:")) { //$NON-NLS-1$
				String taggedId = line.substring("#disabled:".length()); //$NON-NLS-1$
				inMatchingBlock = iuIds.contains(taggedId);
				if (!inMatchingBlock) {
					remaining.append(line).append('\n');
				}
				continue;
			}
			if (inMatchingBlock) {
				toRestore.append(line).append('\n');
			} else {
				remaining.append(line).append('\n');
			}
		}
		if (toRestore.length() == 0) {
			return; // nothing to restore
		}
		List<String> currentLines = Files.readAllLines(bundlesInfo, StandardCharsets.UTF_8);
		StringBuilder updated = new StringBuilder();
		for (String line : currentLines) {
			updated.append(line).append('\n');
		}
		updated.append(toRestore);
		Files.writeString(bundlesInfo, updated.toString(), StandardCharsets.UTF_8);
		String remainingStr = remaining.toString().stripTrailing();
		if (remainingStr.isEmpty()) {
			Files.deleteIfExists(store);
		} else {
			Files.writeString(store, remainingStr + "\n", StandardCharsets.UTF_8); //$NON-NLS-1$
		}
	}

	private static boolean isMatchedBundle(String line, Collection<IInstallableUnit> ius) {
		String trimmed = line.trim();
		if (trimmed.isEmpty() || trimmed.startsWith("#")) { //$NON-NLS-1$
			return false;
		}
		String[] parts = trimmed.split(",", -1); //$NON-NLS-1$
		if (parts.length < 5) {
			return false;
		}
		String symbolicName = parts[0].trim();
		String version = parts[1].trim();
		for (IInstallableUnit iu : ius) {
			String bundleName = getOsgiBundleName(iu);
			String bundleVersion = getOsgiBundleVersion(iu);
			boolean nameMatch = symbolicName.equals(bundleName) || symbolicName.equals(iu.getId());
			boolean versionMatch = version.equals(bundleVersion);
			if (nameMatch && versionMatch) {
				return true;
			}
		}
		return false;
	}

	static String getOsgiBundleName(IInstallableUnit iu) {
		for (IProvidedCapability cap : iu.getProvidedCapabilities()) {
			if ("osgi.bundle".equals(cap.getNamespace())) { //$NON-NLS-1$
				return cap.getName();
			}
		}
		return null;
	}

	static String getOsgiBundleVersion(IInstallableUnit iu) {
		for (IProvidedCapability cap : iu.getProvidedCapabilities()) {
			if ("osgi.bundle".equals(cap.getNamespace())) { //$NON-NLS-1$
				return cap.getVersion().toString();
			}
		}
		return null;
	}

	private static Path resolveBundlesInfo(IProfile profile) {
		if (profile != null) {
			String config = profile.getProperty(IProfile.PROP_CONFIGURATION_FOLDER);
			if (config != null) {
				return toPath(config).resolve(BUNDLES_INFO_PATH);
			}
			String install = profile.getProperty(IProfile.PROP_INSTALL_FOLDER);
			if (install != null) {
				return toPath(install).resolve("configuration").resolve(BUNDLES_INFO_PATH); //$NON-NLS-1$
			}
		}
		String configArea = System.getProperty("osgi.configuration.area"); //$NON-NLS-1$
		if (configArea != null) {
			return toPath(configArea).resolve(BUNDLES_INFO_PATH);
		}
		return null;
	}

	private static Path toPath(String location) {
		if (location.startsWith("file:")) { //$NON-NLS-1$
			String path = location.substring("file:".length()); //$NON-NLS-1$
			if (path.startsWith("///")) { //$NON-NLS-1$
				path = path.substring(2); // "///foo" → "/foo"
			} else if (path.startsWith("//")) { //$NON-NLS-1$
				// "//host/path" — strip the authority (host) portion
				int slash = path.indexOf('/', 2);
				path = slash >= 0 ? path.substring(slash) : "/"; //$NON-NLS-1$
			}
			path = URLDecoder.decode(path, StandardCharsets.UTF_8);
			return Path.of(path);
		}
		return Path.of(location);
	}

	@Override
	protected String getProvisioningJobName() {
		return Messages.DisableOperation_ProvisioningJobName;
	}

	@Override
	protected String getResolveJobName() {
		return Messages.DisableOperation_ResolveJobName;
	}

	@Override
	ProvisioningContext getFirstPassProvisioningContext() {
		ProvisioningContext pc = new ProvisioningContext(session.getProvisioningAgent());
		pc.setMetadataRepositories();
		pc.setArtifactRepositories();
		return pc;
	}

	@Override
	IFailedStatusEvaluator getSecondPassEvaluator() {
		return failedPlan -> context;
	}
}
