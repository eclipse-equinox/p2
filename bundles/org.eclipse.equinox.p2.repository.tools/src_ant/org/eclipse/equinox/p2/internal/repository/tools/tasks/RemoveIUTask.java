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

import java.util.*;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.internal.repository.tools.AbstractApplication;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

public class RemoveIUTask extends AbstractRepositoryTask {
	private static final String CLASSIFIER = "classifier"; //$NON-NLS-1$
	private static final String ID = "id"; //$NON-NLS-1$
	private static final String VERSION = "version"; //$NON-NLS-1$

	protected static class RemoveIUApplication extends AbstractApplication {
		//Only need the application to reuse super's repo management.
		public IStatus run(IProgressMonitor monitor) {
			return null;
		}

		public void finalizeRepos() throws ProvisionException {
			super.finalizeRepositories();
		}
	}

	public RemoveIUTask() {
		this.application = new RemoveIUApplication();
	}

	public void execute() throws BuildException {
		try {
			if (iuTasks == null || iuTasks.isEmpty())
				return; //nothing to do

			application.initializeRepos(null);
			if (application.getCompositeMetadataRepository() == null)
				throw new BuildException(Messages.AbstractApplication_no_valid_destinations); //need a repo

			IMetadataRepository repository = application.getDestinationMetadataRepository();
			IArtifactRepository artifacts = application.getDestinationArtifactRepository();

			final Set toRemove = new HashSet();
			for (Iterator iter = iuTasks.iterator(); iter.hasNext();) {
				IUDescription iu = (IUDescription) iter.next();
				IQuery iuQuery = iu.createQuery();

				Collector collector = new Collector();
				repository.query(iuQuery, collector, null);

				if (collector.isEmpty())
					getProject().log(NLS.bind(Messages.AbstractRepositoryTask_unableToFind, iu.toString()));
				else {
					for (Iterator iterator = collector.iterator(); iterator.hasNext();) {
						IInstallableUnit unit = (IInstallableUnit) iterator.next();
						IArtifactKey[] keys = unit.getArtifacts();
						Filter filter = null;
						try {
							filter = iu.getArtifactFilter();
						} catch (InvalidSyntaxException e) {
							getProject().log(NLS.bind(Messages.skippingInvalidFilter, iu.toString()));
							continue;
						}

						//we will only remove the metadata if all artifacts were removed
						boolean removeMetadata = true;
						for (int i = 0; i < keys.length; i++) {
							if (filter == null) {
								artifacts.removeDescriptor(keys[i]);
							} else {
								IArtifactDescriptor[] descriptors = artifacts.getArtifactDescriptors(keys[i]);
								for (int j = 0; j < descriptors.length; j++) {
									if (filter.match(createDictionary(descriptors[j]))) {
										artifacts.removeDescriptor(descriptors[j]);
									} else {
										removeMetadata = false;
									}
								}
							}
						}
						if (removeMetadata)
							toRemove.add(unit);
					}
				}
			}

			if (toRemove.size() > 0) {
				IQuery removeQuery = new MatchQuery() {
					public boolean isMatch(Object candidate) {
						return toRemove.contains(candidate);
					}
				};
				repository.removeInstallableUnits(removeQuery, null);
			}
		} catch (ProvisionException e) {
			throw new BuildException(e);
		} finally {
			try {
				((RemoveIUApplication) application).finalizeRepos();
			} catch (ProvisionException e) {
				throw new BuildException(e);
			}
		}
	}

	private Dictionary createDictionary(IArtifactDescriptor descriptor) {
		Hashtable result = new Hashtable(5);
		result.putAll(descriptor.getProperties());
		IArtifactKey key = descriptor.getArtifactKey();
		result.put(CLASSIFIER, key.getClassifier());
		result.put(ID, key.getId());
		result.put(VERSION, key.getVersion());
		return result;
	}
}
