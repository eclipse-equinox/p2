/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.repository.tools.comparator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

/**
 * A comparator compares two artifacts for equivalence. The exact method
 * used to compare the artifacts depends on the comparator implementation used.
 * @since 2.0
 */
public interface IArtifactComparator {

	/**
	 * Compare 2 equivalent IArtifactDescriptors from different repositories.
	 * 
	 * IArtifactDescriptors with the same id and version should represent the same 
	 * set of bytes.  The comparator should ensure this is true and return an error
	 * or warning otherwise.
	 *
	 * @param source - The source IArtifactRepository
	 * @param sourceDescriptor - The IArtifactDescriptor from the source repository
	 * @param destination - The target IArtifactRepository
	 * @param destDescriptor - The IArtifactDescriptor from the target repository
	 *
	 * @return IStatus
	 */
	public IStatus compare(IArtifactRepository source, IArtifactDescriptor sourceDescriptor, IArtifactRepository destination, IArtifactDescriptor destDescriptor);
}
