/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 * 	IBM Corporation - initial API and implementation
 *     WindRiver - https://bugs.eclipse.org/bugs/show_bug.cgi?id=227372
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.net.URI;
import java.util.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

/**
 * @since 2.0
 */
public class ProvisioningContext {
	private URI[] metadataRepositories; //metadata repositories to consult
	private URI[] artifactRepositories; //artifact repositories to consult
	private Map<String, String> properties = new HashMap<String, String>();
	private List<IInstallableUnit> extraIUs = new ArrayList<IInstallableUnit>();
	private Collection<IRequirement> additionalRequirements;

	public ProvisioningContext() {
		// null repos means look at them all
		metadataRepositories = null;
		artifactRepositories = null;
	}

	public ProvisioningContext(URI[] metadataRepositories) {
		this.metadataRepositories = metadataRepositories;
	}

	/**
	 * Artifact repositories to consult when performing an operation.
	 * @param artifactRepositories array of URLs
	*/
	public void setArtifactRepositories(URI[] artifactRepositories) {
		this.artifactRepositories = artifactRepositories;
	}

	public URI[] getMetadataRepositories() {
		return metadataRepositories;
	}

	public URI[] getArtifactRepositories() {
		return artifactRepositories;
	}

	public String getProperty(String key) {
		return properties.get(key);
	}

	public void setProperty(String key, String value) {
		properties.put(key, value);
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public List<IInstallableUnit> getExtraIUs() {
		return extraIUs;
	}

	public void setExtraIUs(List<IInstallableUnit> extraIUs) {
		this.extraIUs = extraIUs;
	}

	public void setAdditionalRequirements(Collection<IRequirement> requirements) {
		additionalRequirements = requirements;
	}

	public Collection<IRequirement> getAdditionalRequirements() {
		return additionalRequirements;
	}
}
