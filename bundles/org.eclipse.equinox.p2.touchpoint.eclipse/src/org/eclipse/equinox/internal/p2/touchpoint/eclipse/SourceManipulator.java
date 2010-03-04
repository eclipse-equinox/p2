/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.simpleconfigurator.manipulator.SimpleConfiguratorManipulatorImpl;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;

//This class deals with source bundles and how their addition to the source.info
public class SourceManipulator {
	private List<BundleInfo> sourceBundles;
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
		return sourceBundles.toArray(new BundleInfo[sourceBundles.size()]);
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
			manipulator.saveConfiguration(sourceBundles.toArray(new BundleInfo[sourceBundles.size()]), getFileLocation(), getLauncherLocation().toURI());
	}

	private void load() throws MalformedURLException, IOException {
		if (getFileLocation().exists())
			//input stream is bufferd and closed for us
			sourceBundles = new ArrayList<BundleInfo>(Arrays.asList(manipulator.loadConfiguration(new FileInputStream(getFileLocation()), getLauncherLocation().toURI())));
		else
			sourceBundles = new ArrayList<BundleInfo>();
	}

	private File getFileLocation() {
		return new File(Util.getConfigurationFolder(profile), SimpleConfiguratorManipulator.SOURCE_INFO_PATH);
	}

	private File getLauncherLocation() {
		return Util.getInstallFolder(profile);
	}
}
