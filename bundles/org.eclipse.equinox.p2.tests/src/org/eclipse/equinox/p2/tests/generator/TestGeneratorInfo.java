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
package org.eclipse.equinox.p2.tests.generator;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.IGeneratorInfo;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
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
	private URI siteLocation;
	private boolean updateCompatibilty = false;
	private IProvisioningAgent agent;

	public TestGeneratorInfo(IProvisioningAgent agent, File baseLocation) {
		this.baseLocation = baseLocation;
		this.agent = agent;
	}

	public boolean addDefaultIUs() {
		return false;
	}

	public boolean append() {
		return false;
	}

	public IArtifactRepository getArtifactRepository() {
		if (artifactRepo == null)
			artifactRepo = new TestArtifactRepository(agent);
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
			metadataRepo = new TestMetadataRepository(agent, new IInstallableUnit[0]);
		return metadataRepo;
	}

	public String getRootId() {
		return rootId;
	}

	public String getRootVersion() {
		return rootVersion;
	}

	public URI getSiteLocation() {
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

	public void setSiteLocation(URI location) {
		this.siteLocation = location;
	}

	public void setLauncherConfig(String launcherConfig) {
		this.launcherConfig = launcherConfig;
	}

	public boolean getIsUpdateCompatible() {
		return this.updateCompatibilty;
	}

	public void setIsUpdateCompatible(boolean isCompatible) {
		this.updateCompatibilty = isCompatible;
	}

	public boolean reuseExistingPack200Files() {
		return false;
	}

	public void reuseExistingPack200Files(boolean publishPack) {
		//
	}

	public String getProductFile() {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection getOtherIUs() {
		return Collections.EMPTY_LIST;
	}

	public String getVersionAdvice() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setVersionAdvice(String advice) {
		// TODO Auto-generated method stub
	}

}
