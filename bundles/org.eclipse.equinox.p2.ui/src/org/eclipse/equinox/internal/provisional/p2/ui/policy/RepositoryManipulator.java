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
package org.eclipse.equinox.internal.provisional.p2.ui.policy;

import java.net.URI;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
import org.eclipse.swt.widgets.Shell;

/**
 * Abstract class for a mechanism that allows the user to manipulate which repositories
 * are in the system.
 * 
 * @since 3.4
 * 
 */

public abstract class RepositoryManipulator {
	/**
	 * Invoke whatever mechanism is used to manipulate repositories.
	 * Return a boolean indicating whether the repositories were
	 * actually manipulated in any way.
	 */
	public abstract boolean manipulateRepositories(Shell shell);

	/**
	 * Return a String that could be used to label this manager. For example, if
	 * the manager is a dialog that lets you manipulate repositories, the label
	 * could be used in the button that launches the dialog.
	 */
	public abstract String getManipulatorLabel();

	/**
	 * Return an array of URLs containing the repositories already known.
	 */
	public abstract URI[] getKnownRepositories();

	/**
	 * Return an operation that could be used to add the specified URL as
	 * a repository.
	 */
	public abstract ProvisioningOperation getAddOperation(URI repoLocation);

	/**
	 * Return a String describing a possible add operation.  This is used 
	 * when prompting or reporting errors involving a possible add operation.
	 */
	public abstract String getAddOperationLabel();

	/**
	 * Return an operation that could be used to remove the specified URL as
	 * a repositories.
	 */
	public abstract ProvisioningOperation getRemoveOperation(URI[] repoLocations);

	/**
	 * Return a String describing a possible remove operation.  This is used 
	 * when prompting or reporting errors involving a possible remove operation.
	 */
	public abstract String getRemoveOperationLabel();

	/**
	 * Get a URL validator that could be used to validate URLs supplied
	 * by the user.
	 * 
	 * @param shell the shell used by the validator to report any problems.
	 * @return the validator to be used.
	 */

	public abstract RepositoryLocationValidator getRepositoryLocationValidator(Shell shell);
}
