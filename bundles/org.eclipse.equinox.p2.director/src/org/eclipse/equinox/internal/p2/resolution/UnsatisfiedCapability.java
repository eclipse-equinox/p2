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
package org.eclipse.equinox.internal.p2.resolution;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.RequiredCapability;

/**
 * Represents a provisioning dependency that was not satisfied during
 * resolution.
 */
public class UnsatisfiedCapability {
	private IInstallableUnit owner;
	private RequiredCapability require;

	/**
	 * Creates a new unresolved dependency
	 * @param required The dependency that was not satisfied.
	 * @param owner The installable unit whose dependency was not satisfied.
	 */
	public UnsatisfiedCapability(RequiredCapability required, IInstallableUnit owner) {
		this.require = required;
		this.owner = owner;
	}

	public IInstallableUnit getOwner() {
		return owner;
	}

	/**
	 * Returns the specific dependency that was not satisfied.
	 */
	public RequiredCapability getRequiredCapability() {
		return require;
	}

	/**
	 * Returns the installable unit whose dependency was not satisfied.
	 */
	public IInstallableUnit getUnsatisfiedUnit() {
		return owner;
	}

	/**
	 * For debugging purposes only.
	 */
	public String toString() {
		return "[" + owner + "] " + require.toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
