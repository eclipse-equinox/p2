/*******************************************************************************
 * Copyright (c) 2008, 2012 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 *   SAP AG - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.internal.p2.publisher.Messages;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

public class JREAction extends AbstractPublisherAction {
	private static final String DEFAULT_JRE_NAME = "a.jre"; //$NON-NLS-1$
	private static final Version DEFAULT_JRE_VERSION = Version.parseVersion("1.6"); //$NON-NLS-1$
	private static final String DEFAULT_PROFILE = "JavaSE-1.6"; //$NON-NLS-1$
	private static final String PROFILE_LOCATION = "jre.action.profile.location"; //$NON-NLS-1$
	private static final String PROFILE_NAME = "osgi.java.profile.name"; //$NON-NLS-1$
	private static final String PROFILE_TARGET_VERSION = "org.eclipse.jdt.core.compiler.codegen.targetPlatform"; //$NON-NLS-1$
	private static final String PROFILE_SYSTEM_PACKAGES = "org.osgi.framework.system.packages"; //$NON-NLS-1$

	public static final String NAMESPACE_OSGI_EE = "osgi.ee"; //$NON-NLS-1$

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

	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
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
		cu.setHost(new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, null, false, false)});
		cu.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new IProvidedCapability[] {PublisherHelper.createSelfCapability(configId, iu.getVersion())});
		cu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		Map<String, String> touchpointData = new HashMap<String, String>();

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

		List<IProvidedCapability> result = new ArrayList<IProvidedCapability>();
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
				for (int i = 0; i < jrePackages.length; i++) {
					String packageName = jrePackages[i].getValue();
					Version packageVersion = Version.create(jrePackages[i].getAttribute("version")); //$NON-NLS-1$
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
		for (int capabilityIx = 0; capabilityIx < systemCapabilities.length; capabilityIx++) {
			ManifestElement systemCapability = systemCapabilities[capabilityIx];

			// this is general manifest syntax: a "manifest element" can have multiple "value components" -> all attributes apply to each value component (=namespace)
			String[] namespaces = systemCapability.getValueComponents();
			for (int namespaceIx = 0; namespaceIx < namespaces.length; namespaceIx++) {
				String namespace = namespaces[namespaceIx];

				if ("osgi.ee".equals(namespace)) { // this is the OSGi capability namespace "osgi.ee"  //$NON-NLS-1$
					parseEECapability(systemCapability, parsingStatus, parsingResult);

				} else {
					parsingStatus.add(newWarningStatus(NLS.bind(Messages.message_eeIgnoringNamespace, namespace)));
					continue;
				}
			}
		}
	}

	private static void parseEECapability(ManifestElement eeCapability, MultiStatus parsingStatus, List<IProvidedCapability> parsingResult) {
		String eeName = eeCapability.getAttribute("osgi.ee"); // this is an attribute required for capabilities in the "osgi.ee" namespace //$NON-NLS-1$
		if (eeName == null) {
			parsingStatus.add(newErrorStatus(NLS.bind(Messages.message_eeMissingNameAttribute, eeCapability), null));
			return;
		}

		String[] eeVersions = parseEECapabilityVersion(eeCapability, parsingStatus);
		if (eeVersions == null) {
			// status was already updated by parse method
			return;
		}

		for (int versionIx = 0; versionIx < eeVersions.length; versionIx++) {
			String rawVersion = eeVersions[versionIx];
			try {
				Version parsedVersion = Version.parseVersion(rawVersion);

				// complete record -> store
				parsingResult.add(MetadataFactory.createProvidedCapability(NAMESPACE_OSGI_EE, eeName, parsedVersion));

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

		String profileName = profileLocation != null ? new Path(profileLocation).lastSegment() : profileProperties.get(PROFILE_NAME);
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
		this.info = publisherInfo;

		if (jreLocation != null) {

			File javaProfile = null;

			if (jreLocation.isDirectory()) {
				//Look for a JRE profile file to set version and capabilities
				File[] profiles = jreLocation.listFiles(new FileFilter() {
					public boolean accept(File pathname) {
						return pathname.getAbsolutePath().endsWith(".profile"); //$NON-NLS-1$
					}
				});
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
