/*******************************************************************************
 *  Copyright (c) 2008 - 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc - public API
 *******************************************************************************/
package org.eclipse.equinox.p2.repository;

import java.net.URI;

/**
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients. Instead clients should use one of the class implementing this interface.
 * @since 2.0
 */
public interface IRepositoryReference {
	/**
	 * Returns the location of the referenced repository
	 * @return the location
	 */
	URI getLocation();

	/**
	 * Returns the type of the referenced repository (currently either {@link IRepository#TYPE_METADATA}
	 * or {@link IRepository#TYPE_ARTIFACT})
	 * @return the repository type
	 */
	int getType();

	/**
	 * Returns bit-wise or of option constants (currently either 
	 * {@link IRepository#ENABLED} or {@link IRepository#NONE}).
	 * @return bit-wise or of option constants
	 */
	int getOptions();

	/**
	 * Returns the optional nickname of the referenced repository
	 * @return The nickname or <code>null</code>
	 */
	String getNickname();
}