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
package org.eclipse.equinox.internal.p2.metadata.generator.features;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.URLEntry;

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
	private URL locationURL;
	private String locationURLString;
	private List /* of URLEntry */mirrors;
	private String mirrorsURLString;
	private boolean supportsPack200;
	private String type;
	private URLEntry[] associateSites;
	private String digestURLString;

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
		if (!categories.containsKey(category.getName()))
			categories.put(category.getName(), category);
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
	 * Adds a mirror site.
	 * 
	 * @param mirror mirror model 
	 * @since 3.1
	 */
	public void addMirror(URLEntry mirror) {
		if (this.mirrors == null)
			this.mirrors = new ArrayList();
		if (!this.mirrors.contains(mirror))
			this.mirrors.add(mirror);
	}

	private void doSetMirrorSiteEntryModels(URLEntry[] newMirrors) {
		if (newMirrors == null || newMirrors.length == 0)
			this.mirrors = null;
		else
			this.mirrors = new ArrayList(Arrays.asList(newMirrors));
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
	 * Returns the resolved URL for the site.
	 * 
	 * @return url, or <code>null</code>
	 */
	public URL getLocationURL() {
		if (locationURL == null && locationURLString != null) {
			try {
				locationURL = new URL(locationURLString);
			} catch (MalformedURLException e) {
				//ignore and return null
			}
		}
		return locationURL;
	}

	/**
	 * Returns the unresolved URL string for the site.
	 *
	 * @return url string, or <code>null</code>
	 */
	public String getLocationURLString() {
		return locationURLString;
	}

	/**
	 * Return an array of update site mirrors
	 * 
	 * @return an array of mirror entries, or an empty array.
	 * @since 3.1
	 */
	public URLEntry[] getMirrors() {
		//delayedResolve(); no delay;
		if (mirrors == null || mirrors.size() == 0)
			// see if we can get mirrors from the provided url
			if (mirrorsURLString != null)
				doSetMirrorSiteEntryModels(DefaultSiteParser.getMirrors(mirrorsURLString));

		if (mirrors == null || mirrors.size() == 0)
			return new URLEntry[0];
		return (URLEntry[]) mirrors.toArray(new URLEntry[0]);
	}

	/**
	 * Returns the URL from which the list of mirrors of this site can be retrieved.
	 * 
	 * @since org.eclipse.equinox.p2.metadata.generator 1.0
	 */
	public String getMirrorsURL() {
		return mirrorsURLString;
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
	 * Resolve the model object.
	 * Any URL strings in the model are resolved relative to the 
	 * base URL argument. Any translatable strings in the model that are
	 * specified as translation keys are localized using the supplied 
	 * resource bundle.
	 * 
	 * @param base URL
	 * @param bundleURL resource bundle URL
	 * @exception MalformedURLException
	 * @since 2.0
	 */
	public void resolve(URL base, URL bundleURL) throws MalformedURLException {

		// Archives and feature are relative to location URL
		// if the Site element has a URL tag: see spec	
		//		locationURL = resolveURL(base, bundleURL, getLocationURLString());
		//		if (locationURL == null)
		//			locationURL = base;
		//		resolveListReference(getFeatureReferenceModels(), locationURL, bundleURL);
		//		resolveListReference(getArchiveReferenceModels(), locationURL, bundleURL);
		//
		//		resolveReference(getDescriptionModel(), base, bundleURL);
		//		resolveListReference(getCategoryModels(), base, bundleURL);
		//
		//		URL url = resolveURL(base, bundleURL, mirrorsURLString);
		//		if (url != null)
		//			mirrorsURLString = url.toString();
		//
		//		if ((this instanceof ExtendedSite) && ((ExtendedSite) this).isDigestExist()) {
		//			ExtendedSite extendedSite = (ExtendedSite) this;
		//			extendedSite.setLiteFeatures(UpdateManagerUtils.getLightFeatures(extendedSite));
		//		}
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
	 * Sets the unresolved URL for the site.
	 * 
	 * @param locationURLString url for the site (as a string)
	 * @since 2.0
	 */
	public void setLocationURLString(String locationURLString) {
		this.locationURLString = locationURLString;
	}

	/**
	 * Sets additional mirror sites
	 * 
	 * @param mirrors additional update site mirrors
	 * @since 3.1
	 */
	public void setMirrors(URLEntry[] mirrors) {
		doSetMirrorSiteEntryModels(mirrors);
	}

	/**
	 * Sets the mirrors url. Mirror sites will then be obtained from this mirror url later.
	 * This method is complementary to setMirrorsiteEntryModels(), and only one of these 
	 * methods should be called.
	 * 
	 * @param mirrorsURL additional update site mirrors
	 * @since 3.1
	 */
	public void setMirrorsURLString(String mirrorsURL) {
		this.mirrorsURLString = mirrorsURL;
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

	public void setDigestURLString(String digestURLString) {
		this.digestURLString = digestURLString;
	}

	public String getDigestURLString() {
		return digestURLString;
	}
}
