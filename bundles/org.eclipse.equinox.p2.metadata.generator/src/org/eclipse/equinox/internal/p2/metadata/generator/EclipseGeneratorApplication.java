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
package org.eclipse.equinox.internal.p2.metadata.generator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.generator.*;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.ServiceRegistration;

public class EclipseGeneratorApplication implements IApplication {

	// The mapping rules for in-place generation need to construct paths into the structure
	// of an eclipse installation; in the future the default artifact mapping declared in
	// SimpleArtifactRepository may change, for example, to not have a 'bundles' directory
	// instead of a 'plugins' directory, so a separate constant is defined and used here.
	static final private String[][] INPLACE_MAPPING_RULES = { {"(& (namespace=eclipse) (classifier=plugin) (format=packed)", "${repoUrl}/features/${id}_${version}.pack.gz"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=feature))", "${repoUrl}/features/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=plugin))", "${repoUrl}/plugins/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=native))", "${repoUrl}/native/${id}_${version}"}}; //$NON-NLS-1$//$NON-NLS-2$

	static final public String PUBLISH_PACK_FILES_AS_SIBLINGS = "publishPackFilesAsSiblings"; //$NON-NLS-1$

	private ArtifactRepositoryManager defaultArtifactManager;
	private ServiceRegistration registrationDefaultArtifactManager;
	private MetadataRepositoryManager defaultMetadataManager;
	private ServiceRegistration registrationDefaultMetadataManager;
	private ProvisioningEventBus bus;
	private ServiceRegistration registrationBus;
	private String metadataLocation;
	private String artifactLocation;
	private String operation;
	private String argument;
	private String features;
	private String bundles;
	private String base;
	//whether repository xml files should be compressed
	private String compress = "false"; //$NON-NLS-1$

	private File getExecutableName(String base, EclipseInstallGeneratorInfoProvider provider) {
		File location = provider.getExecutableLocation();
		if (location == null)
			return new File(base, EclipseInstallGeneratorInfoProvider.getDefaultExecutableName());
		if (location.isAbsolute())
			return location;
		return new File(base, location.getPath());
	}

	private void initialize(EclipseInstallGeneratorInfoProvider provider) throws ProvisionException {
		if ("-source".equalsIgnoreCase(operation)) //$NON-NLS-1$
			provider.initialize(new File(argument));
		else if ("-inplace".equalsIgnoreCase(operation)) { //$NON-NLS-1$
			provider.initialize(new File(argument));
			initializeForInplace(provider);
		} else if ("-config".equalsIgnoreCase(operation)) { //$NON-NLS-1$
			provider.initialize(new File(argument), new File(argument, "configuration"), getExecutableName(argument, provider), null, null); //$NON-NLS-1$
		} else if ("-updateSite".equalsIgnoreCase(operation)) { //$NON-NLS-1$
			provider.setAddDefaultIUs(false);
			provider.initialize(new File(argument), null, null, new File[] {new File(argument, "plugins")}, new File(argument, "features")); //$NON-NLS-1$ //$NON-NLS-2$
			initializeForInplace(provider);
		} else {
			if (base != null && bundles != null && features != null)
				provider.initialize(new File(base), null, null, new File[] {new File(bundles)}, new File(features));
		}
		initializeRepositories(provider);
	}

	private void initializeArtifactRepository(EclipseInstallGeneratorInfoProvider provider) throws ProvisionException {
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
			provider.setArtifactRepository(repository);
			if (provider.reuseExistingPack200Files())
				repository.setProperty(PUBLISH_PACK_FILES_AS_SIBLINGS, "true"); //$NON-NLS-1$
			if (!provider.append())
				repository.removeAll();
			return;
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}

		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a Simple repo by default.
		String repositoryName = artifactLocation + " - artifacts"; //$NON-NLS-1$
		IArtifactRepository result = manager.createRepository(location, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY);
		provider.setArtifactRepository(result);
		result.setProperty(IRepository.PROP_COMPRESSED, compress);
		if (provider.reuseExistingPack200Files())
			result.setProperty(PUBLISH_PACK_FILES_AS_SIBLINGS, "true"); //$NON-NLS-1$
	}

	public void initializeForInplace(EclipseInstallGeneratorInfoProvider provider) {
		File location = provider.getBaseLocation();
		if (location == null)
			location = provider.getBundleLocations()[0];
		try {
			metadataLocation = location.toURL().toExternalForm();
			artifactLocation = location.toURL().toExternalForm();
		} catch (MalformedURLException e) {
			// ought not happen...
		}
		provider.setPublishArtifactRepository(true);
		provider.setPublishArtifacts(false);
		provider.setMappingRules(INPLACE_MAPPING_RULES);
	}

	private void initializeMetadataRepository(EclipseInstallGeneratorInfoProvider provider) throws ProvisionException {
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
				provider.setMetadataRepository(repository);
				if (!provider.append())
					repository.removeAll();
				return;
			}
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}

		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a random repo by default.
		String repositoryName = metadataLocation + " - metadata"; //$NON-NLS-1$
		IMetadataRepository result = manager.createRepository(location, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
		if (result != null) {
			result.setProperty(IRepository.PROP_COMPRESSED, compress);
			provider.setMetadataRepository(result);
		}
	}

	private void initializeRepositories(EclipseInstallGeneratorInfoProvider provider) throws ProvisionException {
		initializeArtifactRepository(provider);
		initializeMetadataRepository(provider);
	}

	public void processCommandLineArguments(String[] args, EclipseInstallGeneratorInfoProvider provider) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			// check for args without parameters (i.e., a flag arg)

			if (args[i].equalsIgnoreCase("-publishArtifacts") || args[i].equalsIgnoreCase("-pa")) //$NON-NLS-1$ //$NON-NLS-2$
				provider.setPublishArtifacts(true);

			if (args[i].equalsIgnoreCase("-publishArtifactRepository") || args[i].equalsIgnoreCase("-par")) //$NON-NLS-1$ //$NON-NLS-2$
				provider.setPublishArtifactRepository(true);

			if (args[i].equalsIgnoreCase("-append")) //$NON-NLS-1$
				provider.setAppend(true);

			if (args[i].equalsIgnoreCase("-noDefaultIUs")) //$NON-NLS-1$
				provider.setAddDefaultIUs(false);

			if (args[i].equalsIgnoreCase("-compress")) //$NON-NLS-1$
				compress = "true"; //$NON-NLS-1$

			if (args[i].equalsIgnoreCase("-reusePack200Files")) //$NON-NLS-1$
				provider.reuseExistingPack200Files(true);

			// check for args with parameters. If we are at the last argument or if the next one
			// has a '-' as the first character, then we can't have an arg with a parm so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];

			if (args[i - 1].equalsIgnoreCase("-source")) { //$NON-NLS-1$
				operation = args[i - 1];
				argument = arg;
			}

			if (args[i - 1].equalsIgnoreCase("-inplace")) { //$NON-NLS-1$
				operation = args[i - 1];
				argument = arg;
			}

			if (args[i - 1].equalsIgnoreCase("-config")) { //$NON-NLS-1$
				operation = args[i - 1];
				argument = arg;
			}
			if (args[i - 1].equalsIgnoreCase("-updateSite")) { //$NON-NLS-1$
				operation = args[i - 1];
				argument = arg;
			}

			if (args[i - 1].equalsIgnoreCase("-exe")) //$NON-NLS-1$
				provider.setExecutableLocation(arg);

			if (args[i - 1].equalsIgnoreCase("-launcherConfig")) //$NON-NLS-1$
				provider.setLauncherConfig(arg);

			if (args[i - 1].equalsIgnoreCase("-metadataRepository") || args[i - 1].equalsIgnoreCase("-mr")) //$NON-NLS-1$ //$NON-NLS-2$
				metadataLocation = arg;

			if (args[i - 1].equalsIgnoreCase("-artifactRepository") | args[i - 1].equalsIgnoreCase("-ar")) //$NON-NLS-1$ //$NON-NLS-2$
				artifactLocation = arg;

			if (args[i - 1].equalsIgnoreCase("-flavor")) //$NON-NLS-1$
				provider.setFlavor(arg);

			if (args[i - 1].equalsIgnoreCase("-features")) //$NON-NLS-1$
				features = arg;

			if (args[i - 1].equalsIgnoreCase("-bundles")) //$NON-NLS-1$
				bundles = arg;

			if (args[i - 1].equalsIgnoreCase("-base")) //$NON-NLS-1$
				base = arg;

			if (args[i - 1].equalsIgnoreCase("-root")) //$NON-NLS-1$
				provider.setRootId(arg);

			if (args[i - 1].equalsIgnoreCase("-rootVersion")) //$NON-NLS-1$
				provider.setRootVersion(arg);

			if (args[i - 1].equalsIgnoreCase("-p2.os")) //$NON-NLS-1$
				provider.setOS(arg);

			if (args[i - 1].equalsIgnoreCase("-site")) //$NON-NLS-1$
				provider.setSiteLocation(new URL(arg));
		}
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
		if (ServiceHelper.getService(Activator.getContext(), ProvisioningEventBus.class.getName()) == null) {
			bus = new ProvisioningEventBus();
			registrationBus = Activator.getContext().registerService(ProvisioningEventBus.class.getName(), bus, null);
		}
	}

	public Object run(String args[]) throws Exception {
		registerEventBus();
		registerDefaultMetadataRepoManager();
		registerDefaultArtifactRepoManager();
		EclipseInstallGeneratorInfoProvider provider = new EclipseInstallGeneratorInfoProvider();
		processCommandLineArguments(args, provider);
		initialize(provider);

		if (provider.getBaseLocation() == null) {
			System.out.println(Messages.exception_baseLocationNotSpecified);
			for (int i = 0; i < args.length; i++)
				System.out.println(args[i]);
			return IApplication.EXIT_OK;
		}
		System.out.println(NLS.bind(Messages.message_generatingMetadata, provider.getBaseLocation()));

		long before = System.currentTimeMillis();
		IStatus result = new Generator(provider).generate();
		long after = System.currentTimeMillis();
		if (result.isOK()) {
			System.out.println(NLS.bind(Messages.message_generationCompleted, String.valueOf((after - before) / 1000)));
			return IApplication.EXIT_OK;
		}
		System.out.println(result);
		return new Integer(1);
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

}
