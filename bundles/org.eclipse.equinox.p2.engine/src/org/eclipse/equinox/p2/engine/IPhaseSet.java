/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.p2.engine;

/**
 * Describes a set of provisioning phases to be performed by an {@link IEngine}.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IPhaseSet {

	/**
	 * Returns the ids of the phases to be performed by this phase set. The order
	 * of the returned ids indicates the order in which the phases will be run.
	 * @return The phase ids.
	 */
	public String[] getPhaseIds();
}
