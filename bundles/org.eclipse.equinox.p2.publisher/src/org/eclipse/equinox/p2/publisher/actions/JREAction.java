/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 and others. All rights reserved. This
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
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.internal.p2.publisher.Messages;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;

public class JREAction extends AbstractPublisherAction {
	private static final String DEFAULT_JRE_NAME = "a.jre"; //$NON-NLS-1$
	private static final Version DEFAULT_JRE_VERSION = Version.parseVersion("1.6"); //$NON-NLS-1$
	private static final String DEFAULT_PROFILE = "/profiles/JavaSE-1.6.profile"; //$NON-NLS-1$
	private static final String PROFILE_LOCATION = "jre.action.profile.location"; //$NON-NLS-1$
	private static final String PROFILE_NAME = "osgi.java.profile.name"; //$NON-NLS-1$
	private static final String PROFILE_TARGET_VERSION = "org.eclipse.jdt.core.compiler.codegen.targetPlatform"; //$NON-NLS-1$
	private static final String PROFILE_SYSTEM_PACKAGES = "org.osgi.framework.system.packages"; //$NON-NLS-1$

	private File jreLocation;
	private String environment;
	private Map<String, String> profileProperties;

	public JREAction(File location) {
		this.jreLocation = location;
	}

	public JREAction(String environment) {

		this.environment = environment;
	}

	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		initialize(publisherInfo);
		IArtifactDescriptor artifact = createJREData(results);
		if (artifact != null)
			publishArtifact(artifact, new File[] {jreLocation}, null, publisherInfo, createRootPrefixComputer(jreLocation));
		return Status.OK_STATUS;
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

	private IProvidedCapability[] generateJRECapability(String id, Version version) {
		if (profileProperties == null)
			return new IProvidedCapability[0];

		String packages = profileProperties.get(PROFILE_SYSTEM_PACKAGES);
		if (packages == null || (packages.trim().length() == 0))
			return new IProvidedCapability[] {PublisherHelper.createSelfCapability(id, version)};

		try {
			ManifestElement[] jrePackages = ManifestElement.parseHeader(PROFILE_SYSTEM_PACKAGES, packages);
			IProvidedCapability[] exportedPackageAsCapabilities = new IProvidedCapability[jrePackages.length + 1];
			exportedPackageAsCapabilities[0] = PublisherHelper.createSelfCapability(id, version);
			for (int i = 1; i <= jrePackages.length; i++) {
				String packageName = jrePackages[i - 1].getValue();
				Version packageVersion = Version.create(jrePackages[i - 1].getAttribute("version")); //$NON-NLS-1$
				exportedPackageAsCapabilities[i] = MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, packageName, packageVersion);
			}
			return exportedPackageAsCapabilities;
		} catch (BundleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new IProvidedCapability[0];
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

		IProvidedCapability[] capabilities = generateJRECapability(iu.getId(), iu.getVersion());
		iu.setCapabilities(capabilities);
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
			String entry = environment != null ? "/profiles/" + environment.replace('/', '_') + ".profile" : DEFAULT_PROFILE; //$NON-NLS-1$ //$NON-NLS-2$
			URL profileURL = Activator.getContext().getBundle().getEntry(entry);
			profileProperties = loadProfile(profileURL);
		}
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
