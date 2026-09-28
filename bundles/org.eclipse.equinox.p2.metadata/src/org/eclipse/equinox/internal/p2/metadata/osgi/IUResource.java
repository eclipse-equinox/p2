/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
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
package org.eclipse.equinox.internal.p2.metadata.osgi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * Maps an {@link IInstallableUnit} into an OSGi
 * <a href="https://docs.osgi.org/specification/osgi.core/8.0.0/framework.resource.html">Resource
 * API Specification</a> {@link Resource}.
 * <p>
 * Only requirements/capabilities that have a well known, version range based
 * shape (e.g. bundle-, package- and identity-requirements/capabilities as
 * produced for example by
 * <code>org.eclipse.equinox.p2.publisher.eclipse.BundlesAction</code>) are
 * translated into their OSGi counterpart. Generic requirements/capabilities
 * that use an arbitrary LDAP filter (as they can be declared through the
 * <code>Require-Capability</code>/<code>Provide-Capability</code> manifest
 * headers) are passed through unmodified so no information is lost, but they
 * are not further interpreted.
 * </p>
 */
public class IUResource implements Resource {

	/**
	 * Namespace used by p2 to describe the java package capabilities/requirements,
	 * mirrors <code>PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE</code> that can't be
	 * referenced here for layering reasons.
	 */
	public static final String NAMESPACE_JAVA_PACKAGE = "java.package"; //$NON-NLS-1$

	/**
	 * Namespace used by p2 to describe the osgi bundle capabilities/requirements,
	 * mirrors <code>BundlesAction.CAPABILITY_NS_OSGI_BUNDLE</code> that can't be
	 * referenced here for layering reasons.
	 */
	public static final String NAMESPACE_OSGI_BUNDLE = "osgi.bundle"; //$NON-NLS-1$

	/**
	 * Namespace used by p2 to describe that an installable unit represents an
	 * OSGi fragment, mirrors
	 * <code>BundlesAction.CAPABILITY_NS_OSGI_FRAGMENT</code> that can't be
	 * referenced here for layering reasons.
	 */
	public static final String NAMESPACE_OSGI_FRAGMENT = "osgi.fragment"; //$NON-NLS-1$

	static final String PACKAGE_ATTRIBUTE_PROPERTY_PREFIX = NAMESPACE_JAVA_PACKAGE + ".attribute."; //$NON-NLS-1$
	static final String PACKAGE_DIRECTIVE_PROPERTY_PREFIX = NAMESPACE_JAVA_PACKAGE + ".directive."; //$NON-NLS-1$

	final IInstallableUnit installableUnit;
	private final Map<String, List<Requirement>> requirementsMap;
	private final Map<String, List<Capability>> capabilitiesMap;
	private final boolean fragment;

	public IUResource(IInstallableUnit installableUnit) {
		this.installableUnit = installableUnit;
		String hostName = installableUnit.getProvidedCapabilities().stream()
				.filter(capability -> NAMESPACE_OSGI_FRAGMENT.equals(capability.getNamespace()))
				.map(IProvidedCapability::getName).findFirst().orElse(null);
		this.fragment = hostName != null;
		Collection<IRequirement> requirements = installableUnit.getRequirements();
		requirementsMap = new HashMap<>(requirements.size());
		for (IRequirement requirement : requirements) {
			if (RequiredCapability.isVersionRangeRequirement(requirement.getMatches())) {
				IURequirement req = new IURequirement(this, requirement, hostName);
				requirementsMap.computeIfAbsent(req.getNamespace(), nil -> new ArrayList<>()).add(req);
			}
		}
		Collection<IProvidedCapability> capabilities = installableUnit.getProvidedCapabilities();
		capabilitiesMap = new HashMap<>(capabilities.size() + 1);
		for (IProvidedCapability capability : capabilities) {
			if (NAMESPACE_OSGI_FRAGMENT.equals(capability.getNamespace())) {
				// there is no OSGi resource equivalent for this, it is only used to derive the
				// osgi.wiring.host requirement/osgi.identity type above/below.
				continue;
			}
			if (fragment && NAMESPACE_OSGI_BUNDLE.equals(capability.getNamespace())) {
				// unlike p2, in OSGi a fragment does not itself resolve to a bundle wiring and
				// therefore does not provide an osgi.wiring.bundle capability.
				continue;
			}
			IUCapability cap = new IUCapability(this, capability);
			capabilitiesMap.computeIfAbsent(cap.getNamespace(), nil -> new ArrayList<>()).add(cap);
		}
		List<Capability> identityCapabilities = capabilitiesMap.get(IdentityNamespace.IDENTITY_NAMESPACE);
		if (identityCapabilities == null || identityCapabilities.isEmpty()) {
			// only synthesize an identity capability if the installable unit does not
			// already provide one of its own (e.g. as produced by BundlesAction)
			capabilitiesMap.computeIfAbsent(IdentityNamespace.IDENTITY_NAMESPACE, nil -> new ArrayList<>())
					.add(new IUIdentityCapability(this, fragment));
		}
	}

	boolean isFragment() {
		return fragment;
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		if (namespace != null) {
			return Collections.unmodifiableList(capabilitiesMap.getOrDefault(namespace, List.of()));
		}
		return capabilitiesMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		if (namespace != null) {
			return Collections.unmodifiableList(requirementsMap.getOrDefault(namespace, List.of()));
		}
		return requirementsMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return installableUnit.toString();
	}

}
