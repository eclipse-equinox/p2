/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
