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
package org.eclipse.equinox.prov.ui.operations;

import org.eclipse.swt.widgets.Shell;

/**
 * Interface for confirming provisioning operations
 * 
 * @since 3.4
 */

public interface IOperationConfirmer {
	/**
	 * Return a boolean indicating whether the operation should continue.
	 * 
	 * @param op
	 *            the operation to be performed.
	 * @param shell
	 *            the shell that should be used for prompting
	 * @return a boolean indicating whether the operation should be performed.
	 */
	public boolean continuePerformingOperation(ProvisioningOperation op, Shell shell);
}
