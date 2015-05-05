/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.frameworkadmin.equinox.utils.FileUtils;
import org.eclipse.equinox.internal.frameworkadmin.utils.SimpleBundlesState;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;

public class EquinoxBundlesState implements BundlesState {
	static final long DEFAULT_TIMESTAMP = 0L;
	private static final boolean DEBUG = false;
	// While we recognize the amd64 architecture, we change
	// this internally to be x86_64.
	private static final String INTERNAL_AMD64 = "amd64"; //$NON-NLS-1$
	private static final String INTERNAL_ARCH_I386 = "i386"; //$NON-NLS-1$
	@SuppressWarnings("deprecation")
	public static final String[] PROPS = {"osgi.os", "osgi.ws", "osgi.nl", "osgi.arch", Constants.FRAMEWORK_SYSTEMPACKAGES, "osgi.resolverMode", Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "osgi.resolveOptional", "osgi.genericAliases"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

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
		if (launcherData.getFwJar() != null) {
			return launcherData.getFwJar();
		}

		// check -D arguments of jvmArgs ?
		String[] jvmArgs = launcherData.getJvmArgs();
		String location = null;
		for (int i = 0; i < jvmArgs.length; i++) {
			if (jvmArgs[i].endsWith("-D" + "osgi.framework=")) { //$NON-NLS-1$ //$NON-NLS-2$
				location = jvmArgs[i].substring(("-D" + "osgi.framework=").length()); //$NON-NLS-1$ //$NON-NLS-2$
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

	private static File getSystemBundleFromBundleInfos(BundleInfo[] bundleInfos) {
		for (int i = 0; i < bundleInfos.length; i++) {
			File match = isSystemBundle(bundleInfos[i]);
			if (match != null)
				return match;
		}
		return null;
	}

	protected static File getSystemBundleFromBundleInfos(ConfigData configData) {
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
		URI bundleLocation = bundleInfo.getLocation();
		try {
			String[] clauses = Utils.getClausesManifestMainAttributes(bundleLocation, Constants.BUNDLE_SYMBOLICNAME);
			if (bundleLocation.getPath().indexOf(EquinoxConstants.FW_SYMBOLIC_NAME) > 0)
				if (EquinoxConstants.PERSISTENT_DIR_NAME.equals(Utils.getPathFromClause(clauses[0])))
					return new File(bundleLocation);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return null;
	}

	// "osgi.os", "osgi.ws", "osgi.nl", "osgi.arch",
	// Constants.FRAMEWORK_SYSTEMPACKAGES, "osgi.resolverMode",
	// Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "osgi.resolveOptional"
	@SuppressWarnings("deprecation")
	private Properties setDefaultPlatformProperties() {
		Properties platformProperties = new Properties();
		// set default value

		ServiceReference<EnvironmentInfo> environmentRef = context.getServiceReference(EnvironmentInfo.class);
		EnvironmentInfo environment = environmentRef == null ? null : context.getService(environmentRef);
		if (environment != null) {
			try {
				String nl = Locale.getDefault().toString();
				platformProperties.setProperty("osgi.nl", nl); //$NON-NLS-1$

				String os = environment.getOS();
				platformProperties.setProperty("osgi.os", os); //$NON-NLS-1$

				String ws = environment.getWS();
				platformProperties.setProperty("osgi.ws", ws); //$NON-NLS-1$

				// if the user didn't set the system architecture with a command line
				// argument then use the default.
				String arch = null;
				String name = context.getProperty("os.arch");//$NON-NLS-1$
				// Map i386 architecture to x86
				if (name.equalsIgnoreCase(INTERNAL_ARCH_I386))
					arch = org.eclipse.osgi.service.environment.Constants.ARCH_X86;
				// Map amd64 architecture to x86_64
				else if (name.equalsIgnoreCase(INTERNAL_AMD64))
					arch = org.eclipse.osgi.service.environment.Constants.ARCH_X86_64;
				else
					arch = name;
				platformProperties.setProperty("osgi.arch", arch); //$NON-NLS-1$			

				platformProperties.setProperty(Constants.FRAMEWORK_SYSTEMPACKAGES, context.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES));
				platformProperties.setProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, context.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT));
				platformProperties.setProperty("osgi.resolveOptional", "" + "true".equals(context.getProperty("osgi.resolveOptional"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			} finally {
				context.ungetService(environmentRef);
			}
		}
		return platformProperties;
	}

	EquinoxFwAdminImpl fwAdmin = null;
	BundleContext context;
	Manipulator manipulator = null;
	Properties platfromProperties = new Properties();
	long maxId = DEFAULT_TIMESTAMP;
	StateObjectFactory soFactory = null;
	State state = null;

	/**
	 * Map of URI->BundleDescription, where the key is the bundle location.
	 */
	private HashMap<URI, BundleDescription> locationStateIndex = new HashMap<URI, BundleDescription>();

	/**
	 * Map of String->BundleDescription, where the key is the bundle name
	 * and version as defined by the {@link #getKey(BundleDescription)} method.
	 */
	private HashMap<String, BundleDescription> nameVersionStateIndex = new HashMap<String, BundleDescription>();
	private final PlatformAdmin platformAdmin;

	/**
	 * If useFwPersistentData flag equals false, this constructor will not take
	 * a framework persistent data into account. Otherwise, it will.
	 * 
	 * @param context
	 * @param fwAdmin
	 * @param manipulator
	 * @param useFwPersistentData
	 */
	EquinoxBundlesState(BundleContext context, EquinoxFwAdminImpl fwAdmin, Manipulator manipulator, PlatformAdmin admin, boolean useFwPersistentData) {
		this.context = context;
		this.fwAdmin = fwAdmin;
		this.platformAdmin = admin;
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
	EquinoxBundlesState(BundleContext context, EquinoxFwAdminImpl fwAdmin, Manipulator manipulator, PlatformAdmin admin, Properties platformProperties) {
		super();
		this.context = context;
		this.fwAdmin = fwAdmin;
		this.platformAdmin = admin;
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
		this.composeNewState(launcherData, configData, configData.getProperties(), bInfos);
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
	private boolean composeState(BundleInfo[] bInfos, Dictionary<Object, Object> props, File fwPersistentDataLocation) throws IllegalArgumentException, FrameworkAdminRuntimeException {
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
			System.out.println(""); //$NON-NLS-1$
			Log.log(LogService.LOG_DEBUG, this, "composeExpectedState()", "installBundle():"); //$NON-NLS-1$ //$NON-NLS-2$
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
				Log.log(LogService.LOG_DEBUG, this, "composeExpectedState()", "bInfos[" + j + "]=" + bInfos[j]); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			try {
				this.installBundle(bInfos[j]);
				// System.out.println("install bInfos[" + j + "]=" + bInfos[j]);
			} catch (RuntimeException e) {
				//catch the exception and continue
				Log.log(LogService.LOG_ERROR, this, "composeExpectedState()", "BundleInfo:" + bInfos[j], e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return true;
	}

	private BundleInfo convertSystemBundle(BundleDescription toConvert) {
		// Converting the System Bundle
		boolean markedAsStarted = false;
		int sl = BundleInfo.NO_LEVEL;

		URI location = null;
		String symbolicNameTarget = toConvert.getSymbolicName();
		Version versionTarget = toConvert.getVersion();
		try {
			File fwJar = manipulator.getLauncherData().getFwJar();
			if (fwJar != null) {
				URI fwJarLocation = fwJar.toURI();
				String[] clauses = Utils.getClausesManifestMainAttributes(fwJarLocation, Constants.BUNDLE_SYMBOLICNAME);
				String fwJarSymbolicName = Utils.getPathFromClause(clauses[0]);
				String fwJarVersionSt = Utils.getManifestMainAttributes(fwJarLocation, Constants.BUNDLE_VERSION);
				if (fwJarSymbolicName.equals(symbolicNameTarget) && fwJarVersionSt.equals(versionTarget.toString())) {
					location = fwJarLocation;
					markedAsStarted = true;
				}
			}
		} catch (FrameworkAdminRuntimeException e1) {
			Log.log(LogService.LOG_ERROR, "", e1); //$NON-NLS-1$
		}
		return createBundleInfo(toConvert, markedAsStarted, sl, location, null);
	}

	private BundleInfo createBundleInfo(BundleDescription toConvert, boolean markedAsStarted, int sl, URI location, String fragmentHost) {
		BundleInfo result = new BundleInfo();
		result.setSymbolicName(toConvert.getSymbolicName());
		result.setVersion(toConvert.getVersion().toString());
		result.setLocation(location);
		result.setResolved(toConvert.isResolved());
		result.setMarkedAsStarted(markedAsStarted);
		result.setStartLevel(sl);
		result.setBundleId(toConvert.getBundleId());
		result.setFragmentHost(fragmentHost);
		return result;
	}

	public BundleInfo[] convertState(BundleDescription[] bundles) {
		BundleInfo[] originalBInfos = manipulator.getConfigData().getBundles();
		Map<URI, BundleInfo> bundleInfoMap = new HashMap<URI, BundleInfo>();
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

			//URI location = FileUtils.getEclipseRealLocation(manipulator, bundles[i].getLocation());
			//TODO: I believe this is always an absolute URI
			URI location;
			try {
				location = new URI(bundles[i].getLocation());
			} catch (URISyntaxException e) {
				e.printStackTrace();
				throw new IllegalStateException("BundleDescription conversion problem" + e.getMessage(), e); //$NON-NLS-1$ //TODO path_fun
			}
			String fragmentHost = null;
			BundleInfo original = bundleInfoMap.get(location);
			if (original != null) {
				markedAsStarted = original.isMarkedAsStarted();
				sl = getStartLevel(original.getStartLevel());
				fragmentHost = original.getFragmentHost();
			}
			result[i] = createBundleInfo(bundles[i], markedAsStarted, sl, location, fragmentHost);
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
		Set<BundleDescription> set = new HashSet<BundleDescription>();
		URI realLocation = bInfo.getLocation();
		BundleDescription bundle = getBundleByLocation(realLocation);
		ImportPackageSpecification[] imports = bundle.getImportPackages();
		for (int i = 0; i < imports.length; i++) {
			BaseDescription supplier = imports[i].getSupplier();
			if (supplier == null) {
				if (!imports[i].getDirective(Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_OPTIONAL))
					throw new IllegalStateException("Internal error: import supplier should not be null"); //$NON-NLS-1$
			} else
				set.add(supplier.getSupplier());
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
		return (EquinoxConstants.FW_SYMBOLIC_NAME.equals(bundle.getSymbolicName()) ? bundle : null);
	}

	public BundleInfo[] getSystemFragmentedBundles() {
		BundleDescription bundle = this.getSystemBundleDescription();
		if (bundle == null)
			return null;
		return convertState(bundle.getFragments());
	}

	public String[] getUnsatisfiedConstraints(BundleInfo bInfo) {
		URI realLocation = bInfo.getLocation();
		BundleDescription description = getBundleByLocation(realLocation);
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
				File installArea = ParserUtils.getOSGiInstallArea(Arrays.asList(launcherData.getProgramArgs()), configData.getProperties(), launcherData);
				if (DEBUG)
					Log.log(LogService.LOG_DEBUG, this, "initialize(useFwPersistentDat)", "installArea=" + installArea); //$NON-NLS-1$ //$NON-NLS-2$
				if (installArea == null)
					throw new IllegalStateException(Messages.exception_noInstallArea);
				File fwPersistentDataLocation = new File(installArea, "configuration"); //$NON-NLS-1$
				manipulator.getLauncherData().setFwPersistentDataLocation(fwPersistentDataLocation, false);
			}
			if (!composeState(bInfos, null, manipulator.getLauncherData().getFwPersistentDataLocation()))
				composeNewState(launcherData, configData, bInfos);
			resolve(true);
		}
	}

	public void installBundle(BundleInfo bInfo) throws FrameworkAdminRuntimeException {
		SimpleBundlesState.checkAvailability(fwAdmin);

		URI realLocation = bInfo.getLocation();
		if (getBundleByLocation(realLocation) != null)
			return;

		Dictionary<String, String> manifest = Utils.getOSGiManifest(realLocation);
		if (manifest == null)
			return;

		String newSymbolicName = manifest.get(Constants.BUNDLE_SYMBOLICNAME);
		int position = newSymbolicName.indexOf(";"); //$NON-NLS-1$
		if (position >= 0)
			newSymbolicName = newSymbolicName.substring(0, position).trim();
		String newVersion = manifest.get(Constants.BUNDLE_VERSION);

		if (getBundleByNameVersion(newSymbolicName, newVersion) != null)
			return;

		try {
			bInfo.setBundleId(++maxId);
			BundleDescription newBundleDescription = soFactory.createBundleDescription(state, manifest, realLocation.toString(), bInfo.getBundleId());
			addBundleToState(newBundleDescription);
			manipulator.getConfigData().addBundle(bInfo);
		} catch (BundleException e) {
			Log.log(LogService.LOG_WARNING, this, "installBundle(BundleInfo)", e); //$NON-NLS-1$
		}
	}

	public boolean isFullySupported() {
		return true;
	}

	public boolean isResolved() {
		return state.isResolved();
	}

	public boolean isResolved(BundleInfo bInfo) {
		URI realLocation = bInfo.getLocation();
		BundleDescription description = getBundleByLocation(realLocation);
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
		@SuppressWarnings("unchecked")
		Dictionary<String, String> platformProperties = state.getPlatformProperties()[0];
		platfromProperties.clear();
		if (platformProperties != null) {
			for (Enumeration<String> enumeration = platformProperties.keys(); enumeration.hasMoreElements();) {
				String key = enumeration.nextElement();
				Object value = platformProperties.get(key);
				platfromProperties.setProperty(key, (String) value);
			}
		}
		if (DEBUG)
			Utils.printoutProperties(System.out, "PlatformProperties[0]", platfromProperties); //$NON-NLS-1$
	}

	/**
	 * set platfromProperties required to compose state object into
	 * platformProperties of this state.
	 * 
	 * @param props
	 */
	private void setPlatformPropertiesToState(Dictionary<Object, Object> props) {
		Properties platformProperties = setDefaultPlatformProperties();

		for (Enumeration<Object> enumeration = props.keys(); enumeration.hasMoreElements();) {
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
		if (soFactory == null)
			soFactory = platformAdmin.getFactory();
	}

	public String toString() {
		if (state == null)
			return null;
		StringBuffer sb = new StringBuffer();
		BundleDescription[] bundleDescriptions = state.getBundles();
		for (int i = 0; i < bundleDescriptions.length; i++) {
			sb.append(bundleDescriptions[i].getBundleId() + ":"); //$NON-NLS-1$
			sb.append(bundleDescriptions[i].toString() + "("); //$NON-NLS-1$
			sb.append(bundleDescriptions[i].isResolved() + ")"); //$NON-NLS-1$
			String[] ees = bundleDescriptions[i].getExecutionEnvironments();
			for (int j = 0; j < ees.length; j++)
				sb.append(ees[j] + " "); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
		}
		sb.append("PlatformProperties:\n"); //$NON-NLS-1$
		@SuppressWarnings("rawtypes")
		Dictionary[] dics = state.getPlatformProperties();
		for (int i = 0; i < dics.length; i++) {
			// don't disable this warning because it will cause build-time warning.
			// see bug 423628 and 423625.
			for (Enumeration<String> enumeration = dics[i].keys(); enumeration.hasMoreElements();) {
				String key = enumeration.nextElement();
				String value = (String) dics[i].get(key);
				sb.append(" (" + key + "," + value + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		sb.append("\n"); //$NON-NLS-1$
		return sb.toString();
	}

	public void uninstallBundle(BundleInfo bInfo) throws FrameworkAdminRuntimeException {
		SimpleBundlesState.checkAvailability(fwAdmin);
		long id = DEFAULT_TIMESTAMP;
		URI realLocation = bInfo.getLocation();
		BundleDescription bundle = getBundleByLocation(bInfo.getLocation());
		if (bundle != null)
			id = bundle.getBundleId();

		if (id != DEFAULT_TIMESTAMP) {
			try {
				Dictionary<String, String> manifest = Utils.getOSGiManifest(bInfo.getLocation());
				if (manifest == null) {
					Log.log(LogService.LOG_WARNING, this, "uninstallBundle(BundleInfo)", NLS.bind(Messages.exception_bundleManifest, bInfo.getLocation())); //$NON-NLS-1$
					return;
				}
				BundleDescription bundleDescription = soFactory.createBundleDescription(state, manifest, realLocation.toString(), id);
				removeBundleFromState(bundleDescription);
				manipulator.getConfigData().removeBundle(bInfo);
			} catch (BundleException e) {
				Log.log(LogService.LOG_WARNING, this, "uninstallBundle(BundleInfo)", e); //$NON-NLS-1$
			}
		}
	}

	private BundleDescription getBundleByLocation(URI location) {
		if (location == null)
			return null;
		return locationStateIndex.get(location);
	}

	private BundleDescription getBundleByNameVersion(String bundleSymbolicName, String bundleVersion) {
		return nameVersionStateIndex.get(bundleSymbolicName + ";" + bundleVersion); //$NON-NLS-1$
	}

	/**
	 * Returns a key for a bundle description containing the bundle name and version,
	 * for use in the name/version state index map.
	 */
	private String getKey(BundleDescription bundle) {
		return bundle.getSymbolicName() + ';' + bundle.getVersion();
	}

	private void createStateIndexes() {
		BundleDescription[] currentInstalledBundles = state.getBundles();
		for (int i = 0; i < currentInstalledBundles.length; i++) {
			URI location = FileUtils.getRealLocation(manipulator, currentInstalledBundles[i].getLocation());
			locationStateIndex.put(location, currentInstalledBundles[i]);
			nameVersionStateIndex.put(getKey(currentInstalledBundles[i]), currentInstalledBundles[i]);
		}
	}

	private void addBundleToState(BundleDescription bundleDescription) {
		state.addBundle(bundleDescription);
		URI location = FileUtils.getRealLocation(manipulator, bundleDescription.getLocation());
		locationStateIndex.put(location, bundleDescription);
		nameVersionStateIndex.put(getKey(bundleDescription), bundleDescription);
	}

	private void removeBundleFromState(BundleDescription bundleDescription) {
		URI location = FileUtils.getRealLocation(manipulator, bundleDescription.getLocation());
		locationStateIndex.remove(location);
		nameVersionStateIndex.remove(getKey(bundleDescription));
		state.removeBundle(bundleDescription);
	}
}
