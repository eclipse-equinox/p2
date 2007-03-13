/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.frameworkadmin.equinox.internal;

public class EquinoxConstants {
	
	/**
	 * If BundleContext#getProperty(PROP_KEY_USE_REFERENCE) does not equal "false", 
	 * Manipulator#save() will add "reference:" to any bundle location specified osgi.bundles in order to avoid
	 * caching its bundle jar.  Otherwise, it will add nothing to any bundle location.
	 */
	public static final String PROP_KEY_USE_REFERENCE = "org.eclipse.equinox.frameworkadmin.equinox.useReference";

	
	public static final String PLUGINS_DIR = "plugins";
	public final static String FW_SYMBOLIC_NAME = "org.eclipse.osgi";
	//public static final String FW_JAR_PLUGIN_NAME = "org.eclipse.osgi";
	public static final String DEFAULT_CONFIGURATION = "configuration";
	public static final String CONFIG_INI = "config.ini";
	final static String PROP_INITIAL = "osgi.clean";
	//	final static String PROP_FW_JAR = "org.eclipse.service.configManipulator.fwJar";

	//	private final static String DEFAULT_BUNDLE_SEARCH_DIR = "PLUGINS";
	public final static String FW_VERSION = "3.3";
	public final static String FW_NAME = "Equinox";
	public final static String LAUNCHER_VERSION = "3.2";
	public final static String LAUNCHER_NAME = "Eclipse.exe";

	public static final String AOL = "aol";

	public static final String OPTION_CONFIGURATION = "-configuration";
	public static final String OPTION_FW = "-framework";
	public static final String OPTION_VM = "-vm";
	public static final String OPTION_VMARGS = "-vmargs";
	public static final String OPTION_INSTANCE = "-data";
	public static final String OPTION_INSTALL = "-install";
	public static final String OPTION_CONSOLE = "-console";
	public static final String CONSOLE_PORT_VALUE = "9000";
	public static final String OPTION_CLEAN = "-clean";
	public static final String OPTION_STARTUP = "-startup";

	//	private static final String CONSOLE_LOG = "-consoleLog"; //$NON-NLS-1$
	//	private static final String DEBUG = "-debug"; //$NON-NLS-1$
	//	private static final String INITIALIZE = "-initialize"; //$NON-NLS-1$
	//	private static final String DEV = "-dev"; //$NON-NLS-1$
	//	private static final String WS = "-ws"; //$NON-NLS-1$
	//	private static final String OS = "-os"; //$NON-NLS-1$
	//	private static final String ARCH = "-arch"; //$NON-NLS-1$
	//	private static final String NL = "-nl"; //$NON-NLS-1$	
	//	private static final String CONFIGURATION = "-configuration"; //$NON-NLS-1$	
	//	private static final String USER = "-user"; //$NON-NLS-1$
	//	private static final String NOEXIT = "-noExit"; //$NON-NLS-1$
	//
	//	// this is more of an Eclipse argument but this OSGi implementation stores its 
	//	// metadata alongside Eclipse's.
	//	private static final String OPTION_DATA = "-data"; //$NON-NLS-1$

	// System properties
	public static final String PROP_BUNDLES = "osgi.bundles"; //$NON-NLS-1$
	public static final String PROP_BUNDLES_STARTLEVEL = "osgi.bundles.defaultStartLevel"; //$NON-NLS-1$ //The start level used to install the bundles
	public static final String PROP_EXTENSIONS = "osgi.framework.extensions"; //$NON-NLS-1$
	public static final String PROP_INITIAL_STARTLEVEL = "osgi.startLevel"; //$NON-NLS-1$ //The start level when the fwl start
	public static final String PROP_DEBUG = "osgi.debug"; //$NON-NLS-1$
	public static final String PROP_DEV = "osgi.dev"; //$NON-NLS-1$
	public static final String PROP_CLEAN = "osgi.clean"; //$NON-NLS-1$
	public static final String PROP_INSTALL = "osgi.install"; //$NON-NLS-1$
	public static final String PROP_CONSOLE = "osgi.console"; //$NON-NLS-1$
	public static final String PROP_CONSOLE_CLASS = "osgi.consoleClass"; //$NON-NLS-1$
	public static final String PROP_CHECK_CONFIG = "osgi.checkConfiguration"; //$NON-NLS-1$
	public static final String PROP_OS = "osgi.os"; //$NON-NLS-1$
	public static final String PROP_WS = "osgi.ws"; //$NON-NLS-1$
	public static final String PROP_NL = "osgi.nl"; //$NON-NLS-1$
	public static final String PROP_ARCH = "osgi.arch"; //$NON-NLS-1$
	public static final String PROP_ADAPTOR = "osgi.adaptor"; //$NON-NLS-1$
	public static final String PROP_SYSPATH = "osgi.syspath"; //$NON-NLS-1$

	public static final String PROP_CONFIGURATION_AREA = "osgi.configuration.area"; //$NON-NLS-1$
	public static final String PROP_ECLIPSE_COMMANDS = "eclipse.commands"; //$NON-NLS-1$
	public static final String PROP_OSGI_FW = "osgi.framework";

	public static final String PROP_BUNDLES_EXTRADATA = "osgi.bundles.extraData"; //$NON-NLS-1$

	public static final String PROP_LAUNCHER_PATH = "osgi.launcherPath"; //$NON-NLS-1$
	public static final String PROP_LAUNCHER_NAME = "osgi.launcherIni"; //$NON-NLS-1$

	public static final String INI_EXTENSION = ".ini";
	public static final String EXE_EXTENSION = ".exe";

	public static final String PROP_EQUINOX_DEPENDENT_PREFIX = "osgi.";
	static final String REFERENCE = "reference:";
	public static final String PERSISTENT_DIR_NAME = "org.eclipse.osgi";

}
