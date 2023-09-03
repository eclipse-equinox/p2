/*******************************************************************************
 *  Copyright (c) 2007, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.ICopyright;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.expression.IExpressionFactory;
import org.eclipse.equinox.p2.metadata.expression.IFilterExpression;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.metadata.expression.IMemberProvider;

public class InstallableUnit implements IInstallableUnit, IMemberProvider {
	@SuppressWarnings("serial")
	private static final Map<IFilterExpression, IMatchExpression<IInstallableUnit>> filterCache = new LinkedHashMap<>() {
		@Override
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
		filterWrap = factory.matches(factory.member(factory.thisVariable(), MEMBER_PROPERTIES),
				factory.indexedParameter(0));
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
		if (tl == 0) {
			touchpointData = new ITouchpointData[] { newData };
		} else {
			ITouchpointData[] newDatas = new ITouchpointData[tl + 1];
			System.arraycopy(touchpointData, 0, newDatas, 0, tl);
			newDatas[tl] = newData;
			touchpointData = newDatas;
		}
	}

	static final Comparator<IInstallableUnit> ID_FIRST_THEN_VERSION = Comparator //
			.comparing(IInstallableUnit::getId) //
			.thenComparing(IInstallableUnit::getVersion);

	@Override
	public int compareTo(IInstallableUnit other) {
		return ID_FIRST_THEN_VERSION.compare(this, other);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return obj instanceof IInstallableUnit unit //
				&& Objects.equals(getId(), unit.getId()) //
				&& Objects.equals(getVersion(), unit.getVersion());
	}

	@Override
	public Collection<IArtifactKey> getArtifacts() {
		return CollectionUtils.unmodifiableList(artifacts);
	}

	@Override
	public IMatchExpression<IInstallableUnit> getFilter() {
		return filter;
	}

	@Override
	public Collection<IInstallableUnitFragment> getFragments() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return id;
	}

	/**
	 * Get an <i>unmodifiable copy</i> of the properties associated with the
	 * installable unit.
	 * 
	 * @return an <i>unmodifiable copy</i> of the IU properties.
	 */
	@Override
	public Map<String, String> getProperties() {
		return OrderedProperties.unmodifiableProperties(properties());
	}

	/*
	 * Helper method to cache localized properties
	 */
	public String getLocalizedProperty(String key) {
		return localizedProperties != null ? localizedProperties.getProperty(key) : null;
	}

	@Override
	public String getProperty(String key) {
		return properties().getProperty(key);
	}

	@Override
	public Collection<IProvidedCapability> getProvidedCapabilities() {
		return CollectionUtils.unmodifiableList(providedCapabilities);
	}

	@Override
	public String getProperty(String key, String locale) {
		return TranslationSupport.getInstance().getIUProperty(this, key, locale);
	}

	@Override
	public List<IRequirement> getRequirements() {
		return CollectionUtils.unmodifiableList(requires);

	}

	@Override
	public Collection<ITouchpointData> getTouchpointData() {
		return CollectionUtils.unmodifiableList(touchpointData);
	}

	@Override
	public ITouchpointType getTouchpointType() {
		return touchpointType != null ? touchpointType : ITouchpointType.NONE;
	}

	@Override
	public Version getVersion() {
		return version;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, getVersion());
	}

	@Override
	public boolean isResolved() {
		return false;
	}

	@Override
	public boolean isSingleton() {
		return singleton;
	}

	private OrderedProperties properties() {
		return properties != null ? properties : NO_PROPERTIES;
	}

	public void setArtifacts(IArtifactKey[] value) {
		artifacts = value == null || value.length == 0 ? NO_ARTIFACTS : value;
	}

	public void setCapabilities(IProvidedCapability[] newCapabilities) {
		providedCapabilities = newCapabilities == null || newCapabilities.length == 0 ? NO_PROVIDES : newCapabilities;
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
		if (filter == null) {
			return null;
		}
		synchronized (filterCache) {
			IMatchExpression<IInstallableUnit> matchExpr = filterCache.get(filter);
			if (matchExpr != null) {
				return matchExpr;
			}
			matchExpr = ExpressionUtil.getFactory().matchExpression(filterWrap, filter);
			filterCache.put(filter, matchExpr);
			return matchExpr;
		}
	}

	/*
	 * Helper method to cache localized properties
	 */
	public String setLocalizedProperty(String key, String value) {
		if (localizedProperties == null) {
			localizedProperties = new OrderedProperties();
		}
		return localizedProperties.put(key, value);
	}

	public String setProperty(String key, String value) {
		if (value == null) {
			return (properties != null ? (String) properties.remove(key) : null);
		}
		if (properties == null) {
			properties = new OrderedProperties();
		}
		return (String) properties.setProperty(key, value);
	}

	public void setRequiredCapabilities(IRequirement[] capabilities) {
		if (capabilities.length == 0) {
			this.requires = NO_REQUIRES;
		} else {
			// copy array for safety
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

	@Override
	public String toString() {
		return id + ' ' + getVersion();
	}

	@Override
	public IInstallableUnit unresolved() {
		return this;
	}

	@Override
	public IUpdateDescriptor getUpdateDescriptor() {
		return updateInfo;
	}

	public void setUpdateDescriptor(IUpdateDescriptor updateInfo) {
		this.updateInfo = updateInfo;
	}

	public void setLicenses(ILicense[] license) {
		this.licenses = license == null ? NO_LICENSE : license;
	}

	@Override
	public Collection<ILicense> getLicenses() {
		return CollectionUtils.unmodifiableList(licenses);
	}

	@Override
	public Collection<ILicense> getLicenses(String locale) {
		return CollectionUtils.unmodifiableList(TranslationSupport.getInstance().getLicenses(this, locale));
	}

	public void setCopyright(ICopyright copyright) {
		this.copyright = copyright;
	}

	@Override
	public ICopyright getCopyright() {
		return copyright;
	}

	@Override
	public ICopyright getCopyright(String locale) {
		return TranslationSupport.getInstance().getCopyright(this, locale);
	}

	@Override
	public boolean satisfies(IRequirement candidate) {
		return candidate.isMatch(this);
	}

	@Override
	public Collection<IRequirement> getMetaRequirements() {
		return CollectionUtils.unmodifiableList(metaRequires);
	}

	public void setMetaRequiredCapabilities(IRequirement[] metaReqs) {
		if (metaReqs.length == 0) {
			this.metaRequires = NO_REQUIRES;
		} else {
			// copy array for safety
			this.metaRequires = metaReqs.clone();
		}
	}

	@Override
	public Object getMember(String memberName) {
		return switch (memberName) {
		case MEMBER_PROVIDED_CAPABILITIES -> providedCapabilities;
		case MEMBER_ID -> id;
		case MEMBER_VERSION -> version;
		case MEMBER_PROPERTIES -> properties;
		case MEMBER_FILTER -> filter;
		case MEMBER_ARTIFACTS -> artifacts;
		case MEMBER_REQUIREMENTS -> requires;
		case MEMBER_LICENSES -> licenses;
		case MEMBER_COPYRIGHT -> copyright;
		case MEMBER_TOUCHPOINT_DATA -> touchpointData;
		case MEMBER_TOUCHPOINT_TYPE -> touchpointType;
		case MEMBER_UPDATE_DESCRIPTOR -> updateInfo;
		case MEMBER_SINGLETON -> singleton;
		default -> throw new IllegalArgumentException("No such member: " + memberName); //$NON-NLS-1$
		};
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
		environment.forEach(ctxIU::setProperty);
		return ctxIU;
	}
}
