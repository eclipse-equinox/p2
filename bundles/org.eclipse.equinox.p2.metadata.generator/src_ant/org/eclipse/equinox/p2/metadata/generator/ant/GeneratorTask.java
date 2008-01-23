/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.generator.ant;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.*;
import org.eclipse.equinox.internal.p2.metadata.generator.EclipseGeneratorApplication;
import org.eclipse.osgi.util.NLS;

/**
 * An Ant task to call the p2 Metadata Generator application.
 * 
 * @since 1.0
 */
public class GeneratorTask extends Task {

	URL metadataRepository, artifactRepository;
	File source, inplace, updateSite, config;
	String rootVersion, root, flavor, p2OS, launcherConfig;
	boolean publishArtifacts, publishArtifactRepository, append, noDefaultIUs;
	File exe, base, features, bundles;

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		try {
			// collect the arguments and call the application
			new EclipseGeneratorApplication().run(getArguments());
		} catch (Exception e) {
			throw new BuildException(TaskMessages.exception_errorOccurredCallingGenerator, e);
		}
	}

	private String[] getArguments() {
		List result = new ArrayList();
		if (metadataRepository != null) {
			result.add("-metadataRepository"); //$NON-NLS-1$
			result.add(metadataRepository.toExternalForm());
		}
		if (artifactRepository != null) {
			result.add("-artifactRepository"); //$NON-NLS-1$
			result.add(artifactRepository.toExternalForm());
		}
		if (source != null) {
			result.add("-source"); //$NON-NLS-1$
			result.add(source.getAbsolutePath());
		}
		if (config != null) {
			result.add("-config"); //$NON-NLS-1$
			result.add(config.getAbsolutePath());
		}
		if (updateSite != null) {
			result.add("-updateSite"); //$NON-NLS-1$
			result.add(updateSite.getAbsolutePath());
		}
		if (inplace != null) {
			result.add("-inplace"); //$NON-NLS-1$
			result.add(inplace.getAbsolutePath());
		}
		if (rootVersion != null) {
			result.add("-rootVersion"); //$NON-NLS-1$
			result.add(rootVersion);
		}
		if (root != null) {
			result.add("-root"); //$NON-NLS-1$
			result.add(root);
		}
		if (flavor != null) {
			result.add("-flavor"); //$NON-NLS-1$
			result.add(flavor);
		}
		if (exe != null) {
			result.add("-exe"); //$NON-NLS-1$
			result.add(exe.getAbsolutePath());
		}
		if (launcherConfig != null) {
			result.add("-launcherConfig"); //$NON-NLS-1$
			result.add(launcherConfig);
		}
		if (features != null) {
			result.add("-features"); //$NON-NLS-1$
			result.add(features.getAbsolutePath());
		}
		if (bundles != null) {
			result.add("-bundles"); //$NON-NLS-1$
			result.add(bundles.getAbsolutePath());
		}
		if (base != null) {
			result.add("-base"); //$NON-NLS-1$
			result.add(base.getAbsolutePath());
		}
		if (p2OS != null) {
			result.add("-p2.os"); //$NON-NLS-1$
			result.add(p2OS);
		}
		if (publishArtifacts)
			result.add("-publishArtifacts"); //$NON-NLS-1$
		if (publishArtifactRepository)
			result.add("-publishArtifactRepository"); //$NON-NLS-1$
		if (append)
			result.add("-append"); //$NON-NLS-1$
		if (noDefaultIUs)
			result.add("-noDefaultIUs"); //$NON-NLS-1$
		return (String[]) result.toArray(new String[result.size()]);
	}

	public void setAppend(String value) {
		this.append = Boolean.valueOf(value).booleanValue();
	}

	public void setArtifactRepository(String location) {
		try {
			artifactRepository = new URL(location);
		} catch (MalformedURLException e) {
			log(NLS.bind(TaskMessages.exception_errorArtifactRepo, location), e, Project.MSG_WARN);
		}
	}

	public void setBase(String value) {
		base = new File(value);
	}

	public void setBundles(String value) {
		bundles = new File(value);
	}

	public void setConfig(String value) {
		this.config = new File(value);
	}

	public void setExe(String value) {
		exe = new File(value);
	}

	public void setFeatures(String value) {
		features = new File(value);
	}

	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	public void setInplace(String value) {
		this.inplace = new File(value);
	}

	public void setLauncherConfig(String launcherConfig) {
		this.launcherConfig = launcherConfig;
	}

	public void setMetadataRepository(String location) {
		try {
			metadataRepository = new URL(location);
		} catch (MalformedURLException e) {
			log(NLS.bind(TaskMessages.exception_errorMetadataRepo, location), e, Project.MSG_WARN);
		}
	}

	public void setNoDefaultIUs(String value) {
		noDefaultIUs = Boolean.valueOf(value).booleanValue();
	}

	public void setP2OS(String value) {
		p2OS = value;
	}

	public void setPublishArtifactRepository(String value) {
		publishArtifactRepository = Boolean.valueOf(value).booleanValue();
	}

	public void setPublishArtifacts(String value) {
		this.publishArtifacts = Boolean.valueOf(value).booleanValue();
	}

	public void setRoot(String root) {
		this.root = root;
	}

	public void setRootVersion(String rootVersion) {
		this.rootVersion = rootVersion;
	}

	public void setSource(String location) {
		source = new File(location);
	}

	public void setUpdateSite(String value) {
		this.updateSite = new File(value);
	}
}
