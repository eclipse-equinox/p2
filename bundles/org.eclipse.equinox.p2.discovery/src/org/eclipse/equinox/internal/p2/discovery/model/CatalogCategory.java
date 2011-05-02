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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;

/**
 * @author David Green
 */
public class CatalogCategory extends AbstractCatalogItem {

	private AbstractCatalogSource source;

	private final List<CatalogItem> items = new ArrayList<CatalogItem>();

	protected String id;

	protected String name;

	protected String description;

	protected String relevance;

	protected Icon icon;

	protected Overview overview;

	protected java.util.List<Group> group = new java.util.ArrayList<Group>();

	public List<CatalogItem> getItems() {
		return items;
	}

	public AbstractCatalogSource getSource() {
		return source;
	}

	public void setSource(AbstractCatalogSource source) {
		this.source = source;
	}

	/**
	 * an id that uniquely identifies the category
	 */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * the name of the category, as it is displayed in the ui.
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * A description of the category
	 */
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * A relevance, which is a number from 0 to 100. Categories with higher relevance are displayed with preference in
	 * the UI.
	 */
	public String getRelevance() {
		return relevance;
	}

	public void setRelevance(String relevance) {
		this.relevance = relevance;
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

	public java.util.List<Group> getGroup() {
		return group;
	}

	public void setGroup(java.util.List<Group> group) {
		this.group = group;
	}

	public void validate() throws ValidationException {
		if (id == null || id.length() == 0) {
			throw new ValidationException(Messages.CatalogCategory_must_specify_CatalogCategory_id);
		}
		if (name == null || name.length() == 0) {
			throw new ValidationException(Messages.CatalogCategory_must_specify_CatalogCategory_name);
		}
		if (description == null || description.length() == 0) {
			throw new ValidationException("Must specify connectorCategory/@description"); //$NON-NLS-1$
		}
		if (icon != null) {
			icon.validate();
		}
		if (relevance != null) {
			try {
				int r = Integer.parseInt(relevance, 10);
				if (r < 0 || r > 100) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				throw new ValidationException(Messages.CatalogCategory_CatalogCategory_relevance_invalid);
			}
		}
		if (overview != null) {
			overview.validate();
		}
		for (Group groupItem : group) {
			groupItem.validate();
		}
	}

}
