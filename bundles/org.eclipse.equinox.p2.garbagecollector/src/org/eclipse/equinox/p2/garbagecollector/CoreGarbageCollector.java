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
package org.eclipse.equinox.p2.garbagecollector;

import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

/**
 * Given a MarkSet, the CoreGarbageCollector removes any IArtifactDescriptors which
 * are not mapped to be an IArtifactKey in the MarkSet.
 */
public class CoreGarbageCollector {

	/**
	 * When set to true, information will be logged every time an artifact is removed 
	 */
	private static boolean debugMode = false;

	/**
	 * Given a list of IArtifactKeys and an IArtifactRepository, removes all artifacts
	 * in aRepository that are not mapped to by an IArtifactKey in markSet
	 */
	public synchronized void clean(IArtifactKey[] markSet, IArtifactRepository aRepository) {
		IArtifactKey[] repositoryKeys = aRepository.getArtifactKeys();
		for (int j = 0; j < repositoryKeys.length; j++) {
			boolean artifactIsActive = false;
			for (int k = 0; k < markSet.length; k++) {
				if (repositoryKeys[j].equals(markSet[k])) {
					artifactIsActive = true;
					break;
				}
			}
			if (!artifactIsActive) {
				aRepository.removeDescriptor(repositoryKeys[j]);
				if (debugMode) {
					Tracing.debug("Key removed:" + repositoryKeys[j]); //$NON-NLS-1$
				}
			}
		}
	}

	/*
	 * If set to true, debug mode will log information about each artifact deleted by the CoreGarbageCollector
	 * @param inDebugMode
	 */
	public static void setDebugMode(boolean inDebugMode) {
		debugMode = inDebugMode;
	}
}
