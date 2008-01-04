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
 * Interface for a mechanism that chooses a profile from the profile registry.
 * the mechanism may or may not involve the user.
 * 
 * @since 3.4
 * 
 */

public interface IProfileChooser {
	/**
	 * Return a chosen profile id, or <code>null</code> if there is no profile
	 * chosen.
	 */
	public String getProfileId(Shell shell);

	/**
	 * Return a String that could be used to label this chooser.
	 * For example, if the chooser uses a dialog, the label could be used
	 * in the button that launches the dialog.
	 */
	public String getLabel();

}
