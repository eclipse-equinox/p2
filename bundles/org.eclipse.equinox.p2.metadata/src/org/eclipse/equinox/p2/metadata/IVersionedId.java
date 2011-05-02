/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *     IBM - Ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;


/**
 * An interface representing a (id,version) pair. 
 * @since 2.0
 */
public interface IVersionedId {
	/**
	 * Returns the id portion of this versioned id.
	 * 
	 * @return The id portion of this versioned id.
	 */
	String getId();

	/**
	 * Returns the version portion of this versioned id.
	 * 
	 * @return the version portion of this versioned id.
	 */
	Version getVersion();
}
