/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import org.eclipse.equinox.p2.metadata.Version;

/**
 */
public class FeatureEntry {
	private final String id;
	private String version;
	private String url;
	private String os;
	private String ws;
	private String arch;
	private String nl;
	private String match;
	private final boolean isPlugin;
	private boolean isFragment = false;
	private boolean isRequires = false;
	private boolean unpack = true;
	private boolean optional = false;
	private boolean isPatch = false;
	private String filter;

	public static FeatureEntry createRequires(String id, String version, String match, String filter, boolean isPlugin) {
		FeatureEntry result = new FeatureEntry(id, version, isPlugin);
		result.match = match;
		result.isRequires = true;
		// for requires we don't care what the form is so leave it as false (JAR'd)
		result.unpack = false;
		if (filter != null)
			result.setFilter(filter);
		return result;
	}

	public FeatureEntry(String id, String version, boolean isPlugin) {
		this.id = id;
		this.version = Version.parseVersion(version).toString();
		this.isPlugin = isPlugin;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final FeatureEntry other = (FeatureEntry) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;

		if (isPlugin() != other.isPlugin())
			return false;
		if (isRequires() != other.isRequires())
			return false;
		return true;
	}

	public String getArch() {
		return arch;
	}

	public String getFilter() {
		return filter;
	}

	public String getId() {
		return id;
	}

	public String getMatch() {
		return match;
	}

	public String getNL() {
		return nl;
	}

	public String getOS() {
		return os;
	}

	public String getURL() {
		return url;
	}

	public String getVersion() {
		return version;
	}

	public String getWS() {
		return ws;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	public boolean isFragment() {
		return isFragment;
	}

	public boolean isOptional() {
		return optional;
	}

	public boolean isPlugin() {
		return isPlugin;
	}

	public boolean isRequires() {
		return isRequires;
	}

	public boolean isUnpack() {
		return unpack;
	}

	public void setEnvironment(String os, String ws, String arch, String nl) {
		this.os = os;
		this.ws = ws;
		this.arch = arch;
		this.nl = nl;
	}

	public void setFilter(String filter) {
		this.filter = filter;

	}

	public void setFragment(boolean value) {
		isFragment = value;
	}

	public void setOptional(boolean value) {
		optional = value;
	}

	public void setUnpack(boolean value) {
		unpack = value;
	}

	public void setURL(String value) {
		url = value;
	}

	public void setVersion(String value) {
		version = Version.parseVersion(value).toString();
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(isRequires ? "Requires: " : ""); //$NON-NLS-1$ //$NON-NLS-2$
		result.append(isPlugin ? "Plugin: " : "Feature: "); //$NON-NLS-1$ //$NON-NLS-2$
		result.append(id != null ? id.toString() : ""); //$NON-NLS-1$
		result.append(version != null ? " " + version.toString() : ""); //$NON-NLS-1$ //$NON-NLS-2$
		return result.toString();
	}

	public boolean isPatch() {
		return isPatch;
	}

	public void setPatch(boolean patch) {
		this.isPatch = patch;
	}
}
