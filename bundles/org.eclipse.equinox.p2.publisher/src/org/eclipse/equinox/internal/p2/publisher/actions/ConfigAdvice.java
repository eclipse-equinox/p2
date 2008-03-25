package org.eclipse.equinox.internal.p2.publisher.actions;

import java.util.Properties;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;

public class ConfigAdvice extends AbstractAdvice implements IConfigAdvice {

	private ConfigData data;
	private String configSpec;

	public ConfigAdvice(ConfigData data, String configSpec) {
		this.data = data;
		this.configSpec = configSpec;
	}

	public BundleInfo[] getBundles() {
		return data.getBundles();
	}

	protected String getConfigSpec() {
		return configSpec;
	}

	public Properties getProperties() {
		Properties result = new Properties();
		result.putAll(data.getFwDependentProps());
		result.putAll(data.getFwIndependentProps());
		return result;
	}

}
