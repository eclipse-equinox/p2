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
package org.eclipse.equinox.p2.installer;

import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;

/**
 * An install information captures all the data needed to perform a product install.
 */
public interface IInstallDescription {
	public URL getArtifactRepository();

	/**
	 * Returns the flavor used to configure the product being installed.
	 * @return the install flavor
	 */
	public String getFlavor();

	/**
	 * Returns the local file system location to install into.
	 * @return a local file system location
	 */
	public IPath getInstallLocation();

	/**
	 * Returns the name of the product's launcher executable
	 * @return the name of the launcher executable
	 */
	public String getLauncherName();

	/**
	 * Returns the location of the metadata repository to install from
	 * @return a metadata repository URL
	 */
	public URL getMetadataRepository();

	/**
	 * Returns a human-readable name for this install.
	 * @return the name of the product
	 */
	public String getProductName();

	/**
	 * Returns the installable unit whose dependencies describe
	 * all IUs that should be installed.
	 * @return the root installable unit
	 */
	public IInstallableUnit getRootInstallableUnit();

	/**
	 * Returns whether the installed product should be started upon successful
	 * install.
	 * @return <code>true</code> if the product should be started upon successful
	 * install, and <code>false</code> otherwise
	 */
	public boolean isAutoStart();

}
