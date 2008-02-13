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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.equinox.internal.p2.metadata.generator.EclipseGeneratorApplication;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.EclipseInstallGeneratorInfoProvider;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.Generator.GeneratorResult;

/**
 * An Ant task to call the p2 Metadata Generator application.
 * 
 * @since 1.0
 */
public class GeneratorTask extends Task {

	protected EclipseInstallGeneratorInfoProvider provider = null;
	protected EclipseGeneratorApplication generator = null;

	static private GeneratorResult result;
	private String mode;

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		try {
			if ("incremental".equals(mode)) { //$NON-NLS-1$
				if (result == null)
					result = new GeneratorResult();
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

	public void setAppend(String value) {
		if (provider == null)
			provider = new EclipseInstallGeneratorInfoProvider();
		provider.setAppend(Boolean.valueOf(value).booleanValue());
	}

	public void setArtifactRepository(String location) {
		if (generator == null)
			generator = new EclipseGeneratorApplication();
		generator.setArtifactLocation(location);
	}

	public void setBase(String value) {
		if (generator == null)
			generator = new EclipseGeneratorApplication();
		generator.setBase(value);
	}

	public void setBundles(String value) {
		if (generator == null)
			generator = new EclipseGeneratorApplication();
		generator.setBundles(value);
	}

	public void setConfig(String value) {
		if (generator == null)
			generator = new EclipseGeneratorApplication();
		generator.setOperation("-config", value); //$NON-NLS-1$
	}

	public void setInplace(String value) {
		if (generator == null)
			generator = new EclipseGeneratorApplication();
		generator.setOperation("-inplace", value); //$NON-NLS-1$
	}

	public void setSource(String location) {
		if (generator == null)
			generator = new EclipseGeneratorApplication();
		generator.setOperation("-source", location); //$NON-NLS-1$
	}

	public void setUpdateSite(String value) {
		if (generator == null)
			generator = new EclipseGeneratorApplication();
		generator.setOperation("-update", value); //$NON-NLS-1$
	}

	public void setExe(String value) {
		if (provider == null)
			provider = new EclipseInstallGeneratorInfoProvider();
		provider.setExecutableLocation(value);
	}

	public void setFeatures(String value) {
		if (generator == null)
			generator = new EclipseGeneratorApplication();
		generator.setFeatures(value);
	}

	public void setFlavor(String flavor) {
		if (provider == null)
			provider = new EclipseInstallGeneratorInfoProvider();
		provider.setFlavor(flavor);
	}

	public void setLauncherConfig(String launcherConfig) {
		if (provider == null)
			provider = new EclipseInstallGeneratorInfoProvider();
		provider.setLauncherConfig(launcherConfig);
	}

	public void setMetadataRepository(String location) {
		if (generator == null)
			generator = new EclipseGeneratorApplication();
		generator.setMetadataLocation(location);
	}

	public void setNoDefaultIUs(String value) {
		if (provider == null)
			provider = new EclipseInstallGeneratorInfoProvider();
		provider.setAddDefaultIUs(Boolean.valueOf(value).booleanValue());
	}

	public void setP2OS(String value) {
		if (provider == null)
			provider = new EclipseInstallGeneratorInfoProvider();
		provider.setOS(value);
	}

	public void setProductFile(String file) {
		if (provider == null)
			provider = new EclipseInstallGeneratorInfoProvider();
		provider.setProductFile(file);
	}

	public void setPublishArtifactRepository(String value) {
		if (provider == null)
			provider = new EclipseInstallGeneratorInfoProvider();
		provider.setPublishArtifactRepository(Boolean.valueOf(value).booleanValue());
	}

	public void setPublishArtifacts(String value) {
		if (provider == null)
			provider = new EclipseInstallGeneratorInfoProvider();
		provider.setPublishArtifacts(Boolean.valueOf(value).booleanValue());
	}

	public void setRoot(String root) {
		if (provider == null)
			provider = new EclipseInstallGeneratorInfoProvider();
		provider.setRootId(root);
	}

	public void setRootVersion(String rootVersion) {
		if (provider == null)
			provider = new EclipseInstallGeneratorInfoProvider();
		provider.setRootVersion(rootVersion);
	}

	public void setMode(String mode) {
		this.mode = mode;
	}
}
