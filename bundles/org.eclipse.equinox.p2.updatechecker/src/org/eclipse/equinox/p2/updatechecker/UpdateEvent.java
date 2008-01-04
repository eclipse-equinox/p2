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
package org.eclipse.equinox.p2.updatechecker;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * An UpdateEvent describes what IU's have updates for a given profile.
 * 
 * @since 3.4
 */
public class UpdateEvent {

	String profileId;
	IInstallableUnit[] iusWithUpdates;

	public UpdateEvent(String profileId, IInstallableUnit[] iusWithUpdates) {
		this.profileId = profileId;
		this.iusWithUpdates = iusWithUpdates;
	}

	public IInstallableUnit[] getIUs() {
		return iusWithUpdates;
	}

	public String getProfileId() {
		return profileId;
	}

}
