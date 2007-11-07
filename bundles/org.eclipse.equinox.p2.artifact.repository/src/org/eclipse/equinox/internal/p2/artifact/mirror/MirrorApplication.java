/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.mirror;

import java.net.URL;
import java.util.Map;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepositoryManager;

public class MirrorApplication implements IApplication {

	private URL sourceLocation;
	private URL destinationLocation;
	private IArtifactRepository source;
	private IArtifactRepository destination;
	private boolean append;
	private boolean raw = false;

	public Object start(IApplicationContext context) throws Exception {
		Map args = context.getArguments();
		initializeFromArguments((String[]) args.get("application.args"));
		setupRepositories();
		new Mirror(source, destination, raw).run();
		return null;
	}

	private void setupRepositories() {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			// TODO log here
			return;

		source = manager.loadRepository(sourceLocation, null);
		if (destinationLocation != null)
			destination = initializeDestination();
		else
			destination = source;
	}

	private IArtifactRepository initializeDestination() {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		IArtifactRepository repository = manager.loadRepository(destinationLocation, null);
		if (repository != null) {
			if (!repository.isModifiable())
				throw new IllegalArgumentException("Artifact repository not modifiable: " + destinationLocation); //$NON-NLS-1$
			if (!append)
				repository.removeAll();
			return repository;
		}

		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a Simple repo by default.
		String repositoryName = destinationLocation + " - artifacts"; //$NON-NLS-1$
		return manager.createRepository(destinationLocation, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository"); //$NON-NLS-1$
	}

	public void stop() {
	}

	public void initializeFromArguments(String[] args) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			// check for args without parameters (i.e., a flag arg)
			if (args[i].equals("-raw"))
				raw = true;

			// check for args with parameters. If we are at the last argument or 
			// if the next one has a '-' as the first character, then we can't have 
			// an arg with a param so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];

			if (args[i - 1].equalsIgnoreCase("-source"))
				sourceLocation = new URL(arg);
			if (args[i - 1].equalsIgnoreCase("-destination"))
				destinationLocation = new URL(arg);
		}
	}
}
