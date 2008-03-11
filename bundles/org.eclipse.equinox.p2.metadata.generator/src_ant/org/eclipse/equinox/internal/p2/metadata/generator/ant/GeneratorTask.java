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
package org.eclipse.equinox.internal.p2.metadata.generator.ant;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.equinox.internal.p2.metadata.generator.EclipseGeneratorApplication;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;

/**
 * An Ant task to call the p2 Metadata Generator application.
 * 
 * @since 1.0
 */
public class GeneratorTask extends Task {

	protected EclipseInstallGeneratorInfoProvider provider = null;
	protected EclipseGeneratorApplication generator = null;

	static private IPublisherResult result;
	private String mode;

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		try {
			if ("incremental".equals(mode)) { //$NON-NLS-1$
				if (result == null)
					result = new PublisherResult();
				generator.setIncrementalResult(result);
				generator.setGeneratorRootIU(false);
			} else if ("final".equals(mode) && result != null) { //$NON-NLS-1$
				generator.setIncrementalResult(result);
				generator.setGeneratorRootIU(true);
			}

			generator.run(provider);

			if (!"incremental".equals(mode)) { //$NON-NLS-1$
				provider = null;
				generator = null;
				result = null;
			}
		} catch (Exception e) {
			throw new BuildException(TaskMessages.exception_errorOccurredCallingGenerator, e);
		}
	}

	private EclipseInstallGeneratorInfoProvider getProvider() {
		if (provider == null)
			provider = new EclipseInstallGeneratorInfoProvider();
		return provider;
	}

	private EclipseGeneratorApplication getGenerator() {
		if (generator == null)
			generator = new EclipseGeneratorApplication();
		return generator;
	}

	public void setAppend(String value) {
		getProvider().setAppend(Boolean.valueOf(value).booleanValue());
	}

	public void setArtifactRepository(String location) {
		getGenerator().setArtifactLocation(location);
	}

	public void setBase(String value) {
		getGenerator().setBase(value);
	}

	public void setBundles(String value) {
		getProvider().setBundleLocations(new File[] {new File(value)});
	}

	public void setConfig(String value) {
		getGenerator().setOperation("-config", value); //$NON-NLS-1$
	}

	public void setInplace(String value) {
		getGenerator().setOperation("-inplace", value); //$NON-NLS-1$
	}

	public void setSource(String location) {
		getGenerator().setOperation("-source", location); //$NON-NLS-1$
	}

	public void setUpdateSite(String value) {
		getGenerator().setOperation("-update", value); //$NON-NLS-1$
	}

	public void setExe(String value) {
		getProvider().setExecutableLocation(value);
	}

	public void setFeatures(String value) {
		getProvider().setFeaturesLocation(new File(value));
	}

	public void setFlavor(String flavor) {
		getProvider().setFlavor(flavor);
	}

	public void setLauncherConfig(String launcherConfig) {
		getProvider().setLauncherConfig(launcherConfig);
	}

	public void setMetadataRepository(String location) {
		getGenerator().setMetadataLocation(location);
	}

	public void setNoDefaultIUs(String value) {
		getProvider().setAddDefaultIUs(!Boolean.valueOf(value).booleanValue());
	}

	public void setP2OS(String value) {
		getProvider().setOS(value);
	}

	public void setProductFile(String file) {
		getProvider().setProductFile(file);
	}

	public void setPublishArtifactRepository(String value) {
		getProvider().setPublishArtifactRepository(Boolean.valueOf(value).booleanValue());
	}

	public void setPublishArtifacts(String value) {
		getProvider().setPublishArtifacts(Boolean.valueOf(value).booleanValue());
	}

	public void setRoot(String root) {
		getProvider().setRootId(root);
	}

	public void setRootVersion(String rootVersion) {
		getProvider().setRootVersion(rootVersion);
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void setVersionAdvice(String advice) {
		if (provider == null)
			provider = new EclipseInstallGeneratorInfoProvider();
		provider.setVersionAdvice(advice);
	}
}
