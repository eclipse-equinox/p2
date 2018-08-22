/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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

package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.jface.action.IMenuManager;

/**
 * 
 * IViewMenuProvider is used to fill a view menu in dialog groups that support them.
 * @since 3.4
 *
 */
public interface IViewMenuProvider {
	public void fillViewMenu(IMenuManager viewMenu);
}
