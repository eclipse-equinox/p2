/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata.generator;

import org.eclipse.equinox.frameworkadmin.BundleInfo;

public class GeneratorBundleInfo extends BundleInfo {
	//	public static final int NO_LEVEL = -1;

	//	private String symbolicName;
	//	private String version;
	//	private String location;
	//	private int expectedState;
	//	private int startLevel = NO_LEVEL;
	//	private String manifest;

	private String specialConfigCommands;
	private String specialUnconfigCommands;

	public GeneratorBundleInfo(BundleInfo bInfo) {
		super.setBundleId(bInfo.getBundleId());
		super.setLocation(bInfo.getLocation());
		super.setManifest(bInfo.getManifest());
		super.setMarkedAsStarted(bInfo.isMarkedAsStarted());
		super.setResolved(bInfo.isResolved());
		super.setStartLevel(bInfo.getStartLevel());
		super.setSymbolicName(bInfo.getSymbolicName());
		super.setVersion(bInfo.getVersion());
	}

	public GeneratorBundleInfo() {
		// TODO Auto-generated constructor stub
	}

	//	/* (non-Javadoc)
	//	 * @see java.lang.Object#hashCode()
	//	 */
	//	public int hashCode() {
	//		int result = symbolicName == null ? 0 : symbolicName.hashCode();
	//		result = result + (version == null ? 0 : version.hashCode());
	//		result = result + (location == null ? 0 : location.hashCode());
	//		return result;
	//	}
	//
	//	public String getSymbolicName() {
	//		return symbolicName;
	//	}
	//
	//	public String getVersion() {
	//		return version;
	//	}
	//
	//	public int expectedState() {
	//		return expectedState;
	//	}
	//
	//	public int getStartLevel() {
	//		return startLevel;
	//	}
	//
	//	public String getLocation() {
	//		return location;
	//	}
	//
	//	public void setSymbolicName(String id) {
	//		symbolicName = id;
	//	}
	//
	//	public void setVersion(String version) {
	//		this.version = version;
	//	}
	//
	//	public void setExpectedState(int state) {
	//		expectedState = state;
	//	}
	//
	//	public void setStartLevel(int level) {
	//		this.startLevel = level;
	//	}
	//
	//	public void setLocation(String location) {
	//		this.location = location;
	//	}
	//
	//	public void setManifest(String manifest) {
	//		this.manifest = manifest;
	//	}
	//	
	//	public String getManifest() {
	//		return manifest;
	//	}
	//	
	public String getSpecialConfigCommands() {
		return specialConfigCommands;
	}

	public void setSpecialConfigCommands(String specialConfigCommands) {
		this.specialConfigCommands = specialConfigCommands;
	}

	public String getSpecialUnconfigCommands() {
		return specialUnconfigCommands;
	}

	public void setSpecialUnconfigCommands(String specialUnconfigCommands) {
		this.specialUnconfigCommands = specialUnconfigCommands;
	}

	//	/* (non-Javadoc)
	//	 * @see java.lang.Object#equals(java.lang.Object)
	//	 */
	//	public boolean equals(Object toCompare) {
	//		if (toCompare instanceof GeneratorBundleInfo) {
	//			GeneratorBundleInfo info = (GeneratorBundleInfo) toCompare;
	//			if (info.symbolicName.equals(symbolicName) && info.version.equals(version) && (info.location == null || location == null ? true : info.location.equals(location)))
	//				return true;
	//		}
	//		return false;
	//	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		String superSt = super.toString();
		if (superSt.length() > 0)
			buffer.append(superSt.substring(0, superSt.length() - 1));
		buffer.append(", this.specialConfigCommands="); //$NON-NLS-1$
		buffer.append(this.specialConfigCommands);
		buffer.append(')');
		return buffer.toString();
	}
}
