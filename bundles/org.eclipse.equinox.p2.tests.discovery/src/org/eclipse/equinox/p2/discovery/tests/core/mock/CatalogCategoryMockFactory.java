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
package org.eclipse.equinox.p2.discovery.tests.core.mock;

import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;

/**
 * @author David Green
 */
public class CatalogCategoryMockFactory extends AbstractMockFactory<CatalogCategory> {

	CatalogCategory category;

	@Override
	protected CatalogCategory createMockObject() {
		return new CatalogCategory();
	}

	@Override
	protected void populateMockData() {
		// mock up some data

		getMockObject().setSource(source);

		name("Category " + seed).id(CatalogCategoryMockFactory.class.getPackage().getName() + ".connector" + seed).description("A category of things, " + seed); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		Icon icon = new Icon();
		icon.setImage128("images/ico128.png"); //$NON-NLS-1$
		icon.setImage16("images/ico16.png"); //$NON-NLS-1$
		icon.setImage32("images/ico32.png"); //$NON-NLS-1$
		icon.setImage64("images/ico64.png"); //$NON-NLS-1$

		getMockObject().setIcon(icon);
	}

	public CatalogCategoryMockFactory description(String description) {
		getMockObject().setDescription(description);
		return this;
	}

	public CatalogCategoryMockFactory icon(Icon icon) {
		getMockObject().setIcon(icon);
		return this;
	}

	public CatalogCategoryMockFactory id(String id) {
		getMockObject().setId(id);
		return this;
	}

	public CatalogCategoryMockFactory name(String name) {
		getMockObject().setName(name);
		return this;
	}

}
