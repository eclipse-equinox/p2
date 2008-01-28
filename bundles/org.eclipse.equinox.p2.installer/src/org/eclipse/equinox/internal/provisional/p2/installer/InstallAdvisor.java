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
package org.eclipse.equinox.internal.provisional.p2.installer;

import org.eclipse.core.runtime.*;

/**
 * The install advisor helps to make decisions during install, and is the conduit
 * for reporting progress and results back to an end user or log.
 */
public abstract class InstallAdvisor {
	/**
	 * Performs the actual install. The advisor is responsible for handling progress
	 * monitoring and cancelation. The advisor may perform the install in
	 * another thread, but must block the calling thread until the install
	 * completes.
	 * 
	 * @param operation The install operation to run
	 * @return IStatus The result of the install operation. This is typically
	 * just the return value of {@link IInstallOperation#install(IProgressMonitor)},
	 * but the advisor may alter the result status if desired.
	 */
	public abstract IStatus performInstall(IInstallOperation operation);

	/**
	 * Allows the advisor to modify or fill in missing values in the install description.  
	 * @param description The initial install description
	 * @return The install description to be used for the install.
	 * @exception OperationCanceledException if the install should be canceled.
	 */
	public abstract InstallDescription prepareInstallDescription(InstallDescription description);

	/**
	 * Prompts for whether the installed application should be launched immediately.
	 * This method is only called after a successful install.
	 * 
	 * @param description The initial install description
	 * @return <code>true</code> if the product should be launched, and 
	 * <code>false</code> otherwise.
	 */
	public abstract boolean promptForLaunch(InstallDescription description);

	/**
	 * Reports some result information to the context.  The status may be
	 * information, warning, or an error.
	 */
	public abstract void setResult(IStatus status);

	/**
	 * Initializes the install advisor.  This method must be called before calling any 
	 * other methods on the advisor are called.  Subsequent invocations of this
	 * method are ignored.
	 */
	public abstract void start();

	/**
	 * Stops the install advisor. The advisor becomes invalid after it has been
	 * stopped; a stopped advisor cannot be restarted.
	 */
	public abstract void stop();
}
