/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.update;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.p2.reconciler.dropins.Activator;

/*
 * Represents a site in a platform.xml file.
 */
public class Site {

	public final static String POLICY_MANAGED_ONLY = "MANAGED-ONLY"; //$NON-NLS-1$
	public final static String POLICY_USER_EXCLUDE = "USER-EXCLUDE"; //$NON-NLS-1$
	public final static String POLICY_USER_INCLUDE = "USER-INCLUDE"; //$NON-NLS-1$

	private String policy;
	private boolean enabled;
	private boolean updateable;
	private String url;
	private String linkFile;
	private List features = new ArrayList();
	private List list = new ArrayList();

	public void addFeature(Feature feature) {
		this.features.add(feature);
	}

	public void addPlugin(String plugin) {
		this.list.add(plugin);
	}

	public Feature[] getFeatures() {
		return (Feature[]) features.toArray(new Feature[features.size()]);
	}

	public Feature removeFeature(String featureURL) {
		for (int i = 0; i < features.size(); i++) {
			String nextURL = ((Feature) features.get(i)).getUrl();
			if (nextURL != null && nextURL.equals(featureURL)) {
				return (Feature) features.remove(i);
			}
		}
		return null;
	}

	public String getLinkFile() {
		return linkFile;
	}

	public String[] getList() {
		return (String[]) list.toArray(new String[list.size()]);
	}

	public String getPolicy() {
		return policy;
	}

	public String getUrl() {
		return url;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isUpdateable() {
		return updateable;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setLinkFile(String linkFile) {
		this.linkFile = linkFile;
	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getUrl().hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof Site))
			return false;
		Site other = (Site) obj;
		if (isEnabled() != other.isEnabled())
			return false;
		if (isUpdateable() != other.isUpdateable())
			return false;
		if (!getUrl().equals(other.getUrl()))
			return false;
		if (!Activator.equals(getLinkFile(), other.getLinkFile()))
			return false;
		if (!Activator.equals(getPolicy(), other.getPolicy()))
			return false;
		if (!Activator.equals(getList(), other.getList()))
			return false;
		if (!Activator.equals(getFeatures(), other.getFeatures()))
			return false;
		return true;
	}
}
