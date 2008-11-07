/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.generator.features;

import java.util.Map;

/**
 * A category in an update site.
 * 
 * Based on org.eclipse.update.core.model.CategoryModel.
 */
public class SiteCategory {

	private String description;
	private String label;
	private String name;
	private Map localizations;

	/**
	 * Creates an uninitialized model object.
	 * 
	 * @since 2.0
	 */
	public SiteCategory() {
		super();
	}

	/**
	 * Compare two category models for equality.
	 * 
	 * @see Object#equals(Object)
	 * @since 2.0
	 */
	public boolean equals(Object obj) {
		boolean result = false;
		if (obj instanceof SiteCategory) {
			SiteCategory otherCategory = (SiteCategory) obj;
			result = getName().equalsIgnoreCase(otherCategory.getName());
		}
		return result;
	}

	/**
	 * Retrieve the detailed category description
	 * 
	 * @return category description, or <code>null</code>.
	 * @since 2.0
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Retrieve the non-localized displayable label for the category.
	 * 
	 * @return non-localized displayable label, or <code>null</code>.
	 * @since 2.0
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Gets the localizations for the site as a map from locale
	 * to the set of translated properties for that locale.
	 * 
	 * @return a map from locale to property set
	 * @since 3.4
	 */
	public Map getLocalizations() {
		return this.localizations;
	}

	/**
	 * Retrieve the name of the category.
	 * 
	 * @return category name, or <code>null</code>.
	 * @since 2.0
	 */
	public String getName() {
		return name;
	}

	/**
	 * Compute hash code for category model.
	 * 
	 * @see Object#hashCode()
	 * @since 2.0
	 */
	public int hashCode() {
		return getName().hashCode();
	}

	/**
	 * Sets the category description.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param description category description
	 * @since 2.0
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Sets the category displayable label.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param label displayable label, or resource key
	 * @since 2.0
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Sets the localizations for the site as a map from locale
	 * to the set of translated properties for that locale.
	 * 
	 * @param localizations as a map from locale to property set
	 * @since 3.4
	 */
	public void setLocalizations(Map localizations) {
		this.localizations = localizations;
	}

	/**
	 * Sets the category name.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param name category name
	 * @since 2.0
	 */
	public void setName(String name) {
		this.name = name;
	}

}
