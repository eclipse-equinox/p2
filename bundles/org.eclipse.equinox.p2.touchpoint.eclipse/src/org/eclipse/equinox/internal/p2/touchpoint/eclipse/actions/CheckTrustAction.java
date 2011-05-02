/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import org.eclipse.equinox.p2.query.QueryUtil;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * This action collects the set of bundle files on which the signature trust check
 * should be performed. The actual trust checking is done by the CheckTrust phase.
 */
public class CheckTrustAction extends ProvisioningAction {

	public static final String ID = "checkTrust"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction#execute(java.util.Map)
	 */
	public IStatus execute(Map<String, Object> parameters) {
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);
		if (iu == null)
			return null;
		IProvisioningAgent agent = (IProvisioningAgent) parameters.get(ActionConstants.PARM_AGENT);
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		//if the IU is already in the profile there is nothing to do
		if (!profile.available(QueryUtil.createIUQuery(iu), null).isEmpty())
			return null;
		@SuppressWarnings("unchecked")
		Collection<File> bundleFiles = (Collection<File>) parameters.get(ActionConstants.PARM_ARTIFACT_FILES);
		Collection<IArtifactKey> artifacts = iu.getArtifacts();
		if (artifacts == null)
			return null;
		for (IArtifactKey key : artifacts) {
			File bundleFile = Util.getArtifactFile(agent, key, profile);
			if (!bundleFiles.contains(bundleFile))
				bundleFiles.add(bundleFile);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction#undo(java.util.Map)
	 */
	public IStatus undo(Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}
}
