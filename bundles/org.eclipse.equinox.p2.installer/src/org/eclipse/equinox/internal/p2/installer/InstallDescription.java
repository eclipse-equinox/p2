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
package org.eclipse.equinox.internal.p2.installer;

import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.p2.installer.IInstallDescription;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * An editable implementation of {@link IInstallDescription}.
 */
public class InstallDescription implements IInstallDescription {
	private URL artifactRepo;
	private IPath installLocation;
	private URL metadataRepo;
	private String productName, flavor;
	private IInstallableUnit rootUnit;
	private boolean isAutoStart;
	private String launcherName;

	public InstallDescription(String name) {
		this.productName = name;
	}

	public URL getArtifactRepository() {
		return artifactRepo;
	}

	public String getFlavor() {
		return flavor;
	}

	public IPath getInstallLocation() {
		return installLocation;
	}

	public URL getMetadataRepository() {
		return metadataRepo;
	}

	public String getProductName() {
		return productName;
	}

	public IInstallableUnit getRootInstallableUnit() {
		return rootUnit;
	}

	public void setArtifactRepository(URL repository) {
		this.artifactRepo = repository;
	}

	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	public void setInstallLocation(IPath location) {
		this.installLocation = location;
	}

	public void setMetadataRepository(URL repository) {
		this.metadataRepo = repository;
	}

	public void setRootInstallableUnit(IInstallableUnit unit) {
		this.rootUnit = unit;
	}

	public boolean isAutoStart() {
		return isAutoStart;
	}

	public void setAutoStart(boolean value) {
		this.isAutoStart = value;
	}

	public String getLauncherName() {
		return launcherName;
	}

	public void setLauncherName(String name) {
		this.launcherName = name;
	}

}
