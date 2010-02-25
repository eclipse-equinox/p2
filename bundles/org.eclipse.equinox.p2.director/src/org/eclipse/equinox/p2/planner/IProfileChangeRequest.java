/*******************************************************************************
 *  Copyright (c) 2010 Sonatype, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.planner;

import java.util.Collection;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 *  A profile change request is a description of a set of changes to be performed on a profile.
 *  A profile change request is then 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface IProfileChangeRequest {

	/**
	 * Causes the installation of the mentioned IU.
	 * @param toInstall the entity to add to the profile
	 */
	public abstract void add(IInstallableUnit toInstall);

	/**
	 * Causes the installation of all the IUs mentioned
	 * @param toInstall the installableunits to be added to the profile
	 */
	public abstract void addAll(Collection<IInstallableUnit> toInstall);

	/**
	 * Causes the removals of all the IUs mentioned
	 * @param toUninstall the installableunits to be remove from the profile
	 */
	public abstract void remove(IInstallableUnit toUninstall);

	public abstract void removeAll(Collection<IInstallableUnit> toUninstall);

	/**
	 * Associate an inclusion rule with the installable unit. An inclusion rule will dictate whether the 
	 * @param iu
	 * @param inclusionRule 
	 */
	public abstract void setInstallableUnitInclusionRules(IInstallableUnit iu, String inclusionRule);

	public abstract void removeInstallableUnitInclusionRules(IInstallableUnit iu);

	//	public Object getOptionalInclusionRule();
	//
	//	public Object getStrictInclusionRule();

	/** 
	 * Set a global property on the profile
	 * @param key key of the property
	 * @param value value of the property
	 */
	public abstract void setProfileProperty(String key, String value);

	/** 
	 * Remove a global property on the profile
	 * @param key key of the property
	 */
	public abstract void removeProfileProperty(String key);

	/** 
	 * Associate a property with a given IU. 
	 * @param key key of the property
	 * @param value value of the property
	 */
	public abstract void setInstallableUnitProfileProperty(IInstallableUnit iu, String key, String value);

	/** 
	 * Remove a property with a given IU. 
	 * @param iu The installable until to remove a property for
	 * @param key key of the property
	 */
	public abstract void removeInstallableUnitProfileProperty(IInstallableUnit iu, String key);

	/**
	 *  Provide the set of installable units that have been requested for addition
	 * @return a collection of the installable units 
	 */
	public abstract Collection<IInstallableUnit> getAdditions();

	/**
	 *  Provide the set of installable units that have been requested for removal
	 * @return a collection of the installable units
	 */
	public abstract Collection<IInstallableUnit> getRemovals();

}