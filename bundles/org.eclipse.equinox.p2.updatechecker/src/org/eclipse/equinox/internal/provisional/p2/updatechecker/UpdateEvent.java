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
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.updatechecker;

import java.util.Collection;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * An UpdateEvent describes what IU's have updates for a given profile.
 */
public class UpdateEvent {

	String profileId;
	Collection<IInstallableUnit> iusWithUpdates;

	public UpdateEvent(String profileId, Collection<IInstallableUnit> iusWithUpdates) {
		this.profileId = profileId;
		this.iusWithUpdates = iusWithUpdates;
	}

	public Collection<IInstallableUnit> getIUs() {
		return iusWithUpdates;
	}

	public String getProfileId() {
		return profileId;
	}

}
