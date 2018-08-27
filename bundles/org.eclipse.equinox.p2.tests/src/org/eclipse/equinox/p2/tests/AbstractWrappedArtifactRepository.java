/*******************************************************************************
 *  Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
import org.eclipse.equinox.p2.repository.artifact.*;

public class AbstractWrappedArtifactRepository implements IArtifactRepository {

	IArtifactRepository delegate;

	public AbstractWrappedArtifactRepository(IArtifactRepository repo) {
		delegate = repo;
	}

	@Override
	public void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		delegate.addDescriptor(descriptor, monitor);
	}

	@Override
	@Deprecated
	public void addDescriptor(IArtifactDescriptor descriptor) {
		delegate.addDescriptor(descriptor);
	}

	@Override
	public void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		delegate.addDescriptors(descriptors, monitor);
	}

	@Override
	@Deprecated
	public void addDescriptors(IArtifactDescriptor[] descriptors) {
		delegate.addDescriptors(descriptors);
	}

	@Override
	public boolean contains(IArtifactDescriptor descriptor) {
		return delegate.contains(descriptor);
	}

	@Override
	public boolean contains(IArtifactKey key) {
		return delegate.contains(key);
	}

	@Override
	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		return delegate.getArtifact(descriptor, destination, monitor);
	}

	@Override
	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		return delegate.getArtifactDescriptors(key);
	}

	@Override
	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		return delegate.getArtifacts(requests, monitor);
	}

	@Override
	public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
		return delegate.getOutputStream(descriptor);
	}

	@Override
	public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		return delegate.getRawArtifact(descriptor, destination, monitor);
	}

	@Override
	public void removeAll(IProgressMonitor monitor) {
		delegate.removeAll(monitor);
	}

	@Override
	@Deprecated
	public void removeAll() {
		delegate.removeAll();
	}

	@Override
	public void removeDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		delegate.removeDescriptor(descriptor, monitor);
	}

	@Override
	@Deprecated
	public void removeDescriptor(IArtifactDescriptor descriptor) {
		delegate.removeDescriptor(descriptor);
	}

	@Override
	public void removeDescriptor(IArtifactKey key, IProgressMonitor monitor) {
		delegate.removeDescriptor(key, monitor);
	}

	@Override
	@Deprecated
	public void removeDescriptor(IArtifactKey key) {
		delegate.removeDescriptor(key);
	}

	@Override
	public void removeDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		delegate.removeDescriptors(descriptors, monitor);
	}

	@Override
	@Deprecated
	public void removeDescriptors(IArtifactDescriptor[] descriptors) {
		delegate.removeDescriptors(descriptors);
	}

	@Override
	public void removeDescriptors(IArtifactKey[] keys, IProgressMonitor monitor) {
		delegate.removeDescriptors(keys, monitor);
	}

	@Override
	@Deprecated
	public void removeDescriptors(IArtifactKey[] keys) {
		delegate.removeDescriptors(keys);
	}

	@Override
	public String getDescription() {
		return delegate.getDescription();
	}

	@Override
	public URI getLocation() {
		return delegate.getLocation();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public Map<String, String> getProperties() {
		return delegate.getProperties();
	}

	@Override
	public String getProperty(String key) {
		return delegate.getProperty(key);
	}

	@Override
	public String getProvider() {
		return delegate.getProvider();
	}

	@Override
	public IProvisioningAgent getProvisioningAgent() {
		return delegate.getProvisioningAgent();
	}

	@Override
	public String getType() {
		return delegate.getType();
	}

	@Override
	public String getVersion() {
		return delegate.getVersion();
	}

	@Override
	public boolean isModifiable() {
		return delegate.isModifiable();
	}

	@Override
	public String setProperty(String key, String value, IProgressMonitor monitor) {
		return delegate.setProperty(key, value, monitor);
	}

	@Override
	public String setProperty(String key, String value) {
		return delegate.setProperty(key, value);
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return delegate.getAdapter(adapter);
	}

	@Override
	public IArtifactDescriptor createArtifactDescriptor(IArtifactKey key) {
		return delegate.createArtifactDescriptor(key);
	}

	@Override
	public IArtifactKey createArtifactKey(String classifier, String id, Version version) {
		return delegate.createArtifactKey(classifier, id, version);
	}

	@Override
	public IQueryable<IArtifactDescriptor> descriptorQueryable() {
		return delegate.descriptorQueryable();
	}

	@Override
	public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
		return delegate.query(query, monitor);
	}

	@Override
	public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
		return delegate.executeBatch(runnable, monitor);
	}
}
