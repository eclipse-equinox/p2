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

/**
 * A means of specifying that a feature must be present in order for the connectorDescriptor to be presented to the
 * user.
 * 
 * @author David Green
 */
public class FeatureFilter {

	protected String featureId;

	protected String version;

	protected CatalogItem item;

	public FeatureFilter() {
	}

	/**
	 * The id of the feature to test
	 */
	public String getFeatureId() {
		return featureId;
	}

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	/**
	 * A version specifier, specified in the same manner as version dependencies are specified in an OSGi manifest. For
	 * example: "[3.0,4.0)"
	 */
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public CatalogItem getItem() {
		return item;
	}

	public void setItem(CatalogItem catalogItem) {
		this.item = catalogItem;
	}

	public void validate() throws ValidationException {
		if (featureId == null || featureId.length() == 0) {
			throw new ValidationException(Messages.FeatureFilter_must_specify_featureFilter_featureId);
		}
		if (version == null || version.length() == 0) {
			throw new ValidationException(Messages.FeatureFilter_must_specify_featureFilter_version);
		}
	}
}
