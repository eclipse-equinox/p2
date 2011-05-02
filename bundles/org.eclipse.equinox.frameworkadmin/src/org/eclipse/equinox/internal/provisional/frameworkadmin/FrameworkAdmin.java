/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.frameworkadmin;

import java.io.File;
import java.io.IOException;

/**
 * The implementation of this API will be registered to the service 
 * registry as an OSGi service.
 * 
 * *****************************************************************
 * 1. For developers who implement bundles that register this service.
 * 
 * Upon registration, the following service properties must be set to allow clients to 
 * search the FrameworkAdmin object, which will create BundlesState / ConfigData / LauncherData objects.
 * 
 * {@link FrameworkAdmin#SERVICE_PROP_KEY_FW_NAME}: String; name of the framework 
 * {@link FrameworkAdmin#SERVICE_PROP_KEY_FW_VERSION}: String; version of the framework
 * {@link FrameworkAdmin#SERVICE_PROP_KEY_LAUNCHER_NAME}: String; name of the launcher 
 * {@link FrameworkAdmin#SERVICE_PROP_KEY_LAUNCHER_VERSION}: String; version of the launcher 
 *
 * Bundles that register this service will check if the currently running system can be manipulated by 
 * this FrameworkAdmin. If yes and this implementation can create an initialized Manipulator object
 * according to the running framework and launcher, add the service property keyed by 
 * 
 * {@link FrameworkAdmin#SERVICE_PROP_KEY_RUNNING_SYSTEM_FLAG}: String; if "true", 
 * the service that will be returned by getRunningManipulator() is fully initialized to 
 * represent the state of the running system.
 *     
 * It is recommended to implement Manipulator objects created by calling methods of this interface
 * so that they cannot be used after this service is unregistered.  
 *     
 * *****************************************************************
 * 2. For developers who implement client bundles that use this service.
 * 
 * A client of this service can obtain a Manipulator object by calling the {@link #getManipulator()} method.
 * 
 * A client can search among services registered in a service registry to find the 
 * desired FrameworkAdmin implementation that matches the desired framework 
 * type, framework version, launcher type, and launcher version.
 * 
 * In order for a client bundle to manipulate the {@link Manipulator} object 
 * of the running framework and launcher, the service filter (FrameworkAdmin#SERVICE_PROP_KEY_RUNNING_FW_FLAG=true) 
 * should be used.   
 * 
 * As with all OSGi services, the client bundle should track this service state.
 * If the service is unregistered, it should stop using any of the objects obtained from this service and 
 * release them. If it continues to use them, {@link FrameworkAdminRuntimeException} might 
 * be thrown. 
 * 
 * *****************************************************************
 * In addition, FrameworkAdminFactory will create this object. This is used by Java programs.
 * 
 * @see FrameworkAdminFactory
 *    
 */
public interface FrameworkAdmin {

	String SERVICE_PROP_KEY_FW_NAME = "org.eclipse.equinox.frameworkhandler.framework.name"; //$NON-NLS-1$
	String SERVICE_PROP_KEY_FW_VERSION = "org.eclipse.equinox.frameworkhandler.framework.version"; //$NON-NLS-1$

	String SERVICE_PROP_KEY_LAUNCHER_NAME = "org.eclipse.equinox.frameworkhandler.launcher.name"; //$NON-NLS-1$
	String SERVICE_PROP_KEY_LAUNCHER_VERSION = "org.eclipse.equinox.frameworkhandler.launcher.version"; //$NON-NLS-1$
	String SERVICE_PROP_KEY_RUNNING_SYSTEM_FLAG = "org.eclipse.equinox.frameworkhandler.runningfwflag"; //$NON-NLS-1$

	/**
	 * Create new instance of {@link Manipulator} and return it.
	 * 
	 * @return new instance of Manipulator.
	 */
	public Manipulator getManipulator();

	/**
	 * Create new instance of {@link Manipulator} for running system 
	 * and return it. The instance must be initialized fully according to the 
	 * running environment. If this implementation cannot provide it, return null.
	 * 
	 * @return new instance of Manipulator.
	 */
	public Manipulator getRunningManipulator();

	/**
	 * Launch a framework instance under the specified current working directory. 
	 * 
	 * @param manipulator {@link Manipulator} object to be launched.
	 * @param cwd current working directory to be used for launching.
	 * @return process
	 * @throws IllegalArgumentException if specified arguments are null.
	 * @throws IOException if any error relate with IO occurs
	 * @throws FrameworkAdminRuntimeException if the FrameworkAdmin service object
	 * 		 that created the specified Manipulator object is unregistered.
	 */
	public Process launch(Manipulator manipulator, File cwd) throws IllegalArgumentException, IOException, FrameworkAdminRuntimeException;

	/**
	 * 	
	 * @return true if this object is active. false otherwise.
	 */
	public boolean isActive();
}
