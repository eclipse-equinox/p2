package org.eclipse.equinox.internal.p2.publisher.actions;

import java.io.File;
import java.util.Properties;
import org.eclipse.equinox.internal.p2.publisher.AbstractPublishingAction;
import org.eclipse.equinox.internal.p2.publisher.features.ProductFile;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;

public class ProductAdvice extends AbstractAdvice implements ILaunchingAdvice, IConfigAdvice {

	private ProductFile product;
	private String configSpec;
	private String os;
	private DataLoader loader;
	private ConfigData configData;

	public ProductAdvice(ProductFile product, String configSpec) {
		this.product = product;
		this.configSpec = configSpec;
		os = AbstractPublishingAction.parseConfigSpec(configSpec)[1];
		loader = createDataLoader();
		configData = loader.getConfigData();
	}

	protected String getConfigSpec() {
		return configSpec;
	}

	public String[] getProgramArguments() {
		String line = product.getProgramArguments(os);
		return AbstractPublishingAction.getArrayFromString(line, " "); //$NON-NLS-1$
	}

	public String[] getVMArguments() {
		String line = product.getVMArguments(os);
		return AbstractPublishingAction.getArrayFromString(line, " "); //$NON-NLS-1$
	}

	protected boolean matchConfig(String spec, boolean includeDefault) {
		String targetOS = AbstractPublishingAction.parseConfigSpec(spec)[1];
		return os.equals(targetOS);
	}

	public BundleInfo[] getBundles() {
		return configData.getBundles();
	}

	public Properties getProperties() {
		Properties result = new Properties();
		result.putAll(configData.getFwDependentProps());
		result.putAll(configData.getFwIndependentProps());
		return result;
	}

	private DataLoader createDataLoader() {
		String location = product.getConfigIniPath(os);
		if (location == null)
			location = product.getConfigIniPath(null);
		if (location == null)
			return null;
		File configFile = new File(location);
		return new DataLoader(configFile, new File(product.getLauncherName()));
	}

	public String getExecutableName() {
		return loader.getLauncherData().getLauncherName();
	}
}
