/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.model;

import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;

/**
 * Element wrapper class for a rollback repository.
 * 
 * @since 3.4
 */
public class RollbackRepositoryElement extends MetadataRepositoryElement {

	String profileId;

	public RollbackRepositoryElement(IMetadataRepository repo, String profileId) {
		super(repo);
		this.profileId = profileId;
	}

	public String getProfileId() {
		return profileId;
	}
}
