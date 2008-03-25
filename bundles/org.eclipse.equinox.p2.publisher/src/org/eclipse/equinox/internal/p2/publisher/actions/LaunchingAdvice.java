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
