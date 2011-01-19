/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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

	private final String name;
	private final String namespace;
	private final Version version;

	public ProvidedCapability(String namespace, String name, Version version) {
		Assert.isNotNull(namespace, NLS.bind(Messages.provided_capability_namespace_not_defined, null));
		Assert.isNotNull(name, NLS.bind(Messages.provided_capability_name_not_defined, namespace));
		this.namespace = namespace;
		this.name = name;
		this.version = version == null ? Version.emptyVersion : version;
	}

	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof IProvidedCapability))
			return false;
		IProvidedCapability otherCapability = (IProvidedCapability) other;
		if (!(namespace.equals(otherCapability.getNamespace())))
			return false;
		if (!(name.equals(otherCapability.getName())))
			return false;
		return version.equals(otherCapability.getVersion());
	}

	public String getName() {
		return name;
	}

	public String getNamespace() {
		return namespace;
	}

	public Version getVersion() {
		return version;
	}

	public int hashCode() {
		return namespace.hashCode() * name.hashCode() * version.hashCode();
	}

	public String toString() {
		return namespace + '/' + name + '/' + version;
	}

	public Object getMember(String memberName) {
		// It is OK to use identity comparisons here since
		// a) All constant valued strings are always interned
		// b) The Member constructor always interns the name
		//
		if (MEMBER_NAME == memberName)
			return name;
		if (MEMBER_VERSION == memberName)
			return version;
		if (MEMBER_NAMESPACE == memberName)
			return namespace;
		throw new IllegalArgumentException("No such member: " + memberName); //$NON-NLS-1$
	}
}
