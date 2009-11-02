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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.artifact.mirror.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;

public class MirrorApplication extends AbstractApplication {
	private static final String LOG_ROOT = "p2.mirror"; //$NON-NLS-1$

	protected SlicingOptions slicingOptions = new SlicingOptions();

	private URI baseline;
	private String comparatorID;
	private boolean compare = false;
	private boolean failOnError = true;
	private boolean raw = true;
	private boolean verbose = false;
	private boolean validate = false;

	private File mirrorLogFile; // file to log mirror output to (optional)
	private File comparatorLogFile; // file to comparator output to (optional)
	private IArtifactMirrorLog mirrorLog;
	private IArtifactMirrorLog comparatorLog;

	public Object start(IApplicationContext context) throws Exception {
		run(null);
		return IApplication.EXIT_OK;
	}

	public IStatus run(IProgressMonitor monitor) throws ProvisionException {
		IStatus mirrorStatus = Status.OK_STATUS;
		try {
			initializeRepos(new NullProgressMonitor());
			initializeLogs();
			validate();
			initializeIUs();
			IQueryable slice = slice(new NullProgressMonitor());
			if (destinationArtifactRepository != null) {
				mirrorStatus = mirrorArtifacts(slice, new NullProgressMonitor());
				if (mirrorStatus.getSeverity() == IStatus.ERROR)
					return mirrorStatus;
			}
			if (destinationMetadataRepository != null)
				mirrorMetadata(slice, new NullProgressMonitor());
		} finally {
			finalizeRepositories();
			finalizeLogs();
		}
		if (mirrorStatus.isOK())
			return Status.OK_STATUS;
		return mirrorStatus;
	}

	private IStatus mirrorArtifacts(IQueryable slice, IProgressMonitor monitor) throws ProvisionException {
		// Obtain ArtifactKeys from IUs
		Collector ius = slice.query(InstallableUnitQuery.ANY, new Collector(), monitor);
		ArrayList keys = new ArrayList(ius.size());
		for (Iterator iterator = ius.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			IArtifactKey[] iuKeys = iu.getArtifacts();
			for (int i = 0; i < iuKeys.length; i++) {
				keys.add(iuKeys[i]);
			}
		}
		Mirroring mirror = new Mirroring(getCompositeArtifactRepository(), destinationArtifactRepository, raw);

		mirror.setCompare(compare);
		mirror.setComparatorId(comparatorID);
		mirror.setBaseline(initializeBaseline());
		mirror.setValidate(validate);

		// If IUs have been specified then only they should be mirrored, otherwise mirror everything.
		if (keys.size() > 0)
			mirror.setArtifactKeys((IArtifactKey[]) keys.toArray(new IArtifactKey[keys.size()]));

		if (comparatorLog != null)
			mirror.setComparatorLog(comparatorLog);

		IStatus result = mirror.run(failOnError, verbose);

		if (mirrorLog != null)
			mirrorLog.log(result);
		else
			LogHelper.log(result);
		return result;
	}

	private IArtifactRepository initializeBaseline() throws ProvisionException {
		if (baseline == null)
			return null;
		return addRepository(getArtifactRepositoryManager(), baseline, 0, null);
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
		if (sourceRepositories.isEmpty())
			throw new ProvisionException(Messages.MirrorApplication_set_source_repositories);
		if (!hasArtifactSources() && destinationArtifactRepository != null)
			throw new ProvisionException(Messages.MirrorApplication_artifactDestinationNoSource);
		if (!hasMetadataSources() && destinationMetadataRepository != null)
			throw new ProvisionException(Messages.MirrorApplication_metadataDestinationNoSource);
	}

	/*
	 * If no IUs have been specified we want to mirror them all
	 */
	private void initializeIUs() throws ProvisionException {
		if (sourceIUs == null || sourceIUs.isEmpty()) {
			sourceIUs = new ArrayList();
			IMetadataRepository metadataRepo = getCompositeMetadataRepository();
			Collector collector = metadataRepo.query(InstallableUnitQuery.ANY, new Collector(), null);

			for (Iterator iter = collector.iterator(); iter.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) iter.next();
				sourceIUs.add(iu);
			}

			if (collector.size() == 0 && destinationMetadataRepository != null)
				throw new ProvisionException(Messages.MirrorApplication_no_IUs);
		}
	}

	/*
	 * Initialize logs, if applicable
	 */
	private void initializeLogs() {
		if (compare && comparatorLogFile != null)
			comparatorLog = getLog(comparatorLogFile, comparatorID);
		if (mirrorLog == null && mirrorLogFile != null)
			mirrorLog = getLog(mirrorLogFile, LOG_ROOT);
	}

	/*
	 * Finalize logs, if applicable
	 */
	private void finalizeLogs() {
		if (comparatorLog != null)
			comparatorLog.close();
		if (mirrorLog != null)
			mirrorLog.close();
	}

	/*
	 * Get the log for a location
	 */
	private IArtifactMirrorLog getLog(File location, String root) {
		String absolutePath = location.getAbsolutePath();
		if (absolutePath.toLowerCase().endsWith(".xml")) //$NON-NLS-1$
			return new XMLMirrorLog(absolutePath, 0, root);
		return new FileMirrorLog(absolutePath, 0, root);
	}

	private IQueryable slice(IProgressMonitor monitor) throws ProvisionException {
		if (slicingOptions == null)
			slicingOptions = new SlicingOptions();
		PermissiveSlicer slicer = new PermissiveSlicer(getCompositeMetadataRepository(), slicingOptions.getFilter(), slicingOptions.includeOptionalDependencies(), slicingOptions.isEverythingGreedy(), slicingOptions.forceFilterTo(), slicingOptions.considerStrictDependencyOnly(), slicingOptions.followOnlyFilteredRequirements());
		IQueryable slice = slicer.slice((IInstallableUnit[]) sourceIUs.toArray(new IInstallableUnit[sourceIUs.size()]), monitor);

		if (slice != null && slicingOptions.latestVersionOnly()) {
			Collector collector = new Collector();
			collector = slice.query(new LatestIUVersionQuery(), collector, monitor);
			slice = collector;
		}
		if (slicer.getStatus().getSeverity() != IStatus.OK && mirrorLog != null) {
			mirrorLog.log(slicer.getStatus());
		}
		if (slice == null) {
			throw new ProvisionException(slicer.getStatus());
		}
		return slice;
	}

	public void setSlicingOptions(SlicingOptions options) {
		slicingOptions = options;
	}

	/*
	 * Set the location of the baseline repository. (used in comparison)
	 */
	public void setBaseline(URI baseline) {
		this.baseline = baseline;
		compare = true;
	}

	/*
	 * Set the identifier of the comparator to use.
	 */
	public void setComparatorID(String value) {
		comparatorID = value;
		compare = true;
	}

	/*
	 * Set whether or not the application should be calling a comparator when mirroring.
	 */
	public void setCompare(boolean value) {
		compare = value;
	}

	/*
	 * Set whether or not we should ignore errors when running the mirror application.
	 */
	public void setIgnoreErrors(boolean value) {
		failOnError = !value;
	}

	/*
	 * Set whether or not the the artifacts are raw.
	 */
	public void setRaw(boolean value) {
		raw = value;
	}

	/*
	 * Set whether or not the mirror application should be run in verbose mode.
	 */
	public void setVerbose(boolean value) {
		verbose = value;
	}

	/*
	 * Set the location of the log for comparator output
	 */
	public void setComparatorLog(File comparatorLog) {
		this.comparatorLogFile = comparatorLog;
	}

	/*
	 * Set the location of the log for mirroring. 
	 */
	public void setLog(File mirrorLog) {
		this.mirrorLogFile = mirrorLog;
	}

	/*
	 * Set the ArtifactMirror log
	 */
	public void setLog(IArtifactMirrorLog log) {
		mirrorLog = log;
	}

	/*
	 * Set if the artifact mirror should be validated
	 */
	public void setValidate(boolean value) {
		validate = value;
	}
}
