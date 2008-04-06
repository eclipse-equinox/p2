/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;

public class LaunchingAdvice extends AbstractAdvice implements ILaunchingAdvice {

	private LauncherData data;
	private String configSpec;

	public LaunchingAdvice(LauncherData data, String configSpec) {
		this.data = data;
		this.configSpec = configSpec;
	}

	protected String getConfigSpec() {
		return configSpec;
	}

	public String[] getProgramArguments() {
		return data.getProgramArgs();
	}

	public String[] getVMArguments() {
		return data.getJvmArgs();
	}

	public String getExecutableName() {
		return data.getLauncherName();
	}

}
