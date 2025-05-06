/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.phases.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class PhaseSet implements IPhaseSet {

	private static final Set<String> SUPPORTED_PHASES = Set.of(PhaseSetFactory.PHASE_COLLECT,
			PhaseSetFactory.PHASE_UNCONFIGURE, PhaseSetFactory.PHASE_UNINSTALL, PhaseSetFactory.PHASE_PROPERTY,
			PhaseSetFactory.PHASE_CHECK_TRUST, PhaseSetFactory.PHASE_INSTALL, PhaseSetFactory.PHASE_CONFIGURE);

	private Phase[] phases;
	private boolean isRunning = false;
	private boolean isPaused = false;
	private String[] phaseIds;

	public PhaseSet(Phase[] phases) {
		if (phases == null) {
			throw new IllegalArgumentException(Messages.null_phases);
		}
		this.phases = phases;
	}

	public PhaseSet(String[] phases) {
		if (phases == null) {
			throw new IllegalArgumentException(Messages.null_phases);
		}
		this.phaseIds = Arrays.stream(phases).filter(SUPPORTED_PHASES::contains).toArray(String[]::new);
	}

	public final MultiStatus perform(EngineSession session, Operand[] operands, IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		Phase[] array = getPhases(session.getAgent());
		int[] weights = getProgressWeights(operands, array);
		int totalWork = getTotalWork(weights);
		SubMonitor pm = SubMonitor.convert(monitor, totalWork);
		try {
			isRunning = true;
			for (int i = 0; i < array.length; i++) {
				if (pm.isCanceled()) {
					status.add(Status.CANCEL_STATUS);
					return status;
				}
				Phase phase = array[i];
				phase.actionManager = session.getAgent().getService(ActionManager.class);
				try {
					phase.perform(status, session, operands, pm.newChild(weights[i]));
				} catch (OperationCanceledException e) {
					// propagate operation cancellation
					status.add(new Status(IStatus.CANCEL, EngineActivator.ID, e.getMessage(), e));
				} catch (RuntimeException e) {
					// "perform" calls user code and might throw an unchecked exception
					// we catch the error here to gather information on where the problem occurred.
					status.add(new Status(IStatus.ERROR, EngineActivator.ID, e.getMessage(), e));
				} catch (LinkageError e) {
					// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
					status.add(new Status(IStatus.ERROR, EngineActivator.ID, e.getMessage(), e));
				} finally {
					phase.actionManager = null;
				}
				if (status.matches(IStatus.CANCEL)) {
					MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.CANCEL, Messages.Engine_Operation_Canceled_By_User, null);
					result.merge(status);
					return result;
				} else if (status.matches(IStatus.ERROR)) {
					MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.ERROR, phase.getProblemMessage(), null);
					result.add(new Status(IStatus.ERROR, EngineActivator.ID, session.getContextString(), null));
					result.merge(status);
					return result;
				}
			}
		} finally {
			pm.done();
			isRunning = false;
		}
		return status;
	}

	public synchronized boolean pause() {
		if (isRunning && !isPaused && this.phases != null) {
			isPaused = true;
			for (Phase phase : this.phases) {
				phase.setPaused(isPaused);
			}
			return true;
		}
		return false;
	}

	public synchronized boolean resume() {
		if (isRunning && isPaused && this.phases != null) {
			isPaused = false;
			for (Phase phase : this.phases) {
				phase.setPaused(isPaused);
			}
			return true;
		}
		return false;
	}

	public final IStatus validate(ActionManager actionManager, IProfile profile, Operand[] operands,
			ProvisioningContext context, IProvisioningAgent agent, IProgressMonitor monitor) {
		Set<MissingAction> missingActions = new HashSet<>();
		for (Phase phase2 : getPhases(agent)) {
			Phase phase = phase2;
			phase.actionManager = actionManager;
			try {
				for (Operand operand : operands) {
					try {
						if (!phase.isApplicable(operand)) {
							continue;
						}

						List<ProvisioningAction> actions = phase.getActions(operand);
						if (actions == null) {
							continue;
						}
						for (ProvisioningAction action : actions) {
							if (action instanceof MissingAction) {
								missingActions.add((MissingAction) action);
							}
						}
					} catch (RuntimeException e) {
						// "perform" calls user code and might throw an unchecked exception
						// we catch the error here to gather information on where the problem occurred.
						return new Status(IStatus.ERROR, EngineActivator.ID, e.getMessage() + " " + getContextString(profile, phase, operand), e); //$NON-NLS-1$
					} catch (LinkageError e) {
						// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
						return new Status(IStatus.ERROR, EngineActivator.ID, e.getMessage() + " " + getContextString(profile, phase, operand), e); //$NON-NLS-1$
					}
				}
			} finally {
				phase.actionManager = null;
			}
		}
		if (!missingActions.isEmpty()) {
			MissingAction[] missingActionsArray = missingActions.toArray(new MissingAction[missingActions.size()]);
			MissingActionsException exception = new MissingActionsException(missingActionsArray);
			return (new Status(IStatus.ERROR, EngineActivator.ID, exception.getMessage(), exception));
		}
		return Status.OK_STATUS;
	}

	private String getContextString(IProfile profile, Phase phase, Operand operand) {
		return NLS.bind(Messages.session_context, profile.getProfileId(), phase.getClass().getName(), operand.toString(), ""); //$NON-NLS-1$
	}

	private int getTotalWork(int[] weights) {
		int sum = 0;
		for (int weight : weights) {
			sum += weight;
		}
		return sum;
	}

	private int[] getProgressWeights(Operand[] operands, Phase[] array) {
		int[] weights = new int[array.length];
		for (int i = 0; i < array.length; i += 1) {
			if (operands.length > 0) {
				//alter weights according to the number of operands applicable to that phase
				weights[i] = (array[i].weight * countApplicable(array[i], operands) / operands.length);
			} else {
				weights[i] = array[i].weight;
			}
		}
		return weights;
	}

	private int countApplicable(Phase phase, Operand[] operands) {
		int count = 0;
		for (Operand operand : operands) {
			if (phase.isApplicable(operand)) {
				count++;
			}
		}
		return count;
	}

	@Override
	public String[] getPhaseIds() {
		if (phaseIds != null) {
			return phaseIds.clone();
		}
		if (phases != null) {
			return Arrays.stream(phases).map(p -> p.phaseId).toArray(String[]::new);
		}
		return new String[0];
	}

	private Phase[] getPhases(IProvisioningAgent agent) {
		if (phases == null) {
			boolean forcedUninstall = Boolean
					.parseBoolean(EngineActivator.getProperty("org.eclipse.equinox.p2.engine.forcedUninstall", agent)); //$NON-NLS-1$
			List<String> includeList = Arrays.asList(phaseIds);
			List<Phase> list = new ArrayList<>();
			if (includeList.contains(PhaseSetFactory.PHASE_COLLECT)) {
				list.add(new Collect(100));
			}
			if (includeList.contains(PhaseSetFactory.PHASE_CHECK_TRUST)) {
				list.add(new CheckTrust(10));
			}
			if (includeList.contains(PhaseSetFactory.PHASE_UNCONFIGURE)) {
				list.add(new Unconfigure(10, forcedUninstall));
			}
			if (includeList.contains(PhaseSetFactory.PHASE_UNINSTALL)) {
				list.add(new Uninstall(50, forcedUninstall));
			}
			if (includeList.contains(PhaseSetFactory.PHASE_PROPERTY)) {
				list.add(new Property(1));
			}
			if (includeList.contains(PhaseSetFactory.PHASE_INSTALL)) {
				list.add(new Install(50));
			}
			if (includeList.contains(PhaseSetFactory.PHASE_CONFIGURE)) {
				list.add(new Configure(10));
			}
			this.phases = list.toArray(Phase[]::new);
		}
		return phases;
	}
}
