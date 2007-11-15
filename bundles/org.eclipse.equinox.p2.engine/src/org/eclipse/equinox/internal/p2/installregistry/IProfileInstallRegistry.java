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
package org.eclipse.equinox.internal.p2.installregistry;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public interface IProfileInstallRegistry {

	public IInstallableUnit[] getInstallableUnits();

	public IInstallableUnit getInstallableUnit(String id, String version);

	public void addInstallableUnits(IInstallableUnit toAdd);

	public void removeInstallableUnits(IInstallableUnit toRemove);

	public String getProfileId();

	public String getInstallableUnitProfileProperty(IInstallableUnit iu, String key);

	public String setInstallableUnitProfileProperty(IInstallableUnit iu, String key, String value);

}