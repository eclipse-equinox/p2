package org.eclipse.equinox.internal.p2.director;

import java.util.Arrays;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;

public abstract class Explanation implements Comparable<Explanation> {

	private static final String HARD_DEPENDENCY = "Hard Dependency";
	private static final String OPTIONAL_DEPENDENCY = "Optional Dependency";
	private static final String SINGLETON_CONSTRAINT = "Singleton Constraint";
	private static final String IU_TO_INSTALL = "IU to install";
	private static final String IU_INSTALLED = "IU already installed";
	private static final String IU_MISSING = "Missing Requirement";

	public static final int MISSING_REQUIREMENT = 1;
	public static final int VIOLATED_SINGLETON_CONSTRAINT = 2;

	private Explanation() {
		// no instance of that class for the moment
	}

	protected abstract int orderValue();

	public int shortAnswer() {
		throw new UnsupportedOperationException();
	}

	public static class IUToInstall extends Explanation {
		public final IInstallableUnit iu;

		public IUToInstall(IInstallableUnit iu) {
			this.iu = iu;
		}

		public String toString() {
			return IU_TO_INSTALL + ":" + iu;
		}

		@Override
		public int orderValue() {
			return 1;
		}
	}

	public static class IUInstalled extends Explanation {
		public final IInstallableUnit iu;

		public IUInstalled(IInstallableUnit iu) {
			this.iu = iu;
		}

		public String toString() {
			return IU_INSTALLED + ":" + iu;
		}

		@Override
		public int orderValue() {
			return 2;
		}
	}

	public static class MissingIU extends Explanation {
		public final IInstallableUnit iu;
		public final IRequiredCapability req;

		public MissingIU(IInstallableUnit iu, IRequiredCapability req) {
			this.iu = iu;
			this.req = req;
		}

		public String toString() {
			return IU_MISSING + ":" + iu + " missing required " + req;
		}

		@Override
		public int orderValue() {
			return 3;
		}

		@Override
		public int shortAnswer() {
			return MISSING_REQUIREMENT;
		}

	}

	public static class Singleton extends Explanation {
		public final IInstallableUnit[] ius;

		public Singleton(IInstallableUnit[] ius) {
			this.ius = ius;
		}

		public String toString() {
			return SINGLETON_CONSTRAINT + ":" + Arrays.asList(ius);
		}

		@Override
		public int orderValue() {
			return 4;
		}

		@Override
		public int shortAnswer() {
			return VIOLATED_SINGLETON_CONSTRAINT;
		}
	}

	public static class HardRequirement extends Explanation {
		public final IInstallableUnit iu;
		public final IRequiredCapability req;
		public final IInstallableUnitPatch patch;

		public HardRequirement(IInstallableUnit iu, IRequiredCapability req) {
			this.iu = iu;
			this.req = req;
			this.patch = null;
		}

		public HardRequirement(IInstallableUnit iu, IInstallableUnitPatch patch) {
			this.iu = iu;
			this.req = null;
			this.patch = patch;
		}

		public HardRequirement(IInstallableUnit iu, IRequiredCapability req, IInstallableUnitPatch patch) {
			this.iu = iu;
			this.req = req;
			this.patch = patch;
		}

		public String toString() {
			return HARD_DEPENDENCY + ":" + (patch == null ? "" : patch.toString() + " ") + iu + "-> " + req;
		}

		@Override
		public int orderValue() {
			return 5;
		}
	}

	public static final Explanation OPTIONAL_REQUIREMENT = new Explanation() {

		public String toString() {
			return OPTIONAL_DEPENDENCY;
		}

		@Override
		public int orderValue() {
			// TODO Auto-generated method stub
			return 6;
		}
	};

	public int compareTo(Explanation arg0) {
		if (this.orderValue() == arg0.orderValue()) {
			return this.toString().compareTo(arg0.toString());
		}
		return this.orderValue() - arg0.orderValue();
	}
}
