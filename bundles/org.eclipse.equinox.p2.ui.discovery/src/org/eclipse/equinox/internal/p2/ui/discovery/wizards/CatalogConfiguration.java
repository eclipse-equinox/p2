/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.discovery.wizards;

import java.util.*;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;

/**
 * A configuration for the discovery wizard that affects its presentation and behavior.
 * 
 * @author David Green
 */
public class CatalogConfiguration {

	private final List<CatalogFilter> filters = new ArrayList<CatalogFilter>();

	private boolean showCategories = true;

	private boolean showInstalled = false;

	private boolean showInstalledFilter = true;

	private boolean showTagFilter = true;

	private boolean showTextFilter = true;

	private boolean verifyUpdateSiteAvailability = true;

	private Set<Tag> selectedTags;

	public List<CatalogFilter> getFilters() {
		return filters;
	}

	public Set<Tag> getSelectedTags() {
		return selectedTags;
	}

	public boolean isShowCategories() {
		return showCategories;
	}

	public boolean isShowInstalled() {
		return showInstalled;
	}

	public boolean isShowInstalledFilter() {
		return showInstalledFilter;
	}

	public boolean isShowTagFilter() {
		return showTagFilter;
	}

	/**
	 * indicate if a text field should be provided to allow the user to filter connector descriptors
	 */
	public boolean isShowTextFilter() {
		return showTextFilter;
	}

	public boolean isVerifyUpdateSiteAvailability() {
		return verifyUpdateSiteAvailability;
	}

	public void setShowCategories(boolean showCategories) {
		this.showCategories = showCategories;
	}

	public void setShowInstalled(boolean showInstalled) {
		this.showInstalled = showInstalled;
	}

	public void setShowInstalledFilter(boolean showInstalledFilter) {
		this.showInstalledFilter = showInstalledFilter;
	}

	public void setShowTagFilter(boolean showTagFilter) {
		this.showTagFilter = showTagFilter;
	}

	/**
	 * indicate if a text field should be provided to allow the user to filter connector descriptors
	 */
	public void setShowTextFilter(boolean showTextFilter) {
		this.showTextFilter = showTextFilter;
	}

	public void setVerifyUpdateSiteAvailability(boolean verifyUpdateSiteAvailability) {
		this.verifyUpdateSiteAvailability = verifyUpdateSiteAvailability;
	}

	public void setSelectedTags(Collection<Tag> selectedTags) {
		if (selectedTags != null) {
			this.selectedTags = new HashSet<Tag>(selectedTags);
		} else {
			this.selectedTags = null;
		}
	}

}
