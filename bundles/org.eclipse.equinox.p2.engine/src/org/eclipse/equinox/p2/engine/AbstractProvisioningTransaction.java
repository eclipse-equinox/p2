/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.p2.core.helpers.MultiStatus;
import org.eclipse.equinox.p2.core.helpers.MultiStatusUtil;
import org.eclipse.osgi.util.NLS;

/**
 * An abstract provisioning transaction specifies a simple mechanism
 * for atomicity of a sequence of actions, based on the ability to
 * revert the consequences of those actions.
 * 
 * TODO: Implementation(s) of transactions should be registered via
 * 		 an extension point by classes that extended this class.
 * 		 This class should really be abstract.
 */
public abstract class AbstractProvisioningTransaction {

	private ArrayList actions = new ArrayList();

	private String description;

	private boolean isUndoable;

	private MultiStatus result;

	private IProgressMonitor progressMonitor;

	public AbstractProvisioningTransaction(String description, boolean isUndoable, MultiStatus result, IProgressMonitor monitor) {
		this.description = description;
		this.isUndoable = isUndoable;
		this.result = (result != null ? result : new MultiStatus());
		this.progressMonitor = monitor;
	}

	public IStatus performActions(IProvisioningAction[] ops, int[] weights, IProgressMonitor monitor) {
		SubMonitor pm = SubMonitor.convert(monitor, weights.length);
		IStatus status = Status.OK_STATUS;
		try {
			for (int i = 0; i < ops.length; i += 1) {
				IProvisioningAction action = ops[i];
				IProgressMonitor sub = (i < weights.length ? (IProgressMonitor) pm.newChild(weights[i]) : new NullProgressMonitor());
				status = performAction(action, sub);
				if (MultiStatusUtil.isErrorOrCancel(status)) {
					break;
				}
			}
		} finally {
			pm.done();
		}
		return status;
	}

	protected IStatus performAction(IProvisioningAction action, IProgressMonitor monitor) {
		actions.add(action);
		IStatus opStatus = action.perform(this, monitor);
		IStatus status = opStatus;

		if (!isUndoable) {
			result.add(opStatus);
			status = Status.OK_STATUS;
		} else if (MultiStatusUtil.isErrorOrCancel(opStatus)) {
			int length = actions.size();
			IProvisioningAction lastAction = (IProvisioningAction) actions.get(length - 1);
			if (!lastAction.shouldRevertOnError()) {
				actions.remove(length - 1);
			}
		} else if (monitor.isCanceled()) {
			// first time we noticed cancellation
			opStatus = new Status(IStatus.CANCEL, EngineActivator.ID, 0, ""/*Messages.Engine_Operation_Canceled_By_User*/, null);
		}

		if (opStatus.matches(IStatus.ERROR) && result.getMessage().length() == 0) {
			result.setMessage(NLS.bind("Errors occurred during the transaction {0}", //$NON-NLS-1$
					description));
		} else if (opStatus.matches(IStatus.CANCEL) && result.getMessage().length() == 0) {
			result.setMessage(NLS.bind("The transaction {0} was canceled", //$NON-NLS-1$
					description));
		}

		monitor.done();
		return status;
	}

	public void rollback(IProgressMonitor monitor) {
		if (!isUndoable)
			return;
		isUndoable = false;

		// TODO: is it necessary to allow support rollback that does NOT reverse the order
		//		 of the actions?  Consider phases.

		SubMonitor pm = SubMonitor.convert(monitor, actions.size());
		for (int i = actions.size() - 1; i >= 0; i--) {
			IProvisioningAction action = (IProvisioningAction) actions.get(i);
			try {
				IStatus status = action.revert(this, pm.newChild(10));
				// log.statusNotOK(status);
			} catch (Exception e) {
				// log.exception
			}
		}
	}

	public IProgressMonitor getProgressMonitor() {
		return progressMonitor;
	}

	public boolean isUndoable() {
		return isUndoable;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Transaction: "); //$NON-NLS-1$
		sb.append(description);
		sb.append(", ").append(actions.size()).append( //$NON-NLS-1$
				" actions performed"); //$NON-NLS-1$
		if (!isUndoable) {
			sb.append(", isUndoable=").append(isUndoable); //$NON-NLS-1$
		}
		return sb.toString();
	}

}
