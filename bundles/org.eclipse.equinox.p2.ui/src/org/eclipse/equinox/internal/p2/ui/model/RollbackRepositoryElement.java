/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import java.net.URL;

/**
 * Element wrapper class for a rollback repository.
 * 
 * @since 3.4
 */
public class RollbackRepositoryElement extends MetadataRepositoryElement {

	String profileId;

	public RollbackRepositoryElement(URL url, String profileId) {
		super(null, url);
		this.profileId = profileId;
	}

	public String getProfileId() {
		return profileId;
	}
}
