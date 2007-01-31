/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.equinox.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

import org.eclipse.core.runtime.internal.adaptor.EclipseEnvironmentInfo;
import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.frameworkadmin.equinox.internal.utils.AlienStateReader;
import org.eclipse.equinox.frameworkadmin.equinox.internal.utils.BundleHelper;
import org.eclipse.equinox.internal.frameworkadmin.utils.SimpleBundlesState;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;

class EclipseVersion implements Comparable {
	int major = 0;
	int minor = 0;
	int service = 0;
	String qualifier = null;

	EclipseVersion(String version) {
		StringTokenizer tok = new StringTokenizer(version, ".");
		if (!tok.hasMoreTokens())
			return;
		this.major = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreTokens())
			return;
		this.minor = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreTokens())
			return;
		this.service = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreTokens())
			return;
		this.qualifier = tok.nextToken();
	}

	public int compareTo(Object obj) {
		if (obj instanceof EclipseVersion)
			return 0;
		EclipseVersion target = (EclipseVersion) obj;
		if (target.major > this.major)
			return -1;
		if (target.major < this.major)
			return 1;
		if (target.minor > this.minor)
			return -1;
		if (target.minor < this.minor)
			return 1;
		if (target.service > this.service)
			return -1;
		if (target.service < this.service)
			return 1;
		return 0;
	}

}

public class EquinoxBundlesState implements BundlesState {
	private static final boolean DEBUG = false;
	// While we recognize the amd64 architecture, we change
	// this internally to be x86_64.
	private static final String INTERNAL_AMD64 = "amd64"; //$NON-NLS-1$
	private static final String INTERNAL_ARCH_I386 = "i386"; //$NON-NLS-1$
	public static final String[] PROPS = {"osgi.os", "osgi.ws", "osgi.nl", "osgi.arch", Constants.FRAMEWORK_SYSTEMPACKAGES, "osgi.resolverMode", Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "osgi.resolveOptional", "osgi.genericAliases"};

	static boolean checkFullySupported() {
		try {
			BundleHelper.getDefault();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * eclipse.exe will launch a fw where plugins/org.eclipse.osgi_*.*.*.*.jar is an implementation of fw.
	 * 
	 * @param launcherData
	 * @return File of fwJar to be used.
	 * @throws IOException 
	 */
	static File getFwJar(LauncherData launcherData) {

		//		EclipseLauncherParser launcherParser = new EclipseLauncherParser(launcherData);
		//		launcherParser.read();
		if (launcherData.getFwJar() != null)
			return launcherData.getFwJar();

		// check -D arguments of jvmArgs ?
		String[] jvmArgs = launcherData.getJvmArgs();
		String location = null;
		for (int i = 0; i < jvmArgs.length; i++) {
			if (jvmArgs[i].endsWith("-D" + "osgi.framework=")) {
				location = jvmArgs[i].substring(("-D" + "osgi.framework=").length());
			}
		}
		if (location != null)
			return new File(location);
		File pluginsDir = new File(launcherData.getLauncher().getParentFile(), "plugins");
		File[] files = pluginsDir.listFiles();
		File ret = null;
		EclipseVersion maxVersion = null;
		for (int i = 0; i < files.length; i++)
			if (files[i].getName().startsWith("org.eclipse.osgi_")) {
				String version = files[i].getName().substring("org.eclipse.osgi_".length(), files[i].getName().lastIndexOf(".jar"));
				if (ret == null || ((new EclipseVersion(version)).compareTo(maxVersion) > 0)) {
					ret = files[i];
					maxVersion = new EclipseVersion(version);
					continue;
				}
			}
		return ret;
	}

	public static String getStateString(State state) {
		BundleDescription[] descriptions = state.getBundles();
		StringBuffer sb = new StringBuffer();
		sb.append("# state=\n");
		for (int i = 0; i < descriptions.length; i++)
			sb.append("# " + descriptions[i].toString() + "\n");
		return sb.toString();
	}

	EquinoxFwAdminImpl fwAdmin = null;

	BundleContext context;

	Manipulator manipulator = null;
	Properties properties = new Properties();

	long maxId = -1;

	StateObjectFactory soFactory = null;
	State state = null;

	/**
	 * This constructor will not take a framework persistent data into account.
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
		// copy manipulator object for avoiding modifying the parameters of the manipulator.
		this.manipulator = fwAdmin.getManipulator();
		this.manipulator.setConfigData(manipulator.getConfigData());
		this.manipulator.setLauncherData(manipulator.getLauncherData());
		LauncherData launcherData = manipulator.getLauncherData();
		ConfigData configData = manipulator.getConfigData();
		BundleInfo[] bInfos = configData.getBundles();
		this.composeCleanState(launcherData, configData, properties, bInfos);
	}

	/**
	 * If useFwPersistentData flag equals false,
	 * this constructor will not take a framework persistent data into account.	
	 * Otherwise, it will.
	 * 
	 * @param context
	 * @param fwAdmin
	 * @param manipulator
	 * @param useFwPersistentData
	 */
	EquinoxBundlesState(BundleContext context, EquinoxFwAdminImpl fwAdmin, Manipulator manipulator, boolean useFwPersistentData) {
		super();
		this.context = context;
		this.fwAdmin = fwAdmin;
		// copy manipulator object for avoiding modifying the parameters of the manipulator.
		this.manipulator = fwAdmin.getManipulator();
		this.manipulator.setConfigData(manipulator.getConfigData());
		this.manipulator.setLauncherData(manipulator.getLauncherData());
		initialize(useFwPersistentData);
	}

	//	EquinoxBundlesState(BundleContext context, EquinoxFwAdminImpl fwAdmin, Manipulator manipulator) {
	//		this(context, fwAdmin, manipulator, true);
	//		//		this.context = context;
	//		//		this.fwAdmin = fwAdmin;
	//		//		// copy manipulator object for avoiding modifying the parameters of the manipulator.
	//		//		this.manipulator = fwAdmin.getManipulator();
	//		//		this.manipulator.setConfigData(manipulator.getConfigData());
	//		//		this.manipulator.setLauncherData(manipulator.getLauncherData());
	//		//		initialize();
	//	}

	private void composeCleanState(LauncherData launcherData, ConfigData configData, BundleInfo[] bInfos) {
		this.composeCleanState(launcherData, configData, configData.getFwDependentProps(), bInfos);
	}

	private void composeCleanState(LauncherData launcherData, ConfigData configData, Properties properties, BundleInfo[] bInfos) {
		composeExpectedState(bInfos, properties, null);
		resolve(true);
		if (getSystemBundle() == null) {
			File fwJar = getFwJar(launcherData);;
			if (fwJar == null)
				throw new IllegalStateException("launcherData.getLauncherConfigFile() == null && fwJar is not set.");

			BundleInfo[] newBInfos = new BundleInfo[bInfos.length + 1];
			try {
				newBInfos[0] = new BundleInfo(fwJar.toURL().toExternalForm(), 0, true);
			} catch (MalformedURLException e) {
				// Nothign to do because never happens.
				e.printStackTrace();
			}
			System.arraycopy(bInfos, 0, newBInfos, 1, bInfos.length);
			configData.setBundles(newBInfos);
			composeExpectedState(bInfos, properties, null);
			resolve(true);
		}
	}

	private boolean composeExpectedState(BundleInfo[] bInfos, Dictionary props, File fwPersistentDataLocation) throws IllegalArgumentException, FrameworkAdminRuntimeException {
		SimpleBundlesState.checkAvailability(fwAdmin);
		this.setStateObjectFactory();
		BundleDescription[] cachedInstalledBundles = null;
		state = null;
		if (fwPersistentDataLocation != null) {
			AlienStateReader alienStateReader = new AlienStateReader(fwPersistentDataLocation, null);
			state = alienStateReader.getState();
			if (state != null) {
				cachedInstalledBundles = state.getBundles();
				getPlatformProperties(state);
			}
		}
		if (state == null) {
			state = soFactory.createState(true);
			cachedInstalledBundles = new BundleDescription[0];
			if (props == null)
				return false;
			this.setPlatformProperties(props);
			getPlatformProperties(state);
		}

		// remove initial bundle which were installed but not listed in fwConfigFileBInfos.
		//bundleList.addAll(Arrays.asList(cachedInstalledBundles));
		for (int i = 0; i < cachedInstalledBundles.length; i++) {
			if (cachedInstalledBundles[i].getLocation().startsWith("initial@")) {
				String location = cachedInstalledBundles[i].getLocation().substring("initial@".length());
				if (location.startsWith("reference:"))
					location = location.substring("reference:".length());
				boolean found = false;
				for (int j = 0; j < bInfos.length; j++) {
					if (location.equals(bInfos[j].getLocation())) {
						found = true;
						break;
					}
				}
				if (!found)
					state.removeBundle(cachedInstalledBundles[i].getBundleId());
			}
		}

		maxId = state.getHighestBundleId();

		for (int j = 0; j < bInfos.length; j++) {
			try {
				this.installBundle(bInfos[j]);
			} catch (RuntimeException e) {
				Log.log(LogService.LOG_ERROR, this, "composeExpectedState()", "BundleInfo:" + bInfos[j], e);
				e.printStackTrace();
				throw e;
			}
		}
		return true;
	}

	//	public void composeRuntimeState() throws FrameworkAdminRuntimeException {
	//		SimpleBundlesState.checkAvailability(fwAdmin);
	//		PlatformAdmin platformAdmin = (PlatformAdmin) BundleHelper.getDefault().acquireService(PlatformAdmin.class.getName());
	//		State currentState = platformAdmin.getState(false);
	//		state = this.soFactory.createState(currentState);
	//		state.setPlatformProperties(currentState.getPlatformProperties());
	//	}

	private void getPlatformProperties(State state) {
		Dictionary platformProperties = state.getPlatformProperties()[0];

		properties.clear();
		if (platformProperties != null) {
			for (Enumeration enumeration = platformProperties.elements(); enumeration.hasMoreElements();) {
				String key = (String) enumeration.nextElement();
				Object value = platformProperties.get(key);
				if (value != null)
					properties.setProperty(key, (String) value);
			}
		}
	}

	public BundleInfo convert(BundleDescription toConvert) {
		boolean markedAsStarted = false;
		int sl = BundleInfo.NO_LEVEL;
		boolean found = false;
		BundleInfo[] originalBInfos = manipulator.getConfigData().getBundles();
		for (int i = 0; i < originalBInfos.length; i++)
			if (originalBInfos[i].getLocation().equals(toConvert.getLocation())) {
				markedAsStarted = originalBInfos[i].isMarkedAsStarted();
				sl = originalBInfos[i].getStartLevel();
				found = true;
				break;
			}

		if (!found)
			throw new IllegalStateException("Unexpected error !");

		BundleInfo result = new BundleInfo();
		result.setSymbolicName(toConvert.getSymbolicName());
		result.setVersion(toConvert.getVersion().toString());
		result.setLocation(toConvert.getLocation());
		result.setResolved(toConvert.isResolved());
		result.setMarkedAsStarted(markedAsStarted);
		result.setStartLevel(sl);
		return result;
	}

	public BundleInfo[] convertState(BundleDescription[] bundles) {
		//		BundleDescription[] bundles = state.getBundles();

		BundleInfo[] result = new BundleInfo[bundles.length];
		for (int i = 0; i < bundles.length; i++)
			result[i] = convert(bundles[i]);
		return result;
	}

	public BundleInfo[] convertState(State state) {
		return convertState(state.getBundles());
	}

	public BundleInfo[] getExpectedState() throws FrameworkAdminRuntimeException {
		SimpleBundlesState.checkAvailability(fwAdmin);
		return convertState(state);
	}

	public BundleInfo[] getPrerequisteBundles(BundleInfo bInfo) {
		Set set = new HashSet();
		BundleDescription bundle = state.getBundleByLocation(bInfo.getLocation());
		ImportPackageSpecification[] imports = bundle.getImportPackages();
		for (int i = 0; i < imports.length; i++) {
			BaseDescription supplier = imports[i].getSupplier();
			set.add(supplier.getSupplier());
			//			System.out.println(supplier.getSupplier());
		}
		BundleDescription[] requires = bundle.getResolvedRequires();
		for (int i = 0; i < requires.length; i++)
			set.add(requires[i]);
		BundleDescription[] bundles = new BundleDescription[set.size()];
		set.toArray(bundles);
		return convertState(bundles);
	}

	public BundleInfo getSystemBundle() {
		BundleDescription bundle = this.getSystemBundleDescription();
		if (bundle == null)
			return null;
		return convert(bundle);
	}

	private BundleDescription getSystemBundleDescription() {
		BundleDescription bundle = state.getBundle(0);
		if (bundle.getHost() != null)// this is a fragment bundle.
			return null;
		bundle.getSymbolicName();
		if (bundle.getSymbolicName().equals(EquinoxConstants.FW_SYMBOLIC_NAME))
			return bundle;
		return null;
	}

	public BundleInfo[] getSystemFragmentedBundles() {
		BundleDescription bundle = this.getSystemBundleDescription();
		if (bundle == null)
			return null;
		return convertState(bundle.getFragments());
	}

	public String[] getUnsatisfiedConstraints(BundleInfo bInfo) {
		BundleDescription description = state.getBundleByLocation(bInfo.getLocation());
		PlatformAdmin platformAdmin = (PlatformAdmin) BundleHelper.getDefault().acquireService(PlatformAdmin.class.getName());
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
			composeCleanState(launcherData, configData, bInfos);
			return;
		}

		EquinoxManipulatorImpl.checkConsistencyOfFwConfigLocAndFwPersistentDataLoc(launcherData);
		if (launcherData.isClean()) {
			composeCleanState(launcherData, configData, bInfos);
		} else {
			if (manipulator.getLauncherData().getFwPersistentDataLocation() != null) {
				//	TODO default value should be set more precisely.
				File installArea = null;
				String installAreaSt = configData.getFwDependentProp(EquinoxConstants.PROP_INSTALL);
				if (installAreaSt == null) {
					if (manipulator.getLauncherData().getLauncher() == null) {
						// TODO implement
					} else {
						installArea = manipulator.getLauncherData().getLauncher().getParentFile();
					}
				} else
					installArea = new File(installAreaSt);
				File fwPersistentDataLocation = new File(installArea, "configuration");
				manipulator.getLauncherData().setFwPersistentDataLocation(fwPersistentDataLocation, false);
			}
			if (!composeExpectedState(bInfos, null, manipulator.getLauncherData().getFwPersistentDataLocation()))
				composeCleanState(launcherData, configData, bInfos);
			resolve(true);
		}

		//		state = null;
		//		soFactory = null;
		//		maxId = -1;
	}

	public void installBundle(BundleInfo bInfo) throws FrameworkAdminRuntimeException {
		SimpleBundlesState.checkAvailability(fwAdmin);
		boolean found = false;

		BundleDescription[] currentInstalledBundles = state.getBundles();
		String newLocation = bInfo.getLocation();
		Dictionary manifest = Utils.getOSGiManifest(newLocation);
		String newSymbolicName = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
		String newVersion = (String) manifest.get(Constants.BUNDLE_VERSION);
		//		if (DEBUG)
		//			System.out.println("> currentInstalledBundles.length=" + currentInstalledBundles.length);
		for (int i = 0; i < currentInstalledBundles.length; i++) {
			String location = currentInstalledBundles[i].getLocation();
			//			if (DEBUG)
			//				System.out.println("> currentInstalledBundles[" + i + "]=" + currentInstalledBundles[i]);
			// TODO Is handling "reference:" needed ?
			//if(location.startsWith("reference:"))
			//	location = location.substring("reference:".length());
			if (newLocation.equals(location)) {
				found = true;
				break;
			}
			String symbolicName = currentInstalledBundles[i].getSymbolicName();
			String version = currentInstalledBundles[i].getVersion().toString();
			if (newSymbolicName.equals(symbolicName) && newVersion.equals(version)) {
				found = true;
				break;
			}
		}
		if (!found) {
			BundleDescription newBundleDescription = null;
			try {
				newBundleDescription = soFactory.createBundleDescription(state, Utils.getOSGiManifest(newLocation), newLocation, ++maxId);
				//				System.out.println(">>" + newBundleDescription.getLocation());
				state.addBundle(newBundleDescription);
				manipulator.getConfigData().addBundle(bInfo);
				//				System.out.println(getStateString(state));
			} catch (BundleException e) {
				Log.log(LogService.LOG_WARNING, this, "installBundle(BundleInfo)", e);
			}
		}
	}

	public boolean isFullySupported() {
		return true;
	}

	public boolean isResolved() {
		return state.isResolved();
	}

	public boolean isResolved(BundleInfo bInfo) {
		BundleDescription description = state.getBundleByLocation(bInfo.getLocation());
		if (description == null)
			return false;
		return description.isResolved();
	}

	public void resolve(boolean increment) {
		state.resolve(increment);
	}

	// "osgi.os", "osgi.ws", "osgi.nl", "osgi.arch", Constants.FRAMEWORK_SYSTEMPACKAGES, "osgi.resolverMode", 
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
		platformProperties.setProperty("osgi.resolveOptional", Boolean.toString("true".equals(FrameworkProperties.getProperty("osgi.resolveOptional"))));
		return platformProperties;
	}

	/**
	 * set properties required to compose state object
	 * into platformProperties of this state.
	 * 
	 * @param props
	 */
	private void setPlatformProperties(Dictionary props) {
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
		BundleHelper helper = BundleHelper.getDefault();
		if (helper == null) {
			helper = new BundleHelper();
			try {
				helper.start(context);
			} catch (Exception e) {
				Log.log(LogService.LOG_WARNING, this, "setStateObjectFactory()", e);
			}
		}
		helper.acquireService(PlatformAdmin.class.getName());

		PlatformAdmin platformAdmin = (PlatformAdmin) BundleHelper.getDefault().acquireService(PlatformAdmin.class.getName());
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
		long id = -1;
		String targetLocation = bInfo.getLocation();
		BundleDescription[] currentInstalledBundles = state.getBundles();
		for (int i = 0; i < currentInstalledBundles.length; i++) {
			String location = currentInstalledBundles[i].getLocation();
			// TODO Is handling "reference:" needed ?
			//if(location.startsWith("reference:"))
			//	location = location.substring("reference:".length());
			if (targetLocation.equals(location)) {
				id = currentInstalledBundles[i].getBundleId();
				break;
			}
		}
		if (id != -1) {

			try {
				BundleDescription bundleDescription = soFactory.createBundleDescription(state, Utils.getOSGiManifest(bInfo.getLocation()), bInfo.getLocation(), id);
				state.removeBundle(bundleDescription);
				manipulator.getConfigData().removeBundle(bInfo);
			} catch (BundleException e) {
				Log.log(LogService.LOG_WARNING, this, "uninstallBundle(BundleInfo)", e);
				//throw new ManipulatorException("Fail to createBundleDescription of bInfo:" + bInfo.toString(), e, ManipulatorException.OTHERS);
			}

		}

	}

	Properties getProperties() {
		return properties;
	}

}
