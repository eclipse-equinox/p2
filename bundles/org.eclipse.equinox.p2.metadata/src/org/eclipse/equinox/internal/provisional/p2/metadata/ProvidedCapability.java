/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata;

import org.eclipse.core.runtime.Assert;
import org.osgi.framework.Version;

/**
 * Describes a capability as exposed or required by an installable unit
 */
public class ProvidedCapability {
	private final String name;
	private final String namespace;
	private final Version version;

	ProvidedCapability(String namespace, String name, Version version) {
		Assert.isNotNull(namespace);
		Assert.isNotNull(name);
		this.namespace = namespace;
		this.name = name;
		this.version = version == null ? Version.emptyVersion : version;
	}

	public boolean equals(Object other) {
		if (other instanceof ProvidedCapability) {
			ProvidedCapability otherCapability = (ProvidedCapability) other;
			return otherCapability.namespace.equals(namespace) && otherCapability.name.equals(name) && otherCapability.version.equals(version);
		}
		return false;
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

	/**
	 * TODO The name of this method is backwards. A provided capability does not
	 * get satisfied - it is the required capability that is satisfied or not. It should be "satisfies"
	 * or "isSatisfied"
	 */
	public boolean isSatisfiedBy(RequiredCapability candidate) {
		if (getName() == null || !getName().equals(candidate.getName()))
			return false;
		if (getNamespace() == null || !getNamespace().equals(candidate.getNamespace()))
			return false;
		return candidate.getRange().isIncluded(version);
	}

	public String toString() {
		return namespace + '/' + name + '/' + version;
	}

}
