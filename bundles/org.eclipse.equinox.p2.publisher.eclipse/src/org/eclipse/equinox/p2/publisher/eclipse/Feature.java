/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     SAP AG - consolidation of publishers for PDE formats
 *******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * 
 * Feature information
 */
public class Feature implements IPlatformEntry {

	private final String id;
	private String version;
	private String label;
	private String image;
	private String brandingPlugin;
	private boolean primary = false;
	private boolean exclusive = false;
	private String application;
	private String colocationAffinity;

	private URLEntry description;
	private URLEntry license;
	private String licenseFeature;
	private String licenseFeatureVersion;
	private URLEntry copyright;

	private String installHandler;
	private String installHandlerURL;
	private String installHandlerLibrary;

	private URLEntry updateSite;
	private ArrayList<URLEntry> discoverySites;

	private ArrayList<FeatureEntry> entries;
	private String providerName;
	private String os;
	private String ws;
	private String arch;
	private String nl;

	private String location;

	private Map<Locale, Map<String, String>> localizations;

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
			this.discoverySites = new ArrayList<URLEntry>();

		URLEntry entry = new URLEntry(url, siteLabel);
		this.discoverySites.add(entry);
	}

	public void addEntry(FeatureEntry plugin) {
		if (entries == null)
			entries = new ArrayList<FeatureEntry>();
		entries.add(plugin);
	}

	public String getApplication() {
		return application;
	}

	public String getArch() {
		return arch;
	}

	public String getBrandingPlugin() {
		return brandingPlugin;
	}

	public String getColocationAffinity() {
		return colocationAffinity;
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
		return discoverySites.toArray(new URLEntry[discoverySites.size()]);
	}

	public FeatureEntry[] getEntries() {
		if (entries == null)
			return new FeatureEntry[0];
		return entries.toArray(new FeatureEntry[entries.size()]);
	}

	public String getId() {
		return id;
	}

	public String getImage() {
		return image;
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

	public String getLicenseFeature() {
		return licenseFeature;
	}

	public String getLicenseFeatureVersion() {
		return licenseFeatureVersion;
	}

	public String getLicenseURL() {
		if (license != null)
			return license.getURL();
		return null;
	}

	public Map<Locale, Map<String, String>> getLocalizations() {
		return this.localizations;
	}

	public String getLocation() {
		return this.location;
	}

	public String getNL() {
		return nl;
	}

	public String getOS() {
		return os;
	}

	public String getProviderName() {
		return providerName;
	}

	public URLEntry getUpdateSite() {
		return updateSite;
	}

	public String getUpdateSiteLabel() {
		if (updateSite != null)
			return updateSite.getAnnotation();
		return null;
	}

	public String getUpdateSiteURL() {
		if (updateSite != null)
			return updateSite.getURL();
		return null;
	}

	public String getVersion() {
		return version;
	}

	public String getWS() {
		return ws;
	}

	public boolean isExclusive() {
		return exclusive;
	}

	public boolean isPrimary() {
		return primary;
	}

	public boolean removeEntry(FeatureEntry entry) {
		if (entry == null || entries == null)
			return false;
		return entries.remove(entry);
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public void setBrandingPlugin(String brandingPlugin) {
		this.brandingPlugin = brandingPlugin;
	}

	public void setColocationAffinity(String colocationAffinity) {
		this.colocationAffinity = colocationAffinity;
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

	public void setEnvironment(String os, String ws, String arch, String nl) {
		this.os = os;
		this.ws = ws;
		this.arch = arch;
		this.nl = nl;
	}

	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	public void setImage(String image) {
		this.image = image;
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

	public void setLicenseFeature(String name) {
		this.licenseFeature = name;
	}

	public void setLicenseFeatureVersion(String version) {
		this.licenseFeatureVersion = version;
	}

	public void setLicenseURL(String licenseURL) {
		if (this.license == null)
			this.license = new URLEntry();
		this.license.setURL(licenseURL);
	}

	public void setLocalizations(Map<Locale, Map<String, String>> localizations) {
		this.localizations = localizations;
	}

	public void setLocation(String location) {
		this.location = location;
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

	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * For debugging purposes only.
	 */
	@Override
	public String toString() {
		return "Feature " + id + " version: " + version; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
