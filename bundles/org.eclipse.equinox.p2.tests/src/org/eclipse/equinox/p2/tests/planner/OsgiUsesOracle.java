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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.resolver.Resolver;

/**
 * A validation "oracle" that checks whether a set of {@link IInstallableUnit
 * IInstallableUnits} - typically the result of a p2 planner/resolver
 * operation - is actually consistent with respect to the OSGi
 * <code>uses</code> constraint
 * (https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html#i3127019),
 * by handing the very same set over to the real, standard
 * {@link Resolver}/{@link ResolveContext} OSGi resolver service (backed by
 * Felix's resolver, embedded in <code>org.eclipse.osgi</code>).
 * <p>
 * Only the <code>java.package</code> namespace (p2's representation of Java
 * package import/export, see {@link PublisherHelper#CAPABILITY_NS_JAVA_PACKAGE})
 * is translated; every candidate {@link IInstallableUnit} is mapped 1:1 to an
 * OSGi {@link Resource} exposing {@link Capability Capabilities} for each of
 * its <code>java.package</code>-namespace {@link IProvidedCapability} (with
 * the <code>uses</code> directive carried over verbatim from the
 * <code>java.package.directive.uses</code> property, when present) and
 * {@link Requirement Requirements} for each of its <code>java.package</code>
 * -namespace {@link IRequirement}. All other namespaces are ignored, since
 * they are irrelevant for detecting a <code>uses</code>-conflict among Java
 * packages.
 * <p>
 * This is a validation-only prototype: it does not attempt to influence how
 * p2 arrives at its candidate set, it merely reports - via the thrown
 * {@link ResolutionException}, exactly as the real OSGi framework would when
 * actually resolving these bundles - whether the given set of IUs could ever
 * be wired together consistently by a real OSGi framework.
 */
public class OsgiUsesOracle {

	/**
	 * Builds the OSGi resource model for the given candidate installable units
	 * and asks the real OSGi {@link Resolver} service whether they can be
	 * resolved consistently (i.e. without violating any <code>uses</code>
	 * constraint).
	 *
	 * @param ius the candidate installable units, e.g. all installable units
	 *            currently present in a p2 profile
	 * @return <code>null</code> if the given IUs can be resolved consistently, or
	 *         the {@link ResolutionException} thrown by the real OSGi resolver
	 *         otherwise
	 */
	public static ResolutionException validate(Collection<IInstallableUnit> ius, BundleContext context) {
		Map<IInstallableUnit, IuResource> resources = new LinkedHashMap<>();
		for (IInstallableUnit iu : ius) {
			resources.put(iu, new IuResource(iu));
		}
		IuResolveContext resolveContext = new IuResolveContext(new ArrayList<>(resources.values()));
		org.osgi.framework.ServiceReference<Resolver> reference = context.getServiceReference(Resolver.class);
		if (reference == null) {
			throw new IllegalStateException("No OSGi " + Resolver.class.getName() + " service available"); //$NON-NLS-1$
		}
		Resolver resolver = context.getService(reference);
		try {
			resolver.resolve(resolveContext);
			return null;
		} catch (ResolutionException e) {
			return e;
		} finally {
			context.ungetService(reference);
		}
	}

	/** Maps a single {@link IInstallableUnit} to an OSGi {@link Resource}. */
	private static final class IuResource implements Resource {

		private final IInstallableUnit iu;
		private final List<Capability> capabilities = new ArrayList<>();
		private final List<Requirement> requirements = new ArrayList<>();

		IuResource(IInstallableUnit iu) {
			this.iu = iu;
			for (IProvidedCapability capability : iu.getProvidedCapabilities()) {
				if (PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE.equals(capability.getNamespace())) {
					capabilities.add(new IuCapability(this, capability));
				}
			}
			for (IRequirement requirement : iu.getRequirements()) {
				IMatchExpression<IInstallableUnit> expression = requirement.getMatches();
				if (RequiredCapability.isVersionRangeRequirement(expression)
						&& PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE.equals(RequiredCapability.extractNamespace(expression))) {
					requirements.add(new IuRequirement(this, requirement));
				}
			}
		}

		@Override
		public List<Capability> getCapabilities(String namespace) {
			if (namespace == null) {
				return Collections.unmodifiableList(capabilities);
			}
			return capabilities.stream().filter(c -> namespace.equals(c.getNamespace())).toList();
		}

		@Override
		public List<Requirement> getRequirements(String namespace) {
			if (namespace == null) {
				return Collections.unmodifiableList(requirements);
			}
			return requirements.stream().filter(r -> namespace.equals(r.getNamespace())).toList();
		}

		@Override
		public String toString() {
			return iu.toString();
		}
	}

	/**
	 * Maps a single <code>java.package</code>-namespace {@link IProvidedCapability}
	 * to an OSGi {@link Capability} in the {@link PackageNamespace#PACKAGE_NAMESPACE}
	 * namespace, carrying over the <code>uses</code> directive.
	 */
	private static final class IuCapability implements Capability {

		private final Resource resource;
		private final Map<String, Object> attributes = new HashMap<>();
		private final Map<String, String> directives = new HashMap<>();

		IuCapability(Resource resource, IProvidedCapability capability) {
			this.resource = resource;
			String packageName = capability.getName();
			attributes.put(PackageNamespace.PACKAGE_NAMESPACE, packageName);
			attributes.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE,
					new Version(capability.getVersion().toString()));
			Object uses = capability.getProperties()
					.get(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE + ".directive.uses"); //$NON-NLS-1$
			if (uses instanceof String usesString && !usesString.isBlank()) {
				directives.put(Namespace.CAPABILITY_USES_DIRECTIVE, usesString);
			}
		}

		@Override
		public String getNamespace() {
			return PackageNamespace.PACKAGE_NAMESPACE;
		}

		@Override
		public Map<String, String> getDirectives() {
			return directives;
		}

		@Override
		public Map<String, Object> getAttributes() {
			return attributes;
		}

		@Override
		public Resource getResource() {
			return resource;
		}

		@Override
		public String toString() {
			return getNamespace() + "; " + attributes; //$NON-NLS-1$
		}
	}

	/**
	 * Maps a single <code>java.package</code>-namespace {@link IRequirement} to an
	 * OSGi {@link Requirement} in the {@link PackageNamespace#PACKAGE_NAMESPACE}
	 * namespace, using a real LDAP filter (as a real bundle's Import-Package
	 * requirement would) to express the package name/version range constraint.
	 */
	private static final class IuRequirement implements Requirement {

		private final Resource resource;
		private final Map<String, String> directives = new HashMap<>();

		IuRequirement(Resource resource, IRequirement requirement) {
			this.resource = resource;
			IMatchExpression<IInstallableUnit> expression = requirement.getMatches();
			String name = RequiredCapability.extractName(expression);
			org.eclipse.equinox.p2.metadata.VersionRange range = RequiredCapability.extractRange(expression);
			String filter = "(&(" + PackageNamespace.PACKAGE_NAMESPACE + "=" + name + ")" //$NON-NLS-1$ //$NON-NLS-2$
					+ versionFilter(range) + ")"; //$NON-NLS-1$
			directives.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
			if (requirement.getMin() == 0) {
				directives.put(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Namespace.RESOLUTION_OPTIONAL);
			}
		}

		private static String versionFilter(org.eclipse.equinox.p2.metadata.VersionRange range) {
			String versionAttribute = PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
			org.eclipse.equinox.p2.metadata.Version minimum = range.getMinimum();
			org.eclipse.equinox.p2.metadata.Version maximum = range.getMaximum();
			if (org.eclipse.equinox.p2.metadata.Version.MAX_VERSION.equals(maximum)) {
				return "(" + versionAttribute + (range.getIncludeMinimum() ? ">=" : ">") + minimum + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			return "(" + versionAttribute + (range.getIncludeMinimum() ? ">=" : ">") + minimum + ")" //$NON-NLS-1$ //$NON-NLS-2$
					+ "(!(" + versionAttribute + (range.getIncludeMaximum() ? ">" : ">=") + maximum + "))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		@Override
		public String getNamespace() {
			return PackageNamespace.PACKAGE_NAMESPACE;
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
			return getNamespace() + "; " + directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE); //$NON-NLS-1$
		}
	}

	/**
	 * A {@link ResolveContext} restricted to a fixed, already fully-p2-solved
	 * candidate universe: every given resource is mandatory (we want to know
	 * whether all of them, exactly as p2 chose them, can be wired together
	 * consistently - not whether some subset could be dropped), and
	 * {@link #findProviders(Requirement)} matches the requirement's LDAP filter
	 * against every candidate capability of every resource in that same fixed
	 * universe (no external candidates, no wirings to a running framework).
	 */
	private static final class IuResolveContext extends ResolveContext {

		private final List<Resource> mandatory;
		private final List<Capability> allPackageCapabilities = new ArrayList<>();

		IuResolveContext(List<Resource> resources) {
			this.mandatory = resources;
			for (Resource resource : resources) {
				allPackageCapabilities.addAll(resource.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE));
			}
		}

		@Override
		public Collection<Resource> getMandatoryResources() {
			return mandatory;
		}

		@Override
		public List<Capability> findProviders(Requirement requirement) {
			String filterSpec = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
			if (filterSpec == null) {
				return List.of();
			}
			try {
				Filter filter = FrameworkUtil.createFilter(filterSpec);
				List<Capability> matches = new ArrayList<>();
				for (Capability capability : allPackageCapabilities) {
					if (!requirement.getNamespace().equals(capability.getNamespace())) {
						continue;
					}
					if (filter.matches(capability.getAttributes())) {
						matches.add(capability);
					}
				}
				return matches;
			} catch (InvalidSyntaxException e) {
				throw new IllegalStateException("Invalid generated filter: " + filterSpec, e); //$NON-NLS-1$
			}
		}

		@Override
		public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability) {
			capabilities.add(hostedCapability);
			return capabilities.size() - 1;
		}

		@Override
		public boolean isEffective(Requirement requirement) {
			return true;
		}

		@Override
		public Map<Resource, Wiring> getWirings() {
			return Collections.emptyMap();
		}
	}
}
