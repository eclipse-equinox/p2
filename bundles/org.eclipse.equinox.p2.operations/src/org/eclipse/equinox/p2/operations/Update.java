/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.equinox.p2.operations;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * A simple data structure describing a possible update.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public class Update {

	public IInstallableUnit toUpdate;
	public IInstallableUnit replacement;

	/**
	 * Creates a new update description.
	 * @param toUpdate The installable unit to update
	 * @param replacement The replacement installable unit
	 */
	public Update(IInstallableUnit toUpdate, IInstallableUnit replacement) {
		this.toUpdate = toUpdate;
		this.replacement = replacement;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Update))
			return false;
		if (toUpdate == null)
			return false;
		if (replacement == null)
			return false;
		Update other = (Update) obj;
		return toUpdate.equals(other.toUpdate) && replacement.equals(other.replacement);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((toUpdate == null) ? 0 : toUpdate.hashCode());
		result = prime * result + ((replacement == null) ? 0 : replacement.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "Update " + toUpdate.toString() + " ==> " + replacement.toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
