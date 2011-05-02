/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
import java.net.URL;
import org.eclipse.equinox.p2.core.ProvisionException;

/*
 * Class which controls the reading and writing of Configuration (platform.xml) objects.
 * We keep a local cached copy to avoid multiple reads. When we install new features we
 * seem to only write out the platform.xml in the "commit" phase so we don't need to 
 * batch the writes.
 */
public class ConfigurationIO {
	private static CacheEntry cache = null;

	// class used to represent cache values
	static class CacheEntry {
		Configuration config;
		URL osgiInstallArea;
		File location;
		long timestamp;
	}

	/*
	 * Return the configuration object which is represented by the given file. 
	 */
	static Configuration read(File file, URL osgiInstallArea) throws ProvisionException {
		// check the cached copy first
		if (cache != null && file.lastModified() == cache.timestamp)
			return cache.config;

		// cache miss or file is out of date, read from disk
		Configuration config = ConfigurationParser.parse(file, osgiInstallArea);
		if (config == null)
			return null;

		// successful read, store in the cache before we return
		cache(file, config, osgiInstallArea);
		return config;
	}

	/*
	 * Store the given configuration file in the local cache.
	 */
	private static void cache(File location, Configuration config, URL osgiInstallArea) {
		CacheEntry entry = new CacheEntry();
		entry.config = config;
		entry.osgiInstallArea = osgiInstallArea;
		entry.location = location;
		entry.timestamp = location.lastModified();
		cache = entry;
	}

	/*
	 * Save the given configuration to the file-system.
	 */
	static void write(File location, Configuration config, URL osgiInstallArea) throws ProvisionException {
		// write it to disk 
		ConfigurationWriter.save(config, location, osgiInstallArea);
		// save a copy in the cache
		cache(location, config, osgiInstallArea);
	}

}
