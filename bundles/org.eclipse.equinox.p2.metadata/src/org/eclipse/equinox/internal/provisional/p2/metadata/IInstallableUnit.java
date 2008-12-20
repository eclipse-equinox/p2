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

	/**
	 * A capability namespace representing a particular profile flavor.
	 */
	public static final String NAMESPACE_FLAVOR = "org.eclipse.equinox.p2.flavor"; //$NON-NLS-1$

	/**
	 * A capability namespace representing a particular InstallableUnit by id.
	 * Each InstallableUnit automatically provides a capability in this namespace representing
	 * itself, and other InstallableUnits can require such a capability to state that they
	 * require a particular InstallableUnit to be present.
	 * 
	 * @see IInstallableUnit#getId()
	 */
	public static final String NAMESPACE_IU_ID = "org.eclipse.equinox.p2.iu"; //$NON-NLS-1$

	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.partial.iu"</code>) for a 
	 * boolean property indicating the IU is generated from incomplete information and
	 * should be replaced by the complete IU if available.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_PARTIAL_IU = "org.eclipse.equinox.p2.partial.iu"; //$NON-NLS-1$

	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.type.profile"</code>) for a 
	 * boolean property indicating that an installable unit is a profile.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_TYPE_PROFILE = "org.eclipse.equinox.p2.type.profile"; //$NON-NLS-1$	 

	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.type.category"</code>) for a 
	 * boolean property indicating that an installable unit is a category.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_TYPE_CATEGORY = "org.eclipse.equinox.p2.type.category"; //$NON-NLS-1$

	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.type.fragment"</code>) for a 
	 * boolean property indicating that an installable unit is a fragment.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_TYPE_FRAGMENT = "org.eclipse.equinox.p2.type.fragment"; //$NON-NLS-1$

	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.type.group"</code>) for a 
	 * boolean property indicating that an installable unit is a group.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_TYPE_GROUP = "org.eclipse.equinox.p2.type.group"; //$NON-NLS-1$

	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.type.patch"</code>) for a 
	 * boolean property indicating that an installable unit is a group.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_TYPE_PATCH = "org.eclipse.equinox.p2.type.patch"; //$NON-NLS-1$

	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.type.lock"</code>) for an
	 * integer property indicating how an installable unit is locked in its profile.
	 * The integer is a bit-mask indicating the different locks defined on the installable
	 * unit.  The property should be obtained from a profile using 
	 * IProfile#getInstallableUnitProperty(IInstallableUnit, String).
	 * 
	 * @see #LOCK_UNINSTALL
	 * @see #LOCK_UPDATE
	 * @see #LOCK_NONE
	 */
	public static final String PROP_PROFILE_LOCKED_IU = "org.eclipse.equinox.p2.type.lock"; //$NON-NLS-1$

	//TODO Move to UI
	public static final String PROP_PROFILE_ROOT_IU = "org.eclipse.equinox.p2.type.root"; //$NON-NLS-1$

	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.contact"</code>) for a 
	 * String property containing a contact address where problems can be reported, 
	 * such as an email address.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_CONTACT = "org.eclipse.equinox.p2.contact"; //$NON-NLS-1$
	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.description"</code>) for a 
	 * String property containing a human-readable description of the installable unit.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_DESCRIPTION = "org.eclipse.equinox.p2.description"; //$NON-NLS-1$

	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.description.url"</code>) for a 
	 * String property containing a URL to the description of the installable unit.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_DESCRIPTION_URL = "org.eclipse.equinox.p2.description.url"; //$NON-NLS-1$
	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.doc.url"</code>) for a 
	 * String property containing a URL for documentation about the installable unit.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_DOC_URL = "org.eclipse.equinox.p2.doc.url"; //$NON-NLS-1$

	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.name"</code>) for a 
	 * String property containing a human-readable name for the installable unit.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_NAME = "org.eclipse.equinox.p2.name"; //$NON-NLS-1$
	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.provider"</code>) for a 
	 * String property containing information about the vendor or provider of the 
	 * installable unit.
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_PROVIDER = "org.eclipse.equinox.p2.provider"; //$NON-NLS-1$

	/**
	 * Constant used to indicate that an installable unit is not locked in anyway.
	 * @see #PROP_PROFILE_LOCKED_IU
	 */
	public static int LOCK_NONE = 0;
	/**
	 * Constant used to indicate that an installable unit is locked so that it may
	 * not be uninstalled.
	 * @see #PROP_PROFILE_LOCKED_IU
	 */
	public static int LOCK_UNINSTALL = 1 << 0;
	/**
	 * Constant used to indicate that an installable unit is locked so that it may
	 * not be updated. updates.
	 * @see #PROP_PROFILE_LOCKED_IU
	 */
	public static int LOCK_UPDATE = 1 << 1;

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
	 * Returns whether this unit has a provided capability that satisfies the given 
	 * required capability.
	 * @return <code>true</code> if this unit satisfies the given required
	 * capability, and <code>false</code> otherwise.
	 */
	public boolean satisfies(RequiredCapability candidate);

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