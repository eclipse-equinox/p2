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
package org.eclipse.equinox.prov.installregistry;

import org.eclipse.equinox.prov.metadata.IInstallableUnit;

public interface IProfileInstallRegistry {

	public abstract IInstallableUnit[] getInstallableUnits();

	public abstract IInstallableUnit getInstallableUnit(String id, String version);

	public abstract void addInstallableUnits(IInstallableUnit toAdd);

	public abstract void removeInstallableUnits(IInstallableUnit toRemove);

	public abstract String getProfileId();

}