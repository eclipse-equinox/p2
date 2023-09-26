/*******************************************************************************
 * Copyright (c) 2009, 2018 Daniel Le Berre and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Daniel Le Berre - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.Arrays;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.osgi.util.NLS;

public abstract class Explanation implements Comparable<Explanation> {

	public static class PatchedHardRequirement extends Explanation {
		public final IInstallableUnit iu;
		public final IInstallableUnitPatch patch;
		public final IRequirement req;

		public PatchedHardRequirement(IInstallableUnit iu, IInstallableUnitPatch patch) {
			this.iu = iu;
			this.req = null;
			this.patch = patch;
		}

		public PatchedHardRequirement(IInstallableUnit iu, IRequirement req, IInstallableUnitPatch patch) {
			this.iu = iu;
			this.req = req;
			this.patch = patch;
		}

		@Override
		public int orderValue() {
			return 6;
		}

		@Override
		public IStatus toStatus() {
			MultiStatus result = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, Messages.Explanation_unsatisfied, null);
			final String fromString = patch.toString() + ' ' + getUserReadableName(iu);
			result.add(Status.error(NLS.bind(Messages.Explanation_fromPatch, fromString)));
			result.add(Status.error(NLS.bind(Messages.Explanation_to, req)));
			return result;
		}

		@Override
		public String toString() {
			return NLS.bind(Messages.Explanation_patchedHardDependency, new Object[] {patch, iu, req});
		}

		@Override
		public int shortAnswer() {
			return Explanation.VIOLATED_PATCHED_HARD_REQUIREMENT;
		}

	}

	public static class HardRequirement extends Explanation {
		public final IInstallableUnit iu;
		public final IRequirement req;

		public HardRequirement(IInstallableUnit iu, IRequirement req) {
			this.iu = iu;
			this.req = req;
		}

		@Override
		public int orderValue() {
			return 5;
		}

		@Override
		public IStatus toStatus() {
			MultiStatus result = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, Messages.Explanation_unsatisfied, null);
			result.add(Status.error(NLS.bind(Messages.Explanation_from, getUserReadableName(iu))));
			result.add(Status.error(NLS.bind(Messages.Explanation_to, req)));
			return result;
		}

		@Override
		public String toString() {
			return NLS.bind(req.getMax() == 0 ? Messages.Explanation_hardDependencyNegative
					: Messages.Explanation_hardDependency, iu, req);
		}

		@Override
		public int shortAnswer() {
			return VIOLATED_HARD_REQUIREMENT;
		}
	}

	public static class IUInstalled extends Explanation {
		public final IInstallableUnit iu;

		public IUInstalled(IInstallableUnit iu) {
			this.iu = iu;
		}

		@Override
		public int orderValue() {
			return 2;
		}

		@Override
		public String toString() {
			return NLS.bind(Messages.Explanation_alreadyInstalled, iu);
		}

		@Override
		public IStatus toStatus() {
			return Status.error(NLS.bind(Messages.Explanation_alreadyInstalled, getUserReadableName(iu)));
		}

		@Override
		public int shortAnswer() {
			return IU_INSTALLED;
		}
	}

	public static class IUToInstall extends Explanation {
		public final IInstallableUnit iu;

		public IUToInstall(IInstallableUnit iu) {
			this.iu = iu;
		}

		@Override
		public int orderValue() {
			return 1;
		}

		@Override
		public String toString() {
			return NLS.bind(Messages.Explanation_toInstall, iu);
		}

		@Override
		public IStatus toStatus() {
			return Status.error(NLS.bind(Messages.Explanation_toInstall, getUserReadableName(iu)));
		}

		@Override
		public int shortAnswer() {
			return IU_TO_INSTALL;
		}
	}

	public static class NotInstallableRoot extends Explanation {
		public final IRequirement req;

		public NotInstallableRoot(IRequirement req) {
			this.req = req;
		}

		@Override
		public String toString() {
			return NLS.bind(Messages.Explanation_missingRootFilter, req);
		}

		@Override
		public IStatus toStatus() {
			return Status.error(NLS.bind(Messages.Explanation_missingRootFilter, req));
		}

		@Override
		protected int orderValue() {
			return 2;
		}

		@Override
		public int shortAnswer() {
			return NON_INSTALLABLE_ROOT;
		}
	}

	public static class MissingIU extends Explanation {
		public final IInstallableUnit iu;
		public final IRequirement req;
		public boolean isEntryPoint;

		public MissingIU(IInstallableUnit iu, IRequirement req, boolean isEntryPoint) {
			this.iu = iu;
			this.req = req;
			this.isEntryPoint = isEntryPoint;
		}

		@Override
		public int orderValue() {
			return 3;
		}

		@Override
		public int shortAnswer() {
			return MISSING_REQUIREMENT;
		}

		@Override
		public String toString() {
			if (isEntryPoint) {
				return NLS.bind(Messages.Explanation_missingRootRequired, req);
			}
			if (req.getFilter() == null) {
				return NLS.bind(Messages.Explanation_missingRequired, iu, req);
			}
			return NLS.bind(Messages.Explanation_missingRequiredFilter, new Object[] {req.getFilter(), iu, req});
		}

		@Override
		public IStatus toStatus() {
			if (isEntryPoint) {
				return Status.error(NLS.bind(Messages.Explanation_missingRootRequired, req));
			}
			if (req.getFilter() == null) {
				return Status.error(NLS.bind(Messages.Explanation_missingRequired, getUserReadableName(iu), req));
			}
			return Status.error(NLS.bind(Messages.Explanation_missingRequiredFilter,
					new Object[] { req.getFilter(), getUserReadableName(iu), req }));
		}
	}

	public static class MissingGreedyIU extends Explanation {
		public final IInstallableUnit iu;

		public MissingGreedyIU(IInstallableUnit iu) {
			this.iu = iu;
		}

		@Override
		public int orderValue() {
			return 3;
		}

		@Override
		public int shortAnswer() {
			return MISSING_REQUIREMENT;
		}

		@Override
		public String toString() {
			return NLS.bind(Messages.Explanation_missingNonGreedyRequired, iu);
		}

		@Override
		public IStatus toStatus() {
			return Status.error(NLS.bind(Messages.Explanation_missingNonGreedyRequired, getUserReadableName(iu)));
		}
	}

	public static class Singleton extends Explanation {
		public final IInstallableUnit[] ius;

		public Singleton(IInstallableUnit[] ius) {
			this.ius = ius;
		}

		@Override
		public int orderValue() {
			return 4;
		}

		@Override
		public int shortAnswer() {
			return VIOLATED_SINGLETON_CONSTRAINT;
		}

		@Override
		public IStatus toStatus() {
			MultiStatus result = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, NLS.bind(Messages.Explanation_singleton, ""), null); //$NON-NLS-1$
			for (IInstallableUnit iu : ius)
				result.add(Status.error(getUserReadableName(iu)));
			return result;
		}

		@Override
		public String toString() {
			return NLS.bind(Messages.Explanation_singleton, Arrays.asList(ius));
		}

	}

	public static final Explanation OPTIONAL_REQUIREMENT = new Explanation() {

		@Override
		public int orderValue() {
			return 6;
		}

		@Override
		public String toString() {
			return Messages.Explanation_optionalDependency;
		}

		@Override
		public int shortAnswer() {
			return OTHER_REASON;
		}
	};

	public static final int MISSING_REQUIREMENT = 1;
	public static final int VIOLATED_SINGLETON_CONSTRAINT = 2;
	public static final int IU_INSTALLED = 3;
	public static final int IU_TO_INSTALL = 4;
	public static final int VIOLATED_HARD_REQUIREMENT = 5;
	public static final int VIOLATED_PATCHED_HARD_REQUIREMENT = 6;
	public static final int NON_INSTALLABLE_ROOT = 7;
	public static final int OTHER_REASON = 100;

	protected Explanation() {
		super();
	}

	@Override
	public int compareTo(Explanation exp) {
		if (this.orderValue() == exp.orderValue()) {
			return this.toString().compareTo(exp.toString());
		}
		return this.orderValue() - exp.orderValue();
	}

	protected abstract int orderValue();

	abstract public int shortAnswer();

	/**
	 * Returns a representation of this explanation as a status object.
	 */
	public IStatus toStatus() {
		return Status.error(toString());
	}

	protected static String getUserReadableName(IInstallableUnit iu) {
		if (iu == null)
			return ""; //$NON-NLS-1$
		String result = getLocalized(iu);
		if (result == null)
			return iu.toString();
		return result + ' ' + iu.getVersion() + " (" + iu.toString() + ')'; //$NON-NLS-1$
	}

	private static String getLocalized(IInstallableUnit iu) {
		String value = iu.getProperty(IInstallableUnit.PROP_NAME);
		if (value == null || value.length() <= 1 || value.charAt(0) != '%')
			return value;
		final String actualKey = value.substring(1); // Strip off the %
		return iu.getProperty("df_LT." + actualKey); //$NON-NLS-1$
	}
}
