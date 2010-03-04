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
 * A provisioning context defines the scope in which a provisioning operation
 * occurs. A context can be used to specify the set of repositories available
 * to the planner and engine as they perform provisioning work.
 * @since 2.0
 */
public class ProvisioningContext {
	private Collection<IRequirement> additionalRequirements;
	private URI[] artifactRepositories; //artifact repositories to consult
	private final List<IInstallableUnit> extraIUs = Collections.synchronizedList(new ArrayList<IInstallableUnit>());
	private URI[] metadataRepositories; //metadata repositories to consult
	private final Map<String, String> properties = new HashMap<String, String>();

	/**
	 * Creates a new provisioning context that includes all available metadata and
	 * artifact repositories.
	 */
	public ProvisioningContext() {
		// null repos means look at them all
		metadataRepositories = null;
		artifactRepositories = null;
	}

	/**
	 * Creates a new provisioning context that includes only the specified metadata
	 * repositories, and all available artifact repositories.
	 * 
	 * @param metadataRepositories the metadata repositories to include
	 */
	public ProvisioningContext(URI[] metadataRepositories) {
		this.metadataRepositories = metadataRepositories;
	}

	/**
	 * Returns a collection of additional requirements that must be satisfied by the planner,
	 * or <code>null</code> if there are no additional requirements.
	 * This method has no effect on the execution of the engine.
	 * 
	 * @return a collection of additional requirements
	 */
	public Collection<IRequirement> getAdditionalRequirements() {
		return additionalRequirements;
	}

	/**
	 * Returns the locations of the artifact repositories that are included in this
	 * provisioning context, or <code>null</code> to indicate that all available
	 * artifact repositories are included
	 * 
	 * @return The artifact repositories to use, or <code>null</code>
	 */
	public URI[] getArtifactRepositories() {
		return artifactRepositories;
	}

	/**
	 * Returns the list of additional installable units that should be considered as
	 * available for installation by the planner. Returns an empty list if
	 * there are no extra installable units to consider. This method has no effect on the
	 * execution of the engine.
	 * 
	 * @return The extra installable units that are available
	 */
	public List<IInstallableUnit> getExtraInstallableUnits() {
		return extraIUs;
	}

	/**
	 * Returns the locations of the metadata repositories that are included in this
	 * provisioning context, or <code>null</code> to indicate that all available
	 * metadata repositories are included
	 * 
	 * @return The metadata repositories to use, or <code>null</code>
	 */
	public URI[] getMetadataRepositories() {
		return metadataRepositories;
	}

	/**
	 * Returns the properties that are defined in this context. Context properties can
	 * be used to influence the behavior of either the planner or engine.
	 * 
	 * @return the defined provisioning context properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Returns the value of the property with the given key, or <code>null</code>
	 * if no such property is defined
	 * @param key the property key
	 * @return the property value, or <code>null</code>
	 */
	public String getProperty(String key) {
		return properties.get(key);
	}

	/**
	 * Sets the additional requirements that must be satisfied by the planner.
	 * This method has no effect on the execution of the engine.
	 * 
	 * @param requirements the additional requirements
	 */
	public void setAdditionalRequirements(Collection<IRequirement> requirements) {
		additionalRequirements = requirements;
	}

	/**
	 * Sets the artifact repositories to consult when performing an operation.
	 * @param artifactRepositories the artifact repository locations
	*/
	public void setArtifactRepositories(URI[] artifactRepositories) {
		this.artifactRepositories = artifactRepositories;
	}

	/**
	 * Sets the list of additional installable units that should be considered as
	 * available for installation by the planner. This method has no effect on the
	 * execution of the engine.
	 * @param extraIUs the extra installable units
	 */
	public void setExtraInstallableUnits(List<IInstallableUnit> extraIUs) {
		this.extraIUs.clear();
		//copy the list to prevent future client tampering
		if (extraIUs != null)
			this.extraIUs.addAll(extraIUs);
	}

	/**
	 * Sets a property on this provisioning context. Context properties can
	 * be used to influence the behavior of either the planner or engine.
	 * @param key the property key
	 * @param value the property value
	 */
	public void setProperty(String key, String value) {
		properties.put(key, value);
	}
}
