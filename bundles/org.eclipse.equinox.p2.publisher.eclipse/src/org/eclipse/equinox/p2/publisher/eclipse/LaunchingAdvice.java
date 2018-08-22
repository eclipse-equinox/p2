/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 * All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;

public class LaunchingAdvice extends AbstractAdvice implements IExecutableAdvice {

	private final LauncherData data;
	private final String configSpec;

	public LaunchingAdvice(LauncherData data, String configSpec) {
		this.data = data;
		this.configSpec = configSpec;
	}

	@Override
	protected String getConfigSpec() {
		return configSpec;
	}

	@Override
	public String[] getProgramArguments() {
		return data.getProgramArgs();
	}

	@Override
	public String[] getVMArguments() {
		return data.getJvmArgs();
	}

	@Override
	public String getExecutableName() {
		return data.getLauncherName();
	}

}
