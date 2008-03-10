/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui;

import java.net.URL;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.URLValidator;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
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

	/**
	 * Return an array of URLs containing the repositories already known.
	 */
	public URL[] getKnownRepositories();

	/**
	 * Return an operation that could be used to add the specified URL as
	 * a repository.
	 */
	public ProvisioningOperation getAddOperation(URL repoURL);

	/**
	 * Return an operation that could be used to remove the specified URL as
	 * a repositories.
	 */
	public ProvisioningOperation getRemoveOperation(URL[] repoURLs);

	public URLValidator getURLValidator(Shell shell);
}
