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

import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.MultiStatus;

public class EngineSession {

	private static class ActionsRecord {
		Operand operand;
		List actions = new ArrayList();

		ActionsRecord(Operand operand) {
			this.operand = operand;
		}
	}

	private List phaseActionRecordsPairs = new ArrayList();

	private Phase currentPhase;
	private List currentActionRecords;
	private ActionsRecord currentRecord;

	private Profile profile;

	public EngineSession(Profile profile) {
		this.profile = profile;
	}

	public void commit() {
		phaseActionRecordsPairs.clear();
	}

	public MultiStatus rollback() {
		MultiStatus result = new MultiStatus();
		if (currentPhase != null) {
			rollBackPhase(currentPhase, currentActionRecords);
			currentPhase = null;
			currentActionRecords = null;
			currentRecord = null;
		}

		for (ListIterator it = phaseActionRecordsPairs.listIterator(phaseActionRecordsPairs.size()); it.hasPrevious();) {
			Object[] pair = (Object[]) it.previous();
			Phase phase = (Phase) pair[0];
			List actionRecords = (List) pair[1];
			rollBackPhase(phase, actionRecords);
		}
		phaseActionRecordsPairs.clear();
		return result;
	}

	private MultiStatus rollBackPhase(Phase phase, List actionRecords) {
		MultiStatus result = new MultiStatus();

		if (phase != currentPhase)
			phase.prePerform(result, profile, new NullProgressMonitor());

		for (ListIterator it = actionRecords.listIterator(actionRecords.size()); it.hasPrevious();) {
			ActionsRecord record = (ActionsRecord) it.previous();
			ProvisioningAction[] actions = (ProvisioningAction[]) record.actions.toArray(new ProvisioningAction[record.actions.size()]);
			phase.undo(result, this, profile, record.operand, actions);
		}
		phase.postPerform(result, profile, new NullProgressMonitor());
		return result;
	}

	void recordPhaseStart(Phase phase) {
		if (phase == null)
			throw new IllegalArgumentException("Phase must not be null.");

		if (currentPhase != null)
			throw new IllegalStateException("A phase is already started.");

		currentPhase = phase;
		currentActionRecords = new ArrayList();
	}

	void recordPhaseEnd(Phase phase) {
		if (currentPhase == null)
			throw new IllegalStateException("There is no phase to end.");

		if (currentPhase != phase)
			throw new IllegalArgumentException("Current phase does not match argument.");

		phaseActionRecordsPairs.add(new Object[] {currentPhase, currentActionRecords});
		currentPhase = null;
		currentActionRecords = null;
		currentRecord = null;
	}

	void recordAction(ProvisioningAction action, Operand operand) {
		if (action == null || operand == null)
			throw new IllegalArgumentException("Action and Operand must not be null.");

		if (currentRecord == null || operand != currentRecord.operand) {
			currentRecord = new ActionsRecord(operand);
			currentActionRecords.add(currentRecord);
		}
		currentRecord.actions.add(action);
	}
}
