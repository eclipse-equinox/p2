/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
import org.eclipse.equinox.internal.p2.core.ProvisioningEventBus;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.ServiceRegistration;

public abstract class AbstractGeneratorApplication implements IApplication {

	// The mapping rules for in-place generation need to construct paths into the structure
	// of an eclipse installation; in the future the default artifact mapping declared in
	// SimpleArtifactRepository may change, for example, to not have a 'bundles' directory
	// instead of a 'plugins' directory, so a separate constant is defined and used here.
	static final protected String[][] INPLACE_MAPPING_RULES = { {"(& (classifier=osgi.bundle) (format=packed)", "${repoUrl}/features/${id}_${version}.jar.pack.gz"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (classifier=osgi.bundle))", "${repoUrl}/plugins/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (classifier=binary))", "${repoUrl}/binary/${id}_${version}"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (classifier=org.eclipse.update.feature))", "${repoUrl}/features/${id}_${version}.jar"}}; //$NON-NLS-1$//$NON-NLS-2$

	static final public String PUBLISH_PACK_FILES_AS_SIBLINGS = "publishPackFilesAsSiblings"; //$NON-NLS-1$

	private ArtifactRepositoryManager defaultArtifactManager;
	private ServiceRegistration registrationDefaultArtifactManager;
	private MetadataRepositoryManager defaultMetadataManager;
	private ServiceRegistration registrationDefaultMetadataManager;
	private IProvisioningEventBus bus;
	private ServiceRegistration registrationBus;
	protected PublisherInfo info;
	protected String source;
	protected String metadataLocation;
	protected String metadataRepoName;
	protected String artifactLocation;
	protected String artifactRepoName;
	//whether repository xml files should be compressed
	protected String compress = "false"; //$NON-NLS-1$
	protected boolean inplace = false;
	protected boolean append = false;
	protected boolean reusePackedFiles = false;

	protected void initialize(PublisherInfo info) throws ProvisionException {
		initializeRepositories(info);
	}

	protected void initializeArtifactRepository(PublisherInfo info) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.context, IArtifactRepositoryManager.class.getName());
		URL location;
		try {
			location = new URL(artifactLocation);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(NLS.bind(Messages.exception_artifactRepoLocationURL, artifactLocation));
		}
		try {
			IArtifactRepository repository = manager.loadRepository(location, null);
			if (!repository.isModifiable())
				throw new IllegalArgumentException(NLS.bind(Messages.exception_artifactRepoNotWritable, location));
			info.setArtifactRepository(repository);
			if (reusePackedFiles)
				repository.setProperty(PUBLISH_PACK_FILES_AS_SIBLINGS, "true"); //$NON-NLS-1$
			if (!append)
				repository.removeAll();
			return;
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}

		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a Simple repo by default.
		String repositoryName = artifactRepoName != null ? artifactRepoName : artifactLocation + " - artifacts"; //$NON-NLS-1$
		IArtifactRepository result = manager.createRepository(location, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY);
		manager.addRepository(result.getLocation());
		if (inplace) {
			// TODO there must be something we have to do to set up the mapping rules here...
		}
		result.setProperty(IRepository.PROP_COMPRESSED, compress);
		if (reusePackedFiles)
			result.setProperty(PUBLISH_PACK_FILES_AS_SIBLINGS, "true"); //$NON-NLS-1$
		if (artifactRepoName != null)
			result.setName(artifactRepoName);
		info.setArtifactRepository(result);
	}

	protected void initializeForInplace(PublisherInfo info) {
		File location = new File(source);
		try {
			if (metadataLocation == null)
				metadataLocation = location.toURL().toExternalForm();
			if (artifactLocation == null)
				artifactLocation = location.toURL().toExternalForm();
		} catch (MalformedURLException e) {
			// ought not happen...
		}
		info.setPublishArtifactRepository(true);
		info.setPublishArtifacts(false);
	}

	protected void initializeMetadataRepository(PublisherInfo info) throws ProvisionException {
		URL location;
		try {
			location = new URL(metadataLocation);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(NLS.bind(Messages.exception_metadataRepoLocationURL, artifactLocation));
		}
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.context, IMetadataRepositoryManager.class.getName());
		try {
			IMetadataRepository repository = manager.loadRepository(location, null);
			if (repository != null) {
				repository.setProperty(IRepository.PROP_COMPRESSED, compress);
				if (!repository.isModifiable())
					throw new IllegalArgumentException(NLS.bind(Messages.exception_metadataRepoNotWritable, location));
				info.setMetadataRepository(repository);
				if (!append)
					repository.removeAll();
				return;
			}
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}

		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a random repo by default.
		String repositoryName = metadataRepoName == null ? metadataLocation + " - metadata" : metadataRepoName; //$NON-NLS-1$
		IMetadataRepository result = manager.createRepository(location, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
		manager.addRepository(result.getLocation());
		if (result != null) {
			result.setProperty(IRepository.PROP_COMPRESSED, compress);
			if (metadataRepoName != null)
				result.setName(metadataRepoName);
			info.setMetadataRepository(result);
		}
	}

	protected void initializeRepositories(PublisherInfo info) throws ProvisionException {
		initializeArtifactRepository(info);
		initializeMetadataRepository(info);
	}

	protected void processCommandLineArguments(String[] args, PublisherInfo info) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			// check for args without parameters (i.e., a flag arg)
			processFlag(args[i], info);

			// check for args with parameters. If we are at the last argument or if the next one
			// has a '-' as the first character, then we can't have an arg with a parm so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			processParameter(args[i], args[++i], info);
		}
	}

	protected void processParameter(String arg, String parameter, PublisherInfo info) {
		if (arg.equalsIgnoreCase("-metadataRepository") || arg.equalsIgnoreCase("-mr")) //$NON-NLS-1$ //$NON-NLS-2$
			metadataLocation = parameter;

		if (arg.equalsIgnoreCase("-metadataRepositoryName")) //$NON-NLS-1$
			metadataRepoName = parameter;

		if (arg.equalsIgnoreCase("-source")) //$NON-NLS-1$
			source = parameter;

		if (arg.equalsIgnoreCase("-artifactRepository") | arg.equalsIgnoreCase("-ar")) //$NON-NLS-1$ //$NON-NLS-2$
			artifactLocation = parameter;

		if (arg.equalsIgnoreCase("-artifactRepositoryName")) //$NON-NLS-1$
			artifactRepoName = parameter;
	}

	protected void processFlag(String arg, PublisherInfo info) {
		if (arg.equalsIgnoreCase("-publishArtifacts") || arg.equalsIgnoreCase("-pa")) //$NON-NLS-1$ //$NON-NLS-2$
			info.setPublishArtifacts(true);

		if (arg.equalsIgnoreCase("-publishArtifactRepository") || arg.equalsIgnoreCase("-par")) //$NON-NLS-1$ //$NON-NLS-2$
			info.setPublishArtifactRepository(true);

		if (arg.equalsIgnoreCase("-append")) //$NON-NLS-1$
			append = true;

		if (arg.equalsIgnoreCase("-compress")) //$NON-NLS-1$
			compress = "true"; //$NON-NLS-1$

		if (arg.equalsIgnoreCase("-reusePack200Files")) //$NON-NLS-1$
			reusePackedFiles = true;

		if (arg.equalsIgnoreCase("-inplace")) //$NON-NLS-1$
			inplace = true;
	}

	private void registerDefaultArtifactRepoManager() {
		if (ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName()) == null) {
			defaultArtifactManager = new ArtifactRepositoryManager();
			registrationDefaultArtifactManager = Activator.getContext().registerService(IArtifactRepositoryManager.class.getName(), defaultArtifactManager, null);
		}
	}

	private void registerDefaultMetadataRepoManager() {
		if (ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName()) == null) {
			defaultMetadataManager = new MetadataRepositoryManager();
			registrationDefaultMetadataManager = Activator.getContext().registerService(IMetadataRepositoryManager.class.getName(), defaultMetadataManager, null);
		}
	}

	private void registerEventBus() {
		if (ServiceHelper.getService(Activator.getContext(), IProvisioningEventBus.SERVICE_NAME) == null) {
			bus = new ProvisioningEventBus();
			registrationBus = Activator.getContext().registerService(IProvisioningEventBus.SERVICE_NAME, bus, null);
		}
	}

	public Object run(String args[]) throws Exception {
		info = createPublisherInfo();
		processCommandLineArguments(args, info);
		Object result = run(info);
		if (result != IApplication.EXIT_OK)
			for (int i = 0; i < args.length; i++)
				System.out.println(args[i]);
		return result;
	}

	protected PublisherInfo createPublisherInfo() {
		return new PublisherInfo();
	}

	public Object run(PublisherInfo info) throws Exception {
		registerEventBus();
		registerDefaultMetadataRepoManager();
		registerDefaultArtifactRepoManager();
		initialize(info);
		validateInfo(info);
		System.out.println(NLS.bind(Messages.message_generatingMetadata, info.getSummary()));

		long before = System.currentTimeMillis();
		IPublishingAction[] actions = createActions();
		Publisher publisher = createPublisher(info);
		IStatus result = publisher.publish(actions);
		long after = System.currentTimeMillis();

		if (result.isOK()) {
			System.out.println(NLS.bind(Messages.message_generationCompleted, String.valueOf((after - before) / 1000)));
			return IApplication.EXIT_OK;
		}
		System.out.println(result);
		return new Integer(1);
	}

	protected abstract IPublishingAction[] createActions();

	protected Publisher createPublisher(PublisherInfo info) {
		return new Publisher(info);
	}

	protected void validateInfo(PublisherInfo info) {
		//		if (info.getBaseLocation() == null && info.getProduct() == null) {
		//			System.out.println(Messages.exception_baseLocationNotSpecified);
		//			return new Integer(-1);
		//		}
	}

	public Object start(IApplicationContext context) throws Exception {
		return run((String[]) context.getArguments().get("application.args")); //$NON-NLS-1$
	}

	public void stop() {
		if (registrationDefaultMetadataManager != null) {
			registrationDefaultMetadataManager.unregister();
			registrationDefaultMetadataManager = null;
		}
		if (registrationDefaultArtifactManager != null) {
			registrationDefaultArtifactManager.unregister();
			registrationDefaultArtifactManager = null;
		}
		if (registrationBus != null) {
			registrationBus.unregister();
			registrationBus = null;
		}
	}

	public void setArtifactLocation(String location) {
		this.artifactLocation = location;
	}

	public void setMetadataLocation(String location) {
		this.metadataLocation = location;
	}

	public boolean reuseExistingPack200Files() {
		return reusePackedFiles;
	}

	public void setReuseExistingPackedFiles(boolean value) {
		reusePackedFiles = value;
	}

}
