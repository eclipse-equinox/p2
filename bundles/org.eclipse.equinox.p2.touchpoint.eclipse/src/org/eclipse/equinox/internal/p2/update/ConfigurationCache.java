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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO ensure thread safety
 *
 */
public class ConfigurationCache {
	private static Map<String, CacheEntry> cache = new HashMap<String, CacheEntry>();

	// class used to represent cache values
	static class CacheEntry {
		long timestamp;
		Configuration config;
	}

	// helper method to convert the file to a cache key. convert to an absolute
	// path to ensure equality between relative and absolute comparisons
	private static String toKey(File file) {
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			// ignore and return the absolute value instead
		}
		return file.getAbsolutePath();
	}

	/*
	 * Return the configuration object in the cache which is represented
	 * by the given file. Do a check on disk to see if the cache is up-to-date.
	 * If not, then treat it as a cache miss.
	 */
	public static Configuration get(File file) {
		String key = toKey(file);
		CacheEntry entry = cache.get(key);
		if (entry == null)
			return null;
		return file.lastModified() == entry.timestamp ? entry.config : null;
	}

	/*
	 * Store the given configuration in the cache.
	 */
	public static void put(File file, Configuration config) {
		String key = toKey(file);
		if (config == null) {
			cache.remove(key);
			return;
		}
		CacheEntry entry = new CacheEntry();
		entry.config = config;
		entry.timestamp = file.lastModified();
		cache.put(key, entry);
	}

}
