/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.io.File;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class SetProgramPropertyAction extends ProvisioningAction {
	public static final String ID = "setProgramProperty"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String propName = (String) parameters.get(ActionConstants.PARM_PROP_NAME);
		if (propName == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_PROP_NAME, ID));
		String propValue = (String) parameters.get(ActionConstants.PARM_PROP_VALUE);
		if (propValue != null && propValue.equals(ActionConstants.PARM_ARTIFACT)) {
			try {
				propValue = resolveArtifactParam(parameters);
			} catch (CoreException e) {
				return e.getStatus();
			}
		}

		getMemento().put(ActionConstants.PARM_PREVIOUS_VALUE, manipulator.getConfigData().getProperty(propName));
		manipulator.getConfigData().setProperty(propName, propValue);
		return Status.OK_STATUS;
	}

	public IStatus undo(Map parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String propName = (String) parameters.get(ActionConstants.PARM_PROP_NAME);
		if (propName == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_PROP_NAME, ID));
		String previousValue = (String) getMemento().get(ActionConstants.PARM_PREVIOUS_VALUE);
		manipulator.getConfigData().setProperty(propName, previousValue);
		return Status.OK_STATUS;
	}

	private static String resolveArtifactParam(Map parameters) throws CoreException {
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);
		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts == null || artifacts.length == 0)
			throw new CoreException(Util.createError(NLS.bind(Messages.iu_contains_no_arifacts, iu)));

		IArtifactKey artifactKey = artifacts[0];

		File fileLocation = Util.getArtifactFile(artifactKey, profile);
		if (fileLocation == null || !fileLocation.exists())
			throw new CoreException(Util.createError(NLS.bind(Messages.artifact_file_not_found, artifactKey)));
		return fileLocation.getAbsolutePath();
	}
}