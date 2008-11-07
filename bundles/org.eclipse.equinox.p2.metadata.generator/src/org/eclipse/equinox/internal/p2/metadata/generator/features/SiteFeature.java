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
package org.eclipse.equinox.internal.p2.metadata.generator.features;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A reference to a feature in an update site.xml file.
 * 
 * Based on org.eclipse.update.core.model.FeatureReferenceModel.
 */
public class SiteFeature {

	// performance
	private URL base;
	private List /* of String*/categoryNames;
	private String featureId;
	private String featureVersion;

	private final boolean resolved = false;
	private URL url;
	private String urlString;

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
	 * Returns the resolved URL for the feature reference.
	 * 
	 * @return url string
	 */
	public URL getURL() {
		delayedResolve();
		return url;
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
