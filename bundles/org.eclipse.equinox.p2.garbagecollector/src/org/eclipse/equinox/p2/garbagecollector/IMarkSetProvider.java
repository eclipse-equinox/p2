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

import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.engine.Profile;

/** 
 * Any class which declares itself as an extension to the org.eclipse.equinox.p2.garbagecollector.marksetproviders
 * extension point must implement this interface.  Given a Profile, implementors will need to provide an array of
 * MarkSet objects, each of which should contain an IArtifactRepository and the IArtifactKeys used by the given
 * Profile.
 */
public interface IMarkSetProvider {
	
	/**
	 * Returns a MarkSet for each bundle pool used by a Profile p.  The MarkSet will contain
	 * all of the IArtifactKeys found in p, as well as the IArtifactRepository over which the
	 * root set of keys is being created.
	 * @param p A profile whose ArtifactRepositories require a garbage collection
	 * @return An array of MarkSet object(s) containing p's IArtifactRepository and its root set of IArtifactKeys
	 */
	public MarkSet[] getMarkSets(Profile p);
	
	/**
	 * Returns the IArtifactRepository for which this IMarkSetProvider provides a MarkSet.
	 * @param p The Profile whose IArtifactRepository is required
	 * @return The IArtifactRepository for which this IMarkSetProvider provides a MarkSet.
	 */
	public IArtifactRepository getRepository(Profile p);

}
