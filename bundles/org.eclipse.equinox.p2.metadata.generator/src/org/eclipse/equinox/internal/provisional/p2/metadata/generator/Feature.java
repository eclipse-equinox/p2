/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata.generator;

import java.util.ArrayList;
import java.util.Map;

/**
 * 
 * Feature information
 */
public class Feature {

	private final String id;
	private final String version;
	private String label;
	private String pluginId;
	private boolean primary = false;
	private boolean exclusive = false;
	private String application;

	private URLEntry description;
	private URLEntry license;
	private URLEntry copyright;

	private String installHandler;
	private String installHandlerURL;
	private String installHandlerLibrary;

	private URLEntry updateSite;
	private ArrayList discoverySites;

	private ArrayList entries;
	private String providerName;

	private String location;

	private Map localizations;

	public Feature(String id, String version) {
		if (id == null)
			throw new IllegalArgumentException();
		this.id = id;
		this.version = version;
	}

	public void addDiscoverySite(String siteLabel, String url) {
		if (siteLabel == null && url == null)
			return;

		if (this.discoverySites == null)
			this.discoverySites = new ArrayList();

		URLEntry entry = new URLEntry(url, siteLabel);
		this.discoverySites.add(entry);
	}

	public void addEntry(FeatureEntry plugin) {
		if (entries == null)
			entries = new ArrayList();
		entries.add(plugin);
	}

	public String getApplication() {
		return application;
	}

	public String getCopyright() {
		if (copyright != null)
			return copyright.getAnnotation();
		return null;
	}

	public String getCopyrightURL() {
		if (copyright != null)
			return copyright.getURL();
		return null;
	}

	public String getDescription() {
		if (description != null)
			return description.getAnnotation();
		return null;
	}

	public String getDescriptionURL() {
		if (description != null)
			return description.getURL();
		return null;
	}

	public URLEntry[] getDiscoverySites() {
		if (discoverySites == null)
			return new URLEntry[0];
		return (URLEntry[]) discoverySites.toArray(new URLEntry[discoverySites.size()]);
	}

	public FeatureEntry[] getEntries() {
		if (entries == null)
			return new FeatureEntry[0];
		return (FeatureEntry[]) entries.toArray(new FeatureEntry[entries.size()]);
	}

	public String getId() {
		return id;
	}

	public String getInstallHandler() {
		return installHandler;
	}

	public String getInstallHandlerLibrary() {
		return installHandlerLibrary;
	}

	public String getInstallHandlerURL() {
		return installHandlerURL;
	}

	public String getLabel() {
		return label;
	}

	public String getLicense() {
		if (license != null)
			return license.getAnnotation();
		return null;
	}

	public String getLicenseURL() {
		if (license != null)
			return license.getURL();
		return null;
	}

	public Map getLocalizations() {
		return this.localizations;
	}

	public String getLocation() {
		return this.location;
	}

	public String getPlugin() {
		return pluginId;
	}

	public String getProviderName() {
		return providerName;
	}

	public URLEntry getUpdateSite() {
		return updateSite;
	}

	public String getVersion() {
		return version;
	}

	public boolean isExclusive() {
		return exclusive;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public void setCopyright(String copyright) {
		if (this.copyright == null)
			this.copyright = new URLEntry();
		this.copyright.setAnnotation(copyright);
	}

	public void setCopyrightURL(String copyrightURL) {
		if (this.copyright == null)
			this.copyright = new URLEntry();
		this.copyright.setURL(copyrightURL);
	}

	public void setDescription(String description) {
		if (this.description == null)
			this.description = new URLEntry();
		this.description.setAnnotation(description);
	}

	public void setDescriptionURL(String descriptionURL) {
		if (this.description == null)
			this.description = new URLEntry();
		this.description.setURL(descriptionURL);
	}

	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	public void setInstallHandler(String installHandler) {
		this.installHandler = installHandler;
	}

	public void setInstallHandlerLibrary(String installHandlerLibrary) {
		this.installHandlerLibrary = installHandlerLibrary;
	}

	public void setInstallHandlerURL(String installHandlerURL) {
		this.installHandlerURL = installHandlerURL;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setLicense(String license) {
		if (this.license == null)
			this.license = new URLEntry();
		this.license.setAnnotation(license);
	}

	public void setLicenseURL(String licenseURL) {
		if (this.license == null)
			this.license = new URLEntry();
		this.license.setURL(licenseURL);
	}

	public void setLocalizations(Map localizations) {
		this.localizations = localizations;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void setPlugin(String pluginId) {
		this.pluginId = pluginId;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public void setProviderName(String value) {
		providerName = value;
	}

	public void setUpdateSiteLabel(String updateSiteLabel) {
		if (this.updateSite == null)
			this.updateSite = new URLEntry();
		this.updateSite.setAnnotation(updateSiteLabel);
	}

	public void setUpdateSiteURL(String updateSiteURL) {
		if (this.updateSite == null)
			this.updateSite = new URLEntry();
		this.updateSite.setURL(updateSiteURL);
	}

	public void setURL(String value) {
		//
	}

	/**
	 * For debugging purposes only.
	 */
	public String toString() {
		return "Feature " + id + " version: " + version; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
