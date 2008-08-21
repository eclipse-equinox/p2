/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     James D Miles (IBM Corp.) - bug 191783, NullPointerException in FeatureDownloader
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * A reference to a feature in an update site.xml file.
 * 
 * Based on org.eclipse.update.core.model.FeatureReferenceModel.
 */
public class SiteFeature {

	private String arch;
	// performance
	private URL base;
	private List /* of String*/categoryNames;
	private String featureId;
	private String featureVersion;
	private String label;
	private String nl;

	private String os;
	private String patch;
	private final boolean resolved = false;
	private SiteModel site;
	private String type;
	private URL url;
	private String urlString;
	private String ws;

	/*
	 * Compares two URL for equality
	 * Return false if one of them is null
	 */
	public static boolean sameURL(URL url1, URL url2) {

		if (url1 == null || url2 == null)
			return false;
		if (url1 == url2)
			return true;
		if (url1.equals(url2))
			return true;

		// check if URL are file: URL as we may
		// have 2 URL pointing to the same featureReference
		// but with different representation
		// (i.e. file:/C;/ and file:C:/)
		if (!"file".equalsIgnoreCase(url1.getProtocol())) //$NON-NLS-1$
			return false;
		if (!"file".equalsIgnoreCase(url2.getProtocol())) //$NON-NLS-1$
			return false;

		File file1 = new File(url1.getFile());
		File file2 = new File(url2.getFile());

		if (file1 == null)
			return false;

		return (file1.equals(file2));
	}

	/**
	 * Creates an uninitialized feature reference model object.
	 */
	public SiteFeature() {
		super();
	}

	/**
	 * Adds the name of a category this feature belongs to.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param categoryName category name
	 */
	public void addCategoryName(String categoryName) {
		if (this.categoryNames == null)
			this.categoryNames = new ArrayList();
		if (!this.categoryNames.contains(categoryName))
			this.categoryNames.add(categoryName);
	}

	private void delayedResolve() {

		// PERF: delay resolution
		if (resolved)
			return;

		// resolve local elements
		try {
			url = new URL(base, urlString);
		} catch (MalformedURLException e) {
			//			UpdateCore.warn("", e); //$NON-NLS-1$
		}
	}

	/**
	 * Compares 2 feature reference models for equality
	 *  
	 * @param object feature reference model to compare with
	 * @return <code>true</code> if the two models are equal, 
	 * <code>false</code> otherwise
	 */
	public boolean equals(Object object) {

		if (object == null)
			return false;
		if (getURL() == null)
			return false;

		if (!(object instanceof SiteFeature))
			return false;

		SiteFeature f = (SiteFeature) object;

		return sameURL(getURL(), f.getURL());
	}

	/**
	 * Returns the names of categories the referenced feature belongs to.
	 * 
	 * @return an array of names, or an empty array.
	 */
	public String[] getCategoryNames() {
		if (categoryNames == null)
			return new String[0];

		return (String[]) categoryNames.toArray(new String[0]);
	}

	/**
	 * Returns the feature identifier as a string
	 * 
	 * @return feature identifier
	 */
	public String getFeatureIdentifier() {
		return featureId;
	}

	/**
	 * Returns the feature version as a string
	 * 
	 * @return feature version 
	 */
	public String getFeatureVersion() {
		return featureVersion;
	}

	/**
	 * Retrieve the displayable label for the feature reference. If the model
	 * object has been resolved, the label is localized.
	 *
	 * @return displayable label, or <code>null</code>.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Retrieve the non-localized displayable label for the feature reference.
	 *
	 * @return non-localized displayable label, or <code>null</code>.
	 */
	public String getLabelNonLocalized() {
		return label;
	}

	/**
	 * Get optional locale specification as a comma-separated string.
	 *
	 * @return the locale specification string, or <code>null</code>.
	 */
	public String getNL() {
		return nl;
	}

	/**
	 * Get optional operating system specification as a comma-separated string.
	 *
	 * @return the operating system specification string, or <code>null</code>.
	 */
	public String getOS() {
		return os;
	}

	/**
	 * Get optional system architecture specification as a comma-separated string.
	 *
	 * @return the system architecture specification string, or <code>null</code>.
	 */
	public String getOSArch() {
		return arch;
	}

	/**
	 * Returns the patch mode.
	 */
	public String getPatch() {
		return patch;
	}

	/**
	 * Returns the site model for the reference.
	 * 
	 * @return site model
	 * @since 2.0
	 */
	public SiteModel getSiteModel() {
		return site;
	}

	/**
	 * Returns the referenced feature type.
	 * 
	 * @return feature type, or <code>null</code> representing the default
	 * feature type for the site
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the resolved URL for the feature reference.
	 * 
	 * @return url string
	 */
	public URL getURL() {
		delayedResolve();
		return url;
	}

	/**
	 * Returns the unresolved URL string for the reference.
	 *
	 * @return url string
	 */
	public String getURLString() {
		return urlString;
	}

	/**
	 * Get optional windowing system specification as a comma-separated string.
	 *
	 * @return the windowing system specification string, or <code>null</code>.
	 */
	public String getWS() {
		return ws;
	}

	/**
	 * Resolve the model object.
	 * Any URL strings in the model are resolved relative to the 
	 * base URL argument. Any translatable strings in the model that are
	 * specified as translation keys are localized using the supplied 
	 * resource bundle.
	 * 
	 * @param resolveBase URL
	 * @param bundleURL resource bundle URL
	 * @exception MalformedURLException
	 */
	public void resolve(URL resolveBase, URL bundleURL) throws MalformedURLException {
		this.base = resolveBase;
	}

	/**
	 * Sets the system architecture specification.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param arch system architecture specification as a comma-separated list
	 */
	public void setArch(String arch) {
		this.arch = arch;
	}

	/**
	 * Sets the names of categories this feature belongs to.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param categoryNames an array of category names
	 */
	public void setCategoryNames(String[] categoryNames) {
		if (categoryNames == null)
			this.categoryNames = null;
		else
			this.categoryNames = new ArrayList(Arrays.asList(categoryNames));
	}

	/**
	 * Sets the feature identifier.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param featureId feature identifier
	 */
	public void setFeatureIdentifier(String featureId) {
		this.featureId = featureId;
	}

	/**
	 * Sets the feature version.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param featureVersion feature version
	 */
	public void setFeatureVersion(String featureVersion) {
		this.featureVersion = featureVersion;
	}

	/**
	 * Sets the label.
	 * @param label The label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Sets the locale specification.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param nl locale specification as a comma-separated list
	 */
	public void setNL(String nl) {
		this.nl = nl;
	}

	/**
	 * Sets the operating system specification.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param os operating system specification as a comma-separated list
	 */
	public void setOS(String os) {
		this.os = os;
	}

	/**
	 * Sets the patch mode.
	 */
	public void setPatch(String patch) {
		this.patch = patch;
	}

	/**
	 * Sets the site for the referenced.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param site site for the reference
	 */
	public void setSiteModel(SiteModel site) {
		this.site = site;
	}

	/**
	 * Sets the referenced feature type.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param type referenced feature type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Sets the unresolved URL for the feature reference.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param urlString unresolved URL string
	 */
	public void setURLString(String urlString) {
		this.urlString = urlString;
		this.url = null;
	}

	/**
	 * Sets the windowing system specification.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param ws windowing system specification as a comma-separated list
	 */
	public void setWS(String ws) {
		this.ws = ws;
	}

	/**
	 * @see Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(getClass().toString() + " :"); //$NON-NLS-1$
		buffer.append(" at "); //$NON-NLS-1$
		if (url != null)
			buffer.append(url.toExternalForm());
		return buffer.toString();
	}

}
