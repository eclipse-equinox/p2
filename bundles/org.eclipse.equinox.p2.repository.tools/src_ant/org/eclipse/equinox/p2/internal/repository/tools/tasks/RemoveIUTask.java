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

import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;

import org.eclipse.equinox.internal.provisional.p2.metadata.query.Query;

import java.util.Iterator;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.internal.repository.tools.AbstractApplication;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.osgi.util.NLS;

public class RemoveIUTask extends AbstractRepositoryTask {

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

			for (Iterator iter = iuTasks.iterator(); iter.hasNext();) {
				IUDescription iu = (IUDescription) iter.next();
				Query iuQuery = iu.createQuery();

				Collector collector = new Collector();
				repository.query(iuQuery, collector, null);

				if (collector.isEmpty())
					getProject().log(NLS.bind(Messages.AbstractRepositoryTask_unableToFind, iu.toString()));
				else if (repository.removeInstallableUnits(iuQuery, null) && artifacts != null) {
					for (Iterator iterator = collector.iterator(); iterator.hasNext();) {
						IInstallableUnit unit = (IInstallableUnit) iterator.next();
						IArtifactKey[] keys = unit.getArtifacts();
						for (int i = 0; i < keys.length; i++) {
							artifacts.removeDescriptor(keys[i]);
						}
					}
				}
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
}
