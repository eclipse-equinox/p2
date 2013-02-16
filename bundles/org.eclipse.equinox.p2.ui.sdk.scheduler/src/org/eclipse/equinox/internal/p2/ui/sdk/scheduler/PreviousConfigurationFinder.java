package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;

public class PreviousConfigurationFinder {

	private static final Pattern path = Pattern.compile("(.+)_(.*)_(\\d+)_.*"); //$NON-NLS-1$

	static class Identifier {
		private static final String DELIM = ". _-"; //$NON-NLS-1$
		private int major, minor, service;

		Identifier(int major, int minor, int service) {
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
			if (other instanceof Identifier)
				return false;
			Identifier o = (Identifier) other;
			if (major == o.major && minor == o.minor && service == o.service)
				return true;
			return false;
		}
	}

	static class ConfigurationData {
		String productId;
		Identifier version;
		String installPathHashcode;
		File config;

		public ConfigurationData(String productId, Identifier version, String installPathHashcode, File config) {
			this.productId = productId;
			this.version = version;
			this.installPathHashcode = installPathHashcode;
			this.config = config;
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
			return config;
		}
	}

	private File currentConfig;

	public PreviousConfigurationFinder(File currentConfiguration) {
		currentConfig = currentConfiguration;
	}

	private ConfigurationData extractConfigurationData(File candidate) {
		Matcher m = path.matcher(candidate.getName());
		if (!m.matches())
			return null;
		return new ConfigurationData(m.group(1), new Identifier(m.group(2)), m.group(3), candidate.getAbsoluteFile());
	}

	public IProfile findPreviousInstalls(File searchRoot, File installFolder) {
		List<ConfigurationData> potentialConfigurations = readPreviousConfigurations(searchRoot);
		Object[] productInfo = loadEclipseProductFile(installFolder);
		ConfigurationData match = findMostRelevantConfiguration(potentialConfigurations, getInstallDirHash(installFolder), productInfo);
		if (match == null)
			match = findMostRelevantConfiguration(potentialConfigurations, productInfo);
		if (match == null)
			return null;
		return fromConfigurationToProfile(match.getConfig());
	}

	private IProfile fromConfigurationToProfile(File configurationFolder) {
		//TODO dispose the agent
		String toBeImportedProfileId = null;
		File config = new File(configurationFolder, "configuration/config.ini"); //$NON-NLS-1$ 
		URI configArea = config.getParentFile().toURI();
		InputStream is = null;
		// default area
		File p2DataArea = new File(configurationFolder, "p2"); //$NON-NLS-1$
		try {
			Properties props = new Properties();
			is = new FileInputStream(config);
			props.load(is);
			toBeImportedProfileId = props.getProperty("eclipse.p2.profile"); //$NON-NLS-1$
			String url = props.getProperty("eclipse.p2.data.area"); //$NON-NLS-1$
			if (url != null) {
				final String CONFIG_DIR = "@config.dir/"; //$NON-NLS-1$
				final String FILE_PROTOCOL = "file:"; //$NON-NLS-1$
				if (url.startsWith(CONFIG_DIR))
					url = FILE_PROTOCOL + url.substring(CONFIG_DIR.length());
				p2DataArea = new File(URIUtil.makeAbsolute(URIUtil.fromString(new File(url.substring(FILE_PROTOCOL.length())).isAbsolute() ? url : url.substring(FILE_PROTOCOL.length())), configArea));
			}
		} catch (IOException ioe) {
			//ignore
		} catch (URISyntaxException e) {
			return null;
		} finally {
			try {
				is.close();
			} catch (IOException ioe) {
				//ignore
			}
			is = null;
		}
		if (p2DataArea.exists()) {
			IProvisioningAgent agent = null;
			try {
				agent = AutomaticUpdatePlugin.getDefault().getAgentProvider().createAgent(p2DataArea.toURI());
			} catch (ProvisionException e) {
				//Can't happen
			}
			IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
			if (toBeImportedProfileId != null)
				return registry.getProfile(toBeImportedProfileId);

			//TODO we may need to set the SELF profile on the registry to load the repos
			IProfile[] allProfiles = registry.getProfiles();
			if (allProfiles.length == 1)
				return allProfiles[0];

			//			IMetadataRepositoryManager metadataRepoMgr = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			//			URI[] metadataRepos = metadataRepoMgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
			//TODO deal with the repos
		}
		return null;
	}

	private ConfigurationData findMostRelevantConfiguration(List<ConfigurationData> configurations, String installDirHash, Object[] productInfo) {
		ConfigurationData bestMatch = null;
		int numberOfcriteriaMet = 0;
		for (ConfigurationData candidate : configurations) {
			int criteriaMet = 0;
			if (!candidate.getInstallPathHashcode().equals(installDirHash))
				continue;
			criteriaMet++;
			if (!candidate.getProductId().equals(productInfo[0]))
				continue;
			criteriaMet++;
			if (!candidate.getVersion().equals(productInfo[1]))
				continue; //This is most likely ourselves
			criteriaMet++;
			if (criteriaMet > numberOfcriteriaMet) {
				bestMatch = candidate;
				numberOfcriteriaMet = criteriaMet;
			} else if (criteriaMet == numberOfcriteriaMet) {
				if (bestMatch.getConfig().lastModified() < candidate.getConfig().lastModified())
					bestMatch = candidate;
			}
		}
		return bestMatch;
	}

	//Out of a set of configuration, find the one with the most similar product info.
	//TODO do we look for the higer or lower versions?
	private ConfigurationData findMostRelevantConfiguration(List<ConfigurationData> configurations, Object[] productInfo) {
		ConfigurationData bestMatch = null;
		int numberOfcriteriaMet = 0;
		for (ConfigurationData candidate : configurations) {
			int criteriaMet = 0;
			criteriaMet++;
			if (!candidate.getProductId().equals(productInfo[0]))
				continue;
			criteriaMet++;
			if (candidate.getVersion().equals(productInfo[1]))
				continue; //This is most likely ourselves
			else if (bestMatch != null && (candidate.getVersion().equals(bestMatch.getVersion())))
				criteriaMet++;
			if (criteriaMet > numberOfcriteriaMet) {
				bestMatch = candidate;
				numberOfcriteriaMet = criteriaMet;
			} else if (criteriaMet == numberOfcriteriaMet) {
				if (bestMatch.getConfig().lastModified() < candidate.getConfig().lastModified())
					bestMatch = candidate;
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
			try {
				props.load(new FileInputStream(eclipseProduct));
				appId = props.getProperty(PRODUCT_SITE_ID);
				if (appId == null || appId.trim().length() == 0)
					appId = ECLIPSE;
				String version = props.getProperty(PRODUCT_SITE_VERSION);
				if (version == null || version.trim().length() == 0)
					appVersion = new Identifier(0, 0, 0);
				else
					appVersion = new Identifier(version);

			} catch (IOException e) {
				return new String[0];
			}
		} else {
			return new String[0];
		}
		return new Object[] {appId, appVersion};
	}

	//Iterate through a folder to look for potential configuration folders and reify them.
	private List<ConfigurationData> readPreviousConfigurations(File configurationFolder) {
		File[] candidates = configurationFolder.listFiles();
		List<ConfigurationData> configurations = new ArrayList<ConfigurationData>(candidates.length);
		for (File candidate : candidates) {
			if (!candidate.isDirectory())
				continue;
			if (candidate.equals(currentConfig))
				continue;
			ConfigurationData tmp = extractConfigurationData(candidate);
			if (tmp != null)
				configurations.add(tmp);
		}
		return configurations;
	}

	//Simplified code computing the hashCode of the install location. The real runtime code is in the launcher
	private String getInstallDirHash(File installFolder) {
		try {
			return Integer.toString(installFolder.getCanonicalPath().hashCode());
		} catch (IOException e) {
			return ""; //$NON-NLS-1$
		}
	}
}
