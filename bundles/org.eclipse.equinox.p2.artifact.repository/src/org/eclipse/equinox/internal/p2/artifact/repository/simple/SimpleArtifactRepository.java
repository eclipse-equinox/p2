/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 * 						Genuitec, LLC - support for multi-threaded downloads
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.artifact.repository.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.AbstractArtifactRepository;
import org.eclipse.osgi.util.NLS;

public class SimpleArtifactRepository extends AbstractArtifactRepository implements IArtifactRepository, IFileArtifactRepository {
	/** 
	 * A boolean property controlling whether mirroring is enabled.
	 */
	//	public static final boolean MIRRORS_ENABLED = !"false".equals(Activator.getContext().getProperty("eclipse.p2.mirrors")); //$NON-NLS-1$//$NON-NLS-2$
	public static final boolean MIRRORS_ENABLED = "true".equals(Activator.getContext().getProperty("eclipse.p2.mirrors")); //$NON-NLS-1$//$NON-NLS-2$

	/** 
	 * The key for a integer property controls the maximum number
	 * of threads that should be used when optimizing downloads from a remote
	 * artifact repository.
	 */
	public static final String PROP_MAX_THREADS = "eclipse.p2.max.threads"; //$NON-NLS-1$

	public class ArtifactOutputStream extends OutputStream implements IStateful {
		private boolean closed;
		private long count = 0;
		private IArtifactDescriptor descriptor;
		private OutputStream destination;
		private File file;
		private IStatus status = Status.OK_STATUS;

		public ArtifactOutputStream(OutputStream os, IArtifactDescriptor descriptor) {
			this(os, descriptor, null);
		}

		public ArtifactOutputStream(OutputStream os, IArtifactDescriptor descriptor, File file) {
			this.destination = os;
			this.descriptor = descriptor;
			this.file = file;
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

		public IStatus getStatus() {
			return status;
		}

		public void setStatus(IStatus status) {
			this.status = status == null ? Status.OK_STATUS : status;
		}

		public void write(byte[] b) throws IOException {
			destination.write(b);
			count += b.length;
		}

		public void write(byte[] b, int off, int len) throws IOException {
			destination.write(b, off, len);
			count += len;
		}

		public void write(int b) throws IOException {
			destination.write(b);
			count++;
		}
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

		public void write(byte[] b) throws IOException {
			fos.write(b);
		}

		public void write(byte[] b, int off, int len) throws IOException {
			fos.write(b, off, len);
		}

		public void write(int b) throws IOException {
			fos.write(b);
		}
	}

	private static final String ARTIFACT_FOLDER = "artifact.folder"; //$NON-NLS-1$
	private static final String ARTIFACT_REFERENCE = "artifact.reference"; //$NON-NLS-1$
	private static final String ARTIFACT_UUID = "artifact.uuid"; //$NON-NLS-1$
	static final private String BLOBSTORE = ".blobstore/"; //$NON-NLS-1$
	static final private String CONTENT_FILENAME = "artifacts"; //$NON-NLS-1$
	static final private String[][] PACKED_MAPPING_RULES = { {"(& (namespace=eclipse) (classifier=plugin) (format=packed))", "${repoUrl}/plugins/${id}_${version}.jar.pack.gz"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=plugin))", "${repoUrl}/plugins/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=native))", "${repoUrl}/native/${id}_${version}"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=feature))", "${repoUrl}/features/${id}_${version}.jar"}}; //$NON-NLS-1$//$NON-NLS-2$

	static final private String[][] DEFAULT_MAPPING_RULES = { {"(& (namespace=eclipse) (classifier=plugin))", "${repoUrl}/plugins/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=native))", "${repoUrl}/native/${id}_${version}"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=feature))", "${repoUrl}/features/${id}_${version}.jar"}}; //$NON-NLS-1$//$NON-NLS-2$
	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	static final private String REPOSITORY_TYPE = IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY;

	static final private Integer REPOSITORY_VERSION = new Integer(1);
	private static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	protected Set artifactDescriptors = new HashSet();
	private transient BlobStore blobStore;
	transient private Mapper mapper = new Mapper();

	static final private String PACKED_FORMAT = "packed"; //$NON-NLS-1$
	static final private String PUBLISH_PACK_FILES_AS_SIBLINGS = "publishPackFilesAsSiblings"; //$NON-NLS-1$

	private static final int DEFAULT_MAX_THREADS = 4;

	protected String[][] mappingRules = DEFAULT_MAPPING_RULES;

	private boolean signatureVerification = false;
	private MirrorSelector mirrors;

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

	public static URL getActualLocation(URL base, boolean compress) {
		return getActualLocation(base, compress ? JAR_EXTENSION : XML_EXTENSION);
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

	public SimpleArtifactRepository(String name, String type, String version, String description, String provider, boolean verifySignature, Set artifacts, String[][] mappingRules, Map properties) {
		super(name, type, version, null, description, provider);
		signatureVerification = verifySignature;
		this.artifactDescriptors.addAll(artifacts);
		this.mappingRules = mappingRules;
		this.properties.putAll(properties);
	}

	public SimpleArtifactRepository(String repositoryName, URL location) {
		super(repositoryName, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null);
		initializeAfterLoad(location);
	}

	public synchronized void addDescriptor(IArtifactDescriptor toAdd) {
		// TODO perhaps the argument here should be ArtifactDescriptor.  IArtifactDescriptors are for 
		// people who are reading the repository.
		// TODO: here we may want to ensure that the artifact has not been added concurrently
		((ArtifactDescriptor) toAdd).setRepository(this);
		artifactDescriptors.add(toAdd);
		save();
	}

	public synchronized void addDescriptors(IArtifactDescriptor[] descriptors) {

		for (int i = 0; i < descriptors.length; i++) {
			((ArtifactDescriptor) descriptors[i]).setRepository(this);
			artifactDescriptors.add(descriptors[i]);
		}
		save();
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

	private byte[] bytesFromHexString(String string) {
		byte[] bytes = new byte[UniversalUniqueIdentifier.BYTES_SIZE];
		for (int i = 0; i < string.length(); i += 2) {
			String byteString = string.substring(i, i + 2);
			bytes[i / 2] = (byte) Integer.parseInt(byteString, 16);
		}
		return bytes;
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

	public synchronized String createLocation(ArtifactDescriptor descriptor) {
		if (flatButPackedEnabled(descriptor)) {
			return getLocationForPackedButFlatArtifacts(descriptor);
		}
		// if the descriptor is canonical, clear out any UUID that might be set and use the Mapper
		if (descriptor.getProcessingSteps().length == 0) {
			descriptor.setProperty(ARTIFACT_UUID, null);
			IArtifactKey key = descriptor.getArtifactKey();
			String result = mapper.map(location.toExternalForm(), key.getNamespace(), key.getClassifier(), key.getId(), key.getVersion().toString(), descriptor.getProperty(IArtifactDescriptor.FORMAT));
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

		//download from the best available mirror
		String baseLocation = getLocation(descriptor);
		String mirrorLocation = getMirror(baseLocation);
		IStatus result = getTransport().download(mirrorLocation, destination, monitor);
		if (mirrors != null)
			mirrors.reportResult(mirrorLocation, result);
		if (result.isOK() || baseLocation.equals(mirrorLocation))
			return result;
		//maybe we hit a bad mirror - try the base location
		return getTransport().download(baseLocation, destination, monitor);
	}

	/**
	 * Returns an equivalent mirror location for the given artifact location.
	 * @param baseLocation The location of the artifact in this repository
	 * @return the Location of the artifact in this repository, or an equivalent mirror
	 */
	private synchronized String getMirror(String baseLocation) {
		if (!MIRRORS_ENABLED || isLocal())
			return baseLocation;
		if (mirrors == null)
			mirrors = new MirrorSelector(this);
		return mirrors.getMirrorLocation(baseLocation);
	}

	public Object getAdapter(Class adapter) {
		// if we are adapting to file or writable repositories then make sure we have a file location
		if (adapter == IFileArtifactRepository.class)
			if (!isLocal())
				return null;
		return super.getAdapter(adapter);
	}

	IStatus getArtifact(ArtifactRequest request, IProgressMonitor monitor) {
		request.setSourceRepository(this);
		request.perform(monitor);
		return request.getResult();
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

	public synchronized IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		ArrayList result = new ArrayList();
		for (Iterator iterator = artifactDescriptors.iterator(); iterator.hasNext();) {
			IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
			if (descriptor.getArtifactKey().equals(key))
				result.add(descriptor);
		}
		return (IArtifactDescriptor[]) result.toArray(new IArtifactDescriptor[result.size()]);
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

	public synchronized IArtifactKey[] getArtifactKeys() {
		// there may be more descriptors than keys to collect up the unique keys
		HashSet result = new HashSet(artifactDescriptors.size());
		for (Iterator it = artifactDescriptors.iterator(); it.hasNext();)
			result.add(((IArtifactDescriptor) it.next()).getArtifactKey());
		return (IArtifactKey[]) result.toArray(new IArtifactKey[result.size()]);
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		final MultiStatus overallStatus = new MultiStatus(Activator.ID, IStatus.OK, null, null);
		LinkedList requestsPending = new LinkedList(Arrays.asList(requests));

		int numberOfJobs = Math.min(requests.length, getMaximumThreads());
		if (isLocal() || numberOfJobs <= 1) {
			SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
			try {
				for (int i = 0; i < requests.length; i++) {
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
					IStatus result = getArtifact((ArtifactRequest) requests[i], subMonitor.newChild(1));
					if (!result.isOK())
						overallStatus.add(result);
				}
			} finally {
				subMonitor.done();
			}
		} else {
			// initialize the various jobs needed to process the get artifact requests
			monitor.beginTask(NLS.bind(Messages.sar_downloading, Integer.toString(requests.length)), requests.length);
			try {
				DownloadJob jobs[] = new DownloadJob[numberOfJobs];
				for (int i = 0; i < numberOfJobs; i++) {
					jobs[i] = new DownloadJob(Messages.sar_downloadJobName + i);
					jobs[i].initialize(this, requestsPending, monitor, overallStatus);
					jobs[i].schedule();
				}
				// wait for all the jobs to complete
				try {
					Job.getJobManager().join(DownloadJob.FAMILY, null);
				} catch (InterruptedException e) {
					//ignore
				}
			} finally {
				monitor.done();
			}
		}
		return (monitor.isCanceled() ? Status.CANCEL_STATUS : overallStatus);
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

	public synchronized Set getDescriptors() {
		return artifactDescriptors;
	}

	/**
	 * Typically non-canonical forms of the artifact are stored in the blob store.
	 * However, we support having the pack200 files alongside the canonical artifact
	 * for compatibility with the format used in optimized update sites.  We call
	 * this arrangement "flat but packed".
	 */
	private boolean flatButPackedEnabled(IArtifactDescriptor descriptor) {
		return Boolean.TRUE.toString().equals(getProperties().get(PUBLISH_PACK_FILES_AS_SIBLINGS)) && PACKED_FORMAT.equals(descriptor.getProperty(IArtifactDescriptor.FORMAT));
	}

	/**
	 * @see #flatButPackedEnabled(IArtifactDescriptor)
	 */
	private String getLocationForPackedButFlatArtifacts(IArtifactDescriptor descriptor) {
		IArtifactKey key = descriptor.getArtifactKey();
		return mapper.map(location.toExternalForm(), key.getNamespace(), key.getClassifier(), key.getId(), key.getVersion().toString(), descriptor.getProperty(IArtifactDescriptor.FORMAT));
	}

	public synchronized String getLocation(IArtifactDescriptor descriptor) {
		// if the artifact has a uuid then use it
		String uuid = descriptor.getProperty(ARTIFACT_UUID);
		if (uuid != null)
			return blobStore.fileFor(bytesFromHexString(uuid));

		if (flatButPackedEnabled(descriptor)) {
			return getLocationForPackedButFlatArtifacts(descriptor);
		}

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
			String result = mapper.map(location.toExternalForm(), key.getNamespace(), key.getClassifier(), key.getId(), key.getVersion().toString(), descriptor.getProperty(IArtifactDescriptor.FORMAT));
			if (result != null) {
				if (isFolderBased(descriptor) && result.endsWith(JAR_EXTENSION))
					return result.substring(0, result.lastIndexOf(JAR_EXTENSION));

				return result;
			}
		}

		// in the end there is not enough information so return null 
		return null;
	}

	/**
	 * Returns the maximum number of concurrent download threads.
	 */
	private int getMaximumThreads() {
		try {
			String maxThreadString = (String) getProperties().get(PROP_MAX_THREADS);
			if (maxThreadString != null)
				return Math.max(1, Integer.parseInt(maxThreadString));
		} catch (NumberFormatException nfe) {
			// return default number of threads
		}
		return DEFAULT_MAX_THREADS;
	}

	public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
		assertModifiable();
		// Check if the artifact is already in this repository
		if (contains(descriptor)) {
			String msg = NLS.bind(Messages.available_already_in, getLocation().toExternalForm());
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.ARTIFACT_EXISTS, msg, null));
		}

		// create a copy of the original descriptor that we can manipulate
		ArtifactDescriptor newDescriptor = new ArtifactDescriptor(descriptor);
		if (isFolderBased(descriptor))
			newDescriptor.setRepositoryProperty(ARTIFACT_FOLDER, Boolean.TRUE.toString());

		// Determine writing location
		String newLocation = createLocation(newDescriptor);
		String file = null;
		try {
			file = new URL(newLocation).getFile();
		} catch (MalformedURLException e1) {
			// This should not happen
			Assert.isTrue(false, "Unexpected failure: " + e1); //$NON-NLS-1$
		}

		// TODO at this point we have to assume that the repository is file-based.  Eventually 
		// we should end up with writeable URLs...
		// Make sure that the file does not exist and that the parents do
		File outputFile = new File(file);
		if (outputFile.exists()) {
			System.err.println("Artifact repository out of sync. Overwriting " + outputFile.getAbsoluteFile()); //$NON-NLS-1$
			delete(outputFile);
		}

		OutputStream target = null;
		try {
			if (isFolderBased(newDescriptor)) {
				outputFile.mkdirs();
				if (!outputFile.exists())
					throw failedWrite(new IOException(NLS.bind(Messages.sar_failedMkdir, outputFile.toString())));
				target = new ZippedFolderOutputStream(outputFile);
			} else {
				// file based0
				File parent = outputFile.getParentFile();
				if (!parent.exists() && !parent.mkdirs())
					throw failedWrite(new IOException(NLS.bind(Messages.sar_failedMkdir, parent.toString()))); //$NON-NLS-1$
				target = new FileOutputStream(file);
			}

			// finally create and return an output stream suitably wrapped so that when it is 
			// closed the repository is updated with the descriptor
			return new ArtifactOutputStream(new BufferedOutputStream(target), newDescriptor, outputFile);
		} catch (IOException e) {
			throw failedWrite(e);
		}
	}

	private ProvisionException failedWrite(Exception e) throws ProvisionException {
		String msg = NLS.bind(Messages.repoFailedWrite, getLocation().toExternalForm());
		throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_WRITE, msg, e));
	}

	public synchronized String[][] getRules() {
		return mappingRules;
	}

	public synchronized boolean getSignatureVerification() {
		return signatureVerification;
	}

	private Transport getTransport() {
		return ECFTransport.getInstance();
	}

	// use this method to setup any transient fields etc after the object has been restored from a stream
	public synchronized void initializeAfterLoad(URL location) {
		this.location = location;
		blobStore = new BlobStore(getBlobStoreLocation(location), 128);
		initializeMapper();
		for (Iterator i = artifactDescriptors.iterator(); i.hasNext();) {
			((ArtifactDescriptor) i.next()).setRepository(this);
		}
	}

	private synchronized void initializeMapper() {
		mapper = new Mapper();
		mapper.initialize(Activator.getContext(), mappingRules);
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

	private boolean isLocal() {
		return "file".equalsIgnoreCase(location.getProtocol()); //$NON-NLS-1$
	}

	public boolean isModifiable() {
		return isLocal();
	}

	public OutputStream processDestination(ProcessingStepHandler handler, IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		destination = addPostSteps(handler, descriptor, destination, monitor);
		destination = handler.createAndLink(descriptor.getProcessingSteps(), descriptor, destination, monitor);
		destination = addPreSteps(handler, descriptor, destination, monitor);
		return destination;
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
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.sar_reportStatus, descriptor.getArtifactKey().toExternalForm()), e);
		}

		IStatus stepStatus = ((ProcessingStep) destination).getStatus(true);
		// if the steps all ran ok and there is no interesting information, return the status from this method
		if (!stepStatus.isMultiStatus() && stepStatus.isOK())
			return status;
		// else gather up the status from the steps
		MultiStatus result = new MultiStatus(Activator.ID, IStatus.OK, new IStatus[0], NLS.bind(Messages.sar_reportStatus, descriptor.getArtifactKey().toExternalForm()), null);
		result.merge(status);
		result.merge(stepStatus);
		return result;
	}

	public void save() {
		boolean compress = "true".equalsIgnoreCase((String) properties.get(PROP_COMPRESSED)); //$NON-NLS-1$
		save(compress);
	}

	public void save(boolean compress) {
		OutputStream os = null;
		try {
			try {
				URL actualLocation = getActualLocation(location, false);
				File artifactsFile = new File(actualLocation.getPath());
				File jarFile = new File(getActualLocation(location, true).getPath());
				if (!compress) {
					if (jarFile.exists()) {
						jarFile.delete();
					}
					if (!artifactsFile.exists()) {
						// create parent folders
						artifactsFile.getParentFile().mkdirs();
					}
					os = new FileOutputStream(artifactsFile);
				} else {
					if (artifactsFile.exists()) {
						artifactsFile.delete();
					}
					if (!jarFile.exists()) {
						if (!jarFile.getParentFile().exists())
							jarFile.getParentFile().mkdirs();
						jarFile.createNewFile();
					}
					JarOutputStream jOs = new JarOutputStream(new FileOutputStream(jarFile));
					jOs.putNextEntry(new JarEntry(new Path(actualLocation.getFile()).lastSegment()));
					os = jOs;
				}
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

	public String setProperty(String key, String newValue) {
		String oldValue = super.setProperty(key, newValue);
		if (oldValue == newValue || (oldValue != null && oldValue.equals(newValue)))
			return oldValue;
		if (PUBLISH_PACK_FILES_AS_SIBLINGS.equals(key)) {
			synchronized (this) {
				if (Boolean.TRUE.toString().equals(newValue)) {
					mappingRules = PACKED_MAPPING_RULES;
				} else {
					mappingRules = DEFAULT_MAPPING_RULES;
				}
				initializeMapper();
			}
		}
		save();
		//force repository manager to reload this repository because it caches properties
		ArtifactRepositoryManager manager = (ArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		try {
			if (manager.getRepository(location) != null)
				manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			//ignore
		}
		return oldValue;
	}

	public synchronized void setRules(String[][] rules) {
		mappingRules = rules;
	}

	public synchronized void setSignatureVerification(boolean value) {
		signatureVerification = value;
	}

	public String toString() {
		return location.toExternalForm();
	}

}
