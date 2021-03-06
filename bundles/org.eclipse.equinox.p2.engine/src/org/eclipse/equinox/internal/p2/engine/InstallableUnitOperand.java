/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * @since 2.0
 */
public class InstallableUnitOperand extends Operand {
	private final IInstallableUnit first;
	private final IInstallableUnit second;

	/**
	 * Creates a new operand that represents replacing an installable unit
	 * with another. At least one of the provided installable units must be
	 * non-null.
	 *
	 * @param first The installable unit being removed, or <code>null</code>
	 * @param second The installable unit being added, or <code>null</code>
	 */
	public InstallableUnitOperand(IInstallableUnit first, IInstallableUnit second) {
		//the operand must have at least one non-null units
		Assert.isTrue(first != null || second != null);
		this.first = first;
		this.second = second;
	}

	/**
	 *
	 * @return The installable unit being removed, or <code>null</code>
	 */
	public IInstallableUnit first() {
		return first;
	}

	/**
	 * @return The installable unit being added, or <code>null</code>
	 */
	public IInstallableUnit second() {
		return second;
	}

	@Override
	public String toString() {
		return first + " --> " + second; //$NON-NLS-1$
	}
}
