/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.viewers;

import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

/**
 * 
 * @since 3.5
 */
public class MetadataRepositoryElementComparator extends ViewerComparator {
	private int key;
	private static final String ENABLED = "XX"; //$NON-NLS-1$  // Value doesn't matter, just that it's not BLANK
	private static final String BLANK = ""; //$NON-NLS-1$
	private static final int ASCENDING = 1;
	private static final int DESCENDING = -1;
	private int primaryKeyDirection = ASCENDING;

	public MetadataRepositoryElementComparator(int sortKey) {
		this.key = sortKey;
	}

	public int compare(Viewer viewer, Object obj1, Object obj2) {
		MetadataRepositoryElement repo1 = obj1 instanceof MetadataRepositoryElement ? (MetadataRepositoryElement) obj1 : null;
		MetadataRepositoryElement repo2 = obj2 instanceof MetadataRepositoryElement ? (MetadataRepositoryElement) obj2 : null;
		if (repo1 == null || repo2 == null)
			// If these are not both metadata repository elements, use the super class comparator.
			return super.compare(viewer, obj1, obj2);
		int result = compare(repo1, repo2, key, primaryKeyDirection);
		if (result == 0) {
			int secondaryKey = getSecondaryKeyFor(key);
			if (secondaryKey < 0)
				return result;
			result = compare(repo1, repo2, secondaryKey, ASCENDING);
			if (result == 0)
				result = compare(repo1, repo2, getSecondaryKeyFor(secondaryKey), ASCENDING);
		}
		return result;
	}

	int compare(MetadataRepositoryElement repo1, MetadataRepositoryElement repo2, int sortKey, int direction) {
		String key1, key2;
		if (sortKey == RepositoryDetailsLabelProvider.COL_NAME) {
			// Compare the repo names
			key1 = repo1.getName();
			key2 = repo2.getName();
		} else if (sortKey == RepositoryDetailsLabelProvider.COL_LOCATION) {
			key1 = URIUtil.toUnencodedString(repo1.getLocation());
			key2 = URIUtil.toUnencodedString(repo2.getLocation());
		} else { // COL_ENABLEMENT
			key1 = repo1.isEnabled() ? ENABLED : BLANK;
			key2 = repo2.isEnabled() ? ENABLED : BLANK;
		}
		// If the key is blank (no location, or disabled)..it should appear last
		if (key1.length() == 0 && key2.length() > 0)
			return direction;
		if (key1.length() > 0 && key2.length() == 0)
			return direction * -1;
		return key1.compareToIgnoreCase(key2) * direction;
	}

	private int getSecondaryKeyFor(int primaryKey) {
		// This is the important one - if sorting by enablement, sort by location, since
		// not all repos have a name
		if (primaryKey == RepositoryDetailsLabelProvider.COL_ENABLEMENT)
			return RepositoryDetailsLabelProvider.COL_LOCATION;
		if (primaryKey == RepositoryDetailsLabelProvider.COL_NAME)
			return RepositoryDetailsLabelProvider.COL_LOCATION;
		// unlikely to have duplicate locations requiring a secondary key
		if (primaryKey == RepositoryDetailsLabelProvider.COL_LOCATION)
			return RepositoryDetailsLabelProvider.COL_NAME;
		return -1;
	}

	public void sortAscending() {
		primaryKeyDirection = ASCENDING;
	}

	public void sortDescending() {
		primaryKeyDirection = DESCENDING;
	}

	public boolean isAscending() {
		return primaryKeyDirection == ASCENDING;
	}

	public void setSortKey(int key) {
		this.key = key;
	}

	public int getSortKey() {
		return key;
	}

}
