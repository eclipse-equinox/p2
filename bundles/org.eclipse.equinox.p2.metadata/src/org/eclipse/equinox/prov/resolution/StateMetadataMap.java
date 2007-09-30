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
package org.eclipse.equinox.prov.resolution;

import java.util.Map;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;

//Instances of this class are stored in the user object slot of bundlescriptions to ease navigation between the two models.
public class StateMetadataMap {
	private IInstallableUnit unit;
	private Map correspondingSpecifications; //indexes in this array maps to the ones in the dependencies array. This is gross. TODO

	public StateMetadataMap(IInstallableUnit unit, Map correspondingSpecifications) {
		super();
		this.unit = unit;
		this.correspondingSpecifications = correspondingSpecifications;
	}

	public IInstallableUnit getUnit() {
		return unit;
	}

	public Map getGenericSpecifications() {
		return correspondingSpecifications;
	}
}
