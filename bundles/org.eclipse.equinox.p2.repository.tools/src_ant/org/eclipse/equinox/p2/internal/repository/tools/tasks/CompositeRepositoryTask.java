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
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.CompositeRepositoryApplication;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;

public class CompositeRepositoryTask extends AbstractRepositoryTask<CompositeRepositoryApplication> {
	private static final String COMPOSITE_REMOVE = "p2.composite.artifact.repository.remove"; //$NON-NLS-1$
	private static final String COMPOSITE_ADD = "p2.composite.artifact.repository.add"; //$NON-NLS-1$

	public CompositeRepositoryTask() {
		super(new CompositeRepositoryApplication());
	}

	@Override
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
		processChildElement(list, application::addChild);
	}

	/*
	 * Remove the listed repositories from the composite repository
	 */
	public void addConfiguredRemove(RepositoryList list) {
		processChildElement(list, application::removeChild);
	}

	private void processChildElement(RepositoryList list, Consumer<RepositoryDescriptor> processor) {
		if (list.getRepoLocation() != null) {
			RepositoryDescriptor descriptor = new RepositoryDescriptor();
			try {
				//don't use RepositoryList#getRepoLocationURI() because we want relative URIs if they were specified
				descriptor.setLocation(URIUtil.fromString(list.getRepoLocation()));
				descriptor.setOptional(list.isOptional());
				if (!list.isBoth()) {
					descriptor.setKind(list.isArtifact() ? RepositoryDescriptor.KIND_ARTIFACT : RepositoryDescriptor.KIND_METADATA);
				}
				processor.accept(descriptor);
			} catch (URISyntaxException e) {
				// no good, don't remove
			}
		}
		for (DestinationRepository repo : list.getRepositoryList()) {
			processor.accept(repo.getDescriptor());
		}
	}

	/*
	 * Set whether the task should fail if the repository already exists
	 */
	public void setFailOnExists(boolean value) {
		application.setFailOnExists(value);
	}

	public void setValidate(String value) {
		application.setComparator(value);
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

		if (getTaskName().equals(COMPOSITE_ADD)) {
			application.addChild(repo);
		} else if (getTaskName().equals(COMPOSITE_REMOVE)) {
			application.removeChild(repo);
		}
	}

	/*  p2.composite.artifact.repository.add */
	public void setComparatorID(String value) {
		if (value != null && !value.startsWith(ANT_PREFIX)) {
			application.setComparator(value);
		}
	}

	/*  p2.composite.artifact.repository.remove */
	public void setAllChildren(String value) {
		if (value != null && !value.startsWith(ANT_PREFIX)) {
			application.setRemoveAll(Boolean.parseBoolean(value));
		}
	}
}
