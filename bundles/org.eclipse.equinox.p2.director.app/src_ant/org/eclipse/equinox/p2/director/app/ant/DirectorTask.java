/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.director.app.ant;

import org.eclipse.equinox.internal.provisional.p2.core.Version;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.*;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.director.app.Application;

/**
 * An Ant task to call the p2 Director application.
 * 
 * @since 1.0
 */
public class DirectorTask extends Task {

	boolean roaming;
	boolean list;
	String profile, flavor, os, ws, nl, arch, installIU, uninstallIU;
	File destination, bundlePool;
	URI metadataRepository, artifactRepository;
	Version version;

	/*
	 * (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		try {
			// collect the arguments and call the application
			new Application().run(getArguments());
		} catch (Exception e) {
			throw new BuildException("Exception while calling the director task.", e);
		}
	}

	private String[] getArguments() {
		List result = new ArrayList();
		if (roaming)
			result.add("-roaming");
		if (profile != null) {
			result.add("-profile");
			result.add(profile);
		}
		if (destination != null) {
			result.add("-destination");
			result.add(destination.getAbsolutePath());
		}
		if (bundlePool != null) {
			result.add("-bundlePool");
			result.add(bundlePool.getAbsolutePath());
		}
		if (metadataRepository != null) {
			result.add("-metadataRepository");
			result.add(metadataRepository.toString());
		}
		if (artifactRepository != null) {
			result.add("-artifactRepository");
			result.add(artifactRepository.toString());
		}
		if (flavor != null) {
			result.add("-flavor");
			result.add(flavor);
		}
		if (version != null) {
			result.add("-version");
			result.add(version.toString());
		}
		if (os != null) {
			result.add("-p2.os");
			result.add(os);
		}
		if (ws != null) {
			result.add("-p2.ws");
			result.add(ws);
		}
		if (arch != null) {
			result.add("-p2.arch");
			result.add(arch);
		}
		if (nl != null) {
			result.add("-p2.nl");
			result.add(nl);
		}
		if (list) {
			result.add(Application.COMMAND_NAMES[Application.COMMAND_LIST]);
		}
		if (installIU != null) {
			result.add(Application.COMMAND_NAMES[Application.COMMAND_INSTALL]);
			result.add(installIU);
		}
		if (uninstallIU != null) {
			result.add(Application.COMMAND_NAMES[Application.COMMAND_UNINSTALL]);
			result.add(uninstallIU);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	public void setArch(String value) {
		arch = value;
	}

	public void setArtifactRepository(String value) {
		try {
			artifactRepository = URIUtil.fromString(value);
		} catch (URISyntaxException e) {
			log("Error setting the artifact repository.", e, Project.MSG_ERR);
		}
	}

	public void setBundlePool(String value) {
		bundlePool = new File(value);
	}

	public void setDestination(String value) {
		destination = new File(value);
	}

	public void setFlavor(String value) {
		flavor = value;
	}

	public void setInstallIU(String value) {
		installIU = value;
	}

	public void setList(String value) {
		list = Boolean.valueOf(value).booleanValue();
	}

	public void setMetadataRepository(String value) {
		try {
			metadataRepository = URIUtil.fromString(value);
		} catch (URISyntaxException e) {
			log("Error setting the metadata repository.", e, Project.MSG_ERR);
		}
	}

	public void setNl(String value) {
		nl = value;
	}

	public void setOs(String value) {
		os = value;
	}

	public void setProfile(String value) {
		profile = value;
	}

	public void setRoaming(String value) {
		roaming = Boolean.valueOf(value).booleanValue();
	}

	public void setUninstallIU(String value) {
		uninstallIU = value;
	}

	public void setVersion(String value) {
		version = new Version(value);
	}

	public void setWs(String value) {
		ws = value;
	}
}
