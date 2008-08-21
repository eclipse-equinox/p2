/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.directorywatcher;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;

public class CachingArtifactRepository implements IArtifactRepository, IFileArtifactRepository {

	private static final String NULL = ""; //$NON-NLS-1$
	private IArtifactRepository innerRepo;
	private Set descriptorsToAdd = new HashSet();
	private Map artifactMap = new HashMap();
	private Set descriptorsToRemove = new HashSet();
	private Map propertyChanges = new HashMap();

	protected CachingArtifactRepository(IArtifactRepository innerRepo) {
		this.innerRepo = innerRepo;
	}

	public void save() {
		savePropertyChanges();
		saveAdditions();
		saveRemovals();
	}

	private void saveRemovals() {
		for (Iterator i = descriptorsToRemove.iterator(); i.hasNext();)
			innerRepo.removeDescriptor((IArtifactDescriptor) i.next());
		descriptorsToRemove.clear();
	}

	private void saveAdditions() {
		if (descriptorsToAdd.isEmpty())
			return;
		innerRepo.addDescriptors((IArtifactDescriptor[]) descriptorsToAdd.toArray(new IArtifactDescriptor[descriptorsToAdd.size()]));
		descriptorsToAdd.clear();
		artifactMap.clear();
	}

	private void savePropertyChanges() {
		for (Iterator i = propertyChanges.keySet().iterator(); i.hasNext();) {
			String key = (String) i.next();
			String value = (String) propertyChanges.get(key);
			innerRepo.setProperty(key, value == NULL ? null : value);
		}
		propertyChanges.clear();
	}

	private void mapDescriptor(IArtifactDescriptor descriptor) {
		IArtifactKey key = descriptor.getArtifactKey();
		Collection descriptors = (Collection) artifactMap.get(key);
		if (descriptors == null) {
			descriptors = new ArrayList();
			artifactMap.put(key, descriptors);
		}
		descriptors.add(descriptor);
	}

	private void unmapDescriptor(IArtifactDescriptor descriptor) {
		IArtifactKey key = descriptor.getArtifactKey();
		Collection descriptors = (Collection) artifactMap.get(key);
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

	public synchronized void addDescriptors(IArtifactDescriptor[] descriptors) {
		for (int i = 0; i < descriptors.length; i++) {
			((ArtifactDescriptor) descriptors[i]).setRepository(this);
			descriptorsToAdd.add(descriptors[i]);
			mapDescriptor(descriptors[i]);
		}
	}

	public synchronized void addDescriptor(IArtifactDescriptor toAdd) {
		((ArtifactDescriptor) toAdd).setRepository(this);
		descriptorsToAdd.add(toAdd);
		mapDescriptor(toAdd);
	}

	public synchronized IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		Collection result = (Collection) artifactMap.get(key);
		if (result == null)
			return innerRepo.getArtifactDescriptors(key);
		result.addAll(Arrays.asList(innerRepo.getArtifactDescriptors(key)));
		return (IArtifactDescriptor[]) result.toArray(new IArtifactDescriptor[result.size()]);
	}

	public synchronized IArtifactKey[] getArtifactKeys() {
		// there may be more descriptors than keys to collect up the unique keys
		HashSet result = new HashSet();
		result.addAll(artifactMap.keySet());
		result.addAll(Arrays.asList(innerRepo.getArtifactKeys()));
		return (IArtifactKey[]) result.toArray(new IArtifactKey[result.size()]);
	}

	public synchronized boolean contains(IArtifactDescriptor descriptor) {
		return descriptorsToAdd.contains(descriptor) || innerRepo.contains(descriptor);
	}

	public synchronized boolean contains(IArtifactKey key) {
		return artifactMap.containsKey(key) || innerRepo.contains(key);
	}

	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		return innerRepo.getArtifact(descriptor, destination, monitor);
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	public OutputStream getOutputStream(IArtifactDescriptor descriptor) {
		return null;
	}

	public synchronized void removeAll() {
		IArtifactDescriptor[] toRemove = (IArtifactDescriptor[]) descriptorsToAdd.toArray(new IArtifactDescriptor[descriptorsToAdd.size()]);
		for (int i = 0; i < toRemove.length; i++)
			doRemoveArtifact(toRemove[i]);
	}

	public synchronized void removeDescriptor(IArtifactDescriptor descriptor) {
		doRemoveArtifact(descriptor);
	}

	public synchronized void removeDescriptor(IArtifactKey key) {
		IArtifactDescriptor[] toRemove = getArtifactDescriptors(key);
		for (int i = 0; i < toRemove.length; i++)
			doRemoveArtifact(toRemove[i]);
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

	public String getDescription() {
		return innerRepo.getDescription();
	}

	public URL getLocation() {
		return innerRepo.getLocation();
	}

	public String getName() {
		return innerRepo.getName();
	}

	public Map getProperties() {
		// TODO need to combine the local and inner properties
		return innerRepo.getProperties();
	}

	public String getProvider() {
		return innerRepo.getProvider();
	}

	public String getType() {
		return innerRepo.getType();
	}

	public String getVersion() {
		return innerRepo.getVersion();
	}

	public boolean isModifiable() {
		return innerRepo.isModifiable();
	}

	public void setDescription(String description) {
		innerRepo.setDescription(description);
	}

	public void setName(String name) {
		innerRepo.setName(name);
	}

	public String setProperty(String key, String value) {
		String result = (String) getProperties().get(key);
		propertyChanges.put(key, value == null ? NULL : value);
		return result;
	}

	public void setProvider(String provider) {
		innerRepo.setProvider(provider);
	}

	public Object getAdapter(Class adapter) {
		return innerRepo.getAdapter(adapter);
	}

	public File getArtifactFile(IArtifactKey key) {
		if (innerRepo instanceof IFileArtifactRepository)
			return ((IFileArtifactRepository) innerRepo).getArtifactFile(key);
		return null;
	}

	public File getArtifactFile(IArtifactDescriptor descriptor) {
		if (innerRepo instanceof IFileArtifactRepository)
			return ((IFileArtifactRepository) innerRepo).getArtifactFile(descriptor);
		return null;
	}
}
