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

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.Touchpoint;
import org.eclipse.osgi.util.NLS;

public class NativeTouchpoint extends Touchpoint {

	public static final String PARM_INSTALL_FOLDER = "installFolder"; //$NON-NLS-1$
	public static final String PARM_BACKUP = "backup"; //$NON-NLS-1$

	private static Map backups = new WeakHashMap();

	public IStatus initializePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map touchpointParameters) {
		touchpointParameters.put(PARM_INSTALL_FOLDER, Util.getInstallFolder(profile));
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
		return Status.OK_STATUS;
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
		IBackupStore store = (IBackupStore) backups.get(profile);
		if (store == null) {
			store = new LazyBackupStore(profile.getProfileId());
			backups.put(profile, store);
		}
		return store;
	}
}
