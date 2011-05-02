/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 and others. All rights reserved. This
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
