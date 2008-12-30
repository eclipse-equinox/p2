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

import org.eclipse.equinox.internal.p2.director.Messages;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.osgi.util.NLS;

/**
 * Represents a provisioning dependency that was not satisfied during
 * resolution.
 */
public class UnsatisfiedCapability {
	private IInstallableUnit owner;
	private IRequiredCapability require;

	/**
	 * Creates a new unresolved dependency
	 * @param required The dependency that was not satisfied.
	 * @param owner The installable unit whose dependency was not satisfied.
	 */
	public UnsatisfiedCapability(IRequiredCapability required, IInstallableUnit owner) {
		this.require = required;
		this.owner = owner;
	}

	public IInstallableUnit getOwner() {
		return owner;
	}

	/**
	 * Returns the specific dependency that was not satisfied.
	 */
	public IRequiredCapability getRequiredCapability() {
		return require;
	}

	/**
	 * Returns the installable unit whose dependency was not satisfied.
	 */
	public IInstallableUnit getUnsatisfiedUnit() {
		return owner;
	}

	/**
	 * Prints out a human-readable representation of an unsatisfied capability
	 */
	public String toString() {
		return NLS.bind(Messages.Director_Unsatisfied_Dependency, owner, require);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof UnsatisfiedCapability))
			return false;
		if (owner == null || require == null)
			return false;
		return owner.equals(((UnsatisfiedCapability) obj).getOwner()) && require.equals(((UnsatisfiedCapability) obj).getRequiredCapability());
	}

	public int hashCode() {
		if (owner == null || require == null)
			return 0;
		return 31 * owner.hashCode() + require.hashCode();
	}
}
