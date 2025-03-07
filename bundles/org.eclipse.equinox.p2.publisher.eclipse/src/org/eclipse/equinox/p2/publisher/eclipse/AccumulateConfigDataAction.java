/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.File;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.eclipse.DataLoader;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.p2.publisher.*;

public class AccumulateConfigDataAction extends AbstractPublisherAction {

	private final String configSpec;
	private final DataLoader loader;

	public AccumulateConfigDataAction(IPublisherInfo info, String configSpec, File configurationLocation, File executableLocation) {
		this.configSpec = configSpec;
		loader = new DataLoader(configurationLocation, executableLocation);
	}

	@Override
	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		storeConfigData(publisherInfo, configSpec, results);
		return Status.OK_STATUS;
	}

	protected void storeConfigData(IPublisherInfo publisherInfo, String config, IPublisherResult result) {
		ConfigData data = loader.getConfigData();
		if (data == null) {
			return;
		}
		publisherInfo.addAdvice(new ConfigAdvice(data, config));
		LauncherData launcherData = loader.getLauncherData();
		if (launcherData == null) {
			return;
		}
		publisherInfo.addAdvice(new LaunchingAdvice(launcherData, config));
	}
}
