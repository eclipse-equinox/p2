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
package org.eclipse.equinox.prov.artifact.repository;

import org.eclipse.equinox.prov.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.prov.metadata.IArtifactKey;

public interface IArtifactDescriptor {

	public static final String DOWNLOAD_SIZE = "download.size"; //$NON-NLS-1$
	public static final String ARTIFACT_SIZE = "artifact.size"; //$NON-NLS-1$
	public static final String DOWNLOAD_MD5 = "download.md5"; //$NON-NLS-1$
	public static final String ARTIFACT_MD5 = "artifact.md5"; //$NON-NLS-1$

	public abstract IArtifactKey getArtifactKey();

	public abstract String getProperty(String key);

	public abstract ProcessingStepDescriptor[] getProcessingSteps();

}