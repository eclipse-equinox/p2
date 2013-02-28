/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.*;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.equinox.p2.core.*;

/**
 * Given an eclipse installation folder and / or an eclipse configuration folder, 
 * this class creates the agent representing the system point at.
 */
public class AgentFromInstall {
	//Input
	private File configurationFolder;
	private File installFolder;
	private IProvisioningAgentProvider agentProvider;

	//Figured out
	private String profileId;
	private String p2DataAreaURI;
	private String userSpecifiedProfileId;

	private AgentFromInstall(IProvisioningAgentProvider agentProvider, File installFolder, File configurationFolder, String profileId) {
		this.installFolder = installFolder;
		this.configurationFolder = configurationFolder;
		this.agentProvider = agentProvider;
		this.userSpecifiedProfileId = profileId;
	}

	/** 
	 * Creates an agent deriving the p2 data area from the information contained in the installFolder or the configurationFolder.
	 * In the returned agent, the services will be initialized such that the SELF variable is bound to the default profile referred to in the configuration files.
	 *  
	 * @param agentProvider an instance of an agent provider from which the agent will be created. 
	 * @param installFolder null or a file referring to the installation folder of eclipse.  
	 * @param configurationFolder null or a file referring to the configuration folder of eclipse.
	 * @param profileId null or the name of the expected profile. This value is used when the detection of the profile id from the configuration file does not succeed.   
	 */
	static public IProvisioningAgent createAgentFrom(IProvisioningAgentProvider agentProvider, File installFolder, File configurationFolder, String profileId) {
		AgentFromInstall newInstance = new AgentFromInstall(agentProvider, installFolder, configurationFolder, profileId);
		return newInstance.loadAgent();
	}

	private IProvisioningAgent loadAgent() {
		if (installFolder != null) {
			if (!installFolder.exists())
				return null;
			initializeFromConfigFiles();
		} else {
			initializeByGuessing();
		}
		if (profileId == null)
			profileId = userSpecifiedProfileId;

		if (profileId == null || p2DataAreaURI == null) {
			return null;
		}
		return createAgent();
	}

	public IProvisioningAgent createAgent() {
		IProvisioningAgent agent = null;
		try {
			agent = agentProvider.createAgent(URIUtil.fromString(p2DataAreaURI));
		} catch (ProvisionException e) {
			//Can't happen
		} catch (URISyntaxException e) {
			//Can't happen since we are always constructing the string from code that manipulate files (included the code in the Manipulator) 
		}
		agent.registerService("FORCED_SELF", profileId);
		return agent;
	}

	private boolean initializeFromConfigFiles() {
		FrameworkAdmin fwk = LazyManipulator.getFrameworkAdmin();
		if (fwk == null)
			return false;
		Manipulator manipulator = fwk.getManipulator();
		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configurationFolder != null ? configurationFolder : new File(installFolder, "configuration/config.ini")); //$NON-NLS-1$
		launcherData.setHome(installFolder);
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			return false;
		} catch (FrameworkAdminRuntimeException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		profileId = manipulator.getConfigData().getProperty("eclipse.p2.profile"); //$NON-NLS-1$
		p2DataAreaURI = manipulator.getConfigData().getProperty("eclipse.p2.data.area"); //$NON-NLS-1$
		return true;
	}

	private void initializeByGuessing() {
		File p2Folder = new File(configurationFolder, "p2"); //$NON-NLS-1$
		if (!p2Folder.exists()) {
			p2Folder = new File(configurationFolder.getParentFile(), "p2"); //$NON-NLS-1$
			if (!p2Folder.exists())
				return;
		}
		p2DataAreaURI = p2Folder.toURI().toASCIIString();
		if (profileId == null) {
			profileId = findProfile(getProfileRegistryFolder(p2Folder));
		}
	}

	private File getProfileRegistryFolder(File p2Folder) {
		return new File(p2Folder, "org.eclipse.equinox.p2.engine/profileRegistry/"); //$NON-NLS-1$
	}

	private String findProfile(File profileDirectory) {
		final String PROFILE_EXT = ".profile"; //$NON-NLS-1$
		File[] profileDirectories = profileDirectory.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(PROFILE_EXT) && pathname.isDirectory();
			}
		});
		if (profileDirectories.length == 1) {
			String directoryName = profileDirectories[0].getName();
			return SimpleProfileRegistry.unescape(directoryName.substring(0, directoryName.lastIndexOf(PROFILE_EXT)));
		}

		return null;
	}
}
