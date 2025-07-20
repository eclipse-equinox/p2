/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;

/**
 * @since 2.8
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
	private final IInstallableUnit iu;

	/**
	 * @since 2.8
	 */
	public RemedyIUDetail(IInstallableUnit iu) {
		this.iu = iu;
	}

	/**
	 * @since 2.8
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * @since 2.8
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * @since 2.8
	 */
	public Version getRequestedVersion() {
		return requestedVersion;
	}

	/**
	 * @since 2.8
	 */
	public void setRequestedVersion(Version requestedVersion) {
		this.requestedVersion = requestedVersion;
	}

	/**
	 * @since 2.8
	 */
	public Version getBeingInstalledVersion() {
		return beingInstalledVersion;
	}

	/**
	 * @since 2.8
	 */
	public void setBeingInstalledVersion(Version beingInstalledVersion) {
		this.beingInstalledVersion = beingInstalledVersion;
	}

	/**
	 * @since 2.8
	 */
	public IInstallableUnit getIu() {
		return iu;
	}

	/**
	 * @since 2.8
	 */
	public Version getInstalledVersion() {
		return installedVersion;
	}

	/**
	 * @since 2.8
	 */
	public void setInstalledVersion(Version installedVersion) {
		this.installedVersion = installedVersion;
	}
}
