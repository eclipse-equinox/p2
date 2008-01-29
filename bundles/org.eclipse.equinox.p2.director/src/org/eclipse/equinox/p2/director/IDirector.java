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
package org.eclipse.equinox.p2.director;

import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * Directors are responsible for determining what should be done to a given 
 * profile to reshape it as requested. That is, given the current state of a 
 * profile, a description of the desired end state of that profile and metadata 
 * describing the available IUs, a director produces a list of provisioning 
 * operations (e.g., install, update or uninstall) to perform on the related IUs. 
 * Directors are also able to validate profiles and assist in the diagnosis of 
 * configuration errors. Note that directors may range in complexity from 
 * very simple (e.g., reading a list of bundles from a static file) to very complex. 
 */
public interface IDirector {

	public IStatus provision(ProfileChangeRequest profileChangeRequest, ProvisioningContext context, IProgressMonitor monitor);

	/**
	 * Installs the given units into the given profile.
	 * 
	 * @param toInstall The units to install
	 * @param profile The profile to install into
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 */
	//	public IStatus install(IInstallableUnit[] toInstall, Profile profile, ProvisioningContext context, IProgressMonitor monitor);
	/**
	 * Uninstalls the given units from the given profile.
	 * 
	 * @param previous The units to uninstall
	 * @param profile The profile from which to uninstall
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 */
	//	public IStatus uninstall(IInstallableUnit[] toUninstall, Profile profile, ProvisioningContext context, IProgressMonitor monitor);
	//public IStatus replace(IInstallableUnit[] toUninstall, IInstallableUnit[] toInstall, Profile profile, ProvisioningContext context, IProgressMonitor monitor);
	public IStatus revert(IInstallableUnit previous, Profile profile, ProvisioningContext context, IProgressMonitor monitor);

	/**
	 * Returns the location of the director's rollback repository, where information about
	 * previous profile states is stored.
	 */
	public URL getRollbackRepositoryLocation();
}
