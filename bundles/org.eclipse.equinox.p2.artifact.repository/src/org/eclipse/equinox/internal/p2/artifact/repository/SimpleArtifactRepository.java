/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.p2.core.helpers.MultiStatus;
import org.eclipse.equinox.p2.core.repository.AbstractRepository;
import org.eclipse.equinox.p2.core.repository.IRepositoryInfo;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

public class SimpleArtifactRepository extends AbstractRepository implements IWritableArtifactRepository {

	static final private String CONTENT_FILENAME = "artifacts.xml"; //$NON-NLS-1$
	static final private String REPOSITORY_TYPE = SimpleArtifactRepository.class.getName();
	static final private Integer REPOSITORY_VERSION = new Integer(1);
	static final public String[][] DEFAULT_MAPPING_RULES = { {"(& (namespace=eclipse) (classifier=plugin))", "${repoUrl}/plugins/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=native))", "${repoUrl}/native/${id}_${version}"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=feature))", "${repoUrl}/features/${id}_${version}.jar"}}; //$NON-NLS-1$//$NON-NLS-2$

	transient private Mapper mapper = new Mapper();
	protected String[][] mappingRules = DEFAULT_MAPPING_RULES;
	protected Set artifactDescriptors = new HashSet();
	private boolean signatureVerification = false;

	public static URL getActualLocation(URL base) {
		String spec = base.toExternalForm();
		if (spec.endsWith(CONTENT_FILENAME))
			return base;
		if (spec.endsWith("/")) //$NON-NLS-1$
			spec += CONTENT_FILENAME;
		else
			spec += "/" + CONTENT_FILENAME; //$NON-NLS-1$
		try {
			return new URL(spec);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public SimpleArtifactRepository(String repositoryName, URL location) {
		super(repositoryName, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location);
		mapper.initialize(Activator.getContext(), mappingRules);
	}

	private IStatus getArtifact(ArtifactRequest request, IProgressMonitor monitor) {
		request.setSourceRepository(this);
		request.perform(monitor);
		return request.getResult();
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
		try {
			MultiStatus overallStatus = new MultiStatus();
			for (int i = 0; i < requests.length; i++) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				overallStatus.add(getArtifact((ArtifactRequest) requests[i], subMonitor.newChild(1)));
			}
			return (monitor.isCanceled() ? Status.CANCEL_STATUS : overallStatus);
		} finally {
			subMonitor.done();
		}
	}

	private String basicGetArtifactLocation(IArtifactDescriptor descriptor) {
		return computeLocation(descriptor.getArtifactKey());
	}

	private String basicGetArtifactLocation(IArtifactKey key) {
		boolean found = false;
		for (Iterator iterator = artifactDescriptors.iterator(); iterator.hasNext();) {
			IArtifactDescriptor desc = (IArtifactDescriptor) iterator.next();
			if (desc.getArtifactKey().equals(key)) { //TODO This should probably be a lookup in the set
				found = true;
				break;
			}
		}
		return (found ? computeLocation(key) : null);
	}

	String computeLocation(IArtifactKey key) {
		return mapper.map(location.toExternalForm(), key.getNamespace(), key.getClassifier(), key.getId(), key.getVersion().toString());
	}

	public IArtifactKey[] getArtifactKeys() {
		// there may be more descriptors than keys to collect up the unique keys
		HashSet result = new HashSet(artifactDescriptors.size());
		for (Iterator it = artifactDescriptors.iterator(); it.hasNext();)
			result.add(((IArtifactDescriptor) it.next()).getArtifactKey());
		return (IArtifactKey[]) result.toArray(new IArtifactKey[result.size()]);
	}

	public URI getArtifact(IArtifactKey key) {
		String result = basicGetArtifactLocation(key);
		return (result != null ? URI.create(result) : null);
		//			if (location == null)
		//				return null;
		//			try {
		//				URL url = new URL(location);
		//				return new URI(url.getProtocol(), url.getHost(), url.getPath(), url.getQuery());
		//			} catch (MalformedURLException e) {
		//				throw new IllegalArgumentException(e);
		//			} catch (URISyntaxException e) {
		//				throw new IllegalArgumentException(e);
		//			}
	}

	public String toString() {
		return location.toExternalForm();
	}

	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		ProcessingStepHandler handler = new ProcessingStepHandler();
		try {
			destination = addPostSteps(handler, descriptor, destination, monitor);
			destination = handler.createAndLink(descriptor.getProcessingSteps(), descriptor, destination, monitor);
			destination = addPreSteps(handler, descriptor, destination, monitor);
			IStatus status = handler.validateSteps(destination);
			if (status.isOK() || status.getSeverity() == IStatus.INFO)
				return getTransport().download(basicGetArtifactLocation(descriptor), destination, monitor);
			return status;
		} finally {
			try {
				//don't close the underlying output stream - the caller will do that
				if (destination instanceof ProcessingStep)
					destination.close();
			} catch (IOException e) {
				return new Status(IStatus.ERROR, Activator.ID, "Error closing processing steps", e);
			}
		}
	}

	private OutputStream addPostSteps(ProcessingStepHandler handler, IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		ArrayList steps = new ArrayList();
		if (signatureVerification)
			steps.add(new SignatureVerifier());
		//		if (md5Verification)
		//			steps.add(new MD5Verifier(descriptor.getProperty(IArtifactDescriptor.ARTIFACT_MD5)));
		if (steps.isEmpty())
			return destination;
		ProcessingStep[] stepArray = (ProcessingStep[]) steps.toArray(new ProcessingStep[steps.size()]);
		// TODO should probably be using createAndLink here
		return handler.link(stepArray, destination, monitor);
	}

	private OutputStream addPreSteps(ProcessingStepHandler handler, IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		ArrayList steps = new ArrayList();
		// Add steps here if needed
		if (steps.isEmpty())
			return destination;
		ProcessingStep[] stepArray = (ProcessingStep[]) steps.toArray(new ProcessingStep[steps.size()]);
		// TODO should probably be using createAndLink here
		return handler.link(stepArray, destination, monitor);
	}

	private Transport getTransport() {
		return ECFTransport.getInstance();
	}

	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		ArrayList result = new ArrayList();
		for (Iterator iterator = artifactDescriptors.iterator(); iterator.hasNext();) {
			IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
			if (descriptor.getArtifactKey().equals(key))
				result.add(descriptor);
		}
		return (IArtifactDescriptor[]) result.toArray(new IArtifactDescriptor[result.size()]);
	}

	private class ArtifactOutputStream extends OutputStream {
		private OutputStream destination;
		private IArtifactDescriptor descriptor;
		private long count = 0;

		public ArtifactOutputStream(OutputStream os, IArtifactDescriptor descriptor) {
			this.destination = os;
			this.descriptor = descriptor;
		}

		public void write(int b) throws IOException {
			destination.write(b);
			count++;
		}

		public void write(byte[] b) throws IOException {
			destination.write(b);
			count += b.length;
		}

		public void write(byte[] b, int off, int len) throws IOException {
			destination.write(b, off, len);
			count += len;
		}

		public void close() throws IOException {
			// Write the artifact descriptor
			destination.close();
			((ArtifactDescriptor) descriptor).setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(count));
			addDescriptor(descriptor);
		}
	}

	public void addDescriptor(IArtifactDescriptor toAdd) {
		// TODO: here we may want to ensure that the artifact has not been added concurrently
		artifactDescriptors.add(toAdd);
		save();
	}

	public void save() {
		try {
			FileOutputStream os = new FileOutputStream(getActualLocation(location).getFile());
			ArtifactRepositoryIO.write(this, os);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public OutputStream getOutputStream(IArtifactDescriptor descriptor) {
		// TODO we need a better way of distinguishing between errors and cases where 
		// the stuff just already exists
		// Check if the artifact is already in this repository
		if (contains(descriptor))
			return null;

		// Determine writing location
		String location = computeLocation(descriptor.getArtifactKey());
		if (location == null)
			// TODO: Log an error, or throw an exception?
			return null;

		String file = null;
		try {
			file = new URL(location).getFile();
		} catch (MalformedURLException e1) {
			// This should not happen
		}

		File outputFile = new File(file);
		if (outputFile.exists())
			System.err.println("Artifact repository out of synch. Overwriting " + outputFile.getAbsoluteFile());

		if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs())
			// TODO: Log an error, or throw an exception?
			return null;

		try {
			return new ArtifactOutputStream(new BufferedOutputStream(new FileOutputStream(file)), descriptor);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean contains(IArtifactDescriptor descriptor) {
		return artifactDescriptors.contains(descriptor);
	}

	public boolean contains(IArtifactKey key) {
		for (Iterator iterator = artifactDescriptors.iterator(); iterator.hasNext();) {
			IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
			if (descriptor.getArtifactKey().equals(key))
				return true;
		}
		return false;
	}

	/**
	 * Return the set of descriptors in this repository.  
	 * <b>NOTE:</b> this is NOT part of the API
	 * @return the set of descriptors
	 */
	public Set getDescriptors() {
		return artifactDescriptors;
	}

	public String[][] getRules() {
		return mappingRules;
	}

	public void setRules(String[][] rules) {
		mappingRules = rules;
	}

	public void tagAsImplementation() {
		properties.setProperty(IRepositoryInfo.IMPLEMENTATION_ONLY_KEY, Boolean.valueOf(true).toString());
	}

	public void removeAll() {
		artifactDescriptors.clear();
		save();
	}

	public void removeDescriptor(IArtifactDescriptor descriptor) {
		artifactDescriptors.remove(descriptor);
		save();
	}

	public void removeDescriptor(IArtifactKey key) {
		ArrayList toRemove = new ArrayList();
		for (Iterator iterator = artifactDescriptors.iterator(); iterator.hasNext();) {
			IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
			if (descriptor.getArtifactKey().equals(key))
				toRemove.add(descriptor);
		}
		artifactDescriptors.removeAll(toRemove);
		save();
	}

	// use this method to setup any transient fields etc after the object has been restored from a stream
	public void initializeAfterLoad(URL location) {
		this.location = location;
		if (mapper == null)
			mapper = new Mapper();
		mapper.initialize(Activator.getContext(), mappingRules);
	}

	public void setSignatureVerification(boolean value) {
		signatureVerification = value;
	}
}
