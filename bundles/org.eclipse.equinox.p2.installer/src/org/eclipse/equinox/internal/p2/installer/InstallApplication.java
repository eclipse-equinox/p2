/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.installer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.installer.ui.SWTInstallAdvisor;
import org.eclipse.equinox.internal.provisional.p2.installer.InstallAdvisor;
import org.eclipse.equinox.internal.provisional.p2.installer.InstallDescription;
import org.eclipse.equinox.p2.core.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

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
	private static CoreException fail(String message, Throwable throwable) {
		return new CoreException(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, message, throwable));
	}

	/**
	 * Copied from ServiceHelper because we need to obtain services
	 * before p2 has been started.
	 */
	public static Object getService(BundleContext context, String name) {
		if (context == null)
			return null;
		ServiceReference<?> reference = context.getServiceReference(name);
		if (reference == null)
			return null;
		Object result = context.getService(reference);
		context.ungetService(reference);
		return result;
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
		try {
			return InstallDescriptionParser.createDescription(site, monitor);
		} catch (Exception e) {
			throw fail(Messages.App_InvalidSite + site, e);
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
		return new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, Messages.App_Error, cause);
	}

	private void launchProduct(InstallDescription description) throws CoreException {
		IPath installLocation = description.getInstallLocation();
		IPath toRun = installLocation.append(description.getLauncherName());
		try {
			Runtime.getRuntime().exec(toRun.toString(), null, installLocation.toFile());
		} catch (IOException e) {
			throw fail(Messages.App_LaunchFailed + toRun, e);
		}
		//wait a few seconds to give the user a chance to read the message
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			//ignore
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext appContext) {
		try {
			appContext.applicationRunning();
			initializeProxySupport();
			advisor = createInstallContext();
			//fetch description of what to install
			InstallDescription description = null;
			try {
				description = computeInstallDescription();
				IProvisioningAgent agent = startAgent(description);
				//perform long running install operation
				InstallUpdateProductOperation operation = new InstallUpdateProductOperation(agent, description);
				IStatus result = advisor.performInstall(operation);
				if (!result.isOK()) {
					LogHelper.log(result);
					advisor.setResult(result);
					return IApplication.EXIT_OK;
				}
				//just exit after a successful update
				if (!operation.isFirstInstall())
					return IApplication.EXIT_OK;
				if (canAutoStart(description))
					launchProduct(description);
				else {
					//notify user that the product was installed
					//TODO present the user an option to immediately start the product
					advisor.setResult(result);
				}
				agent.stop();
			} catch (OperationCanceledException e) {
				advisor.setResult(Status.CANCEL_STATUS);
			} catch (Exception e) {
				IStatus error = getStatus(e);
				advisor.setResult(error);
				LogHelper.log(error);
			}
			return IApplication.EXIT_OK;
		} finally {
			if (advisor != null)
				advisor.stop();
		}
	}

	private void initializeProxySupport() {
		IProxyService proxies = (IProxyService) getService(InstallerActivator.getDefault().getContext(), IProxyService.class.getName());
		if (proxies == null)
			return;
		proxies.setProxiesEnabled(true);
		proxies.setSystemProxiesEnabled(true);
	}

	/**
	 * Returns whether the configuration described by the given install
	 * description can be started automatically.
	 */
	private boolean canAutoStart(InstallDescription description) {
		if (!description.isAutoStart())
			return false;
		//can't start if we don't know launcher name and path
		if (description.getLauncherName() == null || description.getInstallLocation() == null)
			return false;
		return advisor.promptForLaunch(description);
	}

	/**
	 * Starts the p2 bundles needed to continue with the install.
	 */
	private IProvisioningAgent startAgent(InstallDescription description) throws CoreException {
		IPath installLocation = description.getInstallLocation();
		if (installLocation == null)
			throw fail(Messages.App_NoInstallLocation, null);
		//set agent location if specified
		IPath agentLocation = description.getAgentLocation();
		try {
			IProvisioningAgentProvider provider = (IProvisioningAgentProvider) getService(InstallerActivator.getDefault().getContext(), IProvisioningAgentProvider.SERVICE_NAME);
			return provider.createAgent(agentLocation == null ? null : agentLocation.toFile().toURI());
		} catch (ProvisionException e) {
			throw fail(Messages.App_FailedStart, e);
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
