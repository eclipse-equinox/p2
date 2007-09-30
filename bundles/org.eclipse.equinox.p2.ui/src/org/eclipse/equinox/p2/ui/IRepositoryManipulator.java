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
package org.eclipse.equinox.p2.ui;

import org.eclipse.swt.widgets.Shell;

/**
 * Interface for a mechanism that allows the user to manipulate which repositories
 * are in the system.
 * 
 * @since 3.4
 * 
 */

public interface IRepositoryManipulator {
	/**
	 * Invoke whatever mechanism is used to manipulate repositories.
	 * Return a boolean indicating whether the repositories were
	 * actually manipulated in any way.
	 */
	public boolean manipulateRepositories(Shell shell);

	/**
	 * Return a String that could be used to label this manager. For example, if
	 * the manager is a dialog that lets you manipulate repositories, the label
	 * could be used in the button that launches the dialog.
	 */
	public String getLabel();

}
