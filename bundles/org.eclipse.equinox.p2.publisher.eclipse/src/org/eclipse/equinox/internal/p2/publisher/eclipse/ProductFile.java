/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - Additional function and fixes
 *     EclipseSource - ongoing development
 *     Felix Riegger (SAP AG) - consolidation of publishers for PDE formats (bug 331974)
 *     SAP AG - ongoing development
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.publisher.eclipse;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.publishing.Activator;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 *  Used to parse a .product file.
 */
public class ProductFile extends DefaultHandler implements IProductDescriptor {
	public final static String GENERIC_VERSION_NUMBER = "0.0.0"; //$NON-NLS-1$

	private static final String ATTRIBUTE_PATH = "path"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ICON = "icon"; //$NON-NLS-1$
	protected static final String ATTRIBUTE_FRAGMENT = "fragment"; //$NON-NLS-1$
	private static final String ATTRIBUTE_APPLICATION = "application"; //$NON-NLS-1$
	private static final String ATTRIBUTE_INCLUDE_LAUNCHERS = "includeLaunchers"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
	private static final String ATTRIBUTE_VALUE = "value"; //$NON-NLS-1$
	private static final String ATTRIBUTE_LOCATION = "location"; //$NON-NLS-1$
	private static final String ATTRIBUTE_AUTO_START = "autoStart"; //$NON-NLS-1$
	private static final String ATTRIBUTE_START_LEVEL = "startLevel"; //$NON-NLS-1$
	protected static final String ATTRIBUTE_VERSION = "version"; //$NON-NLS-1$
	protected static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	private static final String ATTRIBUTE_UID = "uid"; //$NON-NLS-1$
	private static final String ATTRIBUTE_CONTENT_TYPE = "type"; //$NON-NLS-1$

	private static final String PROPERTY_ECLIPSE_APPLICATION = "eclipse.application"; //$NON-NLS-1$
	private static final String PROPERTY_ECLIPSE_PRODUCT = "eclipse.product"; //$NON-NLS-1$

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
	private static final String WIN32_256_HIGH = "winExtraLargeHigh"; //$NON-NLS-1$

	private static final String OS_WIN32 = "win32";//$NON-NLS-1$
	private static final String OS_LINUX = "linux";//$NON-NLS-1$
	private static final String OS_SOLARIS = "solaris";//$NON-NLS-1$
	private static final String OS_MACOSX = "macosx";//$NON-NLS-1$

	//element names
	private static final String EL_FEATURES = "features"; //$NON-NLS-1$
	private static final String EL_FEATURE = "feature"; //$NON-NLS-1$
	private static final String EL_PLUGINS = "plugins"; //$NON-NLS-1$
	private static final String EL_PLUGIN = "plugin"; //$NON-NLS-1$
	private static final String EL_PRODUCT = "product"; //$NON-NLS-1$
	private static final String EL_PROPERTY = "property"; //$NON-NLS-1$
	private static final String EL_CONFIG_INI = "configIni"; //$NON-NLS-1$
	private static final String EL_LAUNCHER = "launcher"; //$NON-NLS-1$
	private static final String EL_LAUNCHER_ARGS = "launcherArgs"; //$NON-NLS-1$
	private static final String EL_SPLASH = "splash"; //$NON-NLS-1$
	private static final String EL_CONFIGURATIONS = "configurations"; //$NON-NLS-1$
	private static final String EL_LICENSE = "license"; //$NON-NLS-1$
	private static final String EL_URL = "url"; //$NON-NLS-1$
	private static final String EL_TEXT = "text"; //$NON-NLS-1$

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
	private static final int STATE_CONFIGURATIONS = 17;
	private static final int STATE_LICENSE = 18;
	private static final int STATE_LICENSE_URL = 19;
	private static final int STATE_LICENSE_TEXT = 20;

	private static final String PI_PDEBUILD = "org.eclipse.pde.build"; //$NON-NLS-1$
	private final static int EXCEPTION_PRODUCT_FORMAT = 23;
	private final static int EXCEPTION_PRODUCT_FILE = 24;

	private int state = STATE_START;

	private SAXParser parser;
	private String launcherName = null;
	//	private boolean useIco = false;
	private final Map<String, Collection<String>> icons = new HashMap<String, Collection<String>>(6);
	private String configPath = null;
	private final Map<String, String> platformSpecificConfigPaths = new HashMap<String, String>();
	private String configPlatform = null;
	private String platformConfigPath = null;
	private String id = null;
	private String uid = null;
	private ProductContentType productContentType = null;
	protected List<FeatureEntry> plugins = new ArrayList<FeatureEntry>();
	protected List<FeatureEntry> fragments = new ArrayList<FeatureEntry>();
	private final List<FeatureEntry> features = new ArrayList<FeatureEntry>();
	private String splashLocation = null;
	private String productName = null;
	private String application = null;
	private String version = null;
	private Properties launcherArgs = new Properties();
	private final File location;
	private List<BundleInfo> bundleInfos;
	private Map<String, String> properties;
	private boolean includeLaunchers = true;
	private String licenseURL;
	private String licenseText = null;
	private String currentOS;

	private static String normalize(String text) {
		if (text == null || text.trim().length() == 0)
			return ""; //$NON-NLS-1$

		StringBuffer result = new StringBuffer(text.length());
		boolean haveSpace = false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.isWhitespace(c)) {
				if (haveSpace)
					continue;
				haveSpace = true;
				result.append(" "); //$NON-NLS-1$
			} else {
				haveSpace = false;
				result.append(c);
			}
		}
		return result.toString();
	}

	public ProductFile(String location, String os) throws CoreException {
		super();
		this.currentOS = os;
		this.location = new File(location);
		try {
			parserFactory.setNamespaceAware(true);
			parser = parserFactory.newSAXParser();
			InputStream in = new BufferedInputStream(new FileInputStream(location));
			try {
				parser.parse(new InputSource(in), this);
			} finally {
				try {
					in.close();
				} catch (IOException e) {
					// ignore exception on close (as it was done by Utils.close() before)
				}
			}
		} catch (ParserConfigurationException e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PRODUCT_FORMAT, NLS.bind(Messages.exception_productParse, location), e));
		} catch (SAXException e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PRODUCT_FORMAT, NLS.bind(Messages.exception_productParse, location), e));
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PRODUCT_FILE, NLS.bind(Messages.exception_missingElement, location), null));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PRODUCT_FORMAT, NLS.bind(Messages.exception_productParse, location), e));
		}
	}

	/**
	 * Constructs a product file parser.
	 */
	public ProductFile(String location) throws Exception {
		super();
		this.location = new File(location);

		parserFactory.setNamespaceAware(true);
		parser = parserFactory.newSAXParser();
		InputStream in = new BufferedInputStream(new FileInputStream(location));
		try {
			parser.parse(new InputSource(in), this);
		} finally {
			in.close();
		}
		parser = null;
	}

	/**
	 * Gets the name of the launcher specified in the .product file.
	 */
	public String getLauncherName() {
		return launcherName;
	}

	/**
	 * Gets the location of the .product file.
	 */
	public File getLocation() {
		return location;
	}

	/**
	 * Returns the properties found in .product file.  Properties
	 * are located in the <configurations> block of the file
	 */
	public Map<String, String> getConfigurationProperties() {
		Map<String, String> result = properties != null ? properties : new HashMap<String, String>();
		if (application != null && !result.containsKey(PROPERTY_ECLIPSE_APPLICATION))
			result.put(PROPERTY_ECLIPSE_APPLICATION, application);
		if (id != null && !result.containsKey(PROPERTY_ECLIPSE_PRODUCT))
			result.put(PROPERTY_ECLIPSE_PRODUCT, id);

		return result;
	}

	/**
	 * Returns a List<VersionedName> for each bundle that makes up this product.
	 * @param includeFragments Indicates whether or not fragments should
	 * be included in the list
	 */
	public List<IVersionedId> getBundles(boolean includeFragments) {
		List<IVersionedId> result = new LinkedList<IVersionedId>();

		for (FeatureEntry plugin : plugins) {
			result.add(new VersionedId(plugin.getId(), plugin.getVersion()));
		}

		if (includeFragments) {
			for (FeatureEntry fragment : fragments) {
				result.add(new VersionedId(fragment.getId(), fragment.getVersion()));
			}
		}

		return result;
	}

	private List<FeatureEntry> getBundleEntries(boolean includeFragments) {
		List<FeatureEntry> result = new LinkedList<FeatureEntry>();
		result.addAll(plugins);
		if (includeFragments)
			result.addAll(fragments);
		return result;
	}

	/**
	 * Returns a List<BundleInfo> for each bundle that has custom configuration data
	 * in the product file.
	 * @return A List<BundleInfo>
	 */
	public List<BundleInfo> getBundleInfos() {
		return bundleInfos != null ? bundleInfos : CollectionUtils.<BundleInfo> emptyList();
	}

	/**
	 * Returns a list<VersionedName> of fragments that constitute this product.
	 */
	public List<IVersionedId> getFragments() {
		List<IVersionedId> result = new LinkedList<IVersionedId>();

		for (FeatureEntry fragment : fragments) {
			result.add(new VersionedId(fragment.getId(), fragment.getVersion()));
		}

		return result;
	}

	/**
	 * Returns a List<VersionedName> of features that constitute this product.
	 */
	public List<IVersionedId> getFeatures() {
		List<IVersionedId> result = new LinkedList<IVersionedId>();

		for (FeatureEntry feature : features) {
			result.add(new VersionedId(feature.getId(), feature.getVersion()));
		}

		return result;
	}

	public List<FeatureEntry> getProductEntries() {
		if (useFeatures()) {
			return features;
		}
		return getBundleEntries(true);
	}

	public boolean containsPlugin(String plugin) {
		return getBundles(true).contains(plugin);
	}

	public String[] getIcons() {
		return getIcons(currentOS);
	}

	public String[] getIcons(String os) {
		Collection<String> result = icons.get(os);
		if (result == null)
			return new String[0];
		return result.toArray(new String[result.size()]);
	}

	public String getConfigIniPath(String os) {
		String specific = platformSpecificConfigPaths.get(os);
		return specific == null ? configPath : specific;
	}

	public String getConfigIniPath() {
		return configPath;
	}

	public boolean haveCustomConfig() {
		return configPath != null || platformSpecificConfigPaths.size() > 0;
	}

	/**
	 * Returns the ID for this product.
	 */
	public String getId() {
		if (uid != null)
			return uid;
		return id;
	}

	public String getProductId() {
		return id;
	}

	/**
	 * Returns the location (the bundle) that defines the splash screen
	 */
	public String getSplashLocation() {
		return splashLocation;
	}

	/**
	 * Returns the product name.
	 */
	public String getProductName() {
		return productName;
	}

	/**
	 * Returns the application identifier for this product.
	 */
	public String getApplication() {
		return application;
	}

	/**
	 * Returns true if this product is built using feature, 
	 * false otherwise.
	 */
	public boolean useFeatures() {
		return productContentType == ProductContentType.FEATURES;
	}

	/**
	 * Returns the version of the product
	 */
	public String getVersion() {
		return (version == null || version.length() == 0) ? "0.0.0" : version; //$NON-NLS-1$
	}

	public boolean includeLaunchers() {
		return includeLaunchers;
	}

	public Map<String, BundleInfo> getConfigurationInfo() {
		Map<String, BundleInfo> result = new HashMap<String, BundleInfo>();
		for (BundleInfo info : getBundleInfos()) {
			result.put(info.getSymbolicName(), info);
		}
		return result;
	}

	public Properties getConfigProperties() {
		Properties props = new Properties();
		for (Entry<String, String> property : getConfigurationProperties().entrySet()) {
			props.setProperty(property.getKey(), property.getValue());
		}
		return props;
	}

	/**
	 * Returns the VM arguments for a specific platform.
	 * If the empty string is used for the OS, this returns
	 * the default VM arguments
	 */
	public String getVMArguments(String os) {
		os = os == null ? "" : os; //$NON-NLS-1$
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

	/**
	 * Returns the program arguments for a specific platform.
	 * If the empty string is used for the OS, this returns
	 * the default program arguments
	 */
	public String getProgramArguments(String os) {
		os = os == null ? "" : os; //$NON-NLS-1$
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

	public String getLicenseText() {
		return licenseText;
	}

	public String getLicenseURL() {
		return licenseURL;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		switch (state) {
			case STATE_START :
				if (EL_PRODUCT.equals(localName)) {
					processProduct(attributes);
					state = STATE_PRODUCT;
				}
				break;

			case STATE_PRODUCT :
				if (EL_CONFIG_INI.equals(localName)) {
					processConfigIni(attributes);
					state = STATE_CONFIG_INI;
				} else if (EL_LAUNCHER.equals(localName)) {
					processLauncher(attributes);
					state = STATE_LAUNCHER;
				} else if (EL_PLUGINS.equals(localName)) {
					state = STATE_PLUGINS;
				} else if (EL_FEATURES.equals(localName)) {
					state = STATE_FEATURES;
				} else if (EL_LAUNCHER_ARGS.equals(localName)) {
					state = STATE_LAUNCHER_ARGS;
				} else if (EL_SPLASH.equals(localName)) {
					splashLocation = attributes.getValue(ATTRIBUTE_LOCATION);
				} else if (EL_CONFIGURATIONS.equals(localName)) {
					state = STATE_CONFIGURATIONS;
				} else if (EL_LICENSE.equals(localName)) {
					state = STATE_LICENSE;
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
				if (EL_PLUGIN.equals(localName)) {
					processPlugin(attributes);
				}
				break;

			case STATE_LICENSE :
				if (EL_URL.equals(localName)) {
					state = STATE_LICENSE_URL;
				} else if (EL_TEXT.equals(localName)) {
					licenseText = ""; //$NON-NLS-1$
					state = STATE_LICENSE_TEXT;
				}
				break;

			case STATE_FEATURES :
				if (EL_FEATURE.equals(localName)) {
					processFeature(attributes);
				}
				break;
			case STATE_CONFIGURATIONS :
				if (EL_PLUGIN.equals(localName)) {
					processPluginConfiguration(attributes);
				} else if (EL_PROPERTY.equals(localName)) {
					processPropertyConfiguration(attributes);
				}
				break;
		}
	}

	/**
	 * Processes the property tag in the .product file.  These tags contain
	 * a Name and Value pair.  For each tag (with a non-null name), a property 
	 * is created.
	 */
	private void processPropertyConfiguration(Attributes attributes) {
		String name = attributes.getValue(ATTRIBUTE_NAME);
		String value = attributes.getValue(ATTRIBUTE_VALUE);
		if (name == null)
			return;
		if (value == null)
			value = ""; //$NON-NLS-1$
		if (properties == null)
			properties = new HashMap<String, String>();
		properties.put(name, value);
	}

	private void processPluginConfiguration(Attributes attributes) {
		BundleInfo info = new BundleInfo();
		info.setSymbolicName(attributes.getValue(ATTRIBUTE_ID));
		info.setVersion(attributes.getValue(ATTRIBUTE_VERSION));
		String value = attributes.getValue(ATTRIBUTE_START_LEVEL);
		if (value != null) {
			int startLevel = Integer.parseInt(value);
			if (startLevel > 0)
				info.setStartLevel(startLevel);
		}
		value = attributes.getValue(ATTRIBUTE_AUTO_START);
		if (value != null)
			info.setMarkedAsStarted(Boolean.valueOf(value).booleanValue());
		if (bundleInfos == null)
			bundleInfos = new ArrayList<BundleInfo>();
		bundleInfos.add(info);
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		switch (state) {
			case STATE_PLUGINS :
				if (EL_PLUGINS.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_FEATURES :
				if (EL_FEATURES.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_LAUNCHER_ARGS :
				if (EL_LAUNCHER_ARGS.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_LAUNCHER :
				if (EL_LAUNCHER.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_CONFIGURATIONS :
				if (EL_CONFIGURATIONS.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_LICENSE :
				if (EL_LICENSE.equals(localName))
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
			case STATE_LICENSE_URL :
			case STATE_LICENSE_TEXT :
				state = STATE_LICENSE;
				break;

			case STATE_CONFIG_INI :
				if (EL_CONFIG_INI.equals(localName))
					state = STATE_PRODUCT;
				else
					processConfigIniPlatform(localName, false);
				break;
		}
	}

	@Override
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
			case STATE_LICENSE_URL :
				licenseURL = String.valueOf(ch, start, length);
				break;
			case STATE_LICENSE_TEXT :
				if (licenseText != null)
					licenseText += String.valueOf(ch, start, length);
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

	protected void processPlugin(Attributes attributes) {
		String fragment = attributes.getValue(ATTRIBUTE_FRAGMENT);
		String pluginId = attributes.getValue(ATTRIBUTE_ID);
		String pluginVersion = attributes.getValue(ATTRIBUTE_VERSION);

		FeatureEntry entry = new FeatureEntry(pluginId, pluginVersion != null ? pluginVersion : GENERIC_VERSION_NUMBER, true);
		entry.setFragment(Boolean.valueOf(fragment).booleanValue());

		if (fragment != null && new Boolean(fragment).booleanValue()) {
			fragments.add(entry);
		} else {
			plugins.add(entry);
		}
	}

	private void processFeature(Attributes attributes) {
		String featureId = attributes.getValue(ATTRIBUTE_ID);
		String featureVersion = attributes.getValue(ATTRIBUTE_VERSION);
		FeatureEntry featureEntry = new FeatureEntry(featureId, featureVersion != null ? featureVersion : GENERIC_VERSION_NUMBER, false);

		features.add(featureEntry);
	}

	private void processProduct(Attributes attributes) {
		id = attributes.getValue(ATTRIBUTE_ID);
		uid = attributes.getValue(ATTRIBUTE_UID);
		productName = attributes.getValue(ATTRIBUTE_NAME);
		application = attributes.getValue(ATTRIBUTE_APPLICATION);
		if (attributes.getIndex(ATTRIBUTE_INCLUDE_LAUNCHERS) >= 0)
			includeLaunchers = Boolean.valueOf(attributes.getValue(ATTRIBUTE_INCLUDE_LAUNCHERS));
		String contentTypeString = attributes.getValue(ATTRIBUTE_CONTENT_TYPE);
		if (contentTypeString != null)
			productContentType = ProductContentType.toProductContentType(contentTypeString);
		if (productContentType == null) { // useFeatures attribute is taken into account only if the contentType attribute is missing
			String use = attributes.getValue("useFeatures"); //$NON-NLS-1$
			// for backward compatibility with the old behavior 
			if (use != null && Boolean.valueOf(use).booleanValue())
				productContentType = ProductContentType.FEATURES;
			else
				productContentType = ProductContentType.BUNDLES;
		}

		version = attributes.getValue(ATTRIBUTE_VERSION);
	}

	private void processConfigIni(Attributes attributes) {
		String path = null;
		if ("custom".equals(attributes.getValue("use"))) { //$NON-NLS-1$//$NON-NLS-2$
			path = attributes.getValue(ATTRIBUTE_PATH);
		}
		String os = attributes.getValue("os"); //$NON-NLS-1$
		if (os != null && os.length() > 0) {
			// TODO should we allow a platform-specific default to over-ride a custom generic path?
			if (path != null)
				platformSpecificConfigPaths.put(os, path);
		} else if (path != null) {
			configPath = path;
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
		launcherName = attributes.getValue(ATTRIBUTE_NAME);
	}

	private void addIcon(String os, String value) {
		if (value == null)
			return;

		File iconFile = new File(value);
		if (!iconFile.isFile()) {
			//workspace
			Location instanceLocation = (Location) ServiceHelper.getService(Activator.getContext(), Location.class.getName(), Location.INSTANCE_FILTER);
			if (instanceLocation != null && instanceLocation.getURL() != null) {
				File workspace = URLUtil.toFile(instanceLocation.getURL());
				if (workspace != null)
					iconFile = new File(workspace, value);
			}
		}
		if (!iconFile.isFile())
			iconFile = new File(location.getParentFile(), value);

		Collection<String> list = icons.get(os);
		if (list == null) {
			list = new ArrayList<String>(6);
			icons.put(os, list);
		}
		list.add(iconFile.getAbsolutePath());
	}

	private void processSolaris(Attributes attributes) {
		addIcon(OS_SOLARIS, attributes.getValue(SOLARIS_LARGE));
		addIcon(OS_SOLARIS, attributes.getValue(SOLARIS_MEDIUM));
		addIcon(OS_SOLARIS, attributes.getValue(SOLARIS_SMALL));
		addIcon(OS_SOLARIS, attributes.getValue(SOLARIS_TINY));
	}

	private void processWin(Attributes attributes) {
		//		useIco = Boolean.valueOf(attributes.getValue(P_USE_ICO)).booleanValue();
	}

	private void processIco(Attributes attributes) {
		addIcon(OS_WIN32, attributes.getValue(ATTRIBUTE_PATH));
	}

	private void processBmp(Attributes attributes) {
		addIcon(OS_WIN32, attributes.getValue(WIN32_16_HIGH));
		addIcon(OS_WIN32, attributes.getValue(WIN32_16_LOW));
		addIcon(OS_WIN32, attributes.getValue(WIN32_24_LOW));
		addIcon(OS_WIN32, attributes.getValue(WIN32_32_HIGH));
		addIcon(OS_WIN32, attributes.getValue(WIN32_32_LOW));
		addIcon(OS_WIN32, attributes.getValue(WIN32_48_HIGH));
		addIcon(OS_WIN32, attributes.getValue(WIN32_48_LOW));
		addIcon(OS_WIN32, attributes.getValue(WIN32_256_HIGH));
	}

	private void processLinux(Attributes attributes) {
		addIcon(OS_LINUX, attributes.getValue(ATTRIBUTE_ICON));
	}

	private void processMac(Attributes attributes) {
		addIcon(OS_MACOSX, attributes.getValue(ATTRIBUTE_ICON));
	}

	public ProductContentType getProductContentType() {
		return productContentType;
	}
}
