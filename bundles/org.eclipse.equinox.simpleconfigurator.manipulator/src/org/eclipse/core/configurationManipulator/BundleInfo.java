/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.configurationManipulator;

public class BundleInfo {
	public static final int NO_LEVEL = -1;

	private String symbolicName;
	private String version;
	private String location;
	private int expectedState;
	private int startLevel = NO_LEVEL;

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int result = symbolicName == null ? 0 : symbolicName.hashCode();
		result = result + (version == null ? 0 : version.hashCode());
		result = result + (location == null ? 0 : location.hashCode());
		return result;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public String getVersion() {
		return version;
	}

	public int expectedState() {
		return expectedState;
	}

	public int getStartLevel() {
		return startLevel;
	}

	public String getLocation() {
		return location;
	}

	public void setSymbolicName(String id) {
		symbolicName = id;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setExpectedState(int state) {
		expectedState = state;
	}

	public void setStartLevel(int level) {
		this.startLevel = level;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object toCompare) {
		if (toCompare instanceof BundleInfo) {
			BundleInfo info = (BundleInfo) toCompare;
			if (info.symbolicName.equals(symbolicName) && info.version.equals(version) && (info.location == null || location == null ? true : info.location.equals(location)))
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("BundleInfo("); //$NON-NLS-1$
		buffer.append(symbolicName);
		buffer.append(' ');
		buffer.append(version);
		buffer.append(", startLevel="); //$NON-NLS-1$
		buffer.append(startLevel);
		buffer.append(", expectedState="); //$NON-NLS-1$
		buffer.append(expectedState);
		buffer.append(')');
		return buffer.toString();
	}
}
