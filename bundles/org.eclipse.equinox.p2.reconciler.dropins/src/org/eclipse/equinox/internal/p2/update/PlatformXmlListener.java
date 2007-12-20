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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.p2.reconciler.dropins.Activator;
import org.eclipse.equinox.internal.p2.update.SiteDelta.Change;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.directorywatcher.DirectoryChangeListener;
import org.eclipse.equinox.p2.directorywatcher.DirectoryWatcher;

/**
 * @since 1.0
 */
public class PlatformXmlListener extends DirectoryChangeListener {

	private static final String PLATFORM_XML = "platform.xml"; //$NON-NLS-1$
	private boolean changed = false;
	private Map sites = new HashMap();
	private File root;

	public PlatformXmlListener(File file) throws ProvisionException {
		super();
		if (!PLATFORM_XML.equals(file.getName()))
			throw new IllegalArgumentException();
		this.root = file;
		// don't need to set the "sites" variable since we will treat
		// everything as "added" in the delta if it is null
		process();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.IDirectoryChangeListener#added(java.io.File)
	 */
	public boolean added(File file) {
		changed = changed || PLATFORM_XML.equals(file.getName());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.IDirectoryChangeListener#changed(java.io.File)
	 */
	public boolean changed(File file) {
		changed = changed || PLATFORM_XML.equals(file.getName());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.IDirectoryChangeListener#getSeenFile(java.io.File)
	 */
	public Long getSeenFile(File file) {
		return new Long(0);
	}

	/*
	 * Parse the platform.xml file and return the list of sites.
	 */
	private Site[] parseConfiguration() throws ProvisionException {
		return new ConfigurationParser().parse(root);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.DirectoryChangeListener#isInterested(java.io.File)
	 */
	public boolean isInterested(File file) {
		return file.getName().equals(PLATFORM_XML);
	}

	private Site[] getSites() {
		List result = new ArrayList();
		for (Iterator iter = sites.values().iterator(); iter.hasNext();) {
			SiteInfo info = (SiteInfo) iter.next();
			result.add(info.getSite());
		}
		return (Site[]) result.toArray(new Site[result.size()]);
	}

	/*
	 * This is where we reconcile the platform.xml and bundles.txt.
	 */
	private void process() throws ProvisionException {
		SiteDelta delta = SiteDelta.create(getSites(), parseConfiguration());
		if (delta.isEmpty())
			return;
		added(delta.added());
		removed(delta.removed());
		changed(delta.changed());
		Activator.synchronize(getMetadataRepositories(), null); // TODO proper progress monitoring?
	}

	// iterate over the site listeners and collect the metadata repositories
	public List getMetadataRepositories() {
		List result = new ArrayList();
		for (Iterator iter = sites.values().iterator(); iter.hasNext();) {
			SiteInfo info = (SiteInfo) iter.next();
			result.add(info.getListener().getMetadataRepository());
		}
		return result;
	}

	/*
	 * The given list of sites has been added so add directory
	 * watchers for each of them.
	 */
	private void added(Site[] added) throws ProvisionException {
		if (added == null || added.length == 0)
			return;
		for (int i = 0; i < added.length; i++) {
			Site site = added[i];
			// TODO skip for now
			if ("platform:/base/".equals(site.getUrl()))
				continue;
			try {
				URL url = new URL(site.getUrl());
				try {
					url = FileLocator.resolve(url);
				} catch (IOException e) {
					throw new ProvisionException("Exception while processing configuration.", e);
				}
				File file = new File(url.getPath(), "plugins");
				DirectoryWatcher watcher = new DirectoryWatcher(file);
				SiteListener listener = new SiteListener(site);
				watcher.addListener(listener);
				watcher.start();
				sites.put(site.getUrl(), new SiteInfo(site, watcher, listener));
			} catch (MalformedURLException e) {
				throw new ProvisionException(e);
			}
		}
	}

	/*
	 * The given list of sites has been removed so act accordingly.
	 * Remove all the registered directory watchers.
	 */
	private void removed(Site[] removed) {
		if (removed == null || removed.length == 0)
			return;
		for (int i = 0; i < removed.length; i++) {
			Site site = removed[i];
			SiteInfo info = (SiteInfo) sites.get(site.getUrl());
			// TODO I think this should be an error?
			if (info == null) {
				// 
			}
			info.getWatcher().stop();
			sites.remove(site.getUrl());
		}
	}

	/*
	 * The given set of sites has had their contents changed.
	 */
	private void changed(Change[] changes) throws ProvisionException {
		for (int i = 0; i < changes.length; i++) {
			Change change = changes[i];
			if (majorChange(change)) {
				removed(new Site[] {change.oldSite});
				added(new Site[] {change.newSite});
			}
		}
	}

	/*
	 * Return true if the differences between the 2 sites should cause
	 * a new listener to be created.
	 */
	private boolean majorChange(Change change) {
		Site one = change.oldSite;
		Site two = change.newSite;
		if (!Activator.equals(one.getPolicy(), two.getPolicy()))
			return true;
		return !Activator.equals(one.getList(), two.getList());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.IDirectoryChangeListener#removed(java.io.File)
	 */
	public boolean removed(File file) {
		changed = changed || PLATFORM_XML.equals(file.getName());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.IDirectoryChangeListener#startPoll()
	 */
	public void startPoll() {
		changed = false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.IDirectoryChangeListener#stopPoll()
	 */
	public void stopPoll() {
		if (changed)
			try {
				process();
			} catch (ProvisionException e) {
				e.printStackTrace();
			}
		changed = false;
	}

}
