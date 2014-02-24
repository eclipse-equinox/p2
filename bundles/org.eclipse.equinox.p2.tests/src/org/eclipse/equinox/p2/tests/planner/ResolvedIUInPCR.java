package org.eclipse.equinox.p2.tests.planner;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.metadata.ResolvedInstallableUnit;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.tests.*;

public class ResolvedIUInPCR extends AbstractProvisioningTest {

	@IUDescription(content = "package: iu1 \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit iu1;

	@IUDescription(content = "package: iu2 \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit iu2;

	private IProfile profile;

	protected void setUp() throws Exception {
		IULoader.loadIUs(this);
		profile = createProfile("ResolvedIUInPCR." + getName());
	}

	public void testNoResolvedIUInAddition() {
		ResolvedInstallableUnit riu1 = new ResolvedInstallableUnit(iu1);
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.add(riu1);

		assertFalse(identityContains(pcr.getAdditions(), riu1));
		assertTrue(identityContains(pcr.getAdditions(), iu1));
	}

	public void testNoResolvedIUInBulkAddition() {
		Collection<IInstallableUnit> riusToAdd = new ArrayList<IInstallableUnit>();
		ResolvedInstallableUnit riu1 = new ResolvedInstallableUnit(iu1);
		riusToAdd.add(riu1);

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addAll(riusToAdd);

		assertFalse(identityContains(pcr.getAdditions(), riu1));
		assertTrue(identityContains(pcr.getAdditions(), iu1));
	}

	public void testNoResolvedIUInBulkAddition2() {
		ResolvedInstallableUnit riu1 = new ResolvedInstallableUnit(iu1);
		ResolvedInstallableUnit riu2 = new ResolvedInstallableUnit(iu2);

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(riu1, riu2);

		assertFalse(identityContains(pcr.getAdditions(), riu1));
		assertFalse(identityContains(pcr.getAdditions(), riu2));

		assertTrue(identityContains(pcr.getAdditions(), iu1));
		assertTrue(identityContains(pcr.getAdditions(), iu2));
	}

	public void testNoResolvedIUInRemoval() {
		ResolvedInstallableUnit riu1 = new ResolvedInstallableUnit(iu1);
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.remove(riu1);

		assertFalse(identityContains(pcr.getRemovals(), riu1));
		assertTrue(identityContains(pcr.getRemovals(), iu1));
	}

	public void testNoResolvedIUInBulkRemoval() {
		Collection<IInstallableUnit> riusToAdd = new ArrayList<IInstallableUnit>();
		ResolvedInstallableUnit riu1 = new ResolvedInstallableUnit(iu1);
		riusToAdd.add(riu1);

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.removeAll(riusToAdd);

		assertFalse(identityContains(pcr.getRemovals(), riu1));
		assertTrue(identityContains(pcr.getRemovals(), iu1));
	}

	public void testNoResolvedIUInBulkRemoval2() {
		ResolvedInstallableUnit riu1 = new ResolvedInstallableUnit(iu1);
		ResolvedInstallableUnit riu2 = new ResolvedInstallableUnit(iu2);

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.removeInstallableUnits(new IInstallableUnit[] {riu1, riu2});

		assertFalse(identityContains(pcr.getRemovals(), riu1));
		assertFalse(identityContains(pcr.getRemovals(), riu2));

		assertTrue(identityContains(pcr.getRemovals(), iu1));
		assertTrue(identityContains(pcr.getRemovals(), iu2));
	}

	public void testNoResolvedIUInstallableUnitInclusionRules() {
		ResolvedInstallableUnit riu1 = new ResolvedInstallableUnit(iu1);
		ResolvedInstallableUnit riu2 = new ResolvedInstallableUnit(iu2);

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.setInstallableUnitInclusionRules(riu1, "inclusion");
		pcr.setInstallableUnitProfileProperty(riu2, "a", "b");

		assertFalse(identityContains(pcr.getInstallableUnitProfilePropertiesToAdd().keySet(), riu1));
		assertFalse(identityContains(pcr.getInstallableUnitProfilePropertiesToAdd().keySet(), riu2));

		assertTrue(identityContains(pcr.getInstallableUnitProfilePropertiesToAdd().keySet(), iu1));
		assertTrue(identityContains(pcr.getInstallableUnitProfilePropertiesToAdd().keySet(), iu2));
	}

	public void testNoResolvedIUInstallableUnitInclusionRules2() {
		ResolvedInstallableUnit riu1 = new ResolvedInstallableUnit(iu1);
		ResolvedInstallableUnit riu2 = new ResolvedInstallableUnit(iu2);

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.removeInstallableUnitInclusionRules(riu1);
		pcr.removeInstallableUnitProfileProperty(riu2, "a");

		assertFalse(identityContains(pcr.getInstallableUnitProfilePropertiesToRemove().keySet(), riu1));
		assertFalse(identityContains(pcr.getInstallableUnitProfilePropertiesToRemove().keySet(), riu2));

		assertTrue(identityContains(pcr.getInstallableUnitProfilePropertiesToRemove().keySet(), iu1));
		assertTrue(identityContains(pcr.getInstallableUnitProfilePropertiesToRemove().keySet(), iu2));
	}

	private boolean identityContains(Collection<IInstallableUnit> ius, IInstallableUnit match) {
		for (IInstallableUnit iu : ius) {
			if (iu == match)
				return true;
		}
		return false;
	}
}
