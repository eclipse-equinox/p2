/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import org.eclipse.equinox.internal.p2.update.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**
 * @since 1.0
 */
public class LinksManager {

	private static final String EXTENSION_LINK = ".link"; //$NON-NLS-1$
	private static final String PATH_PROPERTY = "path"; //$NON-NLS-1$
	private static final String PLATFORM_PROTOCOL = "platform:"; //$NON-NLS-1$
	private static final String ECLIPSE_FOLDER = "eclipse"; //$NON-NLS-1$
	private String defaultPolicy;
	private Configuration configuration;
	private boolean dirty = false;

	/*
	 * If one site has a MANAGED_ONLY policy, then newly discovered sites must also have
	 * the same thing. Otherwise, they will have a policy of USER_EXCLUDE.
	 */
	private String getDefaultPolicy() {
		if (defaultPolicy == null) {
			for (Iterator iter = configuration.getSites().iterator(); defaultPolicy == null && iter.hasNext();) {
				if (Site.POLICY_MANAGED_ONLY.equals(((Site) iter.next()).getPolicy()))
					defaultPolicy = Site.POLICY_MANAGED_ONLY;
			}
			defaultPolicy = Site.POLICY_USER_EXCLUDE;
		}
		return defaultPolicy;
	}

	/*
	 * Synchronize the given configuration file with the files that are in the specified links folder.
	 * If any extension locations from the links folder are missing from the file, then update
	 * the configuration.
	 */
	public void synchronize(File configurationFile, File linksFolder) throws ProvisionException {
		if (!configurationFile.exists() || !linksFolder.exists())
			return;

		// read the existing configuration from disk
		configuration = ConfigurationParser.parse(configurationFile, null);
		if (configuration == null)
			return;

		// get the list of extension locations from the links folder
		linksFolder.listFiles(new FileFilter() {
			public boolean accept(File file) {
				if (file.isFile() && file.getName().endsWith(EXTENSION_LINK))
					configure(file);
				return false;
			}
		});

		// write out a new file if there were any changes.
		if (dirty)
			ConfigurationWriter.save(configuration, configurationFile, null);
		dirty = false;
	}

	/*
	 * Roughly copied from PlatformConfiguration#configureExternalLinkSite in 
	 * Update Configurator.
	 */
	void configure(File location) {
		String path = readExtension(location);
		boolean updateable = true;

		// parse out link information
		if (path.startsWith("r ")) { //$NON-NLS-1$
			updateable = false;
			path = path.substring(2).trim();
		} else if (path.startsWith("rw ")) { //$NON-NLS-1$
			path = path.substring(3).trim();
		} else {
			path = path.trim();
		}

		URL url;
		// 	make sure we have a valid link specification
		try {
			File siteFile = new File(path);
			siteFile = new File(siteFile, ECLIPSE_FOLDER);
			url = siteFile.toURL();
			if (findConfiguredSite(url) != null)
				// linked site is already known
				return;
		} catch (MalformedURLException e) {
			// ignore bad links ...
			e.printStackTrace();
			return;
		}

		Site site = new Site();
		site.setLinkFile(location.getAbsolutePath());
		site.setEnabled(true);
		site.setPolicy(getDefaultPolicy());
		site.setUpdateable(updateable);
		site.setUrl(url.toExternalForm());
		configuration.add(site);
		dirty = true;
	}

	/*
	 * Look through the list of sites already known to this configuration
	 * and determine if there is one with the given URL.
	 */
	private Site findConfiguredSite(URL url) {
		String urlString = url.toExternalForm();
		Site result = internalFindConfiguredSite(urlString);
		if (result != null)
			return result;
		// try again with fixed URLs since they can be tricky
		try {
			urlString = Utils.decode(urlString, "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			// ignore
		}
		urlString = Utils.canonicalizeURL(urlString);
		return internalFindConfiguredSite(urlString);
	}

	private Site internalFindConfiguredSite(String url) {
		for (Iterator iter = configuration.getSites().iterator(); iter.hasNext();) {
			Site site = (Site) iter.next();
			String urlString = site.getUrl();
			urlString = Utils.canonicalizeURL(urlString);
			if (urlString.startsWith(PLATFORM_PROTOCOL))
				continue;
			if (urlString.equals(url))
				return site;
		}
		return null;
	}

	/*
	 * Read the contents of a link file and return the path. May or may not include
	 * a prefix indicating read-only or read-write status.
	 */
	private String readExtension(File file) {
		Properties props = new Properties();
		InputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(file));
			props.load(input);
		} catch (IOException e) {
			// TODO
			e.printStackTrace();
			return null;
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
		}
		return props.getProperty(PATH_PROPERTY);
	}
}
