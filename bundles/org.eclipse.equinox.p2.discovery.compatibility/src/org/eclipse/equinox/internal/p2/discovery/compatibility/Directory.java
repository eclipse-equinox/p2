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

package org.eclipse.equinox.internal.p2.discovery.compatibility;

import java.util.ArrayList;
import java.util.List;

/**
 * A directory representing locations of discovery sources.
 *
 * @author David Green
 */
public class Directory {

	/**
	 * an entry in the directory, which represents a specific discovery source. Future versions of this class may
	 * include policy or other attributes declared by the directory.
	 */
	public static final class Entry {
		private String location;

		private boolean permitCategories;

		public Entry() {
		}

		/**
		 * the location of the entry (an URL)
		 */
		public String getLocation() {
			return location;
		}

		/**
		 * the location of the entry (an URL)
		 */
		public void setLocation(String location) {
			this.location = location;
		}

		/**
		 * indicate if creation of categories by this location is permitted
		 */
		public boolean isPermitCategories() {
			return permitCategories;
		}

		/**
		 * indicate if creation of categories by this location is permitted
		 */
		public void setPermitCategories(boolean permitCategories) {
			this.permitCategories = permitCategories;
		}
	}

	private final List<Entry> entries = new ArrayList<>();

	public List<Entry> getEntries() {
		return entries;
	}
}
