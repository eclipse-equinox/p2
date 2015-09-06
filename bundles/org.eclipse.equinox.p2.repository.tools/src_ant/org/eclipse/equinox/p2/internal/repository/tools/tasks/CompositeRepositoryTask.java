/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.CompositeRepositoryApplication;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;

public class CompositeRepositoryTask extends AbstractRepositoryTask {
	private static String COMPOSITE_REMOVE = "p2.composite.artifact.repository.remove"; //$NON-NLS-1$
	private static String COMPOSITE_ADD = "p2.composite.artifact.repository.add"; //$NON-NLS-1$

	public CompositeRepositoryTask() {
		application = new CompositeRepositoryApplication();
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		try {
			IStatus result = application.run(null);
			if (result.matches(IStatus.ERROR)) {
				throw new BuildException(TaskHelper.statusToString(result, IStatus.ERROR, null).toString());
			}
		} catch (ProvisionException e) {
			throw new BuildException(e);
		}
	}

	/*
	 * Add the listed repositories to the composite repository
	 */
	public void addConfiguredAdd(RepositoryList list) {
		if (list.getRepoLocation() != null) {
			RepositoryDescriptor descriptor = new RepositoryDescriptor();
			//don't use RepositoryList#getRepoLocationURI() because we want relative URIs if they were specified
			try {
				descriptor.setLocation(URIUtil.fromString(list.getRepoLocation()));
				descriptor.setOptional(list.isOptional());
				if (!list.isBoth()) {
					descriptor.setKind(list.isArtifact() ? RepositoryDescriptor.KIND_ARTIFACT : RepositoryDescriptor.KIND_METADATA);
				}
				((CompositeRepositoryApplication) application).addChild(descriptor);
			} catch (URISyntaxException e) {
				//no good
			}
		}

		for (DestinationRepository repo : list.getRepositoryList()) {
			((CompositeRepositoryApplication) application).addChild(repo.getDescriptor());
		}
	}

	/*	
	 * Remove the listed repositories from the composite repository
	 */
	public void addConfiguredRemove(RepositoryList list) {
		if (list.getRepoLocation() != null) {
			RepositoryDescriptor descriptor = new RepositoryDescriptor();
			try {
				//don't use RepositoryList#getRepoLocationURI() because we want relative URIs if they were specified
				descriptor.setLocation(URIUtil.fromString(list.getRepoLocation()));
				descriptor.setOptional(list.isOptional());
				if (!list.isBoth()) {
					descriptor.setKind(list.isArtifact() ? RepositoryDescriptor.KIND_ARTIFACT : RepositoryDescriptor.KIND_METADATA);
				}
				((CompositeRepositoryApplication) application).removeChild(descriptor);
			} catch (URISyntaxException e) {
				// no good, don't remove
			}
		}
		for (DestinationRepository repo : list.getRepositoryList()) {
			((CompositeRepositoryApplication) application).removeChild(repo.getDescriptor());
		}
	}

	/*
	 * Set whether the task should fail if the repository already exists
	 */
	public void setFailOnExists(boolean value) {
		((CompositeRepositoryApplication) application).setFailOnExists(value);
	}

	public void setValidate(String value) {
		((CompositeRepositoryApplication) application).setComparator(value);
	}

	/*  p2.composite.artifact.repository.add
	 *  p2.composite.artifact.repository.remove*/
	public void setLocation(String value) {
		super.setDestination(value);
	}

	/*  p2.composite.artifact.repository.add
	 *  p2.composite.artifact.repository.remove*/
	public void setChild(String value) throws URISyntaxException {
		URI childURI = URIUtil.fromString(value);
		RepositoryDescriptor repo = new RepositoryDescriptor();
		repo.setLocation(childURI);

		if (getTaskName().equals(COMPOSITE_ADD))
			((CompositeRepositoryApplication) application).addChild(repo);
		else if (getTaskName().equals(COMPOSITE_REMOVE))
			((CompositeRepositoryApplication) application).removeChild(repo);
	}

	/*  p2.composite.artifact.repository.add */
	public void setComparatorID(String value) {
		if (value != null && !value.startsWith(ANT_PREFIX))
			((CompositeRepositoryApplication) application).setComparator(value);
	}

	/*  p2.composite.artifact.repository.remove */
	public void setAllChildren(String value) {
		if (value != null && !value.startsWith(ANT_PREFIX))
			((CompositeRepositoryApplication) application).setRemoveAll(Boolean.parseBoolean(value));
	}
}
