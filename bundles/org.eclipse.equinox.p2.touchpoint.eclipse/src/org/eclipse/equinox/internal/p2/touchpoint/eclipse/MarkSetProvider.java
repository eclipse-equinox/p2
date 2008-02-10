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
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.util.*;
import org.eclipse.equinox.internal.p2.garbagecollector.IMarkSetProvider;
import org.eclipse.equinox.internal.p2.garbagecollector.MarkSet;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;

/**
 * IMarkSetProvider implementation for the EclipseTouchPoint.
 */
public class MarkSetProvider implements IMarkSetProvider {

	private Collection artifactKeyList = null;

	public MarkSet[] getMarkSets(IProfile inProfile) {
		artifactKeyList = new HashSet();
		IArtifactRepository repositoryToGC = Util.getBundlePoolRepository(inProfile);
		addArtifactKeys(inProfile);
		return new MarkSet[] {new MarkSet((IArtifactKey[]) artifactKeyList.toArray(new IArtifactKey[0]), repositoryToGC)};
	}

	private void addArtifactKeys(IProfile aProfile) {
		Iterator installableUnits = aProfile.query(InstallableUnitQuery.ANY, new Collector(), null).iterator();
		while (installableUnits.hasNext()) {
			IArtifactKey[] keys = ((IInstallableUnit) installableUnits.next()).getArtifacts();
			if (keys == null)
				return;
			for (int i = 0; i < keys.length; i++) {
				artifactKeyList.add(keys[i]);
			}
		}
	}

	public IArtifactRepository getRepository(IProfile aProfile) {
		return Util.getBundlePoolRepository(aProfile);
	}
}
