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
package org.eclipse.equinox.internal.p2.artifact.mirror;

import java.net.URL;
import java.util.Map;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**
 * An application that performs mirroring of artifacts between repositories.
 */
public class MirrorApplication implements IApplication {

	private URL sourceLocation;
	private URL destinationLocation;
	private IArtifactRepository source;
	private IArtifactRepository destination;
	private boolean append = false;
	private boolean raw = false;
	private IArtifactRepositoryManager cachedManager;
	private boolean sourceLoaded = false;
	private boolean destinationLoaded = false;

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		Map args = context.getArguments();
		initializeFromArguments((String[]) args.get(IApplicationContext.APPLICATION_ARGS));
		setupRepositories();
		new Mirroring(source, destination, raw).run();
		//if the repository was not already loaded before the mirror application started, close it.
		if (!sourceLoaded)
			getManager().removeRepository(sourceLocation);
		if (!destinationLoaded)
			getManager().removeRepository(destinationLocation);

		return IApplication.EXIT_OK;
	}

	/*
	 * Return the artifact repository manager. We need to check the service here
	 * as well as creating one manually in case we are running a stand-alone application
	 * in which no one has registered a manager yet.
	 */
	private ArtifactRepositoryManager getManager() {
		if (cachedManager != null)
			//TODO remove cast when API is available
			return (ArtifactRepositoryManager) cachedManager;
		IArtifactRepositoryManager result = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		// service not available... create one and hang onto it
		if (result == null) {
			cachedManager = new ArtifactRepositoryManager();
			result = cachedManager;
		}
		//TODO remove cast when API is available
		return (ArtifactRepositoryManager) result;
	}

	private void setupRepositories() throws ProvisionException {
		if (destinationLocation == null || sourceLocation == null)
			throw new IllegalStateException("Must specify a source and destination"); //$NON-NLS-1$

		//Check if repositories are already loaded
		//TODO modify the contains statement once the API is available
		sourceLoaded = getManager().contains(sourceLocation);
		//TODO modify the contains statement once the API is available
		destinationLoaded = getManager().contains(destinationLocation);

		destination = initializeDestination();
		source = getManager().loadRepository(sourceLocation, null);
	}

	private IArtifactRepository initializeDestination() throws ProvisionException {
		IArtifactRepositoryManager manager = getManager();
		try {
			IArtifactRepository repository = manager.loadRepository(destinationLocation, null);
			if (!repository.isModifiable())
				throw new IllegalArgumentException("Artifact repository not modifiable: " + destinationLocation); //$NON-NLS-1$
			if (!append)
				repository.removeAll();
			return repository;
		} catch (ProvisionException e) {
			//fall through and create a new repository below
		}
		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a Simple repo by default.
		String repositoryName = destinationLocation + " - artifacts"; //$NON-NLS-1$
		return manager.createRepository(destinationLocation, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		// nothing to do
	}

	public void initializeFromArguments(String[] args) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			// check for args without parameters (i.e., a flag arg)
			if (args[i].equalsIgnoreCase("-raw")) //$NON-NLS-1$
				raw = true;
			if (args[i].equalsIgnoreCase("-append")) //$NON-NLS-1$
				append = true;

			// check for args with parameters. If we are at the last argument or 
			// if the next one has a '-' as the first character, then we can't have 
			// an arg with a param so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];

			if (args[i - 1].equalsIgnoreCase("-source")) //$NON-NLS-1$
				sourceLocation = new URL(arg);
			if (args[i - 1].equalsIgnoreCase("-destination")) //$NON-NLS-1$
				destinationLocation = new URL(arg);
		}
	}
}
