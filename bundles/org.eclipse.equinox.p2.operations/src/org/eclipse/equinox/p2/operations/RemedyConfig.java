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

import java.util.ArrayList;
import java.util.Collection;

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
public class RemedyConfig {

	public boolean allowInstalledUpdate = false;
	public boolean allowInstalledRemoval = false;
	public boolean allowDifferentVersion = false;
	public boolean allowPartialInstall = false;

	public RemedyConfig() {

	}

	private RemedyConfig(boolean allowPartialInstall, boolean allowDifferentVersion, boolean allowInstalledUpdate, boolean allowInstalledRemoval) {
		this.allowDifferentVersion = allowDifferentVersion;
		this.allowInstalledRemoval = allowInstalledRemoval;
		this.allowInstalledUpdate = allowInstalledUpdate;
		this.allowPartialInstall = allowPartialInstall;
	}

	public static RemedyConfig[] getCheckForUpdateRemedyConfigs() {
		return new RemedyConfig[] {new RemedyConfig(false, true, true, false)};
	}

	public static RemedyConfig[] getAllRemedyConfigs() {
		Collection<RemedyConfig> remedyConfigs = new ArrayList<RemedyConfig>();
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
}
