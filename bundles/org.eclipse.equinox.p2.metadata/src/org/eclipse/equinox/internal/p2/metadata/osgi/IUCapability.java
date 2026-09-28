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

import static org.eclipse.equinox.internal.p2.metadata.osgi.IUResource.NAMESPACE_JAVA_PACKAGE;
import static org.eclipse.equinox.internal.p2.metadata.osgi.IUResource.NAMESPACE_OSGI_BUNDLE;
import static org.eclipse.equinox.internal.p2.metadata.osgi.IUResource.PACKAGE_ATTRIBUTE_PROPERTY_PREFIX;
import static org.eclipse.equinox.internal.p2.metadata.osgi.IUResource.PACKAGE_DIRECTIVE_PROPERTY_PREFIX;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

/**
 * Maps an {@link IProvidedCapability} of an {@link IInstallableUnit} to an OSGi
 * <a href="https://docs.osgi.org/specification/osgi.core/8.0.0/framework.resource.html">Resource
 * API Specification</a> {@link Capability}
 */
public class IUCapability implements Capability {

	private final IProvidedCapability capability;
	private final IUResource resource;
	private final Map<String, Object> attributes;
	private final Map<String, String> directives;
	private final String namespace;

	public IUCapability(IUResource resource, IProvidedCapability capability) {
		this.resource = resource;
		this.capability = capability;
		Map<String, Object> properties = capability.getProperties();
		Map<String, Object> attrs = new HashMap<>(properties);
		Map<String, String> dirs = new HashMap<>();
		String ns = capability.getNamespace();
		if (NAMESPACE_JAVA_PACKAGE.equals(ns)) {
//      <capability namespace='osgi.wiring.package'>
//        <attribute name='osgi.wiring.package' value='org.acme.pool'/>
//        <attribute name='version' type='Version' value='1.1.2'/>
//        <attribute name='bundle-version' type='Version' value='1.5.6'/>
//        <attribute name='bundle-symbolic-name' value='org.acme.pool'/>
//        <directive name='uses' value='org.acme.pool,org.acme.util'/>
//      </capability>
			namespace = PackageNamespace.PACKAGE_NAMESPACE;
			Object packageName = properties.get(NAMESPACE_JAVA_PACKAGE);
			if (packageName instanceof String) {
				attrs.put(PackageNamespace.PACKAGE_NAMESPACE, packageName);
			}
			attrs.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version(capability.getVersion().toString()));
			findBundleCapability(resource.installableUnit).ifPresent(bundle -> {
				Object bundleName = bundle.getProperties().get(NAMESPACE_OSGI_BUNDLE);
				if (bundleName instanceof String) {
					attrs.put(PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE, bundleName);
				}
				attrs.put(PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
						new Version(bundle.getVersion().toString()));
			});
			for (Entry<String, Object> entry : properties.entrySet()) {
				String key = entry.getKey();
				if (key.startsWith(PACKAGE_ATTRIBUTE_PROPERTY_PREFIX)) {
					attrs.put(key.substring(PACKAGE_ATTRIBUTE_PROPERTY_PREFIX.length()), entry.getValue());
				} else if (key.startsWith(PACKAGE_DIRECTIVE_PROPERTY_PREFIX)) {
					dirs.put(key.substring(PACKAGE_DIRECTIVE_PROPERTY_PREFIX.length()), String.valueOf(entry.getValue()));
				}
			}
		} else if (NAMESPACE_OSGI_BUNDLE.equals(ns)) {
//          <capability namespace='osgi.wiring.bundle'>
//            <attribute name='osgi.wiring.bundle' value='org.acme.pool'/>
//            <attribute name='bundle-version' type='Version' value='1.5.6'/>
//          </capability>
			namespace = BundleNamespace.BUNDLE_NAMESPACE;
			Object bundleName = properties.get(NAMESPACE_OSGI_BUNDLE);
			if (bundleName instanceof String) {
				attrs.put(BundleNamespace.BUNDLE_NAMESPACE, bundleName);
			}
			attrs.put(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
					new Version(capability.getVersion().toString()));
		} else {
			//generic namespace definition e.g.
//          <capability namespace='osgi.identity'>
//            <attribute name='osgi.identity' value='org.acme.pool'/>
//            <attribute name='version'type='Version' value='1.5.6'/>
//            <attribute name='type' value='osgi.bundle'/>
//          </capability>
			attrs.put("version", new Version(capability.getVersion().toString())); //$NON-NLS-1$
			namespace = ns;
		}
		this.attributes = Map.copyOf(attrs);
		this.directives = Map.copyOf(dirs);
	}

	private Optional<IProvidedCapability> findBundleCapability(IInstallableUnit installableUnit) {
		return installableUnit.getProvidedCapabilities().stream()
				.filter(cap -> NAMESPACE_OSGI_BUNDLE.equals(cap.getNamespace())).findFirst();
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
		return attributes;
	}

	@Override
	public Resource getResource() {
		return resource;
	}

	@Override
	public String toString() {
		return capability.toString();
	}

}
