/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.ArrayList;
import java.util.Map;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.core.Version;

public class InstallableUnit implements IInstallableUnit {

	private static final OrderedProperties NO_PROPERTIES = new OrderedProperties();
	private static final ProvidedCapability[] NO_PROVIDES = new ProvidedCapability[0];
	private static final RequiredCapability[] NO_REQUIRES = new RequiredCapability[0];
	private static final IArtifactKey[] NO_ARTIFACTS = new IArtifactKey[0];
	private static final TouchpointData[] NO_TOUCHPOINT_DATA = new TouchpointData[0];

	private IArtifactKey[] artifacts = NO_ARTIFACTS;
	private String filter;

	private String id;

	private OrderedProperties properties;
	private OrderedProperties localizedProperties;
	ProvidedCapability[] providedCapabilities = NO_PROVIDES;
	private RequiredCapability[] requires = NO_REQUIRES;

	private boolean singleton;

	private ArrayList touchpointData = null;

	private TouchpointType touchpointType;

	private Version version;

	private IUpdateDescriptor updateInfo;
	private License license;
	private Copyright copyright;

	public InstallableUnit() {
		super();
	}

	public void addTouchpointData(TouchpointData newData) {
		ensureTouchpointDataCapacity(1);
		touchpointData.add(newData);
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

	private void ensureTouchpointDataCapacity(int size) {
		if (touchpointData != null) {
			touchpointData.ensureCapacity(size);
		} else {
			touchpointData = new ArrayList(size);
		}
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

	public IArtifactKey[] getArtifacts() {
		return artifacts;
	}

	public String getFilter() {
		return filter;
	}

	public IInstallableUnitFragment[] getFragments() {
		return null;
	}

	public String getId() {
		return id;
	}

	/**
	 * Get an <i>unmodifiable copy</i> of the properties
	 * associated with the installable unit.
	 * 
	 * @return an <i>unmodifiable copy</i> of the IU properties.
	 */
	public Map getProperties() {
		return OrderedProperties.unmodifiableProperties(properties());
	}

	/*
	 * Helper method to cache localized properties
	 */
	public String getLocalizedProperty(String key) {
		String result = null;
		if (localizedProperties != null)
			result = localizedProperties.getProperty(key);
		return result;
	}

	public String getProperty(String key) {
		return properties().getProperty(key);
	}

	public ProvidedCapability[] getProvidedCapabilities() {
		return providedCapabilities;
	}

	public RequiredCapability[] getRequiredCapabilities() {
		return requires;

	}

	public TouchpointData[] getTouchpointData() {
		return (touchpointData == null ? NO_TOUCHPOINT_DATA //
				: (TouchpointData[]) touchpointData.toArray(new TouchpointData[touchpointData.size()]));
	}

	public TouchpointType getTouchpointType() {
		return touchpointType != null ? touchpointType : TouchpointType.NONE;
	}

	public Version getVersion() {
		return version;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((getVersion() == null) ? 0 : getVersion().hashCode());
		return result;
	}

	public boolean isFragment() {
		return false;
	}

	public boolean isResolved() {
		return false;
	}

	public boolean isSingleton() {
		return singleton;
	}

	private OrderedProperties properties() {
		return (properties != null ? properties : NO_PROPERTIES);
	}

	public void setArtifacts(IArtifactKey[] value) {
		if (value == null || value.length == 0)
			artifacts = NO_ARTIFACTS;
		else
			artifacts = value;
	}

	public void setCapabilities(ProvidedCapability[] newCapabilities) {
		if (newCapabilities == null || newCapabilities.length == 0)
			providedCapabilities = NO_PROVIDES;
		else
			providedCapabilities = newCapabilities;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public void setId(String id) {
		this.id = id;
	}

	/*
	 * Helper method to cache localized properties
	 */
	public String setLocalizedProperty(String key, String value) {
		if (localizedProperties == null)
			localizedProperties = new OrderedProperties();
		return (String) localizedProperties.put(key, value);
	}

	public String setProperty(String key, String value) {
		if (value == null)
			return (properties != null ? (String) properties.remove(key) : null);
		if (properties == null)
			properties = new OrderedProperties();
		return (String) properties.setProperty(key, value);
	}

	public void setRequiredCapabilities(RequiredCapability[] capabilities) {
		if (capabilities.length == 0) {
			this.requires = NO_REQUIRES;
		} else {
			//copy array for safety
			this.requires = (RequiredCapability[]) capabilities.clone();
		}
	}

	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	public void setTouchpointType(TouchpointType type) {
		this.touchpointType = (type != TouchpointType.NONE ? type : null);
	}

	public void setVersion(Version newVersion) {
		this.version = (newVersion != null ? newVersion : Version.emptyVersion);
	}

	public String toString() {
		return id + ' ' + getVersion();
	}

	public IInstallableUnit unresolved() {
		return this;
	}

	public IUpdateDescriptor getUpdateDescriptor() {
		return updateInfo;
	}

	public void setUpdateDescriptor(IUpdateDescriptor updateInfo) {
		this.updateInfo = updateInfo;
	}

	public void setLicense(License license) {
		this.license = license;
	}

	public License getLicense() {
		return license;
	}

	public void setCopyright(Copyright copyright) {
		this.copyright = copyright;
	}

	public Copyright getCopyright() {
		return copyright;
	}

	public boolean satisfies(RequiredCapability candidate) {
		ProvidedCapability[] provides = getProvidedCapabilities();
		for (int i = 0; i < provides.length; i++)
			if (provides[i].satisfies(candidate))
				return true;
		return false;
	}
}
