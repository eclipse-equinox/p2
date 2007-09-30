/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import org.osgi.framework.Version;

/**
 * Describes a capability as exposed or required by an installable unit
 */
public class ProvidedCapability {
	String namespace;
	String name;
	transient Version versionObject;
	String version;

	public String getNamespace() {
		return namespace;
	}

	public String getName() {
		return name;
	}

	public Version getVersion() {
		if (versionObject == null)
			versionObject = version == null ? Version.emptyVersion : new Version(version);
		return versionObject;
	}

	public ProvidedCapability(String namespace, String name, Version newVersion) {
		this.name = name;
		this.versionObject = newVersion;
		this.version = newVersion == null ? null : newVersion.toString();
		this.namespace = namespace;
	}

	public boolean isSatisfiedBy(RequiredCapability candidate) {
		if (getName() == null || !getName().equals(candidate.getName()))
			return false;
		if (getNamespace() == null || !getNamespace().equals(candidate.getNamespace()))
			return false;
		return candidate.getRange().isIncluded(getVersion());
	}

	public void accept(IMetadataVisitor visitor) {
		visitor.visitCapability(this);
	}

	public boolean equals(Object other) {
		if (other instanceof ProvidedCapability) {
			ProvidedCapability otherCapability = (ProvidedCapability) other;
			return otherCapability.namespace.equals(namespace) && otherCapability.name.equals(name) && otherCapability.getVersion().equals(getVersion());
		}
		return false;
	}

	public int hashCode() {
		return namespace.hashCode() * name.hashCode() * getVersion().hashCode();
	}

	public String toString() {
		return namespace + '/' + name + '/' + getVersion();
	}

}
