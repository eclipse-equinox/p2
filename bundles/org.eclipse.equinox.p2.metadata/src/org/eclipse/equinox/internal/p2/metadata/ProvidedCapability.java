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
	/** Used for fast access from P2 queries to the {@link #getNamespace} method */
	public static final String MEMBER_NAMESPACE = "namespace"; //$NON-NLS-1$
	/** Used for fast access from P2 queries to the {@link #getName} method */
	public static final String MEMBER_NAME = "name"; //$NON-NLS-1$
	/** Used for fast access from P2 queries to the {@link #getVersion} method */
	public static final String MEMBER_VERSION = "version"; //$NON-NLS-1$
	/** Used for fast access from P2 queries to the {@link #getAttributes} method */
	public static final String MEMBER_ATTRIBUTES = "attributes"; //$NON-NLS-1$

	// TODO Move this to IProvidedCapability?
	// The "version" attribute is part of the public contract of getVersion() and getAttributes()
	public static final String ATTRIBUTE_VERSION = "version"; //$NON-NLS-1$

	private final String namespace;
	private final Map<String, Object> attributes;

	public ProvidedCapability(String namespace, Map<String, Object> attrs) {
		Assert.isNotNull(namespace, NLS.bind(Messages.provided_capability_namespace_not_defined, null));
		this.namespace = namespace;

		Assert.isNotNull(attrs);
		Assert.isTrue(!attrs.isEmpty());

		this.attributes = new HashMap<>(attrs);

		// Verify the name
		Assert.isTrue(attributes.containsKey(namespace) && (attributes.get(namespace) instanceof String),
				NLS.bind(Messages.provided_capability_name_not_defined, namespace));

		// Verify the version
		Object version = attributes.get(ATTRIBUTE_VERSION);
		if (version != null) {
			Assert.isTrue(attributes.get(ATTRIBUTE_VERSION) instanceof Version);
		} else {
			attributes.put(ATTRIBUTE_VERSION, Version.emptyVersion);
		}
	}

	public ProvidedCapability(String namespace, String name, Version version) {
		Assert.isNotNull(namespace, NLS.bind(Messages.provided_capability_namespace_not_defined, null));
		Assert.isNotNull(name, NLS.bind(Messages.provided_capability_name_not_defined, namespace));
		this.namespace = namespace;
		this.attributes = new HashMap<>();
		attributes.put(namespace, name);
		attributes.put(ATTRIBUTE_VERSION, version == null ? Version.emptyVersion : version);
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

	public String getNamespace() {
		return namespace;
	}

	public String getName() {
		return (String) attributes.get(namespace);
	}

	public Version getVersion() {
		return (Version) attributes.get(ATTRIBUTE_VERSION);
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

	@Override
	public Object getMember(String memberName) {
		switch (memberName) {
			case MEMBER_NAMESPACE :
				return namespace;
			case MEMBER_NAME :
				return attributes.get(namespace);
			case MEMBER_VERSION :
				return attributes.get(ATTRIBUTE_VERSION);
			case MEMBER_ATTRIBUTES :
				return attributes;
			default :
				throw new IllegalArgumentException("No such member: " + memberName); //$NON-NLS-1$
		}
	}
}
