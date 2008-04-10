/*******************************************************************************
* Copyright (c) 2007, 2008 IBM Corporation and others.
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.update.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;

/**
 * @since 1.0
 */
public class PlatformXmlListener extends DirectoryChangeListener {

	private static final String PLATFORM_XML = "platform.xml"; //$NON-NLS-1$
	private boolean changed = false;
	private File root;
	private long lastModified = -1l;
	private Set configRepositories;

	/*
	 * Construct a new listener based on the given platform.xml file.
	 */
	public PlatformXmlListener(File file) {
		super();
		if (!PLATFORM_XML.equals(file.getName()))
			throw new IllegalArgumentException();
		this.root = file;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#added(java.io.File)
	 */
	public boolean added(File file) {
		changed = changed || PLATFORM_XML.equals(file.getName());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#changed(java.io.File)
	 */
	public boolean changed(File file) {
		changed = changed || PLATFORM_XML.equals(file.getName());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#getSeenFile(java.io.File)
	 */
	public Long getSeenFile(File file) {
		return new Long(0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener#isInterested(java.io.File)
	 */
	public boolean isInterested(File file) {
		return file.getName().equals(PLATFORM_XML) && lastModified != file.lastModified();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#removed(java.io.File)
	 */
	public boolean removed(File file) {
		changed = changed || PLATFORM_XML.equals(file.getName());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#startPoll()
	 */
	public void startPoll() {
		changed = false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#stopPoll()
	 */
	public void stopPoll() {
		if (changed) {
			lastModified = root.lastModified();
			try {
				Configuration configuration = ConfigurationParser.parse(root, (URL) null);
				synchronizeConfiguration(configuration);
			} catch (ProvisionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		changed = false;
	}

	public List getMetadataRepositories() {
		return makeList(configRepositories);
	}

	private IMetadataRepository getMatchingRepo(Collection repositoryList, String urlString) {
		if (repositoryList == null)
			return null;
		IPath urlPath = new Path(urlString).makeAbsolute();
		for (Iterator iter = repositoryList.iterator(); iter.hasNext();) {
			IMetadataRepository repo = (IMetadataRepository) iter.next();
			Path repoPath = new Path(repo.getLocation().toExternalForm());
			if (repoPath.makeAbsolute().equals(urlPath))
				return repo;
		}
		return null;
	}

	protected void synchronizeConfiguration(Configuration config) {
		List sites = config.getSites();
		Set newRepos = new LinkedHashSet();
		for (Iterator iter = sites.iterator(); iter.hasNext();) {
			String siteURL = ((Site) iter.next()).getUrl();
			// TODO: this is our way of skipping the base.
			// we will need to change this to platform:base: at some point
			if ("file:.".equals(siteURL) || "file:".equals(siteURL)) //$NON-NLS-1$//$NON-NLS-2$
				continue;
			if (siteURL.startsWith("file:") && siteURL.endsWith("/eclipse/")) //$NON-NLS-1$//$NON-NLS-2$
				siteURL = siteURL.substring(0, siteURL.length() - 8);
			IMetadataRepository match = getMatchingRepo(Activator.getConfigurationRepositories(), siteURL);
			if (match == null)
				match = getMatchingRepo(configRepositories, siteURL);
			if (match == null) {
				try {
					URL repoURL = new URL(siteURL);
					IMetadataRepository newRepo = Activator.loadMetadataRepository(repoURL);
					Activator.loadArtifactRepository(repoURL);
					newRepos.add(newRepo);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ProvisionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		// if we didn't add any new repos then there is no work to do
		if (newRepos.isEmpty())
			return;
		configRepositories = newRepos;
		Activator.synchronize(makeList(newRepos), null);
	}

	// TODO: this is a kludge to fix collection impedance mismatch
	//		 between the xml listener and the activator; get rid of it!
	private List makeList(Set set) {
		List list = new ArrayList((set != null ? set.size() : 0));
		if (set != null) {
			for (Iterator iter = set.iterator(); iter.hasNext();)
				list.add(iter.next());
		}
		return list;
	}

}
