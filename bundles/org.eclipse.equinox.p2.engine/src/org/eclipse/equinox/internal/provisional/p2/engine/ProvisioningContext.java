/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * 	IBM Corporation - initial API and implementation
 *     WindRiver - https://bugs.eclipse.org/bugs/show_bug.cgi?id=227372
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.engine;

import java.net.URL;
import java.util.*;

public class ProvisioningContext {
	private URL[] metadataRepositories; //metadata repositories to consult
	private URL[] artifactRepositories; //artifact repositories to consult
	private Properties properties = new Properties();
	private List extraIUs = new ArrayList();

	public ProvisioningContext() {
		// null repos means look at them all
		metadataRepositories = null;
		artifactRepositories = null;
	}

	public ProvisioningContext(URL[] metadataRepositories) {
		this.metadataRepositories = metadataRepositories;
	}

	/**
	 * Artifact repositories to consult when performing an operation.
	 * @param artifactRepositories array of URLs
	*/
	public void setArtifactRepositories(URL[] artifactRepositories) {
		this.artifactRepositories = artifactRepositories;
	}

	public URL[] getMetadataRepositories() {
		return metadataRepositories;
	}

	public URL[] getArtifactRepositories() {
		return artifactRepositories;
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	public List getExtraIUs() {
		return extraIUs;
	}

	public void setExtraIUs(List extraIUs) {
		this.extraIUs = extraIUs;
	}
}
