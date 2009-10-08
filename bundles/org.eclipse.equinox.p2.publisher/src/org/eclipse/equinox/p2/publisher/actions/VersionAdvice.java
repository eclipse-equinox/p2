/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.equinox.internal.provisional.p2.metadata.Version;

import java.io.*;
import java.util.*;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

public class VersionAdvice extends AbstractAdvice implements IVersionAdvice {

	Map versions = new HashMap(11);

	/**
	 * Load the given namespace with version mappings from the properties file at 
	 * the given location.  The properties file is expected to be in the normal format
	 * produced and consumed by PDE Build.
	 * @param namespace the namespace to populate.  If "null" is passed
	 * in, the version will be stored in the "null" namespace
	 * @param location the location of the mapping file
	 */
	public void load(String namespace, String location) {
		load(namespace, location, null);
	}

	public void load(String namespace, String location, String idSuffix) {
		if (namespace == null)
			namespace = "null"; //$NON-NLS-1$
		Properties properties = new Properties();
		File file = new File(location);
		if (file.exists()) {
			InputStream stream = null;
			try {
				stream = new BufferedInputStream(new FileInputStream(file));
				properties.load(stream);
			} catch (IOException e) {
				// nothing
			} finally {
				if (stream != null)
					try {
						stream.close();
					} catch (IOException e) {
						//nothing
					}
			}
		}
		if (properties.size() > 0) {
			Enumeration enumeration = properties.keys();
			while (enumeration.hasMoreElements()) {
				String key = (String) enumeration.nextElement();
				if (idSuffix != null)
					key += idSuffix;
				String value = properties.getProperty(key);
				setVersion(namespace, key, new Version(value));
			}
		}
	}

	/**
	 * Returns the version advice for the given id in the given namespace.
	 * @param namespace the namespace in which to look for advice
	 * @param id the item for which advice is sought
	 * @return the version advice found or <code>null</code> if none
	 */
	public Version getVersion(String namespace, String id) {
		Map values = (Map) versions.get(namespace);
		// if no one says anything then don't say anything.  someone else might have an opinion
		if (values != null) {
			Version result = (Version) values.get(id);
			if (result != null)
				return result;
		}

		values = (Map) versions.get("null"); //$NON-NLS-1$
		if (values == null)
			return null;
		return (Version) values.get(id);
	}

	/**
	 * Sets the version advice for the given id in the given namespace.
	 * @param namespace the namespace in which to look for advice
	 * @param id the item for which advice is sought
	 * @param version the version advice for the given id or <code>null</code> to remove advice
	 */
	public void setVersion(String namespace, String id, Version version) {
		Map values = (Map) versions.get(namespace);
		if (values == null) {
			// if we are clearing values then there is nothing to do
			if (version == null)
				return;
			values = new HashMap();
			versions.put(namespace, values);
		}
		if (version == null)
			values.remove(id);
		else
			values.put(id, version);
	}

	public IPublisherAdvice merge(IPublisherAdvice advice) {
		if (!(advice instanceof VersionAdvice))
			return this;
		VersionAdvice source = (VersionAdvice) advice;
		for (Iterator i = source.versions.keySet().iterator(); i.hasNext();) {
			String namespace = (String) i.next();
			Map myValues = (Map) versions.get(namespace);
			Map sourceValues = (Map) source.versions.get(namespace);
			if (myValues == null)
				versions.put(namespace, sourceValues);
			else if (sourceValues != null)
				versions.put(namespace, merge(myValues, sourceValues));
		}
		return this;
	}

	private Map merge(Map myValues, Map sourceValues) {
		Map result = new HashMap(myValues);
		for (Iterator i = sourceValues.keySet().iterator(); i.hasNext();) {
			String key = (String) i.next();
			if (result.get(key) == null)
				result.put(key, sourceValues.get(key));
		}
		return result;
	}
}
