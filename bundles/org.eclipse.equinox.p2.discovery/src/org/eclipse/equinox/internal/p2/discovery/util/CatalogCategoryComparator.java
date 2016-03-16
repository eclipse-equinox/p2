/*******************************************************************************
 * Copyright (c) 2009, 2016 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery.util;

import java.util.Comparator;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;

/**
 * A comparator that orders categories by relevance and name.
 * 
 * @author David Green
 * @author Steffen Pingel
 */
public class CatalogCategoryComparator implements Comparator<CatalogCategory> {

	public int compare(CatalogCategory o1, CatalogCategory o2) {
		if (o1 == o2) {
			return 0;
		}
		String r1 = o1.getRelevance();
		String r2 = o2.getRelevance();
		int i = 0;
		if (r1 != null && r2 != null) {
			// don't have to worry about format, since they were already validated
			// note that higher relevance appears first, thus the reverse order of
			// the comparison.
			i = Integer.valueOf(r2).compareTo(Integer.valueOf(r1));
		} else if (r1 == null && r2 != null) {
			return 1;
		} else if (r2 == null && r1 != null) {
			return -1;
		}
		if (i == 0) {
			i = o1.getName().compareToIgnoreCase(o2.getName());
			if (i == 0) {
				i = o1.getId().compareTo(o2.getId());
			}
		}
		return i;
	}

}
