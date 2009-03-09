/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.metadata.generator.features;

import java.io.*;
import java.util.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @since 3.1
 */
public class ProductFile extends DefaultHandler {
	private final static SAXParserFactory parserFactory = SAXParserFactory.newInstance();

	private static final String PROGRAM_ARGS = "programArgs"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_LINUX = "programArgsLin"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_MAC = "programArgsMac"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_SOLARIS = "programArgsSol"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_WIN = "programArgsWin"; //$NON-NLS-1$
	private static final String VM_ARGS = "vmArgs"; //$NON-NLS-1$
	private static final String VM_ARGS_LINUX = "vmArgsLin"; //$NON-NLS-1$
	private static final String VM_ARGS_MAC = "vmArgsMac"; //$NON-NLS-1$
	private static final String VM_ARGS_SOLARIS = "vmArgsSol"; //$NON-NLS-1$
	private static final String VM_ARGS_WIN = "vmArgsWin"; //$NON-NLS-1$

	private static final String SOLARIS_LARGE = "solarisLarge"; //$NON-NLS-1$
	private static final String SOLARIS_MEDIUM = "solarisMedium"; //$NON-NLS-1$
	private static final String SOLARIS_SMALL = "solarisSmall"; //$NON-NLS-1$
	private static final String SOLARIS_TINY = "solarisTiny"; //$NON-NLS-1$
	private static final String WIN32_16_LOW = "winSmallLow"; //$NON-NLS-1$
	private static final String WIN32_16_HIGH = "winSmallHigh"; //$NON-NLS-1$
	private static final String WIN32_24_LOW = "win24Low"; //$NON-NLS-1$
	private static final String WIN32_32_LOW = "winMediumLow"; //$NON-NLS-1$
	private static final String WIN32_32_HIGH = "winMediumHigh"; //$NON-NLS-1$
	private static final String WIN32_48_LOW = "winLargeLow"; //$NON-NLS-1$
	private static final String WIN32_48_HIGH = "winLargeHigh"; //$NON-NLS-1$

	private static final String OS_WIN32 = "win32";//$NON-NLS-1$
	private static final String OS_LINUX = "linux";//$NON-NLS-1$
	private static final String OS_SOLARIS = "solaris";//$NON-NLS-1$
	private static final String OS_MACOSX = "macosx";//$NON-NLS-1$

	private static final String PRODUCT = "product"; //$NON-NLS-1$
	private static final String CONFIG_INI = "configIni"; //$NON-NLS-1$
	private static final String LAUNCHER = "launcher"; //$NON-NLS-1$
	private static final String LAUNCHER_ARGS = "launcherArgs"; //$NON-NLS-1$
	private static final String PLUGINS = "plugins"; //$NON-NLS-1$
	private static final String FEATURES = "features"; //$NON-NLS-1$
	private static final String P_USE_ICO = "useIco"; //$NON-NLS-1$

	//These constants form a small state machine to parse the .product file
	private static final int STATE_START = 0;
	private static final int STATE_PRODUCT = 1;
	private static final int STATE_LAUNCHER = 2;
	private static final int STATE_LAUNCHER_ARGS = 3;
	private static final int STATE_PLUGINS = 4;
	private static final int STATE_FEATURES = 5;
	private static final int STATE_PROGRAM_ARGS = 6;
	private static final int STATE_PROGRAM_ARGS_LINUX = 7;
	private static final int STATE_PROGRAM_ARGS_MAC = 8;
	private static final int STATE_PROGRAM_ARGS_SOLARIS = 9;
	private static final int STATE_PROGRAM_ARGS_WIN = 10;
	private static final int STATE_VM_ARGS = 11;
	private static final int STATE_VM_ARGS_LINUX = 12;
	private static final int STATE_VM_ARGS_MAC = 13;
	private static final int STATE_VM_ARGS_SOLARIS = 14;
	private static final int STATE_VM_ARGS_WIN = 15;
	private static final int STATE_CONFIG_INI = 16;

	private int state = STATE_START;

	private final SAXParser parser;
	private String currentOS = null;
	private boolean useIco = false;
	private final ArrayList result = new ArrayList(6);
	private final Map platformSpecificConfigPaths = new HashMap();
	private String configPlatform = null;
	private String platformConfigPath = null;
	private String launcherName = null;
	private String id = null;
	private String uid = null;
	private boolean useFeatures = false;
	private List plugins = null;
	private List fragments = null;
	private List features = null;
	private String productName = null;
	private String version = null;

	private Properties launcherArgs = new Properties();

	private static String normalize(String text) {
		if (text == null || text.trim().length() == 0)
			return ""; //$NON-NLS-1$

		text = text.replaceAll("\\r|\\n|\\f|\\t", " "); //$NON-NLS-1$ //$NON-NLS-2$
		return text.replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Constructs a feature parser.
	 */
	public ProductFile(String location, String os) throws Exception {
		super();
		this.currentOS = os;

		parserFactory.setNamespaceAware(true);
		parser = parserFactory.newSAXParser();
		InputStream in = new BufferedInputStream(new FileInputStream(location));
		try {
			parser.parse(new InputSource(in), this);
		} finally {
			if (in != null)
				in.close();
		}
	}

	public List getPlugins() {
		return getPlugins(true);
	}

	public List getPlugins(boolean includeFragments) {
		List p = plugins != null ? plugins : Collections.EMPTY_LIST;
		if (!includeFragments)
			return p;

		List f = fragments != null ? fragments : Collections.EMPTY_LIST;
		int size = p.size() + f.size();
		if (size == 0)
			return Collections.EMPTY_LIST;

		List both = new ArrayList(size);
		both.addAll(p);
		both.addAll(f);
		return both;
	}

	public List getFeatures() {
		if (features == null)
			return Collections.EMPTY_LIST;
		return features;
	}

	public String getId() {
		if (uid != null)
			return uid;
		return id;
	}

	public String getProductId() {
		return id;
	}

	public String getProductName() {
		return productName;
	}

	public boolean useFeatures() {
		return useFeatures;
	}

	public String getVersion() {
		return (version == null || version.length() == 0) ? "0.0.0" : version; //$NON-NLS-1$
	}

	public String getLauncherName() {
		return launcherName;
	}

	public String getVMArguments(String os) {
		String key = null;
		if (os.equals(OS_WIN32)) {
			key = VM_ARGS_WIN;
		} else if (os.equals(OS_LINUX)) {
			key = VM_ARGS_LINUX;
		} else if (os.equals(OS_MACOSX)) {
			key = VM_ARGS_MAC;
		} else if (os.equals(OS_SOLARIS)) {
			key = VM_ARGS_SOLARIS;
		}

		String prefix = launcherArgs.getProperty(VM_ARGS);
		String platform = null, args = null;
		if (key != null)
			platform = launcherArgs.getProperty(key);
		if (prefix != null)
			args = platform != null ? prefix + " " + platform : prefix; //$NON-NLS-1$
		else
			args = platform != null ? platform : ""; //$NON-NLS-1$
		return normalize(args);
	}

	public String getProgramArguments(String os) {
		String key = null;
		if (os.equals(OS_WIN32)) {
			key = PROGRAM_ARGS_WIN;
		} else if (os.equals(OS_LINUX)) {
			key = PROGRAM_ARGS_LINUX;
		} else if (os.equals(OS_MACOSX)) {
			key = PROGRAM_ARGS_MAC;
		} else if (os.equals(OS_SOLARIS)) {
			key = PROGRAM_ARGS_SOLARIS;
		}

		String prefix = launcherArgs.getProperty(PROGRAM_ARGS);
		String platform = null, args = null;
		if (key != null)
			platform = launcherArgs.getProperty(key);
		if (prefix != null)
			args = platform != null ? prefix + " " + platform : prefix; //$NON-NLS-1$
		else
			args = platform != null ? platform : ""; //$NON-NLS-1$
		return normalize(args);
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		switch (state) {
			case STATE_START :
				if (PRODUCT.equals(localName)) {
					processProduct(attributes);
					state = STATE_PRODUCT;
				}
				break;

			case STATE_PRODUCT :
				if (CONFIG_INI.equals(localName)) {
					processConfigIni(attributes);
					state = STATE_CONFIG_INI;
				} else if (LAUNCHER.equals(localName)) {
					processLauncher(attributes);
					state = STATE_LAUNCHER;
				} else if (PLUGINS.equals(localName)) {
					state = STATE_PLUGINS;
				} else if (FEATURES.equals(localName)) {
					state = STATE_FEATURES;
				} else if (LAUNCHER_ARGS.equals(localName)) {
					state = STATE_LAUNCHER_ARGS;
				}
				break;

			case STATE_CONFIG_INI :
				processConfigIniPlatform(localName, true);
				break;

			case STATE_LAUNCHER :
				if (OS_SOLARIS.equals(localName)) {
					processSolaris(attributes);
				} else if ("win".equals(localName)) { //$NON-NLS-1$
					processWin(attributes);
				} else if (OS_LINUX.equals(localName)) {
					processLinux(attributes);
				} else if (OS_MACOSX.equals(localName)) {
					processMac(attributes);
				}
				if ("ico".equals(localName)) { //$NON-NLS-1$
					processIco(attributes);
				} else if ("bmp".equals(localName)) { //$NON-NLS-1$
					processBmp(attributes);
				}
				break;

			case STATE_LAUNCHER_ARGS :
				if (PROGRAM_ARGS.equals(localName)) {
					state = STATE_PROGRAM_ARGS;
				} else if (PROGRAM_ARGS_LINUX.equals(localName)) {
					state = STATE_PROGRAM_ARGS_LINUX;
				} else if (PROGRAM_ARGS_MAC.equals(localName)) {
					state = STATE_PROGRAM_ARGS_MAC;
				} else if (PROGRAM_ARGS_SOLARIS.equals(localName)) {
					state = STATE_PROGRAM_ARGS_SOLARIS;
				} else if (PROGRAM_ARGS_WIN.equals(localName)) {
					state = STATE_PROGRAM_ARGS_WIN;
				} else if (VM_ARGS.equals(localName)) {
					state = STATE_VM_ARGS;
				} else if (VM_ARGS_LINUX.equals(localName)) {
					state = STATE_VM_ARGS_LINUX;
				} else if (VM_ARGS_MAC.equals(localName)) {
					state = STATE_VM_ARGS_MAC;
				} else if (VM_ARGS_SOLARIS.equals(localName)) {
					state = STATE_VM_ARGS_SOLARIS;
				} else if (VM_ARGS_WIN.equals(localName)) {
					state = STATE_VM_ARGS_WIN;
				}
				break;

			case STATE_PLUGINS :
				if ("plugin".equals(localName)) { //$NON-NLS-1$
					processPlugin(attributes);
				}
				break;

			case STATE_FEATURES :
				if ("feature".equals(localName)) { //$NON-NLS-1$
					processFeature(attributes);
				}
				break;
		}
	}

	public void endElement(String uri, String localName, String qName) {
		switch (state) {
			case STATE_PLUGINS :
				if (PLUGINS.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_FEATURES :
				if (FEATURES.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_LAUNCHER_ARGS :
				if (LAUNCHER_ARGS.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_LAUNCHER :
				if (LAUNCHER.equals(localName))
					state = STATE_PRODUCT;
				break;

			case STATE_PROGRAM_ARGS :
			case STATE_PROGRAM_ARGS_LINUX :
			case STATE_PROGRAM_ARGS_MAC :
			case STATE_PROGRAM_ARGS_SOLARIS :
			case STATE_PROGRAM_ARGS_WIN :
			case STATE_VM_ARGS :
			case STATE_VM_ARGS_LINUX :
			case STATE_VM_ARGS_MAC :
			case STATE_VM_ARGS_SOLARIS :
			case STATE_VM_ARGS_WIN :
				state = STATE_LAUNCHER_ARGS;
				break;

			case STATE_CONFIG_INI :
				if (CONFIG_INI.equals(localName))
					state = STATE_PRODUCT;
				else
					processConfigIniPlatform(localName, false);
				break;
		}
	}

	public void characters(char[] ch, int start, int length) {
		switch (state) {
			case STATE_PROGRAM_ARGS :
				addLaunchArgumentToMap(PROGRAM_ARGS, String.valueOf(ch, start, length));
				break;
			case STATE_PROGRAM_ARGS_LINUX :
				addLaunchArgumentToMap(PROGRAM_ARGS_LINUX, String.valueOf(ch, start, length));
				break;
			case STATE_PROGRAM_ARGS_MAC :
				addLaunchArgumentToMap(PROGRAM_ARGS_MAC, String.valueOf(ch, start, length));
				break;
			case STATE_PROGRAM_ARGS_SOLARIS :
				addLaunchArgumentToMap(PROGRAM_ARGS_SOLARIS, String.valueOf(ch, start, length));
				break;
			case STATE_PROGRAM_ARGS_WIN :
				addLaunchArgumentToMap(PROGRAM_ARGS_WIN, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS :
				addLaunchArgumentToMap(VM_ARGS, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS_LINUX :
				addLaunchArgumentToMap(VM_ARGS_LINUX, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS_MAC :
				addLaunchArgumentToMap(VM_ARGS_MAC, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS_SOLARIS :
				addLaunchArgumentToMap(VM_ARGS_SOLARIS, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS_WIN :
				addLaunchArgumentToMap(VM_ARGS_WIN, String.valueOf(ch, start, length));
				break;
			case STATE_CONFIG_INI :
				if (platformConfigPath != null)
					platformConfigPath += String.valueOf(ch, start, length);
				break;
		}
	}

	private void addLaunchArgumentToMap(String key, String value) {
		if (launcherArgs == null)
			launcherArgs = new Properties();

		String oldValue = launcherArgs.getProperty(key);
		if (oldValue != null)
			launcherArgs.setProperty(key, oldValue + value);
		else
			launcherArgs.setProperty(key, value);
	}

	private void processPlugin(Attributes attributes) {
		String fragment = attributes.getValue("fragment"); //$NON-NLS-1$
		if (fragment != null && new Boolean(fragment).booleanValue()) {
			if (fragments == null)
				fragments = new ArrayList();
			fragments.add(attributes.getValue("id")); //$NON-NLS-1$
		} else {
			if (plugins == null)
				plugins = new ArrayList();
			plugins.add(attributes.getValue("id")); //$NON-NLS-1$
		}
	}

	private void processFeature(Attributes attributes) {
		if (features == null)
			features = new ArrayList();
		features.add(attributes.getValue("id")); //$NON-NLS-1$
	}

	private void processProduct(Attributes attributes) {
		id = attributes.getValue("id"); //$NON-NLS-1$
		uid = attributes.getValue("uid"); //$NON-NLS-1$
		productName = attributes.getValue("name"); //$NON-NLS-1$
		String use = attributes.getValue("useFeatures"); //$NON-NLS-1$
		if (use != null)
			useFeatures = Boolean.valueOf(use).booleanValue();
		version = attributes.getValue("version"); //$NON-NLS-1$
	}

	private void processConfigIni(Attributes attributes) {
		String path = null;
		if ("custom".equals(attributes.getValue("use"))) { //$NON-NLS-1$//$NON-NLS-2$
			path = attributes.getValue("path"); //$NON-NLS-1$
		}
		String os = attributes.getValue("os"); //$NON-NLS-1$
		if (os != null && os.length() > 0) {
			// TODO should we allow a platform-specific default to over-ride a custom generic path?
			if (path != null)
				platformSpecificConfigPaths.put(os, path);
		} else if (path != null) {
			//			configPath = path;
		}
	}

	private void processConfigIniPlatform(String key, boolean begin) {
		if (begin) {
			configPlatform = key;
			platformConfigPath = ""; //$NON-NLS-1$
		} else if (configPlatform.equals(key) && platformConfigPath.length() > 0) {
			platformSpecificConfigPaths.put(key, platformConfigPath);
			platformConfigPath = null;
		}
	}

	private void processLauncher(Attributes attributes) {
		launcherName = attributes.getValue("name"); //$NON-NLS-1$
	}

	private boolean osMatch(String os) {
		if (os == currentOS)
			return true;
		if (os == null)
			return false;
		return os.equals(currentOS);
	}

	private void processSolaris(Attributes attributes) {
		if (!osMatch(OS_SOLARIS))
			return;
		result.add(attributes.getValue(SOLARIS_LARGE));
		result.add(attributes.getValue(SOLARIS_MEDIUM));
		result.add(attributes.getValue(SOLARIS_SMALL));
		result.add(attributes.getValue(SOLARIS_TINY));
	}

	private void processWin(Attributes attributes) {
		if (!osMatch(OS_WIN32))
			return;
		useIco = Boolean.valueOf(attributes.getValue(P_USE_ICO)).booleanValue();
	}

	private void processIco(Attributes attributes) {
		if (!osMatch(OS_WIN32) || !useIco)
			return;
		result.add(attributes.getValue("path")); //$NON-NLS-1$
	}

	private void processBmp(Attributes attributes) {
		if (!osMatch(OS_WIN32) || useIco)
			return;
		result.add(attributes.getValue(WIN32_16_HIGH));
		result.add(attributes.getValue(WIN32_16_LOW));
		result.add(attributes.getValue(WIN32_24_LOW));
		result.add(attributes.getValue(WIN32_32_HIGH));
		result.add(attributes.getValue(WIN32_32_LOW));
		result.add(attributes.getValue(WIN32_48_HIGH));
		result.add(attributes.getValue(WIN32_48_LOW));
	}

	private void processLinux(Attributes attributes) {
		if (!osMatch(OS_LINUX))
			return;
		result.add(attributes.getValue("icon")); //$NON-NLS-1$
	}

	private void processMac(Attributes attributes) {
		if (!osMatch(OS_MACOSX))
			return;
		result.add(attributes.getValue("icon")); //$NON-NLS-1$
	}
}
