/******************************************************************************* 
* Copyright (c) 2008, 2017 EclipseSource and others.
 *
 * This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
* available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
*   IBM - ongoing development
*   Todor Boev
******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.util.Map;
import org.eclipse.equinox.p2.metadata.expression.IFilterExpression;

/**
 * Describes a capability that is exposed by an installable unit. These capabilities
 * can satisfy the dependencies of other installable units, causing the unit
 * providing the dependency to be installed.
 * <p>
 * Instances of this class are handle objects and do not necessarily
 * reflect entities that exist in any particular profile or repository. These handle 
 * objects can be created using {@link MetadataFactory}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 * @see IRequirement
 */
public interface IProvidedCapability {
	/**
	 * The name of the property under which the capability version is stored.
	 * 
	 * Can be used with {@link #getProperties()}. The same value can be obtained with {@link #getVersion()}
	 * 
	 * @since 2.4
	 */
	String PROPERTY_VERSION = "version"; //$NON-NLS-1$

	/**
	 * 
	 * @return String the namespace of this capability.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	String getNamespace();

	/**
	 * 
	 * @return String the attribute stored under a key equal to the {@link #getNamespace()} attribute of this capability.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	String getName();

	/**
	 * 
	 * @return String the special {@link #PROPERTY_VERSION} attribute of this capability.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	Version getVersion();

	/**
	 * A full description of this capability including the name and the version.
	 * <p>
	 * Such a description can be used to match this capability with an {@link IFilterExpression LDAP filter} for example.
	 * 
	 * @return An unmodifiable map
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 2.4
	 */
	Map<String, Object> getProperties();

	/**
	 * Returns whether this provided capability is equal to the given object.
	 * 
	 * This method returns <i>true</i> if:
	 * <ul>
	 *  <li>Both this object and the given object are of type IProvidedCapability</li>
	 *  <li>The result of {@link #getNamespace()} on both objects are equal</li>
	 *  <li>The result of {@link #getProperties()} on both objects are equal</li>
	 * </ul> 
	 */
	@Override
	boolean equals(Object other);
}