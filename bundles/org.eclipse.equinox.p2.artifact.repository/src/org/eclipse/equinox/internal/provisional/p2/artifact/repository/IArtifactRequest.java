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
package org.eclipse.equinox.internal.provisional.p2.artifact.repository;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IArtifactRequest {

	public IArtifactKey getArtifactKey();

	/**
	 * Returns the result of the previous call to {@link ArtifactRequest#perform(IProgressMonitor)},
	 * or <code>null</code> if perform has never been called.
	 * 
	 * @return The result of the previous perform call.
	 */
	public IStatus getResult();

}
