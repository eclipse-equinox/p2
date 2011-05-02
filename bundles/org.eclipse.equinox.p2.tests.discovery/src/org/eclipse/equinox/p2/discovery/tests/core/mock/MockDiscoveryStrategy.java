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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;

/**
 * @author David Green
 */
public class MockDiscoveryStrategy extends AbstractDiscoveryStrategy {

	private int connectorCount = 15;

	private int categoryCount = 5;

	protected CatalogItemMockFactory connectorMockFactory = new CatalogItemMockFactory();

	protected CatalogCategoryMockFactory categoryMockFactory = new CatalogCategoryMockFactory();

	@Override
	public void performDiscovery(IProgressMonitor monitor) {
		for (int x = 0; x < categoryCount; ++x) {
			CatalogCategory mockCategory = createDiscoveryCategory();
			getCategories().add(mockCategory);
		}
		for (int x = 0; x < connectorCount; ++x) {
			CatalogItem mockConnector = createDiscoveryConnector();
			// put the connector in a category
			if (!getCategories().isEmpty()) {
				int categoryIndex = x % getCategories().size();
				mockConnector.setCategoryId(getCategories().get(categoryIndex).getId());
			}
			getItems().add(mockConnector);
		}
	}

	protected CatalogCategory createDiscoveryCategory() {
		return categoryMockFactory.get();
	}

	protected CatalogItem createDiscoveryConnector() {
		return connectorMockFactory.get();
	}

	public CatalogCategoryMockFactory getCategoryMockFactory() {
		return categoryMockFactory;
	}

	public void setCategoryMockFactory(CatalogCategoryMockFactory categoryMockFactory) {
		this.categoryMockFactory = categoryMockFactory;
	}

	public CatalogItemMockFactory getConnectorMockFactory() {
		return connectorMockFactory;
	}

	public void setConnectorMockFactory(CatalogItemMockFactory connectorMockFactory) {
		this.connectorMockFactory = connectorMockFactory;
	}

	public int getConnectorCount() {
		return connectorCount;
	}

	public void setConnectorCount(int connectorCount) {
		this.connectorCount = connectorCount;
	}

	public int getCategoryCount() {
		return categoryCount;
	}

	public void setCategoryCount(int categoryCount) {
		this.categoryCount = categoryCount;
	}

}
