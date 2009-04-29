/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.ArrayList;
import java.util.Map;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;

public class InstallableUnit implements IInstallableUnit {

	private static final OrderedProperties NO_PROPERTIES = new OrderedProperties();
	private static final IProvidedCapability[] NO_PROVIDES = new IProvidedCapability[0];
	private static final IRequiredCapability[] NO_REQUIRES = new IRequiredCapability[0];
	private static final IArtifactKey[] NO_ARTIFACTS = new IArtifactKey[0];
	private static final ITouchpointData[] NO_TOUCHPOINT_DATA = new ITouchpointData[0];

	private IArtifactKey[] artifacts = NO_ARTIFACTS;
	private String filter;

	private String id;

	private OrderedProperties properties;
	private OrderedProperties localizedProperties;
	IProvidedCapability[] providedCapabilities = NO_PROVIDES;
	private IRequiredCapability[] requires = NO_REQUIRES;
	private IRequiredCapability[] metaRequires = NO_REQUIRES;

	private boolean singleton;

	private ArrayList touchpointData = null;

	private ITouchpointType touchpointType;

	private Version version;

	private IUpdateDescriptor updateInfo;
	private ILicense license;
	private ICopyright copyright;

	public InstallableUnit() {
		super();
	}

	public void addTouchpointData(ITouchpointData newData) {
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

	public IProvidedCapability[] getProvidedCapabilities() {
		return providedCapabilities;
	}

	public IRequiredCapability[] getRequiredCapabilities() {
		return requires;

	}

	public ITouchpointData[] getTouchpointData() {
		return (touchpointData == null ? NO_TOUCHPOINT_DATA //
				: (ITouchpointData[]) touchpointData.toArray(new ITouchpointData[touchpointData.size()]));
	}

	public ITouchpointType getTouchpointType() {
		return touchpointType != null ? touchpointType : ITouchpointType.NONE;
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

	public void setCapabilities(IProvidedCapability[] newCapabilities) {
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

	public void setRequiredCapabilities(IRequiredCapability[] capabilities) {
		if (capabilities.length == 0) {
			this.requires = NO_REQUIRES;
		} else {
			//copy array for safety
			this.requires = (IRequiredCapability[]) capabilities.clone();
		}
	}

	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	public void setTouchpointType(ITouchpointType type) {
		this.touchpointType = (type != ITouchpointType.NONE ? type : null);
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

	public void setLicense(ILicense license) {
		this.license = license;
	}

	public ILicense getLicense() {
		return license;
	}

	public void setCopyright(ICopyright copyright) {
		this.copyright = copyright;
	}

	public ICopyright getCopyright() {
		return copyright;
	}

	public boolean satisfies(IRequiredCapability candidate) {
		IProvidedCapability[] provides = getProvidedCapabilities();
		for (int i = 0; i < provides.length; i++)
			if (provides[i].satisfies(candidate))
				return true;
		return false;
	}

	public IRequiredCapability[] getMetaRequiredCapabilities() {
		return metaRequires;
	}

	public void setMetaRequiredCapabilities(IRequiredCapability[] metaReqs) {
		if (metaReqs.length == 0) {
			this.metaRequires = NO_REQUIRES;
		} else {
			//copy array for safety
			this.metaRequires = (IRequiredCapability[]) metaReqs.clone();
		}
	}
}
