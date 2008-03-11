package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class DropinsRepositoryListener extends RepositoryListener {

	private static final String DROPIN_ARTIFACT_REPOSITORIES = "dropin.artifactRepositories"; //$NON-NLS-1$
	private static final String DROPIN_METADATA_REPOSITORIES = "dropin.metadataRepositories"; //$NON-NLS-1$
	private static final String PIPE = "|"; //$NON-NLS-1$
	private BundleContext context;
	private List metadataRepositories = new ArrayList();
	private List artifactRepositories = new ArrayList();

	public DropinsRepositoryListener(BundleContext context, String repositoryName) {
		super(context, repositoryName);
		this.context = context;
	}

	public boolean isInterested(File file) {
		if (file.isDirectory())
			return true;

		String name = file.getName();
		return name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".link");
	}

	public boolean added(File file) {
		if (super.isInterested(file))
			return super.added(file);

		URL repositoryURL = createRepositoryURL(file);
		if (repositoryURL != null) {
			loadMetadataRepository(repositoryURL);
			loadArtifactRepository(repositoryURL);
		}
		return true;
	}

	public boolean changed(File file) {
		if (super.isInterested(file))
			return super.added(file);

		URL repositoryURL = createRepositoryURL(file);
		if (repositoryURL != null) {
			loadMetadataRepository(repositoryURL);
			loadArtifactRepository(repositoryURL);
		}
		return true;
	}

	private String getLinkPath(File file) {
		Properties links = new Properties();
		try {
			InputStream input = new BufferedInputStream(new FileInputStream(file));
			try {
				links.load(input);
			} finally {
				input.close();
			}
		} catch (IOException e) {
			// ignore
		}
		String path = links.getProperty("path");
		if (path == null) {
			// log
			return null;
		}

		// parse out link information
		if (path.startsWith("r ")) { //$NON-NLS-1$
			path = path.substring(2).trim();
		} else if (path.startsWith("rw ")) { //$NON-NLS-1$
			path = path.substring(3).trim();
		} else {
			path = path.trim();
		}
		return path;
	}

	private URL createRepositoryURL(File file) {
		try {
			if (file.getName().endsWith(".link")) {
				String path = getLinkPath(file);
				// todo log
				if (path == null)
					return null;
				file = new File(path);
				if (!file.isAbsolute())
					file = new File(file, path).getCanonicalFile();
			}

			URL repositoryURL = file.toURL();
			if (file.getName().endsWith(".zip") || file.getName().endsWith(".jar")) {
				repositoryURL = new URL("jar:" + repositoryURL.toString() + "!/");
			}
			return repositoryURL;
		} catch (IOException e) {
			// todo log			
		}
		return null;
	}

	public void loadMetadataRepository(URL repoURL) {
		try {
			metadataRepositories.add(Activator.loadMetadataRepository(repoURL));
		} catch (ProvisionException e) {
			//TODO: log
			// ignore
		}
	}

	public void loadArtifactRepository(URL repoURL) {
		try {
			artifactRepositories.add(Activator.loadArtifactRepository(repoURL));
		} catch (ProvisionException e) {
			//TODO: log
			// ignore
		}
	}

	public void stopPoll() {

		synchronizeDropinMetadataRepositories();
		synchronizeDropinArtifactRepositories();

		super.stopPoll();
	}

	private void synchronizeDropinMetadataRepositories() {
		List currentRepositories = new ArrayList();
		for (Iterator it = metadataRepositories.iterator(); it.hasNext();) {
			IMetadataRepository repository = (IMetadataRepository) it.next();
			String urlString = repository.getLocation().toExternalForm();
			currentRepositories.add(urlString);
		}
		List previousRepositories = getListRepositoryProperty(getMetadataRepository(), DROPIN_METADATA_REPOSITORIES);
		for (Iterator iterator = previousRepositories.iterator(); iterator.hasNext();) {
			String repository = (String) iterator.next();
			if (!currentRepositories.contains(repository))
				removeMetadataRepository(repository);
		}
		setListRepositoryProperty(getMetadataRepository(), DROPIN_METADATA_REPOSITORIES, currentRepositories);
	}

	private void removeMetadataRepository(String urlString) {
		ServiceReference reference = context.getServiceReference(IMetadataRepositoryManager.class.getName());
		IMetadataRepositoryManager manager = null;
		if (reference != null)
			manager = (IMetadataRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException("MetadataRepositoryManager not registered.");

		try {
			manager.removeRepository(new URL(urlString));
		} catch (MalformedURLException e) {
			// TODO: log
			// ignore
		} finally {
			context.ungetService(reference);
		}
	}

	private void synchronizeDropinArtifactRepositories() {
		List currentRepositories = new ArrayList();
		for (Iterator it = artifactRepositories.iterator(); it.hasNext();) {
			IArtifactRepository repository = (IArtifactRepository) it.next();
			String urlString = repository.getLocation().toExternalForm();
			currentRepositories.add(urlString);
		}
		List previousRepositories = getListRepositoryProperty(getArtifactRepository(), DROPIN_ARTIFACT_REPOSITORIES);
		for (Iterator iterator = previousRepositories.iterator(); iterator.hasNext();) {
			String repository = (String) iterator.next();
			if (!currentRepositories.contains(repository))
				removeArtifactRepository(repository);
		}
		setListRepositoryProperty(getArtifactRepository(), DROPIN_ARTIFACT_REPOSITORIES, currentRepositories);
	}

	public void removeArtifactRepository(String urlString) {
		ServiceReference reference = context.getServiceReference(IArtifactRepositoryManager.class.getName());
		IArtifactRepositoryManager manager = null;
		if (reference != null)
			manager = (IArtifactRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException("ArtifactRepositoryManager not registered.");

		try {
			manager.removeRepository(new URL(urlString));
		} catch (MalformedURLException e) {
			//TODO: log
			// ignore
		} finally {
			context.ungetService(reference);
		}
	}

	private List getListRepositoryProperty(IRepository repository, String key) {
		List listProperty = new ArrayList();
		String dropinRepositories = (String) repository.getProperties().get(key);
		if (dropinRepositories != null) {
			StringTokenizer tokenizer = new StringTokenizer(dropinRepositories, PIPE); //$NON-NLS-1$			
			while (tokenizer.hasMoreTokens()) {
				listProperty.add(tokenizer.nextToken());
			}
		}
		return listProperty;
	}

	private void setListRepositoryProperty(IRepository repository, String key, List listProperty) {
		StringBuffer buffer = new StringBuffer();
		for (Iterator it = listProperty.iterator(); it.hasNext();) {
			String repositoryString = (String) it.next();
			buffer.append(repositoryString);
			if (it.hasNext())
				buffer.append(PIPE);
		}
		String value = (buffer.length() == 0) ? null : buffer.toString();
		repository.setProperty(key, value);
	}

	public IMetadataRepository[] getMetadataRepositories() {
		List result = new ArrayList(metadataRepositories);
		result.add(getMetadataRepository());
		return (IMetadataRepository[]) result.toArray(new IMetadataRepository[0]);
	}

}
