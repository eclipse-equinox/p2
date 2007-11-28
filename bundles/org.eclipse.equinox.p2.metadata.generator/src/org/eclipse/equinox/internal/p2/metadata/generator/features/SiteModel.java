/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import org.eclipse.equinox.p2.metadata.generator.URLEntry;

/**
 * Site model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 2.0
 */
public class SiteModel {

	private List /*of ArchiveReferenceModel*/archiveReferences;
	private Set /*of SiteCategory*/categories;
	private URLEntry description;
	private List /*of SiteFeature*/featureReferences;
	private URL locationURL;
	private String locationURLString;
	private List /* of URLEntry */mirrors;
	private String mirrorsURLString;
	private boolean supportsPack200;
	private String type;

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
	public void addArchiveReferenceModel(URLEntry archiveReference) {
		if (this.archiveReferences == null)
			this.archiveReferences = new ArrayList();
		if (!this.archiveReferences.contains(archiveReference))
			this.archiveReferences.add(archiveReference);
	}

	/**
	 * Adds a category model to site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param category category model
	 * @since 2.0
	 */
	public void addCategoryModel(SiteCategory category) {
		if (this.categories == null)
			this.categories = new TreeSet(SiteCategory.getComparator());
		if (!this.categories.contains(category))
			this.categories.add(category);
	}

	/**
	 * Adds a feature reference model to site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param featureReference feature reference model
	 * @since 2.0
	 */
	public void addFeatureReferenceModel(SiteFeature featureReference) {
		if (this.featureReferences == null)
			this.featureReferences = new ArrayList();
		// PERF: do not check if already present 
		//if (!this.featureReferences.contains(featureReference))
		this.featureReferences.add(featureReference);
	}

	/**
	 * Adds a mirror site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param mirror mirror model 
	 * @since 3.1
	 */
	public void addMirrorModel(URLEntry mirror) {
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
	public URLEntry[] getArchiveReferenceModels() {
		if (archiveReferences == null || archiveReferences.size() == 0)
			return new URLEntry[0];

		return (URLEntry[]) archiveReferences.toArray(new URLEntry[0]);
	}

	/**
	 * Returns an array of category models for this site.
	 * 
	 * @return array of site category models, or an empty array.
	 * @since 2.0
	 */
	public SiteCategory[] getCategoryModels() {
		if (categories == null || categories.size() == 0)
			return new SiteCategory[0];

		return (SiteCategory[]) categories.toArray(new SiteCategory[0]);
	}

	/**
	 * Returns the site description.
	 * 
	 * @return site description, or <code>null</code>.
	 * @since 2.0
	 */
	public URLEntry getDescriptionModel() {
		return description;
	}

	/**
	 * Returns an array of feature reference models on this site.
	 * 
	 * @return an array of feature reference models, or an empty array.
	 * @since 2.0
	 */
	public SiteFeature[] getFeatureReferenceModels() {
		if (featureReferences == null || featureReferences.size() == 0)
			return new SiteFeature[0];

		return (SiteFeature[]) featureReferences.toArray(new SiteFeature[0]);
	}

	/**
	 * Returns the resolved URL for the site.
	 * 
	 * @return url, or <code>null</code>
	 * @since 2.0
	 */
	public URL getLocationURL() {
		return locationURL;
	}

	/**
	 * Returns the unresolved URL string for the site.
	 *
	 * @return url string, or <code>null</code>
	 * @since 2.0
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
	public URLEntry[] getMirrorSiteEntryModels() {
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
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param description site description
	 * @since 2.0
	 */
	public void setDescriptionModel(URLEntry description) {
		this.description = description;
	}

	/**
	 * Sets the unresolved URL for the site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param locationURLString url for the site (as a string)
	 * @since 2.0
	 */
	public void setLocationURLString(String locationURLString) {
		this.locationURLString = locationURLString;
	}

	/**
	 * Sets additional mirror sites
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param mirrors additional update site mirrors
	 * @since 3.1
	 */
	public void setMirrorSiteEntryModels(URLEntry[] mirrors) {
		doSetMirrorSiteEntryModels(mirrors);
	}

	/**
	 * Sets the mirrors url. Mirror sites will then be obtained from this mirror url later.
	 * This method is complementary to setMirrorsiteEntryModels(), and only one of these 
	 * methods should be called.
	 * Throws a runtime exception if this object is marked read-only.
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
}
