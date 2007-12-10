/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.update;

import java.util.ArrayList;
import java.util.List;

/*
 * Represents the changes between 2 lists of sites.
 */
public class SiteDelta {

	private List added = new ArrayList();
	private List removed = new ArrayList();
	private List changed = new ArrayList();

	/*
	 * Create and return a new delta object based on the two given lists of
	 * site objects.
	 */
	public static SiteDelta create(Site[] one, Site[] two) {
		SiteDelta result = new SiteDelta();
		for (int i = 0; one == null || i < one.length; i++) {
			boolean found = false;
			for (int j = 0; !found && j < two.length; j++) {
				if (two[j] != null && one[i].getUrl().equals(two[j].getUrl())) {
					found = true;
					if (!one[i].equals(two[j]))
						result.changed.add(one[i]);
					one[i] = null;
					two[j] = null;
				}
			}
			if (!found)
				result.removed.add(one[i]);
		}
		for (int j = 0; j < two.length; j++) {
			if (two[j] != null)
				result.added.add(two[j]);
		}
		return result;
	}

	/*
	 * Return a list of the sites that were added. May return an empty list
	 * but never returns null.
	 */
	public Site[] added() {
		return (Site[]) added.toArray(new Site[added.size()]);
	}

	/*
	 * Return a list of the sites that were removed. May return an empty list
	 * but never returns null.
	 */
	public Site[] removed() {
		return (Site[]) removed.toArray(new Site[removed.size()]);
	}

	/*
	 * Return a list of the sites that were changed. May return an empty list
	 * but never returns null.
	 */
	public Site[] changed() {
		return (Site[]) changed.toArray(new Site[changed.size()]);
	}

	/*
	 * Return a boolean value indicating whether or not there are any
	 * changes in this delta.
	 */
	public boolean isEmpty() {
		return added.size() == 0 && removed.size() == 0 && changed.size() == 0;
	}
}
