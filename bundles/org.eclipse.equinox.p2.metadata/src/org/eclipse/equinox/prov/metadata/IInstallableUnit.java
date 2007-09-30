/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.prov.metadata;

import org.osgi.framework.Version;

public interface IInstallableUnit extends Comparable {

	/**
	 * A capability namespace representing a particular kind of installable unit.
	 * For example, an InstallableUnit may specify that it provides the "group" kind
	 * capability to express that it represents a group of instalable units. 
	 */
	public static final String IU_KIND_NAMESPACE = "org.eclipse.equinox.prov.type"; //$NON-NLS-1$
	/**
	 * A capability namespace representing a particular InstallableUnit by name.
	 * Each InstallableUnit automatically provides an instance of this namespace representing
	 * itself, and other InstallableUnits can require such a capability to state that they
	 * require a particular InstallableUnit to be present.
	 */
	public static final String IU_NAMESPACE = "org.eclipse.equinox.prov.iunamespace"; //$NON-NLS-1$
	/**
	 * A capability namespace representing a particular profile flavor.
	 */
	public static final String FLAVOR_NAMESPACE = "flavor"; //$NON-NLS-1$
	//These two constants needs to be moved somewhere more appropriate...
	public static final String CAPABILITY_ECLIPSE_TYPES = "org.eclipse.equinox.prov.eclipsetouchpoint.types"; //$NON-NLS-1$
	public static final String CAPABILITY_ECLIPSE_BUNDLE = "bundle"; //$NON-NLS-1$

	public abstract TouchpointType getTouchpointType();

	public abstract String getId();

	/**
	 * Returns the filter on this installable unit. The filter is matched against
	 * the selection context of the profile the unit is installed into. An IU will not
	 * be installed if it has a filter condition that is not satisfied by the context.
	 * 
	 * See Profile#getSelectionContext.
	 */
	public abstract String getFilter();

	public abstract Version getVersion();

	public abstract IArtifactKey[] getArtifacts();

	public abstract RequiredCapability[] getRequiredCapabilities();

	//	public ProvidedCapability[] getProvidedCapabilities() {
	//		return providedCapabilities;
	//	}
	//	
	public abstract ProvidedCapability[] getProvidedCapabilities();

	public abstract boolean isSingleton();

	public abstract String getProperty(String key);

	public abstract TouchpointData[] getTouchpointData();

	public abstract boolean isFragment();

	public abstract String getApplicabilityFilter();

	public abstract void accept(IMetadataVisitor visitor);
}