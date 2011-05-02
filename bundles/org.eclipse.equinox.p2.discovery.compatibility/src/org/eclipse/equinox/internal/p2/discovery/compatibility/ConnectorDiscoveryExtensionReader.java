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
package org.eclipse.equinox.internal.p2.discovery.compatibility;

import java.util.*;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.equinox.internal.p2.discovery.model.*;

/**
 * Connector Discovery extension point reader, for extension points of type
 * <tt>org.eclipse.mylyn.discovery.core.connectorDiscovery</tt>
 * 
 * @author David Green
 */
public class ConnectorDiscoveryExtensionReader {

	private static final String P2_FEATURE_GROUP_SUFFIX = ".feature.group"; //$NON-NLS-1$

	public static final String EXTENSION_POINT_ID = "org.eclipse.mylyn.discovery.core.connectorDiscovery"; //$NON-NLS-1$

	public static final String CONNECTOR_DESCRIPTOR = "connectorDescriptor"; //$NON-NLS-1$

	public static final String CONNECTOR_CATEGORY = "connectorCategory"; //$NON-NLS-1$

	public static final String CERTIFICATION = "certification"; //$NON-NLS-1$

	public static final String ICON = "icon"; //$NON-NLS-1$

	public static final String OVERVIEW = "overview"; //$NON-NLS-1$

	public static final String FEATURE_FILTER = "featureFilter"; //$NON-NLS-1$

	public static final String GROUP = "group"; //$NON-NLS-1$

	public static Tag DOCUMENT = new Tag("document", Messages.ConnectorDiscoveryExtensionReader_Documents); //$NON-NLS-1$

	public static Tag TASK = new Tag("task", Messages.ConnectorDiscoveryExtensionReader_Tasks); //$NON-NLS-1$

	public static Tag VCS = new Tag("vcs", Messages.ConnectorDiscoveryExtensionReader_Version_Control); //$NON-NLS-1$

	public static final Tag[] DEFAULT_TAGS = new Tag[] {DOCUMENT, TASK, VCS};

	private Map<String, Tag> tagById = new HashMap<String, Tag>();

	public ConnectorDiscoveryExtensionReader() {
		// constructor
	}

	/**
	 * return the enum constant whose {@link Tag#getValue() value} is the same as the given value.
	 * 
	 * @param value
	 *            the string value, or null
	 * @return the corresponding enum constant or null if the given value was null
	 * @throws IllegalArgumentException
	 *             if the given value does not correspond to any enum constant
	 */
	public static Tag fromValue(String value) throws IllegalArgumentException {
		if (value == null) {
			return null;
		}
		for (Tag tag : DEFAULT_TAGS) {
			if (tag.getValue().equals(value)) {
				return tag;
			}
		}
		throw new IllegalArgumentException(value);
	}

	public Set<Tag> getTags() {
		return new HashSet<Tag>(tagById.values());
	}

	private Tag getTag(String id) {
		if (id == null) {
			return null;
		}
		// first, look for known tag
		Tag result = tagById.get(id);
		if (result != null) {
			return result;
		}
		// second, search default tags
		for (Tag tag : DEFAULT_TAGS) {
			if (tag.getValue().equals(id)) {
				tagById.put(id, tag);
				return tag;
			}
		}
		// third, create new tag
		result = new Tag(id, id);
		tagById.put(id, result);
		return result;
	}

	public CatalogItem readConnectorDescriptor(IConfigurationElement element) throws ValidationException {
		return readConnectorDescriptor(element, CatalogItem.class);
	}

	public <T extends CatalogItem> T readConnectorDescriptor(IConfigurationElement element, Class<T> clazz) throws ValidationException {
		T connectorDescriptor;
		try {
			connectorDescriptor = clazz.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		try {
			String kinds = element.getAttribute("kind"); //$NON-NLS-1$
			if (kinds != null) {
				String[] akinds = kinds.split("\\s*,\\s*"); //$NON-NLS-1$
				for (String kind : akinds) {
					Tag tag = getTag(kind);
					if (tag != null) {
						connectorDescriptor.addTag(tag);
					}
				}
			}
		} catch (IllegalArgumentException e) {
			throw new ValidationException(Messages.ConnectorDiscoveryExtensionReader_unexpected_value_kind);
		}
		connectorDescriptor.setName(element.getAttribute("name")); //$NON-NLS-1$
		connectorDescriptor.setProvider(element.getAttribute("provider")); //$NON-NLS-1$
		connectorDescriptor.setLicense(element.getAttribute("license")); //$NON-NLS-1$
		connectorDescriptor.setDescription(element.getAttribute("description")); //$NON-NLS-1$
		connectorDescriptor.setSiteUrl(element.getAttribute("siteUrl")); //$NON-NLS-1$
		connectorDescriptor.setId(element.getAttribute("id")); //$NON-NLS-1$
		connectorDescriptor.setCategoryId(element.getAttribute("categoryId")); //$NON-NLS-1$
		connectorDescriptor.setCertificationId(element.getAttribute("certificationId")); //$NON-NLS-1$
		connectorDescriptor.setPlatformFilter(element.getAttribute("platformFilter")); //$NON-NLS-1$
		connectorDescriptor.setGroupId(element.getAttribute("groupId")); //$NON-NLS-1$

		IConfigurationElement[] children = element.getChildren("iu"); //$NON-NLS-1$
		if (children.length > 0) {
			for (IConfigurationElement child : children) {
				connectorDescriptor.getInstallableUnits().add(getFeatureId(child.getAttribute("id"))); //$NON-NLS-1$
			}
		} else {
			// no particular iu specified, use connector id
			connectorDescriptor.getInstallableUnits().add(getFeatureId(connectorDescriptor.getId()));
		}
		for (IConfigurationElement child : element.getChildren("featureFilter")) { //$NON-NLS-1$
			FeatureFilter featureFilterItem = readFeatureFilter(child);
			featureFilterItem.setItem(connectorDescriptor);
			connectorDescriptor.getFeatureFilter().add(featureFilterItem);
		}
		for (IConfigurationElement child : element.getChildren("icon")) { //$NON-NLS-1$
			Icon iconItem = readIcon(child);
			if (connectorDescriptor.getIcon() != null) {
				throw new ValidationException(Messages.ConnectorDiscoveryExtensionReader_unexpected_element_icon);
			}
			connectorDescriptor.setIcon(iconItem);
		}
		for (IConfigurationElement child : element.getChildren("overview")) { //$NON-NLS-1$
			Overview overviewItem = readOverview(child);
			overviewItem.setItem(connectorDescriptor);
			if (connectorDescriptor.getOverview() != null) {
				throw new ValidationException(Messages.ConnectorDiscoveryExtensionReader_unexpected_element_overview);
			}
			connectorDescriptor.setOverview(overviewItem);
		}

		connectorDescriptor.validate();

		return connectorDescriptor;
	}

	private String getFeatureId(String id) {
		if (!id.endsWith(P2_FEATURE_GROUP_SUFFIX)) {
			return id + P2_FEATURE_GROUP_SUFFIX;
		}
		return id;
	}

	public CatalogCategory readConnectorCategory(IConfigurationElement element) throws ValidationException {
		return readConnectorCategory(element, CatalogCategory.class);
	}

	public <T extends CatalogCategory> T readConnectorCategory(IConfigurationElement element, Class<T> clazz) throws ValidationException {
		T connectorCategory;
		try {
			connectorCategory = clazz.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		connectorCategory.setId(element.getAttribute("id")); //$NON-NLS-1$
		connectorCategory.setName(element.getAttribute("name")); //$NON-NLS-1$
		connectorCategory.setDescription(element.getAttribute("description")); //$NON-NLS-1$
		connectorCategory.setRelevance(element.getAttribute("relevance")); //$NON-NLS-1$

		for (IConfigurationElement child : element.getChildren("icon")) { //$NON-NLS-1$
			Icon iconItem = readIcon(child);
			if (connectorCategory.getIcon() != null) {
				throw new ValidationException(Messages.ConnectorDiscoveryExtensionReader_unexpected_element_icon);
			}
			connectorCategory.setIcon(iconItem);
		}
		for (IConfigurationElement child : element.getChildren("overview")) { //$NON-NLS-1$
			Overview overviewItem = readOverview(child);
			overviewItem.setCategory(connectorCategory);
			if (connectorCategory.getOverview() != null) {
				throw new ValidationException(Messages.ConnectorDiscoveryExtensionReader_unexpected_element_overview);
			}
			connectorCategory.setOverview(overviewItem);
		}
		for (IConfigurationElement child : element.getChildren("group")) { //$NON-NLS-1$
			Group groupItem = readGroup(child);
			groupItem.setCategory(connectorCategory);
			connectorCategory.getGroup().add(groupItem);
		}

		connectorCategory.validate();

		return connectorCategory;
	}

	public <T extends Certification> T readCertification(IConfigurationElement element, Class<T> clazz) throws ValidationException {
		T certification;
		try {
			certification = clazz.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		certification.setId(element.getAttribute("id")); //$NON-NLS-1$
		certification.setName(element.getAttribute("name")); //$NON-NLS-1$
		certification.setUrl(element.getAttribute("url")); //$NON-NLS-1$

		for (IConfigurationElement child : element.getChildren("icon")) { //$NON-NLS-1$
			Icon iconItem = readIcon(child);
			if (certification.getIcon() != null) {
				throw new ValidationException(Messages.ConnectorDiscoveryExtensionReader_unexpected_element_icon);
			}
			certification.setIcon(iconItem);
		}
		for (IConfigurationElement child : element.getChildren("description")) { //$NON-NLS-1$
			certification.setDescription(child.getValue());
		}
		certification.validate();

		return certification;
	}

	public Icon readIcon(IConfigurationElement element) throws ValidationException {
		Icon icon = new Icon();

		icon.setImage16(element.getAttribute("image16")); //$NON-NLS-1$
		icon.setImage32(element.getAttribute("image32")); //$NON-NLS-1$
		icon.setImage48(element.getAttribute("image48")); //$NON-NLS-1$
		icon.setImage64(element.getAttribute("image64")); //$NON-NLS-1$
		icon.setImage128(element.getAttribute("image128")); //$NON-NLS-1$

		icon.validate();

		return icon;
	}

	public Overview readOverview(IConfigurationElement element) throws ValidationException {
		Overview overview = new Overview();

		overview.setSummary(element.getAttribute("summary")); //$NON-NLS-1$
		overview.setUrl(element.getAttribute("url")); //$NON-NLS-1$
		overview.setScreenshot(element.getAttribute("screenshot")); //$NON-NLS-1$

		overview.validate();

		return overview;
	}

	public FeatureFilter readFeatureFilter(IConfigurationElement element) throws ValidationException {
		FeatureFilter featureFilter = new FeatureFilter();

		featureFilter.setFeatureId(element.getAttribute("featureId")); //$NON-NLS-1$
		featureFilter.setVersion(element.getAttribute("version")); //$NON-NLS-1$

		featureFilter.validate();

		return featureFilter;
	}

	public Group readGroup(IConfigurationElement element) throws ValidationException {
		Group group = new Group();

		group.setId(element.getAttribute("id")); //$NON-NLS-1$

		group.validate();

		return group;
	}

}
