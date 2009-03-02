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
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.osgi.util.NLS;

public abstract class Explanation implements Comparable {

	public static class HardRequirement extends Explanation {
		public final IInstallableUnit iu;
		public final IInstallableUnitPatch patch;
		public final IRequiredCapability req;

		public HardRequirement(IInstallableUnit iu, IInstallableUnitPatch patch) {
			this.iu = iu;
			this.req = null;
			this.patch = patch;
		}

		public HardRequirement(IInstallableUnit iu, IRequiredCapability req) {
			this.iu = iu;
			this.req = req;
			this.patch = null;
		}

		public HardRequirement(IInstallableUnit iu, IRequiredCapability req, IInstallableUnitPatch patch) {
			this.iu = iu;
			this.req = req;
			this.patch = patch;
		}

		public int orderValue() {
			return 5;
		}

		public IStatus toStatus() {
			MultiStatus result = new MultiStatus(DirectorActivator.PI_DIRECTOR, 1, Messages.Explanation_unsatisfied, null);
			final String fromString = (patch == null ? "" : patch.toString() + ' ') + iu;//$NON-NLS-1$
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Explanation_from, fromString)));
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Explanation_to, req)));
			return result;
		}

		public String toString() {
			final String fromString = (patch == null ? "" : patch.toString() + ' ') + iu;//$NON-NLS-1$
			return NLS.bind(Messages.Explanation_hardDependency, fromString, req);
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
	}

	public static class MissingIU extends Explanation {
		public final IInstallableUnit iu;
		public final IRequiredCapability req;

		public MissingIU(IInstallableUnit iu, IRequiredCapability req) {
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
			return NLS.bind(Messages.Explanation_missingRequired, iu, req);
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
				result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, ius[i].toString()));
			return result;
		}

		public String toString() {
			return NLS.bind(Messages.Explanation_singleton, Arrays.asList(ius));
		}

	}

	public static final int MISSING_REQUIREMENT = 1;

	public static final Explanation OPTIONAL_REQUIREMENT = new Explanation() {

		public int orderValue() {
			// TODO Auto-generated method stub
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

	public int compareTo(Object arg0) {
		Explanation exp = (Explanation) arg0;
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
}
