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

import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;

/**
 * @author Steffen Pingel
 */
public class Certification {

	private AbstractCatalogSource source;

	protected String id;

	protected String name;

	protected String description;

	protected Icon icon;

	protected String url;

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

	public Icon getIcon() {
		return icon;
	}

	public void setIcon(Icon icon) {
		this.icon = icon;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void validate() throws ValidationException {
		if (id == null || id.length() == 0) {
			throw new ValidationException(Messages.CatalogCategory_must_specify_CatalogCategory_id);
		}
		if (name == null || name.length() == 0) {
			throw new ValidationException(Messages.CatalogCategory_must_specify_CatalogCategory_name);
		}
		if (icon != null) {
			icon.validate();
		}
	}

}
