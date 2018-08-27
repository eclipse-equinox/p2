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
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRequest;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.util.NLS;

public class TestArtifactRepository implements IArtifactRepository {
	private static String provider = null;
	private HashMap<IArtifactDescriptor, byte[]> repo;
	private String name;
	private String description;
	private String version = "1.0.0"; //$NON-NLS-1$
	protected Map<String, String> properties = new OrderedProperties();

	public class ArtifactOutputStream extends OutputStream implements IStateful {
		private boolean closed;
		private long count = 0;
		private IArtifactDescriptor descriptor;
		private OutputStream destination;
		private IStatus status = Status.OK_STATUS;
		private OutputStream firstLink;

		public ArtifactOutputStream(OutputStream os, IArtifactDescriptor descriptor) {
			this.destination = os;
			this.descriptor = descriptor;
		}

		@Override
		public void close() throws IOException {
			if (closed)
				return;
			try {
				destination.close();
				closed = true;
			} catch (IOException e) {
				if (getStatus().isOK())
					throw e;
				// if the stream has already been e.g. canceled, we can return -
				// the status is already set correctly
				return;
			}
			// if the steps ran ok and there was actual content, write the
			// artifact descriptor
			// TODO the count check is a bit bogus but helps in some error cases
			// (e.g., the optimizer)
			// where errors occurred in a processing step earlier in the chain.
			// We likely need a better
			// or more explicit way of handling this case.
			OutputStream testStream = firstLink == null ? this : firstLink;
			if (ProcessingStepHandler.checkStatus(testStream).isOK() && count > 0) {
				((ArtifactDescriptor) descriptor).setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(count));
				addDescriptor(descriptor, ((ByteArrayOutputStream) destination).toByteArray());
			}
		}

		@Override
		public IStatus getStatus() {
			return status;
		}

		public OutputStream getDestination() {
			return destination;
		}

		@Override
		public void setStatus(IStatus status) {
			this.status = status == null ? Status.OK_STATUS : status;
		}

		@Override
		public void write(byte[] b) throws IOException {
			destination.write(b);
			count += b.length;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			destination.write(b, off, len);
			count += len;
		}

		@Override
		public void write(int b) throws IOException {
			destination.write(b);
			count++;
		}

		public void setFirstLink(OutputStream value) {
			firstLink = value;
		}
	}

	private final IProvisioningAgent agent;

	public TestArtifactRepository(IProvisioningAgent agent) {
		this.agent = agent;
		repo = new HashMap<>();
	}

	@Override
	public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
		// Check if the artifact is already in this repository
		if (contains(descriptor)) {
			String msg = NLS.bind(Messages.available_already_in, getLocation());
			throw new ProvisionException(new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, ProvisionException.ARTIFACT_EXISTS, msg, null));
		}
		return new ArtifactOutputStream(new ByteArrayOutputStream(500), descriptor);
	}

	@Override
	public void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		addDescriptor(descriptor, new byte[0]);
	}

	@Override
	@Deprecated
	public final void addDescriptor(IArtifactDescriptor descriptor) {
		this.addDescriptor(descriptor, new NullProgressMonitor());
	}

	public void addDescriptor(IArtifactDescriptor descriptor, byte[] bytes) {
		repo.put(descriptor, bytes);
	}

	@Override
	public void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		for (int i = 0; i < descriptors.length; i++)
			addDescriptor(descriptors[i]);
	}

	@Override
	@Deprecated
	public final void addDescriptors(IArtifactDescriptor[] descriptors) {
		this.addDescriptors(descriptors, new NullProgressMonitor());
	}

	@Override
	public boolean contains(IArtifactDescriptor descriptor) {
		return repo.containsKey(descriptor);
	}

	@Override
	public synchronized boolean contains(IArtifactKey key) {
		for (IArtifactDescriptor descriptor : repo.keySet()) {
			if (descriptor.getArtifactKey().equals(key))
				return true;
		}
		return false;
	}

	@Override
	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		try {
			byte[] repoContents = repo.get(descriptor);
			if (repoContents == null)
				return new Status(IStatus.ERROR, "test", "no such artifact");
			destination.write(repoContents);
		} catch (IOException e) {
			e.printStackTrace();
			return new Status(IStatus.ERROR, "test", "exception occurred", e);
		}
		return Status.OK_STATUS;
	}

	@Override
	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		Set<IArtifactDescriptor> result = new HashSet<>();
		for (IArtifactDescriptor descriptor : repo.keySet()) {
			if (descriptor.getArtifactKey().equals(key))
				result.add(descriptor);
		}
		return result.toArray(new IArtifactDescriptor[0]);
	}

	@Override
	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
		try {
			//plugin ID taken from TestActivator
			MultiStatus overallStatus = new MultiStatus("org.eclipse.equinox.p2.test", IStatus.OK, null, null); //$NON-NLS-1$
			for (int i = 0; i < requests.length; i++) {
				overallStatus.add(getArtifact((ArtifactRequest) requests[i], subMonitor.newChild(1)));
			}
			return (monitor.isCanceled() ? Status.CANCEL_STATUS : overallStatus);
		} finally {
			subMonitor.done();
		}
	}

	private IStatus getArtifact(ArtifactRequest artifactRequest, IProgressMonitor monitor) {
		artifactRequest.perform(this, monitor);
		return artifactRequest.getResult();
	}

	@Override
	public void removeDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		repo.remove(descriptor);
	}

	@Override
	@Deprecated
	public final void removeDescriptor(IArtifactDescriptor descriptor) {
		this.removeDescriptor(descriptor, new NullProgressMonitor());
	}

	@Override
	public void removeDescriptor(IArtifactKey key, IProgressMonitor monitor) {
		ArrayList<IArtifactDescriptor> removeList = new ArrayList<>();
		for (Iterator<IArtifactDescriptor> iterator = repo.keySet().iterator(); iterator.hasNext();) {
			IArtifactDescriptor descriptor = iterator.next();
			if (descriptor.getArtifactKey().equals(key))
				removeList.add(descriptor);
		}
		for (int i = 0; i < repo.size(); i++) {
			repo.remove(removeList.get(i));
		}
	}

	@Override
	@Deprecated
	public final void removeDescriptor(IArtifactKey key) {
		removeDescriptor(key, new NullProgressMonitor());
	}

	@Override
	public void removeDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		for (IArtifactDescriptor descriptor : descriptors)
			removeDescriptor(descriptor);
	}

	@Override
	@Deprecated
	public final void removeDescriptors(IArtifactDescriptor[] descriptors) {
		this.removeDescriptors(descriptors, new NullProgressMonitor());
	}

	@Override
	public void removeDescriptors(IArtifactKey[] keys, IProgressMonitor monitor) {
		for (IArtifactKey key : keys)
			removeDescriptor(key);
	}

	@Override
	@Deprecated
	public final void removeDescriptors(IArtifactKey[] keys) {
		this.removeDescriptors(keys, new NullProgressMonitor());
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public URI getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Map<String, String> getProperties() {
		return OrderedProperties.unmodifiableProperties(properties);
	}

	@Override
	public String getProperty(String key) {
		return properties.get(key);
	}

	@Override
	public String getProvider() {
		return provider;
	}

	@Override
	public IProvisioningAgent getProvisioningAgent() {
		return agent;
	}

	@Override
	public String getType() {
		return "memoryArtifactRepo"; //$NON-NLS-1$
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public boolean isModifiable() {
		return true;
	}

	public void setDescription(String value) {
		this.description = value;
	}

	public void setName(String value) {
		this.name = value;
	}

	@Override
	public String setProperty(String key, String value, IProgressMonitor monitor) {
		return (value == null ? properties.remove(key) : properties.put(key, value));
	}

	@Override
	public final String setProperty(String key, String value) {
		return this.setProperty(key, value, new NullProgressMonitor());
	}

	public void setProvider(String value) {
		provider = value;
	}

	@Override
	public void removeAll(IProgressMonitor monitor) {
		repo.clear();
	}

	@Override
	@Deprecated
	public final void removeAll() {
		removeAll(new NullProgressMonitor());
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	public ZipInputStream getZipInputStream(IArtifactKey key) {
		//get first descriptor with key
		IArtifactDescriptor[] descriptor = getArtifactDescriptors(key);
		if (descriptor == null || descriptor.length == 0 || descriptor[0] == null)
			return null;
		return new ZipInputStream(getRawInputStream(descriptor[0]));
	}

	public InputStream getRawInputStream(IArtifactDescriptor descriptor) {
		return new ByteArrayInputStream(repo.get(descriptor), 0, repo.get(descriptor).length);
	}

	public ZipInputStream getZipInputStream(IArtifactDescriptor descriptor) {
		return new ZipInputStream(getRawInputStream(descriptor));
	}

	public byte[] getBytes(IArtifactDescriptor artifactDescriptor) {
		return repo.get(artifactDescriptor);
	}

	@Override
	public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		return getArtifact(descriptor, destination, monitor);
	}

	@Override
	public IArtifactDescriptor createArtifactDescriptor(IArtifactKey key) {
		return new ArtifactDescriptor(key);
	}

	@Override
	public IArtifactKey createArtifactKey(String classifier, String id, Version keyVersion) {
		return new ArtifactKey(classifier, id, keyVersion);
	}

	@Override
	public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled())
			return Collector.emptyCollector();

		Collector<IArtifactKey> collector = new Collector<>();
		for (IArtifactDescriptor descriptor : repo.keySet()) {
			collector.accept(descriptor.getArtifactKey());
		}
		return collector;
	}

	@Override
	public IQueryable<IArtifactDescriptor> descriptorQueryable() {
		final Collection<IArtifactDescriptor> descs = repo.keySet();
		return (query, monitor) -> query.perform(descs.iterator());
	}

	@Override
	public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
		try {
			runnable.run(monitor);
		} catch (OperationCanceledException oce) {
			return new Status(IStatus.CANCEL, "org.eclipse.equinox.p2.tests.publisher", oce.getMessage(), oce);
		} catch (Exception e) {
			return new Status(IStatus.ERROR, "org.eclipse.equinox.p2.tests.publisher", e.getMessage(), e);
		}
		return Status.OK_STATUS;
	}
}