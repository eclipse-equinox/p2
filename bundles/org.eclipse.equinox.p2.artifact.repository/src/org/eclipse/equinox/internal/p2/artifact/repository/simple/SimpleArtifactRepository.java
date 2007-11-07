/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.*;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.spi.p2.artifact.repository.AbstractArtifactRepository;

public class SimpleArtifactRepository extends AbstractArtifactRepository implements IArtifactRepository, IFileArtifactRepository {

	static final private String BLOBSTORE = ".blobstore/"; //$NON-NLS-1$
	static final private String CONTENT_FILENAME = "artifacts.xml"; //$NON-NLS-1$
	static final private String REPOSITORY_TYPE = SimpleArtifactRepository.class.getName();
	static final private Integer REPOSITORY_VERSION = new Integer(1);
	static final public String[][] DEFAULT_MAPPING_RULES = { {"(& (namespace=eclipse) (classifier=plugin))", "${repoUrl}/plugins/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=native))", "${repoUrl}/native/${id}_${version}"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=feature))", "${repoUrl}/features/${id}_${version}.jar"}}; //$NON-NLS-1$//$NON-NLS-2$
	private static final String ARTIFACT_UUID = "artifact.uuid"; //$NON-NLS-1$

	transient private Mapper mapper = new Mapper();
	protected String[][] mappingRules = DEFAULT_MAPPING_RULES;
	protected Set artifactDescriptors = new HashSet();
	private boolean signatureVerification = false;
	private transient BlobStore blobStore;

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

	public static URL getBlobStoreLocation(URL base) {
		String spec = base.toExternalForm();
		if (spec.endsWith("/")) //$NON-NLS-1$
			spec += BLOBSTORE;
		else
			spec += "/" + BLOBSTORE; //$NON-NLS-1$
		try {
			return new URL(spec);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public SimpleArtifactRepository(String repositoryName, URL location) {
		super(repositoryName, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null);
		initializeAfterLoad(location);
	}

	public SimpleArtifactRepository(String name, String type, String version, String description, String provider, boolean verifySignature, Set artifacts, String[][] mappingRules, Map properties) {
		super(name, type, version, null, description, provider);
		signatureVerification = verifySignature;
		this.artifactDescriptors.addAll(artifacts);
		this.mappingRules = mappingRules;
		this.properties.putAll(properties);
	}

	private IStatus getArtifact(ArtifactRequest request, IProgressMonitor monitor) {
		request.setSourceRepository(this);
		request.perform(monitor);
		return request.getResult();
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
		try {
			MultiStatus overallStatus = new MultiStatus(Activator.ID, IStatus.OK, null, null);
			for (int i = 0; i < requests.length; i++) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				IStatus result = getArtifact((ArtifactRequest) requests[i], subMonitor.newChild(1));
				if (!result.isOK())
					overallStatus.add(result);
			}
			return (monitor.isCanceled() ? Status.CANCEL_STATUS : overallStatus);
		} finally {
			subMonitor.done();
		}
	}

	private IArtifactDescriptor getCompleteArtifactDescriptor(IArtifactKey key) {
		for (Iterator iterator = artifactDescriptors.iterator(); iterator.hasNext();) {
			IArtifactDescriptor desc = (IArtifactDescriptor) iterator.next();
			// look for a descriptor that matches the key and is "complete"
			if (desc.getArtifactKey().equals(key) && desc.getProcessingSteps().length == 0)
				return desc;
		}
		return null;
	}

	private String getLocation(IArtifactDescriptor descriptor) {
		// if the artifact has a uuid then use it
		String uuid = descriptor.getProperty(ARTIFACT_UUID);
		if (uuid != null)
			return blobStore.fileFor(bytesFromHexString(uuid));

		// if the descriptor is complete then use the mapping rules...
		if (descriptor.getProcessingSteps().length == 0) {
			IArtifactKey key = descriptor.getArtifactKey();
			String result = mapper.map(location.toExternalForm(), key.getNamespace(), key.getClassifier(), key.getId(), key.getVersion().toString());
			if (result != null)
				return result;
		}

		// in the end there is not enough information so return null 
		return null;
	}

	private String createLocation(ArtifactDescriptor descriptor) {
		// if the descriptor is canonical, clear out any UUID that might be set and use the Mapper
		if (descriptor.getProcessingSteps().length == 0) {
			descriptor.setProperty(ARTIFACT_UUID, null);
			IArtifactKey key = descriptor.getArtifactKey();
			String result = mapper.map(location.toExternalForm(), key.getNamespace(), key.getClassifier(), key.getId(), key.getVersion().toString());
			if (result != null)
				return result;
		}

		// Otherwise generate a location by creating a UUID, remembering it in the properties 
		// and computing the location
		byte[] bytes = new UniversalUniqueIdentifier().toBytes();
		descriptor.setProperty(ARTIFACT_UUID, bytesToHexString(bytes));
		return blobStore.fileFor(bytes);
	}

	private String bytesToHexString(byte[] bytes) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			String hexString;
			if (bytes[i] < 0)
				hexString = Integer.toHexString(256 + bytes[i]);
			else
				hexString = Integer.toHexString(bytes[i]);
			if (hexString.length() == 1)
				buffer.append("0"); //$NON-NLS-1$
			buffer.append(hexString);
		}
		return buffer.toString();
	}

	private byte[] bytesFromHexString(String string) {
		byte[] bytes = new byte[UniversalUniqueIdentifier.BYTES_SIZE];
		for (int i = 0; i < string.length(); i += 2) {
			String byteString = string.substring(i, i + 2);
			bytes[i / 2] = (byte) Integer.parseInt(byteString, 16);
		}
		return bytes;
	}

	public IArtifactKey[] getArtifactKeys() {
		// there may be more descriptors than keys to collect up the unique keys
		HashSet result = new HashSet(artifactDescriptors.size());
		for (Iterator it = artifactDescriptors.iterator(); it.hasNext();)
			result.add(((IArtifactDescriptor) it.next()).getArtifactKey());
		return (IArtifactKey[]) result.toArray(new IArtifactKey[result.size()]);
	}

	public File getArtifactFile(IArtifactDescriptor descriptor) {
		String result = getLocation(descriptor);
		if (result == null || !result.startsWith("file:")) //$NON-NLS-1$
			return null;
		return new File(result.substring(5));
	}

	public File getArtifactFile(IArtifactKey key) {
		IArtifactDescriptor descriptor = getCompleteArtifactDescriptor(key);
		if (descriptor == null)
			return null;
		return getArtifactFile(descriptor);
	}

	public String toString() {
		return location.toExternalForm();
	}

	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		ProcessingStepHandler handler = new ProcessingStepHandler();
		destination = addPostSteps(handler, descriptor, destination, monitor);
		destination = handler.createAndLink(descriptor.getProcessingSteps(), descriptor, destination, monitor);
		destination = addPreSteps(handler, descriptor, destination, monitor);
		IStatus status = handler.checkStatus(destination);
		if (!status.isOK() && status.getSeverity() != IStatus.INFO)
			return status;
		String toDownload = getLocation(descriptor);
		status = getTransport().download(toDownload, destination, monitor);

		// If the destination is just a normal stream then the status is simple.  Just return
		// it and do not close the destination
		if (!(destination instanceof ProcessingStep))
			return status;

		// If the destination is a processing step then close the stream to flush the data through all
		// the steps.  then collect up the status from all the steps and return
		try {
			destination.close();
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.ID, "Error closing processing steps", e);
		}

		IStatus stepStatus = ((ProcessingStep) destination).getStatus(true);
		// if the steps all ran ok and there is no interesting information, return the status from this method
		if (!stepStatus.isMultiStatus() && stepStatus.isOK())
			return status;
		// else gather up the status from the 
		MultiStatus result = new MultiStatus(Activator.ID, IStatus.OK, new IStatus[0], "Status of getting artifact " + toDownload, null);
		result.merge(status);
		result.merge(stepStatus);
		return result;
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
			destination.close();
			// if the steps ran ok and there was actual content, write the artifact descriptor
			// TODO the count check is a bit bogus but helps in some error cases (e.g., the optimizer)
			// where errors occured in a processing step earlier in the chain.  We likely need a better
			// or more explicit way of handling this case.
			if (ProcessingStepHandler.checkStatus(destination).isOK() && count > 0) {
				((ArtifactDescriptor) descriptor).setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(count));
				addDescriptor(descriptor);
			}
		}
	}

	public void addDescriptor(IArtifactDescriptor toAdd) {
		// TODO perhaps the argument here should be ArtifactDescriptor.  IArtifactDescriptos are for 
		// people who are reading the repo.
		// TODO: here we may want to ensure that the artifact has not been added concurrently
		((ArtifactDescriptor) toAdd).setRepository(this);
		artifactDescriptors.add(toAdd);
		save();
	}

	public void save() {
		try {
			URL actualLocation = getActualLocation(location);
			FileOutputStream os = new FileOutputStream(actualLocation.getFile());
			new SimpleArtifactRepositoryIO().write(this, os);
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

		// create a copy of the original descriptor that we can manipulate
		ArtifactDescriptor newDescriptor = new ArtifactDescriptor(descriptor);
		// Determine writing location
		String location = createLocation(newDescriptor);
		if (location == null)
			// TODO: Log an error, or throw an exception?
			return null;
		String file = null;
		try {
			file = new URL(location).getFile();
		} catch (MalformedURLException e1) {
			// This should not happen
		}

		// TODO at this point we have to assume that the repo is file-based.  Eventually 
		// we should end up with writeable URLs...
		// Make sure that the file does not exist and that the parents do
		File outputFile = new File(file);
		if (outputFile.exists())
			System.err.println("Artifact repository out of synch. Overwriting " + outputFile.getAbsoluteFile()); //$NON-NLS-1$
		if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs())
			// TODO: Log an error, or throw an exception?
			return null;

		// finally create and return an output stream suitably wrapped so that when it is 
		// closed the repository is updated with the descriptor
		try {
			return new ArtifactOutputStream(new BufferedOutputStream(new FileOutputStream(file)), newDescriptor);
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
		properties.put(IRepository.IMPLEMENTATION_ONLY_KEY, Boolean.TRUE.toString());
	}

	/**
	 * Removes the given descriptor, and the physical artifact corresponding
	 * to that descriptor. Returns <code>true</code> if and only if the
	 * descriptor existed in the repository, and was successfully removed.
	 */
	private boolean doRemoveArtifact(IArtifactDescriptor descriptor) {
		File file = getArtifactFile(descriptor);
		if (file == null)
			return false;
		file.delete();
		if (!file.exists())
			return artifactDescriptors.remove(descriptor);
		return false;
	}

	public void removeAll() {
		IArtifactDescriptor[] toRemove = (IArtifactDescriptor[]) artifactDescriptors.toArray(new IArtifactDescriptor[artifactDescriptors.size()]);
		boolean changed = false;
		for (int i = 0; i < toRemove.length; i++)
			changed |= doRemoveArtifact(toRemove[i]);
		if (changed)
			save();
	}

	public void removeDescriptor(IArtifactDescriptor descriptor) {
		if (doRemoveArtifact(descriptor))
			save();
	}

	public void removeDescriptor(IArtifactKey key) {
		IArtifactDescriptor[] toRemove = getArtifactDescriptors(key);
		boolean changed = false;
		for (int i = 0; i < toRemove.length; i++)
			changed |= doRemoveArtifact(toRemove[i]);
		if (changed)
			save();
	}

	// use this method to setup any transient fields etc after the object has been restored from a stream
	public void initializeAfterLoad(URL location) {
		this.location = location;
		blobStore = new BlobStore(getBlobStoreLocation(location), 128);
		if (mapper == null)
			mapper = new Mapper();
		mapper.initialize(Activator.getContext(), mappingRules);
		for (Iterator i = artifactDescriptors.iterator(); i.hasNext();) {
			((ArtifactDescriptor) i.next()).setRepository(this);
		}
	}

	public boolean getSignatureVerification() {
		return signatureVerification;
	}

	public void setSignatureVerification(boolean value) {
		signatureVerification = value;
	}

	public Object getAdapter(Class adapter) {
		// if we are adapting to file or writable repos then make sure we have a file location
		if (adapter == IFileArtifactRepository.class)
			if (!"file".equalsIgnoreCase(location.getProtocol()))
				return null;
		return super.getAdapter(adapter);
	}

	public boolean isModifiable() {
		return true;
	}

}
