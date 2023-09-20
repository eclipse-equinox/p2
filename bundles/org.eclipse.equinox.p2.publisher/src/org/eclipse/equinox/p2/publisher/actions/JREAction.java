/*******************************************************************************
 * Copyright (c) 2008, 2021 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 *   SAP AG - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.internal.p2.publisher.Messages;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

public class JREAction extends AbstractPublisherAction {
	private static final String DEFAULT_JRE_NAME = "a.jre"; //$NON-NLS-1$
	private static final Version DEFAULT_JRE_VERSION = Version.parseVersion("17.0"); //$NON-NLS-1$
	private static final String DEFAULT_PROFILE = "JavaSE-17"; //$NON-NLS-1$
	private static final String PROFILE_LOCATION = "jre.action.profile.location"; //$NON-NLS-1$
	private static final String PROFILE_NAME = "osgi.java.profile.name"; //$NON-NLS-1$
	private static final String PROFILE_TARGET_VERSION = "org.eclipse.jdt.core.compiler.codegen.targetPlatform"; //$NON-NLS-1$
	private static final String PROFILE_SYSTEM_PACKAGES = "org.osgi.framework.system.packages"; //$NON-NLS-1$

	public static final String NAMESPACE_OSGI_EE = "osgi.ee"; //$NON-NLS-1$
	public static final String VERSION_OSGI_EE = "version"; //$NON-NLS-1$

	private File jreLocation;
	private String environment;
	private Map<String, String> profileProperties;
	private MultiStatus resultStatus;

	public JREAction(File location) {
		this.jreLocation = location;
	}

	public JREAction(String environment) {
		this.environment = environment;
	}

	@Override public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		String problemMessage = NLS.bind(Messages.message_problemsWhilePublishingEE, jreLocation != null ? jreLocation : environment);
		resultStatus = new MultiStatus(Activator.ID, 0, problemMessage, null);

		initialize(publisherInfo);
		IArtifactDescriptor artifact = createJREData(results);
		if (artifact != null)
			publishArtifact(artifact, new File[] {jreLocation}, null, publisherInfo, createRootPrefixComputer(jreLocation));

		if (resultStatus.isOK())
			return Status.OK_STATUS;
		return resultStatus;
	}

	private static Status newErrorStatus(String message, Exception exception) {
		return new Status(IStatus.ERROR, Activator.ID, message, exception);
	}

	private static Status newWarningStatus(String message) {
		return new Status(IStatus.WARNING, Activator.ID, message, null);
	}

	/**
	 * Creates IUs and artifact descriptors for the JRE.  The resulting IUs are added
	 * to the given set, and the resulting artifact descriptor, if any, is returned.
	 * If the jreLocation is <code>null</code>, default information is generated.
	 */
	protected IArtifactDescriptor createJREData(IPublisherResult results) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(false);
		iu.setId(DEFAULT_JRE_NAME);
		iu.setVersion(DEFAULT_JRE_VERSION);
		iu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);

		generateJREIUData(iu);

		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configId = "config." + iu.getId();//$NON-NLS-1$
		cu.setId(configId);
		cu.setVersion(iu.getVersion());
		VersionRange range = iu.getVersion() == Version.emptyVersion ? VersionRange.emptyRange : new VersionRange(iu.getVersion(), true, Version.MAX_VERSION, true);
		cu.setHost(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, null, false,
				false));
		cu.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new IProvidedCapability[] {PublisherHelper.createSelfCapability(configId, iu.getVersion())});
		cu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		Map<String, String> touchpointData = new HashMap<>();

		if (jreLocation == null || !jreLocation.isDirectory()) {
			touchpointData.put("install", ""); //$NON-NLS-1$ //$NON-NLS-2$
			cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
			results.addIU(MetadataFactory.createInstallableUnit(iu), IPublisherResult.ROOT);
			results.addIU(MetadataFactory.createInstallableUnit(cu), IPublisherResult.ROOT);
			return null;
		}

		//Generate artifact for JRE
		IArtifactKey key = new ArtifactKey(PublisherHelper.BINARY_ARTIFACT_CLASSIFIER, iu.getId(), iu.getVersion());
		iu.setArtifacts(new IArtifactKey[] {key});
		results.addIU(MetadataFactory.createInstallableUnit(iu), IPublisherResult.ROOT);

		//Create config info for the CU
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		results.addIU(MetadataFactory.createInstallableUnit(cu), IPublisherResult.ROOT);

		//Create the artifact descriptor
		return PublisherHelper.createArtifactDescriptor(info, key, jreLocation);
	}

	private List<IProvidedCapability> generateJRECapability(String id, Version version) {
		if (profileProperties == null)
			return Collections.emptyList();

		List<IProvidedCapability> result = new ArrayList<>();
		result.add(PublisherHelper.createSelfCapability(id, version));
		generateProvidedPackages(result);
		generateOsgiEESystemCapabilities(result);
		return result;
	}

	private void generateProvidedPackages(List<IProvidedCapability> result) {
		String packages = profileProperties.get(PROFILE_SYSTEM_PACKAGES);
		if (packages != null && (packages.trim().length() > 0)) {

			try {
				ManifestElement[] jrePackages = ManifestElement.parseHeader(PROFILE_SYSTEM_PACKAGES, packages);
				for (ManifestElement jrePackage : jrePackages) {
					String packageName = jrePackage.getValue();
					Version packageVersion = Version.create(jrePackage.getAttribute("version")); //$NON-NLS-1$
					result.add(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, packageName, packageVersion));
				}
			} catch (BundleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	void generateOsgiEESystemCapabilities(List<IProvidedCapability> result) {
		String message = NLS.bind(Messages.message_problemsWhileParsingProfileProperty, Constants.FRAMEWORK_SYSTEMCAPABILITIES);
		MultiStatus parsingStatus = new MultiStatus(Activator.ID, 0, message, null);

		String systemCapabilities = profileProperties.get(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
		parseSystemCapabilities(systemCapabilities, parsingStatus, result);

		// result contains the valid entries, parsingStatus the invalid entries
		if (!parsingStatus.isOK())
			resultStatus.add(parsingStatus);
	}

	static void parseSystemCapabilities(String systemCapabilities, MultiStatus parsingStatus, List<IProvidedCapability> parsingResult) {
		if (systemCapabilities == null || (systemCapabilities.trim().length() == 0)) {
			return;
		}

		try {
			ManifestElement[] eeEntries = ManifestElement.parseHeader(Constants.FRAMEWORK_SYSTEMCAPABILITIES, systemCapabilities);
			parseSystemCapabilities(eeEntries, parsingStatus, parsingResult);

		} catch (BundleException e) {
			parsingStatus.add(newErrorStatus(e.getLocalizedMessage(), e));
		}
	}

	private static void parseSystemCapabilities(ManifestElement[] systemCapabilities, MultiStatus parsingStatus, List<IProvidedCapability> parsingResult) {
		for (ManifestElement systemCapability : systemCapabilities) {
			// this is general manifest syntax: a "manifest element" can have multiple "value components" -> all attributes apply to each value component (=namespace)
			String[] namespaces = systemCapability.getValueComponents();
			for (String namespace : namespaces) {
				if (NAMESPACE_OSGI_EE.equals(namespace)) { // this is the OSGi capability namespace "osgi.ee"
					parseEECapability(systemCapability, parsingStatus, parsingResult);
				} else {
					parsingStatus.add(newWarningStatus(NLS.bind(Messages.message_eeIgnoringNamespace, namespace)));
				}
			}
		}
	}

	private static void parseEECapability(ManifestElement eeCapability, MultiStatus parsingStatus, List<IProvidedCapability> parsingResult) {
		String eeName = eeCapability.getAttribute(NAMESPACE_OSGI_EE); // this is an attribute required for capabilities in the "osgi.ee" namespace
		if (eeName == null) {
			parsingStatus.add(newErrorStatus(NLS.bind(Messages.message_eeMissingNameAttribute, eeCapability), null));
			return;
		}

		String[] eeVersions = parseEECapabilityVersion(eeCapability, parsingStatus);
		if (eeVersions == null) {
			// status was already updated by parse method
			return;
		}

		for (String rawVersion : eeVersions) {
			try {
				Version parsedVersion = Version.parseVersion(rawVersion);

				// complete record -> store
				Map<String, Object> capAttrs = new HashMap<>();
				capAttrs.put(NAMESPACE_OSGI_EE, eeName);
				capAttrs.put(VERSION_OSGI_EE, parsedVersion);

				parsingResult.add(MetadataFactory.createProvidedCapability(NAMESPACE_OSGI_EE, capAttrs));

			} catch (IllegalArgumentException e) {
				parsingStatus.add(newErrorStatus(NLS.bind(Messages.message_eeInvalidVersionAttribute, rawVersion), e));
			}
		}
	}

	private static String[] parseEECapabilityVersion(ManifestElement eeCapability, MultiStatus parsingStatus) {
		String singleVersion = eeCapability.getAttribute("version:Version"); //$NON-NLS-1$
		String[] multipleVersions = ManifestElement.getArrayFromList(eeCapability.getAttribute("version:List<Version>")); //$NON-NLS-1$

		if (singleVersion == null && multipleVersions == null) {
			parsingStatus.add(newErrorStatus(NLS.bind(Messages.message_eeMissingVersionAttribute, eeCapability), null));
			return null;

		} else if (singleVersion == null) {
			return multipleVersions;

		} else if (multipleVersions == null) {
			return new String[] {singleVersion};

		} else {
			parsingStatus.add(newErrorStatus(NLS.bind(Messages.message_eeDuplicateVersionAttribute, eeCapability), null));
			return null;
		}
	}

	private void generateJREIUData(InstallableUnitDescription iu) {
		if (profileProperties == null || profileProperties.size() == 0)
			return; //got nothing

		String profileLocation = profileProperties.get(PROFILE_LOCATION);

		String profileName = profileLocation != null ? IPath.fromOSString(profileLocation).lastSegment() : profileProperties.get(PROFILE_NAME);
		if (profileName.endsWith(".profile")) //$NON-NLS-1$
			profileName = profileName.substring(0, profileName.length() - 8);
		Version version = null;
		int idx = profileName.indexOf('-');
		if (idx != -1) {
			try {
				version = Version.parseVersion(profileName.substring(idx + 1));
			} catch (IllegalArgumentException e) {
				//ignore
			}
			profileName = profileName.substring(0, idx);
		}
		if (version == null) {
			try {
				String targetVersion = profileProperties.get(PROFILE_TARGET_VERSION);
				version = targetVersion != null ? Version.parseVersion(targetVersion) : null;
			} catch (IllegalArgumentException e) {
				//ignore
			}
		}

		if (version == null)
			version = DEFAULT_JRE_VERSION;

		iu.setVersion(version);

		profileName = profileName.replace('-', '.');
		profileName = profileName.replace('/', '.');
		profileName = profileName.replace('_', '.');
		iu.setId("a.jre." + profileName.toLowerCase()); //$NON-NLS-1$

		List<IProvidedCapability> capabilities = generateJRECapability(iu.getId(), iu.getVersion());
		iu.addProvidedCapabilities(capabilities);
	}

	private void initialize(IPublisherInfo publisherInfo) {
		File runtimeProfile = null;
		this.info = publisherInfo;
		if (jreLocation == null && environment == null) {
			// create a runtime profile
			StringBuilder buffer = createDefaultProfileFromRunningJvm();
			try {
				File tempDirectory = Files.createTempDirectory("JREAction").toFile(); //$NON-NLS-1$
				runtimeProfile = new File(tempDirectory, DEFAULT_PROFILE + ".profile"); //$NON-NLS-1$
				try (FileWriter writer = new FileWriter(runtimeProfile)) {
					writer.write(buffer.toString());
				}
				jreLocation = runtimeProfile;
			} catch (IOException e) {
				// ignore
			}
		}
		if (jreLocation != null) {

			File javaProfile = null;

			if (jreLocation.isDirectory()) {
				//Look for a JRE profile file to set version and capabilities
				File[] profiles = jreLocation.listFiles((FileFilter) pathname -> pathname.getAbsolutePath().endsWith(".profile")); //$NON-NLS-1$
				if (profiles != null && profiles.length > 0) {
					javaProfile = profiles[0];
				}
			} else if (jreLocation.isFile())
				javaProfile = jreLocation;
			else
				// jreLocation file does not exist
				throw new IllegalArgumentException(NLS.bind(Messages.exception_nonExistingJreLocationFile, jreLocation.getAbsolutePath()));

			profileProperties = loadProfile(javaProfile);
		}
		if (profileProperties == null) {
			String profileFile = (environment != null ? environment : DEFAULT_PROFILE).replace('/', '_') + ".profile"; //$NON-NLS-1$
			URL profileURL = getResouceFromSystemBundle(profileFile);
			profileProperties = loadProfile(profileURL);
		}
		if (runtimeProfile != null) {
			runtimeProfile.delete();
			runtimeProfile.getParentFile().delete();
		}
	}

	/*
	 * Copied from org.eclipse.osgi.storage.Storage.calculateVMPackages and adopted
	 * to Java 11
	 */
	private String calculateVMPackages() {
		try {
			List<String> packages = new ArrayList<>();
			ModuleLayer bootLayer = ModuleLayer.boot();
			Set<Module> bootModules = bootLayer.modules();
			for (Module m : bootModules) {
				ModuleDescriptor descriptor = m.getDescriptor();
				if (descriptor.isAutomatic()) {
					/*
					 * Automatic modules are supposed to export all their packages. However,
					 * java.lang.module.ModuleDescriptor::exports returns an empty set for them. Add
					 * all their packages (as returned by
					 * java.lang.module.ModuleDescriptor::packages) to the list of VM supplied
					 * packages.
					 */
					packages.addAll(descriptor.packages());
				} else {
					for (Exports export : descriptor.exports()) {
						String pkg = export.source();
						if (!export.isQualified()) {
							packages.add(pkg);
						}
					}
				}
			}
			Collections.sort(packages);
			StringBuilder result = new StringBuilder();
			for (String pkg : packages) {
				if (result.length() != 0) {
					result.append(',').append(' ');
				}
				result.append(pkg);
			}
			return result.toString();
		} catch (Exception e) {

			return null;
		}
	}

	/**
	 * Creates default profile with minumum version as stated by
	 * {@link #DEFAULT_PROFILE} and adds packages observed on currently used JVM.
	 *
	 * @return generated profile content
	 */
	@SuppressWarnings("nls")
	private StringBuilder createDefaultProfileFromRunningJvm() {
		StringBuilder buffer = new StringBuilder();
		final String NEWLINE = System.lineSeparator();
		// add systempackages
		buffer.append("org.osgi.framework.system.packages = \\");
		buffer.append(NEWLINE);
		buffer.append(' ');
		String calculateVMPackages = calculateVMPackages();
		if (calculateVMPackages != null) {
			String[] pack;
			pack = calculateVMPackages.split(",");
			for (int i = 0; i < pack.length; i++) {
				buffer.append(pack[i]);
				if (i != pack.length - 1) {
					buffer.append(',');
					buffer.append("\\");
				}
				buffer.append(NEWLINE);
			}
		}
		// add EE
		buffer.append("org.osgi.framework.executionenvironment = \\\n" + " OSGi/Minimum-1.0,\\\n"
				+ " OSGi/Minimum-1.1,\\\n" + " OSGi/Minimum-1.2,\\\n" + " JavaSE/compact1-1.8,\\\n"
				+ " JavaSE/compact2-1.8,\\\n" + " JavaSE/compact3-1.8,\\\n" + " JRE-1.1,\\\n" + " J2SE-1.2,\\\n"
				+ " J2SE-1.3,\\\n" + " J2SE-1.4,\\\n" + " J2SE-1.5,\\\n" + " JavaSE-1.6,\\\n" + " JavaSE-1.7,\\\n"
				+ " JavaSE-1.8,\\\n");
		String version = DEFAULT_PROFILE.substring(DEFAULT_PROFILE.indexOf('-') + 1);
		int ver = Integer.parseInt(version);
		for (int i = 9; i < ver; i++) {
			buffer.append(" JavaSE-" + String.valueOf(i) + ",\\\n");
		}
		buffer.append(" JavaSE-" + String.valueOf(ver));
		buffer.append(NEWLINE);

		// add capabilities
		StringBuilder versionList = new StringBuilder();
		versionList.append("1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8");
		for (int i = 9; i <= ver; i++) {
			versionList.append(", " + String.valueOf(i) + ".0");
		}
		buffer.append("org.osgi.framework.system.capabilities = \\\n"
				+ " osgi.ee; osgi.ee=\"OSGi/Minimum\"; version:List<Version>=\"1.0, 1.1, 1.2\",\\\n"
				+ " osgi.ee; osgi.ee=\"JRE\"; version:List<Version>=\"1.0, 1.1\",\\\n"
				+ " osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"" + versionList.toString() + "\",\\\n"
				+ " osgi.ee; osgi.ee=\"JavaSE/compact1\"; version:List<Version>=\"1.8," + String.valueOf(ver)
				+ ".0\",\\\n" + " osgi.ee; osgi.ee=\"JavaSE/compact2\"; version:List<Version>=\"1.8,"
				+ String.valueOf(ver) + ".0\",\\\n"
				+ " osgi.ee; osgi.ee=\"JavaSE/compact3\"; version:List<Version>=\"1.8," + String.valueOf(ver) + ".0\"");
		buffer.append(NEWLINE);

		// add profile name and compiler options
		buffer.append("osgi.java.profile.name = " + DEFAULT_PROFILE + "\n" + "org.eclipse.jdt.core.compiler.compliance="
				+ String.valueOf(ver) + "\n" + "org.eclipse.jdt.core.compiler.source=" + String.valueOf(ver) + "\n"
				+ "org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode=enabled\n"
				+ "org.eclipse.jdt.core.compiler.codegen.targetPlatform=" + String.valueOf(ver) + "\n"
				+ "org.eclipse.jdt.core.compiler.problem.assertIdentifier=error\n"
				+ "org.eclipse.jdt.core.compiler.problem.enumIdentifier=error");
		buffer.append(NEWLINE);
		return buffer;
	}

	private static URL getResouceFromSystemBundle(String entry) {
		// get resource from system bundle which is typically on the Java boot classpath
		ClassLoader loader = FrameworkUtil.class.getClassLoader();
		return loader == null ? ClassLoader.getSystemResource(entry) : loader.getResource(entry);
	}

	private Map<String, String> loadProfile(File profileFile) {
		if (profileFile == null || !profileFile.exists())
			return null;

		try {
			InputStream stream = new BufferedInputStream(new FileInputStream(profileFile));
			Map<String, String> properties = loadProfile(stream);
			if (properties != null)
				properties.put(PROFILE_LOCATION, profileFile.getAbsolutePath());
			return properties;
		} catch (FileNotFoundException e) {
			//null
		}
		return null;
	}

	private Map<String, String> loadProfile(URL profileURL) {
		if (profileURL == null)
			return null;

		try {
			InputStream stream = profileURL.openStream();
			return loadProfile(stream);
		} catch (IOException e) {
			//null
		}
		return null;
	}

	/**
	 * Always closes the stream when done
	 */
	private Map<String, String> loadProfile(InputStream stream) {
		if (stream != null) {
			try {
				return CollectionUtils.loadProperties(stream);
			} catch (IOException e) {
				return null;
			} finally {
				try {
					stream.close();
				} catch (IOException e) {
					// error
				}
			}
		}
		return null;
	}
}
