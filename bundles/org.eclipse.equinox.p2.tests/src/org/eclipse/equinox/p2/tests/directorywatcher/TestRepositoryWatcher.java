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
package org.eclipse.equinox.p2.tests.directorywatcher;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryWatcher;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.BundleContext;

/**
 * Helper test class which wraps a repository listener.
 */
class TestRepositoryWatcher extends DirectoryWatcher {

	private RepositoryListener listener;

	/*
	 * Create and return a new test directory watcher class which will listen on the given folder.
	 */
	public static TestRepositoryWatcher createWatcher(File folder) {
		RepositoryListener listener = new RepositoryListener(AbstractProvisioningTest.getUniqueString(), false);
		Dictionary props = new Hashtable();
		props.put(DirectoryWatcher.DIR, folder.getAbsolutePath());
		props.put(DirectoryWatcher.POLL, "500");
		TestRepositoryWatcher result = new TestRepositoryWatcher(props, TestActivator.getContext());
		result.addListener(listener);
		return result;
	}

	/*
	 * Constructor for the class.
	 */
	private TestRepositoryWatcher(Dictionary props, BundleContext context) {
		super(props, context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryWatcher#addListener(org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener)
	 */
	public synchronized void addListener(RepositoryListener repoListener) {
		super.addListener(repoListener);
		this.listener = repoListener;
	}

	/*
	 * Return the list of all the IUs known to the metadata repository this watcher's listener.
	 */
	public IInstallableUnit[] getInstallableUnits() {
		return (IInstallableUnit[]) listener.getMetadataRepository().query(InstallableUnitQuery.ANY, new Collector(), null).toArray(IInstallableUnit.class);
	}

	/*
	 * Return the list of artifact keys known to this listener's repository.
	 */
	public IArtifactKey[] getArtifactKeys() {
		return listener.getArtifactRepository().getArtifactKeys();
	}

	/*
	 * Return the list of artifact descriptors known to this listener's repository.
	 */
	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		return listener.getArtifactRepository().getArtifactDescriptors(key);
	}

	/*
	 * Return the file associated with the given artifact key.
	 */
	public File getArtifactFile(IArtifactKey key) {
		return ((IFileArtifactRepository) listener.getArtifactRepository()).getArtifactFile(key);
	}
}