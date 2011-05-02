/*******************************************************************************
 * Copyright (c) 2009, 2010 Task top Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery.model;

/**
 * groups provide a way to anchor connectors in a grouping with other like entries.
 * 
 * @author David Green
 */
public class Group {

	protected String id;

	protected CatalogCategory category;

	public Group() {
	}

	/**
	 * An identifier that identifies the group. Must be unique for a particular connectorCategory.
	 */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public CatalogCategory getCategory() {
		return category;
	}

	public void setCategory(CatalogCategory category) {
		this.category = category;
	}

	public void validate() throws ValidationException {
		if (id == null || id.length() == 0) {
			throw new ValidationException(Messages.Group_must_specify_group_id);
		}
	}
}
