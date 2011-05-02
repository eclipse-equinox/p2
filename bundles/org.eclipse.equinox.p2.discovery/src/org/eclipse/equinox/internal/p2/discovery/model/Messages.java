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

import org.eclipse.osgi.util.NLS;

/**
 * @author David Green
 */
class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.discovery.model.messages"; //$NON-NLS-1$

	public static String CatalogCategory_CatalogCategory_relevance_invalid;

	public static String CatalogCategory_must_specify_CatalogCategory_id;

	public static String CatalogCategory_must_specify_CatalogCategory_name;

	public static String CatalogItem_invalid_CatalogItem_siteUrl;

	public static String CatalogItem_must_specify_CatalogItem_categoryId;

	public static String CatalogItem_must_specify_CatalogItem_id;

	public static String CatalogItem_must_specify_CatalogItem_license;

	public static String CatalogItem_must_specify_CatalogItem_name;

	public static String CatalogItem_must_specify_CatalogItem_provider;

	public static String CatalogItem_must_specify_CatalogItem_siteUrl;

	public static String FeatureFilter_must_specify_featureFilter_featureId;

	public static String FeatureFilter_must_specify_featureFilter_version;

	public static String Group_must_specify_group_id;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
