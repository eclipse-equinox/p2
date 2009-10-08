/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata;



/**
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IUpdateDescriptor {
	public final int NORMAL = 0;
	public final int HIGH = 1;

	/**
	 * The identifier of an installable unit that the installable unit containing this object is an update for.
	 * @return An installable unit id
	 */
	public String getId();

	/**
	 * The range of the installable unit that the installable unit containing this object is an update for.
	 * @return A version range
	 */
	public VersionRange getRange();

	/**
	 * The description of the update. This allows to explain what the update is about.
	 * @return A description
	 */
	public String getDescription();

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
