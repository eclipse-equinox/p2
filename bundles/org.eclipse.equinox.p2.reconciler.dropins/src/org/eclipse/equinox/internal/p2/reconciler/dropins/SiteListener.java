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

import java.io.File;
import org.eclipse.equinox.internal.p2.update.Site;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener;

/**
 * @since 1.0
 */
public class SiteListener extends RepositoryListener {

	private Site site;

	/*
	 * Create a new site listener on the given site.
	 */
	public SiteListener(Site site) {
		super(Activator.getContext(), Integer.toString(site.getUrl().hashCode()));
		this.site = site;
	}

	/*
	 * Return true if the given list contains the symbolic name for the bundle
	 * represented by the given file handle. Return false otherwise.
	 */
	private boolean contains(String[] plugins, File file) {
		String filename = file.getAbsolutePath();
		for (int i = 0; i < plugins.length; i++)
			if (plugins[i].endsWith(filename))
				return true;
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener#isInterested(java.io.File)
	 */
	public boolean isInterested(File file) {
		String policy = site.getPolicy();
		String[] plugins = site.getList();
		if (Site.POLICY_MANAGED_ONLY.equals(policy)) {
			// TODO
		} else if (Site.POLICY_USER_EXCLUDE.equals(policy)) {
			// ensure the file doesn't refer to a plug-in in our list
			return plugins.length == 0 ? true : !contains(plugins, file);
		} else if (Site.POLICY_USER_INCLUDE.equals(policy)) {
			// we are only interested in plug-ins in the list
			return plugins.length == 0 ? false : contains(plugins, file);
		}
		return false;
	}
}
