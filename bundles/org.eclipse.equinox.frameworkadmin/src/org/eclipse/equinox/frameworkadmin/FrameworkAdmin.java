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
package org.eclipse.equinox.frameworkadmin;

import java.io.File;
import java.io.IOException;

/**
 * The implementation of this API will be registered to the service 
 * registry as an OSGi service.
 * 
 * *****************************************************************
 * 1. For developers who implement bundles that register this service.
 * 
 * At the registration, the following 
 *  service properties must be set for client to search the FrameworkAdmin object
 *  which will create BundlesState / ConfigData / LauncherData objects.
 * 
 * {@link FrameworkAdmin#SERVICE_PROP_KEY_FW_NAME}: String; name of the framework 
 * {@link FrameworkAdmin#SERVICE_PROP_KEY_FW_VERSION}: String; version of the framework
 * {@link FrameworkAdmin#SERVICE_PROP_KEY_LAUNCHER_NAME}: String; name of the launcher 
 * {@link FrameworkAdmin#SERVICE_PROP_KEY_LAUNCHER_VERSION}: String; version of the launcher 
 *
 * Bundles register this service will check if the currently runnning system can be manipulated by 
 * this FrameworkAdmin. If yes and this implementation can create an initialized Manipulator object
 * according to the running fw and launcher, add the service property keyed by 
 * 
 * {@link FrameworkAdmin#SERVICE_PROP_KEY_RUNNING_SYSTEM_FLAG}: String; if "true", this service that will be returned by getRunningManipulator() is fully initialized so as to represent the state of running system.
 *     
 * It is recommended to implement Manipulator objects created by calling methods of this interface
 * so that they cannot be used after this service is unregistered.  
 *     
 * *****************************************************************
 * 2. For developers who implement client bundles that use this service.
 * 
 * A client bundle of this service can get new Manipulator object 
 * by calling its method.
 * 
 * A client bundle can search among services registered in a service registry
 * and can get the desired FrameworkAdmin service object so that the bundle 
 * can get the desired {@link Manipulator} object for desired framework type with version and launcher type with version.
 * 
 * Especially, in order for a client bundle to manipulate the {@link Manipulator} object of the running fw and laucher,
 * filtering (FrameworkAdmin#SERVICE_PROP_KEY_RUNNING_FW_FLAG=true) is used.   
 * 
 * As generally speaking about OSGi service, the client bundle should track this service state.
 * If unregistered, it should stop using any of objects that it got by this service and 
 * release all of them. If it continues to use them, {@link FrameworkAdminRuntimeException} might 
 * be thrown. 
 * 
 * *****************************************************************
 * In addition, FrameworkAdminFactory will creat this object.
 * This is used by Java programs.
 * 
 * @see FrameworkAdminFactory
 *    
 */
public interface FrameworkAdmin {

	String SERVICE_PROP_KEY_FW_NAME = "org.eclipse.equinox.frameworkhandler.framework.name";
	String SERVICE_PROP_KEY_FW_VERSION = "org.eclipse.equinox.frameworkhandler.framework.version";

	String SERVICE_PROP_KEY_LAUNCHER_NAME = "org.eclipse.equinox.frameworkhandler.launcher.name";
	String SERVICE_PROP_KEY_LAUNCHER_VERSION = "org.eclipse.equinox.frameworkhandler.launcher.version";
	String SERVICE_PROP_KEY_RUNNING_SYSTEM_FLAG = "org.eclipse.equinox.frameworkhandler.runningfwflag";

	/**
	 * Create new instance of {@link Manipulator} and return it.
	 * 
	 * @return new instance of Manipulator.
	 */
	Manipulator getManipulator();

	/**
	 * Create new instance of {@link Manipulator} for running system 
	 * and return it. The instance must be initialized fully according to the 
	 * running environment. If this implementation cannot provide it, return null.
	 * 
	 * @return new instance of Manipulator.
	 */
	Manipulator getRunningManipulator();

	/**
	 * Launch a framework instance under the specified current working directory. 
	 * 
	 * 
	 * @param manipulator {@link Manipulator} object to be launched.
	 * @param cwd current working directory to be used for launching.
	 * @return process
	 * @throws IllegalArgumentException if specified arguments are null.
	 * @throws IOException if any error relate with IO occurs
	 * @throws FrameworkAdminRuntimeException if the FrameworkAdmin service object
	 * 		 that created the specified Manipulator object is unregistered.
	 */
	Process launch(Manipulator manipulator, File cwd) throws IllegalArgumentException, IOException, FrameworkAdminRuntimeException;

	/**
	 * 	
	 * @return true if this object is active. false otherwise.
	 */
	boolean isActive();
}
