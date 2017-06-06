/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *     SAP - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IMemberProvider;
import org.eclipse.osgi.util.NLS;

/**
 * Describes a capability as exposed or required by an installable unit
 */
public class ProvidedCapability implements IProvidedCapability, IMemberProvider {
	public static final String MEMBER_NAME = "name"; //$NON-NLS-1$
	public static final String MEMBER_VERSION = "version"; //$NON-NLS-1$
	public static final String MEMBER_NAMESPACE = "namespace"; //$NON-NLS-1$

	private final String namespace;
	private final Map<String, Object> attributes;

	public ProvidedCapability(String namespace, Map<String, Object> attrs) {
		Assert.isNotNull(namespace, NLS.bind(Messages.provided_capability_namespace_not_defined, null));
		this.namespace = namespace;

		Assert.isNotNull(attrs);
		Assert.isTrue(!attrs.isEmpty());
		Assert.isTrue(!attrs.containsKey(MEMBER_NAMESPACE));
		this.attributes = new HashMap<>(attrs);

		if (!attributes.containsKey(MEMBER_NAME)) {
			// It is common for a capability to have a main attribute under a key
			// with value the same as the capability namespace. Use as "name" if present.
			Assert.isTrue(attributes.containsKey(namespace));
			attributes.put(MEMBER_NAME, attributes.get(namespace));
		}

		Object version = attributes.get(MEMBER_VERSION);
		if (version == null) {
			attributes.put(MEMBER_VERSION, Version.emptyVersion);
		} else if (version instanceof org.osgi.framework.Version) {
			org.osgi.framework.Version osgiVer = (org.osgi.framework.Version) version;
			attributes.put(
					MEMBER_VERSION,
					Version.createOSGi(osgiVer.getMajor(), osgiVer.getMinor(), osgiVer.getMicro(), osgiVer.getQualifier()));
		} else {
			Assert.isTrue(version instanceof Version);
		}
	}

	public ProvidedCapability(String namespace, String name, Version version) {
		Assert.isNotNull(namespace, NLS.bind(Messages.provided_capability_namespace_not_defined, null));
		Assert.isNotNull(name, NLS.bind(Messages.provided_capability_name_not_defined, namespace));
		this.namespace = namespace;
		this.attributes = new HashMap<>();
		attributes.put(MEMBER_NAME, name);
		attributes.put(MEMBER_VERSION, version == null ? Version.emptyVersion : version);
	}

	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}

		if (!(other instanceof IProvidedCapability)) {
			return false;
		}

		IProvidedCapability otherCapability = (IProvidedCapability) other;

		if (!(namespace.equals(otherCapability.getNamespace()))) {
			return false;
		}

		if (!(attributes.equals(otherCapability.getAttributes()))) {
			return false;
		}

		return true;
	}

	public String getName() {
		return (String) attributes.get(MEMBER_NAME);
	}

	public String getNamespace() {
		return namespace;
	}

	public Version getVersion() {
		return (Version) attributes.get(MEMBER_VERSION);
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public int hashCode() {
		return namespace.hashCode() * attributes.hashCode();
	}

	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(namespace);

		for (Entry<String, Object> attr : attributes.entrySet()) {
			String key = attr.getKey();
			Object val = attr.getValue();
			String type = val.getClass().getSimpleName();

			str.append("; ").append(key).append(":").append(type).append("=").append(val); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		return str.toString();
	}

	public Object getMember(String memberName) {
		Object res = memberName.equals(MEMBER_NAMESPACE) ? namespace : attributes.get(memberName);
		if (res == null) {
			throw new IllegalArgumentException("No such member: " + memberName); //$NON-NLS-1$
		}
		return res;
	}
}
