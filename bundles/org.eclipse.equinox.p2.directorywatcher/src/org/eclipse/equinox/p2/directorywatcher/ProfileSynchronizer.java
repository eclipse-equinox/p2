/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.p2.directorywatcher;

import java.util.*;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;

public class ProfileSynchronizer extends RepositoryUpdatedListener {

	private final Profile profile;
	private final IDirector director;

	public ProfileSynchronizer(Profile profile, IDirector director) {
		this.director = director;
		this.profile = profile;
	}

	public void updated(RepositoryUpdatedEvent event) {
		synchronizeProfile(event.getMetadataRepository());
	}

	private synchronized void synchronizeProfile(IMetadataRepository metadataRepository) {

		Map snapshot = new HashMap();
		for (Iterator it = profile.getInstallableUnits(); it.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) it.next();
			String fileName = iu.getProperty("file.name");
			if (fileName != null)
				snapshot.put(fileName, iu);
		}

		List toAdd = new ArrayList();

		IInstallableUnit[] ius = metadataRepository.getInstallableUnits(null);
		for (int i = 0; i < ius.length; i++) {
			IInstallableUnit iu = ius[i];
			String iuFileName = iu.getProperty("file.name");

			IInstallableUnit profileIU = (IInstallableUnit) snapshot.get(iuFileName);
			if (profileIU == null) {
				toAdd.add(iu);
				continue;
			}

			Long iuLastModified = new Long(iu.getProperty("file.lastModified"));
			Long profileIULastModified = new Long(profileIU.getProperty("file.lastModified"));
			if (iuLastModified.equals(profileIULastModified))
				snapshot.remove(iuFileName);
			else
				toAdd.add(iu);
		}

		if (snapshot.size() > 0) {
			IInstallableUnit[] iusToUninstall = (IInstallableUnit[]) snapshot.values().toArray(new IInstallableUnit[snapshot.size()]);
			director.uninstall(iusToUninstall, profile, null);
		}

		if (toAdd.size() > 0) {
			IInstallableUnit[] iusToInstall = (IInstallableUnit[]) toAdd.toArray(new IInstallableUnit[toAdd.size()]);
			director.install(iusToInstall, profile, null);
		}
	}
}
