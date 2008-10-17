/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.Touchpoint;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Version;

public class NativeTouchpoint extends Touchpoint {
	public static final String PARM_TARGET_FILE = "targetFile"; //$NON-NLS-1$
	public static final String PARM_PERMISSIONS = "permissions"; //$NON-NLS-1$
	public static final String PARM_TARGET_DIR = "targetDir"; //$NON-NLS-1$
	public static final String PARM_TARGET = "target"; //$NON-NLS-1$
	public static final String ACTION_CHMOD = "chmod"; //$NON-NLS-1$
	public static final String PARM_IU = "iu"; //$NON-NLS-1$
	public static final String PIPE = "|"; //$NON-NLS-1$
	public static final String PARM_SOURCE = "source"; //$NON-NLS-1$
	public static final String PARM_ARTIFACT = "@artifact"; //$NON-NLS-1$
	public static final String PARM_INSTALL_FOLDER = "installFolder"; //$NON-NLS-1$
	public static final String NATIVE_TOUCHPOINT_TYPE = "org.eclipse.equinox.p2.native"; //$NON-NLS-1$
	public static final String ACTION_CLEANUPZIP = "cleanupzip"; //$NON-NLS-1$
	public static final String ACTION_UNZIP = "unzip"; //$NON-NLS-1$
	public static final String PARM_ARTIFACT_REQUESTS = "artifactRequests"; //$NON-NLS-1$
	public static final String PARM_OPERAND = "operand"; //$NON-NLS-1$
	public static final String PARM_PROFILE = "profile"; //$NON-NLS-1$
	public static final String ACTION_COLLECT = "collect"; //$NON-NLS-1$
	public static final String ID = "org.eclipse.equinox.p2.touchpoint.natives"; //$NON-NLS-1$

	public static IStatus createError(String message) {
		return new Status(IStatus.ERROR, ID, message);
	}

	public TouchpointType getTouchpointType() {
		return MetadataFactory.createTouchpointType(NATIVE_TOUCHPOINT_TYPE, new Version(1, 0, 0));
	}

	private String getInstallFolder(IProfile profile) {
		return profile.getProperty(IProfile.PROP_INSTALL_FOLDER);
	}

	private static AgentLocation getAgentLocation() {
		return (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
	}

	public static IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	static public IFileArtifactRepository getDownloadCacheRepo() throws ProvisionException {
		URI location = getDownloadCacheLocation();
		if (location == null)
			throw new IllegalStateException(Messages.could_not_obtain_download_cache);
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		if (manager == null)
			throw new IllegalStateException(Messages.artifact_repo_not_found);
		IArtifactRepository repository;
		try {
			repository = manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			// the download cache doesn't exist or couldn't be read. Create new cache.
			String repositoryName = location + " - Agent download cache"; //$NON-NLS-1$
			Map properties = new HashMap(1);
			properties.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			repository = manager.createRepository(location, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		}

		IFileArtifactRepository downloadCache = (IFileArtifactRepository) repository.getAdapter(IFileArtifactRepository.class);
		if (downloadCache == null)
			throw new ProvisionException(NLS.bind(Messages.download_cache_not_writeable, location));
		return downloadCache;
	}

	static private URI getDownloadCacheLocation() {
		AgentLocation location = getAgentLocation();
		return (location != null ? location.getArtifactRepositoryURI() : null);
	}

	public IStatus initializePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map touchpointParameters) {
		touchpointParameters.put(PARM_INSTALL_FOLDER, getInstallFolder(profile));
		return null;
	}

	public static IStatus unzip(Map parameters) {
		String source = (String) parameters.get(PARM_SOURCE);
		if (source == null)
			return createError(NLS.bind(Messages.param_not_set, PARM_SOURCE, ACTION_UNZIP));

		String originalSource = source;
		String target = (String) parameters.get(PARM_TARGET);
		if (target == null)
			return createError(NLS.bind(Messages.param_not_set, PARM_TARGET, ACTION_UNZIP));

		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		Profile profile = (Profile) parameters.get(PARM_PROFILE);

		if (source.equals(PARM_ARTIFACT)) {
			//TODO: fix wherever this occurs -- investigate as this is probably not desired
			if (iu.getArtifacts() == null || iu.getArtifacts().length == 0)
				return Status.OK_STATUS;

			IArtifactKey artifactKey = iu.getArtifacts()[0];

			IFileArtifactRepository downloadCache;
			try {
				downloadCache = getDownloadCacheRepo();
			} catch (ProvisionException e) {
				return e.getStatus();
			}
			File fileLocation = downloadCache.getArtifactFile(artifactKey);
			if ((fileLocation == null) || !fileLocation.exists())
				return createError(NLS.bind(Messages.artifact_not_available, artifactKey));
			source = fileLocation.getAbsolutePath();
		}

		File[] unzippedFiles = new Zip().unzip(source, target, null);
		StringBuffer unzippedFileNameBuffer = new StringBuffer();
		for (int i = 0; i < unzippedFiles.length; i++)
			unzippedFileNameBuffer.append(unzippedFiles[i].getAbsolutePath()).append(PIPE);

		profile.setInstallableUnitProperty(iu, "unzipped" + PIPE + originalSource + PIPE + target, unzippedFileNameBuffer.toString()); //$NON-NLS-1$

		return Status.OK_STATUS;
	}

	public static IStatus cleanupzip(Map parameters) {
		String source = (String) parameters.get(PARM_SOURCE);
		if (source == null)
			return createError(NLS.bind(Messages.param_not_set, PARM_SOURCE, ACTION_CLEANUPZIP));
		String target = (String) parameters.get(PARM_TARGET);
		if (target == null)
			return createError(NLS.bind(Messages.param_not_set, PARM_TARGET, ACTION_CLEANUPZIP));

		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		IProfile profile = (IProfile) parameters.get(PARM_PROFILE);

		String unzipped = profile.getInstallableUnitProperty(iu, "unzipped" + PIPE + source + PIPE + target); //$NON-NLS-1$

		if (unzipped == null)
			return Status.OK_STATUS;

		StringTokenizer tokenizer = new StringTokenizer(unzipped, PIPE);
		List directories = new ArrayList();
		while (tokenizer.hasMoreTokens()) {
			String fileName = tokenizer.nextToken();
			File file = new File(fileName);
			if (!file.exists())
				continue;

			if (file.isDirectory())
				directories.add(file);
			else
				file.delete();
		}

		for (Iterator it = directories.iterator(); it.hasNext();) {
			File directory = (File) it.next();
			directory.delete();
		}

		return Status.OK_STATUS;
	}

	public String qualifyAction(String actionId) {
		return ID + "." + actionId; //$NON-NLS-1$
	}
}
