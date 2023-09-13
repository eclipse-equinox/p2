/*******************************************************************************
 *  Copyright (c) 2008, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository;

import java.net.URI;
import java.util.List;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * A composite repository doesn't directly contain any contents, but rather contains
 * only a list of child repositories. The composite repository aggregates content
 * from the children and acts as a single repository containing the union of all
 * child contents. When a composite repository is queried programmatically,
 * it will appear to contain all elements that currently exist in one or more
 * of its children.
 * @param <T> The type of repository content. Typically this is either {@link IInstallableUnit}
 * or {@link IArtifactKey}.
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface ICompositeRepository<T> extends IRepository<T> {
	/**
	 * Adds a specified URI to list of child repositories.
	 * Does nothing if URI is a duplicate of an existing child repository.
	 * @param child The child to add.
	 */
	void addChild(URI child);

	/**
	 * Returns a list of URIs containing the locations of the children repositories
	 *
	 * @return a list of URIs containing the locations of the children repositories
	 */
	List<URI> getChildren();

	/**
	 * Removes all child repositories
	 */
	void removeAllChildren();

	/**
	 * Removes the specified URI from the list of child repositories.
	 * This method has no effect if the specified URI is not a child repository
	 * @param child The child to remove
	 */
	void removeChild(URI child);
}
