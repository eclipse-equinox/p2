/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.eclipse.EclipseInstallAction;

/**
 * An Ant task to call the p2 publisher application.
 * 
 * @since 1.0
 */
public class PublisherTask extends AbstractPublishTask {

	protected boolean inplace = false;
	protected String[] configurations;
	protected String mode;
	private String flavor;
	private String operation;
	private String operationValue;
	private String root;
	private String rootVersion;
	private String versionAdvice;
	private String rootName;
	private String executableName;
	private IVersionedId[] topLevel;
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
		//TODO Do something with publisher result
		new Publisher(getInfo()).publish(actions, new NullProgressMonitor());
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
			return new IPublisherAction[] {new EclipseInstallAction(operationValue, root, Version.parseVersion(rootVersion), rootName, executableName, flavor, topLevel, nonRootFiles, start)};
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
			File sourceLocation = new File(source);
			if (metadataLocation == null)
				metadataLocation = sourceLocation.toURI();
			if (artifactLocation == null)
				artifactLocation = sourceLocation.toURI();
			info.setArtifactOptions(info.getArtifactOptions() | IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH);
		} else
			info.setArtifactOptions(info.getArtifactOptions() | IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH | IPublisherInfo.A_OVERWRITE);
		initializeRepositories(info);
	}

	public void setBase(String value) {
		source = value;
	}

	public void setBundles(String value) {
		//TODO Remove - currently exists for compatibility with generator task
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
		super.source = location;
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
		//TODO Remove - currently exists for compatibility with generator task
	}

	public void setFlavor(String value) {
		flavor = value;
	}

	/**
	 * @deprecated
	 */
	public void setLauncherConfig(String value) {
		//TODO Remove - currently exists for compatibility with generator task
	}

	public void setNoDefaultIUs(String value) {
		//TODO Remove - currently exists for compatibility with generator task
	}

	/**
	 * @deprecated
	 */
	public void setP2OS(String value) {
		//TODO Remove - currently exists for compatibility with generator task
	}

	public void setProductFile(String file) {
		//TODO Remove - currently exists for compatibility with generator task
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
