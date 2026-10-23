/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.operations.AffectedIUStatus;
import org.eclipse.equinox.internal.p2.operations.IStatusCodes;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerStatus;
import org.eclipse.equinox.internal.provisional.p2.director.RequestStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.osgi.util.NLS;

public final class ResolutionProblems {

	/**
	 * The kinds of groups, in the order in which they are presented.
	 */
	enum Kind {
		MISSING_REQUIREMENTS, NOT_INSTALLABLE, CONFLICTS, DEPENDENCIES, TO_INSTALL, INSTALLED, UPDATED, IGNORED, OTHER
	}

	/**
	 * A single entry of a group.
	 * <p>
	 * This is deliberately not a record: viewers identify their elements by
	 * <code>equals</code>, and two entries can legitimately look the same (for
	 * example two versions of the same unit in a conflict).
	 * </p>
	 */
	public static final class Problem {
		private final String name;
		private final String message;
		private final int severity;

		Problem(String name, String message, int severity) {
			this.name = name;
			this.message = message;
			this.severity = severity;
		}

		public String getName() {
			return name;
		}

		public String getMessage() {
			return message;
		}

		public int getSeverity() {
			return severity;
		}
	}

	/**
	 * A group of problems of the same {@link Kind}.
	 */
	public static final class Group {
		private final Kind kind;
		private final List<Problem> problems = new ArrayList<>();

		Group(Kind kind) {
			this.kind = kind;
		}

		public String getTitle() {
			Integer count = Integer.valueOf(problems.size());
			return NLS.bind(switch (kind) {
				case MISSING_REQUIREMENTS -> ProvUIMessages.ResolutionProblems_GroupMissingRequirements;
				case NOT_INSTALLABLE -> ProvUIMessages.ResolutionProblems_GroupNotInstallable;
				case CONFLICTS -> ProvUIMessages.ResolutionProblems_GroupConflicts;
				case DEPENDENCIES -> ProvUIMessages.ResolutionProblems_GroupDependencies;
				case TO_INSTALL -> ProvUIMessages.ResolutionProblems_GroupToInstall;
				case INSTALLED -> ProvUIMessages.ResolutionProblems_GroupInstalled;
				case UPDATED -> ProvUIMessages.ResolutionProblems_GroupUpdated;
				case IGNORED -> ProvUIMessages.ResolutionProblems_GroupIgnored;
				case OTHER -> ProvUIMessages.ResolutionProblems_GroupOther;
			}, count);
		}

		public List<Problem> getProblems() {
			return problems;
		}

		/**
		 * Returns the highest {@link IStatus#getSeverity() severity} of the entries.
		 */
		public int getSeverity() {
			int severity = IStatus.OK;
			for (Problem problem : problems) {
				severity = Math.max(severity, problem.getSeverity());
			}
			return severity;
		}
	}

	public static List<Group> of(IStatus resolutionResult) {
		Map<Kind, Group> groups = new EnumMap<>(Kind.class);
		if (resolutionResult != null) {
			collect(resolutionResult, groups);
		}
		return new ArrayList<>(groups.values());
	}

	private static void collect(IStatus status, Map<Kind, Group> groups) {
		if (status instanceof PlannerStatus plannerStatus) {
			RequestStatus requestStatus = plannerStatus.getRequestStatus();
			Set<Explanation> explanations = requestStatus == null ? null : requestStatus.getExplanations();
			if (explanations != null && !explanations.isEmpty()) {
				for (Explanation explanation : explanations) {
					add(explanation, groups);
				}
				return;
			}
		}
		if (status.isMultiStatus()) {
			for (IStatus child : status.getChildren()) {
				collect(child, groups);
			}
			return;
		}
		if (!status.isOK()) {
			add(status, groups);
		}
	}

	private static void add(IStatus status, Map<Kind, Group> groups) {
		String text = status.getMessage();
		if (status instanceof AffectedIUStatus affected) {
			String name = affected.getIUName();
			int severity = status.getSeverity();
			switch (status.getCode()) {
				case IStatusCodes.ALTERED_IMPLIED_UPDATE ->
					add(groups, Kind.UPDATED, name, ProvUIMessages.ResolutionProblems_UpdatedInstead, severity);
				case IStatusCodes.ALTERED_IGNORED_IMPLIED_DOWNGRADE ->
					add(groups, Kind.IGNORED, name, ProvUIMessages.ResolutionProblems_NewerInstalled, severity);
				case IStatusCodes.ALTERED_IGNORED_ALREADY_INSTALLED ->
					add(groups, Kind.IGNORED, name, ProvUIMessages.ResolutionProblems_AlreadyInstalled, severity);
				default ->
					add(groups, Kind.OTHER, name, text, severity);
			}
			return;
		}
		add(groups, Kind.OTHER, "", text, status.getSeverity()); //$NON-NLS-1$
	}

	private static void add(Explanation explanation, Map<Kind, Group> groups) {
		String text = explanation.toString();
		if (explanation instanceof Explanation.MissingIU missing) {
			add(groups, Kind.MISSING_REQUIREMENTS, missing.isEntryPoint ? getName(missing.req) : getName(missing.iu), text, IStatus.ERROR);
		} else if (explanation instanceof Explanation.NotInstallableRoot root) {
			add(groups, Kind.NOT_INSTALLABLE, getName(root.req), text, IStatus.ERROR);
		} else if (explanation instanceof Explanation.HardRequirement hard) {
			add(groups, Kind.DEPENDENCIES, getName(hard.iu), text, IStatus.ERROR);
		} else if (explanation instanceof Explanation.Singleton singleton) {
			for (IInstallableUnit iu : singleton.ius) {
				add(groups, Kind.CONFLICTS, getName(iu), text, IStatus.ERROR);
			}
		} else if (explanation instanceof Explanation.IUToInstall toInstall) {
			add(groups, Kind.TO_INSTALL, getName(toInstall.iu), text, IStatus.INFO);
		} else if (explanation instanceof Explanation.IUInstalled installed) {
			add(groups, Kind.INSTALLED, getName(installed.iu), text, IStatus.INFO);
		} else {
			add(groups, Kind.OTHER, "", text, IStatus.ERROR); //$NON-NLS-1$
		}
	}

	private static void add(Map<Kind, Group> groups, Kind kind, String name, String message, int severity) {
		groups.computeIfAbsent(kind, Group::new).problems.add(new Problem(name, message, severity));
	}

	/**
	 * Returns the name of the unit as it is shown elsewhere in the user interface:
	 * the name in the default locale, or the id if the unit has no name.
	 */
	private static String getName(IInstallableUnit iu) {
		if (iu == null) {
			return ""; //$NON-NLS-1$
		}
		String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
		return name != null ? name : iu.getId();
	}

	/**
	 * Returns a short description of what is required. For a requirement of a unit
	 * this is the id of that unit, without the version range, which is part of the
	 * message.
	 */
	private static String getName(IRequirement requirement) {
		if (requirement instanceof IRequiredCapability capability && IInstallableUnit.NAMESPACE_IU_ID.equals(capability.getNamespace())) {
			return capability.getName();
		}
		return String.valueOf(requirement);
	}
}
