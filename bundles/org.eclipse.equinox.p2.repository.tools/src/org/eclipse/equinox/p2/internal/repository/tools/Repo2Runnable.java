/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.engine.phases.Collect;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;

/**
 * The transformer takes an existing p2 repository (local or remote), iterates over 
 * its list of IUs, and fetches all of the corresponding artifacts to a user-specified location. 
 * Once fetched, the artifacts will be in "runnable" form... that is directory-based bundles will be
 * extracted into folders and packed JAR files will be un-packed.
 * 
 * @since 1.0
 */
public class Repo2Runnable implements IApplication {

	private String destinationArtifactRepository; // where to publish the files
	private String destinationMetadataRepository; // where to copy the metadata to
	private List sourceArtifactRepositories = new ArrayList(); // where are the artifacts?

	// only one of these needs to be set. if it is the repo then we will process
	// the whole repo. otherwise we will process just the list of IUS.
	private List sourceMetadataRepositories = new ArrayList(); // where is the metadata?
	private List sourceIUs = new ArrayList(); // list of IUs to process

	// the list of IUs that we actually transformed... could have come from the repo 
	// or have been user-specified.
	private Collection processedIUs = new ArrayList();

	/*
	 * Perform the transformation.
	 */
	public void run(IProgressMonitor monitor) throws ProvisionException {
		SubMonitor progress = SubMonitor.convert(monitor, 4);
		// ensure all the right parameters are set
		validate();

		// figure out which IUs we need to process
		collectIUs(progress.newChild(1));

		// create the operands from the list of IUs
		InstallableUnitOperand[] operands = new InstallableUnitOperand[processedIUs.size()];
		int i = 0;
		for (Iterator iter = processedIUs.iterator(); iter.hasNext();)
			operands[i++] = new InstallableUnitOperand(null, (IInstallableUnit) iter.next());

		// ensure the artifact repo will be consulted by loading it
		IArtifactRepositoryManager artifactRepositoryManager = Activator.getArtifactRepositoryManager();
		for (Iterator iter = sourceArtifactRepositories.iterator(); iter.hasNext();) {
			artifactRepositoryManager.loadRepository((URI) iter.next(), progress.newChild(1));
		}

		// call the engine with only the "collect" phase so all we do is download
		IProfile profile = createProfile();
		ProvisioningContext context = new ProvisioningContext();
		PhaseSet phaseSet = new PhaseSet(new Phase[] {new Collect(100)}) {};
		Engine engine = (Engine) ServiceHelper.getService(Activator.getBundleContext(), Engine.SERVICE_NAME);
		if (engine == null)
			throw new ProvisionException("Unable to acquire engine service.");
		engine.perform(profile, phaseSet, operands, context, progress.newChild(1));

		// publish the metadata to a destination - if requested
		publishMetadata(progress.newChild(1));

		// cleanup by removing the temporary profile
		removeProfile(profile);
	}

	/*
	 * Figure out exactly which IUs we have to process.
	 */
	private void collectIUs(IProgressMonitor monitor) throws ProvisionException {
		// if the user told us exactly which IUs to process, then just set it and return.
		if (sourceIUs != null && !sourceIUs.isEmpty()) {
			processedIUs = sourceIUs;
			return;
		}
		// get all IUs from the repos
		if (sourceMetadataRepositories == null || sourceMetadataRepositories.isEmpty())
			throw new ProvisionException("Need to specify either a source metadata repository or a valid list of IUs.");
		for (Iterator iter = sourceMetadataRepositories.iterator(); iter.hasNext();) {
			processedIUs.addAll(getAllIUs((URI) iter.next(), monitor).toCollection());
		}
	}

	/*
	 * If there is a destination metadata repository set, then add all our transformed
	 * IUs to it. 
	 */
	private void publishMetadata(IProgressMonitor monitor) throws ProvisionException {
		// publishing the metadata is optional
		if (destinationMetadataRepository == null)
			return;
		URI location = new File(destinationMetadataRepository).toURI();
		IMetadataRepositoryManager manager = Activator.getMetadataRepositoryManager();
		IMetadataRepository repository;
		try {
			repository = manager.createRepository(location, location + " - metadata", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			repository = manager.loadRepository(location, monitor);
		}
		repository.addInstallableUnits((IInstallableUnit[]) processedIUs.toArray(new IInstallableUnit[processedIUs.size()]));
	}

	/*
	 * Return a collector over all the IUs contained in the given repository.
	 */
	private Collector getAllIUs(URI location, IProgressMonitor monitor) throws ProvisionException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		IMetadataRepositoryManager manager = Activator.getMetadataRepositoryManager();
		IMetadataRepository repository = manager.loadRepository(location, progress.newChild(1));
		Collector result = new Collector();
		repository.query(InstallableUnitQuery.ANY, result, progress.newChild(1)).iterator();
		return result;
	}

	/*
	 * Remove the given profile from the profile registry.
	 */
	private void removeProfile(IProfile profile) throws ProvisionException {
		IProfileRegistry registry = Activator.getProfileRegistry();
		registry.removeProfile(profile.getProfileId());
	}

	/*
	 * Create and return a new profile.
	 */
	private IProfile createProfile() throws ProvisionException {
		Map properties = new Properties();
		properties.put(IProfile.PROP_CACHE, destinationArtifactRepository);
		properties.put(IProfile.PROP_INSTALL_FOLDER, destinationArtifactRepository);
		IProfileRegistry registry = Activator.getProfileRegistry();
		return registry.addProfile(System.currentTimeMillis() + "-" + Math.random(), properties);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		processCommandLineArgs(args);
		// perform the transformation
		run(null);
		return IApplication.EXIT_OK;
	}

	/*
	 * Iterate over the command-line arguments and prepare the transformer for processing.
	 */
	private void processCommandLineArgs(String[] args) {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			String option = args[i];
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];

			if (option.equalsIgnoreCase("-source")) { //$NON-NLS-1$
				addSourceArtifactRepository(arg);
				addSourceMetadataRepository(arg);
			}

			if (option.equalsIgnoreCase("-destination")) { //$NON-NLS-1$
				setDestinationArtifactRepository(arg);
				setDestinationMetadataRepository(arg);
			}
		}
	}

	/*
	 * Ensure all mandatory parameters have been set. Throw an exception if there
	 * are any missing.
	 */
	private void validate() throws ProvisionException {
		if (sourceArtifactRepositories == null || sourceArtifactRepositories.isEmpty())
			throw new ProvisionException("Need to set the source artifact repository location.");
		if (sourceMetadataRepositories == null && sourceIUs == null)
			throw new ProvisionException("Need to set the source metadata repository location or set a list of IUs to process.");
		if (destinationArtifactRepository == null)
			throw new ProvisionException("Need to set the destination artifact repository location.");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		// nothing to do
	}

	/*
	 * Set the location of the metadata repository. 
	 */
	public void addSourceMetadataRepository(String location) {
		URI uri = Activator.getURI(location);
		if (uri != null)
			sourceMetadataRepositories.add(uri);
	}

	/*
	 * Add the given location as a metadata repository.
	 */
	public void addSourceMetadataRepository(URI location) {
		if (location != null)
			sourceMetadataRepositories.add(location);
	}

	/*
	 * Get the list of source metadata repositories for this transformer.
	 */
	public List getSourceMetadataRepositories() {
		return sourceMetadataRepositories;
	}

	/*
	 * Set the location of the artifact repository.
	 */
	public void addSourceArtifactRepository(String location) {
		URI uri = Activator.getURI(location);
		if (uri != null)
			sourceArtifactRepositories.add(uri);
	}

	/*
	 * Add the given location as an artifact repository.
	 */
	public void addSourceArtifactRepository(URI location) {
		if (location != null)
			sourceArtifactRepositories.add(location);
	}

	/*
	 * Set the destination location for the artifacts.
	 */
	public void setDestinationArtifactRepository(String location) {
		destinationArtifactRepository = new Path(location).toOSString();
	}

	/*
	 * Set the destination location for the metadata if the user wishes to
	 * copy/publish the metadata.
	 */
	public void setDestinationMetadataRepository(String location) {
		destinationMetadataRepository = new Path(location).toOSString();
	}

	/*
	 * Set the list of installable units that we should process. Should use only one
	 * of either this list or the source metadata repository.
	 */
	public void setSourceIUs(List ius) {
		sourceIUs = ius;
	}
}
