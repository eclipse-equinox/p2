/*******************************************************************************
 * Copyright (c) 2007, 2009 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.repository;

import org.eclipse.core.runtime.IStatus;

/**
 * Implementing <code>IStateful</code> adds the ability to store status information.
 */
public interface IStateful {

	/**
	 * Set the status.
	 * 
	 * @param status if status equals null => getStatus().isOK
	 */
	void setStatus(IStatus status);

	/**
	 * Get status.
	 * @return status
	 */
	public IStatus getStatus();

}
