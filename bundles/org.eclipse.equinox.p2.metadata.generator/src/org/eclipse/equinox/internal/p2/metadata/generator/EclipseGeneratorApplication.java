/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.metadata.generator.EclipseInstallGeneratorInfoProvider;
import org.eclipse.equinox.p2.metadata.generator.Generator;
import org.eclipse.equinox.p2.metadata.repository.*;
import org.osgi.framework.ServiceRegistration;

public class EclipseGeneratorApplication implements IApplication {

	// The mapping rules for in-place generation need to construct paths into the structure
	// of an eclipse installation; in the future the default artifact mapping declared in
	// SimpleArtifactRepository may change, for example, to not have a 'bundles' directory
	// instead of a 'plugins' directory, so a separate constant is defined and used here.
	static final private String[][] INPLACE_MAPPING_RULES = { {"(& (namespace=eclipse) (classifier=feature))", "${repoUrl}/features/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=plugin))", "${repoUrl}/plugins/${id}_${version}.jar"}, //$NON-NLS-1$//$NON-NLS-2$
			{"(& (namespace=eclipse) (classifier=native))", "${repoUrl}/native/${id}_${version}"}}; //$NON-NLS-1$//$NON-NLS-2$

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

	private void registerDefaultMetadataRepoManager() {
		if (ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName()) == null) {
			defaultMetadataManager = new MetadataRepositoryManager();
			registrationDefaultMetadataManager = Activator.getContext().registerService(IMetadataRepositoryManager.class.getName(), defaultMetadataManager, null);
		}
	}

	private void registerDefaultArtifactRepoManager() {
		if (ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName()) == null) {
			defaultArtifactManager = new ArtifactRepositoryManager();
			registrationDefaultArtifactManager = Activator.getContext().registerService(IArtifactRepositoryManager.class.getName(), defaultArtifactManager, null);
		}
	}

	private void registerEventBus() {
		if (ServiceHelper.getService(Activator.getContext(), ProvisioningEventBus.class.getName()) == null) {
			bus = new ProvisioningEventBus();
			registrationBus = Activator.getContext().registerService(ProvisioningEventBus.class.getName(), bus, null);
		}
	}

	public Object start(IApplicationContext context) throws Exception {
		registerEventBus();
		registerDefaultMetadataRepoManager();
		registerDefaultArtifactRepoManager();
		EclipseInstallGeneratorInfoProvider provider = new EclipseInstallGeneratorInfoProvider();
		Map args = context.getArguments();
		processCommandLineArguments((String[]) args.get("application.args"), provider);
		initialize(provider);

		if (provider.getBaseLocation() == null) {
			System.out.println("Eclipse base location not specified");
			String[] a = (String[]) args.get("application.args");
			for (int i = 0; i < a.length; i++)
				System.out.println(a[i]);
			return IApplication.EXIT_OK;
		}
		System.out.println("Generating metadata for " + provider.getBaseLocation());

		IStatus result = new Generator(provider).generate();

		if (result.isOK()) {
			System.out.println("Generation completed with success");
			return IApplication.EXIT_OK;
		}
		System.out.println(result);
		return new Integer(1);
	}

	private void initialize(EclipseInstallGeneratorInfoProvider provider) {
		if ("-source".equalsIgnoreCase(operation))
			provider.initialize(new File(argument));
		else if ("-inplace".equalsIgnoreCase(operation)) {
			provider.initialize(new File(argument));
			initializeForInplace(provider);
		} else if ("-config".equalsIgnoreCase(operation)) {
			provider.initialize(new File(argument), new File(argument, "configuration"), getExecutableName(argument, provider), null, null);
		} else if ("-updateSite".equalsIgnoreCase(operation)) {
			provider.setAddDefaultIUs(false);
			provider.initialize(new File(argument), null, null, new File[] {new File(argument, "plugins")}, new File(argument, "features"));
			initializeForInplace(provider);
		} else {
			if (base != null && bundles != null && features != null)
				provider.initialize(new File(base), null, null, new File[] {new File(bundles)}, new File(features));
		}
		initializeRepositories(provider);
	}

	private File getExecutableName(String base, EclipseInstallGeneratorInfoProvider provider) {
		File location = provider.getExecutableLocation();
		if (location == null)
			return new File(base, EclipseInstallGeneratorInfoProvider.getDefaultExecutableName());
		if (location.isAbsolute())
			return location;
		return new File(base, location.getPath());
	}

	private void initializeRepositories(EclipseInstallGeneratorInfoProvider provider) {
		initializeArtifactRepository(provider);
		initializeMetadataRepository(provider);
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

	public void processCommandLineArguments(String[] args, EclipseInstallGeneratorInfoProvider provider) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			// check for args without parameters (i.e., a flag arg)

			if (args[i].equalsIgnoreCase("-publishArtifacts") || args[i].equalsIgnoreCase("-pa"))
				provider.setPublishArtifacts(true);

			if (args[i].equalsIgnoreCase("-publishArtifactRepository") || args[i].equalsIgnoreCase("-par"))
				provider.setPublishArtifactRepository(true);

			if (args[i].equalsIgnoreCase("-append"))
				provider.setAppend(true);

			if (args[i].equalsIgnoreCase("-noDefaultIUs"))
				provider.setAddDefaultIUs(false);

			// check for args with parameters. If we are at the last argument or if the next one
			// has a '-' as the first character, then we can't have an arg with a parm so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];

			if (args[i - 1].equalsIgnoreCase("-source")) {
				operation = args[i - 1];
				argument = arg;
			}

			if (args[i - 1].equalsIgnoreCase("-inplace")) {
				operation = args[i - 1];
				argument = arg;
			}

			if (args[i - 1].equalsIgnoreCase("-config")) {
				operation = args[i - 1];
				argument = arg;
			}
			if (args[i - 1].equalsIgnoreCase("-updateSite")) {
				operation = args[i - 1];
				argument = arg;
			}

			if (args[i - 1].equalsIgnoreCase("-exe"))
				provider.setExecutableLocation(arg);

			if (args[i - 1].equalsIgnoreCase("-metadataRepository") || args[i - 1].equalsIgnoreCase("-mr"))
				metadataLocation = arg;

			if (args[i - 1].equalsIgnoreCase("-artifactRepository") | args[i - 1].equalsIgnoreCase("-ar"))
				artifactLocation = arg;

			if (args[i - 1].equalsIgnoreCase("-flavor"))
				provider.setFlavor(arg);

			if (args[i - 1].equalsIgnoreCase("-features"))
				features = arg;

			if (args[i - 1].equalsIgnoreCase("-bundles"))
				bundles = arg;

			if (args[i - 1].equalsIgnoreCase("-base"))
				base = arg;

			if (args[i - 1].equalsIgnoreCase("-root"))
				provider.setRootId(arg);

			if (args[i - 1].equalsIgnoreCase("-rootVersion"))
				provider.setRootVersion(arg);

			if (args[i - 1].equalsIgnoreCase("-prov.os"))
				provider.setOS(arg);
		}
	}

	private void initializeArtifactRepository(EclipseInstallGeneratorInfoProvider provider) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.context, IArtifactRepositoryManager.class.getName());
		URL location;
		try {
			location = new URL(artifactLocation);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Artifact repository location not a valid URL:" + artifactLocation); //$NON-NLS-1$
		}
		IArtifactRepository repository = manager.loadRepository(location, null);
		if (repository != null) {
			IWritableArtifactRepository result = (IWritableArtifactRepository) repository.getAdapter(IWritableArtifactRepository.class);
			if (result == null)
				throw new IllegalArgumentException("Artifact repository not writeable: " + location); //$NON-NLS-1$
			provider.setArtifactRepository(result);
			if (!provider.append())
				result.removeAll();
			return;
		}

		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a Simple repo by default.
		String repositoryName = artifactLocation + " - artifacts"; //$NON-NLS-1$
		IWritableArtifactRepository result = (IWritableArtifactRepository) manager.createRepository(location, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository"); //$NON-NLS-1$
		if (result != null)
			provider.setArtifactRepository(result);
	}

	private void initializeMetadataRepository(EclipseInstallGeneratorInfoProvider provider) {
		URL location;
		try {
			location = new URL(metadataLocation);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Metadata repository location not a valid URL:" + artifactLocation); //$NON-NLS-1$
		}
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.context, IMetadataRepositoryManager.class.getName());
		IMetadataRepository repository = manager.loadRepository(location, null);
		if (repository != null) {
			IWritableMetadataRepository result = (IWritableMetadataRepository) repository.getAdapter(IWritableMetadataRepository.class);
			if (result == null)
				throw new IllegalArgumentException("Metadata repository not writeable: " + location); //$NON-NLS-1$
			provider.setMetadataRepository(result);
			if (!provider.append())
				result.removeAll();
			return;
		}

		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a random repo by default.
		String repositoryName = metadataLocation + " - metadata"; //$NON-NLS-1$
		IWritableMetadataRepository result = (IWritableMetadataRepository) manager.createRepository(location, repositoryName, "org.eclipse.equinox.p2.metadata.repository.simpleRepository"); //$NON-NLS-1$
		if (result != null)
			provider.setMetadataRepository(result);
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
