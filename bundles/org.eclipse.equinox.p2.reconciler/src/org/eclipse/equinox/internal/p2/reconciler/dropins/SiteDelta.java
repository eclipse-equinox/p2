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
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.p2.update.Site;

/*
 * Represents the changes between 2 lists of sites.
 */
public class SiteDelta {

	private static final String PLATFORM_BASE = "platform:/base/"; //$NON-NLS-1$

	static class Change {
		Site oldSite;
		Site newSite;

		Change(Site oldSite, Site newSite) {
			this.oldSite = oldSite;
			this.newSite = newSite;
		}
	}

	private List added = new ArrayList();
	private List removed = new ArrayList();
	private List changed = new ArrayList();

	/*
	 * Create and return a new delta object based on the two given lists of
	 * site objects.
	 */
	public static SiteDelta create(List oneList, List twoList) {
		Site[] one = (Site[]) oneList.toArray(new Site[oneList.size()]);
		Site[] two = (Site[]) twoList.toArray(new Site[twoList.size()]);
		SiteDelta result = new SiteDelta();
		for (int i = 0; one == null || i < one.length; i++) {
			boolean found = false;
			for (int j = 0; !found && j < two.length; j++) {
				if (two[j] != null && one[i].getUrl().equals(two[j].getUrl())) {
					found = true;
					// TODO
					if (!one[i].getUrl().equals(PLATFORM_BASE) && !one[i].equals(two[j]))
						result.changed.add(new Change(one[i], two[j]));
					one[i] = null;
					two[j] = null;
				}
			}
			// TODO
			if (!found && !PLATFORM_BASE.equals(one[i].getUrl()))
				result.removed.add(one[i]);
		}
		for (int j = 0; j < two.length; j++) {
			// TODO
			if (two[j] != null && !PLATFORM_BASE.equals(two[j].getUrl()))
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
	 * Return a list of the site changes that were changed. May return an empty list
	 * but never returns null.
	 */
	public Change[] changed() {
		return (Change[]) changed.toArray(new Change[changed.size()]);
	}

	/*
	 * Return a boolean value indicating whether or not there are any
	 * changes in this delta.
	 */
	public boolean isEmpty() {
		return added.size() == 0 && removed.size() == 0 && changed.size() == 0;
	}
}
