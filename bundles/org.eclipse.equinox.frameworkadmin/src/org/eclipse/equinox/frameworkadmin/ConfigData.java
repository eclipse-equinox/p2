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

package org.eclipse.equinox.frameworkadmin;

import java.util.*;

/**
 * This object is instantiated by {@link Manipulator#getConfigData()};
 * The class that keeps some parameters of the {@link Manipulator}
 * created this object. The manipulating of the parameters will affect
 * the  {@link Manipulator}.
 *   
 * @see Manipulator
 */
public class ConfigData {

	private static Properties appendProperties(Properties to, Properties from) {
		if (from != null) {
			if (to == null)
				to = new Properties();
			//			printoutProperties(System.out, "to", to);
			//			printoutProperties(System.out, "from", from);

			for (Enumeration enumeration = from.keys(); enumeration.hasMoreElements();) {
				String key = (String) enumeration.nextElement();
				to.setProperty(key, from.getProperty(key));
			}
		}
		//		printoutProperties(System.out, "to", to);
		return to;
	}

	final private String fwName;
	final private String fwVersion;
	final private String launcherName;
	final private String launcherVersion;
	private int beginningFwStartLevel = BundleInfo.NO_LEVEL;
	private int initialBundleStartLevel = BundleInfo.NO_LEVEL;
	// List of BundleInfo
	private LinkedHashSet bundlesList = new LinkedHashSet();
	private Properties fwIndependentProps = new Properties();

	private Properties fwDependentProps = new Properties();

	public ConfigData(String fwName, String fwVersion, String launcherName, String launcherVersion) {
		this.fwName = fwName;
		this.fwVersion = fwVersion;
		this.launcherName = launcherName;
		this.launcherVersion = launcherVersion;
		this.initialize();
	}

	public void addBundle(BundleInfo bundleInfo) {
		this.bundlesList.add(bundleInfo);
	}

	public int getBeginingFwStartLevel() {
		return this.beginningFwStartLevel;
	}

	public BundleInfo[] getBundles() {
		if (bundlesList.size() == 0)
			return new BundleInfo[0];
		BundleInfo[] ret = new BundleInfo[bundlesList.size()];
		bundlesList.toArray(ret);
		return ret;
	}

	public String getFwDependentProp(String key) {
		return fwDependentProps.getProperty(key);
	}

	public Properties getFwDependentProps() {
		Properties ret = new Properties();
		appendProperties(ret, fwDependentProps);
		return ret;
	}

	public String getFwIndependentProp(String key) {
		return fwIndependentProps.getProperty(key);
	}

	public Properties getFwIndependentProps() {
		Properties ret = new Properties();
		appendProperties(ret, fwIndependentProps);
		return ret;
	}

	public String getFwName() {
		return this.fwName;
	}

	public String getFwVersion() {
		return this.fwVersion;
	}

	public int getInitialBundleStartLevel() {
		return this.initialBundleStartLevel;
	}

	public String getLauncherName() {
		return launcherName;
	}

	public String getLauncherVersion() {
		return launcherVersion;
	}

	public void initialize() {
		this.beginningFwStartLevel = BundleInfo.NO_LEVEL;
		this.initialBundleStartLevel = BundleInfo.NO_LEVEL;
		this.bundlesList.clear();
		this.fwIndependentProps.clear();
		this.fwDependentProps.clear();
	}

	public void removeBundle(BundleInfo bundleInfo) {
		this.bundlesList.remove(bundleInfo);
	}

	public void setBeginningFwStartLevel(int startLevel) {
		this.beginningFwStartLevel = startLevel;
	}

	public void setBundles(BundleInfo[] bundleInfos) {
		this.bundlesList.clear();
		if (bundleInfos != null)
			for (int i = 0; i < bundleInfos.length; i++)
				bundlesList.add(bundleInfos[i]);
	}

	public void setFwDependentProp(String key, String value) {
		this.fwDependentProps.setProperty(key, value);
	}

	public void setFwDependentProps(Properties props) {
		this.fwDependentProps.clear();
		appendProperties(fwDependentProps, props);
	}

	public void setFwIndependentProp(String key, String value) {
		this.fwIndependentProps.setProperty(key, value);
	}

	public void setFwIndependentProps(Properties props) {
		this.fwIndependentProps.clear();
		appendProperties(fwIndependentProps, props);
	}

	public void setInitialBundleStartLevel(int startLevel) {
		this.initialBundleStartLevel = startLevel;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Class:" + this.getClass().getName() + "\n");
		sb.append("============Independent===============\n");
		sb.append("fwName=" + this.fwName + "\n");
		sb.append("fwVersion=" + this.fwVersion + "\n");
		sb.append("launcherName=" + this.launcherName + "\n");
		sb.append("launcherVersion=" + this.launcherVersion + "\n");
		sb.append("beginningFwStartLevel=" + this.beginningFwStartLevel + "\n");
		sb.append("initialBundleStartLevel=" + this.initialBundleStartLevel + "\n");
		if (this.bundlesList.size() == 0)
			sb.append("bundlesList=null\n");
		else {
			sb.append("bundlesList=\n");
			int i = 0;
			for (Iterator iter = this.bundlesList.iterator(); iter.hasNext();) {
				sb.append("\tbundlesList[" + i + "]=" + iter.next().toString() + "\n");
				i++;
			}
		}

		sb.append("============ Fw Independent Props ===============\n");
		sb.append("fwIndependentProps=");
		setPropsStrings(sb, fwIndependentProps);
		sb.append("============ Fw Dependent Props ===============\n");
		sb.append("fwDependentProps=");
		setPropsStrings(sb, fwDependentProps);
		return sb.toString();
	}

	private static void setPropsStrings(StringBuffer sb, Properties props) {
		if (props.size() > 0) {
			sb.append("\n");
			for (Enumeration enumeration = props.keys(); enumeration.hasMoreElements();) {
				String key = (String) enumeration.nextElement();
				String value = props.getProperty(key);
				if (value == null || value.equals(""))
					continue;
				sb.append("\t{" + key + " ,\t" + value + "}\n");
			}
		} else
			sb.append("empty\n");
	}
}
