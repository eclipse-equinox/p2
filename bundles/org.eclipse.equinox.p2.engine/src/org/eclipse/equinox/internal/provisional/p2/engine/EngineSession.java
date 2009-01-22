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
package org.eclipse.equinox.internal.provisional.p2.engine;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.osgi.util.NLS;

public class EngineSession {

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

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

	private IProfile profile;

	private File profileDataDirectory;

	private ProvisioningContext context;

	private Set touchpoints = new HashSet();

	public EngineSession(IProfile profile, File profileDataDirectory, ProvisioningContext context) {
		this.profile = profile;
		this.profileDataDirectory = profileDataDirectory;
		this.context = context;
	}

	public File getProfileDataDirectory() {
		return profileDataDirectory;
	}

	IStatus prepare() {
		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		for (Iterator iterator = touchpoints.iterator(); iterator.hasNext();) {
			Touchpoint touchpoint = (Touchpoint) iterator.next();
			try {
				status.add(touchpoint.prepare(profile));
			} catch (RuntimeException e) {
				// "touchpoint.prepare" calls user code and might throw an unchecked exception
				// we catch the error here to gather information on where the problem occurred.
				status.add(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.touchpoint_prepare_error, touchpoint.getClass().getName()), e));
			} catch (LinkageError e) {
				// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
				status.add(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.touchpoint_prepare_error, touchpoint.getClass().getName()), e));
			}
		}

		if (status.matches(IStatus.ERROR)) {
			MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.ERROR, NLS.bind(Messages.session_prepare_error, profile.getProfileId()), null);
			result.merge(status);
			return result;
		}
		return status;
	}

	IStatus commit() {
		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		phaseActionRecordsPairs.clear();
		for (Iterator iterator = touchpoints.iterator(); iterator.hasNext();) {
			Touchpoint touchpoint = (Touchpoint) iterator.next();
			try {
				status.add(touchpoint.commit(profile));
			} catch (RuntimeException e) {
				// "touchpoint.commit" calls user code and might throw an unchecked exception
				// we catch the error here to gather information on where the problem occurred.
				status.add(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.touchpoint_commit_error, touchpoint.getClass().getName()), e));
			} catch (LinkageError e) {
				// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
				status.add(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.touchpoint_commit_error, touchpoint.getClass().getName()), e));
			}
		}

		if (status.matches(IStatus.ERROR)) {
			MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.ERROR, NLS.bind(Messages.session_commit_error, profile.getProfileId()), null);
			result.merge(status);
			return result;
		}
		return status;
	}

	IStatus rollback() {
		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);

		if (currentPhase != null) {
			try {
				status.add(rollBackPhase(currentPhase, currentActionRecords));
			} catch (RuntimeException e) {
				// "phase.undo" calls user code and might throw an unchecked exception
				// we catch the error here to gather information on where the problem occurred.
				status.add(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.phase_undo_error, currentPhase.getClass().getName()), e));
			} catch (LinkageError e) {
				// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
				status.add(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.phase_undo_error, currentPhase.getClass().getName()), e));
			}
			currentPhase = null;
			currentActionRecords = null;
			currentRecord = null;
		}

		for (ListIterator it = phaseActionRecordsPairs.listIterator(phaseActionRecordsPairs.size()); it.hasPrevious();) {
			Object[] pair = (Object[]) it.previous();
			Phase phase = (Phase) pair[0];
			List actionRecords = (List) pair[1];
			try {
				status.add(rollBackPhase(phase, actionRecords));
			} catch (RuntimeException e) {
				// "phase.undo" calls user code and might throw an unchecked exception
				// we catch the error here to gather information on where the problem occurred.
				status.add(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.phase_undo_error, phase.getClass().getName()), e));
			} catch (LinkageError e) {
				// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
				status.add(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.phase_undo_error, phase.getClass().getName()), e));
			}
		}

		phaseActionRecordsPairs.clear();
		for (Iterator iterator = touchpoints.iterator(); iterator.hasNext();) {
			try {
				Touchpoint touchpoint = (Touchpoint) iterator.next();
				status.add(touchpoint.commit(profile));
			} catch (RuntimeException e) {
				// "touchpoint.commit" calls user code and might throw an unchecked exception
				// we catch the error here to gather information on where the problem occurred.
				status.add(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.touchpoint_rollback_error, Touchpoint.class.getName()), e));
			} catch (LinkageError e) {
				// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
				status.add(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.touchpoint_rollback_error, Touchpoint.class.getName()), e));
			}
		}

		if (status.matches(IStatus.ERROR)) {
			MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.ERROR, NLS.bind(Messages.session_commit_error, profile.getProfileId()), null);
			result.merge(status);
			return result;
		}
		return status;
	}

	private IStatus rollBackPhase(Phase phase, List actionRecords) {
		MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);

		if (phase != currentPhase)
			phase.prePerform(result, this, profile, context, new NullProgressMonitor());

		for (ListIterator it = actionRecords.listIterator(actionRecords.size()); it.hasPrevious();) {
			ActionsRecord record = (ActionsRecord) it.previous();
			ProvisioningAction[] actions = (ProvisioningAction[]) record.actions.toArray(new ProvisioningAction[record.actions.size()]);
			phase.undo(result, this, profile, record.operand, actions, context);
		}
		phase.postPerform(result, profile, context, new NullProgressMonitor());
		return result;
	}

	void recordPhaseStart(Phase phase) {
		if (phase == null)
			throw new IllegalArgumentException(Messages.null_phase);

		if (currentPhase != null)
			throw new IllegalStateException(Messages.phase_started);

		currentPhase = phase;
		currentActionRecords = new ArrayList();
	}

	void recordPhaseEnd(Phase phase) {
		if (currentPhase == null)
			throw new IllegalStateException(Messages.phase_not_started);

		if (currentPhase != phase)
			throw new IllegalArgumentException(Messages.not_current_phase);

		phaseActionRecordsPairs.add(new Object[] {currentPhase, currentActionRecords});
		currentPhase = null;
		currentActionRecords = null;
	}

	void recordOperandStart(Operand operand) {
		if (operand == null)
			throw new IllegalArgumentException(Messages.null_operand);

		if (currentRecord != null)
			throw new IllegalStateException(Messages.operand_started);

		currentRecord = new ActionsRecord(operand);
		currentActionRecords.add(currentRecord);
	}

	void recordOperandEnd(Operand operand) {
		if (currentRecord == null)
			throw new IllegalStateException(Messages.operand_not_started);

		if (currentRecord.operand != operand)
			throw new IllegalArgumentException(Messages.not_current_operand);

		currentRecord = null;
	}

	void recordAction(ProvisioningAction action) {
		if (action == null)
			throw new IllegalArgumentException(Messages.null_action);

		currentRecord.actions.add(action);

		Touchpoint touchpoint = action.getTouchpoint();
		if (touchpoint != null)
			touchpoints.add(touchpoint);
	}

	public String getContextString() {
		return NLS.bind(Messages.session_context, new Object[] {profile.getProfileId(), getCurrentPhaseId(), getCurrentOperandId(), getCurrentActionId()});
	}

	private Object getCurrentActionId() {
		if (currentRecord == null || currentRecord.actions.isEmpty())
			return EMPTY_STRING;

		Object currentAction = currentRecord.actions.get(currentRecord.actions.size() - 1);
		return currentAction.getClass().getName();
	}

	private String getCurrentPhaseId() {
		if (currentPhase == null)
			return EMPTY_STRING;
		return currentPhase.getClass().getName();
	}

	private String getCurrentOperandId() {
		if (currentRecord == null)
			return EMPTY_STRING;
		return currentRecord.operand.toString();
	}
}
