package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

public class BundlePool implements IFileArtifactRepository {

	public static final String REPOSITORY_TYPE = "org.eclipse.equinox.p2.touchpoint.eclipse.bundlePool"; //$NON-NLS-1$
	private static final String BUNDLE_FOLDER = "bundle.folder"; //$NON-NLS-1$
	private static final String TMP_ZIP = "tmp.zip"; //$NON-NLS-1$
	public static final String PROFILE_EXTENSION = "profile.extension"; //$NON-NLS-1$

	// TODO: optimize
	// we could stream right into the folder
	public class ZippedFolderOutputStream extends OutputStream {

		private final File bundleFolder;
		private final FileOutputStream fos;
		private final File zipFile;

		public ZippedFolderOutputStream(File bundleFolder) throws FileNotFoundException {
			this.bundleFolder = bundleFolder;
			zipFile = new File(bundleFolder, TMP_ZIP);
			fos = new FileOutputStream(zipFile);
		}

		public void close() throws IOException {
			fos.close();
			FileUtils.unzipFile(zipFile, bundleFolder);
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

	private final SimpleArtifactRepository repository;

	public BundlePool(SimpleArtifactRepository repository) {
		if (repository == null)
			throw new IllegalArgumentException("repository must not be null");
		this.repository = repository;
	}

	public OutputStream getOutputStream(IArtifactDescriptor descriptor, IArtifactRequest request) {

		if (Boolean.valueOf(descriptor.getProperty(BUNDLE_FOLDER)).booleanValue()) {
			File bundleFolder = getBundleFolder(descriptor);
			if (bundleFolder == null)
				return null;

			if (bundleFolder.exists()) {
				System.err.println("Artifact repository out of synch. Overwriting " + bundleFolder.getAbsoluteFile()); //$NON-NLS-1$
				FileUtils.deleteAll(bundleFolder);
			}

			bundleFolder.mkdirs();

			// finally create and return an output stream suitably wrapped so that when it is 
			// closed the repository is updated with the descriptor
			try {
				return repository.new ArtifactOutputStream(new BufferedOutputStream(new ZippedFolderOutputStream(bundleFolder)), descriptor, request, bundleFolder);
			} catch (FileNotFoundException e) {
				Assert.isTrue(false, "Unexpected exception:" + e);
			}
			return null;
		}
		return repository.getOutputStream(descriptor, request);
	}

	private File getBundleFolder(IArtifactDescriptor descriptor) {
		ArtifactDescriptor destinationDescriptor = new ArtifactDescriptor(descriptor);

		String location = repository.createLocation(destinationDescriptor);
		return toBundleFolder(location);
	}

	public File getArtifactFile(IArtifactDescriptor descriptor) {
		if (Boolean.valueOf(descriptor.getProperty(BUNDLE_FOLDER)).booleanValue()) {
			String location = repository.getLocation(descriptor);
			return toBundleFolder(location);
		}
		return repository.getArtifactFile(descriptor);
	}

	private File toBundleFolder(String location) {
		if (location == null || !location.endsWith(".jar"))
			return null;

		location = location.substring(0, location.lastIndexOf(".jar"));

		try {
			location = new URL(location).getPath();
		} catch (MalformedURLException e) {
			// should not occur
			e.printStackTrace();
		}

		return new File(location);
	}

	public void addDescriptor(IArtifactDescriptor toAdd) {
		repository.addDescriptor(toAdd);
	}

	public boolean contains(IArtifactDescriptor descriptor) {
		return repository.contains(descriptor);
	}

	public boolean contains(IArtifactKey key) {
		return repository.contains(key);
	}

	public boolean equals(Object obj) {
		return repository.equals(obj);
	}

	public Object getAdapter(Class adapter) {
		return repository.getAdapter(adapter);
	}

	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		if (Boolean.valueOf(descriptor.getProperty(BUNDLE_FOLDER)).booleanValue()) {
			ProcessingStepHandler handler = new ProcessingStepHandler();
			destination = repository.processDestination(handler, descriptor, destination, monitor);
			IStatus status = handler.checkStatus(destination);
			if (!status.isOK() && status.getSeverity() != IStatus.INFO)
				return status;

			status = downloadArtifact(descriptor, destination, monitor);
			return repository.reportStatus(descriptor, destination, status);

		}
		return repository.getArtifact(descriptor, destination, monitor);
	}

	private IStatus downloadArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		File bundleFolder = getArtifactFile(descriptor);
		// TODO: optimize and ensure manifest is written first
		File zipFile = new File(bundleFolder, "tmp.zip");
		try {
			FileUtils.zip(new File[] {bundleFolder}, zipFile);
			FileInputStream fis = new FileInputStream(zipFile);
			FileUtils.copyStream(fis, true, destination, true);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.ID, e.getMessage());
		} finally {
			zipFile.delete();
		}
		return Status.OK_STATUS;
	}

	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		return repository.getArtifactDescriptors(key);
	}

	public File getArtifactFile(IArtifactKey key) {
		IArtifactDescriptor descriptor = repository.getCompleteArtifactDescriptor(key);
		if (descriptor == null)
			return null;
		return getArtifactFile(descriptor);
	}

	public IArtifactKey[] getArtifactKeys() {
		return repository.getArtifactKeys();
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		return repository.getArtifacts(requests, monitor);
	}

	public String getDescription() {
		return repository.getDescription();
	}

	public Set getDescriptors() {
		return repository.getDescriptors();
	}

	public URL getLocation() {
		return repository.getLocation();
	}

	public Map getModifiableProperties() {
		return repository.getModifiableProperties();
	}

	public String getName() {
		return repository.getName();
	}

	public Map getProperties() {
		return repository.getProperties();
	}

	public String getProvider() {
		return repository.getProvider();
	}

	public String[][] getRules() {
		return repository.getRules();
	}

	public boolean getSignatureVerification() {
		return repository.getSignatureVerification();
	}

	public String getType() {
		return REPOSITORY_TYPE;
	}

	public String getVersion() {
		return repository.getVersion();
	}

	public int hashCode() {
		return repository.hashCode();
	}

	public void initializeAfterLoad(URL location) {
		repository.initializeAfterLoad(location);
	}

	public boolean isModifiable() {
		return repository.isModifiable();
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

		FileUtils.deleteAll(file);
		if (!file.exists())
			return getDescriptors().remove(descriptor);
		return false;
	}

	public void removeAll() {
		IArtifactDescriptor[] toRemove = (IArtifactDescriptor[]) getDescriptors().toArray(new IArtifactDescriptor[getDescriptors().size()]);
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

	public void save() {
		repository.save();
	}

	public void setDescription(String description) {
		repository.setDescription(description);
	}

	public void setName(String value) {
		repository.setName(value);
	}

	public void setProvider(String provider) {
		repository.setProvider(provider);
	}

	public void setRules(String[][] rules) {
		repository.setRules(rules);
	}

	public void setSignatureVerification(boolean value) {
		repository.setSignatureVerification(value);
	}

	public void tagAsImplementation() {
		repository.tagAsImplementation();
	}

	public String toString() {
		return repository.toString();
	}
}
