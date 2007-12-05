package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.util.*;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.garbagecollector.IMarkSetProvider;
import org.eclipse.equinox.p2.garbagecollector.MarkSet;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * IMarkSetProvider implementation for the EclipseTouchPoint.
 */
public class MarkSetProvider implements IMarkSetProvider {

	private Collection artifactKeyList = null;

	public MarkSet[] getMarkSets(Profile inProfile) {
		artifactKeyList = new HashSet();
		IArtifactRepository repositoryToGC = Util.getBundlePoolRepository(inProfile);
		addArtifactKeys(inProfile);
		return new MarkSet[] {new MarkSet((IArtifactKey[]) artifactKeyList.toArray(new IArtifactKey[0]), repositoryToGC)};
	}

	private void addArtifactKeys(Profile aProfile) {
		Iterator installableUnits = aProfile.getInstallableUnits();
		while (installableUnits.hasNext()) {
			IArtifactKey[] keys = ((IInstallableUnit) installableUnits.next()).getArtifacts();
			if (keys == null)
				return;
			for (int i = 0; i < keys.length; i++) {
				artifactKeyList.add(keys[i]);
			}
		}
	}

	public IArtifactRepository getRepository(Profile aProfile) {
		return Util.getBundlePoolRepository(aProfile);
	}
}
