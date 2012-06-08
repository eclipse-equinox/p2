/*******************************************************************************
 * Copyright (c) 2012, Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat) - Initial API & implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class SiteBundle {

	private String arch;
	// performance
	private URL base;
	private List<String> categoryNames;
	private String bundleId;
	private String bundleVersion;
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
	 */
	public static boolean sameURL(URL url1, URL url2) {
		if (url1 == url2)
			return true;
		if (url1 == null ^ url2 == null)
			return false;

		// check if URL are file: URL as we may
		// have 2 URL pointing to the same bundleReference
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
	 * Creates an uninitialized bundle reference model object.
	 */
	public SiteBundle() {
		super();
	}

	/**
	 * Adds the name of a category this bundle belongs to.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param categoryName category name
	 */
	public void addCategoryName(String categoryName) {
		if (this.categoryNames == null)
			this.categoryNames = new ArrayList<String>();
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
	 * Compares 2 bundle reference models for equality
	 *  
	 * @param object bundle reference model to compare with
	 * @return <code>true</code> if the two models are equal, 
	 * <code>false</code> otherwise
	 */
	public boolean equals(Object object) {
		if (object == null)
			return false;
		if (!(object instanceof SiteBundle))
			return false;
		SiteBundle that = (SiteBundle) object;
		if (this.bundleId == null) {
			if (that.bundleId != null)
				return false;
		} else if (!this.bundleId.equals(that.bundleId))
			return false;
		if (this.bundleVersion == null) {
			if (that.bundleVersion != null)
				return false;
		} else if (!this.bundleVersion.equals(that.bundleVersion))
			return false;
		if (this.label == null) {
			if (that.label != null)
				return false;
		} else if (!this.label.equals(that.label))
			return false;
		return sameURL(this.getURL(), that.getURL());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (bundleId == null ? 0 : bundleId.hashCode());
		result = prime * result + (bundleVersion == null ? 0 : bundleVersion.hashCode());
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
	 * Returns the names of categories the referenced bundle belongs to.
	 * 
	 * @return an array of names, or an empty array.
	 */
	public String[] getCategoryNames() {
		if (categoryNames == null)
			return new String[0];

		return categoryNames.toArray(new String[0]);
	}

	/**
	 * Returns the bundle identifier as a string
	 * 
	 * @return bundle identifier
	 */
	public String getBundleIdentifier() {
		return bundleId;
	}

	/**
	 * Returns the bundle version as a string
	 * 
	 * @return bundle version 
	 */
	public String getBundleVersion() {
		return bundleVersion;
	}

	/**
	 * Retrieve the displayable label for the bundle reference. If the model
	 * object has been resolved, the label is localized.
	 *
	 * @return displayable label, or <code>null</code>.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Retrieve the non-localized displayable label for the bundle reference.
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
	 * Returns the referenced bundle type.
	 * 
	 * @return bundle type, or <code>null</code> representing the default
	 * bundle type for the site
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the resolved URL for the bundle reference.
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
	 * Sets the names of categories this bundle belongs to.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param categoryNames an array of category names
	 */
	public void setCategoryNames(String[] categoryNames) {
		if (categoryNames == null)
			this.categoryNames = null;
		else
			this.categoryNames = new ArrayList<String>(Arrays.asList(categoryNames));
	}

	/**
	 * Sets the bundle identifier.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param bundleId bundle identifier
	 */
	public void setBundleIdentifier(String bundleId) {
		this.bundleId = bundleId;
	}

	/**
	 * Sets the bundle version.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param bundleVersion bundle version
	 */
	public void setBundleVersion(String bundleVersion) {
		this.bundleVersion = bundleVersion;
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
	 * Sets the referenced bundle type.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param type referenced bundle type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Sets the unresolved URL for the bundle reference.
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
