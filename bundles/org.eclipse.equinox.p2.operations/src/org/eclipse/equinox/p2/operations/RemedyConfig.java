/*******************************************************************************
 * Copyright (c) 2013, 2017 Red Hat, Inc. and others
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

import java.util.ArrayList;
import java.util.Collection;

/**
 * @since 2.8
 */
public class RemedyConfig {

	boolean allowInstalledUpdate = false;
	boolean allowInstalledRemoval = false;
	boolean allowDifferentVersion = false;
	boolean allowPartialInstall = false;

	RemedyConfig() {

	}

	RemedyConfig(boolean allowPartialInstall, boolean allowDifferentVersion, boolean allowInstalledUpdate,
			boolean allowInstalledRemoval) {
		this.allowDifferentVersion = allowDifferentVersion;
		this.allowInstalledRemoval = allowInstalledRemoval;
		this.allowInstalledUpdate = allowInstalledUpdate;
		this.allowPartialInstall = allowPartialInstall;
	}

	/**
	 * @since 2.8
	 */
	public static RemedyConfig[] getCheckForUpdateRemedyConfigs() {
		return new RemedyConfig[] {new RemedyConfig(false, true, true, false)};
	}

	/**
	 * @since 2.8
	 */
	public static RemedyConfig[] getAllRemedyConfigs() {
		Collection<RemedyConfig> remedyConfigs = new ArrayList<>();
		int allMasks = (1 << 4);
		for (int i = 1; i < allMasks; i++) {
			RemedyConfig remedyConfig = new RemedyConfig();
			for (int j = 0; j < 4; j++) {
				if ((i & (1 << j)) > 0) {
					switch (j) {
						case 0 :
							remedyConfig.allowPartialInstall = true;
							break;
						case 1 :
							remedyConfig.allowDifferentVersion = true;
							break;
						case 2 :
							remedyConfig.allowInstalledUpdate = true;
							break;
						case 3 :
							remedyConfig.allowInstalledRemoval = true;
							break;
					}
				}

			}
			remedyConfigs.add(remedyConfig);
		}
		RemedyConfig[] test = remedyConfigs.toArray(new RemedyConfig[remedyConfigs.size()]);
		return test;
	}

	/**
	 * @since 2.8
	 */
	public boolean isAllowDifferentVersion() {
		return allowDifferentVersion;
	}

	/**
	 * @since 2.8
	 */
	public boolean isAllowInstalledRemoval() {
		return allowInstalledRemoval;
	}

	/**
	 * @since 2.8
	 */
	public boolean isAllowInstalledUpdate() {
		return allowInstalledUpdate;
	}

	/**
	 * @since 2.8
	 */
	public boolean isAllowPartialInstall() {
		return allowPartialInstall;
	}
}
