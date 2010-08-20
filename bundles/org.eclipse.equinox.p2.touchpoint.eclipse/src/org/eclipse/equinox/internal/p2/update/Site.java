/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.update;

import java.util.*;

/*
 * Represents a site in a platform.xml file.
 */
public class Site {

	public final static String POLICY_MANAGED_ONLY = "MANAGED-ONLY"; //$NON-NLS-1$
	public final static String POLICY_USER_EXCLUDE = "USER-EXCLUDE"; //$NON-NLS-1$
	public final static String POLICY_USER_INCLUDE = "USER-INCLUDE"; //$NON-NLS-1$
	public final static String PROP_LINK_FILE = "org.eclipse.update.site.linkFile"; //$NON-NLS-1$

	private String policy;
	private boolean enabled = true;
	private boolean updateable = true;
	private String url;
	private String linkFile;
	private Collection<Feature> features = new HashSet<Feature>();
	private List<String> list = new ArrayList<String>();

	public void addFeature(Feature feature) {
		this.features.add(feature);
	}

	public void addPlugin(String plugin) {
		this.list.add(plugin);
	}

	public boolean removePlugin(String plugin) {
		return this.list.remove(plugin);
	}

	public void setPluginList(List<String> plugins) {
		if (plugins == null)
			this.list = new ArrayList<String>();
		else
			this.list = plugins;
	}

	public Feature[] getFeatures() {
		return features.toArray(new Feature[features.size()]);
	}

	/*
	 * Return the feature object with the specific id and version. Return null 
	 * if there is no match or the id is null. If the version is null then return the
	 * first feature with a matching id.
	 */
	public Feature getFeature(String id, String version) {
		if (id == null)
			return null;
		for (Feature feature : features) {
			if (id.equals(feature.getId())) {
				if (version == null || version.equals(feature.getVersion()))
					return feature;
			}
		}
		return null;
	}

	public Feature removeFeature(String featureURL) {
		for (Feature feature : features) {
			String nextURL = feature.getUrl();
			if (nextURL != null && nextURL.equals(featureURL))
				return features.remove(feature) ? feature : null;
		}
		return null;
	}

	public String getLinkFile() {
		return linkFile;
	}

	public String[] getList() {
		return list.toArray(new String[list.size()]);
	}

	public String getPolicy() {
		return policy;
	}

	/**
	 * Note the string that we are returning is an <em>ENCODED</em> URI string.
	 */
	public String getUrl() {
		return url;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isUpdateable() {
		return updateable;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setLinkFile(String linkFile) {
		this.linkFile = linkFile;
	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}

	/**
	 * Note that the string should be an <em>ENCODED</em> URI string.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getUrl().hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof Site))
			return false;
		Site other = (Site) obj;
		if (isEnabled() != other.isEnabled())
			return false;
		if (isUpdateable() != other.isUpdateable())
			return false;
		if (!getUrl().equals(other.getUrl()))
			return false;
		if (!Site.equals(getLinkFile(), other.getLinkFile()))
			return false;
		if (!Site.equals(getPolicy(), other.getPolicy()))
			return false;
		if (!Site.equals(getList(), other.getList()))
			return false;
		if (!Site.equals(getFeatures(), other.getFeatures()))
			return false;
		return true;
	}

	/*
	 * Return a boolean value indicating whether or not the given
	 * objects are considered equal.
	 */
	public static boolean equals(Object one, Object two) {
		return one == null ? two == null : one.equals(two);
	}

	/*
	 * Return a boolean value indicating whether or not the given
	 * lists are considered equal.
	 */
	public static boolean equals(Object[] one, Object[] two) {
		if (one == null && two == null)
			return true;
		if (one == null || two == null)
			return false;
		if (one.length != two.length)
			return false;
		for (int i = 0; i < one.length; i++) {
			boolean found = false;
			for (int j = 0; !found && j < two.length; j++)
				found = one[i].equals(two[j]);
			if (!found)
				return false;
		}
		return true;
	}
}
