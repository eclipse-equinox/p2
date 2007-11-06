/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.metadata.InternalInstallableUnit;
import org.eclipse.equinox.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.core.helpers.UnmodifiableProperties;
import org.osgi.framework.Version;

public class InstallableUnit implements IInstallableUnitConstants, IInstallableUnit, InternalInstallableUnit {

	private static final ProvidedCapability[] NO_PROVIDES = new ProvidedCapability[0];
	private static final RequiredCapability[] NO_REQUIRES = new RequiredCapability[0];
	private static final OrderedProperties NO_PROPERTIES = new OrderedProperties();

	private String id;
	private Version version;
	private boolean singleton;

	private OrderedProperties properties;

	private IArtifactKey[] artifacts;
	private TouchpointType touchpointType;
	private ArrayList touchpointData = null;

	private RequiredCapability[] requires;

	private String filter;

	String applicabilityFilter;

	ProvidedCapability[] providedCapabilities = NO_PROVIDES;

	public InstallableUnit() {
		super();
	}

	public InstallableUnit(String id, Version version, boolean singleton) {
		super();
		this.id = id;
		this.version = (version != null ? version : Version.emptyVersion);
		this.singleton = singleton;
	}

	public TouchpointType getTouchpointType() {
		return touchpointType != null ? touchpointType : TouchpointType.NONE;
	}

	public String getId() {
		return id;
	}

	public String getFilter() {
		return filter;
	}

	public Version getVersion() {
		return version;
	}

	public IArtifactKey[] getArtifacts() {
		return artifacts;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setVersion(Version newVersion) {
		this.version = (newVersion != null ? newVersion : Version.emptyVersion);
	}

	public void setTouchpointType(TouchpointType type) {
		this.touchpointType = (type != TouchpointType.NONE ? type : null);
	}

	public void setArtifacts(IArtifactKey[] value) {
		artifacts = value;
	}

	public RequiredCapability[] getRequiredCapabilities() {
		return requires != null ? requires : NO_REQUIRES;

	}

	public void setRequiredCapabilities(RequiredCapability[] capabilities) {
		if (capabilities == NO_REQUIRES) {
			this.requires = null;
		} else {
			//copy array for safety
			this.requires = (RequiredCapability[]) capabilities.clone();
		}
	}

	public ProvidedCapability[] getProvidedCapabilities() {
		return (providedCapabilities != null ? providedCapabilities : NO_PROVIDES);
	}

	protected void addProvidedCapability(ProvidedCapability capability) {
		if (providedCapabilities != null && providedCapabilities.length > 0) {
			ProvidedCapability[] result = new ProvidedCapability[providedCapabilities.length + 1];
			result[0] = capability;
			System.arraycopy(providedCapabilities, 0, result, 1, providedCapabilities.length);
			providedCapabilities = result;
		} else {
			providedCapabilities = new ProvidedCapability[] {capability};
		}
	}

	public void setCapabilities(ProvidedCapability[] exportedCapabilities) {
		providedCapabilities = exportedCapabilities;
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
		if (value == null)
			return (properties != null ? (String) properties.remove(key) : null);
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
		return (touchpointData == null ? TouchpointData.NO_TOUCHPOINT_DATA //
				: (TouchpointData[]) touchpointData.toArray(new TouchpointData[touchpointData.size()]));
	}

	// TODO: resolve the schizophrenia between the singleton immutable data
	//	   	 and the public returned touchpoint data array.
	public void setImmutableTouchpointData(TouchpointData immutableData) {
		ensureTouchpointDataCapacity(4);
		touchpointData.add(immutableData);
	}

	public void addTouchpointData(TouchpointData[] newData) {
		ensureTouchpointDataCapacity(newData.length);
		for (int i = 0; i < newData.length; i++) {
			touchpointData.add(newData[i]);
		}
	}

	private void ensureTouchpointDataCapacity(int size) {
		if (touchpointData != null) {
			touchpointData.ensureCapacity(size);
		} else {
			touchpointData = new ArrayList(size);
		}
	}

	private OrderedProperties properties() {
		return (properties != null ? properties : NO_PROPERTIES);
	}

	/**
	 * Get an <i>unmodifiable copy</i> of the properties
	 * associated with the installable unit.
	 * 
	 * @return an <i>unmodifiable copy</i> of the IU properties.
	 */
	public OrderedProperties getProperties() {
		return new UnmodifiableProperties(properties());
	}

	public void addProperties(OrderedProperties newProperties) {
		if (properties == null)
			properties = new OrderedProperties(newProperties.size());
		properties.putAll(newProperties);
	}

	public boolean isFragment() {
		return false;
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
