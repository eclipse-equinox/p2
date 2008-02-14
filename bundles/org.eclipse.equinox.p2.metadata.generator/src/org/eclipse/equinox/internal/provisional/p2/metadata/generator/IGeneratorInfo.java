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
package org.eclipse.equinox.internal.provisional.p2.metadata.generator;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;

public interface IGeneratorInfo {
	public boolean addDefaultIUs();

	public boolean append();

	public IArtifactRepository getArtifactRepository();

	public File[] getBundleLocations();

	public ConfigData getConfigData();

	public File getConfigurationLocation();

	public ArrayList getDefaultIUs(Set ius);

	public File getExecutableLocation();

	public File getFeaturesLocation();

	public String getFlavor();

	public boolean getIsUpdateCompatible();

	public File getJRELocation();

	/**
	 * The platform for the data this location
	 * @return Returns a pde.build style platform config in the form os_ws_arch
	 */
	public String getLauncherConfig();

	public LauncherData getLauncherData();

	public String[][] getMappingRules();

	public IMetadataRepository getMetadataRepository();

	public String getRootId();

	public String getRootVersion();

	public String getProductFile();

	/**
	 * Returns the location of the site.xml file, or <code>null</code> if not
	 * generating for an update site.
	 * @return The location of site.xml, or <code>null</code>
	 */
	public URL getSiteLocation();

	public boolean publishArtifactRepository();

	public boolean publishArtifacts();

	public boolean reuseExistingPack200Files();

	public void reuseExistingPack200Files(boolean publishPack);

	public void setArtifactRepository(IArtifactRepository value);

	public void setFlavor(String value);

	public void setIsUpdateCompatible(boolean isCompatible);

	public void setMetadataRepository(IMetadataRepository value);

	public void setPublishArtifacts(boolean value);

	public void setRootId(String value);

}
