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
package org.eclipse.equinox.p2.tests.generator;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import org.eclipse.equinox.frameworkadmin.ConfigData;
import org.eclipse.equinox.frameworkadmin.LauncherData;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.generator.IGeneratorInfo;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.tests.TestArtifactRepository;
import org.eclipse.equinox.p2.tests.TestMetadataRepository;

/**
 * Simple implementation of IGeneratorInfo used for testing purposes.
 */
public class TestGeneratorInfo implements IGeneratorInfo {

	private IArtifactRepository artifactRepo;
	private File baseLocation;
	private String flavor;
	private IMetadataRepository metadataRepo;
	private String rootId;
	private String rootVersion;
	private String launcherConfig;
	private URL siteLocation;

	public TestGeneratorInfo(File baseLocation) {
		this.baseLocation = baseLocation;
	}

	public boolean addDefaultIUs() {
		return false;
	}

	public boolean append() {
		return false;
	}

	public IArtifactRepository getArtifactRepository() {
		if (artifactRepo == null)
			artifactRepo = new TestArtifactRepository();
		return artifactRepo;
	}

	public File[] getBundleLocations() {
		File[] children = new File(baseLocation, "plugins").listFiles();
		return children == null ? new File[0] : children;
	}

	public ConfigData getConfigData() {
		return null;
	}

	public File getConfigurationLocation() {
		return new File(baseLocation, "configuration");
	}

	public ArrayList getDefaultIUs(Set ius) {
		return null;
	}

	public File getExecutableLocation() {
		return null;
	}

	public File getFeaturesLocation() {
		return new File(baseLocation, "features");
	}

	public String getFlavor() {
		return flavor;
	}

	public File getJRELocation() {
		return null;
	}

	public LauncherData getLauncherData() {
		return null;
	}

	public String[][] getMappingRules() {
		return null;
	}

	public IMetadataRepository getMetadataRepository() {
		if (metadataRepo == null)
			metadataRepo = new TestMetadataRepository(new IInstallableUnit[0]);
		return metadataRepo;
	}

	public String getRootId() {
		return rootId;
	}

	public String getRootVersion() {
		return rootVersion;
	}

	public URL getSiteLocation() {
		return siteLocation;
	}

	public String getLauncherConfig() {
		return launcherConfig;
	}

	public boolean publishArtifactRepository() {
		return false;
	}

	public boolean publishArtifacts() {
		return false;
	}

	public void setArtifactRepository(IArtifactRepository value) {
		this.artifactRepo = value;
	}

	public void setFlavor(String value) {
		this.flavor = value;
	}

	public void setMetadataRepository(IMetadataRepository value) {
		this.metadataRepo = value;
	}

	public void setPublishArtifacts(boolean value) {
	}

	public void setRootId(String value) {
		this.rootId = value;
	}

	public void setSiteLocation(URL location) {
		this.siteLocation = location;
	}

	public void setLauncherConfig(String launcherConfig) {
		this.launcherConfig = launcherConfig;
	}

	public boolean reuseExistingPack200Files() {
		return false;
	}

	public void reuseExistingPack200Files(boolean publishPack) {
		//
	}

}
