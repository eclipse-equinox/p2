/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	IBM Corporation - initial API and implementation
 * 	Genuitec, LLC - support for multi-threaded downloads
 *  Cloudsmith Inc. - query indexes
 *  Sonatype Inc - ongoing development
 *  EclipseSource - file locking and ongoing development
 *  Red Hat Inc. - Fix compiler problems from generified IAdaptable#getAdapter
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.artifact.processors.md5.MD5Verifier;
import org.eclipse.equinox.internal.p2.artifact.repository.*;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.expression.CompoundIterator;
import org.eclipse.equinox.internal.p2.metadata.index.IndexProvider;
import org.eclipse.equinox.internal.p2.repository.DownloadStatus;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.*;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.index.IIndex;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;

public class SimpleArtifactRepository extends AbstractArtifactRepository implements IFileArtifactRepository, IIndexProvider<IArtifactKey> {
	/** 
	 * A boolean property controlling whether mirroring is enabled.
	 */
	public static final boolean MIRRORS_ENABLED = !"false".equals(Activator.getContext().getProperty("eclipse.p2.mirrors")); //$NON-NLS-1$//$NON-NLS-2$

	public static final boolean MD5_CHECK_ENABLED = !"false".equals(Activator.getContext().getProperty("eclipse.p2.MD5Check")); //$NON-NLS-1$//$NON-NLS-2$

	public static final boolean MD5_ARTIFACT_CHECK_ENABLED = !"false".equals(Activator.getContext().getProperty("eclipse.p2.MD5ArtifactCheck")); //$NON-NLS-1$//$NON-NLS-2$

	public static final String CONTENT_FILENAME = "artifacts"; //$NON-NLS-1$

	/** 
	 * The key for a integer property controls the maximum number
	 * of threads that should be used when optimizing downloads from a remote
	 * artifact repository.
	 */
	public static final String PROP_MAX_THREADS = "eclipse.p2.max.threads"; //$NON-NLS-1$

	/**
	 * Allows override of whether threading should be used.
	 */
	public static final String PROP_FORCE_THREADING = "eclipse.p2.force.threading"; //$NON-NLS-1$

	/**
	 * Location of the repository lock
	 */
	private Location lockLocation = null;

	/**
	 * Allows an artifact repository to set the name of its blobstore.
	 */
	public static final String PROP_BLOBSTORE_NAME = "p2.blobstore.name"; //$NON-NLS-1$

	/**
	 * Does this instance of the repository currently hold a lock
	 */
	private boolean holdsLock = false;
	/**
	 * Does this instance of the repository can be locked.
	 * It will be initialized when initializing the location for repository
	 */
	private Boolean canLock = null;

	private long cacheTimestamp = 0l;

	public class ArtifactOutputStream extends OutputStream implements IStateful {
		private boolean closed;
		private long count = 0;
		private IArtifactDescriptor descriptor;
		private OutputStream destination;
		private File file;
		private IStatus status = Status.OK_STATUS;
		private OutputStream firstLink;

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
			OutputStream testStream = firstLink == null ? this : firstLink;
			if (ProcessingStepHandler.checkStatus(testStream).isOK() && count > 0) {
				((ArtifactDescriptor) descriptor).setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(count));
				addDescriptor(descriptor);
			} else if (file != null)
				// cleanup if possible
				delete(file);
		}

		public IStatus getStatus() {
			return status;
		}

		public OutputStream getDestination() {
			return destination;
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

		public void setFirstLink(OutputStream value) {
			firstLink = value;
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
			try {
				FileUtils.unzipFile(zipFile, folder);
			} finally {
				zipFile.delete();
			}
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
	private static final String ARTIFACT_UUID = "artifact.uuid"; //$NON-NLS-1$
	static final private String BLOBSTORE = ".blobstore/"; //$NON-NLS-1$
	static final private String[][] PACKED_MAPPING_RULES = {{"(& (classifier=osgi.bundle) (format=packed))", "${repoUrl}/plugins/${id}_${version}.jar.pack.gz"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (classifier=osgi.bundle))", "${repoUrl}/plugins/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (classifier=binary))", "${repoUrl}/binary/${id}_${version}"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"(& (classifier=org.eclipse.update.feature) (format=packed))", "${repoUrl}/features/${id}_${version}.jar.pack.gz"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (classifier=org.eclipse.update.feature))", "${repoUrl}/features/${id}_${version}.jar"}}; //$NON-NLS-1$//$NON-NLS-2$

	static final private String[][] DEFAULT_MAPPING_RULES = {{"(& (classifier=osgi.bundle))", "${repoUrl}/plugins/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (classifier=binary))", "${repoUrl}/binary/${id}_${version}"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"(& (classifier=org.eclipse.update.feature))", "${repoUrl}/features/${id}_${version}.jar"}}; //$NON-NLS-1$//$NON-NLS-2$
	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	static final private String REPOSITORY_TYPE = IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY;

	static final private Integer REPOSITORY_VERSION = 1;
	private static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	protected Set<SimpleArtifactDescriptor> artifactDescriptors = new HashSet<SimpleArtifactDescriptor>();
	/**
	 * Map<IArtifactKey,List<IArtifactDescriptor>> containing the index of artifacts in the repository.
	 */
	private Map<IArtifactKey, List<IArtifactDescriptor>> artifactMap = new HashMap<IArtifactKey, List<IArtifactDescriptor>>();
	private transient BlobStore blobStore;
	transient private Mapper mapper = new Mapper();
	private KeyIndex keyIndex;
	private boolean snapshotNeeded = false;

	static final private String PUBLISH_PACK_FILES_AS_SIBLINGS = "publishPackFilesAsSiblings"; //$NON-NLS-1$

	private static final int DEFAULT_MAX_THREADS = 4;

	protected String[][] mappingRules = DEFAULT_MAPPING_RULES;

	private MirrorSelector mirrors;

	private boolean disableSave = false;

	static void delete(File toDelete) {
		if (toDelete.isDirectory()) {
			File[] children = toDelete.listFiles();
			if (children != null) {
				for (int i = 0; i < children.length; i++) {
					delete(children[i]);
				}
			}
		}
		toDelete.delete();
	}

	public static URI getActualLocation(URI base, boolean compress) {
		return getActualLocation(base, compress ? JAR_EXTENSION : XML_EXTENSION);
	}

	private static URI getActualLocation(URI base, String extension) {
		return URIUtil.append(base, CONTENT_FILENAME + extension);
	}

	public static URI getBlobStoreLocation(URI base, String suffix) {
		return URIUtil.append(base, suffix);
	}

	/*
	 * This is only called by the parser when loading a repository.
	 */
	SimpleArtifactRepository(IProvisioningAgent agent, String name, String type, String version, String description, String provider, Set<SimpleArtifactDescriptor> artifacts, String[][] mappingRules, Map<String, String> properties) {
		super(agent, name, type, version, null, description, provider, properties);
		this.artifactDescriptors.addAll(artifacts);
		this.mappingRules = mappingRules;
		for (SimpleArtifactDescriptor desc : artifactDescriptors)
			mapDescriptor(desc);
	}

	private synchronized void mapDescriptor(IArtifactDescriptor descriptor) {
		IArtifactKey key = descriptor.getArtifactKey();
		if (snapshotNeeded) {
			cloneAritfactMap();
			snapshotNeeded = false;
		}
		List<IArtifactDescriptor> descriptors = artifactMap.get(key);
		if (descriptors == null) {
			descriptors = new ArrayList<IArtifactDescriptor>();
			artifactMap.put(key, descriptors);
		}
		descriptors.add(descriptor);
		keyIndex = null;
	}

	private synchronized void unmapDescriptor(IArtifactDescriptor descriptor) {
		IArtifactKey key = descriptor.getArtifactKey();
		List<IArtifactDescriptor> descriptors = artifactMap.get(key);
		if (descriptors == null)
			return;

		if (snapshotNeeded) {
			cloneAritfactMap();
			snapshotNeeded = false;
			descriptors = artifactMap.get(key);
		}
		descriptors.remove(descriptor);
		if (descriptors.isEmpty())
			artifactMap.remove(key);
		keyIndex = null;
	}

	private void cloneAritfactMap() {
		HashMap<IArtifactKey, List<IArtifactDescriptor>> clone = new HashMap<IArtifactKey, List<IArtifactDescriptor>>(artifactMap.size());
		for (Entry<IArtifactKey, List<IArtifactDescriptor>> entry : artifactMap.entrySet())
			clone.put(entry.getKey(), new ArrayList<IArtifactDescriptor>(entry.getValue()));
		artifactMap = clone;
	}

	public SimpleArtifactRepository(IProvisioningAgent agent, String repositoryName, URI location, Map<String, String> properties) {
		super(agent, repositoryName, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null, properties);

		boolean lockAcquired = false;
		try {
			canLock = Boolean.valueOf(canLock());
			if (canLock.booleanValue()) {
				lockAcquired = lockAndLoad(true, new NullProgressMonitor());
				if (!lockAcquired)
					throw new IllegalStateException("Cannot acquire the lock for " + location); //$NON-NLS-1$
			}

			initializeAfterLoad(location, false); // Don't update the timestamp, it will be done during save
			if (properties != null) {
				if (properties.containsKey(PUBLISH_PACK_FILES_AS_SIBLINGS)) {
					synchronized (this) {
						String newValue = properties.get(PUBLISH_PACK_FILES_AS_SIBLINGS);
						if (Boolean.TRUE.toString().equals(newValue)) {
							mappingRules = PACKED_MAPPING_RULES;
						} else {
							mappingRules = DEFAULT_MAPPING_RULES;
						}
						initializeMapper();
					}
				}
			}
			save();
		} finally {
			if (lockAcquired)
				unlock();
		}
	}

	public synchronized void addDescriptor(IArtifactDescriptor toAdd, IProgressMonitor monitor) {
		boolean lockAcquired = false;
		try {
			if (canLock()) {
				lockAcquired = lockAndLoad(false, monitor);
				if (!lockAcquired)
					return;
			}

			if (artifactDescriptors.contains(toAdd))
				return;

			SimpleArtifactDescriptor internalDescriptor = createInternalDescriptor(toAdd);
			artifactDescriptors.add(internalDescriptor);
			mapDescriptor(internalDescriptor);
			save();
		} finally {
			if (lockAcquired)
				unlock();
		}
	}

	public IArtifactDescriptor createArtifactDescriptor(IArtifactKey key) {
		return new SimpleArtifactDescriptor(key);
	}

	private SimpleArtifactDescriptor createInternalDescriptor(IArtifactDescriptor descriptor) {
		SimpleArtifactDescriptor internal = new SimpleArtifactDescriptor(descriptor);

		internal.setRepository(this);
		if (isFolderBased(descriptor))
			internal.setRepositoryProperty(ARTIFACT_FOLDER, Boolean.TRUE.toString());

		//clear out the UUID if we aren't using the blobstore.
		if (flatButPackedEnabled(descriptor) && internal.getProperty(ARTIFACT_UUID) != null) {
			internal.setProperty(ARTIFACT_UUID, null);
		}

		if (descriptor instanceof SimpleArtifactDescriptor) {
			Map<String, String> repoProperties = ((SimpleArtifactDescriptor) descriptor).getRepositoryProperties();
			for (Map.Entry<String, String> entry : repoProperties.entrySet()) {
				internal.setRepositoryProperty(entry.getKey(), entry.getValue());
			}
		}
		return internal;
	}

	public synchronized void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		boolean lockAcquired = false;
		try {
			if (canLock()) {
				lockAcquired = lockAndLoad(false, monitor);
				if (!lockAcquired)
					return;
			}

			for (int i = 0; i < descriptors.length; i++) {
				if (artifactDescriptors.contains(descriptors[i]))
					continue;
				SimpleArtifactDescriptor internalDescriptor = createInternalDescriptor(descriptors[i]);
				artifactDescriptors.add(internalDescriptor);
				mapDescriptor(internalDescriptor);
			}
			save();
		} finally {
			if (lockAcquired)
				unlock();
		}
	}

	private synchronized OutputStream addPostSteps(ProcessingStepHandler handler, IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		ArrayList<ProcessingStep> steps = new ArrayList<ProcessingStep>();
		steps.add(new SignatureVerifier());
		if (MD5_ARTIFACT_CHECK_ENABLED && descriptor.getProperty(IArtifactDescriptor.ARTIFACT_MD5) != null)
			steps.add(new MD5Verifier(descriptor.getProperty(IArtifactDescriptor.ARTIFACT_MD5)));
		if (steps.isEmpty())
			return destination;
		ProcessingStep[] stepArray = steps.toArray(new ProcessingStep[steps.size()]);
		// TODO should probably be using createAndLink here
		return handler.link(stepArray, destination, monitor);
	}

	private OutputStream addPreSteps(ProcessingStepHandler handler, IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		ArrayList<ProcessingStep> steps = new ArrayList<ProcessingStep>();
		if (IArtifactDescriptor.TYPE_ZIP.equals(descriptor.getProperty(IArtifactDescriptor.DOWNLOAD_CONTENTTYPE)))
			steps.add(new ZipVerifierStep());
		if (MD5_CHECK_ENABLED && descriptor.getProperty(IArtifactDescriptor.DOWNLOAD_MD5) != null)
			steps.add(new MD5Verifier(descriptor.getProperty(IArtifactDescriptor.DOWNLOAD_MD5)));
		// Add steps here if needed
		if (steps.isEmpty())
			return destination;
		ProcessingStep[] stepArray = steps.toArray(new ProcessingStep[steps.size()]);
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
		if (!holdsLock() && URIUtil.isFileURI(getLocation())) {
			load(new NullProgressMonitor());
		}
		SimpleArtifactDescriptor simpleDescriptor = createInternalDescriptor(descriptor);
		return artifactDescriptors.contains(simpleDescriptor);
	}

	public synchronized boolean contains(IArtifactKey key) {
		if (!holdsLock() && URIUtil.isFileURI(getLocation())) {
			load(new NullProgressMonitor());
		}
		return artifactMap.containsKey(key);
	}

	public synchronized URI createLocation(ArtifactDescriptor descriptor) {
		if (flatButPackedEnabled(descriptor)) {
			return getLocationForPackedButFlatArtifacts(descriptor);
		}
		// if the descriptor is canonical, clear out any UUID that might be set and use the Mapper
		if (descriptor.getProcessingSteps().length == 0) {
			descriptor.setProperty(ARTIFACT_UUID, null);
			IArtifactKey key = descriptor.getArtifactKey();
			URI result = mapper.map(getLocation(), key.getClassifier(), key.getId(), key.getVersion().toString(), descriptor.getProperty(IArtifactDescriptor.FORMAT));
			if (result != null) {
				if (isFolderBased(descriptor) && URIUtil.lastSegment(result).endsWith(JAR_EXTENSION)) {
					return URIUtil.removeFileExtension(result);
				}
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
		SimpleArtifactDescriptor simple = null;
		if (descriptor instanceof SimpleArtifactDescriptor)
			simple = (SimpleArtifactDescriptor) descriptor;
		else
			simple = createInternalDescriptor(descriptor);
		if (simple.getRepositoryProperty(SimpleArtifactDescriptor.ARTIFACT_REFERENCE) == null) {
			File file = getArtifactFile(descriptor);
			if (file != null) {
				// If the file != null remove it, otherwise just remove
				// the descriptor
				delete(file);
				if (file.exists())
					return false;
			}
		}
		boolean result = artifactDescriptors.remove(descriptor);
		if (result)
			unmapDescriptor(descriptor);

		return result;
	}

	protected IStatus downloadArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		if (isFolderBased(descriptor)) {
			File artifactFolder = getArtifactFile(descriptor);
			if (artifactFolder == null) {
				if (getLocation(descriptor) != null && !URIUtil.isFileURI(getLocation(descriptor)))
					return reportStatus(descriptor, destination, new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.folder_artifact_not_file_repo, descriptor.getArtifactKey())));
				return reportStatus(descriptor, destination, new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.artifact_not_found, descriptor.getArtifactKey())));
			}
			// TODO: optimize and ensure manifest is written first
			File zipFile = null;
			long start = System.currentTimeMillis();
			long totalArtifactSize = 0;
			try {
				zipFile = File.createTempFile(artifactFolder.getName(), JAR_EXTENSION, null);
				FileUtils.zip(artifactFolder.listFiles(), null, zipFile, FileUtils.createRootPathComputer(artifactFolder));
				FileInputStream fis = new FileInputStream(zipFile);
				totalArtifactSize += FileUtils.copyStream(fis, true, destination, false);
			} catch (IOException e) {
				return reportStatus(descriptor, destination, new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e));
			} finally {
				if (zipFile != null)
					zipFile.delete();
			}
			long end = System.currentTimeMillis();
			DownloadStatus statusWithDownloadSpeed = new DownloadStatus(IStatus.OK, Activator.ID, Status.OK_STATUS.getMessage());
			try {
				statusWithDownloadSpeed.setFileSize(totalArtifactSize);
				statusWithDownloadSpeed.setTransferRate(totalArtifactSize / Math.max((end - start), 1) * 1000);
			} catch (NumberFormatException e) {
				// ignore
			}
			return reportStatus(descriptor, destination, statusWithDownloadSpeed);
		}

		//download from the best available mirror
		URI baseLocation = getLocation(descriptor);
		if (baseLocation == null)
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.no_location, descriptor));
		URI mirrorLocation = getMirror(baseLocation, monitor);
		IStatus status = downloadArtifact(descriptor, mirrorLocation, destination, monitor);
		IStatus result = reportStatus(descriptor, destination, status);
		// if the original download went reasonably but the reportStatus found some issues
		// (e..g, in the processing steps/validators) then mark the mirror as bad and return
		// a retry code (assuming we have more mirrors)
		if ((status.isOK() || status.matches(IStatus.INFO | IStatus.WARNING)) && result.getSeverity() == IStatus.ERROR && !artifactError(result)) {
			if (mirrors != null) {
				mirrors.reportResult(mirrorLocation.toString(), result);
				if (mirrors.hasValidMirror())
					return new MultiStatus(Activator.ID, CODE_RETRY, new IStatus[] {result}, "Retry another mirror", null); //$NON-NLS-1$
			}
		}
		// if the original status was a retry, don't lose that.
		return status.getCode() == CODE_RETRY ? status : result;
	}

	/**
	 * Return true if there is an 'artifact error'. You cannot retry when an artifact error is found.
	 * @return True if the status (or children status) have an artifact error status code. False otherwise
	 */
	private boolean artifactError(IStatus status) {
		if (status.getCode() == MirrorRequest.ARTIFACT_PROCESSING_ERROR)
			return true;
		if (status.getChildren() != null) {
			IStatus[] children = status.getChildren();
			for (IStatus child : children) {
				if (artifactError(child))
					return true;
			}
		}
		return false;
	}

	/**
	 * Copy a file to an output stream.
	 * Optionally close the streams when done.
	 * Notify the monitor about progress we make
	 * 
	 * @return the number of bytes written.
	 */
	private IStatus copyFileToStream(File in, OutputStream out, IProgressMonitor monitor) {
		// Buffer filled with contents from the stream at a time
		int bufferSize = 16 * 1024;
		byte[] buffer = new byte[bufferSize];
		// Number of passes in the below loop, convert to integer which is needed in monitor conversion below
		int expected_loops = new Double(in.length() / bufferSize).intValue() + 1; // +1: also count the initial run
		SubMonitor sub = SubMonitor.convert(monitor, Messages.downloading + in.getName(), expected_loops);
		// Be optimistic about the outcome of this...
		IStatus status = new DownloadStatus(IStatus.OK, Activator.ID, Status.OK_STATUS.getMessage());
		try {
			long start = System.currentTimeMillis();
			FileInputStream stream = new FileInputStream(in);
			try {
				int len;
				while ((len = stream.read(buffer)) != -1) {
					out.write(buffer, 0, len);
					sub.worked(1);
				}
			} finally {
				stream.close();
			}
			long end = System.currentTimeMillis();
			((DownloadStatus) status).setFileSize(in.length());
			((DownloadStatus) status).setLastModified(in.lastModified());
			((DownloadStatus) status).setTransferRate(in.length() / Math.max((end - start), 1) * 1000);
		} catch (IOException ioe) {
			status = new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.error_copying_local_file, in.getAbsolutePath()), ioe);
		}
		sub.done();
		return status;
	}

	private IStatus downloadArtifact(IArtifactDescriptor descriptor, URI mirrorLocation, OutputStream destination, IProgressMonitor monitor) {
		//Bug 340352: transport has performance overhead of 100ms and more, bypass it for local copies
		IStatus result = Status.OK_STATUS;
		if (mirrorLocation.getScheme().equals(SimpleArtifactRepositoryFactory.PROTOCOL_FILE))
			result = copyFileToStream(new File(mirrorLocation), destination, monitor);
		else
			result = getTransport().download(mirrorLocation, destination, monitor);
		if (mirrors != null)
			mirrors.reportResult(mirrorLocation.toString(), result);
		if (result.isOK() || result.getSeverity() == IStatus.CANCEL)
			return result;
		if (monitor.isCanceled())
			return Status.CANCEL_STATUS;
		// If there are more valid mirrors then return an error with a special code that tells the caller
		// to keep trying.  Note that the message in the status is largely irrelevant but the child
		// status tells the story of why we failed on this try.
		// TODO find a better way of doing this.
		if (mirrors != null && mirrors.hasValidMirror())
			return new MultiStatus(Activator.ID, CODE_RETRY, new IStatus[] {result}, "Retry another mirror", null); //$NON-NLS-1$
		return result;
	}

	/**
	 * Returns an equivalent mirror location for the given artifact location.
	 * @param baseLocation The location of the artifact in this repository
	 * @return the Location of the artifact in this repository, or an equivalent mirror
	 */
	private synchronized URI getMirror(URI baseLocation, IProgressMonitor monitor) {
		if (!MIRRORS_ENABLED || (!isForceThreading() && isLocal()))
			return baseLocation;
		if (mirrors == null)
			mirrors = new MirrorSelector(this, getTransport());
		return mirrors.getMirrorLocation(baseLocation, monitor);
	}

	public <T> T getAdapter(Class<T> adapter) {
		// if we are adapting to file or writable repositories then make sure we have a file location
		if (adapter == IFileArtifactRepository.class)
			if (!isLocal())
				return null;
		return super.getAdapter(adapter);
	}

	IStatus getArtifact(IArtifactRequest request, IProgressMonitor monitor) {
		request.perform(this, monitor);
		return request.getResult();
	}

	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		if (!holdsLock() && URIUtil.isFileURI(getLocation())) {
			load(new NullProgressMonitor());
		}
		if (monitor.isCanceled())
			return Status.CANCEL_STATUS;
		ProcessingStepHandler handler = new ProcessingStepHandler();
		destination = processDestination(handler, descriptor, destination, monitor);
		IStatus status = ProcessingStepHandler.checkStatus(destination);
		if (!status.isOK() && status.getSeverity() != IStatus.INFO)
			return status;

		return downloadArtifact(descriptor, destination, monitor);
	}

	public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		if (!holdsLock() && URIUtil.isFileURI(getLocation())) {
			load(new NullProgressMonitor());
		}
		if (monitor.isCanceled())
			return Status.CANCEL_STATUS;
		return downloadArtifact(descriptor, destination, monitor);
	}

	public synchronized IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		if (!holdsLock() && URIUtil.isFileURI(getLocation())) {
			load(new NullProgressMonitor());
		}

		List<IArtifactDescriptor> result = artifactMap.get(key);
		if (result == null)
			return new IArtifactDescriptor[0];

		return result.toArray(new IArtifactDescriptor[result.size()]);
	}

	public File getArtifactFile(IArtifactDescriptor descriptor) {
		URI result = getLocation(descriptor);
		if (result == null || !URIUtil.isFileURI(result))
			return null;
		return URIUtil.toFile(result);
	}

	public File getArtifactFile(IArtifactKey key) {
		IArtifactDescriptor descriptor = getCompleteArtifactDescriptor(key);
		if (descriptor == null)
			return null;
		return getArtifactFile(descriptor);
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		if (!holdsLock() && URIUtil.isFileURI(getLocation())) {
			load(new NullProgressMonitor());
		}
		if (monitor.isCanceled())
			return Status.CANCEL_STATUS;

		final MultiStatus overallStatus = new MultiStatus(Activator.ID, IStatus.OK, NLS.bind(Messages.message_problemReadingArtifact, getLocation()), null);
		LinkedList<IArtifactRequest> requestsPending = new LinkedList<IArtifactRequest>(Arrays.asList(requests));

		int numberOfJobs = Math.min(requests.length, getMaximumThreads());
		if (numberOfJobs <= 1 || (!isForceThreading() && isLocal())) {
			SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
			try {
				for (int i = 0; i < requests.length; i++) {
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
					IStatus result = getArtifact(requests[i], subMonitor.newChild(1));
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

		if (monitor.isCanceled())
			return Status.CANCEL_STATUS;
		else if (overallStatus.isOK())
			return Status.OK_STATUS;
		else
			return overallStatus;
	}

	public synchronized IArtifactDescriptor getCompleteArtifactDescriptor(IArtifactKey key) {
		if (!holdsLock() && URIUtil.isFileURI(getLocation())) {
			load(new NullProgressMonitor());
		}
		List<IArtifactDescriptor> descriptors = artifactMap.get(key);
		if (descriptors == null)
			return null;

		for (IArtifactDescriptor desc : descriptors) {
			// look for a descriptor that matches the key and is "complete"
			if (desc.getArtifactKey().equals(key) && desc.getProcessingSteps().length == 0)
				return desc;
		}
		return null;
	}

	public synchronized Set<SimpleArtifactDescriptor> getDescriptors() {
		if (!holdsLock() && URIUtil.isFileURI(getLocation())) {
			load(new NullProgressMonitor());
		}
		return artifactDescriptors;
	}

	/**
	 * Typically non-canonical forms of the artifact are stored in the blob store.
	 * However, we support having the pack200 files alongside the canonical artifact
	 * for compatibility with the format used in optimized update sites.  We call
	 * this arrangement "flat but packed".
	 */
	private boolean flatButPackedEnabled(IArtifactDescriptor descriptor) {
		return Boolean.TRUE.toString().equals(getProperties().get(PUBLISH_PACK_FILES_AS_SIBLINGS)) && IArtifactDescriptor.FORMAT_PACKED.equals(descriptor.getProperty(IArtifactDescriptor.FORMAT));
	}

	/**
	 * @see #flatButPackedEnabled(IArtifactDescriptor)
	 */
	private URI getLocationForPackedButFlatArtifacts(IArtifactDescriptor descriptor) {
		IArtifactKey key = descriptor.getArtifactKey();
		return mapper.map(getLocation(), key.getClassifier(), key.getId(), key.getVersion().toString(), descriptor.getProperty(IArtifactDescriptor.FORMAT));
	}

	public synchronized URI getLocation(IArtifactDescriptor descriptor) {
		// if the artifact has a uuid then use it
		String uuid = descriptor.getProperty(ARTIFACT_UUID);
		if (uuid != null)
			return blobStore.fileFor(bytesFromHexString(uuid));

		if (flatButPackedEnabled(descriptor)) {
			return getLocationForPackedButFlatArtifacts(descriptor);
		}

		try {
			// if the artifact is just a reference then return the reference location
			if (descriptor instanceof SimpleArtifactDescriptor) {
				String artifactReference = ((SimpleArtifactDescriptor) descriptor).getRepositoryProperty(SimpleArtifactDescriptor.ARTIFACT_REFERENCE);
				if (artifactReference != null) {
					try {
						return new URI(artifactReference);
					} catch (URISyntaxException e) {
						return URIUtil.fromString(artifactReference);
					}
				}
			}

			// if the descriptor is complete then use the mapping rules...
			if (descriptor.getProcessingSteps().length == 0) {
				IArtifactKey key = descriptor.getArtifactKey();
				URI result = mapper.map(getLocation(), key.getClassifier(), key.getId(), key.getVersion().toString(), descriptor.getProperty(IArtifactDescriptor.FORMAT));
				if (result != null) {
					if (isFolderBased(descriptor) && URIUtil.lastSegment(result).endsWith(JAR_EXTENSION))
						return URIUtil.removeFileExtension(result);
					if (result.getScheme() == null && "file".equals(getLocation().getScheme())) //$NON-NLS-1$
						return URIUtil.makeAbsolute(result, new File(System.getProperty("user.dir")).toURI()); //$NON-NLS-1$
					return result;
				}
			}
		} catch (URISyntaxException e) {
			return null;
		}
		// in the end there is not enough information so return null 
		return null;
	}

	/**
	 * Returns the maximum number of concurrent download threads.
	 */
	private int getMaximumThreads() {
		int repoMaxThreads = DEFAULT_MAX_THREADS;
		int userMaxThreads = DEFAULT_MAX_THREADS;
		try {
			String maxThreadString = getProperties().get(PROP_MAX_THREADS);
			if (maxThreadString != null)
				repoMaxThreads = Math.max(1, Integer.parseInt(maxThreadString));
		} catch (NumberFormatException nfe) {
			// default number of threads
		}
		try {
			String maxThreadString = Activator.getContext().getProperty(PROP_MAX_THREADS);
			if (maxThreadString != null)
				userMaxThreads = Math.max(1, Integer.parseInt(maxThreadString));
		} catch (NumberFormatException nfe) {
			// default number of threads
		}
		return Math.min(repoMaxThreads, userMaxThreads);
	}

	public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
		if (!holdsLock() && URIUtil.isFileURI(getLocation())) {
			load(new NullProgressMonitor());
		}

		assertModifiable();

		// Create a copy of the original descriptor that we can manipulate and add to our repo.
		ArtifactDescriptor newDescriptor = createInternalDescriptor(descriptor);

		// Check if the artifact is already in this repository, check the newDescriptor instead of the original
		// since the implementation of hash/equals on the descriptor matters here.
		if (contains(newDescriptor)) {
			String msg = NLS.bind(Messages.available_already_in, getLocation().toString());
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.ARTIFACT_EXISTS, msg, null));
		}

		// Determine writing location
		URI newLocation = createLocation(newDescriptor);
		if (newLocation == null)
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.no_location, newDescriptor)));
		String file = URIUtil.toFile(newLocation).getAbsolutePath();

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
				mkdirs(outputFile);
				if (!outputFile.isDirectory())
					throw failedWrite(new IOException(NLS.bind(Messages.sar_failedMkdir, outputFile.toString())));
				target = new ZippedFolderOutputStream(outputFile);
			} else {
				// file based
				File parent = outputFile.getParentFile();
				mkdirs(parent);
				if (!parent.isDirectory())
					throw failedWrite(new IOException(NLS.bind(Messages.sar_failedMkdir, parent.toString())));
				target = new FileOutputStream(file);
			}

			// finally create and return an output stream suitably wrapped so that when it is 
			// closed the repository is updated with the descriptor
			return new ArtifactOutputStream(new BufferedOutputStream(target), newDescriptor, outputFile);
		} catch (IOException e) {
			throw failedWrite(e);
		}

	}

	/**
	 * We implement mkdirs ourselves because this code is known to run in
	 * highly concurrent scenarios, and there is a race condition in the JRE implementation
	 * of mkdirs (see bug 265654).
	 */
	private void mkdirs(File dir) {
		if (dir == null || dir.exists())
			return;
		if (dir.mkdir())
			return;
		mkdirs(dir.getParentFile());
		dir.mkdir();
	}

	private ProvisionException failedWrite(Exception e) throws ProvisionException {
		String msg = NLS.bind(Messages.repoFailedWrite, getLocation());
		throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_WRITE, msg, e));
	}

	public synchronized String[][] getRules() {
		if (!holdsLock() && URIUtil.isFileURI(getLocation())) {
			load(new NullProgressMonitor());
		}
		return mappingRules;
	}

	private Transport getTransport() {
		return (Transport) getProvisioningAgent().getService(Transport.SERVICE_NAME);
	}

	// use this method to setup any transient fields etc after the object has been restored from a stream
	public synchronized void initializeAfterLoad(URI repoLocation) {
		this.initializeAfterLoad(repoLocation, true);
	}

	private synchronized void initializeAfterLoad(URI repoLocation, boolean updateTimestamp) {
		setLocation(repoLocation);
		String suffix = getBlobStoreName(BLOBSTORE);
		blobStore = new BlobStore(getBlobStoreLocation(repoLocation, suffix), 128);
		initializeMapper();
		for (SimpleArtifactDescriptor desc : artifactDescriptors)
			desc.setRepository(this);
		if (updateTimestamp)
			updateTimestamp();
		if (canLock == null)
			canLock = Boolean.valueOf(canLock());
	}

	private String getBlobStoreName(String defaultValue) {
		String value = getProperty(PROP_BLOBSTORE_NAME);
		if (value == null || value.length() == 0) {
			return defaultValue;
		}
		return value;
	}

	private synchronized void initializeMapper() {
		mapper = new Mapper();
		mapper.initialize(Activator.getContext(), mappingRules);
	}

	private boolean isFolderBased(IArtifactDescriptor descriptor) {
		// This is called from createInternalDescriptor, so if we aren't a
		// SimpleArtifactDescriptor then just check the descriptor properties instead 
		// of creating the internal descriptor.
		SimpleArtifactDescriptor internalDescriptor = null;
		if (descriptor instanceof SimpleArtifactDescriptor)
			internalDescriptor = (SimpleArtifactDescriptor) descriptor;
		if (internalDescriptor != null) {
			String useArtifactFolder = internalDescriptor.getRepositoryProperty(ARTIFACT_FOLDER);
			if (useArtifactFolder != null)
				return Boolean.parseBoolean(useArtifactFolder);
		}
		return Boolean.parseBoolean(descriptor.getProperty(ARTIFACT_FOLDER));
	}

	private boolean isForceThreading() {
		return "true".equals(getProperties().get(PROP_FORCE_THREADING)); //$NON-NLS-1$
	}

	private boolean isLocal() {
		return "file".equalsIgnoreCase(getLocation().getScheme()); //$NON-NLS-1$
	}

	public boolean isModifiable() {
		return isLocal();
	}

	public OutputStream processDestination(ProcessingStepHandler handler, IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		destination = addPostSteps(handler, descriptor, destination, monitor);
		destination = handler.createAndLink(getProvisioningAgent(), descriptor.getProcessingSteps(), descriptor, destination, monitor);
		destination = addPreSteps(handler, descriptor, destination, monitor);
		return destination;
	}

	public synchronized void removeAll(IProgressMonitor monitor) {
		boolean lockAcquired = false;
		try {
			if (canLock()) {
				lockAcquired = lockAndLoad(false, monitor);
				if (!lockAcquired)
					return;
			}

			IArtifactDescriptor[] toRemove = artifactDescriptors.toArray(new IArtifactDescriptor[artifactDescriptors.size()]);
			boolean changed = false;
			for (int i = 0; i < toRemove.length; i++)
				changed |= doRemoveArtifact(toRemove[i]);
			if (changed)
				save();
		} finally {
			if (lockAcquired)
				unlock();
		}
	}

	public synchronized void removeDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		boolean lockAcquired = false;
		try {
			if (canLock()) {
				lockAcquired = lockAndLoad(false, monitor);
				if (!lockAcquired)
					return;
			}

			if (doRemoveArtifact(descriptor))
				save();
		} finally {
			if (lockAcquired)
				unlock();
		}
	}

	public synchronized void removeDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		boolean lockAcquired = false;
		try {
			if (canLock()) {
				lockAcquired = lockAndLoad(false, monitor);
				if (!lockAcquired)
					return;
			}

			boolean changed = false;
			for (IArtifactDescriptor descriptor : descriptors)
				changed |= doRemoveArtifact(descriptor);
			if (changed)
				save();
		} finally {
			if (lockAcquired)
				unlock();
		}
	}

	public synchronized void removeDescriptors(IArtifactKey[] keys, IProgressMonitor monitor) {
		boolean lockAcquired = false;
		try {
			if (canLock()) {
				lockAcquired = lockAndLoad(false, monitor);
				if (!lockAcquired)
					return;
			}

			boolean changed = false;
			for (IArtifactKey key : keys) {
				IArtifactDescriptor[] descriptors = getArtifactDescriptors(key);
				for (IArtifactDescriptor descriptor : descriptors)
					changed |= doRemoveArtifact(descriptor);
			}
			if (changed)
				save();
		} finally {
			if (lockAcquired)
				unlock();
		}
	}

	public synchronized void removeDescriptor(IArtifactKey key, IProgressMonitor monitor) {
		boolean lockAcquired = false;
		try {
			if (canLock()) {
				lockAcquired = lockAndLoad(false, monitor);
				if (!lockAcquired)
					return;
			}

			IArtifactDescriptor[] toRemove = getArtifactDescriptors(key);
			boolean changed = false;
			for (int i = 0; i < toRemove.length; i++)
				changed |= doRemoveArtifact(toRemove[i]);
			if (changed)
				save();
		} finally {
			if (lockAcquired)
				unlock();
		}
	}

	private IStatus reportStatus(IArtifactDescriptor descriptor, OutputStream destination, IStatus status) {
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

		// An error occurred obtaining the artifact, ProcessingStep errors aren't important
		if (status.matches(IStatus.ERROR))
			return status;

		IStatus stepStatus = ProcessingStepHandler.getErrorStatus(destination);
		// if the steps all ran ok and there is no interesting information, return the status from this method
		if (!stepStatus.isMultiStatus() && stepStatus.isOK())
			return status;
		// else gather up the status from the steps
		MultiStatus result = new MultiStatus(Activator.ID, IStatus.OK, new IStatus[0], NLS.bind(Messages.sar_reportStatus, descriptor.getArtifactKey().toExternalForm()), null);

		if (!status.isOK()) {
			// Transport pushes its status onto the output stream if the stream implements IStateful, to prevent
			// duplication determine if the Status is present in the ProcessingStep status. 
			boolean found = false;
			IStatus[] stepStatusChildren = stepStatus.getChildren();
			for (int i = 0; i < stepStatusChildren.length && !found; i++)
				if (stepStatusChildren[i] == status)
					found = true;
			if (!found)
				result.merge(status);
		}

		result.merge(stepStatus);
		return result;
	}

	public void save() {
		if (disableSave)
			return;
		boolean compress = "true".equalsIgnoreCase(getProperty(PROP_COMPRESSED)); //$NON-NLS-1$
		save(compress);
	}

	private void save(boolean compress) {
		assertModifiable();
		OutputStream os = null;
		try {
			try {
				URI actualLocation = getActualLocation(getLocation(), false);
				File artifactsFile = URIUtil.toFile(actualLocation);
				File jarFile = URIUtil.toFile(getActualLocation(getLocation(), true));
				if (!compress) {
					if (jarFile.exists()) {
						jarFile.delete();
					}
					if (!artifactsFile.exists()) {
						// create parent folders
						mkdirs(artifactsFile.getParentFile());
					}
					os = new FileOutputStream(artifactsFile);
				} else {
					if (artifactsFile.exists()) {
						artifactsFile.delete();
					}
					if (!jarFile.exists()) {
						mkdirs(jarFile.getParentFile());
						jarFile.createNewFile();
					}
					os = new JarOutputStream(new FileOutputStream(jarFile));
					((JarOutputStream) os).putNextEntry(new JarEntry(new Path(artifactsFile.getAbsolutePath()).lastSegment()));
				}
				super.setProperty(IRepository.PROP_TIMESTAMP, Long.toString(System.currentTimeMillis()), new NullProgressMonitor());
				new SimpleArtifactRepositoryIO(getProvisioningAgent()).write(this, os);
			} catch (IOException e) {
				// TODO proper exception handling
				e.printStackTrace();
			} finally {
				if (os != null)
					os.close();
				updateTimestamp();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String doSetProperty(String key, String newValue, IProgressMonitor monitor, boolean save) {
		String oldValue = super.setProperty(key, newValue, new NullProgressMonitor());
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
		if (save)
			save();
		return oldValue;
	}

	public String setProperty(String key, String newValue, IProgressMonitor monitor) {
		boolean lockAcquired = false;
		try {
			if (canLock()) {
				lockAcquired = lockAndLoad(false, monitor);
				if (!lockAcquired)
					return super.getProperty(key);
			}
			return doSetProperty(key, newValue, monitor, true);
		} finally {
			if (lockAcquired)
				unlock();
		}
	}

	public synchronized void setRules(String[][] rules) {
		mappingRules = rules;
	}

	public String toString() {
		return getLocation().toString();
	}

	public IQueryable<IArtifactDescriptor> descriptorQueryable() {
		return new IQueryable<IArtifactDescriptor>() {
			public IQueryResult<IArtifactDescriptor> query(IQuery<IArtifactDescriptor> query, IProgressMonitor monitor) {
				synchronized (SimpleArtifactRepository.this) {
					snapshotNeeded = true;
					Collection<List<IArtifactDescriptor>> descs = SimpleArtifactRepository.this.artifactMap.values();
					return query.perform(new CompoundIterator<IArtifactDescriptor>(descs.iterator()));
				}
			}
		};
	}

	public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
		return IndexProvider.query(this, query, monitor);
	}

	public synchronized Iterator<IArtifactKey> everything() {
		if (!holdsLock() && URIUtil.isFileURI(getLocation())) {
			load(new NullProgressMonitor());
		}
		snapshotNeeded = true;
		return artifactMap.keySet().iterator();
	}

	public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
		IStatus result = null;

		boolean lockAcquired = false;
		synchronized (this) {
			try {
				if (canLock()) {
					lockAcquired = lockAndLoad(false, monitor);
					if (!lockAcquired)
						return new Status(IStatus.ERROR, Activator.ID, "Could not lock artifact repository for writing", null); //$NON-NLS-1$
				}

				disableSave = true;
				runnable.run(monitor);
			} catch (OperationCanceledException oce) {
				return new Status(IStatus.CANCEL, Activator.ID, oce.getMessage(), oce);
			} catch (Throwable e) {
				result = new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e);
			} finally {
				disableSave = false;
				try {
					save();
				} catch (Exception e) {
					if (result != null)
						result = new MultiStatus(Activator.ID, IStatus.ERROR, new IStatus[] {result}, e.getMessage(), e);
					else
						result = new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e);
				} finally {
					if (lockAcquired)
						unlock();
				}
			}
		}
		if (result == null)
			result = Status.OK_STATUS;
		return result;
	}

	public synchronized IIndex<IArtifactKey> getIndex(String memberName) {
		if (!holdsLock() && URIUtil.isFileURI(getLocation())) {
			load(new NullProgressMonitor());
		}
		if (ArtifactKey.MEMBER_ID.equals(memberName)) {
			snapshotNeeded = true;
			if (keyIndex == null)
				keyIndex = new KeyIndex(artifactMap.keySet());
			return keyIndex;
		}
		return null;
	}

	public Object getManagedProperty(Object client, String memberName, Object key) {
		return null;
	}

	/**
	 * Locks the location and optionally loads the repository.
	 * 
	 * @param ignoreLoad If ignoreLoad is set to true, then the location is locked
	 *                   but the repository is not loaded.  It is expected
	 *                   that the caller will load the repository manually
	 * @return Tue if the lock was acquired, false otherwise
	 */
	private synchronized boolean lockAndLoad(boolean ignoreLoad, IProgressMonitor monitor) {
		if (holdsLock) {
			throw new IllegalStateException("Locking is not reentrant"); //$NON-NLS-1$
		}
		holdsLock = false;
		boolean success = true;

		try {
			try {
				holdsLock = lock(true, monitor);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

			if (holdsLock) {
				if (!ignoreLoad) {
					success = false;
					doLoad(new NullProgressMonitor());
					success = true;
				}
				return true;
			}
			return false;
		} finally {
			// If we did not successfully load the repository, make sure we free the lock.
			// This will only happen if doLoad() throws an exception, otherwise
			// we will set success to true, and return above
			if (!success)
				unlock();
		}
	}

	private synchronized boolean canLock() {
		if (holdsLock())
			return false;
		if (!URIUtil.isFileURI(getLocation()))
			return false;

		try {
			lockLocation = getLockLocation();
		} catch (IOException e) {
			return false;
		}
		return !lockLocation.isReadOnly();
	}

	/**
	 * Actually lock the location.  This method should only be called
	 * from LockAndLoad. If you only want to lock the repository and not
	 * load it, see {@link SimpleArtifactRepository#lockAndLoad(boolean, IProgressMonitor)}.
	 */
	private synchronized boolean lock(boolean wait, IProgressMonitor monitor) throws IOException {
		if (!Activator.getInstance().enableArtifactLocking())
			return true;

		if (holdsLock()) {
			throw new IllegalStateException("Locking is not reentrant"); //$NON-NLS-1$
		}

		lockLocation = getLockLocation();
		boolean locked = lockLocation.lock();
		if (locked || !wait)
			return locked;

		//Someone else must have the directory locked
		while (true) {
			if (monitor.isCanceled())
				return false;
			try {
				Thread.sleep(200); // 5x per second
			} catch (InterruptedException e) {/*ignore*/
			}
			locked = lockLocation.lock();
			if (locked)
				return true;
		}
	}

	/**
	 * Returns true if this instance of SimpleArtifactRepository holds the lock or 
	 * this repository can't be locked at all due to permission, false otherwise.
	 */
	private boolean holdsLock() {
		return (canLock != null && !canLock.booleanValue()) || holdsLock;
	}

	/**URIUtil.toURI(location.toURI()
	 * Returns the location of the lock file.
	 */
	private Location getLockLocation() throws IOException {
		if (this.lockLocation != null)
			return this.lockLocation;

		URI repositoryLocation = getLocation();
		if (!URIUtil.isFileURI(repositoryLocation)) {
			throw new IOException("Cannot lock a non file based repository"); //$NON-NLS-1$
		}

		return Activator.getInstance().getLockLocation(repositoryLocation);
	}

	/**
	 * Loads the repository from disk. This method will do nothing
	 * if this instance of SimpleArtifactRepository holds the lock
	 * because it will have loaded the repo when it acquired the lock.
	 * 
	 * @param monitor
	 */
	private void load(IProgressMonitor monitor) {
		if (!holdsLock())
			doLoad(monitor);
		else
			monitor.done();
	}

	private void updateTimestamp() {
		if (!isModifiable())
			return;
		try {
			SimpleArtifactRepositoryFactory repositoryFactory = new SimpleArtifactRepositoryFactory();
			File localFile = repositoryFactory.getLocalFile(getLocation(), new NullProgressMonitor());
			long lastModified = localFile.lastModified();
			if (lastModified > 0)
				cacheTimestamp = lastModified;
		} catch (Exception e) {
			// Do nothing
		}
	}

	/**
	 * Loads the repository from disk. If the last modified timestamp on the file <=
	 * to our cache, then this method does nothing.  Otherwise the artifact repository
	 * on disk is loaded, and reconciled with this instance of the artifact repository.
	 * 
	 * @param monitor
	 */
	private void doLoad(IProgressMonitor monitor) {

		SimpleArtifactRepositoryFactory repositoryFactory = new SimpleArtifactRepositoryFactory();
		IArtifactRepository repositoryOnDisk = null;
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, 4);
			try {
				File localFile = repositoryFactory.getLocalFile(getLocation(), subMonitor.newChild(1));
				long lastModified = localFile.lastModified();
				if (lastModified <= cacheTimestamp)
					return;
				cacheTimestamp = lastModified;
			} catch (Exception e) {
				// Dont'r worry if we can't load
				return;
			}
			try {
				repositoryOnDisk = repositoryFactory.load(getLocation(), IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, subMonitor.newChild(3), false);
			} catch (Exception e) {
				// Don't worry if we can't load
				return;
			}

			if (repositoryOnDisk != null && repositoryOnDisk instanceof SimpleArtifactRepository) {
				setName(repositoryOnDisk.getName());
				setType(repositoryOnDisk.getType());
				setVersion(repositoryOnDisk.getVersion());
				setLocation(repositoryOnDisk.getLocation()); // Will this ever change, should it?
				setDescription(repositoryOnDisk.getDescription());
				setProvider(repositoryOnDisk.getProvider());
				this.mappingRules = ((SimpleArtifactRepository) repositoryOnDisk).mappingRules;

				// Clear the existing properties
				//				this.setProperties(new OrderedProperties());
				//
				Map<String, String> prop = repositoryOnDisk.getProperties();
				Set<Entry<String, String>> entrySet = prop.entrySet();
				for (Entry<String, String> entry : entrySet) {
					doSetProperty(entry.getKey(), entry.getValue(), new NullProgressMonitor(), false);
				}

				//
				this.artifactDescriptors = ((SimpleArtifactRepository) repositoryOnDisk).artifactDescriptors;
				this.artifactMap = ((SimpleArtifactRepository) repositoryOnDisk).artifactMap;
			}
		} finally {
			monitor.done();
		}
		return;
	}

	private void unlock() {
		if (!Activator.getInstance().enableArtifactLocking()) {
			holdsLock = false;
			return;
		}
		if (lockLocation != null) {
			// If we don't have the lock location, then we don't have the lock
			holdsLock = false;
			lockLocation.release();
		}
		lockLocation = null;
	}

}
