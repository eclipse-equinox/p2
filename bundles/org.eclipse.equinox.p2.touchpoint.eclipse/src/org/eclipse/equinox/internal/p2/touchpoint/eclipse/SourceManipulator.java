/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import org.eclipse.equinox.internal.provisional.p2.core.Version;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.simpleconfigurator.manipulator.SimpleConfiguratorManipulatorImpl;

//This class deals with source bundles and how their addition to the source.info
public class SourceManipulator {
	private List sourceBundles;
	private IProfile profile;
	boolean changed = false;
	private SimpleConfiguratorManipulatorImpl manipulator;

	public SourceManipulator(IProfile profile) {
		this.profile = profile;
		this.manipulator = new SimpleConfiguratorManipulatorImpl();
	}

	public BundleInfo[] getBundles() throws IOException {
		if (sourceBundles == null)
			load();
		return (BundleInfo[]) sourceBundles.toArray(new BundleInfo[sourceBundles.size()]);
	}

	public void addBundle(File bundleFile, String bundleId, Version bundleVersion) throws IOException {
		if (sourceBundles == null)
			load();
		BundleInfo sourceInfo = new BundleInfo(bundleFile.toURI());
		sourceInfo.setSymbolicName(bundleId);
		sourceInfo.setVersion(bundleVersion.toString());
		sourceBundles.add(sourceInfo);
	}

	public void removeBundle(File bundleFile, String bundleId, Version bundleVersion) throws MalformedURLException, IOException {
		if (sourceBundles == null)
			load();

		BundleInfo sourceInfo = new BundleInfo();
		if (bundleFile != null)
			sourceInfo.setLocation(bundleFile.toURI());
		sourceInfo.setSymbolicName(bundleId);
		sourceInfo.setVersion(bundleVersion.toString());
		sourceBundles.remove(sourceInfo);
	}

	public void save() throws IOException {
		if (sourceBundles != null)
			manipulator.saveConfiguration((BundleInfo[]) sourceBundles.toArray(new BundleInfo[sourceBundles.size()]), getFileLocation(), getLauncherLocation());
	}

	private void load() throws MalformedURLException, IOException {
		if (getFileLocation().exists())
			sourceBundles = new ArrayList(Arrays.asList(manipulator.loadConfiguration(getFileLocation().toURL(), getLauncherLocation())));
		else
			sourceBundles = new ArrayList();
	}

	private File getFileLocation() {
		return new File(Util.getConfigurationFolder(profile), "org.eclipse.equinox.source/source.info"); //$NON-NLS-1$
	}

	private File getLauncherLocation() {
		return Util.getInstallFolder(profile);
	}
}
