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
package org.eclipse.equinox.prov.ui.operations;

import org.eclipse.equinox.prov.core.ProvisionException;
import org.eclipse.equinox.prov.engine.Profile;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;

/**
 * Abstract class representing provisioning profile operations
 * 
 * @since 3.4
 */
public abstract class ProfileModificationOperation extends ProfileOperation {

	IInstallableUnit[] ius;
	String entryPointName;

	ProfileModificationOperation(String label, String id, IInstallableUnit[] ius, String entryPointName) {
		super(label, new String[] {id});
		this.entryPointName = entryPointName;
		this.ius = ius;
	}

	ProfileModificationOperation(String label, String id, IInstallableUnit[] ius) {
		this(label, id, ius, null);
	}

	boolean isValid() {
		return super.isValid() && ius != null && ius.length > 0;
	}

	public String getProfileId() {
		try {
			return super.getProfiles()[0].getProfileId();
		} catch (ProvisionException e) {
			return null;
		}
	}

	public Profile getProfile() {
		try {
			return super.getProfiles()[0];
		} catch (ProvisionException e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.prov.ui.operations.ProvisioningOperation#getAffectedObjects()
	 */
	public Object[] getAffectedObjects() {
		if (ius != null)
			return ius;
		return super.getAffectedObjects();
	}
}
