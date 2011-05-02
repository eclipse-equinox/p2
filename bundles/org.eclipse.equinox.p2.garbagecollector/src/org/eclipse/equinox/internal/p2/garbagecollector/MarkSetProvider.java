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

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

/** 
 * Any class which declares itself as an extension to the <tt>org.eclipse.equinox.p2.garbagecollector.marksetproviders</tt>
 * extension point must extend this base class.  Given a Profile, implementors are required
 * to provide an array of MarkSet objects, each of which must contain an IArtifactRepository 
 * and the IArtifactKeys used by the given Profile.
 */
public abstract class MarkSetProvider {

	/**
	 * Returns a MarkSet for each bundle pool used by a Profile p.  The MarkSet will contain
	 * all of the IArtifactKeys found in p, as well as the IArtifactRepository over which the
	 * root set of keys is being created.
	 * @param profile A profile whose ArtifactRepositories require a garbage collection
	 * @return An array of MarkSet object(s) containing p's IArtifactRepository and its root set of IArtifactKeys
	 */
	public abstract MarkSet[] getMarkSets(IProvisioningAgent agent, IProfile profile);

	/**
	 * Returns the IArtifactRepository for which this MarkSetProvider provides a MarkSet.
	 * @param p The Profile whose IArtifactRepository is required
	 * @return The IArtifactRepository for which this MarkSetProvider provides a MarkSet.
	 */
	public abstract IArtifactRepository getRepository(IProvisioningAgent agent, IProfile p);

}
