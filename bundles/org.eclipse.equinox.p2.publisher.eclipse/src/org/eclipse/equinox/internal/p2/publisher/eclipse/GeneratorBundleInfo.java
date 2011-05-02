/*******************************************************************************
 *  Copyright (c) 2006, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class GeneratorBundleInfo extends BundleInfo {
	private IInstallableUnit iu = null;
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
		super();
	}

	public String getSpecialConfigCommands() {
		return specialConfigCommands;
	}

	public void setSpecialConfigCommands(String specialConfigCommands) {
		this.specialConfigCommands = specialConfigCommands;
	}

	public void setIU(IInstallableUnit iu) {
		this.iu = iu;
	}

	public IInstallableUnit getIU() {
		return iu;
	}

	public String getSpecialUnconfigCommands() {
		return specialUnconfigCommands;
	}

	public void setSpecialUnconfigCommands(String specialUnconfigCommands) {
		this.specialUnconfigCommands = specialUnconfigCommands;
	}

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
