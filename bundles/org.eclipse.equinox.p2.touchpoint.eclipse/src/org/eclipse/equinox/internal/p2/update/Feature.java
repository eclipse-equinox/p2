/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.update;

import java.net.URL;

/*
 * Represents a feature entry in a platform.xml file.
 */
public class Feature {

	private String id;
	private String url;
	private String version;
	private Site site;
	private String pluginIdentifier;
	private String pluginVersion;
	private String application;
	private URL[] roots;
	private boolean primary = false;

	public Feature(Site site) {
		super();
		if (site == null)
			throw new IllegalArgumentException(Messages.empty_feature_site);
		this.site = site;
	}

	public String getApplication() {
		return application;
	}

	public String getId() {
		return id;
	}

	public String getPluginIdentifier() {
		return pluginIdentifier;
	}

	public String getPluginVersion() {
		return pluginVersion;
	}

	public URL[] getRoots() {
		return roots;
	}

	public Site getSite() {
		return site;
	}

	public String getUrl() {
		return url;
	}

	public String getVersion() {
		return version;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setPluginIdentifier(String pluginIdentifier) {
		this.pluginIdentifier = pluginIdentifier;
	}

	public void setPluginVersion(String pluginVersion) {
		this.pluginVersion = pluginVersion;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public void setRoots(URL[] roots) {
		this.roots = roots;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof Feature))
			return false;
		Feature other = (Feature) obj;
		if (!equals(getId(), other.getId()))
			return false;
		// shallow equals here. sites should never be null
		if (!equals(getSite().getUrl(), other.getSite().getUrl()))
			return false;
		if (!equals(getUrl(), other.getUrl()))
			return false;
		if (!equals(getVersion(), other.getVersion()))
			return false;
		return true;
	}

	private boolean equals(Object one, Object two) {
		return one == null ? two == null : one.equals(two);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return id.hashCode() + version.hashCode();
	}
}
