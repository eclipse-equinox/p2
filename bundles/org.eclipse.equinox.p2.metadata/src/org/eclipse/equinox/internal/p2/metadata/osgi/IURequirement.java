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

import static org.eclipse.equinox.internal.p2.metadata.osgi.IUResource.FEATURE_GROUP_ID_SUFFIX;
import static org.eclipse.equinox.internal.p2.metadata.osgi.IUResource.NAMESPACE_JAVA_PACKAGE;
import static org.eclipse.equinox.internal.p2.metadata.osgi.IUResource.NAMESPACE_OSGI_BUNDLE;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.metadata.osgi.namespace.FeatureNamespace;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * Maps a {@link IRequirement} of an {@link IInstallableUnit} into an OSGi
 * <a href="https://docs.osgi.org/specification/osgi.core/8.0.0/framework.resource.html">Resource
 * API Specification</a> {@link Requirement}
 */
public class IURequirement implements Requirement {

	private static final String VERSION_REQUIREMENT_FILTER = "(&(%s=%s)(%s%s%s))"; //$NON-NLS-1$
	private static final String VERSION_RANGE_REQUIREMENT_FILTER = "(&(%s=%s)(%s%s%s)(!(%s%s%s)))"; //$NON-NLS-1$

	private final IUResource resource;
	private final String namespace;
	private final Map<String, String> directives;
	private final IRequirement requirement;

	/**
	 * @param resource the resource this requirement belongs to
	 * @param requirement the requirement to translate
	 * @param hostName the name of the fragment-host (as given by the
	 *            {@value IUResource#NAMESPACE_OSGI_FRAGMENT} capability) if the
	 *            owning installable unit is an OSGi fragment, <code>null</code>
	 *            otherwise. This is used to distinguish a fragment-host
	 *            requirement (mapped to {@link HostNamespace#HOST_NAMESPACE})
	 *            from a plain <code>Require-Bundle</code> requirement (mapped to
	 *            {@link BundleNamespace#BUNDLE_NAMESPACE}) as both use the same
	 *            p2 {@value IUResource#NAMESPACE_OSGI_BUNDLE} namespace.
	 */
	public IURequirement(IUResource resource, IRequirement requirement, String hostName) {
		this.requirement = requirement;
		this.resource = resource;
		Map<String, String> map = new HashMap<>(2);
		IMatchExpression<IInstallableUnit> expression = requirement.getMatches();
		String name = RequiredCapability.extractName(expression);
		String ns = RequiredCapability.extractNamespace(expression);
		VersionRange range = RequiredCapability.extractRange(expression);
		String versionAttribute;
		if (NAMESPACE_JAVA_PACKAGE.equals(ns)) {
			this.namespace = PackageNamespace.PACKAGE_NAMESPACE;
			versionAttribute = PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		} else if (NAMESPACE_OSGI_BUNDLE.equals(ns)) {
			if (hostName != null && hostName.equals(name)) {
				this.namespace = HostNamespace.HOST_NAMESPACE;
				versionAttribute = HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
			} else {
				this.namespace = BundleNamespace.BUNDLE_NAMESPACE;
				versionAttribute = BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
			}
		} else if (resource.isFeature() && IInstallableUnit.NAMESPACE_IU_ID.equals(ns)) {
			// a feature "contains" requirement: either another nested feature (its
			// group IU id carries the FEATURE_GROUP_ID_SUFFIX, stripped here since it
			// is a p2-internal convention) or a plain plugin/bundle (unsuffixed id).
			if (name.endsWith(FEATURE_GROUP_ID_SUFFIX)) {
				this.namespace = FeatureNamespace.FEATURE_NAMESPACE;
				versionAttribute = FeatureNamespace.CAPABILITY_VERSION_ATTRIBUTE;
				name = name.substring(0, name.length() - FEATURE_GROUP_ID_SUFFIX.length());
			} else {
				this.namespace = BundleNamespace.BUNDLE_NAMESPACE;
				versionAttribute = BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
			}
		} else {
			this.namespace = ns;
			versionAttribute = "version"; //$NON-NLS-1$
		}
		Version minimum = range.getMinimum();
		Version maximum = range.getMaximum();
		String filter;
		if (Version.MAX_VERSION.equals(maximum)) {
			filter = String.format(VERSION_REQUIREMENT_FILTER, namespace, name, //
					versionAttribute, range.getIncludeMinimum() ? ">=" : ">", minimum.toString()); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			filter = String.format(VERSION_RANGE_REQUIREMENT_FILTER, namespace, name, //
					versionAttribute, range.getIncludeMinimum() ? ">=" : ">", minimum.toString(), versionAttribute, //$NON-NLS-1$ //$NON-NLS-2$
					range.getIncludeMaximum() ? ">" : ">=", maximum.toString()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		map.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
		if (requirement.getMin() == 0) {
			map.put(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Namespace.RESOLUTION_OPTIONAL);
		}
		if (requirement.getMax() > 1) {
			map.put(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, Namespace.CARDINALITY_MULTIPLE);
		}
		this.directives = Map.copyOf(map);
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public Map<String, String> getDirectives() {
		return directives;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return Map.of();
	}

	@Override
	public Resource getResource() {
		return resource;
	}

	@Override
	public String toString() {
		return requirement.toString();
	}

}
