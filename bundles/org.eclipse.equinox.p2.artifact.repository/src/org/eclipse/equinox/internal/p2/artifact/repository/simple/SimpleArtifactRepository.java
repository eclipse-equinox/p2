/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
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
import java.util.zip.GZIPOutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.spi.p2.artifact.repository.AbstractArtifactRepository;

public class SimpleArtifactRepository extends AbstractArtifactRepository implements IArtifactRepository, IFileArtifactRepository {

	static final private String BLOBSTORE = ".blobstore/"; //$NON-NLS-1$
	static final private String CONTENT_FILENAME = "artifacts.xml"; //$NON-NLS-1$
	static final private String REPOSITORY_TYPE = "org.eclipse.equinox.p2.artifact.repository.simpleRepository"; //$NON-NLS-1$
	static final private Integer REPOSITORY_VERSION = new Integer(1);
	static final public String[][] DEFAULT_MAPPING_RULES = { {"(& (namespace=eclipse) (classifier=plugin))", "${repoUrl}/plugins/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=native))", "${repoUrl}/native/${id}_${version}"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=feature))", "${repoUrl}/features/${id}_${version}.jar"}}; //$NON-NLS-1$//$NON-NLS-2$
	private static final String ARTIFACT_UUID = "artifact.uuid"; //$NON-NLS-1$
	private static final String ARTIFACT_FOLDER = "artifact.folder"; //$NON-NLS-1$
	private static final String ARTIFACT_REFERENCE = "artifact.reference"; //$NON-NLS-1$
	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	private static final String GZIP_EXTENSION = ".gz"; //$NON-NLS-1$

	transient private Mapper mapper = new Mapper();
	protected String[][] mappingRules = DEFAULT_MAPPING_RULES;
	protected Set artifactDescriptors = new HashSet();
	private boolean signatureVerification = false;
	private transient BlobStore blobStore;

	public static URL getActualLocation(URL base, boolean gzip) {
		return getActualLocation(base, gzip ? GZIP_EXTENSION : ""); //$NON-NLS-1$
	}

	private static URL getActualLocation(URL base, String extension) {
		final String name = CONTENT_FILENAME + extension;
		String spec = base.toExternalForm();
		if (spec.endsWith(name))
			return base;
		if (spec.endsWith("/")) //$NON-NLS-1$
			spec += name;
		else
			spec += "/" + name; //$NON-NLS-1$
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

	public synchronized IArtifactDescriptor getCompleteArtifactDescriptor(IArtifactKey key) {
		for (Iterator iterator = artifactDescriptors.iterator(); iterator.hasNext();) {
			IArtifactDescriptor desc = (IArtifactDescriptor) iterator.next();
			// look for a descriptor that matches the key and is "complete"
			if (desc.getArtifactKey().equals(key) && desc.getProcessingSteps().length == 0)
				return desc;
		}
		return null;
	}

	public synchronized String getLocation(IArtifactDescriptor descriptor) {
		// if the artifact has a uuid then use it
		String uuid = descriptor.getProperty(ARTIFACT_UUID);
		if (uuid != null)
			return blobStore.fileFor(bytesFromHexString(uuid));

		// if the artifact is just a reference then return the reference location
		if (descriptor instanceof ArtifactDescriptor) {
			String artifactReference = ((ArtifactDescriptor) descriptor).getRepositoryProperty(ARTIFACT_REFERENCE);
			if (artifactReference != null)
				return artifactReference;
		}

		// TODO: remove this when we are consistently using repository properties
		// if the artifact is just a reference then return the reference location
		String artifactReference = descriptor.getProperty(ARTIFACT_REFERENCE);
		if (artifactReference != null)
			return artifactReference;

		// if the descriptor is complete then use the mapping rules...
		if (descriptor.getProcessingSteps().length == 0) {
			IArtifactKey key = descriptor.getArtifactKey();
			String result = mapper.map(location.toExternalForm(), key.getNamespace(), key.getClassifier(), key.getId(), key.getVersion().toString());
			if (result != null) {
				if (isFolderBased(descriptor) && result.endsWith(JAR_EXTENSION))
					return result.substring(0, result.lastIndexOf(JAR_EXTENSION));

				return result;
			}
		}

		// in the end there is not enough information so return null 
		return null;
	}

	public synchronized String createLocation(ArtifactDescriptor descriptor) {
		// if the descriptor is canonical, clear out any UUID that might be set and use the Mapper
		if (descriptor.getProcessingSteps().length == 0) {
			descriptor.setProperty(ARTIFACT_UUID, null);
			IArtifactKey key = descriptor.getArtifactKey();
			String result = mapper.map(location.toExternalForm(), key.getNamespace(), key.getClassifier(), key.getId(), key.getVersion().toString());
			if (result != null) {
				if (isFolderBased(descriptor) && result.endsWith(JAR_EXTENSION))
					return result.substring(0, result.lastIndexOf(JAR_EXTENSION));

				return result;
			}
		}

		// Otherwise generate a location by creating a UUID, remembering it in the properties 
		// and computing the location
		byte[] bytes = new UniversalUniqueIdentifier().toBytes();
		descriptor.setProperty(ARTIFACT_UUID, bytesToHexString(bytes));
		return blobStore.fileFor(bytes);
	}

	private boolean isFolderBased(IArtifactDescriptor descriptor) {
		// if the artifact is just a reference then return the reference location
		if (descriptor instanceof ArtifactDescriptor) {
			String useArtifactFolder = ((ArtifactDescriptor) descriptor).getRepositoryProperty(ARTIFACT_FOLDER);
			if (useArtifactFolder != null)
				return Boolean.valueOf(useArtifactFolder).booleanValue();
		}
		//TODO: refactor this when the artifact folder property is consistently set in repository properties
		return Boolean.valueOf(descriptor.getProperty(ARTIFACT_FOLDER)).booleanValue();
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

	public synchronized IArtifactKey[] getArtifactKeys() {
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
		destination = processDestination(handler, descriptor, destination, monitor);
		IStatus status = ProcessingStepHandler.checkStatus(destination);
		if (!status.isOK() && status.getSeverity() != IStatus.INFO)
			return status;

		status = downloadArtifact(descriptor, destination, monitor);
		return reportStatus(descriptor, destination, status);
	}

	public IStatus reportStatus(IArtifactDescriptor descriptor, OutputStream destination, IStatus status) {
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
		MultiStatus result = new MultiStatus(Activator.ID, IStatus.OK, new IStatus[0], "Status of getting artifact " + descriptor.getArtifactKey(), null);
		result.merge(status);
		result.merge(stepStatus);
		return result;
	}

	public OutputStream processDestination(ProcessingStepHandler handler, IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		destination = addPostSteps(handler, descriptor, destination, monitor);
		destination = handler.createAndLink(descriptor.getProcessingSteps(), descriptor, destination, monitor);
		destination = addPreSteps(handler, descriptor, destination, monitor);
		return destination;
	}

	protected IStatus downloadArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {

		if (isFolderBased(descriptor)) {
			File artifactFolder = getArtifactFile(descriptor);
			// TODO: optimize and ensure manifest is written first
			File zipFile = null;
			try {
				zipFile = File.createTempFile(artifactFolder.getName(), JAR_EXTENSION, null);
				FileUtils.zip(artifactFolder.listFiles(), zipFile);
				FileInputStream fis = new FileInputStream(zipFile);
				FileUtils.copyStream(fis, true, destination, false);
			} catch (IOException e) {
				return new Status(IStatus.ERROR, Activator.ID, e.getMessage());
			} finally {
				if (zipFile != null)
					zipFile.delete();
			}
			return Status.OK_STATUS;
		}

		String toDownload = getLocation(descriptor);
		return getTransport().download(toDownload, destination, monitor);
	}

	private synchronized OutputStream addPostSteps(ProcessingStepHandler handler, IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
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

	public synchronized IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		ArrayList result = new ArrayList();
		for (Iterator iterator = artifactDescriptors.iterator(); iterator.hasNext();) {
			IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
			if (descriptor.getArtifactKey().equals(key))
				result.add(descriptor);
		}
		return (IArtifactDescriptor[]) result.toArray(new IArtifactDescriptor[result.size()]);
	}

	public class ArtifactOutputStream extends OutputStream implements IStateful {
		private OutputStream destination;
		private IArtifactDescriptor descriptor;
		private IStatus status = Status.OK_STATUS;
		private File file;
		private long count = 0;
		private boolean closed;

		public ArtifactOutputStream(OutputStream os, IArtifactDescriptor descriptor) {
			this(os, descriptor, null);
		}

		public ArtifactOutputStream(OutputStream os, IArtifactDescriptor descriptor, File file) {
			this.destination = os;
			this.descriptor = descriptor;
			this.file = file;
		}

		public IStatus getStatus() {
			return status;
		}

		public void setStatus(IStatus status) {
			this.status = status;
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
			if (closed)
				return;
			closed = true;

			try {
				destination.close();
			} catch (IOException e) {
				// cleanup if possible
				if (file != null)
					delete(file);
				if (getStatus().isOK())
					throw e;
				// if the stream has already been e.g. canceled, we can return - the status is already set correctly 
				return;
			}
			// if the steps ran ok and there was actual content, write the artifact descriptor
			// TODO the count check is a bit bogus but helps in some error cases (e.g., the optimizer)
			// where errors occurred in a processing step earlier in the chain.  We likely need a better
			// or more explicit way of handling this case.
			if (getStatus().isOK() && ProcessingStepHandler.checkStatus(destination).isOK() && count > 0) {
				((ArtifactDescriptor) descriptor).setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(count));
				addDescriptor(descriptor);
			} else if (file != null)
				// cleanup if possible
				delete(file);
		}
	}

	public synchronized void addDescriptor(IArtifactDescriptor toAdd) {
		// TODO perhaps the argument here should be ArtifactDescriptor.  IArtifactDescriptors are for 
		// people who are reading the repository.
		// TODO: here we may want to ensure that the artifact has not been added concurrently
		((ArtifactDescriptor) toAdd).setRepository(this);
		artifactDescriptors.add(toAdd);
		save();
	}

	public void save() {
		save(true);
	}

	public void save(boolean gzip) {
		OutputStream os = null;
		try {
			try {
				URL actualLocation = getActualLocation(location, gzip);
				File artifactsFile = new File(actualLocation.getPath());
				if (!artifactsFile.exists()) {
					// create parent folders
					artifactsFile.getParentFile().mkdirs();
				}
				os = new FileOutputStream(artifactsFile);
				if (gzip)
					os = new GZIPOutputStream(os);
				new SimpleArtifactRepositoryIO().write(this, os);
			} catch (IOException e) {
				// TODO proper exception handling
				e.printStackTrace();
			} finally {
				if (os != null)
					os.close();
			}
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
		if (isFolderBased(descriptor))
			newDescriptor.setRepositoryProperty(ARTIFACT_FOLDER, Boolean.TRUE.toString());

		// Determine writing location
		String newLocation = createLocation(newDescriptor);
		if (newLocation == null)
			// TODO: Log an error, or throw an exception?
			return null;
		String file = null;
		try {
			file = new URL(newLocation).getFile();
		} catch (MalformedURLException e1) {
			// This should not happen
			Assert.isTrue(false, "Unexpected failure: " + e1); //$NON-NLS-1$
		}

		// TODO at this point we have to assume that the repo is file-based.  Eventually 
		// we should end up with writeable URLs...
		// Make sure that the file does not exist and that the parents do
		File outputFile = new File(file);
		if (outputFile.exists()) {
			System.err.println("Artifact repository out of synch. Overwriting " + outputFile.getAbsoluteFile()); //$NON-NLS-1$
			delete(outputFile);
		}

		OutputStream target = null;
		try {
			if (isFolderBased(newDescriptor)) {
				outputFile.mkdirs();
				if (!outputFile.exists())
					// TODO: Log an error, or throw an exception?
					return null;

				target = new ZippedFolderOutputStream(outputFile);
			} else {
				// file based
				if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs())
					// TODO: Log an error, or throw an exception?
					return null;
				target = new FileOutputStream(file);
			}

			// finally create and return an output stream suitably wrapped so that when it is 
			// closed the repository is updated with the descriptor

			return new ArtifactOutputStream(new BufferedOutputStream(target), newDescriptor, outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public synchronized boolean contains(IArtifactDescriptor descriptor) {
		return artifactDescriptors.contains(descriptor);
	}

	public synchronized boolean contains(IArtifactKey key) {
		for (Iterator iterator = artifactDescriptors.iterator(); iterator.hasNext();) {
			IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
			if (descriptor.getArtifactKey().equals(key))
				return true;
		}
		return false;
	}

	public synchronized Set getDescriptors() {
		return artifactDescriptors;
	}

	public synchronized String[][] getRules() {
		return mappingRules;
	}

	public String setProperty(String key, String newValue) {
		String oldValue = super.setProperty(key, newValue);
		if (oldValue != newValue && (oldValue == null || !oldValue.equals(newValue)))
			save();
		return oldValue;
	}

	public synchronized void setRules(String[][] rules) {
		mappingRules = rules;
	}

	/**
	 * Removes the given descriptor, and the physical artifact corresponding
	 * to that descriptor. Returns <code>true</code> if and only if the
	 * descriptor existed in the repository, and was successfully removed.
	 */
	private boolean doRemoveArtifact(IArtifactDescriptor descriptor) {
		if (descriptor.getProperty(ARTIFACT_REFERENCE) == null) {
			File file = getArtifactFile(descriptor);
			if (file == null)
				return false;
			delete(file);
			if (file.exists())
				return false;
		}
		return artifactDescriptors.remove(descriptor);
	}

	static void delete(File toDelete) {
		if (toDelete.isFile()) {
			toDelete.delete();
			return;
		}
		if (toDelete.isDirectory()) {
			File[] children = toDelete.listFiles();
			if (children != null) {
				for (int i = 0; i < children.length; i++) {
					delete(children[i]);
				}
			}
			toDelete.delete();
		}
	}

	public synchronized void removeAll() {
		IArtifactDescriptor[] toRemove = (IArtifactDescriptor[]) artifactDescriptors.toArray(new IArtifactDescriptor[artifactDescriptors.size()]);
		boolean changed = false;
		for (int i = 0; i < toRemove.length; i++)
			changed |= doRemoveArtifact(toRemove[i]);
		if (changed)
			save();
	}

	public synchronized void removeDescriptor(IArtifactDescriptor descriptor) {
		if (doRemoveArtifact(descriptor))
			save();
	}

	public synchronized void removeDescriptor(IArtifactKey key) {
		IArtifactDescriptor[] toRemove = getArtifactDescriptors(key);
		boolean changed = false;
		for (int i = 0; i < toRemove.length; i++)
			changed |= doRemoveArtifact(toRemove[i]);
		if (changed)
			save();
	}

	// use this method to setup any transient fields etc after the object has been restored from a stream
	public synchronized void initializeAfterLoad(URL location) {
		this.location = location;
		blobStore = new BlobStore(getBlobStoreLocation(location), 128);
		if (mapper == null)
			mapper = new Mapper();
		mapper.initialize(Activator.getContext(), mappingRules);
		for (Iterator i = artifactDescriptors.iterator(); i.hasNext();) {
			((ArtifactDescriptor) i.next()).setRepository(this);
		}
	}

	public synchronized boolean getSignatureVerification() {
		return signatureVerification;
	}

	public synchronized void setSignatureVerification(boolean value) {
		signatureVerification = value;
	}

	public Object getAdapter(Class adapter) {
		// if we are adapting to file or writable repositories then make sure we have a file location
		if (adapter == IFileArtifactRepository.class)
			if (!"file".equalsIgnoreCase(location.getProtocol())) //$NON-NLS-1$
				return null;
		return super.getAdapter(adapter);
	}

	public boolean isModifiable() {
		return true;
	}

	// TODO: optimize
	// we could stream right into the folder
	public static class ZippedFolderOutputStream extends OutputStream {

		private final File folder;
		private final FileOutputStream fos;
		private final File zipFile;

		public ZippedFolderOutputStream(File folder) throws IOException {
			this.folder = folder;
			zipFile = File.createTempFile(folder.getName(), JAR_EXTENSION, null);
			fos = new FileOutputStream(zipFile);
		}

		public void close() throws IOException {
			fos.close();
			FileUtils.unzipFile(zipFile, folder);
			zipFile.delete();
		}

		public void flush() throws IOException {
			fos.flush();
		}

		public String toString() {
			return fos.toString();
		}

		public void write(byte[] b, int off, int len) throws IOException {
			fos.write(b, off, len);
		}

		public void write(byte[] b) throws IOException {
			fos.write(b);
		}

		public void write(int b) throws IOException {
			fos.write(b);
		}
	}

}
