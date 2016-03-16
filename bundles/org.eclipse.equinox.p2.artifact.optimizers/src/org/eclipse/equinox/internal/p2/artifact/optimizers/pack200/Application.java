/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * 	IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers.pack200;

import java.net.URI;
import java.util.Map;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.artifact.optimizers.OptimizerApplication;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;

public class Application extends OptimizerApplication {
	//Application return code
	private static final Integer NON_WRITTABLE_REPOSITORY = -1;

	//Application arguments
	private static final String ARTIFACT_REPOSITORY_ARG = "-artifactRepository"; //$NON-NLS-1$
	private static final String ARTIFACT_REPOSITORY_SHORT_ARG = "-ar"; //$NON-NLS-1$

	private URI artifactRepositoryLocation;

	public Object start(IApplicationContext context) throws Exception {
		Map<?, ?> args = context.getArguments();
		initializeFromArguments((String[]) args.get("application.args")); //$NON-NLS-1$
		IArtifactRepository repository = setupRepository(artifactRepositoryLocation);
		if (!repository.isModifiable())
			return NON_WRITTABLE_REPOSITORY;
		new Optimizer(repository).run();
		return null;
	}

	private IArtifactRepository setupRepository(URI location) throws ProvisionException {
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		if (manager == null)
			// TODO log here
			return null;
		return manager.loadRepository(location, null);
	}

	public void initializeFromArguments(String[] args) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			// check for args with parameters. If we are at the last argument or 
			// if the next one has a '-' as the first character, then we can't have 
			// an arg with a param so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];

			if (args[i - 1].equalsIgnoreCase(ARTIFACT_REPOSITORY_ARG) || args[i - 1].equalsIgnoreCase(ARTIFACT_REPOSITORY_SHORT_ARG))
				artifactRepositoryLocation = new URI(arg);
		}
	}
}
