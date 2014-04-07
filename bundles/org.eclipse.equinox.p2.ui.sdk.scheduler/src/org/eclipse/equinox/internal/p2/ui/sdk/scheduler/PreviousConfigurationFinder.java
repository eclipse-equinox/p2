/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson AB - initial API and implementation
 *     Ericsson AB (Hamdan Msheik) - bug- 432167
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.AgentFromInstall;
import org.eclipse.equinox.p2.core.IProvisioningAgent;

public class PreviousConfigurationFinder {

	private static final Pattern path = Pattern.compile("(.+?)_{1}?([0-9\\.]+)_{1}?(\\d+)(_*?([^_].*)|$)"); //$NON-NLS-1$

	public static class Identifier implements Comparable<Identifier> {
		private static final String DELIM = ". _-"; //$NON-NLS-1$
		private int major, minor, service;

		public Identifier(int major, int minor, int service) {
			super();
			this.major = major;
			this.minor = minor;
			this.service = service;
		}

		/**
		 * @throws NumberFormatException if cannot parse the major and minor version components
		 */
		Identifier(String versionString) {
			super();
			StringTokenizer tokenizer = new StringTokenizer(versionString, DELIM);

			// major
			if (tokenizer.hasMoreTokens())
				major = Integer.parseInt(tokenizer.nextToken());

			// minor
			if (tokenizer.hasMoreTokens())
				minor = Integer.parseInt(tokenizer.nextToken());

			try {
				// service
				if (tokenizer.hasMoreTokens())
					service = Integer.parseInt(tokenizer.nextToken());
			} catch (NumberFormatException nfe) {
				// ignore the service qualifier in that case and default to 0
				// this will allow us to tolerate other non-conventional version numbers 
			}
		}

		/**
		 * Returns true if this id is considered to be greater than or equal to the given baseline.
		 * e.g. 
		 * 1.2.9 >= 1.3.1 -> false
		 * 1.3.0 >= 1.3.1 -> false
		 * 1.3.1 >= 1.3.1 -> true
		 * 1.3.2 >= 1.3.1 -> true
		 * 2.0.0 >= 1.3.1 -> true
		 */
		boolean isGreaterEqualTo(Identifier minimum) {
			if (major < minimum.major)
				return false;
			if (major > minimum.major)
				return true;
			// major numbers are equivalent so check minor
			if (minor < minimum.minor)
				return false;
			if (minor > minimum.minor)
				return true;
			// minor numbers are equivalent so check service
			return service >= minimum.service;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Identifier))
				return false;
			Identifier o = (Identifier) other;
			if (major == o.major && minor == o.minor && service == o.service)
				return true;
			return false;
		}

		@Override
		public String toString() {
			return "" + major + '.' + minor + '.' + service; //$NON-NLS-1$
		}

		public int compareTo(Identifier o) {

			if (o != null) {
				if (major < o.major) {
					return -1;
				} else if (major > o.major) {
					return 1;
				} else if (minor < o.minor) {
					return -1;
				} else if (minor > o.minor) {
					return 1;
				} else if (service < o.service) {
					return -1;
				} else if (service > o.service) {
					return 1;
				} else {
					return 0;
				}
			}

			return 1;
		}
	}

	public static class ConfigurationDescriptor {
		String productId;
		Identifier version;
		String installPathHashcode;
		File configFolder;
		String os_ws_arch;

		public ConfigurationDescriptor(String productId, Identifier version, String installPathHashcode, String platformConfig, File configFolder) {
			this.productId = productId;
			this.version = version;
			this.installPathHashcode = installPathHashcode;
			this.configFolder = configFolder;
			this.os_ws_arch = platformConfig;
		}

		public String getProductId() {
			return productId;
		}

		public Identifier getVersion() {
			return version;
		}

		public String getInstallPathHashcode() {
			return installPathHashcode;
		}

		public File getConfig() {
			return configFolder;
		}

		public String getPlatformConfig() {
			return os_ws_arch;
		}
	}

	public static class ConfigurationDescriptorComparator implements Comparator<ConfigurationDescriptor> {

		// compare ConfigurationDescriptor according to their versions and when equals according to their lastModified field
		public int compare(ConfigurationDescriptor o1, ConfigurationDescriptor o2) {
			int result = -1;
			if (o1 != null && o2 != null) {
				if (o1.getVersion().compareTo(o2.getVersion()) != 0) {
					result = o1.getVersion().compareTo(o2.getVersion());
				} else {
					if (o1.getConfig().lastModified() > o2.getConfig().lastModified()) {
						result = 1;
					} else if (o1.getConfig().lastModified() < o2.getConfig().lastModified()) {
						result = -1;
					} else
						result = 0;
				}
			} else if (o1 == null) {
				result = -1;
			} else if (o2 == null) {
				result = 1;
			}
			return result;
		}

	}

	private File currentConfig;

	public PreviousConfigurationFinder(File currentConfiguration) {
		currentConfig = currentConfiguration;
	}

	public ConfigurationDescriptor extractConfigurationData(File candidate) {
		Matcher m = path.matcher(candidate.getName());
		if (!m.matches())
			return null;
		return new ConfigurationDescriptor(m.group(1), new Identifier(m.group(2)), m.group(3), m.group(5), candidate.getAbsoluteFile());
	}

	public IProvisioningAgent findPreviousInstalls(File searchRoot, File installFolder) {
		List<ConfigurationDescriptor> potentialConfigurations = readPreviousConfigurations(searchRoot);
		ConfigurationDescriptor runningConfiguration = getConfigdataFromProductFile(installFolder);
		if (runningConfiguration == null)
			return null;
		ConfigurationDescriptor match = findMostRelevantConfigurationFromInstallHashDir(potentialConfigurations, runningConfiguration);
		if (match == null)
			match = findMostRelevantConfigurationFromProductId(potentialConfigurations, runningConfiguration);
		if (match == null)
			match = findSpecifiedConfiguration(searchRoot);
		if (match == null)
			return null;
		return AgentFromInstall.createAgentFrom(AutomaticUpdatePlugin.getDefault().getAgentProvider(), null, new File(match.getConfig(), "configuration"), null); //$NON-NLS-1$

	}

	public ConfigurationDescriptor findSpecifiedConfiguration(File searchRoot) {
		final String prefixesAsString = System.getProperty("p2.forcedMigrationLocation"); //$NON-NLS-1$
		if (prefixesAsString == null)
			return null;

		String[] prefixes = prefixesAsString.split(","); //$NON-NLS-1$
		for (String prefix : prefixes) {
			final String p = prefix;
			File[] match = searchRoot.listFiles(new FileFilter() {
				public boolean accept(File candidate) {
					if (!candidate.isDirectory())
						return false;
					if (currentConfig.equals(candidate))
						return false;
					return candidate.getName().contains(p);
				}
			});
			if (match.length != 0)
				return new ConfigurationDescriptor("unknown", new Identifier("0.0.0"), "unknown", "unknown", match[0]); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
		}
		return null;
	}

	private ConfigurationDescriptor getConfigdataFromProductFile(File installFolder) {
		Object[] productFileInfo = loadEclipseProductFile(installFolder);
		//Contrarily  to the runtime, when the .eclipseproduct can't be found, we don't fallback to org.eclipse.platform. 
		if (productFileInfo.length == 0)
			return null;
		return new ConfigurationDescriptor((String) productFileInfo[0], (Identifier) productFileInfo[1], getInstallDirHash(installFolder), Platform.getOS() + '_' + Platform.getWS() + '_' + Platform.getOSArch(), null);
	}

	public ConfigurationDescriptor findMostRelevantConfigurationFromInstallHashDir(List<ConfigurationDescriptor> configurations, ConfigurationDescriptor configToMatch) {
		ConfigurationDescriptor bestMatch = null;
		int numberOfcriteriaMet = 0;
		for (ConfigurationDescriptor candidate : configurations) {
			int criteriaMet = 0;
			if (!candidate.getInstallPathHashcode().equals(configToMatch.getInstallPathHashcode())) {
				continue;
			}
			criteriaMet++;

			if (configToMatch.getProductId().equals(candidate.getProductId()) && //
					configToMatch.getPlatformConfig().equals(candidate.getPlatformConfig()) && //
					(!candidate.getVersion().isGreaterEqualTo(configToMatch.getVersion()))) {
				//We have a match
				criteriaMet++;
			}

			if (criteriaMet == 0)
				continue;
			if (criteriaMet > numberOfcriteriaMet) {
				bestMatch = candidate;
				numberOfcriteriaMet = criteriaMet;
			} else if (criteriaMet == numberOfcriteriaMet) {
				if (bestMatch.getVersion().equals(candidate.getVersion())) {
					if (bestMatch.getConfig().lastModified() < candidate.getConfig().lastModified()) {
						bestMatch = candidate;
					}
				} else {
					if (candidate.getVersion().isGreaterEqualTo(bestMatch.getVersion()))
						bestMatch = candidate;
				}
			}
		}
		return bestMatch;
	}

	//Out of a set of configuration, find the one with the most similar product info.
	public ConfigurationDescriptor findMostRelevantConfigurationFromProductId(List<ConfigurationDescriptor> configurations, ConfigurationDescriptor configToMatch) {
		ConfigurationDescriptor bestMatch = null;

		List<ConfigurationDescriptor> candidates = new ArrayList<ConfigurationDescriptor>();
		List<ConfigurationDescriptor> candidatesWithUnkonwArchitecture = new ArrayList<ConfigurationDescriptor>();
		for (ConfigurationDescriptor candidate : configurations) {
			if (configToMatch.getProductId().equals(candidate.getProductId()) && configToMatch.getVersion().isGreaterEqualTo(candidate.getVersion())) {
				if (configToMatch.getPlatformConfig().equals(candidate.getPlatformConfig())) {
					candidates.add(candidate);
				} else { //candidate.getPlatformConfig() returns null in legacy installation prior to 4.x.x releases
					candidatesWithUnkonwArchitecture.add(candidate);
				}
			}
		}

		if (!candidates.isEmpty()) {
			Collections.sort(candidates, new ConfigurationDescriptorComparator());
			bestMatch = candidates.get(candidates.size() - 1);
		}

		if (bestMatch == null) {
			if (!candidatesWithUnkonwArchitecture.isEmpty()) {
				Collections.sort(candidatesWithUnkonwArchitecture, new ConfigurationDescriptorComparator());
				bestMatch = candidatesWithUnkonwArchitecture.get(candidatesWithUnkonwArchitecture.size() - 1);
			}
		}

		return bestMatch;
	}

	//Load the .eclipseproduct file in the base of the installation. This logic is very similar to the one found in the launcher
	private Object[] loadEclipseProductFile(File installDir) {
		final String ECLIPSE = "eclipse"; //$NON-NLS-1$
		final String PRODUCT_SITE_MARKER = ".eclipseproduct"; //$NON-NLS-1$
		final String PRODUCT_SITE_ID = "id"; //$NON-NLS-1$
		final String PRODUCT_SITE_VERSION = "version"; //$NON-NLS-1$

		File eclipseProduct = new File(installDir, PRODUCT_SITE_MARKER);
		String appId = null;
		Identifier appVersion = null;
		if (eclipseProduct.exists()) {
			Properties props = new Properties();
			FileInputStream is = null;
			try {
				try {
					is = new FileInputStream(eclipseProduct);
					props.load(is);
					appId = props.getProperty(PRODUCT_SITE_ID);
					if (appId == null || appId.trim().length() == 0)
						appId = ECLIPSE;
					String version = props.getProperty(PRODUCT_SITE_VERSION);
					if (version == null || version.trim().length() == 0)
						appVersion = new Identifier(0, 0, 0);
					else
						appVersion = new Identifier(version);
				} finally {
					if (is != null)
						is.close();
				}
			} catch (IOException e) {
				return new String[0];
			}
		} else {
			return new String[0];
		}
		return new Object[] {appId, appVersion};
	}

	//Iterate through a folder to look for potential configuration folders and reify them.
	public List<ConfigurationDescriptor> readPreviousConfigurations(File configurationFolder) {
		File[] candidates = configurationFolder.listFiles();
		List<ConfigurationDescriptor> configurations = new ArrayList<ConfigurationDescriptor>(candidates.length);
		for (File candidate : candidates) {
			if (!candidate.isDirectory())
				continue;
			if (candidate.equals(currentConfig))
				continue;
			ConfigurationDescriptor tmp = extractConfigurationData(candidate);
			if (tmp != null)
				configurations.add(tmp);
		}
		return configurations;
	}

	//This code computes the hashCode of the install location. 
	//This is a simplified version of the code that the launcher executes.
	private String getInstallDirHash(File installFolder) {
		try {
			return Integer.toString(installFolder.getCanonicalPath().hashCode());
		} catch (IOException e) {
			return ""; //$NON-NLS-1$
		}
	}
}
