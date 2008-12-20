/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import java.util.*;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.internal.provisional.p2.core.Version;

public class VersionAdvice extends AbstractAdvice implements IVersionAdvice {

	Map versions = new HashMap(11);

	/**
	 * Load the given namespace with version mappings from the properties file at 
	 * the given location.  The properties file is expected to be in the normal format
	 * produced and consumed by PDE Build.
	 * @param namespace the namespace to populate
	 * @param location the location of the mapping file
	 */
	public void load(String namespace, String location) {

	}

	public Version getVersion(String namespace, String id) {
		Map values = (Map) versions.get(namespace);
		// if no one says anything then don't say anything.  someone else might have an opinion
		if (values == null)
			return null;
		return (Version) values.get(id);
	}

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
