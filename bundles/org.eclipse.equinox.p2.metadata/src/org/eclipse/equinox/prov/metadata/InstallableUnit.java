/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.prov.metadata;

import org.eclipse.equinox.internal.prov.metadata.InternalInstallableUnit;
import org.eclipse.equinox.prov.core.helpers.OrderedProperties;
import org.eclipse.equinox.prov.core.helpers.UnmodifiableProperties;
import org.osgi.framework.Version;

public class InstallableUnit implements IInstallableUnitConstants, IInstallableUnit, InternalInstallableUnit {
	private static final RequiredCapability[] NO_REQUIRES = new RequiredCapability[0];
	private static final OrderedProperties NO_PROPERTIES = new OrderedProperties();

	private String id;
	private transient Version versionObject;
	private String version;
	private boolean singleton;

	private OrderedProperties properties;

	private IArtifactKey[] artifacts;
	private TouchpointType touchpointType;
	private TouchpointData immutableTouchpointData;

	private RequiredCapability[] requires;

	private String filter;

	String applicabilityFilter;

	ProvidedCapability[] providedCapabilities = new ProvidedCapability[0];

	public InstallableUnit() {
		super();
	}

	public TouchpointType getTouchpointType() {
		return touchpointType == null ? TouchpointType.NONE : touchpointType;
	}

	public String getId() {
		return id;
	}

	public String getFilter() {
		return filter;
	}

	public Version getVersion() {
		if (versionObject == null)
			versionObject = version == null ? Version.emptyVersion : new Version(version);
		return versionObject;
	}

	public IArtifactKey[] getArtifacts() {
		return artifacts;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setVersion(Version newVersion) {
		this.versionObject = newVersion;
		this.version = newVersion == null ? null : newVersion.toString();
	}

	public void setTouchpointType(TouchpointType type) {
		this.touchpointType = type == TouchpointType.NONE ? null : type;
	}

	public void setArtifacts(IArtifactKey[] value) {
		artifacts = value;
	}

	public RequiredCapability[] getRequiredCapabilities() {
		return requires != null ? requires : NO_REQUIRES;

	}

	public ProvidedCapability[] getProvidedCapabilities() {
		ProvidedCapability self = new ProvidedCapability(IU_NAMESPACE, id, getVersion());
		if (providedCapabilities == null)
			return new ProvidedCapability[] {self};

		ProvidedCapability[] result = new ProvidedCapability[providedCapabilities.length + 1];
		result[0] = self;
		System.arraycopy(providedCapabilities, 0, result, 1, providedCapabilities.length);
		return result;
	}

	public void setRequiredCapabilities(RequiredCapability[] capabilities) {
		if (capabilities == NO_REQUIRES) {
			this.requires = null;
		} else {
			//copy array for safety
			this.requires = (RequiredCapability[]) capabilities.clone();
		}
	}

	public void accept(IMetadataVisitor visitor) {
		visitor.visitInstallableUnit(this);
	}

	public boolean isSingleton() {
		return singleton;
	}

	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	public String getProperty(String key) {
		return getProperties().getProperty(key);
	}

	public String setProperty(String key, String value) {
		if (properties == null)
			properties = new OrderedProperties();
		return (String) properties.setProperty(key, value);
	}

	public String toString() {
		return id + ' ' + getVersion();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((getVersion() == null) ? 0 : getVersion().hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof IInstallableUnit))
			return false;
		final IInstallableUnit other = (IInstallableUnit) obj;
		if (id == null) {
			if (other.getId() != null)
				return false;
		} else if (!id.equals(other.getId()))
			return false;
		if (getVersion() == null) {
			if (other.getVersion() != null)
				return false;
		} else if (!getVersion().equals(other.getVersion()))
			return false;
		return true;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public TouchpointData[] getTouchpointData() {
		return immutableTouchpointData == null ? TouchpointData.NO_TOUCHPOINT_DATA : new TouchpointData[] {immutableTouchpointData};
	}

	public void setImmutableTouchpointData(TouchpointData immutableData) {
		this.immutableTouchpointData = immutableData;
	}

	OrderedProperties getProperties() {
		return properties == null ? NO_PROPERTIES : properties;
	}

	/**
	 * Get an <i>unmodifiable copy</i> of the properties
	 * associated with the installable unit.
	 * 
	 * @return an <i>unmodifiable copy</i> of the IU properties.
	 */
	public OrderedProperties copyProperties() {
		return (properties != null ? new UnmodifiableProperties(getProperties()) : new UnmodifiableProperties(new OrderedProperties()));
	}

	public boolean isFragment() {
		return false;
	}

	public void setCapabilities(ProvidedCapability[] exportedCapabilities) {
		providedCapabilities = exportedCapabilities;
	}

	public void setApplicabilityFilter(String ldapFilter) {
		applicabilityFilter = ldapFilter;
	}

	public String getApplicabilityFilter() {
		return applicabilityFilter;
	}

	public IResolvedInstallableUnit getResolved() {
		return new ResolvedInstallableUnit(this);
	}

	public int compareTo(Object toCompareTo) {
		if (!(toCompareTo instanceof IInstallableUnit)) {
			return -1;
		}
		IInstallableUnit other = (IInstallableUnit) toCompareTo;
		if (getId().compareTo(other.getId()) == 0)
			return (getVersion().compareTo(other.getVersion()));
		return getId().compareTo(other.getId());
	}
}
