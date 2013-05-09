/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import org.eclipse.equinox.p2.metadata.Version;
/**
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same. Please do not use this API without
 * consulting with the p2 team.
 * </p>
 * @since 2.3
 * @noreference
 */
public class RemedyIUDetail {

	public static final int STATUS_ADDED = 1;
	public static final int STATUS_REMOVED = 2;
	public static final int STATUS_NOT_ADDED = 3;
	public static final int STATUS_CHANGED = 4;

	private int status;
	private Version installedVersion;
	private Version requestedVersion;
	private Version beingInstalledVersion;
	private IInstallableUnit iu;

	public RemedyIUDetail(IInstallableUnit iu) {
		this.iu = iu;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

	public Version getRequestedVersion() {
		return requestedVersion;
	}

	public void setRequestedVersion(Version requestedVersion) {
		this.requestedVersion = requestedVersion;
	}

	public Version getBeingInstalledVersion() {
		return beingInstalledVersion;
	}

	public void setBeingInstalledVersion(Version beingInstalledVersion) {
		this.beingInstalledVersion = beingInstalledVersion;
	}

	public IInstallableUnit getIu() {
		return iu;
	}

	public Version getInstalledVersion() {
		return installedVersion;
	}

	public void setInstalledVersion(Version installedVersion) {
		this.installedVersion = installedVersion;
	}
}
