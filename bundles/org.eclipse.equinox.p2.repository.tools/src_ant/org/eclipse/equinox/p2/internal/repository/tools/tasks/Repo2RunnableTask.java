/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc. - fragment creation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.internal.repository.tools.Repo2Runnable;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

/**
 * Ant task which calls the "repo to runnable" application. This application
 * takes an existing p2 repository (local or remote), iterates over its list of
 * IUs, and fetches all of the corresponding artifacts to a user-specified
 * location. Once fetched, the artifacts will be in "runnable" form... that is
 * directory-based bundles will be extracted into folders.
 *
 * @since 1.0
 */
public class Repo2RunnableTask extends AbstractRepositoryTask {

	private boolean failOnError = true;
	private boolean flagAsRunnable = false;
	private boolean createFragments = false;

	/*
	 * Constructor for the class. Create a new instance of the application
	 * so we can populate it with attributes.
	 */
	public Repo2RunnableTask() {
		super();
		this.application = new Repo2Runnable();
	}

	@Override
	public void execute() throws BuildException {
		try {
			prepareSourceRepos();
			application.initializeRepos(null);
			List<IInstallableUnit> ius = prepareIUs();
			if ((ius == null || ius.size() == 0) && !(application.hasArtifactSources() || application.hasMetadataSources()))
				throw new BuildException(Messages.exception_needIUsOrNonEmptyRepo);
			application.setSourceIUs(ius);
			((Repo2Runnable) application).setFlagAsRunnable(flagAsRunnable);
			((Repo2Runnable) application).setCreateFragments(createFragments);
			IStatus result = application.run(null);
			if (failOnError && result.matches(IStatus.ERROR))
				throw new ProvisionException(result);
		} catch (ProvisionException e) {
			if (failOnError)
				throw new BuildException(NLS.bind(Messages.Repo2RunnableTask_errorTransforming, null != e.getMessage() ? e.getMessage() : e.toString()), e);
			/* else */
			getProject().log(NLS.bind(Messages.Repo2RunnableTask_errorTransforming, null != e.getMessage() ? e.getMessage() : e.toString()), Project.MSG_WARN);
			getProject().log(e.getMessage(), Project.MSG_WARN);
		}
	}

	public void setFailOnError(boolean fail) {
		this.failOnError = fail;
	}

	public void setFlagAsRunnable(boolean runnable) {
		this.flagAsRunnable = runnable;
	}

	public void setCreateFragments(boolean fragments) {
		this.createFragments = fragments;
	}
}
