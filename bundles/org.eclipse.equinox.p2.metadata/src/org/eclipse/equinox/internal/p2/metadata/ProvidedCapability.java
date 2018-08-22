/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *     EclipseSource - ongoing development
 *     SAP - ongoing development
 *     Todor Boev
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
	/** Used for fast access from P2 queries to the {@link #getProperties} method */
	public static final String MEMBER_PROPERTIES = "properties"; //$NON-NLS-1$

	private final String namespace;
	private final Map<String, Object> properties;

	public ProvidedCapability(String namespace, Map<String, Object> props) {
		Assert.isNotNull(namespace, NLS.bind(Messages.provided_capability_namespace_not_defined, null));
		this.namespace = namespace;

		Assert.isNotNull(props);
		Assert.isTrue(!props.isEmpty());

		assertValidPropertyTypes(props);

		Map<String, Object> resolvedProps = new HashMap<>(props);

		// Verify the name
		Assert.isTrue(resolvedProps.containsKey(namespace) && (resolvedProps.get(namespace) instanceof String), NLS.bind(Messages.provided_capability_name_not_defined, namespace));

		// Verify the version
		Object version = resolvedProps.get(PROPERTY_VERSION);
		if (version != null) {
			Assert.isTrue(props.get(PROPERTY_VERSION) instanceof Version);
		} else {
			resolvedProps.put(PROPERTY_VERSION, Version.emptyVersion);
		}

		this.properties = Collections.unmodifiableMap(props);
	}

	public ProvidedCapability(String namespace, String name, Version version) {
		Assert.isNotNull(namespace, NLS.bind(Messages.provided_capability_namespace_not_defined, null));
		Assert.isNotNull(name, NLS.bind(Messages.provided_capability_name_not_defined, namespace));
		this.namespace = namespace;
		this.properties = new HashMap<>();
		properties.put(namespace, name);
		properties.put(PROPERTY_VERSION, version == null ? Version.emptyVersion : version);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(namespace);

		for (Entry<String, Object> attr : properties.entrySet()) {
			String key = attr.getKey();
			Object val = attr.getValue();
			String type = val.getClass().getSimpleName();

			str.append("; ").append(key).append(":").append(type).append("=").append(val); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		return str.toString();
	}

	@Override
	public int hashCode() {
		return namespace.hashCode() * properties.hashCode();
	}

	@Override
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

		if (!(properties.equals(otherCapability.getProperties()))) {
			return false;
		}

		return true;
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public String getName() {
		return (String) properties.get(namespace);
	}

	@Override
	public Version getVersion() {
		return (Version) properties.get(PROPERTY_VERSION);
	}

	@Override
	public Map<String, Object> getProperties() {
		return properties;
	}

	@Override
	public Object getMember(String memberName) {
		switch (memberName) {
			case MEMBER_NAMESPACE :
				return namespace;
			case MEMBER_NAME :
				return properties.get(namespace);
			case MEMBER_VERSION :
				return properties.get(PROPERTY_VERSION);
			case MEMBER_PROPERTIES :
				return properties;
			default :
				throw new IllegalArgumentException(String.format("No such member: %s", memberName)); //$NON-NLS-1$
		}
	}

	private void assertValidPropertyTypes(Map<String, Object> props) {
		props.forEach(this::assertValidValueType);
	}

	private void assertValidValueType(String key, Object prop) {
		if (prop instanceof List<?>) {
			int idx = 0;
			for (Object scalar : (List<?>) prop) {
				assertValidScalarType(String.format("%s[%s]", key, idx++), scalar); //$NON-NLS-1$
			}
		} else {
			assertValidScalarType(key, prop);
		}
	}

	private void assertValidScalarType(String key, Object scalar) {
		Arrays.asList(Version.class, String.class, Long.class, Integer.class, Short.class, Byte.class, Double.class, Float.class, Boolean.class, Character.class)
				.stream()
				.filter(t -> t.isAssignableFrom(scalar.getClass()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(String.format("Invalid type %s of property %s", scalar.getClass(), key))); //$NON-NLS-1$
	}
}
