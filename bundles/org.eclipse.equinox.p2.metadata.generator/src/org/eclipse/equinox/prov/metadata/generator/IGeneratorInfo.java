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
package org.eclipse.equinox.prov.metadata.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import org.eclipse.equinox.frameworkadmin.ConfigData;
import org.eclipse.equinox.frameworkadmin.LauncherData;
import org.eclipse.equinox.prov.artifact.repository.IWritableArtifactRepository;
import org.eclipse.equinox.prov.metadata.repository.IWritableMetadataRepository;

public interface IGeneratorInfo {
	public boolean addDefaultIUs();

	public boolean append();

	public IWritableArtifactRepository getArtifactRepository();

	public File[] getBundleLocations();

	public ConfigData getConfigData();

	public File getConfigurationLocation();

	public ArrayList getDefaultIUs(Set ius);

	public File getExecutableLocation();

	public File getFeaturesLocation();

	public String getFlavor();

	public LauncherData getLauncherData();

	public String[][] getMappingRules();

	public IWritableMetadataRepository getMetadataRepository();

	public String getRootId();

	public String getRootVersion();
	
	public boolean publishArtifactRepository();

	public boolean publishArtifacts();

	public void setArtifactRepository(IWritableArtifactRepository value);

	public void setFlavor(String value);

	public void setMetadataRepository(IWritableMetadataRepository value);

	public void setPublishArtifacts(boolean value);

	public void setRootId(String value);
}