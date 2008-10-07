/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.sdk;

import org.eclipse.equinox.internal.provisional.p2.ui.IRepositoryManipulator;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.swt.widgets.Shell;

/**
 * @since 3.4
 * @deprecated temp hack class for PDE UI
 */
public class RepositoryManipulationDialog extends org.eclipse.equinox.internal.provisional.p2.ui.dialogs.RepositoryManipulationDialog {

	/**
	 * @param shell
	 * @param policy
	 */
	public RepositoryManipulationDialog(Shell shell, Policy policy) {
		super(shell, policy);
	}

	public RepositoryManipulationDialog(Shell shell, IRepositoryManipulator manipulator) {
		super(shell, Policy.getDefault());
	}

}
