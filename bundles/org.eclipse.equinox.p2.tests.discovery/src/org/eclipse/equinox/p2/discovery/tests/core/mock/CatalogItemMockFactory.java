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

import org.eclipse.equinox.internal.p2.discovery.model.*;

/**
 * @author David Green
 */
public class CatalogItemMockFactory extends AbstractMockFactory<CatalogItem> {

	@Override
	protected void populateMockData() {

		// mock up some data

		getMockObject().setSource(source);

		name("Connector " + seed).id(CatalogItemMockFactory.class.getPackage().getName() + ".connector" + seed).siteUrl("http://example.nodomain/some/path/updateSite3.x/").tag(new Tag("", "")).license(seed % 2 == 0 ? "EPL 1.0" : "APL 2.0").description("a connector for the Example Task System versions 1.0 - 5.3").categoryId("example").provider("Testing 123 Inc."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$

		Icon icon = new Icon();
		icon.setImage128("images/ico128.png"); //$NON-NLS-1$
		icon.setImage16("images/ico16.png"); //$NON-NLS-1$
		icon.setImage32("images/ico32.png"); //$NON-NLS-1$
		icon.setImage64("images/ico64.png"); //$NON-NLS-1$

		Overview overview = new Overview();
		overview.setScreenshot("images/screenshot-main.png"); //$NON-NLS-1$
		overview.setSummary("some long text that summarizes the connector"); //$NON-NLS-1$
		overview.setUrl("http://example.nodomain/some/path/updateSite3.x/overview.html"); //$NON-NLS-1$

		icon(icon).overview(overview);
		overview.setItem(getMockObject());
	}

	@Override
	protected CatalogItem createMockObject() {
		return new CatalogItem();
	}

	public CatalogItemMockFactory categoryId(String categoryId) {
		getMockObject().setCategoryId(categoryId);
		return this;
	}

	public CatalogItemMockFactory description(String description) {
		getMockObject().setDescription(description);
		return this;
	}

	public CatalogItemMockFactory icon(Icon icon) {
		getMockObject().setIcon(icon);
		return this;
	}

	public CatalogItemMockFactory id(String id) {
		getMockObject().setId(id);
		return this;
	}

	public CatalogItemMockFactory tag(Tag tag) {
		getMockObject().addTag(tag);
		return this;
	}

	public CatalogItemMockFactory license(String license) {
		getMockObject().setLicense(license);
		return this;
	}

	public CatalogItemMockFactory name(String name) {
		getMockObject().setName(name);
		return this;
	}

	public CatalogItemMockFactory overview(Overview overview) {
		getMockObject().setOverview(overview);
		return this;
	}

	public CatalogItemMockFactory platformFilter(String platformFilter) {
		getMockObject().setPlatformFilter(platformFilter);
		return this;
	}

	public CatalogItemMockFactory provider(String provider) {
		getMockObject().setProvider(provider);
		return this;
	}

	public CatalogItemMockFactory siteUrl(String siteUrl) {
		getMockObject().setSiteUrl(siteUrl);
		return this;
	}

	public CatalogItemMockFactory featureFilter(String featureId, String versionRange) {
		FeatureFilter featureFilter = new FeatureFilter();
		featureFilter.setItem(getMockObject());
		featureFilter.setFeatureId(featureId);
		featureFilter.setVersion(versionRange);
		getMockObject().getFeatureFilter().add(featureFilter);
		return this;
	}
}
