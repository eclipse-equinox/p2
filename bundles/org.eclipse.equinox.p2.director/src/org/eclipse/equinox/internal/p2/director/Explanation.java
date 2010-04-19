/*******************************************************************************
 * Copyright (c) 2009 Daniel Le Berre and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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

		public int orderValue() {
			return 6;
		}

		public IStatus toStatus() {
			MultiStatus result = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, Messages.Explanation_unsatisfied, null);
			final String fromString = patch.toString() + ' ' + getUserReadableName(iu);
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Explanation_fromPatch, fromString)));
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Explanation_to, req)));
			return result;
		}

		public String toString() {
			return NLS.bind(Messages.Explanation_patchedHardDependency, new Object[] {patch, iu, req});
		}
	}

	public static class HardRequirement extends Explanation {
		public final IInstallableUnit iu;
		public final IRequirement req;

		public HardRequirement(IInstallableUnit iu, IRequirement req) {
			this.iu = iu;
			this.req = req;
		}

		public int orderValue() {
			return 5;
		}

		public IStatus toStatus() {
			MultiStatus result = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, Messages.Explanation_unsatisfied, null);
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Explanation_from, getUserReadableName(iu))));
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Explanation_to, req)));
			return result;
		}

		public String toString() {
			return NLS.bind(Messages.Explanation_hardDependency, iu, req);
		}
	}

	public static class IUInstalled extends Explanation {
		public final IInstallableUnit iu;

		public IUInstalled(IInstallableUnit iu) {
			this.iu = iu;
		}

		public int orderValue() {
			return 2;
		}

		public String toString() {
			return NLS.bind(Messages.Explanation_alreadyInstalled, iu);
		}

		public IStatus toStatus() {
			return new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Explanation_alreadyInstalled, getUserReadableName(iu)));
		}
	}

	public static class IUToInstall extends Explanation {
		public final IInstallableUnit iu;

		public IUToInstall(IInstallableUnit iu) {
			this.iu = iu;
		}

		public int orderValue() {
			return 1;
		}

		public String toString() {
			return NLS.bind(Messages.Explanation_toInstall, iu);
		}

		public IStatus toStatus() {
			return new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Explanation_toInstall, getUserReadableName(iu)));
		}
	}

	public static class MissingIU extends Explanation {
		public final IInstallableUnit iu;
		public final IRequirement req;

		public MissingIU(IInstallableUnit iu, IRequirement req) {
			this.iu = iu;
			this.req = req;
		}

		public int orderValue() {
			return 3;
		}

		public int shortAnswer() {
			return MISSING_REQUIREMENT;
		}

		public String toString() {
			if (req.getFilter() == null) {
				return NLS.bind(Messages.Explanation_missingRequired, iu, req);
			}
			return NLS.bind(Messages.Explanation_missingRequiredFilter, new Object[] {req.getFilter(), iu, req});
		}

		public IStatus toStatus() {
			if (req.getFilter() == null) {
				return new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Explanation_missingRequired, getUserReadableName(iu), req));
			}
			return new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Explanation_missingRequiredFilter, new Object[] {req.getFilter(), getUserReadableName(iu), req}));
		}
	}

	public static class MissingGreedyIU extends Explanation {
		public final IInstallableUnit iu;

		public MissingGreedyIU(IInstallableUnit iu) {
			this.iu = iu;
		}

		public int orderValue() {
			return 3;
		}

		public int shortAnswer() {
			return MISSING_REQUIREMENT;
		}

		public String toString() {
			return NLS.bind(Messages.Explanation_missingNonGreedyRequired, iu);
		}

		public IStatus toStatus() {
			return new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Explanation_missingNonGreedyRequired, getUserReadableName(iu)));
		}
	}

	public static class Singleton extends Explanation {
		public final IInstallableUnit[] ius;

		public Singleton(IInstallableUnit[] ius) {
			this.ius = ius;
		}

		public int orderValue() {
			return 4;
		}

		public int shortAnswer() {
			return VIOLATED_SINGLETON_CONSTRAINT;
		}

		public IStatus toStatus() {
			MultiStatus result = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, NLS.bind(Messages.Explanation_singleton, ""), null); //$NON-NLS-1$
			for (int i = 0; i < ius.length; i++)
				result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, getUserReadableName(ius[i])));
			return result;
		}

		public String toString() {
			return NLS.bind(Messages.Explanation_singleton, Arrays.asList(ius));
		}

	}

	public static final int MISSING_REQUIREMENT = 1;

	public static final Explanation OPTIONAL_REQUIREMENT = new Explanation() {

		public int orderValue() {
			return 6;
		}

		public String toString() {
			return Messages.Explanation_optionalDependency;
		}
	};

	public static final int VIOLATED_SINGLETON_CONSTRAINT = 2;

	protected Explanation() {
		super();
	}

	public int compareTo(Explanation exp) {
		if (this.orderValue() == exp.orderValue()) {
			return this.toString().compareTo(exp.toString());
		}
		return this.orderValue() - exp.orderValue();
	}

	protected abstract int orderValue();

	public int shortAnswer() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns a representation of this explanation as a status object.
	 */
	public IStatus toStatus() {
		return new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, toString());
	}

	protected String getUserReadableName(IInstallableUnit iu) {
		if (iu == null)
			return ""; //$NON-NLS-1$
		String result = getLocalized(iu);
		if (result == null)
			return iu.toString();
		return result + ' ' + iu.getVersion() + " (" + iu.toString() + ')'; //$NON-NLS-1$
	}

	private String getLocalized(IInstallableUnit iu) {
		String value = iu.getProperty(IInstallableUnit.PROP_NAME);
		if (value == null || value.length() <= 1 || value.charAt(0) != '%')
			return value;
		final String actualKey = value.substring(1); // Strip off the %
		return iu.getProperty("df_LT." + actualKey); //$NON-NLS-1$
	}
}
