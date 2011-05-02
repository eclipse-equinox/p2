/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.MalformedURLException;
import java.util.*;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;

/**
 * @author David Green
 */
public class CatalogItem extends AbstractCatalogItem {

	private AbstractCatalogSource source;

	private CatalogCategory category;

	private boolean selected;

	private Boolean available;

	private Certification certification;

	private final PropertyChangeSupport changeSupport;

	private boolean installed;

	private Set<Tag> tags;

	protected String name;

	protected String provider;

	protected String license;

	protected String description;

	protected String siteUrl;

	protected String id;

	protected String categoryId;

	protected String platformFilter;

	protected String groupId;

	protected java.util.List<FeatureFilter> featureFilter = new java.util.ArrayList<FeatureFilter>();

	protected java.util.List<String> installableUnits = new java.util.ArrayList<String>();

	protected Icon icon;

	protected Overview overview;

	protected String certificationId;

	public CatalogItem() {
		changeSupport = new PropertyChangeSupport(this);
	}

	public CatalogCategory getCategory() {
		return category;
	}

	public void setCategory(CatalogCategory category) {
		this.category = category;
	}

	public AbstractCatalogSource getSource() {
		return source;
	}

	public void setSource(AbstractCatalogSource source) {
		this.source = source;
	}

	public Certification getCertification() {
		return certification;
	}

	public void setCertification(Certification certification) {
		this.certification = certification;
	}

	/**
	 * support selection
	 * 
	 * @return true if the item is selected, otherwise false
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * support selection
	 * 
	 * @param selected
	 *            true if the item is selected, otherwise false
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * indicate if this connector is available
	 * 
	 * @return true if available, false if not, or null if availability is unknown
	 */
	public Boolean getAvailable() {
		return available;
	}

	/**
	 * indicate if this connector is available
	 * 
	 * @param available
	 *            true if available, false if not, or null if availability is unknown
	 */
	public void setAvailable(Boolean available) {
		if (available != this.available || (available != null && !available.equals(this.available))) {
			Boolean previous = this.available;
			this.available = available;
			changeSupport.firePropertyChange("available", previous, this.available); //$NON-NLS-1$
		}
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(propertyName, listener);
	}

	/**
	 * the name of the connector including the name of the organization that produces the repository if appropriate, for
	 * example 'Mozilla Bugzilla'.
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The name of the organization that supplies the connector.
	 */
	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	/**
	 * The short name of the license, for example 'EPL 1.0', 'GPL 2.0', or 'Commercial'.
	 */
	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	/**
	 * A description of the connector. Plug-ins should provide a description, especially if the description is not
	 * self-evident from the @name and
	 * 
	 * @organization.
	 */
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * The URL of the update site containing the connector.
	 */
	public String getSiteUrl() {
		return siteUrl;
	}

	public void setSiteUrl(String siteUrl) {
		this.siteUrl = siteUrl;
	}

	/**
	 * The id of the feature that installs this connector
	 */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * the id of the connectorCategory in which this connector belongs
	 */
	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public String getCertificationId() {
		return certificationId;
	}

	public void setCertificationId(String certificationId) {
		this.certificationId = certificationId;
	}

	/**
	 * E.g., "(& (osgi.os=macosx) (osgi.ws=carbon))"
	 */
	public String getPlatformFilter() {
		return platformFilter;
	}

	public void setPlatformFilter(String platformFilter) {
		this.platformFilter = platformFilter;
	}

	/**
	 * The id of the connectorCategory group. See group/@id for more details.
	 */
	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public java.util.List<FeatureFilter> getFeatureFilter() {
		return featureFilter;
	}

	public void setFeatureFilter(java.util.List<FeatureFilter> featureFilter) {
		this.featureFilter = featureFilter;
	}

	public Icon getIcon() {
		return icon;
	}

	public void setIcon(Icon icon) {
		this.icon = icon;
	}

	public Overview getOverview() {
		return overview;
	}

	public void setOverview(Overview overview) {
		this.overview = overview;
	}

	public void validate() throws ValidationException {
		if (name == null || name.length() == 0) {
			throw new ValidationException(Messages.CatalogItem_must_specify_CatalogItem_name);
		}
		if (provider == null || provider.length() == 0) {
			throw new ValidationException(Messages.CatalogItem_must_specify_CatalogItem_provider);
		}
		if (license == null || license.length() == 0) {
			throw new ValidationException(Messages.CatalogItem_must_specify_CatalogItem_license);
		}
		if (siteUrl == null || siteUrl.length() == 0) {
			throw new ValidationException(Messages.CatalogItem_must_specify_CatalogItem_siteUrl);
		}
		try {
			new java.net.URL(siteUrl);
		} catch (MalformedURLException e) {
			throw new ValidationException(Messages.CatalogItem_invalid_CatalogItem_siteUrl);
		}
		if (id == null || id.length() == 0) {
			throw new ValidationException(Messages.CatalogItem_must_specify_CatalogItem_id);
		}
		if (categoryId == null || categoryId.length() == 0) {
			throw new ValidationException(Messages.CatalogItem_must_specify_CatalogItem_categoryId);
		}
		for (FeatureFilter featureFilterItem : featureFilter) {
			featureFilterItem.validate();
		}
		if (icon != null) {
			icon.validate();
		}
		if (overview != null) {
			overview.validate();
		}
	}

	public java.util.List<String> getInstallableUnits() {
		return installableUnits;
	}

	public void setInstallableUnits(java.util.List<String> installableUnits) {
		this.installableUnits = installableUnits;
	}

	public boolean isInstalled() {
		return installed;
	}

	public void setInstalled(boolean installed) {
		this.installed = installed;
	}

	public Set<Tag> getTags() {
		if (tags == null) {
			return Collections.emptySet();
		} else {
			return Collections.unmodifiableSet(tags);
		}
	}

	public boolean hasTag(Tag tag) {
		return tags == null ? false : tags.contains(tag);
	}

	public void addTag(Tag tag) {
		if (tags == null) {
			tags = new HashSet<Tag>();
		}
		tags.add(tag);
	}

	public boolean removeTag(Tag tag) {
		if (tags == null) {
			return false;
		}
		return tags.remove(tag);
	}

}
