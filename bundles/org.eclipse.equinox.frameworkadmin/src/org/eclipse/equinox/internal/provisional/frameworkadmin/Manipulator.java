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
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.configuratormanipulator.ConfiguratorManipulator;

/**
 * A manipulator is used to query and modify the state of a framework instance. 
 * A manipulator instance is obtained by calling {@link FrameworkAdmin#getManipulator()}.
 * 
 * The typical use-cases of this interface:
 * 
 * Usecase 1: set parameters, check the expected state, save them into configuration files, and launch. 
 * 	A. create a {@link Manipulator} object from a {@link FrameworkAdmin}.
 *  B. set parameters to the {@link Manipulator} object.
 *  C. getExpectedState() and check what bundle state will be realized.
 *     If it is not same as you desire, repeat B and C until it becomes as you desire.
 *  D. save parameters into configuration files by {@link Manipulator#save(boolean)}.
 *  E. launch the framework by {@link FrameworkAdmin#launch(Manipulator, File)}.
 *     
 * Usecase 2: set parameters required for loading, load parameters from configuration files,
 * 		  check the expected state, and launch. 
 * 	A. create a {@link Manipulator} object from a {@link FrameworkAdmin}.
 *  B. set parameters about launcher or framework configuration file to the {@link Manipulator} object.
 *  C. load parameters from configuration files by {@link Manipulator#load()};
 *  D. getExpectedState() and check what bundle state will be realized.
 *  E. launch the framework by {@link FrameworkAdmin#launch(Manipulator, File)}.
 * @see FrameworkAdmin
 * @see ConfigData
 * @see LauncherData
 */
public interface Manipulator {

	/**
	 * Return the newly created BundldsState object,
	 * according to the parameters set to this object "in memory".
	 * 
	 * None of launcher config file, framework config file and configurator config file
	 * will be read by this method. However, the framework persistent data location should be
	 * taken into consideration. In other words, this method will return 
	 * the expected {@link BundlesState} object assuming that the current parameters were saved and 
	 * {@link FrameworkAdmin#launch(Manipulator, File)} with an argument of this object 
	 * were called. (It would read the framework persistent data location if required).
	 *  
	 * This method should not modify the parameters in this {@link Manipulator} object.
	 * 
	 * @return framework bundle state object created according to he current parameters set.
	 * @throws FrameworkAdminRuntimeException - If the {@link FrameworkAdmin} service created this object is unregistered or this implementation doesn't support this method. 
	 */
	BundlesState getBundlesState() throws FrameworkAdminRuntimeException;

	/**
	 * The reference of {@link ConfigData} object representing configuration information related with framework settings will be returned.
	 * Remind that manipulating returned object will affect this Manipulator behavior.
	 *  
	 * @return ConfigData object representing configuration information related with framework setting 
	 * @throws FrameworkAdminRuntimeException - If the {@link FrameworkAdmin} service created this object is unregistered or this implementation doesn't support this method. 
	 * @see ConfigData
	 */
	ConfigData getConfigData() throws FrameworkAdminRuntimeException;

	/**
	 * Return the expected BundleInfo array representing state of bundles,
	 * according to the parameters set to this object "in memory".
	 * 
	 * None of launcher config file, framework config file and configurator config file
	 * will be read by this method. However, the framework persistent data location should be
	 * taken into consideration. In other words, this method will return 
	 * the expected bundles state assuming that the current parameters were saved and 
	 * {@link FrameworkAdmin#launch(Manipulator, File)} with an argument of this object 
	 * were called. (It would read the framework persistent data location if required).
	 * 
	 * Returned BundleInfos must have resolved flag set.
	 * This method should not modify the parameters in this {@link Manipulator} object. 
	 * 
	 * cf. getConfigData().getBundles() will return array of BundleInfo too.
	 * 	However the resolved flag of returned BundleInfos might not be reliable.
	 * 
	 * This method is equivalent to calling getBundlesState().getExpectedState().
	 *  
	 * @return array of BundleInfo representing expected state of all bundles installed.
	 * @throws IllegalArgumentException - If either of fwJar or cwd doesn't exist.
	 * @throws IOException - If reading fw configuration file or reading persistently recorded information 
	 *   of fw fails. 
	 * @throws FrameworkAdminRuntimeException - If the {@link FrameworkAdmin} service created this object is unregistered or this implementation doesn't support this method. 
	 */
	BundleInfo[] getExpectedState() throws IllegalStateException, IOException, FrameworkAdminRuntimeException;

	/**
	 * The reference of {@link LauncherData} object representing configuration information
	 * related with launcher settings will be returned. 
	 * Remember that manipulating returned object will affect this Manipulator object behavior.
	 * 
	 * @return LauncherData object representing configuration information related with launcher setting 
	 * @throws FrameworkAdminRuntimeException - If the ManipulatorAdmin service created this object is unregistered or this implementation doesn't support this method. 
	 * @see LauncherData
	 */
	LauncherData getLauncherData() throws FrameworkAdminRuntimeException;

	/**
	 * Return timestamp of configurations which will be loaded by load() method
	 * according to the parameters set to this manipulator in long value.
	 * 
	 * This method will check last modified time of all launcher configuration file, framework configuration file,
	 * and framework persistent storage according to the parameters set.
	 * @return long
	 */
	long getTimeStamp();

	/**
	 * Initialize all information that this object keeps at that time.
	 */
	void initialize();

	/**
	 * load configs from appropriate config files, 
	 * including launcher config file, fw config file, and configurator config files, 
	 * whose locations are determined by the current setting. In addition, 
	 * the fw persistent data location should be taken into consideration. 
	 * 
	 * The following procedure contains the matters of implementation detail.
	 * However, it is an example how it works.
	 * 
	 * 1. if launcher object is set, corresponding launcher config file will be read.
	 * According to the information retrieved, setting of this object will be updated.
	 * including fw config file.
	 * 
	 * 2. If fw config file is not specified, IllegalStateException will be thrown. 
	 * Otherwise, the information will be retrieved from the fw config file.
	 * 
	 * 3. If any ConfiguratorBundle is included in the bundle list,
	 * read appropriate configurator config file by 
	 * {@link ConfiguratorManipulator#updateBundles(Manipulator)},
	 *  which will update the parameter about installed bundles in its 
	 *  {@link Manipulator#getConfigData()} object.
	 *  
	 * Most old parameters will be updated by this method call. 
	 * 
	 * @throws IOException - If reading info from configuration files fails. 
	 * @throws IllegalStateException - If config files cannot be determined.
	 * @throws FrameworkAdminRuntimeException - If the {@link FrameworkAdmin} service created this object is unregistered or this implementation doesn't support this method. 
	 */
	void load() throws IllegalStateException, IOException, FrameworkAdminRuntimeException;

	/**
	 * Save parameters that this object keeps at that time into appropriate configuration files, 
	 * which include launcher configuration file, framework configuration file, and configurator configuration files
	 * (if required and implementation of this object supports), according to the current setting and situation. 
	 * 
	 * The following procedure contains the matters of implementation detail.
	 * However, it is an example how it works.
	 * 
	 * 1. if a launcher file is set,
	 * the parameters to be saved into a LauncherConfigFile will be saved into the default LauncherConfigFile
	 * that is determined by the location of the launcher file.
	 * 
	 * 
	 * 2. if there are any {@link ConfiguratorManipulator} objects available whose corresponding ConfiguratorBundle
	 * is set to be started, choose the ConfiguratorBudnle that starts the first among them and go to next step.
	 * Otherwise, save the BundleInfo[] set to this object into a FwConfigFile that is determined
	 * by the parameters set. 
	 *  
	 * 3. call {@link ConfiguratorManipulator#save(Manipulator, boolean)} of 
	 * the ConfiguratorManipulator that can manipulate the chosen ConfiguratorBudnle. 
	 * This method will save configurations for ConfiguratorBundle to read appropriately  
	 * and return BundleInfo[] to be saved in the FwConfigFile, which is determined by the parameters set.
	 * 
	 * 4. Save the returned BundleInfo[] in the FwConfigFile, which is determined by the parameters set.
	 * 	   
	 * @param backup - if true, keep old file by renaming if exists. 
	 * @throws IOException - If writing info into configuration files fails. 
	 * @throws FrameworkAdminRuntimeException - If the {@link FrameworkAdmin} service created this object is unregistered or this implementation doesn't support this method. 
	 */
	void save(boolean backup) throws IOException, FrameworkAdminRuntimeException;

	/**
	 * Copy all information the specified {@link ConfigData} contains into this object.
	 * All of old settings will be initialized and replaced.
	 * 
	 * @param configData fw config data to be set to this object.
	 */
	void setConfigData(ConfigData configData);

	/**
	 * Copy all information the specified {@link LauncherData} contains into this object.
	 * All of old settings will be initialized and replaced.
	 * 
	 * @param launcherData launcher config data to be set to this object.
	 */
	void setLauncherData(LauncherData launcherData);
}
