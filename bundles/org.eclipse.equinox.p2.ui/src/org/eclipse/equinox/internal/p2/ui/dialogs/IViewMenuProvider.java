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
