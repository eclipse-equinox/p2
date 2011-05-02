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

import org.eclipse.equinox.frameworkadmin.BundleInfo;

/**
 * This API is used for expecting bundles state virtually.
 * 
 * This object is instantiated by {@link Manipulator#getBundlesState()}.
 * At its instantiation, state of the bundles will be created in this object 
 * according to the parameters which the calling {@link Manipulator} object has.
 * For its creation, information in the fw persistent data will be taken into account. 
 * 
 * Modification of this object after its instantiation never affects the parameters
 * in the calling {@link Manipulator} object.  
 *  
 * XXX Implementation of Simple ConfiguratorManipulator needs the functions.   
 *  
 * @see Manipulator
 */
public interface BundlesState {

	/** Return expected bundles state currently kept in this object. 
	 * 
	 * The implementation of this method will try to resolve the state
	 * if resolving a state is supported.
	 * 
	 * @return bundle array of BundleInfo currently composed in this object.
	 * @throws FrameworkAdminRuntimeException - If the {@link FrameworkAdmin} service created the parent {@link Manipulator} is unregistered. 
	 */
	BundleInfo[] getExpectedState() throws FrameworkAdminRuntimeException;

	/**	
	 * Return required bundles to be resolve the specified bInfo
	 * under the state currently composed.  
	 * 
	 * The implementation of this method will try to resolve the state
	 * if resolving a state is supported. 
	 * 
	 * @param bInfo bundleinfo whose prerequisite bundles will be searched.
	 * @return bundle array of BundleInfos required for the specified bInfo to be resolved. 
	 */
	BundleInfo[] getPrerequisteBundles(BundleInfo bInfo);

	/**
	 * Return a bundle to be used as a framework under the state currently composed.  
	 * @return a bundle to be used as a framework under the state currently composed. 
	 */
	public BundleInfo getSystemBundle();

	/**
	 * Return bundles which are fragment bundles of the framework under the state currently composed.  
	 * @return array of BundleInfos which are fragment bundles of the framework.
	 */
	public BundleInfo[] getSystemFragmentedBundles();

	/**
	 * Return array of Strings which tells the unsatisfied constaints
	 * to resolve the specified bInfo under the state currently composed.  
	 * 
	 * If this implementation doesn't support resolving state,
	 * FrameworkAdminRuntimeException with a cause of {@link FrameworkAdminRuntimeException#UNSUPPORTED_OPERATION}
	 * will be thrown.
	 * 
	 * XXX this method is prepared mainly for debugging. 
	 * 
	 * @param bInfo
	 * @return array of Strings which tells the unsatisfied constaints.
	 * @throws FrameworkAdminRuntimeException if this implementation doesn't support resolving state, FrameworkAdminRuntimeException with a cause of {@link FrameworkAdminRuntimeException#UNSUPPORTED_OPERATION}  will be thrown.
	 */
	public String[] getUnsatisfiedConstraints(BundleInfo bInfo) throws FrameworkAdminRuntimeException;

	/**
	 * Install the specified bInfo as a installed bundle to the current state virtually.
	 * Note that resolve this bundle is not done in this implementation.
	 * 
	 * @param bInfo BundleInfo to be installed
	 * @throws FrameworkAdminRuntimeException - If the {@link FrameworkAdmin} service created the parent {@link Manipulator} is unregistered. 
	 */
	void installBundle(BundleInfo bInfo) throws FrameworkAdminRuntimeException;

	/**
	 * Return true if this implementation supports full functions,
	 *  such as resolving bundles and .
	 * Otherwise false will be returend.
	 * @return  true if this implementation supports resolving state. Otherwise false.
	 */
	boolean isFullySupported();

	/**
	 * Return true if the state currently composed is resolved after the last change of the state.
	 * Otherwise false. 
	 * 
	 * If this implementation doesn't support resolving state,
	 * FrameworkAdminRuntimeException with a cause of {@link FrameworkAdminRuntimeException#UNSUPPORTED_OPERATION}
	 * will be thrown.
	 * 
	 * @return true if the state currently composed is resolved after the last change of the state. Otherwise false. 
	 * @throws FrameworkAdminRuntimeException if this implementation doesn't support resolving state, FrameworkAdminRuntimeException with a cause of {@link FrameworkAdminRuntimeException#UNSUPPORTED_OPERATION}  will be thrown.
	 */
	public boolean isResolved() throws FrameworkAdminRuntimeException;

	/**
	 * Return true if the specified bundle is resolved.
	 * Otherwise false. 
	 * 
	 * If this implementation doesn't support resolving state,
	 * FwLauncherException with a cause of {@link FrameworkAdminRuntimeException#UNSUPPORTED_OPERATION}
	 * will be thrown.
	 * 
	 * @return true if the specified bundle is resolved. Otherwise false. 
	 * @throws FrameworkAdminRuntimeException if this implementation doesn't support resolving state, FrameworkAdminRuntimeException with a cause of {@link FrameworkAdminRuntimeException#UNSUPPORTED_OPERATION}  will be thrown.
	 */
	public boolean isResolved(BundleInfo bInfo) throws FrameworkAdminRuntimeException;

	/**	
	 * Resolves the constraints contained in this state.
	 * 
	 * If this implementation doesn't support resolving state,
	 * FrameworkAdminRuntimeException with a cause of {@link FrameworkAdminRuntimeException#UNSUPPORTED_OPERATION}
	 * will be thrown.
	 * 	 
	 * @param incremental a flag controlling whether resolution should be incremental
	 * @throws FrameworkAdminRuntimeException 
	 */
	void resolve(boolean increment) throws FrameworkAdminRuntimeException;

	/**
	 * Uninstall the specified bInfo from the current state virtually. 
	 * 
	 * @param bInfo BundleInfo to be uninstalled
	 * @throws FrameworkAdminRuntimeException - If the {@link FrameworkAdmin} service created the parent {@link Manipulator} is unregistered. 
	 */
	void uninstallBundle(BundleInfo bInfo) throws FrameworkAdminRuntimeException;
}
