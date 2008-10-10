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
package org.eclipse.equinox.internal.p2.publisher.ant;

import java.io.File;
import java.net.URI;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.eclipse.EclipseInstallAction;
import org.osgi.framework.Version;

/**
 * An Ant task to call the p2 Metadata Generator application.
 * 
 * @since 1.0
 */
public class PublisherTask extends Task {

	protected PublisherInfo provider = null;

	protected String source;
	protected URI metadataLocation;
	protected String metadataRepoName;
	protected URI artifactLocation;
	protected String artifactRepoName;
	protected boolean compress = false;
	protected boolean inplace = false;
	protected boolean append = false;
	protected boolean reusePackedFiles = false;
	protected String[] configurations;
	protected String mode;
	private File[] bundles;
	private File[] features;
	private String productFile;
	private String flavor;
	private String operation;
	private String operationValue;
	private boolean addDefaultIUs;
	private String root;
	private String rootVersion;
	private String versionAdvice;
	private String rootName;
	private String executableName;
	private String[] topLevel;
	private boolean start;
	private String[] nonRootFiles;

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		try {
			initialize(getInfo());
		} catch (ProvisionException e) {
			throw new BuildException("Unable to configure repositories", e); //$NON-NLS-1$
		}
		createVersionAdvice();
		IPublisherAction[] actions = createActions();
		IStatus result = new Publisher(getInfo()).publish(actions, new NullProgressMonitor());

		// TODO hack assignments to keep the compiler from whining about the unreferenced privates.
		Object o = bundles;
		o = features;
		o = productFile;
		boolean b = addDefaultIUs;

		//		try {
		//			if ("incremental".equals(mode)) { //$NON-NLS-1$
		//				if (result == null)
		//					result = new PublisherResult();
		//				generator.setIncrementalResult(result);
		//				generator.setGeneratorRootIU(false);
		//			} else if ("final".equals(mode) && result != null) { //$NON-NLS-1$
		//				generator.setIncrementalResult(result);
		//				generator.setGeneratorRootIU(true);
		//			}
		//
		//			generator.run(provider);
		//
		//			if (!"incremental".equals(mode)) { //$NON-NLS-1$
		//				provider = null;
		//				generator = null;
		//				result = null;
		//			}
		//		} catch (Exception e) {
		//			throw new BuildException(TaskMessages.exception_errorOccurredCallingGenerator, e);
		//		}
	}

	private IPublisherAction[] createActions() {
		if (operation == null)
			// TODO what to do in this case?
			return new IPublisherAction[] {};
		if (operation.equals("-update")) //$NON-NLS-1$
			// TODO fix this up.  watch for circularities
			//			return new IPublishingAction[] {new LocalUpdateSiteAction(operationValue)};
			return new IPublisherAction[] {};
		if (operation.equals("-source")) //$NON-NLS-1$
			// TODO what to do in this case?
			return new IPublisherAction[] {new EclipseInstallAction(operationValue, root, new Version(rootVersion), rootName, executableName, flavor, topLevel, nonRootFiles, start)};
		// TODO what to do in this case?
		return new IPublisherAction[] {};
	}

	private void createVersionAdvice() {
		if (versionAdvice == null)
			return;
		// TODO read the version advice and add the IVersionAdvice
	}

	protected void initialize(PublisherInfo info) throws ProvisionException {
		if (inplace) {
			File location = new File(source);
			if (metadataLocation == null)
				metadataLocation = location.toURI();
			if (artifactLocation == null)
				artifactLocation = location.toURI();
			info.setArtifactOptions(info.getArtifactOptions() | IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH);
		} else
			info.setArtifactOptions(info.getArtifactOptions() | IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH | IPublisherInfo.A_OVERWRITE);
		initializeRepositories(info);
	}

	protected void initializeRepositories(PublisherInfo info) throws ProvisionException {
		info.setArtifactRepository(Publisher.createArtifactRepository(artifactLocation, artifactRepoName, append, compress, reusePackedFiles));
		info.setMetadataRepository(Publisher.createMetadataRepository(metadataLocation, metadataRepoName, append, compress));
	}

	private PublisherInfo getInfo() {
		if (provider == null)
			provider = new PublisherInfo();
		return provider;
	}

	public void setAppend(String value) {
		append = Boolean.valueOf(value).booleanValue();
	}

	public void setArtifactRepository(URI location) {
		artifactLocation = location;
	}

	public void setArtifactRepositoryName(String value) {
		artifactRepoName = value;
	}

	public void setBase(String value) {
		source = value;
	}

	public void setBundles(String value) {
		bundles = new File[] {new File(value)};
	}

	public void setCompress(String value) {
		compress = Boolean.valueOf(value).booleanValue();
	}

	public void setConfig(String value) {
		operation = "-config"; //$NON-NLS-1$
		operationValue = value;
	}

	public void setInplace(String value) {
		operation = "-inplace"; //$NON-NLS-1$
		operationValue = value;
	}

	public void setSource(String location) {
		operation = "-source"; //$NON-NLS-1$
		operationValue = location;
	}

	public void setUpdateSite(String value) {
		operation = "-update"; //$NON-NLS-1$
		operationValue = value;
	}

	/**
	 * @deprecated
	 */
	public void setExe(String value) {
		executableName = value;
	}

	public void setFeatures(String value) {
		features = new File[] {new File(value)};
	}

	public void setFlavor(String value) {
		flavor = value;
	}

	/**
	 * @deprecated
	 */
	public void setLauncherConfig(String value) {
	}

	public void setMetadataRepository(URI location) {
		metadataLocation = location;
	}

	public void setMetadataRepositoryName(String value) {
		metadataRepoName = value;
	}

	public void setNoDefaultIUs(String value) {
		addDefaultIUs = !Boolean.valueOf(value).booleanValue();
	}

	/**
	 * @deprecated
	 */
	public void setP2OS(String value) {
	}

	public void setProductFile(String file) {
		productFile = file;
	}

	public void setPublishArtifactRepository(String value) {
		getInfo().setArtifactOptions(getInfo().getArtifactOptions() | IPublisherInfo.A_INDEX);
	}

	public void setPublishArtifacts(String value) {
		getInfo().setArtifactOptions(getInfo().getArtifactOptions() | IPublisherInfo.A_PUBLISH);
	}

	public void setRoot(String value) {
		root = value;
	}

	public void setRootVersion(String value) {
		rootVersion = value;
	}

	public void setMode(String value) {
		mode = value;
	}

	public void setVersionAdvice(String value) {
		versionAdvice = value;
	}
}
