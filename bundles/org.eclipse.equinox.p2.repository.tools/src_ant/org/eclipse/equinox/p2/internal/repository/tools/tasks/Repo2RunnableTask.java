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
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.util.List;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.Repo2Runnable;

/**
 * Ant task which calls the "repo to runnable" application. This application takes an
 * existing p2 repository (local or remote), iterates over its list of IUs, and fetches 
 * all of the corresponding artifacts to a user-specified location. Once fetched, the
 * artifacts will be in "runnable" form... that is directory-based bundles will be
 * extracted into folders and packed JAR files will be un-packed.
 * 
 * @since 1.0
 */
public class Repo2RunnableTask extends AbstractRepositoryTask {

	/*
	 * Constructor for the class. Create a new instance of the application
	 * so we can populate it with attributes.
	 */
	public Repo2RunnableTask() {
		super();
		this.application = new Repo2Runnable();
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		try {
			prepareSourceRepos();
			application.initializeRepos(null);
			List ius = prepareIUs();
			if ((ius == null || ius.size() == 0) && !(application.hasArtifactSources() || application.hasMetadataSources()))
				throw new BuildException("Need to specify either a non-empty source metadata repository or a valid list of IUs.");
			application.setSourceIUs(ius);
			IStatus result = application.run(null);
			if (result.matches(IStatus.ERROR))
				throw new ProvisionException(result);
		} catch (ProvisionException e) {
			throw new BuildException("Error occurred while transforming repository.", e);
		}
	}
}
