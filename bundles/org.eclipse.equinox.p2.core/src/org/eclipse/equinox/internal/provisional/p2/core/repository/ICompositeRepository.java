/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.core.repository;

import java.net.URI;
import java.util.ArrayList;

public interface ICompositeRepository extends IRepository {
	/**
	 * 
	 * @return an ArrayList of URIs containing the locations of the children repositories
	 */
	public abstract ArrayList getChildren();

	/**
	 * Removes all child repositories
	 */
	public abstract void removeAllChildren();

	/**
	 * Removes specified URI from list of child repositories.
	 * Does nothing if specified URI is not a child repository
	 * @param child
	 */
	public abstract void removeChild(URI child);

	/**
	 * Adds a specified URI to list of child repositories.
	 * Does nothing if URI is a duplicate of an existing child repository.
	 * @param child
	 */
	public abstract void addChild(URI child);
}
