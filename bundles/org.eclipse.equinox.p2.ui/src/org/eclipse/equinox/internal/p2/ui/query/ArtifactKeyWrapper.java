/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.query;

import org.eclipse.equinox.internal.p2.ui.model.ArtifactElement;
import org.eclipse.equinox.internal.p2.ui.model.QueriedElementWrapper;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

/**
 * Wrapper that accepts artifact keys and wraps them in an ArtifactKeyElement.
 * 
 * @since 3.6
 */
public class ArtifactKeyWrapper extends QueriedElementWrapper {

	IArtifactRepository repo;

	public ArtifactKeyWrapper(IArtifactRepository repo, Object parent) {
		super(repo, parent);
		this.repo = repo;
	}

	@Override
	protected boolean shouldWrap(Object match) {
		if ((match instanceof IArtifactKey))
			return true;
		return false;
	}

	/**
	 * Transforms the item to a UI element
	 */
	@Override
	protected Object wrap(Object item) {
		return super.wrap(new ArtifactElement(parent, (IArtifactKey) item, repo));
	}

}
