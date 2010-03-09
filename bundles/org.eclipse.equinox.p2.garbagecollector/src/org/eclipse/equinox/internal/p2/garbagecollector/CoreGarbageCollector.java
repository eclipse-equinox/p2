/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.garbagecollector;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

/**
 * Given a MarkSet, the CoreGarbageCollector removes any IArtifactDescriptors which
 * are not mapped to be an IArtifactKey in the MarkSet.
 */
public class CoreGarbageCollector {

	/**
	 * When set to true, information will be logged every time an artifact is removed 
	 */
	static boolean debugMode = false;

	/**
	 * Given a list of IArtifactKeys and an IArtifactRepository, removes all artifacts
	 * in aRepository that are not mapped to by an IArtifactKey in markSet
	 */
	public synchronized void clean(IArtifactKey[] markSet, final IArtifactRepository aRepository) {
		Set<IArtifactKey> set = new HashSet<IArtifactKey>(Arrays.asList(markSet));
		//this query will match all artifact keys that are not in the given set
		IQuery<IArtifactKey> query = QueryUtil.createQuery(IArtifactKey.class, "unique($0)", set); //$NON-NLS-1$
		final IQueryResult<IArtifactKey> inactive = aRepository.query(query, null);
		aRepository.executeBatch(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				for (Iterator<IArtifactKey> iterator = inactive.iterator(); iterator.hasNext();) {
					IArtifactKey key = iterator.next();
					aRepository.removeDescriptor(key);
					if (debugMode) {
						Tracing.debug("Key removed:" + key); //$NON-NLS-1$
					}
				}
			}
		}, new NullProgressMonitor());
	}

	/*
	 * If set to true, debug mode will log information about each artifact deleted by the CoreGarbageCollector
	 * @param inDebugMode
	 */
	public static void setDebugMode(boolean inDebugMode) {
		debugMode = inDebugMode;
	}
}
