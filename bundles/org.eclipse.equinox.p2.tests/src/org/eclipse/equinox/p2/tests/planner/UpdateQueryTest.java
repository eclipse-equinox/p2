package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.UpdateQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class UpdateQueryTest extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IInstallableUnit updateOfA;
	private IInstallableUnit a11;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		a1 = createIU("A", Version.create("2.0.0"));
		IUpdateDescriptor update = MetadataFactory.createUpdateDescriptor("A", new VersionRange("[2.0.0, 2.0.0]"), 0, "update description");
		updateOfA = createIU("UpdateA", Version.createOSGi(1, 0, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, false, update, NO_REQUIRES);
		a11 = createIUUpdate();
	}

	public void testUpdateWithDifferentId() {
		IMetadataRepository repo = createTestMetdataRepository(new IInstallableUnit[] {a1, updateOfA});
		Collector c = repo.query(new UpdateQuery(a1), new Collector(), null);
		assertEquals(1, c.size());
		assertEquals(updateOfA, c.iterator().next());
	}

	public void testWithSuperiorVersion() {
		IMetadataRepository repo2 = createTestMetdataRepository(new IInstallableUnit[] {a11, a1});
		Collector c2 = repo2.query(new UpdateQuery(a1), new Collector(), null);
		assertEquals(1, c2.size());
		assertEquals(a11, c2.iterator().next());
	}

	public void testUpdateWithSameId() {

	}

	private IInstallableUnit createIUUpdate() {
		return createIU("A", Version.create("2.1.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false, MetadataFactory.createUpdateDescriptor("A", new VersionRange("[2.0.0, 2.1.0]"), 0, "update description"), null);
	}
}
