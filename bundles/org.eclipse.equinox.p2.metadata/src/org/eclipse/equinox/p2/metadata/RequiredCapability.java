/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import org.eclipse.core.runtime.Assert;
import org.eclipse.osgi.service.resolver.VersionRange;

/**
 * A required capability represents some external constraint on an {@link InstallableUnit}.
 * Each capability represents something an {@link InstallableUnit} needs that
 * it expects to be provided by another {@link InstallableUnit}. Capabilities are
 * entirely generic, and are intended to be capable of representing anything that
 * an {@link InstallableUnit} may need either at install time, or at runtime.
 * <p>
 * Capabilities are segmented into namespaces.  Anyone can introduce new 
 * capability namespaces. Some well-known namespaces are introduced directly
 * by the provisioning framework.
 * 
 * @see InstallableUnit#IU_KIND_NAMESPACE
 * @see InstallableUnit#IU_NAMESPACE
 */
public class RequiredCapability {

	private String filter;
	private final boolean multiple;
	private final String name;//never null
	private final String namespace;//never null
	private boolean optional;
	private final VersionRange range;//never null
	private String[] selectors;

	/**
	 * Returns a {@link RequiredCapability} on the installable unit with the given name
	 * and version range.
	 * 
	 * @param name The name of the {@link InstallableUnit} that is required.
	 * @param versionRange The range of versions that are required, or <code>null</code>
	 * to indicate that any version will do.
	 * @param optional <code>true</code> if this required capability is optional,
	 * and <code>false</code> otherwise.
	 */
	public static RequiredCapability createRequiredCapabilityForName(String name, VersionRange versionRange, boolean optional) {
		return new RequiredCapability(IInstallableUnit.IU_NAMESPACE, name, versionRange, null, optional, false);
	}

	public RequiredCapability(String namespace, String name, VersionRange range) {
		this(namespace, name, range, null, false, false);
	}

	public RequiredCapability(String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple) {
		this(namespace, name, range, null, filter, optional, multiple);
	}

	public RequiredCapability(String namespace, String name, VersionRange range, String[] selectors, String filter, boolean optional, boolean multiple) {
		Assert.isNotNull(namespace);
		Assert.isNotNull(name);
		this.namespace = namespace;
		this.name = name;
		this.range = range == null ? VersionRange.emptyRange : range;
		this.selectors = selectors == null ? new String[0] : selectors;
		this.optional = optional;
		this.filter = filter;
		this.multiple = multiple;
	}

	public void accept(IMetadataVisitor visitor) {
		visitor.visitRequiredCapability(this);
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final RequiredCapability other = (RequiredCapability) obj;
		if (filter == null) {
			if (other.filter != null)
				return false;
		} else if (!filter.equals(other.filter))
			return false;
		if (multiple != other.multiple)
			return false;
		if (!name.equals(other.name))
			return false;
		if (!namespace.equals(other.namespace))
			return false;
		if (optional != other.optional)
			return false;
		if (!range.equals(other.range))
			return false;
		return true;
	}

	public String getFilter() {
		return filter;
	}

	public String getName() {
		return name;
	}

	public String getNamespace() {
		return namespace;
	}

	/**
	 * Returns the range of versions that satisfy this required capability. Returns
	 * an empty version range ({@link VersionRange#emptyRange} if any version
	 * will satisfy the capability.
	 * @return the range of versions that satisfy this required capability.
	 */
	public VersionRange getRange() {
		return range;
	}

	/**
	 * Returns the properties to use for evaluating required capability filters 
	 * downstream from this capability. For example, if the selector "doc"
	 * is provided, then a downstream InstallableUnit with a required capability
	 * filtered with "doc=true" will be included.
	 */
	public String[] getSelectors() {
		return selectors;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result + (multiple ? 1231 : 1237);
		result = prime * result + name.hashCode();
		result = prime * result + namespace.hashCode();
		result = prime * result + (optional ? 1231 : 1237);
		result = prime * result + range.hashCode();
		return result;
	}

	public boolean isMultiple() {
		return multiple;
	}

	public boolean isOptional() {
		return optional;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public void setSelectors(String[] selectors) {
		this.selectors = selectors;
	}

	public String toString() {
		return "requiredCapability: " + namespace + '/' + name + '/' + range; //$NON-NLS-1$
	}
}
