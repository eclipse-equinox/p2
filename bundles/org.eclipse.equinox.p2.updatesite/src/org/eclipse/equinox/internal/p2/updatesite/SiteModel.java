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
package org.eclipse.equinox.internal.p2.updatesite;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.equinox.p2.publisher.eclipse.URLEntry;

/**
 * A model of an update site.
 * 
 * Copied from org.eclipse.update.core.model.SiteModel.
 */
public class SiteModel {

	private List /*of ArchiveReferenceModel*/archiveReferences;
	/**
	 * Map of String (category id) -> SiteCategory
	 */
	private Map categories;
	private URLEntry description;
	/**
	 * Map of String (feature id) -> SiteFeature
	 */
	private List features;
	private URI locationURI;
	private String locationURIString;
	private String mirrorsURIString;
	private boolean supportsPack200;
	private String type;
	private URLEntry[] associateSites;
	private String digestURIString;
	private List messageKeys;
	private Map localizations;

	/**
	 * Creates an uninitialized site model object.
	 * 
	 * @since 2.0
	 */
	public SiteModel() {
		super();
	}

	/**
	 * Adds an archive reference model to site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param archiveReference archive reference model
	 * @since 2.0
	 */
	public void addArchive(URLEntry archiveReference) {
		if (this.archiveReferences == null)
			this.archiveReferences = new ArrayList();
		if (!this.archiveReferences.contains(archiveReference))
			this.archiveReferences.add(archiveReference);
	}

	/**
	 * Adds a category to the site.
	 * 
	 * @param category category model
	 */
	public void addCategory(SiteCategory category) {
		if (categories == null)
			categories = new HashMap();
		if (!categories.containsKey(category.getName())) {
			categories.put(category.getName(), category);
			if (localizations != null && !localizations.isEmpty())
				category.setLocalizations(localizations);
		}
	}

	/**
	 * Adds a feature reference model to site.
	 * 
	 * @param featureReference feature reference model
	 */
	public void addFeature(SiteFeature featureReference) {
		if (this.features == null)
			this.features = new ArrayList();
		this.features.add(featureReference);
	}

	/**
	 * Returns an array of plug-in and non-plug-in archive reference models
	 * on this site
	 * 
	 * @return an array of archive reference models, or an empty array if there are
	 * no archives known to this site.
	 * @since 2.0
	 */
	public URLEntry[] getArchives() {
		if (archiveReferences == null || archiveReferences.size() == 0)
			return new URLEntry[0];

		return (URLEntry[]) archiveReferences.toArray(new URLEntry[0]);
	}

	public URLEntry[] getAssociatedSites() {
		return associateSites;
	}

	/**
	 * Returns an array of category models for this site.
	 * 
	 * @return array of site category models, or an empty array.
	 * @since 2.0
	 */
	public SiteCategory[] getCategories() {
		if (categories == null || categories.size() == 0)
			return new SiteCategory[0];
		return (SiteCategory[]) categories.values().toArray(new SiteCategory[0]);
	}

	/**
	 * Returns the category with the given name.
	 * @return the category with the given name, or <code>null</code>
	 */
	public SiteCategory getCategory(String name) {
		return (SiteCategory) (categories == null ? null : categories.get(name));
	}

	/**
	 * Returns the site description.
	 * 
	 * @return site description, or <code>null</code>.
	 */
	public URLEntry getDescription() {
		return description;
	}

	/**
	 * Returns an array of feature reference models on this site.
	 * 
	 * @return an array of feature reference models, or an empty array.
	 */
	public SiteFeature[] getFeatures() {
		if (features == null || features.size() == 0)
			return new SiteFeature[0];
		return (SiteFeature[]) features.toArray(new SiteFeature[0]);
	}

	/**
	 * Gets the localizations for the site as a map from locale
	 * to the set of translated properties for that locale.
	 * 
	 * @return a map from locale to property set
	 * @since 3.4
	 */
	public Map getLocalizations() {
		return this.localizations;
	}

	/**
	 * Returns the resolved URI for the site.
	 * 
	 * @return url, or <code>null</code>
	 */
	public URI getLocationURI() {
		if (locationURI == null && locationURIString != null) {
			try {
				locationURI = new URI(locationURIString);
			} catch (URISyntaxException e) {
				//ignore and return null
			}
		}
		return locationURI;
	}

	/**
	 * Returns the unresolved URI string for the site.
	 *
	 * @return url string, or <code>null</code>
	 */
	public String getLocationURIString() {
		return locationURIString;
	}

	/**
	 * Return the keys for translatable strings
	 *
	 * @return the list of keys for translatable strings; may be null
	 * @since 3.4
	 */
	public List getMessageKeys() {
		return messageKeys;
	}

	/**
	 * Returns the URI from which the list of mirrors of this site can be retrieved.
	 * 
	 * @since org.eclipse.equinox.p2.metadata.generator 1.0
	 */
	public String getMirrorsURI() {
		return mirrorsURIString;
	}

	/** 
	 * Returns the site type.
	 * 
	 * @return site type, or <code>null</code>.
	 * @since 2.0
	 */
	public String getType() {
		return type;
	}

	public boolean isPack200Supported() {
		return supportsPack200;
	}

	/**
	 * Sets the site description.
	 * 
	 * @param description site description
	 * @since 2.0
	 */
	public void setDescription(URLEntry description) {
		this.description = description;
	}

	/**
	 * Sets the localizations for the site as a map from locale
	 * to the set of translated properties for that locale.
	 * 
	 * @param localizations as a map from locale to property set
	 * @since 3.4
	 */
	public void setLocalizations(Map localizations) {
		this.localizations = localizations;
		if (localizations != null && !localizations.isEmpty() && //
				categories != null && !categories.isEmpty()) {
			for (Iterator catIter = categories.entrySet().iterator(); catIter.hasNext();) {
				Map.Entry entry = (Map.Entry) catIter.next();
				SiteCategory category = (SiteCategory) entry.getValue();
				category.setLocalizations(localizations);
			}
		}
	}

	/**
	 * Sets the unresolved URI for the site.
	 * 
	 * @param locationURIString url for the site (as a string)
	 * @since 2.0
	 */
	public void setLocationURIString(String locationURIString) {
		this.locationURIString = locationURIString;
	}

	/**
	 * Sets keys for translatable strings
	 * 
	 * @param keys for translatable strings
	 * @since 3.4
	 */
	public void setMessageKeys(List keys) {
		this.messageKeys = keys;
	}

	/**
	 * Sets the mirrors url. Mirror sites will then be obtained from this mirror url later.
	 * This method is complementary to setMirrorsiteEntryModels(), and only one of these 
	 * methods should be called.
	 * 
	 * @param mirrorsURI additional update site mirrors
	 * @since 3.1
	 */
	public void setMirrorsURIString(String mirrorsURI) {
		this.mirrorsURIString = mirrorsURI;
	}

	public void setSupportsPack200(boolean value) {
		this.supportsPack200 = value;
	}

	/**
	 * Sets the site type.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param type site type
	 * @since 2.0
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Sets the associated sites for this update site.
	 * 
	 * @param associateSites the associated sites
	 */
	public void setAssociateSites(URLEntry[] associateSites) {
		this.associateSites = associateSites;
	}

	public void setDigestURIString(String digestURIString) {
		this.digestURIString = digestURIString;
	}

	public String getDigestURIString() {
		return digestURIString;
	}
}
