/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.p2.reconciler.dropins;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.configurator.Configurator;
import org.eclipse.equinox.internal.p2.reconciler.dropins.Activator;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.Query;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ProfileSynchronizer {

	private static final String FILE_LAST_MODIFIED = "file.lastModified"; //$NON-NLS-1$
	private static final String FILE_NAME = "file.name"; //$NON-NLS-1$
	private static final String REPOSITORY_ID = "repository.id"; //$NON-NLS-1$
	private IInstallableUnit[] iusToRemove;
	private IInstallableUnit[] iusToAdd;
	private Profile profile;
	private List repositories;

	/*
	 * Constructor for the class.
	 */
	public ProfileSynchronizer(Profile profile, List repositories) {
		super();
		this.profile = profile;
		this.repositories = repositories;
		initialize();
	}

	/*
	 * Initialize the synchronizer with default values.
	 */
	private void initialize() {
		// snapshot is a table of all the IUs from this repository which are installed in the profile 
		final Map snapshot = new HashMap();
		for (Iterator iter = repositories.iterator(); iter.hasNext();) {
			IMetadataRepository metadataRepository = (IMetadataRepository) iter.next();
			String repositoryId = metadataRepository.getLocation().toExternalForm();
			Iterator it = profile.query(InstallableUnitQuery.ANY, new Collector(), null).iterator();
			while (it.hasNext()) {
				IInstallableUnit iu = (IInstallableUnit) it.next();
				if (repositoryId.equals(iu.getProperty(REPOSITORY_ID))) {
					String fileName = iu.getProperty(FILE_NAME);
					if (fileName != null)
						snapshot.put(fileName, iu);
				}
			}
		}

		final List toAdd = new ArrayList();
		//create the collector that will visit all the IUs in the repositories being synchronized
		Collector syncCollector = new Collector() {
			public boolean accept(Object object) {
				IInstallableUnit iu = (IInstallableUnit) object;
				String iuFileName = iu.getProperty(FILE_NAME);
				// TODO is this right?
				if (iuFileName == null)
					return true;

				// if the repository contains an IU that the profile doesn't, then add it to the list to install
				IInstallableUnit profileIU = (IInstallableUnit) snapshot.get(iuFileName);
				if (profileIU == null) {
					toAdd.add(iu);
					return true;
				}

				Long iuLastModified = new Long(iu.getProperty(FILE_LAST_MODIFIED));
				Long profileIULastModified = new Long(profileIU.getProperty(FILE_LAST_MODIFIED));
				// TODO is this right?
				if (iuLastModified == null || profileIULastModified == null)
					return true;

				// if the timestamp hasn't changed, then there is nothing to do so remove
				// the IU from the snapshot so we don't accidentally remove it later
				if (iuLastModified.equals(profileIULastModified))
					snapshot.remove(iuFileName);
				else
					toAdd.add(iu);
				return true;
			}
		};

		Query query = new InstallableUnitQuery(null);
		for (Iterator it = repositories.iterator(); it.hasNext();) {
			IMetadataRepository repo = (IMetadataRepository) it.next();
			// TODO report progress
			repo.query(query, syncCollector, null);
		}

		// the IUs to remove is everything left that hasn't been removed from the snapshot
		if (!snapshot.isEmpty()) {
			iusToRemove = (IInstallableUnit[]) snapshot.values().toArray(new IInstallableUnit[snapshot.size()]);
		}

		// the list of IUs to add
		if (!toAdd.isEmpty()) {
			iusToAdd = (IInstallableUnit[]) toAdd.toArray(new IInstallableUnit[toAdd.size()]);
		}
	}

	/*
	 * Synchronize the profile with the list of metadata repositories.
	 */
	public void synchronize(IProgressMonitor monitor) {
		if (iusToRemove != null)
			removeIUs(iusToRemove, null); // TODO proper progress monitoring

		// disable repo cleanup for now until we see how we want to handle support for links folders and eclipse extensions
		//removeUnwatchedRepositories(context, profile, watchedFolder);

		if (iusToAdd != null)
			addIUs(iusToAdd, null); // TODO proper progress monitoring
		// if we did any work we have to apply the changes
		if (iusToAdd != null || iusToRemove != null)
			applyConfiguration();
	}

	/*
	 * Call the director to install the given list of IUs.
	 */
	private void addIUs(IInstallableUnit[] toAdd, IProgressMonitor monitor) {
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(IDirector.class.getName());
		IDirector director = (IDirector) context.getService(reference);
		try {
			director.install(toAdd, profile, new URL[0], monitor);
		} finally {
			context.ungetService(reference);
		}
	}

	/*
	 * Call the director to uninstall the given list of IUs.
	 */
	private void removeIUs(IInstallableUnit[] toRemove, IProgressMonitor monitor) {
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(IDirector.class.getName());
		IDirector director = (IDirector) context.getService(reference);
		try {
			director.uninstall(toRemove, profile, new URL[0], monitor);
		} finally {
			context.ungetService(reference);
		}
	}

	/*
	 * Write out the configuration file.
	 */
	private void applyConfiguration() {
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(Configurator.class.getName());
		Configurator configurator = (Configurator) context.getService(reference);
		try {
			configurator.applyConfiguration();
		} catch (IOException e) {
			// TODO unexpected -- log
			e.printStackTrace();
		} finally {
			context.ungetService(reference);
		}
	}

}
