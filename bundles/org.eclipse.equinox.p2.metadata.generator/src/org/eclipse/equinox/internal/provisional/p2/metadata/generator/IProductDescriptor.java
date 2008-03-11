package org.eclipse.equinox.internal.provisional.p2.metadata.generator;

import java.util.List;

public interface IProductDescriptor {

	public String getLauncherName();

	public List getPlugins();

	public List getPlugins(boolean includeFragments);

	public List getFragments();

	public List getFeatures();

	public boolean containsPlugin(String plugin);

	/**
	 * Parses the specified url and constructs a feature
	 */
	public String[] getIcons();

	public String getConfigIniPath();

	public String getId();

	public String getSplashLocation();

	public String getProductName();

	public String getApplication();

	public boolean useFeatures();

	public String getVersion();

	public String getVMArguments(String os);

	public String getProgramArguments(String os);

}