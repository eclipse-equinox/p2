package org.eclipse.equinox.internal.p2.director;

import java.util.Arrays;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;

public abstract class Explanation implements Comparable<Explanation> {

	public static final String HARD_DEPENDENCY = "Hard Dependency";
	public static final String OPTIONAL_DEPENDENCY = "Optional Dependency";
	public static final String SINGLETON_CONSTRAINT = "Singleton Constraint";
	public static final String IU_TO_INSTALL = "IU to install";
	public static final String IU_MISSING = "Missing Requirement";

	private Explanation() {
		// no instance of that class for the moment
	}

	protected abstract int orderValue();

	public static class IUToInstall extends Explanation {
		public final IRequiredCapability req;

		public IUToInstall(IRequiredCapability req) {
			this.req = req;
		}

		public String toString() {
			return IU_TO_INSTALL + ":" + req;
		}

		@Override
		public int orderValue() {
			return 1;
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
			return 2;
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
			return 3;
		}
	}

	public static class HardRequirement extends Explanation {
		public final IInstallableUnit iu;
		public final IRequiredCapability req;

		public HardRequirement(IInstallableUnit iu, IRequiredCapability req) {
			this.iu = iu;
			this.req = req;
		}

		public HardRequirement(IInstallableUnit iu, IInstallableUnitPatch path) {
			this.iu = iu;
			this.req = null;
		}

		public String toString() {
			return HARD_DEPENDENCY + ":" + iu + "-> " + req;
		}

		@Override
		public int orderValue() {
			return 4;
		}
	}

	public static Explanation OPTIONAL_REQUIREMENT = new Explanation() {

		public String toString() {
			return OPTIONAL_DEPENDENCY;
		}

		@Override
		public int orderValue() {
			// TODO Auto-generated method stub
			return 5;
		}
	};

	public int compareTo(Explanation arg0) {
		if (this.orderValue() == arg0.orderValue()) {
			return this.toString().compareTo(arg0.toString());
		}
		return this.orderValue() - arg0.orderValue();
	}
}
