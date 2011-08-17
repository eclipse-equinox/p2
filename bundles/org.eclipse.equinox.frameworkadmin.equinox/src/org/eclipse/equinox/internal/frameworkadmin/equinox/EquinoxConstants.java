/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox;

public class EquinoxConstants {

	/**
	 * If BundleContext#getProperty(PROP_KEY_USE_REFERENCE) does not equal "false", 
	 * Manipulator#save() will add "reference:" to any bundle location specified osgi.bundles in order to avoid
	 * caching its bundle jar.  Otherwise, it will add nothing to any bundle location.
	 */
	public static final String PROP_KEY_USE_REFERENCE = "org.eclipse.equinox.frameworkadmin.equinox.useReference"; //$NON-NLS-1$

	public static final String PLUGINS_DIR = "plugins"; //$NON-NLS-1$
	public final static String FW_SYMBOLIC_NAME = "org.eclipse.osgi"; //$NON-NLS-1$
	public static final String DEFAULT_CONFIGURATION = "configuration"; //$NON-NLS-1$
	public static final String CONFIG_INI = "config.ini"; //$NON-NLS-1$

	public final static String FW_VERSION = "3.3"; //$NON-NLS-1$
	public final static String FW_NAME = "Equinox"; //$NON-NLS-1$
	public final static String LAUNCHER_VERSION = "3.2"; //$NON-NLS-1$
	public final static String LAUNCHER_NAME = "Eclipse.exe"; //$NON-NLS-1$

	public static final String OPTION_CONFIGURATION = "-configuration"; //$NON-NLS-1$
	public static final String OPTION_FW = "-framework"; //$NON-NLS-1$
	public static final String OPTION_VM = "-vm"; //$NON-NLS-1$
	public static final String OPTION_VMARGS = "-vmargs"; //$NON-NLS-1$
	public static final String OPTION_CLEAN = "-clean"; //$NON-NLS-1$
	public static final String OPTION_STARTUP = "-startup"; //$NON-NLS-1$
	public static final String OPTION_INSTALL = "-install"; //$NON-NLS-1$
	public static final String OPTION_LAUNCHER_LIBRARY = "--launcher.library"; //$NON-NLS-1$

	// System properties
	public static final String PROP_BUNDLES = "osgi.bundles"; //$NON-NLS-1$
	public static final String PROP_BUNDLES_STARTLEVEL = "osgi.bundles.defaultStartLevel"; //$NON-NLS-1$ //The start level used to install the bundles
	public static final String PROP_INITIAL_STARTLEVEL = "osgi.startLevel"; //$NON-NLS-1$ //The start level when the fwl start
	public static final String PROP_INSTALL = "osgi.install"; //$NON-NLS-1$
	public static final String PROP_ECLIPSE_COMMANDS = "eclipse.commands"; //$NON-NLS-1$
	public static final String PROP_FW_EXTENSIONS = "osgi.framework.extensions"; //$NON-NLS-1$
	public static final String PROP_OSGI_FW = "osgi.framework"; //$NON-NLS-1$
	public static final String PROP_OSGI_SYSPATH = "osgi.syspath"; //$NON-NLS-1$
	public static final String PROP_LAUNCHER_NAME = "osgi.launcherIni"; //$NON-NLS-1$
	public static final String PROP_SHARED_CONFIGURATION_AREA = "osgi.sharedConfiguration.area"; //$NON-NLS-1$	

	public static final String INI_EXTENSION = ".ini"; //$NON-NLS-1$
	public static final String EXE_EXTENSION = ".exe"; //$NON-NLS-1$

	public static final String PROP_EQUINOX_DEPENDENT_PREFIX = "osgi."; //$NON-NLS-1$
	static final String REFERENCE = "reference:"; //$NON-NLS-1$
	public static final String PERSISTENT_DIR_NAME = "org.eclipse.osgi"; //$NON-NLS-1$

}
