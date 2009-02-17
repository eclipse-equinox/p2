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

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.artifact.mirror.Mirroring;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;

public class MirrorApplication extends AbstractApplication {
	protected SlicingOptions slicingOptions = new SlicingOptions();

	public Object start(IApplicationContext context) throws Exception {
		run(null);
		return IApplication.EXIT_OK;
	}

	public IStatus run(IProgressMonitor monitor) throws ProvisionException {
		try {
			validate();
			initializeRepos(new NullProgressMonitor());
			IQueryable slice = slice(new NullProgressMonitor());
			mirrorArtifacts(slice, new NullProgressMonitor());
			mirrorMetadata(slice, new NullProgressMonitor());
		} finally {
			finalizeRepositories();
		}
		return Status.OK_STATUS;
	}

	private void mirrorArtifacts(IQueryable slice, IProgressMonitor monitor) {
		Collector ius = slice.query(InstallableUnitQuery.ANY, new Collector(), monitor);
		ArrayList keys = new ArrayList(ius.size());
		for (Iterator iterator = ius.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			IArtifactKey[] iuKeys = iu.getArtifacts();
			for (int i = 0; i < iuKeys.length; i++) {
				keys.add(iuKeys[i]);
			}
		}
		Mirroring mirror = new Mirroring(getCompositeArtifactRepository(), destinationArtifactRepository, true);
		mirror.setArtifactKeys((IArtifactKey[]) keys.toArray(new IArtifactKey[keys.size()]));
		mirror.run(true, false);
	}

	private void mirrorMetadata(IQueryable slice, IProgressMonitor monitor) {
		Collector allIUs = slice.query(InstallableUnitQuery.ANY, new Collector(), monitor);
		destinationMetadataRepository.addInstallableUnits((IInstallableUnit[]) allIUs.toArray(IInstallableUnit.class));
	}

	/*
	 * Ensure all mandatory parameters have been set. Throw an exception if there
	 * are any missing. We don't require the user to specify the artifact repository here,
	 * we will default to the ones already registered in the manager. (callers are free
	 * to add more if they wish)
	 */
	private void validate() throws ProvisionException {
		if (sourceMetadataRepositories == null)
			throw new ProvisionException("Need to set the source metadata repository location.");
		if (sourceIUs == null)
			throw new ProvisionException("Mirroring root needs to be specified.");
		//TODO Check that the IU is in repo
	}

	private IQueryable slice(IProgressMonitor monitor) {
		if (slicingOptions == null)
			slicingOptions = new SlicingOptions();
		PermissiveSlicer slicer = new PermissiveSlicer(getCompositeMetadataRepository(), slicingOptions.getFilter(), slicingOptions.includeOptionalDependencies(), slicingOptions.isEverythingGreedy(), slicingOptions.forceFilterTo(), slicingOptions.considerStrictDependencyOnly());
		return slicer.slice((IInstallableUnit[]) sourceIUs.toArray(new IInstallableUnit[sourceIUs.size()]), monitor);
	}

	public void setSlicingOptions(SlicingOptions options) {
		slicingOptions = options;
	}
}
