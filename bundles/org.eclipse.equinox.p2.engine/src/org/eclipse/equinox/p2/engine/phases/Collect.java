/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.engine.phases;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRequest;
import org.eclipse.equinox.p2.download.DownloadManager;
import org.eclipse.equinox.p2.engine.*;

/**
 * The goal of the collect phase is to ask the touchpoints if the artifacts associated with an IU need to be downloaded.
 */
public class Collect extends Phase {
	private static final String PHASE_ID = "collect"; //$NON-NLS-1$

	public Collect(int weight) {
		super(PHASE_ID, weight, Messages.Engine_Collect_Phase);
		//re-balance work since postPerform will do almost all the time-consuming work
		PRE_PERFORM_WORK = 0;
		PERFORM_WORK = 100;
		POST_PERFORM_WORK = 1000;
	}

	//	protected IStatus performOperand(EngineSession session, Profile profile, Operand operand, IProgressMonitor monitor) {
	//		IInstallableUnit unit = operand.second();
	//
	//		if (unit != null) {
	//			monitor.subTask(NLS.bind(Messages.Engine_Collecting_For_IU, unit.getId()));
	//
	//			// TODO: Need do progress reporting
	//
	//			// Ask all the touchpoints if they need to download an artifact
	//			ITouchpoint touchpoint = TouchpointManager.getInstance().getTouchpoint(unit.getTouchpointType());
	//			if (touchpoint.supports(PHASE_ID)) {
	//				ITouchpointAction[] actions = touchpoint.getActions(PHASE_ID, profile, operand);
	//				for (int i = 0; i < actions.length; i++) {
	//					Object result = actions[i].execute();
	//					if (result != null)
	//						dm.add((IArtifactRequest[]) result);
	//					session.record(actions[i]);
	//				}
	//			}
	//
	//			if (monitor.isCanceled())
	//				return Status.CANCEL_STATUS;
	//		}
	//
	//		return Status.OK_STATUS;
	//	}

	protected boolean isApplicable(Operand op) {
		if (op.second() != null)
			return true;
		return false;
	}

	protected ITouchpointAction[] getActions(ITouchpoint touchpoint, Profile profile, Operand currentOperand) {
		return touchpoint.getActions(PHASE_ID, profile, currentOperand);
	}

	protected IStatus completePhase(IProgressMonitor monitor, Profile profile, Map parameters) {
		List artifactRequests = (List) parameters.get("artifactRequests");

		DownloadManager dm = new DownloadManager();
		for (Iterator it = artifactRequests.iterator(); it.hasNext();) {
			IArtifactRequest[] requests = (IArtifactRequest[]) it.next();
			dm.add(requests);
		}
		return dm.start(monitor);
	}

	protected IStatus initializePhase(IProgressMonitor monitor, Profile profile, Map parameters) {
		parameters.put("artifactRequests", new ArrayList());
		return null;
	}
}