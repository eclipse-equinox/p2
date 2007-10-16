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
package org.eclipse.equinox.p2.metadata.generator;

import java.io.*;
import java.util.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.generator.Activator;
import org.eclipse.equinox.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;

public class MetadataGeneratorHelper {
	private static final String ECLIPSE_EXTENSIBLE_API = "Eclipse-ExtensibleAPI"; //$NON-NLS-1$

	private static final String CAPABILITY_TYPE_OSGI_PACKAGES = "osgi.packages"; //$NON-NLS-1$

	private static final Version versionMax = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

	private static final String ECLIPSE_TOUCHPOINT = "eclipse"; //$NON-NLS-1$
	private static final Version ECLIPSE_TOUCHPOINT_VERSION = new Version(1, 0, 0);

	private static final String NATIVE_TOUCHPOINT = "native"; //$NON-NLS-1$
	private static final Version NATIVE_TOUCHPOINT_VERSION = new Version(1, 0, 0);

	private static final String ECLIPSE_ARTIFACT_NAMESPACE = "eclipse"; //$NON-NLS-1$
	private static final String ECLIPSE_ARTIFACT_CLASSIFIER = "plugin"; //$NON-NLS-1$

	private static final String LAUNCHER_ID_PREFIX = "org.eclipse.launcher"; //$NON-NLS-1$

	//TODO - need to come up with a way to infer launcher version
	private static final Version LAUNCHER_VERSION = new Version(1, 0, 0);
	private static final String IU_NAMESPACE = IInstallableUnit.IU_NAMESPACE;

	private static final String[] BUNDLE_IU_PROPERTY_MAP = {Constants.BUNDLE_NAME, IInstallableUnitConstants.NAME, Constants.BUNDLE_DESCRIPTION, IInstallableUnitConstants.DESCRIPTION, Constants.BUNDLE_VENDOR, IInstallableUnitConstants.PROVIDER, Constants.BUNDLE_CONTACTADDRESS, IInstallableUnitConstants.CONTACT, Constants.BUNDLE_COPYRIGHT, IInstallableUnitConstants.COPYRIGHT, Constants.BUNDLE_DOCURL, IInstallableUnitConstants.DOC_URL, Constants.BUNDLE_UPDATELOCATION, IInstallableUnitConstants.UPDATE_SITE};

	private static final Version DEFAULT_JRE_VERSION = new Version("1.5"); //$NON-NLS-1$

	/**
	 * Creates IUs and artifact descriptors for the JRE, and adds them to the given sets.
	 * if the jreLocation is <code>null</code>, default information is generated.
	 */
	public static void createJREData(File jreLocation, Set resultantIUs, Set resultantArtifactDescriptors) {
		InstallableUnit iu = new InstallableUnit();
		iu.setSingleton(false);
		iu.setId("a.jre"); //$NON-NLS-1$
		iu.setTouchpointType(new TouchpointType(NATIVE_TOUCHPOINT, NATIVE_TOUCHPOINT_VERSION));

		InstallableUnitFragment cu = new InstallableUnitFragment();
		cu.setId("config." + iu.getId()); //$NON-NLS-1$
		cu.setVersion(iu.getVersion());
		cu.setHost(iu.getId(), new VersionRange(iu.getVersion(), true, versionMax, true));
		cu.setTouchpointType(new TouchpointType(NATIVE_TOUCHPOINT, NATIVE_TOUCHPOINT_VERSION));
		Map touchpointData = new HashMap();

		if (jreLocation == null || !jreLocation.exists()) {
			//set some reasonable defaults
			iu.setVersion(DEFAULT_JRE_VERSION);
			iu.setCapabilities(generateJRECapability(null));
			resultantIUs.add(iu);

			touchpointData.put("install", "");
			cu.setImmutableTouchpointData(new TouchpointData(touchpointData));
			resultantIUs.add(cu);
			return;
		}
		generateJREIUData(iu, jreLocation);

		//Generate artifact for JRE
		IArtifactKey key = new ArtifactKey(ECLIPSE_ARTIFACT_NAMESPACE, NATIVE_TOUCHPOINT, iu.getId(), iu.getVersion());
		iu.setArtifacts(new IArtifactKey[] {key});
		resultantIUs.add(iu);

		//Create config info for the CU
		String configurationData = "unzip(source:@artifact, target:${installFolder});";
		touchpointData.put("install", configurationData);
		cu.setImmutableTouchpointData(new TouchpointData(touchpointData));
		resultantIUs.add(cu);

		//Create the artifact descriptor
		IArtifactDescriptor descriptor = createArtifactDescriptor(key, jreLocation, false, true);
		resultantArtifactDescriptors.add(descriptor);
	}

	private static void generateJREIUData(InstallableUnit iu, File jreLocation) {
		//Look for a JRE profile file to set version and capabilities
		File[] profiles = jreLocation.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getAbsolutePath().endsWith(".profile"); //$NON-NLS-1$
			}
		});
		if (profiles.length != 1) {
			iu.setVersion(DEFAULT_JRE_VERSION);
			iu.setCapabilities(generateJRECapability(null));
			return;
		}
		String profileName = profiles[0].getAbsolutePath().substring(profiles[0].getAbsolutePath().lastIndexOf('/'));
		Version version = DEFAULT_JRE_VERSION;
		//TODO Find a better way to determine JRE version
		if (profileName.indexOf("1.5") > 0) { //$NON-NLS-1$
			version = new Version("1.5"); //$NON-NLS-1$
		} else if (profileName.indexOf("1.4") > 0) { //$NON-NLS-1$
			version = new Version("1.4"); //$NON-NLS-1$
		}
		iu.setVersion(version);
		try {
			iu.setCapabilities(generateJRECapability(new FileInputStream(profiles[0])));
		} catch (FileNotFoundException e) {
			//Shouldn't happen, but ignore and fall through to use default
		}
	}

	/**
	 * Creates IUs and artifacts for the Launcher executable, and adds them to the given
	 * sets.
	 */
	public static void createLauncherData(File launcher, String configurationFlavor, Set resultantIUs, Set resultantArtifactDescriptors) {
		if (launcher == null || !launcher.exists())
			return;

		//Create the IU
		InstallableUnit iu = new InstallableUnit();
		iu.setSingleton(true);
		String launcherId = LAUNCHER_ID_PREFIX + '_' + launcher.getName();
		iu.setId(launcherId);
		iu.setVersion(LAUNCHER_VERSION);

		IArtifactKey key = new ArtifactKey(ECLIPSE_ARTIFACT_NAMESPACE, NATIVE_TOUCHPOINT, launcherId, LAUNCHER_VERSION);
		iu.setArtifacts(new IArtifactKey[] {key});
		iu.setTouchpointType(new TouchpointType(NATIVE_TOUCHPOINT, new Version(1, 0, 0)));
		resultantIUs.add(iu);

		//Create the CU
		InstallableUnitFragment cu = new InstallableUnitFragment();
		cu.setId(configurationFlavor + iu.getId());
		cu.setVersion(iu.getVersion());
		cu.setHost(iu.getId(), new VersionRange(iu.getVersion(), true, versionMax, true));

		cu.setTouchpointType(new TouchpointType(NATIVE_TOUCHPOINT, NATIVE_TOUCHPOINT_VERSION));
		Map touchpointData = new HashMap();
		String configurationData = "unzip(source:@artifact, target:${installFolder});";
		EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
		if (!info.getOS().equals(org.eclipse.osgi.service.environment.Constants.OS_WIN32))
			// FIXME:  is this correct?  do all non-Windows platforms need execute permissions on the launcher?
			configurationData += " chmod(targetDir:${installFolder}, targetFile:" + launcher.getName() + ", permissions:755);";
		touchpointData.put("install", configurationData);
		cu.setImmutableTouchpointData(new TouchpointData(touchpointData));
		resultantIUs.add(cu);

		//Create the artifact descriptor
		IArtifactDescriptor descriptor = createArtifactDescriptor(new ArtifactKey(ECLIPSE_ARTIFACT_NAMESPACE, NATIVE_TOUCHPOINT, launcherId, LAUNCHER_VERSION), launcher, false, true);
		resultantArtifactDescriptors.add(descriptor);
	}

	private static ProvidedCapability[] generateJRECapability(InputStream profileStream) {
		if (profileStream == null) {
			//use the 1.5 profile stored in the generator bundle
			try {
				profileStream = Activator.getContext().getBundle().getEntry("J2SE-1.5.profile").openStream();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		Properties p = new Properties();
		try {
			p.load(profileStream);
			ManifestElement[] jrePackages = ManifestElement.parseHeader("org.osgi.framework.system.packages", (String) p.get("org.osgi.framework.system.packages"));
			ProvidedCapability[] exportedPackageAsCapabilities = new ProvidedCapability[jrePackages.length];
			for (int i = 0; i < jrePackages.length; i++) {
				exportedPackageAsCapabilities[i] = new ProvidedCapability("osgi.packages", jrePackages[i].getValue(), null);
			}
			return exportedPackageAsCapabilities;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BundleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (profileStream != null) {
				try {
					profileStream.close();
				} catch (IOException e) {
					//ignore secondary failure
				}
			}
		}
		return new ProvidedCapability[0];
	}

	public static IInstallableUnit createEclipseConfigurationUnit(String iuId, Version iuVersion, boolean isBundleFragment, GeneratorBundleInfo configInfo, String configurationFlavor) {
		if (configInfo == null)
			return null;

		InstallableUnitFragment cu = new InstallableUnitFragment();
		cu.setId(configurationFlavor + iuId);
		cu.setVersion(iuVersion);

		//Indicate the IU to which this CU apply 
		cu.setHost(iuId, new VersionRange(iuVersion, true, versionMax, true));

		//Add a capability describing the flavor supported
		cu.setCapabilities(new ProvidedCapability[] {new ProvidedCapability(IInstallableUnit.FLAVOR_NAMESPACE, configurationFlavor, Version.emptyVersion)});

		cu.setTouchpointType(new TouchpointType(ECLIPSE_TOUCHPOINT, ECLIPSE_TOUCHPOINT_VERSION)); //TODO Is this necessary? I think we get that from the IU

		Map touchpointData = new HashMap();
		touchpointData.put("install", createConfigScript(configInfo, isBundleFragment));
		touchpointData.put("uninstall", createUnconfigScript(configInfo, isBundleFragment));
		cu.setImmutableTouchpointData(new TouchpointData(touchpointData));

		return cu;
	}

	public static IInstallableUnit createEclipseDefaultConfigurationUnit(GeneratorBundleInfo configInfo, GeneratorBundleInfo unconfigInfo, String configurationFlavor) {
		InstallableUnitFragment cu = new InstallableUnitFragment();
		cu.setId(configurationFlavor + "default");
		cu.setVersion(new Version(1, 0, 0));

		//Add a capability describing the flavor supported
		cu.setCapabilities(new ProvidedCapability[] {new ProvidedCapability(IInstallableUnit.FLAVOR_NAMESPACE, configurationFlavor, Version.emptyVersion)});

		//Create a capability on bundles
		RequiredCapability[] reqs = new RequiredCapability[] {new RequiredCapability(IInstallableUnit.CAPABILITY_ECLIPSE_TYPES, IInstallableUnit.CAPABILITY_ECLIPSE_BUNDLE, VersionRange.emptyRange, null, false, true)};
		cu.setRequiredCapabilities(reqs);
		cu.setTouchpointType(new TouchpointType(ECLIPSE_TOUCHPOINT, ECLIPSE_TOUCHPOINT_VERSION)); //TODO Is this necessary? I think we get that from the IU
		Map touchpointData = new HashMap();

		touchpointData.put("install", createDefaultConfigScript(configInfo));
		touchpointData.put("uninstall", createDefaultUnconfigScript(unconfigInfo));

		cu.setImmutableTouchpointData(new TouchpointData(touchpointData));
		return cu;
	}

	private static String createDefaultConfigScript(GeneratorBundleInfo configInfo) {
		return createConfigScript(configInfo, false);
	}

	private static String createDefaultUnconfigScript(GeneratorBundleInfo unconfigInfo) {
		return createUnconfigScript(unconfigInfo, false);
	}

	private static String createConfigScript(GeneratorBundleInfo configInfo, boolean isBundleFragment) {
		if (configInfo == null)
			return "";

		String configScript = "installBundle(bundle:${artifactId}";//$NON-NLS-1$
		if (!isBundleFragment && configInfo.getStartLevel() != BundleInfo.NO_LEVEL) {
			configScript += ", startLevel:" + configInfo.getStartLevel();
		}
		if (!isBundleFragment && configInfo.isMarkedAsStarted()) {
			configScript += ", markStarted: true";
		}
		configScript += ");";

		if (configInfo.getSpecialConfigCommands() != null) {
			configScript += configInfo.getSpecialConfigCommands();
		}

		return configScript;
	}

	private static String createUnconfigScript(GeneratorBundleInfo unconfigInfo, boolean isBundleFragment) {
		if (unconfigInfo == null)
			return "";
		String unconfigScript = "uninstallBundle(bundle:${artifactId}";//$NON-NLS-1$
		if (unconfigInfo != null) {
			if (unconfigInfo.getSpecialConfigCommands() != null) {
				unconfigScript += unconfigInfo.getSpecialConfigCommands();
			}
		}
		return unconfigScript;

	}

	private static boolean requireAFragment(BundleDescription bd, Map manifest) {
		if (manifest == null)
			return false;
		if (manifest.get(ECLIPSE_EXTENSIBLE_API) == null)
			return false;
		if (bd.getSymbolicName().equals("org.eclipse.osgi")) //Special case for OSGi
			return false;
		String classpath = (String) ((Map) bd.getUserObject()).get(Constants.BUNDLE_CLASSPATH);
		if (classpath == null)
			return true;
		ManifestElement[] classpathEntries;
		try {
			classpathEntries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, classpath);
			if (classpathEntries.length != 0 && classpathEntries[0].getValue().equals("."))
				return true;
		} catch (BundleException e) {
			//If we are here, it is that we have already parsed the bundle manifest and it contains no error 
		}
		return false;
	}

	public static IInstallableUnit createEclipseIU(BundleDescription bd, Map manifest, boolean isFolderPlugin, IArtifactKey key) {
		InstallableUnit iu = new InstallableUnit();
		iu.setSingleton(bd.isSingleton());
		iu.setId(bd.getSymbolicName());
		iu.setVersion(bd.getVersion());
		iu.setFilter(bd.getPlatformFilter());
		iu.setProperty(IInstallableUnitConstants.UPDATE_FROM, bd.getSymbolicName());
		iu.setProperty(IInstallableUnitConstants.UPDATE_RANGE, VersionRange.emptyRange.toString());

		boolean isFragment = bd.getHost() != null;
		boolean requiresAFragment = isFragment ? false : requireAFragment(bd, manifest);

		//Process the required bundles
		BundleSpecification requiredBundles[] = bd.getRequiredBundles();
		ArrayList reqsDeps = new ArrayList();
		if (requiresAFragment)
			reqsDeps.add(new RequiredCapability("fragment", iu.getId(), VersionRange.emptyRange, null, false, false));
		if (isFragment)
			reqsDeps.add(RequiredCapability.createRequiredCapabilityForName(bd.getHost().getName(), bd.getHost().getVersionRange(), false));
		for (int j = 0; j < requiredBundles.length; j++)
			reqsDeps.add(RequiredCapability.createRequiredCapabilityForName(requiredBundles[j].getName(), requiredBundles[j].getVersionRange() == VersionRange.emptyRange ? null : requiredBundles[j].getVersionRange(), requiredBundles[j].isOptional()));

		//Process the import package
		ImportPackageSpecification osgiImports[] = bd.getImportPackages();
		for (int i = 0; i < osgiImports.length; i++) {
			// TODO we need to sort out how we want to handle wild-carded dynamic imports - for now we ignore them
			ImportPackageSpecification importSpec = osgiImports[i];
			String importPackageName = importSpec.getName();
			if (importPackageName.indexOf('*') != -1)
				continue;

			VersionRange versionRange = importSpec.getVersionRange() == VersionRange.emptyRange ? null : importSpec.getVersionRange();

			//TODO this needs to be refined to take into account all the attribute handled by imports
			reqsDeps.add(new RequiredCapability(CAPABILITY_TYPE_OSGI_PACKAGES, importPackageName, versionRange, null, isOptional(importSpec), false));
		}
		iu.setRequiredCapabilities((RequiredCapability[]) reqsDeps.toArray(new RequiredCapability[reqsDeps.size()]));

		//Process the export package
		ExportPackageDescription exports[] = bd.getExportPackages();
		ProvidedCapability[] exportedPackageAsCapabilities = new ProvidedCapability[exports.length + 1 + (isFragment ? 1 : 0)];
		exportedPackageAsCapabilities[exports.length] = new ProvidedCapability(IInstallableUnit.CAPABILITY_ECLIPSE_TYPES, IInstallableUnit.CAPABILITY_ECLIPSE_BUNDLE, new Version(1, 0, 0)); //Here we add a bundle capability to identify bundles 
		for (int i = 0; i < exports.length; i++) {
			exportedPackageAsCapabilities[i] = new ProvidedCapability(CAPABILITY_TYPE_OSGI_PACKAGES, exports[i].getName(), exports[i].getVersion() == Version.emptyVersion ? null : exports[i].getVersion()); //TODO make sure that we support all the refinement on the exports
		}
		if (isFragment)
			exportedPackageAsCapabilities[exportedPackageAsCapabilities.length - 1] = new ProvidedCapability("fragment", bd.getHost().getName(), bd.getVersion());
		iu.setCapabilities(exportedPackageAsCapabilities);
		iu.setApplicabilityFilter("");

		iu.setArtifacts(new IArtifactKey[] {key});

		iu.setTouchpointType(new TouchpointType(ECLIPSE_TOUCHPOINT, ECLIPSE_TOUCHPOINT_VERSION));

		// Set IU properties from the manifest header attributes
		// TODO The values of the attributes may be localized. Metadata generation
		//      should construct property files for the IU based on the bundle/plug-in
		//		property files in whatever locales are provided.
		if (manifest != null) {
			int i = 0;
			while (i < BUNDLE_IU_PROPERTY_MAP.length) {
				if (manifest.containsKey(BUNDLE_IU_PROPERTY_MAP[i])) {
					String value = (String) manifest.get(BUNDLE_IU_PROPERTY_MAP[i]);
					if (value != null) {
						iu.setProperty(BUNDLE_IU_PROPERTY_MAP[i + 1], value);
					}
				}
				i += 2;
			}
		}

		//Define the immutable metadata for this IU. In this case immutable means that this is something that will not impact the configuration
		Map touchpointData = new HashMap();
		if (isFolderPlugin)
			touchpointData.put("zipped", "true");
		touchpointData.put("manifest", toManifestString(manifest));
		iu.setImmutableTouchpointData(new TouchpointData(touchpointData));
		return iu;
	}

	public static VersionRange getVersionRange(FeatureEntry entry) {
		String versionSpec = entry.getVersion();
		if (versionSpec == null)
			// TODO should really be returning VersionRange.emptyRange here...
			return null;
		Version version = new Version(versionSpec);
		if (!entry.isRequires())
			return new VersionRange(version, true, version, true);
		String match = entry.getMatch();
		if (match == null)
			// TODO should really be returning VersionRange.emptyRange here...
			return null;
		if (match.equals("perfect"))
			return new VersionRange(version, true, version, true);
		if (match.equals("equivalent")) {
			Version upper = new Version(version.getMajor(), version.getMinor() + 1, 0);
			return new VersionRange(version, true, upper, false);
		}
		if (match.equals("compatible")) {
			Version upper = new Version(version.getMajor() + 1, 0, 0);
			return new VersionRange(version, true, upper, false);
		}
		if (match.equals("greaterOrEqual"))
			return new VersionRange(version, true, new VersionRange(null).getMaximum(), true);
		return null;
	}

	private static String getTransformedId(String original, boolean isPlugin) {
		return isPlugin ? original : original + ".featureIU";
	}

	public static IInstallableUnit createGroupIU(Feature feature) {
		InstallableUnit iu = new InstallableUnit();
		iu.setId(getTransformedId(feature.getId(), false));
		iu.setVersion(new Version(feature.getVersion()));
		iu.setProperty(IInstallableUnitConstants.UPDATE_FROM, iu.getId());
		iu.setProperty(IInstallableUnitConstants.UPDATE_RANGE, VersionRange.emptyRange.toString());

		FeatureEntry entries[] = feature.getEntries();
		RequiredCapability[] required = new RequiredCapability[entries.length];
		for (int i = 0; i < entries.length; i++) {
			VersionRange range = getVersionRange(entries[i]);
			required[i] = new RequiredCapability(IU_NAMESPACE, getTransformedId(entries[i].getId(), entries[i].isPlugin()), range, getFilter(entries[i]), entries[i].isOptional(), false);
		}
		iu.setRequiredCapabilities(required);
		iu.setTouchpointType(TouchpointType.NONE);
		ProvidedCapability groupCapability = new ProvidedCapability(IInstallableUnit.IU_KIND_NAMESPACE, "group", new Version("1.0.0"));
		iu.setCapabilities(new ProvidedCapability[] {groupCapability});
		return iu;
	}

	public static String getFilter(FeatureEntry entry) {
		StringBuffer result = new StringBuffer();
		result.append("(&"); //$NON-NLS-1$
		if (entry.getFilter() != null)
			result.append(entry.getFilter());
		if (entry.getOS() != null)
			result.append("(osgi.os=" + entry.getOS() + ')');//$NON-NLS-1$
		if (entry.getWS() != null)
			result.append("(osgi.ws=" + entry.getWS() + ')');//$NON-NLS-1$
		if (entry.getArch() != null)
			result.append("(osgi.arch=" + entry.getArch() + ')');//$NON-NLS-1$
		if (entry.getNL() != null)
			result.append("(osgi.nl=" + entry.getNL() + ')');//$NON-NLS-1$
		if (result.length() == 2)
			return null;
		result.append(')');
		return result.toString();
	}

	private static boolean isOptional(ImportPackageSpecification importedPackage) {
		if (importedPackage.getDirective(Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_DYNAMIC) || importedPackage.getDirective(Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_OPTIONAL))
			return true;
		return false;
	}

	private static String toManifestString(Map p) {
		if (p == null)
			return null;
		Collection properties = p.entrySet();
		StringBuffer result = new StringBuffer();
		for (Iterator iterator = properties.iterator(); iterator.hasNext();) {
			Map.Entry aProperty = (Map.Entry) iterator.next();
			result.append(aProperty.getKey()).append(": ").append(aProperty.getValue()).append('\n');
		}
		return result.toString();
	}

	public static IArtifactKey createEclipseArtifactKey(String bsn, String version) {
		return new ArtifactKey(ECLIPSE_ARTIFACT_NAMESPACE, ECLIPSE_ARTIFACT_CLASSIFIER, bsn, new Version(version));
	}

	public static IArtifactDescriptor createArtifactDescriptor(IArtifactKey key, File pathOnDisk, boolean asIs, boolean recurse) {
		//TODO this size calculation is bogus
		ArtifactDescriptor result = new ArtifactDescriptor(key);
		if (pathOnDisk != null)
			result.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, Long.toString(pathOnDisk.length()));
		return result;
	}
}
