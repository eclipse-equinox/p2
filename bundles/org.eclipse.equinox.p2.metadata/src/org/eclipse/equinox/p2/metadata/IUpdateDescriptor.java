/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
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
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IUpdateDescriptor {
	public final int NORMAL = 0;
	public final int HIGH = 1;

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
	 * @return The severity.
	 */
	public int getSeverity();

	/**
	 * Helper method indicating whether or not an installable unit is an update for the installable unit passed  
	 * @param iu the installable unit checked
	 * @return A boolean indicating whether or not an installable unit is an update.
	 */
	public boolean isUpdateOf(IInstallableUnit iu);
}
