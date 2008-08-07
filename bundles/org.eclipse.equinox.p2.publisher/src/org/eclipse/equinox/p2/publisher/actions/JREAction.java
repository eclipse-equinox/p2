/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

public class JREAction extends AbstractPublisherAction {

	private static final Version DEFAULT_JRE_VERSION = new Version("1.6"); //$NON-NLS-1$

	private File location;

	/**
	 * Creates IUs and artifact descriptors for the JRE.  The resulting IUs are added
	 * to the given set, and the resulting artifact descriptor, if any, is returned.
	 * If the jreLocation is <code>null</code>, default information is generated.
	 */
	public static IArtifactDescriptor createJREData(File jreLocation, IPublisherResult results) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(false);
		String id = "a.jre"; //$NON-NLS-1$
		Version version = DEFAULT_JRE_VERSION;
		iu.setId(id);
		iu.setVersion(version);
		iu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);

		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configId = "config." + id;//$NON-NLS-1$
		cu.setId(configId);
		cu.setVersion(version);
		cu.setHost(new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, id, new VersionRange(version, true, PublisherHelper.versionMax, true), null, false, false)});
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new ProvidedCapability[] {PublisherHelper.createSelfCapability(configId, version)});
		cu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		Map touchpointData = new HashMap();

		if (jreLocation == null || !jreLocation.exists()) {
			// set some reasonable defaults
			iu.setVersion(version);
			iu.setCapabilities(generateJRECapability(id, version, null));
			results.addIU(MetadataFactory.createInstallableUnit(iu), IPublisherResult.ROOT);

			touchpointData.put("install", ""); //$NON-NLS-1$ //$NON-NLS-2$
			cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
			results.addIU(MetadataFactory.createInstallableUnit(cu), IPublisherResult.ROOT);
			return null;
		}
		generateJREIUData(iu, id, version, jreLocation);

		//Generate artifact for JRE
		IArtifactKey key = new ArtifactKey(PublisherHelper.BINARY_ARTIFACT_CLASSIFIER, id, version);
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
		return PublisherHelper.createArtifactDescriptor(key, jreLocation);
	}

	private static ProvidedCapability[] generateJRECapability(String installableUnitId, Version installableUnitVersion, InputStream profileStream) {
		if (profileStream == null) {
			//use the 1.6 profile stored in the generator bundle
			try {
				profileStream = Activator.getContext().getBundle().getEntry("/profiles/JavaSE-1.6.profile").openStream(); //$NON-NLS-1$
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		Properties p = new Properties();
		try {
			p.load(profileStream);
			ManifestElement[] jrePackages = ManifestElement.parseHeader("org.osgi.framework.system.packages", (String) p.get("org.osgi.framework.system.packages")); //$NON-NLS-1$ //$NON-NLS-2$
			ProvidedCapability[] exportedPackageAsCapabilities = new ProvidedCapability[jrePackages.length + 1];
			exportedPackageAsCapabilities[0] = PublisherHelper.createSelfCapability(installableUnitId, installableUnitVersion);
			for (int i = 1; i <= jrePackages.length; i++) {
				exportedPackageAsCapabilities[i] = MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, jrePackages[i - 1].getValue(), null);
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

	private static void generateJREIUData(InstallableUnitDescription iu, String installableUnitId, Version installableUnitVersion, File jreLocation) {
		//Look for a JRE profile file to set version and capabilities
		File[] profiles = jreLocation.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getAbsolutePath().endsWith(".profile"); //$NON-NLS-1$
			}
		});
		if (profiles.length != 1) {
			iu.setVersion(DEFAULT_JRE_VERSION);
			iu.setCapabilities(generateJRECapability(installableUnitId, installableUnitVersion, null));
			return;
		}
		String profileName = profiles[0].getAbsolutePath().substring(profiles[0].getAbsolutePath().lastIndexOf('/'));
		Version version = DEFAULT_JRE_VERSION;
		//TODO Find a better way to determine JRE version
		if (profileName.indexOf("1.6") > 0) { //$NON-NLS-1$
			version = new Version("1.6"); //$NON-NLS-1$
		} else if (profileName.indexOf("1.5") > 0) { //$NON-NLS-1$
			version = new Version("1.5"); //$NON-NLS-1$
		} else if (profileName.indexOf("1.4") > 0) { //$NON-NLS-1$
			version = new Version("1.4"); //$NON-NLS-1$
		}
		iu.setVersion(version);
		try {
			iu.setCapabilities(generateJRECapability(installableUnitId, installableUnitVersion, new FileInputStream(profiles[0])));
		} catch (FileNotFoundException e) {
			//Shouldn't happen, but ignore and fall through to use default
		}
	}

	public JREAction(IPublisherInfo info, File location) {
		this.location = location;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		IArtifactDescriptor artifact = createJREData(location, results);
		if (artifact != null)
			publishArtifact(artifact, new File[] {location}, null, info, createRootPrefixComputer(location));
		return Status.OK_STATUS;
	}

}
