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
package org.eclipse.equinox.internal.provisional.configuratormanipulator;

import java.io.File;
import java.io.IOException;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdmin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;

/**
 * 
 * This interface provides methods that enable client bundles to 
 * manipulate the corresponding ConfiguratorBundle.
 * 
 *  see org.eclipse.equinox.internal.provisional.configurator.Configurator
 */

public interface ConfiguratorManipulator {
	String SERVICE_PROP_KEY_CONFIGURATOR_BUNDLESYMBOLICNAME = "org.eclipse.equinox.configurator.BundleSymbolicName"; //$NON-NLS-1$

	/**
	 * Save configuration for the corresponding Configurator Bundle so that 
	 * Bundles kept by the specified {@link Manipulator} would be installed after completion of a launch.
	 * The location of a configuration file is determined by the parameters set to the Manipulator object
	 * and it depends on the corresponding ConfiguratorBundle implementation.
	 * 
	 * While some parameters of the {@link Manipulator} object will be modified (for setting info about the 
	 * location the ConfiguratorBundle would be able to refer in a future launch), 
	 * the Bundles kept by the {@link Manipulator} object should not be modified.
	 * 
	 * Instead, it will return BundleInfo[] to be managed not by the ConfiguratorBundle.
	 * These values are supposed to be saved into fw config files.
	 * 
	 * If backup flag is true, a file have existed already under the same name
	 * will be renamed into another name as a backup. 
	 * 
	 * We assume that the implementation of {@link Manipulator#save(boolean)} will call this method.
	 *  
	 * @return array of BundleInfo to be saved as installing bundles in fw config files.
	 * @param manipulator {@link Manipulator} object which contains the bundles to be installed finally.
	 * @param backup if files exists at the location to save, it will be copied as a backup.
	 * @throws IOException - If fail to save configuration for the corresponding Configurator Bundle.
	 */
	BundleInfo[] save(Manipulator manipulator, boolean backup) throws IOException;

	/**
	 * Update bundles kept by the specified {@link Manipulator} object into installed bundles
	 * if {@link FrameworkAdmin#launch(Manipulator, File)} with the specified 
	 * {@link Manipulator} is called taking the corresponding ConfiguratorBundle behaivior into account.
	 * 
	 * If there is no corresponding ConfiguratorBundle in Manipulator.getConfigData().getBundles(),
	 * just return.
	 * 
	 * The BundleInfo[] of the specified Manipulator object will be modified.
	 *
	 * This method is assumed to be called from {@link Manipulator#load()}.
	 *  
	 * @param manipulator {@link Manipulator} object to be used and updated.
	 * @throws IOException - If fail to read configuration for the corresponding Configurator Bundle.
	 */
	void updateBundles(Manipulator manipulator) throws IOException;

	void cleanup(Manipulator manipulator);
}
