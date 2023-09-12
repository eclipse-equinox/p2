/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   Red Hat Inc. - Fix compiler problems from generified IAdaptable#getAdapter
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.directorywatcher;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.metadata.expression.CompoundIterator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;

public class CachingArtifactRepository implements IFileArtifactRepository {

	private static final String NULL = ""; //$NON-NLS-1$
	private IArtifactRepository innerRepo;
	private Set<IArtifactDescriptor> descriptorsToAdd = new HashSet<>();
	private Map<IArtifactKey, List<IArtifactDescriptor>> artifactMap = new HashMap<>();
	private Set<IArtifactDescriptor> descriptorsToRemove = new HashSet<>();
	private Map<String, String> propertyChanges = new HashMap<>();

	protected CachingArtifactRepository(IArtifactRepository innerRepo) {
		this.innerRepo = innerRepo;
	}

	public void save() {
		innerRepo.executeBatch(monitor -> {
			savePropertyChanges();
			saveAdditions();
			saveRemovals();
		}, null);
	}

	void saveRemovals() {
		for (IArtifactDescriptor desc : descriptorsToRemove)
			innerRepo.removeDescriptor(desc);
		descriptorsToRemove.clear();
	}

	void saveAdditions() {
		if (descriptorsToAdd.isEmpty())
			return;
		innerRepo.addDescriptors(descriptorsToAdd.toArray(new IArtifactDescriptor[descriptorsToAdd.size()]));
		descriptorsToAdd.clear();
		artifactMap.clear();
	}

	void savePropertyChanges() {
		for (String key : propertyChanges.keySet()) {
			String value = propertyChanges.get(key);
			innerRepo.setProperty(key, value == NULL ? null : value);
		}
		propertyChanges.clear();
	}

	private void mapDescriptor(IArtifactDescriptor descriptor) {
		IArtifactKey key = descriptor.getArtifactKey();
		List<IArtifactDescriptor> descriptors = artifactMap.get(key);
		if (descriptors == null) {
			descriptors = new ArrayList<>();
			artifactMap.put(key, descriptors);
		}
		descriptors.add(descriptor);
	}

	private void unmapDescriptor(IArtifactDescriptor descriptor) {
		IArtifactKey key = descriptor.getArtifactKey();
		List<IArtifactDescriptor> descriptors = artifactMap.get(key);
		if (descriptors == null) {
			// we do not have the descriptor locally so remember it to be removed from
			// the inner repo on save.
			descriptorsToRemove.add(descriptor);
			return;
		}

		descriptors.remove(descriptor);
		if (descriptors.isEmpty())
			artifactMap.remove(key);
	}

	@Override
	public synchronized void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, descriptors.length);
			for (IArtifactDescriptor descriptor : descriptors) {
				((ArtifactDescriptor) descriptor).setRepository(this);
				descriptorsToAdd.add(descriptor);
				mapDescriptor(descriptor);
				subMonitor.worked(1);
			}
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	@Override
	@Deprecated
	public final synchronized void addDescriptors(IArtifactDescriptor[] descriptors) {
		addDescriptors(descriptors, new NullProgressMonitor());
	}

	@Override
	public synchronized void addDescriptor(IArtifactDescriptor toAdd, IProgressMonitor monitor) {
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, 1);

			((ArtifactDescriptor) toAdd).setRepository(this);
			descriptorsToAdd.add(toAdd);
			mapDescriptor(toAdd);
			subMonitor.worked(1);
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	@Override
	@Deprecated
	public final synchronized void addDescriptor(IArtifactDescriptor toAdd) {
		addDescriptor(toAdd, new NullProgressMonitor());
	}

	@Override
	public synchronized IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		List<IArtifactDescriptor> result = artifactMap.get(key);
		if (result == null)
			return innerRepo.getArtifactDescriptors(key);
		result = new ArrayList<>(result);
		result.addAll(Arrays.asList(innerRepo.getArtifactDescriptors(key)));
		return result.toArray(new IArtifactDescriptor[result.size()]);
	}

	@Override
	public synchronized boolean contains(IArtifactDescriptor descriptor) {
		return descriptorsToAdd.contains(descriptor) || innerRepo.contains(descriptor);
	}

	@Override
	public synchronized boolean contains(IArtifactKey key) {
		return artifactMap.containsKey(key) || innerRepo.contains(key);
	}

	@Override
	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		return innerRepo.getArtifact(descriptor, destination, monitor);
	}

	@Override
	public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		return innerRepo.getRawArtifact(descriptor, destination, monitor);
	}

	@Override
	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	@Override
	public OutputStream getOutputStream(IArtifactDescriptor descriptor) {
		return null;
	}

	@Override
	public synchronized final void removeAll(IProgressMonitor monitor) {
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
			IArtifactDescriptor[] toRemove = descriptorsToAdd.toArray(new IArtifactDescriptor[descriptorsToAdd.size()]);
			for (IArtifactDescriptor toRemove1 : toRemove) {
				doRemoveArtifact(toRemove1);
			}
			subMonitor.worked(1);
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	@Override
	@Deprecated
	public synchronized void removeAll() {
		this.removeAll(new NullProgressMonitor());
	}

	@Override
	public synchronized void removeDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
			doRemoveArtifact(descriptor);
			subMonitor.worked(1);
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	@Override
	@Deprecated
	public final synchronized void removeDescriptor(IArtifactDescriptor descriptor) {
		removeDescriptor(descriptor, new NullProgressMonitor());
	}

	@Override
	public synchronized void removeDescriptor(IArtifactKey key, IProgressMonitor monitor) {
		try {
			IArtifactDescriptor[] toRemove = getArtifactDescriptors(key);
			SubMonitor subMonitor = SubMonitor.convert(monitor, toRemove.length);
			for (IArtifactDescriptor toRemove1 : toRemove) {
				doRemoveArtifact(toRemove1);
				subMonitor.worked(1);
			}
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	@Override
	@Deprecated
	public final synchronized void removeDescriptor(IArtifactKey key) {
		this.removeDescriptor(key, new NullProgressMonitor());
	}

	@Override
	public synchronized void removeDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, descriptors.length);
			for (IArtifactDescriptor descriptor : descriptors) {
				doRemoveArtifact(descriptor);
				subMonitor.worked(1);
			}
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	@Override
	@Deprecated
	public final synchronized void removeDescriptors(IArtifactDescriptor[] descriptors) {
		removeDescriptors(descriptors, new NullProgressMonitor());
	}

	@Override
	public synchronized void removeDescriptors(IArtifactKey[] keys, IProgressMonitor monitor) {
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, keys.length);
			for (IArtifactKey key : keys) {
				removeDescriptor(key);
				subMonitor.worked(1);
			}
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	@Override
	@Deprecated
	public final synchronized void removeDescriptors(IArtifactKey[] keys) {
		removeDescriptors(keys, new NullProgressMonitor());
	}

	/**
	 * Removes the given descriptor and returns <code>true</code> if and only if the
	 * descriptor existed in the repository, and was successfully removed.
	 */
	private boolean doRemoveArtifact(IArtifactDescriptor descriptor) {
		// if the descriptor is already in the pending additoins, remove it
		boolean result = descriptorsToAdd.remove(descriptor);
		if (result)
			unmapDescriptor(descriptor);
		// either way, note this as a descriptor to remove from the inner repo
		descriptorsToRemove.add(descriptor);
		return result;
	}

	@Override
	public String getDescription() {
		return innerRepo.getDescription();
	}

	@Override
	public URI getLocation() {
		return innerRepo.getLocation();
	}

	@Override
	public String getName() {
		return innerRepo.getName();
	}

	@Override
	public Map<String, String> getProperties() {
		// TODO need to combine the local and inner properties
		return innerRepo.getProperties();
	}

	@Override
	public String getProperty(String key) {
		return innerRepo.getProperty(key);
	}

	@Override
	public String getProvider() {
		return innerRepo.getProvider();
	}

	@Override
	public IProvisioningAgent getProvisioningAgent() {
		return innerRepo.getProvisioningAgent();
	}

	@Override
	public String getType() {
		return innerRepo.getType();
	}

	@Override
	public String getVersion() {
		return innerRepo.getVersion();
	}

	@Override
	public boolean isModifiable() {
		return innerRepo.isModifiable();
	}

	@Override
	public String setProperty(String key, String value, IProgressMonitor monitor) {
		try {
			String result = getProperties().get(key);
			propertyChanges.put(key, value == null ? NULL : value);
			return result;
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	@Override
	public final String setProperty(String key, String value) {
		return setProperty(key, value, new NullProgressMonitor());
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return innerRepo.getAdapter(adapter);
	}

	@Override
	public File getArtifactFile(IArtifactKey key) {
		if (innerRepo instanceof IFileArtifactRepository)
			return ((IFileArtifactRepository) innerRepo).getArtifactFile(key);
		return null;
	}

	@Override
	public File getArtifactFile(IArtifactDescriptor descriptor) {
		if (innerRepo instanceof IFileArtifactRepository)
			return ((IFileArtifactRepository) innerRepo).getArtifactFile(descriptor);
		return null;
	}

	@Override
	public IArtifactDescriptor createArtifactDescriptor(IArtifactKey key) {
		return innerRepo.createArtifactDescriptor(key);
	}

	@Override
	public IArtifactKey createArtifactKey(String classifier, String id, Version version) {
		return innerRepo.createArtifactKey(classifier, id, version);
	}

	@Override
	public IQueryable<IArtifactDescriptor> descriptorQueryable() {
		final Collection<List<IArtifactDescriptor>> descs = artifactMap.values();
		IQueryable<IArtifactDescriptor> cached = (query, monitor) -> query
				.perform(new CompoundIterator<>(descs.iterator()));

		return QueryUtil.compoundQueryable(cached, innerRepo.descriptorQueryable());
	}

	@Override
	public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
		final Iterator<IArtifactKey> keyIterator = artifactMap.keySet().iterator();
		IQueryable<IArtifactKey> cached = (q, mon) -> q.perform(keyIterator);

		IQueryable<IArtifactKey> compound = QueryUtil.compoundQueryable(cached, innerRepo);
		return compound.query(query, monitor);
	}

	@Override
	public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
		try {
			runnable.run(monitor);
		} catch (OperationCanceledException oce) {
			return new Status(IStatus.CANCEL, Constants.BUNDLE_ID, oce.getMessage(), oce);
		} catch (Exception e) {
			return new Status(IStatus.ERROR, Constants.BUNDLE_ID, e.getMessage(), e);
		}
		return Status.OK_STATUS;
	}

}
