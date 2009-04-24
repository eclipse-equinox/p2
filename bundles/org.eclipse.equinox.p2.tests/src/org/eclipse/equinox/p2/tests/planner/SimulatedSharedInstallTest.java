package org.eclipse.equinox.p2.tests.planner;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimulatedSharedInstallTest extends AbstractProvisioningTest {

	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnit c1;

	IPlanner planner;
	IProfile profile;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.parseVersion("1.0.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.0.0]"), null));
		b1 = createIU("B", Version.parseVersion("1.0.0"));
		// Note: C has an "optional" dependency on "B"
		c1 = createIU("C", Version.parseVersion("1.0.0"), new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.0.0]"), null, true, false)});
		profile = createProfile(SimulatedSharedInstallTest.class.getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testRemoveUnresolvedIU() {
		assertEquals(IStatus.OK, engine.perform(profile, new DefaultPhaseSet(), new Operand[] {new InstallableUnitOperand(null, a1), new InstallableUnitPropertyOperand(a1, "org.eclipse.equinox.p2.internal.inclusion.rules", null, "STRICT")}, new ProvisioningContext(new URI[0]), new NullProgressMonitor()).getSeverity());
		assertTrue(profile.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection().contains(a1));

		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.removeInstallableUnits(new IInstallableUnit[] {a1});
		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertEquals(IStatus.OK, PlanExecutionHelper.executePlan(plan, engine, new ProvisioningContext(new URI[0]), new NullProgressMonitor()).getSeverity());
		assertFalse(profile.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection().contains(a1));
	}

	public void testAvailableVsQueryInProfile() {
		assertEquals(IStatus.OK, engine.perform(profile, new DefaultPhaseSet(), new Operand[] {new InstallableUnitOperand(null, c1), new InstallableUnitPropertyOperand(c1, "org.eclipse.equinox.p2.internal.inclusion.rules", null, "STRICT")}, new ProvisioningContext(new URI[0]), new NullProgressMonitor()).getSeverity());
		assertTrue(profile.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection().contains(c1));

		IProfile availableWrapper = new IProfile() {
			public Collector available(Query query, Collector collector, IProgressMonitor monitor) {
				profile.query(query, collector, monitor);

				Collection ius = new ArrayList();
				ius.add(b1);
				return query.perform(ius.iterator(), collector);
			}

			// everything else is delegated
			public Map getInstallableUnitProperties(IInstallableUnit iu) {
				return profile.getInstallableUnitProperties(iu);
			}

			public String getInstallableUnitProperty(IInstallableUnit iu, String key) {
				return profile.getInstallableUnitProperty(iu, key);
			}

			public Map getLocalProperties() {
				return profile.getLocalProperties();
			}

			public String getLocalProperty(String key) {
				return profile.getLocalProperty(key);
			}

			public IProfile getParentProfile() {
				return profile.getParentProfile();
			}

			public String getProfileId() {
				return profile.getProfileId();
			}

			public Map getProperties() {
				return profile.getProperties();
			}

			public String getProperty(String key) {
				return profile.getProperty(key);
			}

			public String[] getSubProfileIds() {
				return profile.getSubProfileIds();
			}

			public long getTimestamp() {
				return profile.getTimestamp();
			}

			public boolean hasSubProfiles() {
				return profile.hasSubProfiles();
			}

			public boolean isRootProfile() {
				return profile.isRootProfile();
			}

			public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
				return profile.query(query, collector, monitor);
			}
		};

		ProfileChangeRequest req = new ProfileChangeRequest(availableWrapper);
		req.addInstallableUnits(new IInstallableUnit[] {a1});
		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());

		//expect to have both (a1+inclusion rule) and b1 added
		assertEquals(3, plan.getOperands().length);
	}
}
