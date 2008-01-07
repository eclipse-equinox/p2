/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite.metadata;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.Query;
import org.eclipse.equinox.p2.updatesite.Activator;
import org.eclipse.equinox.spi.p2.core.repository.AbstractRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class UpdateSiteMetadataRepository extends AbstractRepository implements IMetadataRepository {

	private final IMetadataRepository metadataRepository;

	public UpdateSiteMetadataRepository(URL location) {
		super("update site: " + location.toExternalForm(), null, null, location, null, null);
		BundleContext context = Activator.getBundleContext();

		URL localRepositoryURL = null;
		try {
			String stateDirName = Integer.toString(location.toExternalForm().hashCode());
			File bundleData = context.getDataFile(null);
			File stateDir = new File(bundleData, stateDirName);
			localRepositoryURL = stateDir.toURL();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		metadataRepository = initializeMetadataRepository(context, localRepositoryURL, "update site implementation - " + location.toExternalForm());
	}

	private IMetadataRepository initializeMetadataRepository(BundleContext context, URL stateDirURL, String repositoryName) {
		ServiceReference reference = context.getServiceReference(IMetadataRepositoryManager.class.getName());
		if (reference == null)
			throw new IllegalStateException("MetadataRepositoryManager not registered.");

		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException("MetadataRepositoryManager not registered.");

		IMetadataRepository repository = null;
		try {
			repository = manager.loadRepository(stateDirURL, null);
			if (repository == null) {
				repository = manager.createRepository(stateDirURL, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
				repository.setProperty(IRepository.IMPLEMENTATION_ONLY_KEY, Boolean.TRUE.toString());
			}
		} finally {
			context.ungetService(reference);
		}

		if (repository == null)
			throw new IllegalStateException("Couldn't create metadata repository for: " + repositoryName);

		return repository;
	}

	public Map getProperties() {
		Map result = new HashMap(metadataRepository.getProperties());
		result.remove(IRepository.IMPLEMENTATION_ONLY_KEY);

		return result;
	}

	public String setProperty(String key, String value) {
		return metadataRepository.setProperty(key, value);
	}

	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		return metadataRepository.query(query, collector, monitor);
	}

	public void removeAll() {
		metadataRepository.removeAll();
	}

	public void addInstallableUnits(IInstallableUnit[] installableUnits) {
		metadataRepository.addInstallableUnits(installableUnits);
	}

	public boolean removeInstallableUnits(Query query, IProgressMonitor monitor) {
		return metadataRepository.removeInstallableUnits(query, monitor);
	}

}
