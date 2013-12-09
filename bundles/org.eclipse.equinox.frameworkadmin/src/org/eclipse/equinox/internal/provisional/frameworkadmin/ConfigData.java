/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.frameworkadmin;

import java.util.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;

/**
 * This object is instantiated by {@link Manipulator#getConfigData()};
 * The class that keeps some parameters of the {@link Manipulator}
 * created this object. The manipulating of the parameters will affect
 * the  {@link Manipulator}.
 *   
 * @see Manipulator
 */
public class ConfigData {
	final private String fwName;
	final private String fwVersion;
	final private String launcherName;
	final private String launcherVersion;
	private int beginningFwStartLevel = BundleInfo.NO_LEVEL;
	private int initialBundleStartLevel = BundleInfo.NO_LEVEL;
	// List of BundleInfo
	private LinkedHashSet<BundleInfo> bundlesList = new LinkedHashSet<BundleInfo>();

	private Properties properties = new Properties();

	public ConfigData(String fwName, String fwVersion, String launcherName, String launcherVersion) {
		this.fwName = fwName;
		this.fwVersion = fwVersion;
		this.launcherName = launcherName;
		this.launcherVersion = launcherVersion;
		this.initialize();
	}

	public void addBundle(BundleInfo bundleInfo) {
		bundlesList.add(bundleInfo);
	}

	public int getBeginingFwStartLevel() {
		return beginningFwStartLevel;
	}

	public BundleInfo[] getBundles() {
		if (bundlesList.size() == 0)
			return new BundleInfo[0];
		BundleInfo[] ret = new BundleInfo[bundlesList.size()];
		bundlesList.toArray(ret);
		return ret;
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	public Properties getProperties() {
		Properties ret = new Properties();
		ret.putAll(properties);
		return ret;
	}

	public String getFwName() {
		return fwName;
	}

	public String getFwVersion() {
		return fwVersion;
	}

	public int getInitialBundleStartLevel() {
		return initialBundleStartLevel;
	}

	public String getLauncherName() {
		return launcherName;
	}

	public String getLauncherVersion() {
		return launcherVersion;
	}

	public void initialize() {
		beginningFwStartLevel = BundleInfo.NO_LEVEL;
		initialBundleStartLevel = BundleInfo.NO_LEVEL;
		bundlesList.clear();
		properties.clear();
		properties.clear();
	}

	public boolean removeBundle(BundleInfo bundleInfo) {
		if (bundleInfo == null)
			throw new IllegalArgumentException("Bundle info can't be null:" + bundleInfo); //$NON-NLS-1$
		return bundlesList.remove(bundleInfo);
	}

	public void setBeginningFwStartLevel(int startLevel) {
		beginningFwStartLevel = startLevel;
	}

	public void setBundles(BundleInfo[] bundleInfos) {
		bundlesList.clear();
		if (bundleInfos != null)
			for (int i = 0; i < bundleInfos.length; i++)
				bundlesList.add(bundleInfos[i]);
	}

	public void setProperty(String key, String value) {
		if (value == null)
			properties.remove(key);
		else
			properties.setProperty(key, value);
	}

	public void appendProperties(Properties props) {
		properties.putAll(props);
	}

	public void setProperties(Properties props) {
		properties.clear();
		properties.putAll(props);
	}

	public void setInitialBundleStartLevel(int startLevel) {
		initialBundleStartLevel = startLevel;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Class:" + getClass().getName() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("============Independent===============\n"); //$NON-NLS-1$
		sb.append("fwName=" + fwName + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("fwVersion=" + fwVersion + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("launcherName=" + launcherName + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("launcherVersion=" + launcherVersion + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("beginningFwStartLevel=" + beginningFwStartLevel + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("initialBundleStartLevel=" + initialBundleStartLevel + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.bundlesList.size() == 0)
			sb.append("bundlesList=null\n"); //$NON-NLS-1$
		else {
			sb.append("bundlesList=\n"); //$NON-NLS-1$
			int i = 0;
			for (Iterator<BundleInfo> iter = bundlesList.iterator(); iter.hasNext();) {
				sb.append("\tbundlesList[" + i + "]=" + iter.next().toString() + "\n"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				i++;
			}
		}

		sb.append("============ Properties ===============\n"); //$NON-NLS-1$
		sb.append("fwIndependentProps="); //$NON-NLS-1$
		setPropsStrings(sb, properties);
		return sb.toString();
	}

	private static void setPropsStrings(StringBuffer sb, Properties props) {
		if (props.size() > 0) {
			sb.append("\n"); //$NON-NLS-1$
			for (Enumeration<Object> enumeration = props.keys(); enumeration.hasMoreElements();) {
				String key = (String) enumeration.nextElement();
				String value = props.getProperty(key);
				if (value == null || value.equals("")) //$NON-NLS-1$
					continue;
				sb.append("\t{" + key + " ,\t" + value + "}\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		} else
			sb.append("empty\n"); //$NON-NLS-1$
	}
}
