/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.p2.reconciler.dropins;

import java.util.*;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;

public class ProfileSynchronizer {

	private static final String FILE_LAST_MODIFIED = "file.lastModified"; //$NON-NLS-1$
	private static final String FILE_NAME = "file.name"; //$NON-NLS-1$
	private static final String REPOSITORY_ID = "repository.id"; //$NON-NLS-1$
	private IInstallableUnit[] iusToRemove;
	private IInstallableUnit[] iusToAdd;

	public ProfileSynchronizer(Profile profile, IMetadataRepository metadataRepository) {
		String repositoryId = metadataRepository.getLocation().toExternalForm();
		Map snapshot = new HashMap();
		for (Iterator it = profile.getInstallableUnits(); it.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) it.next();
			if (repositoryId.equals(iu.getProperty(REPOSITORY_ID))) {
				String fileName = iu.getProperty(FILE_NAME);
				if (fileName != null)
					snapshot.put(fileName, iu);
			}
		}

		List toAdd = new ArrayList();

		IInstallableUnit[] ius = metadataRepository.getInstallableUnits(null);
		for (int i = 0; i < ius.length; i++) {
			IInstallableUnit iu = ius[i];
			String iuFileName = iu.getProperty(FILE_NAME);

			IInstallableUnit profileIU = (IInstallableUnit) snapshot.get(iuFileName);
			if (profileIU == null) {
				toAdd.add(iu);
				continue;
			}

			Long iuLastModified = new Long(iu.getProperty(FILE_LAST_MODIFIED));
			Long profileIULastModified = new Long(profileIU.getProperty(FILE_LAST_MODIFIED));
			if (iuLastModified.equals(profileIULastModified))
				snapshot.remove(iuFileName);
			else
				toAdd.add(iu);
		}

		if (!snapshot.isEmpty()) {
			iusToRemove = (IInstallableUnit[]) snapshot.values().toArray(new IInstallableUnit[snapshot.size()]);
		}

		if (!toAdd.isEmpty()) {
			iusToAdd = (IInstallableUnit[]) toAdd.toArray(new IInstallableUnit[toAdd.size()]);
		}
	}

	public IInstallableUnit[] getIUsToRemove() {
		return iusToRemove;
	}

	public IInstallableUnit[] getIUsToAdd() {
		return iusToAdd;
	}
}
