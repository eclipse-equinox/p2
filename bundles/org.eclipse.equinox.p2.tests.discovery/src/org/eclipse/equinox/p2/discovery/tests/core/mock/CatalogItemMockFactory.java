/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.discovery.tests.core.mock;

import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.FeatureFilter;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.discovery.model.Overview;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;

/**
 * @author David Green
 */
public class CatalogItemMockFactory extends AbstractMockFactory<CatalogItem> {

	public CatalogItemMockFactory() {
	}

	@Override
	protected void populateMockData() {

		// mock up some data

		getMockObject().setSource(source);

		name("Connector " + seed).id(CatalogItemMockFactory.class.getPackage().getName() + ".connector" + seed)
				.siteUrl("http://example.nodomain/some/path/updateSite3.x/")
				.tag(new Tag("", ""))
				.license(seed % 2 == 0 ? "EPL 1.0" : "APL 2.0")
				.description("a connector for the Example Task System versions 1.0 - 5.3")
				.categoryId("example")
				.provider("Testing 123 Inc.");

		Icon icon = new Icon();
		icon.setImage128("images/ico128.png");
		icon.setImage16("images/ico16.png");
		icon.setImage32("images/ico32.png");
		icon.setImage64("images/ico64.png");

		Overview overview = new Overview();
		overview.setScreenshot("images/screenshot-main.png");
		overview.setSummary("some long text that summarizes the connector");
		overview.setUrl("http://example.nodomain/some/path/updateSite3.x/overview.html");

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
