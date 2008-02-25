/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * 		IBM Corporation - initial API and implementation
 * 		Genuitec, LLC - added license support
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata;

import java.util.Map;
import org.osgi.framework.Version;

public interface IInstallableUnit extends Comparable {

	// TODO: These two constants need to be moved somewhere more appropriate...
	public static final String CAPABILITY_ECLIPSE_BUNDLE = "bundle"; //$NON-NLS-1$
	public static final String CAPABILITY_ECLIPSE_FEATURE = "feature"; //$NON-NLS-1$
	public static final String CAPABILITY_ECLIPSE_SOURCE = "source"; //$NON-NLS-1$
	public static final String CAPABILITY_ECLIPSE_TYPES = "org.eclipse.equinox.p2.eclipsetouchpoint.types"; //$NON-NLS-1$
	/**
	 * A capability namespace representing a particular profile flavor.
	 */
	public static final String NAMESPACE_FLAVOR = "flavor"; //$NON-NLS-1$

	/**
	 * A capability namespace representing a particular InstallableUnit by id.
	 * Each InstallableUnit automatically provides a capability in this namespace representing
	 * itself, and other InstallableUnits can require such a capability to state that they
	 * require a particular InstallableUnit to be present.
	 * 
	 * @see IInstallableUnit#getId()
	 */
	public static final String NAMESPACE_IU_ID = "org.eclipse.equinox.p2.iunamespace"; //$NON-NLS-1$

	public static final String PROP_TYPE_PROFILE = "org.eclipse.equinox.p2.type.profile"; //$NON-NLS-1$	 
	public static final String PROP_TYPE_CATEGORY = "org.eclipse.equinox.p2.type.category"; //$NON-NLS-1$
	public static final String PROP_TYPE_FRAGMENT = "org.eclipse.equinox.p2.type.fragment"; //$NON-NLS-1$
	public static final String PROP_TYPE_GROUP = "org.eclipse.equinox.p2.type.group"; //$NON-NLS-1$

	//TODO Move to UI
	public static final String PROP_PROFILE_ROOT_IU = "org.eclipse.equinox.p2.type.root"; //$NON-NLS-1$

	/**
	 * A property key (value <code>"equinox.p2.contact"</code>) representing a 
	 * String property containing a contact address where problems can be reported, 
	 * such as an email address.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_CONTACT = "equinox.p2.contact"; //$NON-NLS-1$
	/**
	 * A property key (value <code>"equinox.p2.description"</code>) representing a 
	 * String property containing a human-readable description of the installable unit.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_DESCRIPTION = "equinox.p2.description"; //$NON-NLS-1$
	/**
	 * A property key (value <code>"equinox.p2.doc.url"</code>) representing a 
	 * String property containing a URL for documentation about the installable unit.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_DOC_URL = "equinox.p2.doc.url"; //$NON-NLS-1$

	/**
	 * A property key (value <code>"equinox.p2.name"</code>) representing a 
	 * String property containing a human-readable name for the installable unit.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_NAME = "equinox.p2.name"; //$NON-NLS-1$
	/**
	 * A property key (value <code>"equinox.p2.provider"</code>) representing a 
	 * String property containing information about the vendor or provider of the 
	 * installable unit.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_PROVIDER = "equinox.p2.provider"; //$NON-NLS-1$
	/**
	 * A property key (value <code>"equinox.p2.update.site"</code>) representing a 
	 * String property containing the URL of the Web site or repository where updates for this 
	 * installable unit can be obtained.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_UPDATE_SITE = "equinox.p2.update.site"; //$NON-NLS-1$

	public String getApplicabilityFilter();

	public IArtifactKey[] getArtifacts();

	/**
	 * Returns the filter on this installable unit. The filter is matched against
	 * the selection context of the profile the unit is installed into. An IU will not
	 * be installed if it has a filter condition that is not satisfied by the context.
	 * 
	 * See Profile#getSelectionContext.
	 */
	public String getFilter();

	/**
	 * Returns the fragments that have been bound to this installable unit, or
	 * <code>null</code> if this unit is not resolved.
	 * 
	 * @see #isResolved()
	 * @return The fragments bound to this installable unit, or <code>null</code>
	 */
	public IInstallableUnitFragment[] getFragments();

	public String getId();

	/**
	 * Get an <i>unmodifiable copy</i> of the properties
	 * associated with the installable unit.
	 * 
	 * @return an <i>unmodifiable copy</i> of the IU properties.
	 */
	public Map getProperties();

	public String getProperty(String key);

	public ProvidedCapability[] getProvidedCapabilities();

	public RequiredCapability[] getRequiredCapabilities();

	public TouchpointData[] getTouchpointData();

	public TouchpointType getTouchpointType();

	public Version getVersion();

	public boolean isFragment();

	/**
	 * Returns whether this installable unit has been resolved. A resolved
	 * installable unit represents the union of an installable unit and some
	 * fragments.
	 * 
	 * @see #getFragments()
	 * @see #unresolved()
	 * @return <code>true</code> if this installable unit is resolved, and 
	 * <code>false</code> otherwise.
	 */
	public boolean isResolved();

	public boolean isSingleton();

	/**
	 * Returns the unresolved equivalent of this installable unit. If this unit is
	 * already unresolved, this method returns the receiver. Otherwise, this
	 * method returns an installable unit with the same id and version, but without
	 * any fragments attached.
	 * 
	 * @see #getFragments()
	 * @see #isResolved()
	 * @return The unresolved equivalent of this unit
	 */
	public IInstallableUnit unresolved();

	/**
	 * Returns information about what this installable unit is an update of.
	 * @return The lineage information about the installable unit
	 */
	public IUpdateDescriptor getUpdateDescriptor();

	/**
	 * Returns the license that applies to this installable unit.
	 * @return the license that applies to this installable unit or <code>null</code>
	 */
	public License getLicense();

	/**
	 * Returns the copyright that applies to this installable unit.
	 * @return the copyright that applies to this installable unit or <code>null</code>
	 */
	public Copyright getCopyright();
}