/*******************************************************************************
 * Copyright (c) 2009, 2017 Tasktop Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.discovery.util;

import java.util.Comparator;
import org.eclipse.equinox.internal.p2.discovery.model.*;

/**
 * A comparator that orders connectors by group and alphabetically by their name.
 * 
 * @author David Green
 */
public class CatalogItemComparator implements Comparator<CatalogItem> {

	/**
	 * compute the index of the group id
	 * 
	 * @param groupId
	 *            the group id or null
	 * @return the index, or -1 if not found
	 */
	private int computeGroupIndex(CatalogCategory category, String groupId) {
		if (groupId != null) {
			int index = -1;
			for (Group group : category.getGroup()) {
				++index;
				if (group.getId().equals(groupId)) {
					return index;
				}
			}
		}
		return -1;
	}

	@Override
	public int compare(CatalogItem o1, CatalogItem o2) {
		if (o1.getCategory() != o2.getCategory()) {
			throw new IllegalArgumentException();
		}
		if (o1 == o2) {
			return 0;
		}
		int g1 = computeGroupIndex(o1.getCategory(), o1.getGroupId());
		int g2 = computeGroupIndex(o2.getCategory(), o2.getGroupId());
		int i;
		if (g1 != g2) {
			if (g1 == -1) {
				i = 1;
			} else if (g2 == -1) {
				i = -1;
			} else {
				i = g1 - g2;
			}
		} else {
			i = o1.getName().compareToIgnoreCase(o2.getName());
			if (i == 0) {
				i = o1.getId().compareTo(o2.getId());
			}
		}
		return i;
	}

}
