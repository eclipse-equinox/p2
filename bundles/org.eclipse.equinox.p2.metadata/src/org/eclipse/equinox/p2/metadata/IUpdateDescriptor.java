/*******************************************************************************
 *  Copyright (c) 2008, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.net.URI;
import java.util.Collection;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;

/**
 * An update descriptor is attached to an installable unit to describe what that
 * installable unit is capable of acting as an update for. Typically an installable unit
 * will specify that it is capable of updating all installable units of the same name
 * and with an older version. However, this descriptor allows an installable
 * unit to be considered an update for an installable unit with a different name, or
 * even an update for a unit with a higher version than itself.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 * @see MetadataFactory#createUpdateDescriptor(String, VersionRange, int, String)
 */
public interface IUpdateDescriptor {
	/**
	 * Update severity constant (value 0) indicating a normal severity update.
	 */
	public final int NORMAL = 0;
	/**
	 * Update severity constant (value 1) indicating a high severity update.
	 */
	public final int HIGH = 1;

	/**
	 * Returns an expression matching all installable units that will be updated by
	 * the unit with this update descriptor.
	 * @return An expression matching all matching installable units
	 */
	Collection<IMatchExpression<IInstallableUnit>> getIUsBeingUpdated();

	/**
	 * The description of the update. This allows to explain what the update is about.
	 * @return A description
	 */
	public String getDescription();

	/**
	 * Returns the location of a document containing the description.
	 * 
	 * @return the location of the document, or <code>null</code>
	 */
	public URI getLocation();

	/**
	 * The importance of the update descriptor represented as a int.
	 * @return The severity (either {@link #HIGH} or {@link #NORMAL}).
	 */
	public int getSeverity();

	/**
	 * Helper method indicating whether or not an installable unit is an update for the installable unit passed  
	 * @param iu the installable unit checked
	 * @return A boolean indicating whether or not an installable unit is an update.
	 */
	public boolean isUpdateOf(IInstallableUnit iu);
}
