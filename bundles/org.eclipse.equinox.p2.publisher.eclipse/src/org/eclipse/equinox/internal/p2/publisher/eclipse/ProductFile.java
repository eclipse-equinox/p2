/*******************************************************************************
 * Copyright (c) 2005, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - Additional function and fixes
 *     EclipseSource - ongoing development
 *     Felix Riegger (SAP AG) - consolidation of publishers for PDE formats (bug 331974)
 *     SAP AG - ongoing development
 *     Rapicorp - additional features
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.publisher.eclipse;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;
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
	private static final String ATTRIBUTE_OS = "os"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ARCH = "arch"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ENABLED = "enabled"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FEATURE_INSTALL_MODE = "installMode"; //$NON-NLS-1$

	private static final String PROPERTY_ECLIPSE_APPLICATION = "eclipse.application"; //$NON-NLS-1$
	private static final String PROPERTY_ECLIPSE_PRODUCT = "eclipse.product"; //$NON-NLS-1$

	private static final String PROGRAM_ARGS = "programArgs"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_LINUX = "programArgsLin"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_MAC = "programArgsMac"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_SOLARIS = "programArgsSol"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_WIN = "programArgsWin"; //$NON-NLS-1$
	private static final String VM = "vm"; //$NON-NLS-1$
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
	private static final String OS_MACOS = "macos";//$NON-NLS-1$
	private static final String OS_WINDOWS = "windows";//$NON-NLS-1$

	// These must match Platform constant values
	private static final String ARCH_X86 = "x86"; //$NON-NLS-1$
	private static final String ARCH_X86_64 = "x86_64"; //$NON-NLS-1$
	private static final String ARCH_PPC = "ppc"; //$NON-NLS-1$
	private static final String ARCH_IA_64 = "ia64"; //$NON-NLS-1$
	private static final String ARCH_IA_64_32 = "ia64_32"; //$NON-NLS-1$
	private static final String ARCH_PA_RISC = "PA_RISC"; //$NON-NLS-1$
	private static final String ARCH_SPARC = "sparc"; //$NON-NLS-1$

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
	private static final String EL_ARCH_X86 = "argsX86"; //$NON-NLS-1$
	private static final String EL_ARCH_X86_64 = "argsX86_64"; //$NON-NLS-1$
	private static final String EL_ARCH_PPC = "argsPPC"; //$NON-NLS-1$
	private static final String EL_ARCH_IA_64 = "argsIA_64"; //$NON-NLS-1$
	private static final String EL_ARCH_IA_64_32 = "argsIA_64_32"; //$NON-NLS-1$
	private static final String EL_ARCH_PA_RISC = "argsPA_RISC"; //$NON-NLS-1$
	private static final String EL_ARCH_SPARC = "argsSPARC"; //$NON-NLS-1$
	private static final String EL_REPOSITORIES = "repositories"; //$NON-NLS-1$
	private static final String EL_REPOSITORY = "repository"; //$NON-NLS-1$

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
	private static final int STATE_ARCH_X86 = 21;
	private static final int STATE_ARCH_X86_64 = 22;
	private static final int STATE_ARCH_PPC = 23;
	private static final int STATE_ARCH_IA_64 = 24;
	private static final int STATE_ARCH_IA_64_32 = 25;
	private static final int STATE_ARCH_PA_RISC = 26;
	private static final int STATE_ARCH_SPARC = 27;
	private static final int STATE_REPOSITORIES = 28;
	private static final int STATE_VM = 29;
	private static final int STATE_VM_LINUX = 31;
	private static final int STATE_VM_MACOS = 32;
	private static final int STATE_VM_WINDOWS = 33;

	private static final String PI_PDEBUILD = "org.eclipse.pde.build"; //$NON-NLS-1$
	private final static int EXCEPTION_PRODUCT_FORMAT = 23;
	private final static int EXCEPTION_PRODUCT_FILE = 24;

	private int state = STATE_START;
	private int outerState = STATE_START;
	private String platformKeyPrefix = null;

	private SAXParser parser;
	private String launcherName = null;
	//	private boolean useIco = false;
	private final Map<String, Collection<String>> icons = new HashMap<>(6);
	private String configPath = null;
	private final Map<String, String> platformSpecificConfigPaths = new HashMap<>();
	private String configPlatform = null;
	private String platformConfigPath = null;
	private String id = null;
	private String uid = null;
	private ProductContentType productContentType = null;
	protected List<FeatureEntry> plugins = new ArrayList<>();
	private final List<FeatureEntry> features = new ArrayList<>();
	private final List<FeatureEntry> rootFeatures = new ArrayList<>();
	private String splashLocation = null;
	private String productName = null;
	private String application = null;
	private String version = null;
	private Properties launcherArgs = new Properties();
	private final File location;
	private List<BundleInfo> bundleInfos;
	private Map<String, String> properties;
	private HashMap<String, HashMap<String, String>> filteredProperties;
	private boolean includeLaunchers = true;
	private String licenseURL;
	private String licenseText = null;
	private final String currentOS;
	private final List<IRepositoryReference> repositories = new ArrayList<>();
	private final Map<String, String> vms = new HashMap<>();

	private static String normalize(String text) {
		if (text == null || text.trim().length() == 0)
			return ""; //$NON-NLS-1$

		StringBuilder result = new StringBuilder(text.length());
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
		this.currentOS = os;
		this.location = new File(location);
		try {
			SAXParserFactory parserFactory = SecureXMLUtil.newSecureSAXParserFactory();
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
		this(location, null);
	}

	/**
	 * Gets the name of the launcher specified in the .product file.
	 */
	@Override
	public String getLauncherName() {
		return launcherName;
	}

	/**
	 * Gets the location of the .product file.
	 */
	@Override
	public File getLocation() {
		return location;
	}

	/**
	 * Returns the properties found in .product file.  Properties
	 * are located in the <configurations> block of the file
	 */
	@Override
	public Map<String, String> getConfigurationProperties() {
		return getConfigurationProperties(null, null);
	}

	/**
	 * Returns the properties found in .product file that are valid
	 * for the specified platform os and architecture.  If there is no
	 * platform os and/or architecture specified, return only the properties
	 * that are not filtered by the unspecified os and/or arch. 
	 * Properties are located in the <configurations> block of the file
	 */
	@Override
	public Map<String, String> getConfigurationProperties(String os, String arch) {
		// add all generic properties
		Map<String, String> result = properties != null ? properties : new HashMap<>();
		// add any properties filtered on os and/or arch
		if (filteredProperties != null) {
			String[] filteredKeys = new String[3]; // ".arch", "os.", "os.arch"
			if (os == null) {
				// only arch is specified. Provide properties defined for
				// all os and a specific architecture
				if (arch != null && arch.length() > 0) {
					filteredKeys[0] = "." + arch; //$NON-NLS-1$
				}
			} else {
				if (arch == null) {
					// only os is specified. Provide properties defined for 
					// specific os and all architectures.
					filteredKeys[1] = os + "."; //$NON-NLS-1$
				} else {
					// os and arch specified. Provide properties defined for
					// the os, for the arch, and for both
					filteredKeys[0] = "." + arch; //$NON-NLS-1$
					filteredKeys[1] = os + "."; //$NON-NLS-1$
					filteredKeys[2] = os + "." + arch; //$NON-NLS-1$
				}
			}
			for (String filteredKey : filteredKeys) {
				if (filteredKey != null) {
					// copy all mappings that are filtered for this os and/or arch
					HashMap<String, String> innerMap = filteredProperties.get(filteredKey);
					if (innerMap != null) {
						result.putAll(innerMap);
					}
				}
			}
		}
		if (application != null && !result.containsKey(PROPERTY_ECLIPSE_APPLICATION))
			result.put(PROPERTY_ECLIPSE_APPLICATION, application);
		if (id != null && !result.containsKey(PROPERTY_ECLIPSE_PRODUCT))
			result.put(PROPERTY_ECLIPSE_PRODUCT, id);

		return result;
	}

	/**
	 * Returns a List<VersionedName> for each bundle that makes up this product.
	 */
	@Override
	public List<IVersionedId> getBundles() {
		List<IVersionedId> result = new ArrayList<>();
		for (FeatureEntry plugin : plugins) {
			result.add(new VersionedId(plugin.getId(), plugin.getVersion()));
		}
		return result;
	}

	@Override
	public boolean hasBundles() {
		// implement directly; don't call the potentially overridden getBundles
		return !plugins.isEmpty();
	}

	/**
	 * Returns a List<BundleInfo> for each bundle that has custom configuration data
	 * in the product file.
	 * @return A List<BundleInfo>
	 */
	@Override
	public List<BundleInfo> getBundleInfos() {
		return bundleInfos != null ? bundleInfos : Collections.emptyList();
	}

	/**
	 * Returns a List<VersionedName> of features that constitute this product.
	 */
	@Override
	public List<IVersionedId> getFeatures() {
		return getFeatures(INCLUDED_FEATURES);
	}

	@Override
	public boolean hasFeatures() {
		// implement directly; don't call the potentially overridden getFeatures
		return !features.isEmpty();
	}

	@Override
	public List<IVersionedId> getFeatures(int options) {
		List<IVersionedId> result = new ArrayList<>();

		if ((options & INCLUDED_FEATURES) != 0) {
			for (FeatureEntry feature : features) {
				result.add(new VersionedId(feature.getId(), feature.getVersion()));
			}
		}
		if ((options & ROOT_FEATURES) != 0) {
			for (FeatureEntry feature : rootFeatures) {
				result.add(new VersionedId(feature.getId(), feature.getVersion()));
			}
		}

		return result;
	}

	public List<FeatureEntry> getProductEntries() {
		if (useFeatures()) {
			return Collections.unmodifiableList(features);
		}
		return Collections.unmodifiableList(plugins);
	}

	public boolean containsPlugin(String plugin) {
		List<IVersionedId> bundles = getBundles();
		for (IVersionedId versionedId : bundles) {
			if (versionedId.getId().equals(plugin)) {
				return true;
			}
		}
		return false;
	}

	public String[] getIcons() {
		return getIcons(currentOS);
	}

	@Override
	public String[] getIcons(String os) {
		Collection<String> result = icons.get(os);
		if (result == null)
			return new String[0];
		return result.toArray(new String[result.size()]);
	}

	@Override
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
	@Override
	public String getId() {
		if (uid != null)
			return uid;
		return id;
	}

	@Override
	public String getProductId() {
		return id;
	}

	/**
	 * Returns the location (the bundle) that defines the splash screen
	 */
	@Override
	public String getSplashLocation() {
		return splashLocation;
	}

	/**
	 * Returns the product name.
	 */
	@Override
	public String getProductName() {
		return productName;
	}

	/**
	 * Returns the application identifier for this product.
	 */
	@Override
	public String getApplication() {
		return application;
	}

	/**
	 * Returns true if this product is built using feature, 
	 * false otherwise.
	 */
	@Override
	public boolean useFeatures() {
		return productContentType == ProductContentType.FEATURES;
	}

	/**
	 * Returns the version of the product
	 */
	@Override
	public String getVersion() {
		return (version == null || version.length() == 0) ? "0.0.0" : version; //$NON-NLS-1$
	}

	@Override
	public boolean includeLaunchers() {
		return includeLaunchers;
	}

	public Map<String, BundleInfo> getConfigurationInfo() {
		Map<String, BundleInfo> result = new HashMap<>();
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
	@Override
	public String getVMArguments(String os) {
		return getVMArguments(os, null);
	}

	/**
	 * Returns the VM arguments for a specific platform and architecture
	 * combination. If the empty string is used for the architecture, this
	 * returns the default arguments for the platform.  If the empty string is
	 * used for the OS, this returns the default VM arguments.
	 */
	@Override
	public String getVMArguments(String os, String arch) {
		os = os == null ? "" : os; //$NON-NLS-1$
		String key = null;
		switch (os) {
			case OS_WIN32:
				key = VM_ARGS_WIN;
				break;
			case OS_LINUX:
				key = VM_ARGS_LINUX;
				break;
			case OS_MACOSX:
				key = VM_ARGS_MAC;
				break;
			case OS_SOLARIS:
				key = VM_ARGS_SOLARIS;
				break;
			default:
				break;
		}

		arch = arch == null ? "" : arch; //$NON-NLS-1$
		String archKey = null;
		switch (arch) {
			case ARCH_X86:
				archKey = EL_ARCH_X86;
				break;
			case ARCH_X86_64:
				archKey = EL_ARCH_X86_64;
				break;
			case ARCH_PPC:
				archKey = EL_ARCH_PPC;
				break;
			case ARCH_IA_64:
				archKey = EL_ARCH_IA_64;
				break;
			case ARCH_IA_64_32:
				archKey = EL_ARCH_IA_64_32;
				break;
			case ARCH_PA_RISC:
				archKey = EL_ARCH_PA_RISC;
				break;
			case ARCH_SPARC:
				archKey = EL_ARCH_SPARC;
				break;
			default:
				break;
		}

		String platformArchKey = null;
		String defaults = launcherArgs.getProperty(VM_ARGS);
		// architecture arguments independent of platform should be part
		// of the defaults.
		if (archKey != null) {
			String archOnAllPlatforms = launcherArgs.getProperty(VM_ARGS + "." + archKey); //$NON-NLS-1$
			if (archOnAllPlatforms != null && archOnAllPlatforms.length() > 0) {
				defaults = defaults + " " + archOnAllPlatforms; //$NON-NLS-1$
			}
		}
		String platform = null, platformAndArch = null, args = null;
		if (key != null) {
			// a platform with no arch specified
			platform = launcherArgs.getProperty(key);
			// platform + arch
			if (archKey != null) {
				platformArchKey = key + "." + archKey; //$NON-NLS-1$
				platformAndArch = launcherArgs.getProperty(platformArchKey);
			}
		}
		if (defaults != null) {
			if (platform != null)
				args = platformAndArch != null ? defaults + " " + platform + " " + platformAndArch : defaults + " " + platform; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			else
				args = defaults;
		} else {
			if (platform != null)
				args = platformAndArch != null ? platform + " " + platformAndArch : platform; //$NON-NLS-1$
			else
				args = platformAndArch != null ? platformAndArch : ""; //$NON-NLS-1$
		}
		return normalize(args);
	}

	/**
	 * Returns the program arguments for a specific platform.
	 * If the empty string is used for the OS, this returns
	 * the default program arguments
	 */
	@Override
	public String getProgramArguments(String os) {
		return getProgramArguments(os, null);
	}

	/**
	 * Returns the program arguments for a specific platform.
	 * If the empty string is used for the OS, this returns
	 * the default program arguments
	 */
	@Override
	public String getProgramArguments(String os, String arch) {
		os = os == null ? "" : os; //$NON-NLS-1$
		String key = null;
		switch (os) {
			case OS_WIN32:
				key = PROGRAM_ARGS_WIN;
				break;
			case OS_LINUX:
				key = PROGRAM_ARGS_LINUX;
				break;
			case OS_MACOSX:
				key = PROGRAM_ARGS_MAC;
				break;
			case OS_SOLARIS:
				key = PROGRAM_ARGS_SOLARIS;
				break;
			default:
				break;
		}

		arch = arch == null ? "" : arch; //$NON-NLS-1$
		String archKey = null;
		switch (arch) {
			case ARCH_X86:
				archKey = EL_ARCH_X86;
				break;
			case ARCH_X86_64:
				archKey = EL_ARCH_X86_64;
				break;
			case ARCH_PPC:
				archKey = EL_ARCH_PPC;
				break;
			case ARCH_IA_64:
				archKey = EL_ARCH_IA_64;
				break;
			case ARCH_IA_64_32:
				archKey = EL_ARCH_IA_64_32;
				break;
			case ARCH_PA_RISC:
				archKey = EL_ARCH_PA_RISC;
				break;
			case ARCH_SPARC:
				archKey = EL_ARCH_SPARC;
				break;
			default:
				break;
		}

		String platformArchKey = null;
		String defaults = launcherArgs.getProperty(PROGRAM_ARGS);
		// architecture arguments independent of platform should be part
		// of the defaults.
		if (archKey != null) {
			String archOnAllPlatforms = launcherArgs.getProperty(PROGRAM_ARGS + "." + archKey); //$NON-NLS-1$
			if (archOnAllPlatforms != null && archOnAllPlatforms.length() > 0) {
				defaults = defaults + " " + archOnAllPlatforms; //$NON-NLS-1$
			}
		}
		String platform = null, platformAndArch = null, args = null;
		if (key != null) {
			// a platform with no arch specified
			platform = launcherArgs.getProperty(key);
			// platform + arch
			if (archKey != null) {
				platformArchKey = key + "." + archKey; //$NON-NLS-1$
				platformAndArch = launcherArgs.getProperty(platformArchKey);
			}
		}
		if (defaults != null) {
			if (platform != null)
				args = platformAndArch != null ? defaults + " " + platform + " " + platformAndArch : defaults + " " + platform; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			else
				args = defaults;
		} else {
			if (platform != null)
				args = platformAndArch != null ? platform + " " + platformAndArch : platform; //$NON-NLS-1$
			else
				args = platformAndArch != null ? platformAndArch : ""; //$NON-NLS-1$
		}
		return normalize(args);
	}

	@Override
	public String getLicenseText() {
		return licenseText;
	}

	@Override
	public String getLicenseURL() {
		return licenseURL;
	}

	@Override
	public List<IRepositoryReference> getRepositoryEntries() {
		return repositories;
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
				if (null != localName) switch (localName) {
					case EL_CONFIG_INI:
						processConfigIni(attributes);
						state = STATE_CONFIG_INI;
						break;
					case EL_LAUNCHER:
						processLauncher(attributes);
						state = STATE_LAUNCHER;
						break;
					case EL_PLUGINS:
						state = STATE_PLUGINS;
						break;
					case EL_FEATURES:
						state = STATE_FEATURES;
						break;
					case EL_LAUNCHER_ARGS:
						state = STATE_LAUNCHER_ARGS;
						break;
					case EL_SPLASH:
						splashLocation = attributes.getValue(ATTRIBUTE_LOCATION);
						break;
					case EL_CONFIGURATIONS:
						state = STATE_CONFIGURATIONS;
						break;
					case EL_LICENSE:
						state = STATE_LICENSE;
						break;
					case EL_REPOSITORIES:
						state = STATE_REPOSITORIES;
						break;
					case VM:
						state = STATE_VM;
						break;
					default:
						break;
				}
				break;


			case STATE_CONFIG_INI :
				processConfigIniPlatform(localName, true);
				break;

			case STATE_LAUNCHER :
				if (null != localName) switch (localName) {
					case OS_SOLARIS:
						processSolaris(attributes);
						break;
					case "win": //$NON-NLS-1$
						processWin(attributes);
						break;
					case OS_LINUX:
						processLinux(attributes);
						break;
					case OS_MACOSX:
						processMac(attributes);
						break;
					default:
						break;
				}
				if ("ico".equals(localName)) { //$NON-NLS-1$
					processIco(attributes);
				} else if ("bmp".equals(localName)) { //$NON-NLS-1$
					processBmp(attributes);
				}
				break;


			case STATE_LAUNCHER_ARGS :
				if (null != localName) switch (localName) {
					case PROGRAM_ARGS:
						state = STATE_PROGRAM_ARGS;
						break;
					case PROGRAM_ARGS_LINUX:
						state = STATE_PROGRAM_ARGS_LINUX;
						break;
					case PROGRAM_ARGS_MAC:
						state = STATE_PROGRAM_ARGS_MAC;
						break;
					case PROGRAM_ARGS_SOLARIS:
						state = STATE_PROGRAM_ARGS_SOLARIS;
						break;
					case PROGRAM_ARGS_WIN:
						state = STATE_PROGRAM_ARGS_WIN;
						break;
					case VM_ARGS:
						state = STATE_VM_ARGS;
						break;
					case VM_ARGS_LINUX:
						state = STATE_VM_ARGS_LINUX;
						break;
					case VM_ARGS_MAC:
						state = STATE_VM_ARGS_MAC;
						break;
					case VM_ARGS_SOLARIS:
						state = STATE_VM_ARGS_SOLARIS;
						break;
					case VM_ARGS_WIN:
						state = STATE_VM_ARGS_WIN;
						break;
					default:
						break;
				}
				break;


			// For all argument states.  Set a platform key prefix representing 
			// the outer state (platform) of the launcher arguments and then 
			// set the state of the inner state (architecture).
			case STATE_PROGRAM_ARGS :
				platformKeyPrefix = PROGRAM_ARGS;
				setArchState(localName);
				break;

			case STATE_PROGRAM_ARGS_LINUX :
				platformKeyPrefix = PROGRAM_ARGS_LINUX;
				setArchState(localName);
				break;

			case STATE_PROGRAM_ARGS_MAC :
				platformKeyPrefix = PROGRAM_ARGS_MAC;
				setArchState(localName);
				break;

			case STATE_PROGRAM_ARGS_SOLARIS :
				platformKeyPrefix = PROGRAM_ARGS_SOLARIS;
				setArchState(localName);
				break;

			case STATE_PROGRAM_ARGS_WIN :
				platformKeyPrefix = PROGRAM_ARGS_WIN;
				setArchState(localName);
				break;

			case STATE_VM_ARGS :
				platformKeyPrefix = VM_ARGS;
				setArchState(localName);
				break;

			case STATE_VM_ARGS_LINUX :
				platformKeyPrefix = VM_ARGS_LINUX;
				setArchState(localName);
				break;

			case STATE_VM_ARGS_MAC :
				platformKeyPrefix = VM_ARGS_MAC;
				setArchState(localName);
				break;

			case STATE_VM_ARGS_SOLARIS :
				platformKeyPrefix = VM_ARGS_SOLARIS;
				setArchState(localName);
				break;

			case STATE_VM_ARGS_WIN :
				platformKeyPrefix = VM_ARGS_WIN;
				setArchState(localName);
				break;

			case STATE_PLUGINS :
				if (EL_PLUGIN.equals(localName)) {
					processPlugin(attributes);
				}
				break;

			case STATE_REPOSITORIES :
				if (EL_REPOSITORY.equals(localName)) {
					processRepositoryInformation(attributes);
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

			case STATE_VM :
				if (null != localName) switch (localName) {
					case OS_LINUX:
						state = STATE_VM_LINUX;
						break;
					case OS_WINDOWS:
						state = STATE_VM_WINDOWS;
						break;
					case OS_MACOS:
						state = STATE_VM_MACOS;
						break;
					default:
						break;
				}
				break;

		}
	}

	private void setArchState(String archName) {
		outerState = state;
		if (null != archName) switch (archName) {
			case EL_ARCH_X86:
				state = STATE_ARCH_X86;
				break;
			case EL_ARCH_X86_64:
				state = STATE_ARCH_X86_64;
				break;
			case EL_ARCH_PPC:
				state = STATE_ARCH_PPC;
				break;
			case EL_ARCH_IA_64:
				state = STATE_ARCH_IA_64;
				break;
			case EL_ARCH_IA_64_32:
				state = STATE_ARCH_IA_64_32;
				break;
			case EL_ARCH_PA_RISC:
				state = STATE_ARCH_PA_RISC;
				break;
			case EL_ARCH_SPARC:
				state = STATE_ARCH_SPARC;
				break;
			default:
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
		String os = attributes.getValue(ATTRIBUTE_OS);
		if (os == null)
			os = ""; //$NON-NLS-1$
		String arch = attributes.getValue(ATTRIBUTE_ARCH);
		if (arch == null)
			arch = ""; //$NON-NLS-1$
		String propOSArchKey = os + "." + arch; //$NON-NLS-1$
		if (name == null)
			return;
		if (value == null)
			value = ""; //$NON-NLS-1$
		if (propOSArchKey.length() <= 1) {
			// this is a generic property for all platforms and arch
			if (properties == null)
				properties = new HashMap<>();
			properties.put(name, value);
		} else {
			// store the property in the filtered map, keyed by "os.arch"
			if (filteredProperties == null)
				filteredProperties = new HashMap<>();
			HashMap<String, String> filteredMap = filteredProperties.get(propOSArchKey);
			if (filteredMap == null) {
				filteredMap = new HashMap<>();
				filteredProperties.put(propOSArchKey, filteredMap);
			}
			filteredMap.put(name, value);
		}
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
			info.setMarkedAsStarted(Boolean.parseBoolean(value));
		if (bundleInfos == null)
			bundleInfos = new ArrayList<>();
		bundleInfos.add(info);
	}

	private void processRepositoryInformation(Attributes attributes) {
		try {
			URI uri = URIUtil.fromString(attributes.getValue(ATTRIBUTE_LOCATION));
			String name = attributes.getValue(ATTRIBUTE_NAME);
			boolean enabled = Boolean.parseBoolean(attributes.getValue(ATTRIBUTE_ENABLED));
			int options = enabled ? IRepository.ENABLED : IRepository.NONE;
			// First add a metadata repository
			repositories.add(new RepositoryReference(uri, name, IRepository.TYPE_METADATA, options));
			// Now a colocated artifact repository
			repositories.add(new RepositoryReference(uri, name, IRepository.TYPE_ARTIFACT, options));
		} catch (URISyntaxException e) {
			// ignore malformed URI's. These should have already been caught by the UI
		}
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
			case STATE_VM :
				state = STATE_PRODUCT;
				break;
			case STATE_VM_LINUX :
			case STATE_VM_WINDOWS :
			case STATE_VM_MACOS :
				state = STATE_VM;
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

			case STATE_ARCH_X86 :
			case STATE_ARCH_X86_64 :
			case STATE_ARCH_PPC :
			case STATE_ARCH_IA_64 :
			case STATE_ARCH_IA_64_32 :
			case STATE_ARCH_PA_RISC :
			case STATE_ARCH_SPARC :
				state = outerState;
				break;

			case STATE_CONFIG_INI :
				if (EL_CONFIG_INI.equals(localName))
					state = STATE_PRODUCT;
				else
					processConfigIniPlatform(localName, false);
				break;

			case STATE_REPOSITORIES :
				if (EL_REPOSITORIES.equals(localName))
					state = STATE_PRODUCT;
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
			case STATE_ARCH_X86 :
				addLaunchArgumentToMap(platformKeyPrefix + "." + EL_ARCH_X86, String.valueOf(ch, start, length)); //$NON-NLS-1$
				break;
			case STATE_ARCH_X86_64 :
				addLaunchArgumentToMap(platformKeyPrefix + "." + EL_ARCH_X86_64, String.valueOf(ch, start, length)); //$NON-NLS-1$
				break;
			case STATE_ARCH_PPC :
				addLaunchArgumentToMap(platformKeyPrefix + "." + EL_ARCH_PPC, String.valueOf(ch, start, length)); //$NON-NLS-1$
				break;
			case STATE_ARCH_IA_64 :
				addLaunchArgumentToMap(platformKeyPrefix + "." + EL_ARCH_IA_64, String.valueOf(ch, start, length)); //$NON-NLS-1$
				break;
			case STATE_ARCH_IA_64_32 :
				addLaunchArgumentToMap(platformKeyPrefix + "." + EL_ARCH_IA_64_32, String.valueOf(ch, start, length)); //$NON-NLS-1$
				break;
			case STATE_ARCH_PA_RISC :
				addLaunchArgumentToMap(platformKeyPrefix + "." + EL_ARCH_PA_RISC, String.valueOf(ch, start, length)); //$NON-NLS-1$
				break;
			case STATE_ARCH_SPARC :
				addLaunchArgumentToMap(platformKeyPrefix + "." + EL_ARCH_SPARC, String.valueOf(ch, start, length)); //$NON-NLS-1$
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
			case STATE_VM_LINUX :
				addVM(OS_LINUX, String.valueOf(ch, start, length));
				break;
			case STATE_VM_WINDOWS :
				addVM(OS_WINDOWS, String.valueOf(ch, start, length));
				break;
			case STATE_VM_MACOS :
				addVM(OS_MACOS, String.valueOf(ch, start, length));
				break;
		}
	}

	private void addVM(String os, String vm) {
		vms.put(os, vm);
	}

	@Override
	public String getVM(String os) {
		if (os.equals(OS_MACOSX)) {
			os = OS_MACOS;
		} else if (os.equals(OS_WIN32)) {
			os = OS_WINDOWS;
		}
		return vms.get(os);
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
		entry.setFragment(Boolean.parseBoolean(fragment));
		plugins.add(entry);
	}

	private void processFeature(Attributes attributes) {
		String featureId = attributes.getValue(ATTRIBUTE_ID);
		String featureVersion = attributes.getValue(ATTRIBUTE_VERSION);
		FeatureInstallMode installMode = FeatureInstallMode.parse(attributes.getValue(ATTRIBUTE_FEATURE_INSTALL_MODE));
		FeatureEntry featureEntry = new FeatureEntry(featureId, featureVersion != null ? featureVersion : GENERIC_VERSION_NUMBER, false);

		switch (installMode) {
			case ROOT :
				rootFeatures.add(featureEntry);
				break;
			default :
				features.add(featureEntry);
		}
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
			if (use != null && Boolean.parseBoolean(use))
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
			Location instanceLocation = getInstanceLocation();
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
			list = new ArrayList<>(6);
			icons.put(os, list);
		}
		list.add(iconFile.getAbsolutePath());
	}

	protected Location getInstanceLocation() {
		return ServiceHelper.getService(Activator.getContext(), Location.class, Location.INSTANCE_FILTER);
	}

	private void processSolaris(Attributes attributes) {
		addIcon(OS_SOLARIS, attributes.getValue(SOLARIS_LARGE));
		addIcon(OS_SOLARIS, attributes.getValue(SOLARIS_MEDIUM));
		addIcon(OS_SOLARIS, attributes.getValue(SOLARIS_SMALL));
		addIcon(OS_SOLARIS, attributes.getValue(SOLARIS_TINY));
	}

	private void processWin(Attributes attributes) {
		//		useIco = Boolean.parseBoolean(attributes.getValue(P_USE_ICO));
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

	@Override
	public ProductContentType getProductContentType() {
		return productContentType;
	}
}
