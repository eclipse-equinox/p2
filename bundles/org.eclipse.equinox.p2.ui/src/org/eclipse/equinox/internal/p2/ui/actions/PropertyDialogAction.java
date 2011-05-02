/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.actions;

import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.IShellProvider;

/**
 * PropertyDialogAction which sets its enablement on construction.
 * 
 * @since 3.4
 *
 */

public class PropertyDialogAction extends org.eclipse.ui.dialogs.PropertyDialogAction {
	public PropertyDialogAction(IShellProvider shell, ISelectionProvider provider) {
		super(shell, provider);
		// prime the selection validation
		ISelection selection = provider.getSelection();
		if (selection instanceof IStructuredSelection) {
			selectionChanged((IStructuredSelection) selection);
		} else {
			selectionChanged(selection);
		}

	}

}
