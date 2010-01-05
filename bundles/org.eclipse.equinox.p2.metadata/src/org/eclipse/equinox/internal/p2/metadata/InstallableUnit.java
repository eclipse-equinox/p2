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

import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.query.IQuery;

public class InstallableUnit implements IInstallableUnit {

	private static final OrderedProperties NO_PROPERTIES = new OrderedProperties();
	private static final IProvidedCapability[] NO_PROVIDES = new IProvidedCapability[0];
	private static final IRequirement[] NO_REQUIRES = new IRequirement[0];
	private static final IArtifactKey[] NO_ARTIFACTS = new IArtifactKey[0];
	private static final ITouchpointData[] NO_TOUCHPOINT_DATA = new ITouchpointData[0];
	private static final ILicense[] NO_LICENSE = new ILicense[0];

	private IArtifactKey[] artifacts = NO_ARTIFACTS;
	private LDAPQuery filter;

	private String id;

	private OrderedProperties properties;
	private OrderedProperties localizedProperties;
	IProvidedCapability[] providedCapabilities = NO_PROVIDES;
	private IRequirement[] requires = NO_REQUIRES;
	private IRequirement[] metaRequires = NO_REQUIRES;

	private boolean singleton;

	private ITouchpointData[] touchpointData = NO_TOUCHPOINT_DATA;

	private ITouchpointType touchpointType;

	private Version version = Version.emptyVersion;

	private IUpdateDescriptor updateInfo;
	private ILicense[] licenses = NO_LICENSE;
	private ICopyright copyright;

	public InstallableUnit() {
		super();
	}

	public void addTouchpointData(ITouchpointData newData) {
		int tl = touchpointData.length;
		if (tl == 0)
			touchpointData = new ITouchpointData[] {newData};
		else {
			ITouchpointData[] newDatas = new ITouchpointData[tl + 1];
			System.arraycopy(touchpointData, 0, newDatas, 0, tl);
			newDatas[tl] = newData;
			touchpointData = newDatas;
		}
	}

	public int compareTo(IInstallableUnit other) {
		int cmp = getId().compareTo(other.getId());
		if (cmp == 0)
			cmp = getVersion().compareTo(other.getVersion());
		return cmp;
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

	public Collection<IArtifactKey> getArtifacts() {
		return CollectionUtils.unmodifiableList(artifacts);
	}

	public IQuery<Boolean> getFilter() {
		return filter;
	}

	public List<IInstallableUnitFragment> getFragments() {
		return CollectionUtils.emptyList();
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
	public Map<String, String> getProperties() {
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

	public Collection<IProvidedCapability> getProvidedCapabilities() {
		return CollectionUtils.unmodifiableList(providedCapabilities);
	}

	public String getProperty(String key, String locale) {
		return TranslationSupport.getInstance().getIUProperty(this, key, locale);
	}

	public List<IRequirement> getRequiredCapabilities() {
		return CollectionUtils.unmodifiableList(requires);

	}

	public List<ITouchpointData> getTouchpointData() {
		return CollectionUtils.unmodifiableList(touchpointData);
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
		if (filter != null)
			this.filter = new LDAPQuery(filter);
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
		return localizedProperties.put(key, value);
	}

	public String setProperty(String key, String value) {
		if (value == null)
			return (properties != null ? (String) properties.remove(key) : null);
		if (properties == null)
			properties = new OrderedProperties();
		return (String) properties.setProperty(key, value);
	}

	public void setRequiredCapabilities(IRequirement[] capabilities) {
		if (capabilities.length == 0) {
			this.requires = NO_REQUIRES;
		} else {
			//copy array for safety
			this.requires = capabilities.clone();
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

	public void setLicenses(ILicense[] license) {
		this.licenses = license == null ? NO_LICENSE : license;
	}

	public List<ILicense> getLicenses() {
		return CollectionUtils.unmodifiableList(licenses);
	}

	public ILicense[] getLicenses(String locale) {
		return TranslationSupport.getInstance().getLicenses(this, locale);
	}

	public void setCopyright(ICopyright copyright) {
		this.copyright = copyright;
	}

	public ICopyright getCopyright() {
		return copyright;
	}

	public ICopyright getCopyright(String locale) {
		return TranslationSupport.getInstance().getCopyright(this, locale);
	}

	public boolean satisfies(IRequirement candidate) {
		if (candidate.getMatches() instanceof RequiredCapability) {
			for (int i = 0; i < providedCapabilities.length; i++) {
				if (((IRequiredCapability) candidate).satisfiedBy(providedCapabilities[i]))
					return true;
			}
		} else {
			throw new IllegalArgumentException();
		}
		return false;
	}

	public Collection<IRequirement> getMetaRequiredCapabilities() {
		return CollectionUtils.unmodifiableList(metaRequires);
	}

	public void setMetaRequiredCapabilities(IRequirement[] metaReqs) {
		if (metaReqs.length == 0) {
			this.metaRequires = NO_REQUIRES;
		} else {
			//copy array for safety
			this.metaRequires = metaReqs.clone();
		}
	}
}
