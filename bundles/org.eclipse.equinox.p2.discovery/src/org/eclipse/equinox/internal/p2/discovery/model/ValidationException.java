/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery.model;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;

/**
 * Indicate that a validation has occurred on the model.
 * 
 * @author David Green
 */
public class ValidationException extends CoreException {

	private static final long serialVersionUID = -7542361242327905294L;

	public ValidationException(String message) {
		super(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, message));
	}

}
