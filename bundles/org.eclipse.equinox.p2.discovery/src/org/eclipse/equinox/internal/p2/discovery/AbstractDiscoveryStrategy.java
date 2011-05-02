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
package org.eclipse.equinox.internal.p2.discovery;

import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.model.*;

/**
 * An abstraction of a strategy for discovering connectors and categories. Strategy design pattern. Note that strategies
 * are not reusable and must be disposed.
 * 
 * @author David Green
 * @author Steffen Pingel
 */
public abstract class AbstractDiscoveryStrategy {

	protected List<CatalogCategory> categories;

	protected List<Certification> certifications;

	protected List<CatalogItem> items;

	protected List<Tag> tags;

	public void dispose() {
		// ignore
	}

	public List<CatalogCategory> getCategories() {
		return categories;
	}

	public List<Certification> getCertifications() {
		return certifications;
	}

	public List<CatalogItem> getItems() {
		return items;
	}

	public List<Tag> getTags() {
		return tags;
	}

	/**
	 * Perform discovery and add discovered items to {@link #getCategories() categories} and {@link #getItems()}.
	 */
	public abstract void performDiscovery(IProgressMonitor monitor) throws CoreException;

	public void setCategories(List<CatalogCategory> categories) {
		this.categories = categories;
	}

	public void setCertifications(List<Certification> certifications) {
		this.certifications = certifications;
	}

	public void setItems(List<CatalogItem> connectors) {
		this.items = connectors;
	}

	public void setTags(List<Tag> itemKinds) {
		this.tags = itemKinds;
	}

}
