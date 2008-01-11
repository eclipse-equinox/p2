/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.directorywatcher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.directorywatcher.DirectoryChangeListener;
import org.eclipse.equinox.p2.directorywatcher.DirectoryWatcher;
import org.eclipse.equinox.p2.metadata.generator.*;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;

public class ProvisioningListener extends DirectoryChangeListener {

	// The mapping rules for in-place generation need to construct paths that are flat,
	// with no nesting structure. 
	static final private String[][] INPLACE_MAPPING_RULES = { {"(& (namespace=eclipse) (classifier=feature))", "${repoUrl}/features/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=plugin))", "${repoUrl}/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=native))", "${repoUrl}/${id}_${version}"}}; //$NON-NLS-1$//$NON-NLS-2$

	private Set toUninstall;
	private Set toUpdate;
	private Set toInstall;
	private Set toGenerate;
	private DirectoryWatcher watcher;
	private Map seenFiles;

	public ProvisioningListener(DirectoryWatcher watcher) {
		this.watcher = watcher;
		seenFiles = new HashMap();
	}

	public boolean added(File file) {
		toInstall.add(file);
		if (file.getName().endsWith(".iu")) {
			// add the IU and artifact entries in to the repo associated with the watched dir
			// keep track of the JARs added so we can remove them from the list of JARs
			// to be run through the metadata generator
		} else if (file.getName().endsWith(".jar")) {
			// queue up this JAR to be run through the metadata generator if needed 
			toGenerate.add(file);
		}
		seenFiles.put(file, new Long(file.lastModified()));
		return true;
	}

	public boolean changed(File file) {
		toUpdate.add(file);
		if (file.getName().endsWith(".iu")) {
			// overwrite the IU and artifact entries in to the repo associated with the watched dir
			// keep track of the JARs added so we can remove them from the list of JARs
			// to be run through the metadata generator
		} else if (file.getName().endsWith(".jar")) {
			// queue up this JAR to be run through the metadata generator
			toGenerate.add(file);
		}
		seenFiles.put(file, new Long(file.lastModified()));
		return true;
	}

	public boolean removed(File file) {
		toUninstall.add(file);
		if (file.getName().endsWith(".iu")) {
			// remove the IU and artifact entries in to the repo associated with the watched dir
			// keep track of the JARs added so we can remove them from the list of JARs
			// to be run through the metadata generator
		} else if (file.getName().endsWith(".jar")) {
			// figure out which IU corresponds to this JAR and remove it
		}
		seenFiles.remove(file);
		return true;
	}

	public boolean isInterested(File file) {
		return file.getName().endsWith(".iu") || file.getName().endsWith(".jar");
	}

	private void initialize() {
		toUninstall = new HashSet(3);
		toUpdate = new HashSet(3);
		toInstall = new HashSet(3);
		toGenerate = new HashSet(3);
	}

	public void startPoll() {
		initialize();
	}

	public void stopPoll() {
		processIUs();
		generate();
		// 1) add/remove/update all the IUs and artifacts in the repo as required
		// 2) generate all the IUs we need to generate.  Here we have to sort out which
		// JARs are just JARs and which are artifacts that have associated IUs. Anything with 
		// an IU already does not need to be generated
		// reconcile the lists to ensure that the JAR
		// 3) construct the set of operations needed and call the engine
		// 4) kick something if needed
		initialize();
	}

	private IGeneratorInfo getProvider(File[] locations, File destination) {
		EclipseInstallGeneratorInfoProvider provider = new EclipseInstallGeneratorInfoProvider();
		provider.initialize(null, null, null, locations, null);
		try {
			initializeMetadataRepository(provider, destination.toURL());
			initializeArtifactRepository(provider, destination.toURL());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		provider.setPublishArtifactRepository(true);
		provider.setPublishArtifacts(false);
		provider.setMappingRules(INPLACE_MAPPING_RULES);
		return provider;
	}

	private void processIUs() {
	}

	private void generate() {
		File directory = watcher.getDirectory();
		IGeneratorInfo info = getProvider(new File[] {directory}, directory);
		new Generator(info).generate();
	}

	public Long getSeenFile(File file) {
		return (Long) seenFiles.get(file);
	}

	private void initializeArtifactRepository(EclipseInstallGeneratorInfoProvider provider, URL location) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		IArtifactRepository repository = manager.loadRepository(location, null);
		if (repository != null) {
			if (repository.isModifiable()) {
				provider.setArtifactRepository(repository);
				if (!provider.append())
					repository.removeAll();
				return;
			}
			throw new IllegalArgumentException("Artifact repository not writeable: " + location); //$NON-NLS-1$
		}

		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a Simple repo by default.
		String repositoryName = location + " - artifacts"; //$NON-NLS-1$
		IArtifactRepository result = manager.createRepository(location, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository"); //$NON-NLS-1$
		if (result != null)
			provider.setArtifactRepository(result);
	}

	private void initializeMetadataRepository(EclipseInstallGeneratorInfoProvider provider, URL location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		try {
			IMetadataRepository repository = manager.loadRepository(location, null);
			if (repository != null) {
				if (repository.isModifiable()) {
					provider.setMetadataRepository(repository);
					if (!provider.append())
						repository.removeAll();
					return;
				}
				throw new IllegalArgumentException("Artifact repository not writeable: " + location); //$NON-NLS-1$
			}
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		try {
			String repositoryName = location + " - metadata"; //$NON-NLS-1$
			IMetadataRepository repository = manager.createRepository(location, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
			provider.setMetadataRepository(repository);
		} catch (ProvisionException e) {
			LogHelper.log(e);
		}
	}

}
