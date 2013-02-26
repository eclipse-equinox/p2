package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;

public class ConfigAreaToAgent {
	public IProfile fromConfigurationToProfile(File configurationFolder) {
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

}
