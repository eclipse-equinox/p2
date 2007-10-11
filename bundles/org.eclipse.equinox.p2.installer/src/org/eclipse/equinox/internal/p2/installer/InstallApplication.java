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
package org.eclipse.equinox.internal.p2.installer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.installer.ui.SWTInstallAdvisor;
import org.eclipse.equinox.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.installer.InstallAdvisor;
import org.eclipse.equinox.p2.installer.InstallDescription;

/**
 * This is a simple installer application built using P2.  The application must be given
 * an "install description" as a command line argument or system property 
 * ({@link #SYS_PROP_INSTALL_DESCRIPTION}).  The application reads this
 * install description, and looks for an existing profile in the local install registry that
 * matches it.  If no profile is found, it creates a new profile, and installs the root
 * IU in the install description into the profile. It may then launch the installed application,
 * depending on the specification in the install description.  If an existing profile is found,
 * the application instead performs an update on the existing profile with the new root
 * IU in the install description. Thus, an installed application can be updated by dropping
 * in a new install description file, and re-running this installer application.
 */
public class InstallApplication implements IApplication {
	/**
	 * A property whose value is the URL of an install description. An install description is a file
	 * that contains all the information required to complete the install.
	 */
	private static final String SYS_PROP_INSTALL_DESCRIPTION = "org.eclipse.equinox.p2.installDescription"; //$NON-NLS-1$

	/**
	 * The install advisor. This field is non null while the install application is running.
	 */
	private InstallAdvisor advisor;

	/**
	 * Throws an exception of severity error with the given error message.
	 */
	private static CoreException fail(String message) {
		return fail(message, null);
	}

	/**
	 * Throws an exception of severity error with the given error message.
	 */
	private static CoreException fail(String message, Throwable throwable) {
		return new CoreException(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, message, throwable));
	}

	/**
	 * Loads the install description, filling in any missing data if needed.
	 */
	private InstallDescription computeInstallDescription() throws CoreException {
		InstallDescription description = fetchInstallDescription(SubMonitor.convert(null));
		return advisor.prepareInstallDescription(description);
	}

	private InstallAdvisor createInstallContext() {
		//TODO create an appropriate advisor depending on whether headless or GUI install is desired.
		InstallAdvisor result = new SWTInstallAdvisor();
		result.start();
		return result;
	}

	/**
	 * Fetch and return the install description to be installed.
	 */
	private InstallDescription fetchInstallDescription(SubMonitor monitor) throws CoreException {
		String site = System.getProperty(SYS_PROP_INSTALL_DESCRIPTION);
		if (site == null)
			throw fail("No install site provided");
		try {
			URL siteURL = new URL(site);
			return (InstallDescription) InstallDescriptionParser.loadFromProperties(siteURL.openStream(), monitor);
		} catch (MalformedURLException e) {
			throw fail("Invalid install site: " + site, e);
		} catch (IOException e) {
			throw fail("Invalid install site: " + site, e);
		}
	}

	private IStatus getStatus(final Exception failure) {
		Throwable cause = failure;
		//unwrap target exception if applicable
		if (failure instanceof InvocationTargetException) {
			cause = ((InvocationTargetException) failure).getTargetException();
			if (cause == null)
				cause = failure;
		}
		if (cause instanceof CoreException)
			return ((CoreException) cause).getStatus();
		return new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, "An error occurred during installation", cause);
	}

	private void launchProduct(InstallDescription description) throws CoreException {
		IPath installLocation = description.getInstallLocation();
		IPath toRun = installLocation.append(description.getLauncherName());
		try {
			Runtime.getRuntime().exec(toRun.toString(), null, installLocation.toFile());
		} catch (IOException e) {
			throw fail("Failed to launch the product: " + toRun, e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext appContext) {
		try {
			appContext.applicationRunning();
			advisor = createInstallContext();
			//fetch description of what to install
			InstallDescription description = null;
			try {
				description = computeInstallDescription();

				//perform long running install operation
				InstallUpdateProductOperation operation = new InstallUpdateProductOperation(InstallerActivator.getDefault().getContext(), description);
				advisor.performInstall(operation);
				IStatus result = operation.getResult();
				if (!result.isOK()) {
					advisor.setResult(result);
					return IApplication.EXIT_OK;
				}
				//just exit after a successful update
				if (!operation.isFirstInstall())
					return IApplication.EXIT_OK;
				if (description.isAutoStart())
					launchProduct(description);
				else {
					//notify user that the product was installed
					//TODO present the user an option to immediately start the product
					advisor.setResult(result);
				}
			} catch (OperationCanceledException e) {
				advisor.setResult(Status.CANCEL_STATUS);
			} catch (Exception e) {
				IStatus error = getStatus(e);
				advisor.setResult(error);
				LogHelper.log(error);
			}
			return IApplication.EXIT_OK;
		} finally {
			advisor.stop();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		//note this method can be called from another thread
		InstallAdvisor tempContext = advisor;
		if (tempContext != null) {
			tempContext.stop();
			advisor = null;
		}
	}
}
