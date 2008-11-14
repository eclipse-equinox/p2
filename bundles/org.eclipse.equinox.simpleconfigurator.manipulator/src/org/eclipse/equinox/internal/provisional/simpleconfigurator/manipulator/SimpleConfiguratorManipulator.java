/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.simpleconfigurator.manipulator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.eclipse.equinox.internal.provisional.configuratormanipulator.ConfiguratorManipulator;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;

public interface SimpleConfiguratorManipulator extends ConfiguratorManipulator {
	/**
	 * An instance of an ISimpleConfiguratorManipulator is registered as a ConfiguratorManipulator 
	 * service with ConfiguratorManipulator.SERVICE_PROP_KEY_CONFIGURATOR_BUNDLESYMBOLICNAME =
	 * SERVICE_PROP_VALUE_CONFIGURATOR_SYMBOLICNAME.
	 */
	public static final String SERVICE_PROP_VALUE_CONFIGURATOR_SYMBOLICNAME = "org.eclipse.equinox.simpleconfigurator";

	/**
	 * 
	 * @param url
	 * @param launcherLocation
	 * @return
	 * @throws IOException
	 */
	public BundleInfo[] loadConfiguration(URL url, File launcherLocation) throws IOException;

	/**
	 * 
	 * @param bundleInfoList
	 * @param outputFile
	 * @param base
	 * @throws IOException
	 */
	public void saveConfiguration(List bundleInfoList, File outputFile, File base) throws IOException;
}
