/******************************************************************************* 
* Copyright (c) 2008, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.metadata;


/**
 * Identifies a particular touchpoint. A touchpoint is identified by an id 
 * and a version.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface ITouchpointType {

	/**
	 * A touchpoint type indicating an undefined touchpoint type. Identity (==)
	 * must be used to test for the <code>NONE</code> type.
	 */
	public static final ITouchpointType NONE = new ITouchpointType() {

		public String getId() {
			return "null"; //$NON-NLS-1$
		}

		public Version getVersion() {
			return Version.emptyVersion;
		}
	};

	public String getId();

	public Version getVersion();

	/**
	 * Returns whether this TouchpointInstruction is equal to the given object.
	 * 
	 * This method returns <i>true</i> if:
	 * <ul>
	 *  <li> Both this object and the given object are of type ITouchpointType
	 *  <li> The result of <b>getId()</b> on both objects are equal
	 *  <li> The result of <b>getVersion()</b> on both objects are equal
	 * </ul> 
	 */
	public boolean equals(Object obj);

}