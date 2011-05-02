/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.ActionConstants;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.Touchpoint;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.osgi.util.NLS;

public class NativeTouchpoint extends Touchpoint {

	public static final String PARM_BACKUP = "backup"; //$NON-NLS-1$

	public static final String PARM_ARTIFACT = "artifact"; //$NON-NLS-1$

	public static final String PARM_ARTIFACT_LOCATION = "artifact.location"; //$NON-NLS-1$

	private static Map<IProfile, IBackupStore> backups = new WeakHashMap<IProfile, IBackupStore>();

	public IStatus initializeOperand(IProfile profile, Map<String, Object> parameters) {
		IProvisioningAgent agent = (IProvisioningAgent) parameters.get(ActionConstants.PARM_AGENT);
		IArtifactKey artifactKey = (IArtifactKey) parameters.get(PARM_ARTIFACT);
		if (!parameters.containsKey(PARM_ARTIFACT_LOCATION) && artifactKey != null) {
			try {
				IFileArtifactRepository downloadCache = Util.getDownloadCacheRepo(agent);
				File fileLocation = downloadCache.getArtifactFile(artifactKey);
				if (fileLocation != null && fileLocation.exists())
					parameters.put(PARM_ARTIFACT_LOCATION, fileLocation.getAbsolutePath());
			} catch (ProvisionException e) {
				return e.getStatus();
			}
		}
		return Status.OK_STATUS;
	}

	public IStatus initializePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map<String, Object> touchpointParameters) {
		touchpointParameters.put(PARM_BACKUP, getBackupStore(profile));
		return null;
	}

	public String qualifyAction(String actionId) {
		return Activator.ID + "." + actionId; //$NON-NLS-1$
	}

	public IStatus prepare(IProfile profile) {
		// does not have to do anything - everything is already in the correct place
		// the commit means that the backup is discarded - if that fails it is not a 
		// terrible problem.
		return super.prepare(profile);
	}

	public IStatus commit(IProfile profile) {
		IBackupStore store = getBackupStore(profile);
		store.discard();
		clearProfileState(profile);
		return Status.OK_STATUS;
	}

	/**
	 * Converts a profile id into a string that can be used as a file name in any file system.
	 */
	public static String escape(String toEscape) {
		StringBuffer buffer = new StringBuffer();
		int length = toEscape.length();
		for (int i = 0; i < length; ++i) {
			char ch = toEscape.charAt(i);
			switch (ch) {
				case '\\' :
				case '/' :
				case ':' :
				case '*' :
				case '?' :
				case '"' :
				case '<' :
				case '>' :
				case '|' :
				case '%' :
					buffer.append("%" + (int) ch + ";"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				default :
					buffer.append(ch);
			}
		}
		return buffer.toString();
	}

	public IStatus rollback(IProfile profile) {
		IStatus returnStatus = Status.OK_STATUS;
		IBackupStore store = getBackupStore(profile);
		try {
			store.restore();
		} catch (IOException e) {
			returnStatus = new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.failed_backup_restore, store.getBackupName()), e);
		} catch (ClosedBackupStoreException e) {
			returnStatus = new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.failed_backup_restore, store.getBackupName()), e);
		}
		clearProfileState(profile);
		return returnStatus;
	}

	/**
	 * Cleans up the transactional state associated with a profile.
	 */
	private static synchronized void clearProfileState(IProfile profile) {
		backups.remove(profile);
	}

	/**
	 * Gets the transactional state associated with a profile. A transactional state is
	 * created if it did not exist.
	 * @param profile
	 * @return a lazily initialized backup store
	 */
	private static synchronized IBackupStore getBackupStore(IProfile profile) {
		IBackupStore store = backups.get(profile);
		if (store == null) {
			store = new LazyBackupStore(escape(profile.getProfileId()));
			backups.put(profile, store);
		}
		return store;
	}
}
