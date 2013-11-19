/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
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
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.*;

public class InstallableUnit implements IInstallableUnit, IMemberProvider {
	@SuppressWarnings("serial")
	private static final Map<IFilterExpression, IMatchExpression<IInstallableUnit>> filterCache = new LinkedHashMap<IFilterExpression, IMatchExpression<IInstallableUnit>>() {
		public boolean removeEldestEntry(Map.Entry<IFilterExpression, IMatchExpression<IInstallableUnit>> expr) {
			return size() > 64;
		}
	};

	private static final OrderedProperties NO_PROPERTIES = new OrderedProperties();
	private static final IProvidedCapability[] NO_PROVIDES = new IProvidedCapability[0];
	private static final IRequirement[] NO_REQUIRES = new IRequirement[0];
	private static final IArtifactKey[] NO_ARTIFACTS = new IArtifactKey[0];
	private static final ITouchpointData[] NO_TOUCHPOINT_DATA = new ITouchpointData[0];
	private static final ILicense[] NO_LICENSE = new ILicense[0];

	private static final IExpression filterWrap;

	public static final String MEMBER_PROVIDED_CAPABILITIES = "providedCapabilities"; //$NON-NLS-1$
	public static final String MEMBER_ID = "id"; //$NON-NLS-1$
	public static final String MEMBER_VERSION = "version"; //$NON-NLS-1$
	public static final String MEMBER_PROPERTIES = "properties"; //$NON-NLS-1$
	public static final String MEMBER_FILTER = "filter"; //$NON-NLS-1$
	public static final String MEMBER_ARTIFACTS = "artifacts"; //$NON-NLS-1$
	public static final String MEMBER_REQUIREMENTS = "requirements"; //$NON-NLS-1$
	public static final String MEMBER_LICENSES = "licenses"; //$NON-NLS-1$
	public static final String MEMBER_COPYRIGHT = "copyright"; //$NON-NLS-1$
	public static final String MEMBER_TOUCHPOINT_DATA = "touchpointData"; //$NON-NLS-1$
	public static final String MEMBER_TOUCHPOINT_TYPE = "touchpointType"; //$NON-NLS-1$
	public static final String MEMBER_UPDATE_DESCRIPTOR = "updateDescriptor"; //$NON-NLS-1$
	public static final String MEMBER_SINGLETON = "singleton"; //$NON-NLS-1$

	static {
		IExpressionFactory factory = ExpressionUtil.getFactory();
		filterWrap = factory.matches(factory.member(factory.thisVariable(), MEMBER_PROPERTIES), factory.indexedParameter(0));
	}

	private IArtifactKey[] artifacts = NO_ARTIFACTS;
	private IMatchExpression<IInstallableUnit> filter;

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
	public static final String MEMBER_TRANSLATED_PROPERTIES = "translatedProperties"; //$NON-NLS-1$
	public static final String MEMBER_PROFILE_PROPERTIES = "profileProperties"; //$NON-NLS-1$

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

	public IMatchExpression<IInstallableUnit> getFilter() {
		return filter;
	}

	public Collection<IInstallableUnitFragment> getFragments() {
		return Collections.<IInstallableUnitFragment> emptyList();
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

	public List<IRequirement> getRequirements() {
		return CollectionUtils.unmodifiableList(requires);

	}

	public Collection<ITouchpointData> getTouchpointData() {
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

	public void setFilter(IMatchExpression<IInstallableUnit> filter) {
		this.filter = filter;
	}

	public void setFilter(String filter) {
		setFilter(parseFilter(filter));
	}

	public void setId(String id) {
		this.id = id;
	}

	public static IMatchExpression<IInstallableUnit> parseFilter(String filterStr) {
		IFilterExpression filter = ExpressionUtil.parseLDAP(filterStr);
		if (filter == null)
			return null;

		synchronized (filterCache) {
			IMatchExpression<IInstallableUnit> matchExpr = filterCache.get(filter);
			if (matchExpr != null)
				return matchExpr;

			matchExpr = ExpressionUtil.getFactory().<IInstallableUnit> matchExpression(filterWrap, filter);
			filterCache.put(filter, matchExpr);
			return matchExpr;
		}
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

	public Collection<ILicense> getLicenses() {
		return CollectionUtils.unmodifiableList(licenses);
	}

	public Collection<ILicense> getLicenses(String locale) {
		return CollectionUtils.unmodifiableList(TranslationSupport.getInstance().getLicenses(this, locale));
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
		return candidate.isMatch(this);
	}

	public Collection<IRequirement> getMetaRequirements() {
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

	public Object getMember(String memberName) {
		// It is OK to use identity comparisons here since
		// a) All constant valued strings are always interned
		// b) The Member constructor always interns the name
		//
		if (MEMBER_PROVIDED_CAPABILITIES == memberName)
			return providedCapabilities;
		if (MEMBER_ID == memberName)
			return id;
		if (MEMBER_VERSION == memberName)
			return version;
		if (MEMBER_PROPERTIES == memberName)
			return properties;
		if (MEMBER_FILTER == memberName)
			return filter;
		if (MEMBER_ARTIFACTS == memberName)
			return artifacts;
		if (MEMBER_REQUIREMENTS == memberName)
			return requires;
		if (MEMBER_LICENSES == memberName)
			return licenses;
		if (MEMBER_COPYRIGHT == memberName)
			return copyright;
		if (MEMBER_TOUCHPOINT_DATA == memberName)
			return touchpointData;
		if (MEMBER_TOUCHPOINT_TYPE == memberName)
			return touchpointType;
		if (MEMBER_UPDATE_DESCRIPTOR == memberName)
			return updateInfo;
		if (MEMBER_SINGLETON == memberName)
			return Boolean.valueOf(singleton);
		throw new IllegalArgumentException("No such member: " + memberName); //$NON-NLS-1$
	}

	public static IInstallableUnit contextIU(String ws, String os, String arch) {
		InstallableUnit ctxIU = new InstallableUnit();
		ctxIU.setId("org.eclipse.equinox.p2.context.iu"); //$NON-NLS-1$
		ctxIU.setProperty("osgi.ws", ws); //$NON-NLS-1$
		ctxIU.setProperty("osgi.os", os); //$NON-NLS-1$
		ctxIU.setProperty("osgi.arch", arch); //$NON-NLS-1$
		return ctxIU;
	}

	public static IInstallableUnit contextIU(Map<String, String> environment) {
		InstallableUnit ctxIU = new InstallableUnit();
		ctxIU.setId("org.eclipse.equinox.p2.context.iu"); //$NON-NLS-1$
		for (Map.Entry<String, String> entry : environment.entrySet())
			ctxIU.setProperty(entry.getKey(), entry.getValue());
		return ctxIU;
	}
}
