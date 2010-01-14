/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
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
		if (url1 == url2)
			return true;
		if (url1 == null ^ url2 == null)
			return false;

		// check if URL are file: URL as we may
		// have 2 URL pointing to the same featureReference
		// but with different representation
		// (i.e. file:/C;/ and file:C:/)
		final boolean isFile1 = "file".equalsIgnoreCase(url1.getProtocol());//$NON-NLS-1$
		final boolean isFile2 = "file".equalsIgnoreCase(url2.getProtocol());//$NON-NLS-1$
		if (isFile1 && isFile2) {
			File file1 = new File(url1.getFile());
			File file2 = new File(url2.getFile());
			return file1.equals(file2);
		}
		// URL1 xor URL2 is a file, return false. (They either both need to be files, or neither)
		if (isFile1 ^ isFile2)
			return false;
		return getExternalForm(url1).equals(getExternalForm(url2));
	}

	/**
	 * Gets the external form of this URL. In particular, it trims any white space, 
	 * removes a trailing slash and creates a lower case string.
	 */
	private static String getExternalForm(URL url) {
		String externalForm = url.toExternalForm();
		if (externalForm == null)
			return ""; //$NON-NLS-1$
		externalForm = externalForm.trim();
		if (externalForm.endsWith("/")) { //$NON-NLS-1$
			// Remove the trailing slash
			externalForm = externalForm.substring(0, externalForm.length() - 1);
		}
		return externalForm.toLowerCase();
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
		if (!(object instanceof SiteFeature))
			return false;
		SiteFeature that = (SiteFeature) object;
		if (this.featureId == null) {
			if (that.featureId != null)
				return false;
		} else if (!this.featureId.equals(that.featureId))
			return false;
		if (this.featureVersion == null) {
			if (that.featureVersion != null)
				return false;
		} else if (!this.featureVersion.equals(that.featureVersion))
			return false;
		return sameURL(this.getURL(), that.getURL());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (featureId == null ? 0 : featureId.hashCode());
		result = prime * result + (featureVersion == null ? 0 : featureVersion.hashCode());
		if (this.getURL() == null)
			return result;

		if ("file".equalsIgnoreCase(getURL().getProtocol())) {//$NON-NLS-1$ 
			// If the URL is a file, then create the HashCode from the file
			File f = new File(getURL().getFile());
			if (f != null)
				result = prime * result + f.hashCode();
		} else
			// Otherwise create it from the External form of the URL (in lower case)
			result = prime * result + getExternalForm(this.getURL()).hashCode();
		return result;
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
