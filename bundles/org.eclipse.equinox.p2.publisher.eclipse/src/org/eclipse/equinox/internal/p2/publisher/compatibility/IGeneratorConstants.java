/*******************************************************************************
 *  Copyright (c) 2010, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.publisher.compatibility;

/**
 * @since 1.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IGeneratorConstants {
	public static final String DASH = "-"; //$NON-NLS-1$

	//arguments understood by the old metadata generator application
	public static final String PUBLISH_ARTIFACTS = "-publishArtifacts"; //$NON-NLS-1$
	public static final String PA = "-pa"; //$NON-NLS-1$
	public static final String PUBLISH_ATIFACT_REPOSITORY = "-publishArtifactRepository"; //$NON-NLS-1$
	public static final String PAR = "-par"; //$NON-NLS-1$
	public static final String APPEND = "-append"; //$NON-NLS-1$
	public static final String NO_DEFAULT_IUS = "-noDefaultIUs"; //$NON-NLS-1$
	public static final String COMPRESS = "-compress"; //$NON-NLS-1$
	public static final String REUSE_PACK200 = "-reusePack200Files"; //$NON-NLS-1$
	public static final String SOURCE = "-source"; //$NON-NLS-1$
	public static final String INPLACE = "-inplace"; //$NON-NLS-1$
	public static final String CONFIG = "-config"; //$NON-NLS-1$
	public static final String UPDATE_SITE = "-updateSite"; //$NON-NLS-1$
	public static final String EXE = "-exe"; //$NON-NLS-1$
	public static final String LAUNCHER_CONFIG = "-launcherConfig"; //$NON-NLS-1$
	public static final String METADATA_REPO_NAME = "-metadataRepositoryName"; //$NON-NLS-1$
	public static final String METADATA_REPO = "-metadataRepository"; //$NON-NLS-1$
	public static final String MR = "-mr"; //$NON-NLS-1$
	public static final String ARTIFACT_REPO = "-artifactRepository"; //$NON-NLS-1$
	public static final String AR = "-ar"; //$NON-NLS-1$
	public static final String ARTIFACT_REPO_NAME = "-artifactRepositoryName"; //$NON-NLS-1$
	public static final String FLAVOR = "-flavor"; //$NON-NLS-1$
	public static final String PRODUCT_FILE = "-productFile"; //$NON-NLS-1$
	public static final String FEATURES = "-features"; //$NON-NLS-1$
	public static final String BUNDLES = "-bundles"; //$NON-NLS-1$
	public static final String BASE = "-base"; //$NON-NLS-1$
	public static final String ROOT = "-root"; //$NON-NLS-1$
	public static final String ROOT_VERSION = "-rootVersion"; //$NON-NLS-1$
	public static final String P2_OS = "-p2.os"; //$NON-NLS-1$
	public static final String SITE = "-site"; //$NON-NLS-1$
	public static final String IU = "-iu"; //$NON-NLS-1$
	public static final String ID = "-id"; //$NON-NLS-1$
	public static final String VERSION = "-version"; //$NON-NLS-1$
}
