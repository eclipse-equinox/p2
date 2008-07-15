/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;
import org.eclipse.core.runtime.internal.adaptor.EclipseEnvironmentInfo;
import org.eclipse.equinox.internal.frameworkadmin.equinox.utils.FileUtils;
import org.eclipse.equinox.internal.frameworkadmin.utils.SimpleBundlesState;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;

public class EquinoxBundlesState implements BundlesState {
	static final long DEFAULT_TIMESTAMP = 0L;
	private static final boolean DEBUG = false;
	// While we recognize the amd64 architecture, we change
	// this internally to be x86_64.
	private static final String INTERNAL_AMD64 = "amd64"; //$NON-NLS-1$
	private static final String INTERNAL_ARCH_I386 = "i386"; //$NON-NLS-1$
	public static final String[] PROPS = {"osgi.os", "osgi.ws", "osgi.nl", "osgi.arch", Constants.FRAMEWORK_SYSTEMPACKAGES, "osgi.resolverMode", Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "osgi.resolveOptional", "osgi.genericAliases"};

	static boolean checkFullySupported() {
		//TODO - This was previously doing a bogus check by attempting to instantiate a particular class - it's not clear what this is trying to do
		return true;
	}

	/**
	 * eclipse.exe will launch a fw where plugins/org.eclipse.osgi_*.*.*.*.jar
	 * is an implementation of fw.
	 * 
	 * @param launcherData
	 * @param configData
	 * @return File of fwJar to be used.
	 */
	static File getFwJar(LauncherData launcherData, ConfigData configData) {
		return getFwJar(launcherData, configData, true);
		//
		// // EclipseLauncherParser launcherParser = new
		// EclipseLauncherParser(launcherData);
		// // launcherParser.read();
		// if (launcherData.getFwJar() != null)
		// return launcherData.getFwJar();
		//
		// // check -D arguments of jvmArgs ?
		// String[] jvmArgs = launcherData.getJvmArgs();
		// String location = null;
		// for (int i = 0; i < jvmArgs.length; i++) {
		// if (jvmArgs[i].endsWith("-D" + "osgi.framework=")) {
		// location = jvmArgs[i].substring(("-D" + "osgi.framework=").length());
		// }
		// }
		// if (location != null)
		// return new File(location);
		//
		// File ret = getSystemBundleFromBundleInfos(launcherData, configData);
		// if (ret != null)
		// return ret;
		// return getSystemBundleBySearching(launcherData);
	}

	private static File getFwJar(LauncherData launcherData, ConfigData configData, boolean checkBundleInfos) {
		// EclipseLauncherParser launcherParser = new
		// EclipseLauncherParser(launcherData);
		// launcherParser.read();
		if (launcherData.getFwJar() != null) {
			return launcherData.getFwJar();
		}

		// check -D arguments of jvmArgs ?
		String[] jvmArgs = launcherData.getJvmArgs();
		String location = null;
		for (int i = 0; i < jvmArgs.length; i++) {
			if (jvmArgs[i].endsWith("-D" + "osgi.framework=")) {
				location = jvmArgs[i].substring(("-D" + "osgi.framework=").length());
			}
		}
		if (location != null) {
			return new File(location);
		}

		if (checkBundleInfos) {
			File ret = getSystemBundleFromBundleInfos(configData);
			if (ret != null) {
				return ret;
			}
		}
		return null;
		//		return getSystemBundleBySearching(launcherData);
	}

	private static long getMaxId(State state) {
		BundleDescription[] bundleDescriptions = state.getBundles();
		long maxId = DEFAULT_TIMESTAMP;
		for (int i = 0; i < bundleDescriptions.length; i++)
			if (maxId < bundleDescriptions[i].getBundleId()) {
				maxId = bundleDescriptions[i].getBundleId();
			}
		return maxId;
	}

	//	private static File getSystemBundleBySearching(LauncherData launcherData) {
	//		File pluginsDir;
	//		if (launcherData.getLauncher() == null) {
	//			if (launcherData.getHome() == null)
	//				return null;
	//			pluginsDir = new File(launcherData.getHome(), "plugins");
	//		} else {
	//			pluginsDir = new File(launcherData.getLauncher().getParentFile(), "plugins");
	//		}
	//
	//		String fullLocation = FileUtils.getEclipsePluginFullLocation(EquinoxConstants.FW_SYMBOLIC_NAME, pluginsDir);
	//		if (fullLocation == null)
	//			return null;
	//		URL url = null;
	//		try {
	//			url = new URL(fullLocation);
	//		} catch (MalformedURLException e) {
	//			// TODO Auto-generated catch block
	//			Log.log(LogService.LOG_WARNING, "fullLocation is not in proper format:" + fullLocation);
	//		}
	//		return url == null ? null : new File(url.getFile());
	//	}

	private static File getSystemBundleFromBundleInfos(BundleInfo[] bundleInfos) {
		for (int i = 0; i < bundleInfos.length; i++) {
			File match = isSystemBundle(bundleInfos[i]);
			if (match != null)
				return match;
		}
		return null;
	}

	private static File getSystemBundleFromBundleInfos(ConfigData configData) {
		BundleInfo[] bundleInfos = configData.getBundles();
		return getSystemBundleFromBundleInfos(bundleInfos);
	}

	static long getTimeStamp(File fwPersistentDataLocation) {
		if (fwPersistentDataLocation == null)
			return DEFAULT_TIMESTAMP;

		File file = new File(fwPersistentDataLocation, EquinoxConstants.PERSISTENT_DIR_NAME);
		if (!file.exists() || !file.isDirectory())
			return DEFAULT_TIMESTAMP;
		long ret = file.lastModified();
		File[] lists = file.listFiles();
		if (lists == null)
			return ret;
		for (int i = 0; i < lists.length; i++)
			if (ret < lists[i].lastModified())
				ret = lists[i].lastModified();
		return ret;
	}

	public static File isSystemBundle(BundleInfo bundleInfo) {
		if (bundleInfo == null || bundleInfo.getLocation() == null)
			return null;
		String bundleLocation = bundleInfo.getLocation();
		if (bundleLocation.startsWith(EquinoxConstants.REFERENCE))
			bundleLocation = bundleLocation.substring(EquinoxConstants.REFERENCE.length());
		if (bundleLocation.startsWith("file:")) {
			try {
				String[] clauses = Utils.getClausesManifestMainAttributes(bundleLocation, Constants.BUNDLE_SYMBOLICNAME);
				if (bundleLocation.indexOf(EquinoxConstants.FW_SYMBOLIC_NAME) > 0)
					if (EquinoxConstants.PERSISTENT_DIR_NAME.equals(Utils.getPathFromClause(clauses[0])))
						return new File(bundleLocation.substring("file:".length()));
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	// "osgi.os", "osgi.ws", "osgi.nl", "osgi.arch",
	// Constants.FRAMEWORK_SYSTEMPACKAGES, "osgi.resolverMode",
	// Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "osgi.resolveOptional"
	static Properties setDefaultPlatformProperties() {
		Properties platformProperties = new Properties();
		// set default value

		String nl = Locale.getDefault().toString();
		platformProperties.setProperty("osgi.nl", nl); //$NON-NLS-1$

		// TODO remove EclipseEnvironmentInof
		String os = EclipseEnvironmentInfo.guessOS(System.getProperty("os.name"));//$NON-NLS-1$);
		platformProperties.setProperty("osgi.os", os); //$NON-NLS-1$

		String ws = EclipseEnvironmentInfo.guessWS(os);
		platformProperties.setProperty("osgi.ws", ws);

		// if the user didn't set the system architecture with a command line
		// argument then use the default.
		String arch = null;
		String name = FrameworkProperties.getProperty("os.arch");//$NON-NLS-1$
		// Map i386 architecture to x86
		if (name.equalsIgnoreCase(INTERNAL_ARCH_I386))
			arch = org.eclipse.osgi.service.environment.Constants.ARCH_X86;
		// Map amd64 architecture to x86_64
		else if (name.equalsIgnoreCase(INTERNAL_AMD64))
			arch = org.eclipse.osgi.service.environment.Constants.ARCH_X86_64;
		else
			arch = name;
		platformProperties.setProperty("osgi.arch", arch); //$NON-NLS-1$			

		platformProperties.setProperty(Constants.FRAMEWORK_SYSTEMPACKAGES, FrameworkProperties.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES));
		platformProperties.setProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, FrameworkProperties.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT));
		platformProperties.setProperty("osgi.resolveOptional", "" + "true".equals(FrameworkProperties.getProperty("osgi.resolveOptional")));
		return platformProperties;
	}

	EquinoxFwAdminImpl fwAdmin = null;

	BundleContext context;

	Manipulator manipulator = null;
	Properties platfromProperties = new Properties();

	long maxId = DEFAULT_TIMESTAMP;

	StateObjectFactory soFactory = null;

	State state = null;
	private HashMap locationStateIndex = new HashMap();
	private HashMap nameVersionStateIndex = new HashMap();

	/**
	 * If useFwPersistentData flag equals false, this constructor will not take
	 * a framework persistent data into account. Otherwise, it will.
	 * 
	 * @param context
	 * @param fwAdmin
	 * @param manipulator
	 * @param useFwPersistentData
	 */
	EquinoxBundlesState(BundleContext context, EquinoxFwAdminImpl fwAdmin, Manipulator manipulator, boolean useFwPersistentData) {
		this.context = context;
		this.fwAdmin = fwAdmin;
		// copy manipulator object for avoiding modifying the parameters of the
		// manipulator.
		this.manipulator = fwAdmin.getManipulator();
		this.manipulator.setConfigData(manipulator.getConfigData());
		this.manipulator.setLauncherData(manipulator.getLauncherData());
		initialize(useFwPersistentData);
	}

	/**
	 * This constructor does NOT take a framework persistent data into account.
	 * It will create State object based on the specified platformProperties.
	 * 
	 * @param context
	 * @param fwAdmin
	 * @param manipulator
	 * @param platformProperties
	 */
	EquinoxBundlesState(BundleContext context, EquinoxFwAdminImpl fwAdmin, Manipulator manipulator, Properties platformProperties) {
		super();
		this.context = context;
		this.fwAdmin = fwAdmin;
		// copy manipulator object for avoiding modifying the parameters of the
		// manipulator.
		this.manipulator = fwAdmin.getManipulator();
		this.manipulator.setConfigData(manipulator.getConfigData());
		this.manipulator.setLauncherData(manipulator.getLauncherData());
		LauncherData launcherData = manipulator.getLauncherData();
		ConfigData configData = manipulator.getConfigData();
		BundleInfo[] bInfos = configData.getBundles();
		this.composeNewState(launcherData, configData, platformProperties, bInfos);
	}

	/**
	 * compose new state without reading framework persistent data. The
	 * configData.getFwDependentProps() is used for the composition.
	 * 
	 * @param launcherData
	 * @param configData
	 * @param bInfos
	 */
	private void composeNewState(LauncherData launcherData, ConfigData configData, BundleInfo[] bInfos) {
		this.composeNewState(launcherData, configData, configData.getFwDependentProps(), bInfos);
	}

	/**
	 * compose new state without reading framework persistent data. The given
	 * properties is used for the composition. If system bundle is not included
	 * in the given bInfos, the fw jar launcherData contains will be used.
	 * 
	 * @param launcherData
	 * @param configData
	 * @param properties
	 * @param bInfos
	 */
	private void composeNewState(LauncherData launcherData, ConfigData configData, Properties properties, BundleInfo[] bInfos) {
		//Note, there use to be a lot more code in this method
		File fwJar = getSystemBundleFromBundleInfos(configData);
		launcherData.setFwJar(fwJar);
		this.setFwJar(fwJar);
		composeState(configData.getBundles(), properties, null);
		resolve(true);
	}

	/**
	 * compose state. If it cannot compose it by somehow, false is returned.
	 * 
	 * @param bInfos
	 * @param props
	 * @param fwPersistentDataLocation
	 * @return if it cannot compose it by somehow, false is returned.
	 * @throws IllegalArgumentException
	 * @throws FrameworkAdminRuntimeException
	 */
	private boolean composeState(BundleInfo[] bInfos, Dictionary props, File fwPersistentDataLocation) throws IllegalArgumentException, FrameworkAdminRuntimeException {
		BundleInfo[] infos = manipulator.getConfigData().getBundles();
		this.manipulator.getConfigData().setBundles(null);
		SimpleBundlesState.checkAvailability(fwAdmin);
		this.setStateObjectFactory();
		state = null;
		boolean flagNewState = false;
		if (fwPersistentDataLocation != null) {
			//NOTE Here there was a big chunk of code reading the framework state persisted on disk
			// and I removed it because it was causing various problems. See in previous revision
			this.manipulator.getConfigData().setBundles(infos);
			return false;
		}
		state = soFactory.createState(true);
		createStateIndexes();
		flagNewState = true;
		if (props == null) {
			this.manipulator.getConfigData().setBundles(infos);
			return false;
		}
		setPlatformPropertiesToState(props);
		setPlatformProperties(state);

		try {
			maxId = state.getHighestBundleId();
		} catch (NoSuchMethodError e) {
			maxId = getMaxId(state);
		}
		if (DEBUG) {
			System.out.println("");
			Log.log(LogService.LOG_DEBUG, this, "composeExpectedState()", "installBundle():");
		}
		if (flagNewState) {
			int indexSystemBundle = -1;
			for (int j = 0; j < bInfos.length; j++)
				if (isSystemBundle(bInfos[j]) != null) {
					indexSystemBundle = j;
					break;
				}

			if (indexSystemBundle > 0) {
				BundleInfo[] newBundleInfos = new BundleInfo[bInfos.length];
				newBundleInfos[0] = bInfos[indexSystemBundle];
				System.arraycopy(bInfos, 0, newBundleInfos, 1, indexSystemBundle);
				if (indexSystemBundle < bInfos.length - 1)
					System.arraycopy(bInfos, indexSystemBundle + 1, newBundleInfos, indexSystemBundle + 1, bInfos.length - indexSystemBundle - 1);
				bInfos = newBundleInfos;
			}
		}
		for (int j = 0; j < bInfos.length; j++) {
			if (DEBUG)
				Log.log(LogService.LOG_DEBUG, this, "composeExpectedState()", "bInfos[" + j + "]=" + bInfos[j]);
			try {
				this.installBundle(bInfos[j]);
				// System.out.println("install bInfos[" + j + "]=" + bInfos[j]);
			} catch (RuntimeException e) {
				//catch the exception and continue
				Log.log(LogService.LOG_ERROR, this, "composeExpectedState()", "BundleInfo:" + bInfos[j], e);
			}
		}
		return true;
	}

	private BundleInfo convertSystemBundle(BundleDescription toConvert) {
		// Converting the System Bundle
		boolean markedAsStarted = false;
		int sl = BundleInfo.NO_LEVEL;

		String location = null;
		String symbolicNameTarget = toConvert.getSymbolicName();
		Version versionTarget = toConvert.getVersion();
		try {
			File fwJar = manipulator.getLauncherData().getFwJar();
			if (fwJar != null) {
				String fwJarLocation = fwJar.toURL().toExternalForm();
				String[] clauses = Utils.getClausesManifestMainAttributes(fwJarLocation, Constants.BUNDLE_SYMBOLICNAME);
				String fwJarSymbolicName = Utils.getPathFromClause(clauses[0]);
				String fwJarVersionSt = Utils.getManifestMainAttributes(fwJarLocation, Constants.BUNDLE_VERSION);
				if (fwJarSymbolicName.equals(symbolicNameTarget) && fwJarVersionSt.equals(versionTarget.toString())) {
					location = fwJarLocation;
					markedAsStarted = true;
				}
			}
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (FrameworkAdminRuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return createBundleInfo(toConvert, markedAsStarted, sl, location);
	}

	private BundleInfo createBundleInfo(BundleDescription toConvert, boolean markedAsStarted, int sl, String location) {
		BundleInfo result = new BundleInfo();
		result.setSymbolicName(toConvert.getSymbolicName());
		result.setVersion(toConvert.getVersion().toString());
		result.setLocation(location);
		result.setResolved(toConvert.isResolved());
		result.setMarkedAsStarted(markedAsStarted);
		result.setStartLevel(sl);
		result.setBundleId(toConvert.getBundleId());
		return result;
	}

	public BundleInfo[] convertState(BundleDescription[] bundles) {
		BundleInfo[] originalBInfos = manipulator.getConfigData().getBundles();
		Map bundleInfoMap = new HashMap();
		for (int i = 0; i < originalBInfos.length; i++) {
			bundleInfoMap.put(originalBInfos[i].getLocation(), originalBInfos[i]);
		}

		BundleInfo[] result = new BundleInfo[bundles.length];
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].getBundleId() == 0 && EquinoxConstants.FW_SYMBOLIC_NAME.equals(bundles[i].getSymbolicName())) {
				result[i] = convertSystemBundle(bundles[i]);
				continue;
			}

			boolean markedAsStarted = false;
			int sl = BundleInfo.NO_LEVEL;
			String location = FileUtils.getEclipseRealLocation(manipulator, bundles[i].getLocation());
			BundleInfo original = (BundleInfo) bundleInfoMap.get(location);
			if (original != null) {
				markedAsStarted = original.isMarkedAsStarted();
				sl = getStartLevel(original.getStartLevel());
			}
			result[i] = createBundleInfo(bundles[i], markedAsStarted, sl, location);
		}
		return result;
	}

	public BundleInfo[] getExpectedState() throws FrameworkAdminRuntimeException {
		SimpleBundlesState.checkAvailability(fwAdmin);
		return convertState(state.getBundles());
	}

	Properties getPlatformProperties() {
		return platfromProperties;
	}

	public BundleInfo[] getPrerequisteBundles(BundleInfo bInfo) {
		Set set = new HashSet();
		String realLocation = FileUtils.getRealLocation(manipulator, bInfo.getLocation(), true);
		BundleDescription bundle = getBundleByLocation(realLocation);
		ImportPackageSpecification[] imports = bundle.getImportPackages();
		for (int i = 0; i < imports.length; i++) {
			BaseDescription supplier = imports[i].getSupplier();
			if (supplier == null) {
				if (!imports[i].getDirective(Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_OPTIONAL))
					throw new IllegalStateException("Internal error: import supplier should not be null");
			} else
				set.add(supplier.getSupplier());
			// System.out.println(supplier.getSupplier());
		}
		BundleDescription[] requires = bundle.getResolvedRequires();
		for (int i = 0; i < requires.length; i++) {
			set.add(requires[i]);
		}
		BundleDescription[] bundles = new BundleDescription[set.size()];
		set.toArray(bundles);
		return convertState(bundles);
	}

	private int getStartLevel(int startLevel) {
		return (startLevel == BundleInfo.NO_LEVEL ? manipulator.getConfigData().getInitialBundleStartLevel() : startLevel);
	}

	public BundleInfo getSystemBundle() {
		BundleDescription bundle = this.getSystemBundleDescription();
		return (bundle != null ? convertSystemBundle(bundle) : null);
	}

	private BundleDescription getSystemBundleDescription() {
		BundleDescription bundle = state.getBundle(0);
		if (bundle == null || bundle.getHost() != null) { // null or a
			// fragment bundle.
			return null;
		}
		// if (DEBUG) {
		// System.out.println("EquinoxConstants.FW_SYMBOLIC_NAME=" +
		// EquinoxConstants.FW_SYMBOLIC_NAME);
		// System.out.println("bundle.getSymbolicName()=" +
		// bundle.getSymbolicName());
		// }
		return (EquinoxConstants.FW_SYMBOLIC_NAME.equals(bundle.getSymbolicName()) ? bundle : null);
	}

	public BundleInfo[] getSystemFragmentedBundles() {
		BundleDescription bundle = this.getSystemBundleDescription();
		if (bundle == null)
			return null;
		return convertState(bundle.getFragments());
	}

	public String[] getUnsatisfiedConstraints(BundleInfo bInfo) {
		String realLocation = FileUtils.getRealLocation(manipulator, bInfo.getLocation(), true);
		BundleDescription description = getBundleByLocation(realLocation);
		PlatformAdmin platformAdmin = (PlatformAdmin) Activator.acquireService(PlatformAdmin.class.getName());
		StateHelper helper = platformAdmin.getStateHelper();
		VersionConstraint[] constraints = helper.getUnsatisfiedConstraints(description);
		String[] ret = new String[constraints.length];
		for (int i = 0; i < constraints.length; i++)
			ret[i] = constraints[i].toString();
		return ret;
	}

	private void initialize(boolean useFwPersistentData) {
		LauncherData launcherData = manipulator.getLauncherData();
		ConfigData configData = manipulator.getConfigData();
		BundleInfo[] bInfos = configData.getBundles();

		if (!useFwPersistentData) {
			composeNewState(launcherData, configData, bInfos);
			return;
		}

		EquinoxManipulatorImpl.checkConsistencyOfFwConfigLocAndFwPersistentDataLoc(launcherData);
		if (launcherData.isClean()) {
			composeNewState(launcherData, configData, bInfos);
		} else {
			if (manipulator.getLauncherData().getFwPersistentDataLocation() == null) {
				// TODO default value should be set more precisely.
				File installArea = null;
				String installAreaSt = configData.getFwDependentProp(EquinoxConstants.PROP_INSTALL);
				if (installAreaSt == null) {
					if (manipulator.getLauncherData().getLauncher() == null) {
						// TODO implement
					} else {
						installArea = manipulator.getLauncherData().getLauncher().getParentFile();
					}
				} else {
					if (installAreaSt.startsWith("file:"))
						installArea = new File(installAreaSt.substring("file:".length()));
					else
						throw new IllegalStateException("Current implementation assume that property value keyed by " + EquinoxConstants.PROP_INSTALL + " must start with \"file:\". But it was not:" + installAreaSt);
				}
				if (DEBUG)
					Log.log(LogService.LOG_DEBUG, this, "initialize(useFwPersistentDat)", "installArea=" + installArea);
				File fwPersistentDataLocation = new File(installArea, "configuration");
				manipulator.getLauncherData().setFwPersistentDataLocation(fwPersistentDataLocation, false);
			}
			if (!composeState(bInfos, null, manipulator.getLauncherData().getFwPersistentDataLocation()))
				composeNewState(launcherData, configData, bInfos);
			resolve(true);
			// if(this.getSystemBundle()==null)
		}
	}

	public void installBundle(BundleInfo bInfo) throws FrameworkAdminRuntimeException {
		SimpleBundlesState.checkAvailability(fwAdmin);

		String realLocation = FileUtils.getRealLocation(manipulator, bInfo.getLocation(), true);
		if (getBundleByLocation(realLocation) != null)
			return;

		Dictionary manifest = Utils.getOSGiManifest(realLocation);
		if (manifest == null)
			return;

		String newSymbolicName = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
		int position = newSymbolicName.indexOf(";"); //$NON-NLS-1$
		if (position >= 0)
			newSymbolicName = newSymbolicName.substring(0, position).trim();
		String newVersion = (String) manifest.get(Constants.BUNDLE_VERSION);

		if (getBundleByNameVersion(newSymbolicName, newVersion) != null)
			return;

		try {
			bInfo.setBundleId(++maxId);
			BundleDescription newBundleDescription = soFactory.createBundleDescription(state, manifest, realLocation, bInfo.getBundleId());
			addBundleToState(newBundleDescription);
			manipulator.getConfigData().addBundle(bInfo);
		} catch (BundleException e) {
			Log.log(LogService.LOG_WARNING, this, "installBundle(BundleInfo)", e);
		}
	}

	public boolean isFullySupported() {
		return true;
	}

	public boolean isResolved() {
		return state.isResolved();
	}

	public boolean isResolved(BundleInfo bInfo) {
		String realLocation = FileUtils.getRealLocation(manipulator, bInfo.getLocation(), true);
		BundleDescription description = getBundleByLocation(bInfo.getLocation());
		if (description == null)
			return false;
		return description.isResolved();
	}

	public void resolve(boolean increment) {
		state.resolve(increment);
	}

	void setFwJar(File fwJar) {
		manipulator.getLauncherData().setFwJar(fwJar);
	}

	/**
	 * get platforme properties from the given state.
	 * 
	 * @param state
	 */
	private void setPlatformProperties(State state) {
		Dictionary platformProperties = state.getPlatformProperties()[0];
		platfromProperties.clear();
		if (platformProperties != null) {
			for (Enumeration enumeration = platformProperties.keys(); enumeration.hasMoreElements();) {
				String key = (String) enumeration.nextElement();
				Object value = platformProperties.get(key);
				platfromProperties.setProperty(key, (String) value);
			}
		}
		if (DEBUG)
			Utils.printoutProperties(System.out, "PlatformProperties[0]", platfromProperties);
	}

	/**
	 * set platfromProperties required to compose state object into
	 * platformProperties of this state.
	 * 
	 * @param props
	 */
	private void setPlatformPropertiesToState(Dictionary props) {
		Properties platformProperties = setDefaultPlatformProperties();

		for (Enumeration enumeration = props.keys(); enumeration.hasMoreElements();) {
			String key = (String) enumeration.nextElement();
			for (int i = 0; i < PROPS.length; i++) {
				if (key.equals(PROPS[i])) {
					platformProperties.put(key, props.get(key));
					break;
				}
			}
		}
		state.setPlatformProperties(platformProperties);
	}

	private void setStateObjectFactory() {
		if (soFactory != null)
			return;
		PlatformAdmin platformAdmin = (PlatformAdmin) Activator.acquireService(PlatformAdmin.class.getName());
		// PlatformAdmin platformAdmin = (PlatformAdmin)
		// heBundleHelper.getDefault().acquireService(PlatformAdmin.class.getName());
		soFactory = platformAdmin.getFactory();
	}

	public String toString() {
		if (state == null)
			return null;
		StringBuffer sb = new StringBuffer();
		BundleDescription[] bundleDescriptions = state.getBundles();
		for (int i = 0; i < bundleDescriptions.length; i++) {
			sb.append(bundleDescriptions[i].getBundleId() + ":");
			sb.append(bundleDescriptions[i].toString() + "(");
			sb.append(bundleDescriptions[i].isResolved() + ")");
			String[] ees = bundleDescriptions[i].getExecutionEnvironments();
			for (int j = 0; j < ees.length; j++)
				sb.append(ees[j] + " ");
			sb.append("\n");
		}
		sb.append("PlatformProperties:\n");
		Dictionary[] dics = state.getPlatformProperties();
		for (int i = 0; i < dics.length; i++) {
			for (Enumeration enumeration = dics[i].keys(); enumeration.hasMoreElements();) {
				String key = (String) enumeration.nextElement();
				String value = (String) dics[i].get(key);
				sb.append(" (" + key + "," + value + ")\n");
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	public void uninstallBundle(BundleInfo bInfo) throws FrameworkAdminRuntimeException {
		SimpleBundlesState.checkAvailability(fwAdmin);
		long id = DEFAULT_TIMESTAMP;
		String realLocation = FileUtils.getRealLocation(manipulator, bInfo.getLocation(), true);
		BundleDescription bundle = getBundleByLocation(realLocation);
		if (bundle != null)
			id = bundle.getBundleId();

		if (id != DEFAULT_TIMESTAMP) {
			try {
				Dictionary manifest = Utils.getOSGiManifest(bInfo.getLocation());
				if (manifest == null) {
					Log.log(LogService.LOG_WARNING, this, "uninstallBundle(BundleInfo)", "Unable to get bundle manifest for: " + bInfo.getLocation());
					return;
				}
				BundleDescription bundleDescription = soFactory.createBundleDescription(state, manifest, realLocation, id);
				removeBundleFromState(bundleDescription);
				manipulator.getConfigData().removeBundle(bInfo);
			} catch (BundleException e) {
				Log.log(LogService.LOG_WARNING, this, "uninstallBundle(BundleInfo)", e);
				// throw new ManipulatorException("Fail to
				// createBundleDescription of bInfo:" + bInfo.toString(), e,
				// ManipulatorException.OTHERS);
			}
		}
	}

	private BundleDescription getBundleByLocation(String location) {
		return (BundleDescription) locationStateIndex.get(location);
	}

	private BundleDescription getBundleByNameVersion(String bundleSymbolicName, String bundleVersion) {
		return (BundleDescription) nameVersionStateIndex.get(bundleSymbolicName + ";" + bundleVersion);
	}

	private void createStateIndexes() {
		BundleDescription[] currentInstalledBundles = state.getBundles();
		for (int i = 0; i < currentInstalledBundles.length; i++) {
			String location = FileUtils.getRealLocation(manipulator, currentInstalledBundles[i].getLocation(), true);
			locationStateIndex.put(location, currentInstalledBundles[i]);

			String symbolicName = currentInstalledBundles[i].getSymbolicName();
			String version = currentInstalledBundles[i].getVersion().toString();
			nameVersionStateIndex.put(symbolicName + ";" + version, currentInstalledBundles[i]); //$NON-NLS-1$
		}
	}

	private void addBundleToState(BundleDescription bundleDescription) {
		state.addBundle(bundleDescription);
		String location = FileUtils.getRealLocation(manipulator, bundleDescription.getLocation(), true);
		locationStateIndex.put(location, bundleDescription);

		String symbolicName = bundleDescription.getSymbolicName();
		String version = bundleDescription.getVersion().toString();
		nameVersionStateIndex.put(symbolicName + ";" + version, bundleDescription); //$NON-NLS-1$
	}

	private void removeBundleFromState(BundleDescription bundleDescription) {
		String location = FileUtils.getRealLocation(manipulator, bundleDescription.getLocation(), true);
		locationStateIndex.remove(location);

		String symbolicName = bundleDescription.getSymbolicName();
		String version = bundleDescription.getVersion().toString();
		nameVersionStateIndex.remove(symbolicName + ";" + version); //$NON-NLS-1$
		state.removeBundle(bundleDescription);
	}
}
