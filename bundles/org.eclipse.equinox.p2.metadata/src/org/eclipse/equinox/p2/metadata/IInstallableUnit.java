/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * 		IBM Corporation - initial API and implementation
 * 		Genuitec, LLC - added license support
 * 		EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.util.Collection;
import java.util.Map;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;

/**
 * An installable unit represents an atomic, indivisible unit of installable functionality
 * in a provisioned system. Everything that can be installed or uninstalled in a system, 
 * including both concrete artifacts and instructions describing steps to be performed
 * during install, must be expressed as one or more installable units. Thus the set of
 * installable units present in a system, together with the existing environment 
 * (operating system, etc), completely describes the initial installed state of that system.
 * <p>
 * Installable units may have dependencies on functionality provided by other installable
 * units, such that the unit cannot be installed unless some other installable unit
 * is present in the installed system that provides a matching capability. Such 
 * dependencies are referred to as <i>required capabilities</i>. Conversely,
 * installable units may declared <i>provided capabilities</i>, describing the
 * capabilities that they make available to other units in the system. Note the
 * weak coupling at work here: installable units never directly depend on each other,
 * but instead depend on abstract capabilities that any other installable unit may provide.
 * </p>
 * <p>
 * Instances of this class are handle objects and do not necessarily
 * reflect entities that exist in any particular profile or repository. These handle 
 * objects can be created using {@link MetadataFactory}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 * @see MetadataFactory#createInstallableUnit(org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription)
 */
public interface IInstallableUnit extends IVersionedId, Comparable<IInstallableUnit> {

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
	 * A property key (value <code>"org.eclipse.equinox.p2.bundle.localization"</code>) for a String
	 * property containing the bundle localization property file name
	 */
	public static final String PROP_BUNDLE_LOCALIZATION = "org.eclipse.equinox.p2.bundle.localization"; //$NON-NLS-1$

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
	 * A property key (value <code>"org.eclipse.equinox.p2.icon"</code>) for a String
	 * property containing a URI for an icon that should be shown when displaying this 
	 * installable unit in a user interface.  
	 * 
	 * @see #getProperty(String)
	 */
	public static final String PROP_ICON = "org.eclipse.equinox.p2.icon"; //$NON-NLS-1$

	/**
	 * Returns the collection of artifacts associated with this installable unit.
	 * Installing this unit into a system will cause these artifacts to be fetched from
	 * a repository and applied to the installed system. Uninstalling this unit
	 * will cause these artifacts to be removed from the system.
	 * 
	 * @return The artifacts associated with this installable unit
	 */
	public Collection<IArtifactKey> getArtifacts();

	/**
	 * Returns the filter on this installable unit. The filter is matched against
	 * the properties of the environment the unit is installed into. An installable
	 * unit will not be installed if it has a filter condition that is not satisfied by the 
	 * properties of the environment.
	 * 
	 * @return The installation filter for this unit, or <code>null</code>
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public IMatchExpression<IInstallableUnit> getFilter();

	/**
	 * Returns the fragments that have been bound to this installable unit, or
	 * <code>null</code> if this unit is not resolved.
	 * 
	 * @see #isResolved()
	 * @return The fragments bound to this installable unit, or <code>null</code>
	 */
	public Collection<IInstallableUnitFragment> getFragments();

	/**
	 * Returns an <i>unmodifiable copy</i> of the properties
	 * associated with the installable unit.
	 * 
	 * @return an <i>unmodifiable copy</i> of the properties of this installable unit.
	 */
	public Map<String, String> getProperties();

	/**
	 * Returns the untranslated property of this installable unit associated with the given key. 
	 * Returns <code>null</code> if no such property is defined.
	 * <p>
	 * If the property value has been externalized, this method will return a string containing
	 * the translation key rather than a human-readable string. For this reason, clients
	 * wishing to obtain the value for a property that is typically translated should use
	 * {@link #getProperty(String, String)} instead.
	 * </p>
	 * 
	 * @param key The property key to retrieve a property value for
	 * @return the property that applies to this installable unit or <code>null</code>
	 */
	public String getProperty(String key);

	/**
	 * Returns the property of this installable unit associated with the given key. 
	 * Returns <code>null</code> if no such property is defined or no applicable
	 * translation is available.
	 * 
	 * @param key The property key to retrieve a property value for
	 * @param locale The locale to translate the property for, or null to use the current locale.
	 * @return the property that applies to this installable unit or <code>null</code>
	 */
	public String getProperty(String key, String locale);

	/**
	 * Returns the collection of capabilities provided by this installable unit. 
	 * 
	 * @return The collection of capabilities provided by this installable unit. 
	 */
	public Collection<IProvidedCapability> getProvidedCapabilities();

	public Collection<IRequirement> getRequirements();

	public Collection<IRequirement> getMetaRequirements();

	public Collection<ITouchpointData> getTouchpointData();

	public ITouchpointType getTouchpointType();

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

	/**
	 * Returns whether this installable unit is a singleton. Only one singleton
	 * installable unit with a given id is allowed to exist in a given installed system.
	 * Attempting to install multiple versions of a singleton will fail.
	 * @return <code>true</code> if this unit is a singleton, and <code>false</code> otherwise
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public boolean isSingleton();

	/**
	 * Returns whether this unit has a provided capability that satisfies the given 
	 * requirement.
	 * @return <code>true</code> if this unit satisfies the given requirement, and <code>false</code> otherwise.
	 */
	public boolean satisfies(IRequirement candidate);

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
	 * Returns the untranslated licenses that apply to this installable unit. 
	 * <p>
	 * If the license text has been externalized, this method will return strings containing
	 * the translation keys rather than human-readable strings. For this reason, clients
	 * wishing to obtain a license for display to an end user should use {@link #getLicenses(String)}
	 * instead.
	 * </p>
	 * @return the licenses that apply to this installable unit
	 */
	public Collection<ILicense> getLicenses();

	/**
	 * Returns the licenses that apply to this installable unit. Any translation of the
	 * licenses for the given locale will be applied. Returns an empty collection if this
	 * unit has no licenses, or if the available licenses are externalized and do not
	 * have translations available for the given locale.
	 * 
	 * @param locale The locale to translate the license for, or null to use the current locale.
	 * @return the translated licenses that apply to this installable unit
	 */
	public Collection<ILicense> getLicenses(String locale);

	/**
	 * Returns the untranslated copyright that applies to this installable unit.
	 * <p>
	 * If the copyright text has been externalized, this method will return strings containing
	 * the translation keys rather than human-readable strings. For this reason, clients
	 * wishing to obtain a copyright for display to an end user should use {@link #getCopyright(String)}
	 * instead.
	 * </p>
	 * @return the copyright that applies to this installable unit or <code>null</code>
	 */
	public ICopyright getCopyright();

	/**
	 * Returns the copyright that applies to this installable unit. Any translation of the
	 * copyright for the given locale will be applied. Returns <code>null</code> if this
	 * unit has no copyright, or if the copyright is externalized and no translations are
	 * available for the given locale.
	 * 
	 * @param locale The locale to translate the copyright for, or null to use the current locale.
	 * @return the copyright that applies to this installable unit or <code>null</code>
	 */
	public ICopyright getCopyright(String locale);

	/**
	 * Returns whether this InstallableUnit is equal to the given object.
	 * 
	 * This method returns <i>true</i> if:
	 * <ul>
	 *  <li> Both this object and the given object are of type IInstallableUnit
	 *  <li> The result of <b>getId()</b> on both objects are equal
	 *  <li> The result of <b>getVersion()</b> on both objects are equal
	 * </ul> 
	 */
	public boolean equals(Object obj);
}