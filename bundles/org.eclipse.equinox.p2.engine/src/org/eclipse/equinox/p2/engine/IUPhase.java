/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.helpers.MultiStatus;

// An operation that is applied to a set of IUs.
public abstract class IUPhase extends Phase {
	protected int PRE_PERFORM_WORK = 1000;
	protected int PERFORM_WORK = 10000;
	protected int POST_PERFORM_WORK = 1000;

	protected IUPhase(int weight, String phaseName) {
		super(weight, phaseName);
	}

	protected void perform(MultiStatus status, EngineSession session, Profile profile, Operand[] operands, IProgressMonitor monitor) { //TODO Maybe should we do some kind of adaptable
		SubMonitor subMonitor = SubMonitor.convert(monitor, PRE_PERFORM_WORK + PERFORM_WORK + POST_PERFORM_WORK);
		prePerform(status, profile, operands, subMonitor.newChild(PRE_PERFORM_WORK));
		if (status.isErrorOrCancel())
			return;

		subMonitor.setWorkRemaining(PERFORM_WORK + POST_PERFORM_WORK);
		mainPerform(status, session, profile, operands, subMonitor.newChild(PERFORM_WORK));
		if (status.isErrorOrCancel())
			return;

		subMonitor.setWorkRemaining(POST_PERFORM_WORK);
		postPerform(status, profile, operands, subMonitor.newChild(POST_PERFORM_WORK));
		if (status.isErrorOrCancel())
			return;

		subMonitor.done();
	}

	protected void mainPerform(MultiStatus status, EngineSession session, Profile profile, Operand[] operands, SubMonitor subMonitor) {
		int operandWork = PERFORM_WORK / operands.length;
		for (int i = 0; i < operands.length; ++i) {
			if (subMonitor.isCanceled())
				throw new OperationCanceledException();
			Operand currentOperand = operands[i];
			if (!isApplicable(currentOperand))
				continue;
			IStatus result = performOperand(session, profile, currentOperand, subMonitor.newChild(operandWork));
			status.add(result);
			if (status.isErrorOrCancel())
				return;
		}
	}

	protected abstract boolean isApplicable(Operand op);

	//			ITouchpoint touchpoint = TouchpointManager.getInstance().getTouchpoint(currentOperand.getTouchpointType());
	//			if (touchpoint == null) { //TODO Should we throw an exception instead?
	//				status.add(new Status(IStatus.ERROR, "org.eclipse.equinox.p2.engine", "The touchpoint " + currentOperand.getTouchpointType() + " is not available."));
	//				return;
	//			}
	//			if (touchpoint.supports(phaseId)) {
	//				status.add(performIU(touchpoint, currentOperand, subMonitor.newChild(operandWork)));
	//			}
	//			if (status.isErrorOrCancel() || sub.isCanceled()) {
	//				undoPerform(status, ius, i, context);
	//				return;
	//			}

	// Error or cancel: undo IUs that were done.
	//	private void undoPerform(MultiStatus status, InstallableUnitPair[] ius, int currentIU, InstallContext context) {
	//		if (!status.isErrorOrCancel()) {
	//			status.setCanceled(); // first time we noticed cancelation
	//			currentIU += 1; // currentIU was completed so it must be undone
	//		}
	//		InstallableUnitPair[] undoIUs = new InstallableUnitPair[currentIU];
	//		for (int i = 0; i < currentIU; i += 1) {
	//			log.debug("Undo {0} phase for {1}", super.phaseName, ius[i]); //$NON-NLS-1$
	//			undoIUs[i] = ius[currentIU - (i + 1)].reverse();
	//		}
	//		// 1 unit to undo this phase, 10 for preceding phases
	//		SplitProgressMonitor pm = new SplitProgressMonitor(getUndoProgressMonitor(), new int[] {1, 10});
	//		doPerform(status, /*undoable*/false, undoIUs, context, pm.next());
	//		setUndoProgressMonitor(pm.next());
	//	}
	protected abstract IStatus performOperand(EngineSession session, Profile profile, Operand operand, IProgressMonitor monitor);

	protected void prePerform(MultiStatus status, Profile profile, Operand[] deltas, IProgressMonitor monitor) {
		//Nothing to do.
	}

	protected void postPerform(MultiStatus status, Profile profile, Operand[] deltas, IProgressMonitor monitor) {
		//Nothing to do.
	}
}
