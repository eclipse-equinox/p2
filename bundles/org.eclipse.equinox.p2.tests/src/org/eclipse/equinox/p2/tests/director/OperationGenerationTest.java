/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.director.OperationGenerator;
import org.eclipse.equinox.internal.p2.resolution.ResolutionHelper;
import org.eclipse.equinox.internal.provisional.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class OperationGenerationTest extends AbstractProvisioningTest {
	public void testInstallUninstall() {
		IInstallableUnit a1 = createIU("a", new Version(1, 0, 0), false);
		IInstallableUnit a2 = createIU("a", new Version(2, 0, 0), false);
		IInstallableUnit a3 = createIU("a", new Version(3, 0, 0), false);

		Collection from;
		from = new ArrayList();
		from.add(a1);
		from.add(a2);

		Collection to;
		to = new ArrayList();
		to.add(a1);
		to.add(a3);

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);
		InstallableUnitOperand[] operands = new OperationGenerator().generateOperation(from, to);
		// 1 x install
		// 1 x uninstall
		assertEquals(2, operands.length);
	}

	public void test1() {
		IInstallableUnit a1 = createIU("a", new Version(1, 0, 0), false);
		IInstallableUnit a2 = createIU("a", new Version(2, 0, 0), false);
		IInstallableUnit a3 = createIU("a", new Version(3, 0, 0), false);

		Collection from;
		from = new ArrayList();
		from.add(a1);
		from.add(a3);

		Collection to;
		to = new ArrayList();
		to.add(a1);
		to.add(a3);
		to.add(a2);

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);
		InstallableUnitOperand[] operands = new OperationGenerator().generateOperation(from, to);
		// 1 x install
		assertEquals(1, operands.length);
	}

	public void test2() {
		IInstallableUnit a1 = createIU("a", new Version(1, 0, 0), false);
		IInstallableUnit a2 = createIU("a", new Version(2, 0, 0), false);
		IInstallableUnit a3 = createIU("a", new Version(3, 0, 0), false);

		Collection from;
		from = new ArrayList();
		from.add(a1);
		from.add(a2);
		from.add(a3);

		Collection to;
		to = new ArrayList();
		to.add(a1);
		to.add(a3);

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);
		InstallableUnitOperand[] operands = new OperationGenerator().generateOperation(from, to);
		// 1 x uninstall
		assertEquals(1, operands.length);
	}

	public void testUpdate1() {
		IInstallableUnit a = createIU("a", new Version(1, 0, 0), false);

		InstallableUnitDescription b = new MetadataFactory.InstallableUnitDescription();
		b.setId("b");
		b.setVersion(new Version(1, 0, 0));
		b.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor("a", new VersionRange("[1.0.0, 2.0.0)"), IUpdateDescriptor.NORMAL, null));

		Collection from;
		from = new ArrayList();
		from.add(a);

		Collection to;
		to = new ArrayList();
		to.add(MetadataFactory.createInstallableUnit(b));

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);
		InstallableUnitOperand[] operands = new OperationGenerator().generateOperation(from, to);
		// 1 x upgrade
		assertEquals(1, operands.length);
	}

	public void testUpdate2() {
		IInstallableUnit a1 = createIU("a", new Version(1, 0, 0), false);
		IInstallableUnit a2 = createIU("a", new Version(2, 0, 0), false);

		InstallableUnitDescription b = new MetadataFactory.InstallableUnitDescription();
		b.setId("b");
		b.setVersion(new Version(1, 0, 0));
		b.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor("a", new VersionRange("[1.0.0, 3.0.0)"), IUpdateDescriptor.NORMAL, null));

		Collection from;
		from = new ArrayList();
		from.add(a1);
		from.add(a2);

		Collection to;
		to = new ArrayList();
		to.add(MetadataFactory.createInstallableUnit(b));

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);
		InstallableUnitOperand[] operands = new OperationGenerator().generateOperation(from, to);
		// 1 x install
		// 2 x uninstall
		assertEquals(3, operands.length);
	}

	public void testUpdate3() {
		IInstallableUnit a1 = createIU("a", new Version(1, 0, 0), false);
		IInstallableUnit a2 = createIU("a", new Version(2, 0, 0), false);

		InstallableUnitDescription b = new MetadataFactory.InstallableUnitDescription();
		b.setId("b");
		b.setVersion(new Version(1, 0, 0));
		b.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor("a", new VersionRange("[1.0.0, 2.0.0)"), IUpdateDescriptor.NORMAL, null));

		InstallableUnitDescription c = new MetadataFactory.InstallableUnitDescription();
		c.setId("c");
		c.setVersion(new Version(1, 0, 0));
		c.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor("a", new VersionRange("[2.0.0, 2.3.0)"), IUpdateDescriptor.NORMAL, null));

		Collection from;
		from = new ArrayList();
		from.add(a1);
		from.add(a2);

		Collection to;
		to = new ArrayList();
		to.add(MetadataFactory.createInstallableUnit(b));
		to.add(MetadataFactory.createInstallableUnit(c));

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);
		InstallableUnitOperand[] operands = new OperationGenerator().generateOperation(from, to);
		// 2 x update
		assertEquals(2, operands.length);
	}

	public void testUpdate4() {
		IInstallableUnit a1 = createIU("a", new Version(1, 0, 0), false);
		IInstallableUnit a2 = createIU("a", new Version(2, 0, 0), false);
		IInstallableUnit b1 = createIU("b", new Version(1, 0, 0), false);

		InstallableUnitDescription b2 = new MetadataFactory.InstallableUnitDescription();
		b2.setId("b");
		b2.setVersion(new Version(2, 0, 0));
		b2.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor("b", new VersionRange("[1.0.0, 2.0.0)"), IUpdateDescriptor.NORMAL, null));

		Collection from;
		from = new ArrayList();
		from.add(a1);
		from.add(a2);
		from.add(b1);

		Collection to;
		to = new ArrayList();
		to.add(a1);
		to.add(a2);
		to.add(MetadataFactory.createInstallableUnit(b2));

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);
		InstallableUnitOperand[] operands = new OperationGenerator().generateOperation(from, to);
		// 1 x update
		assertEquals(1, operands.length);
	}

	public void testUpdate5() {
		IInstallableUnit a1 = createIU("a", new Version(1, 0, 0), false);
		IInstallableUnit a2 = createIU("a", new Version(2, 0, 0), false);
		IInstallableUnit b1 = createIU("b", new Version(1, 0, 0), false);

		InstallableUnitDescription b2 = new MetadataFactory.InstallableUnitDescription();
		b2.setId("b");
		b2.setVersion(new Version(2, 0, 0));
		b2.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor("b", new VersionRange("[1.0.0, 2.0.0)"), IUpdateDescriptor.NORMAL, null));

		Collection from;
		from = new ArrayList();
		from.add(a1);
		from.add(a2);
		from.add(b1);

		Collection to;
		to = new ArrayList();
		to.add(a1);
		to.add(MetadataFactory.createInstallableUnit(b2));

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);
		InstallableUnitOperand[] operands = new OperationGenerator().generateOperation(from, to);
		// 1 x update
		// 1 x uninstall
		assertEquals(2, operands.length);
	}

	private IUpdateDescriptor createUpdateDescriptor(String id, Version version) {
		return MetadataFactory.createUpdateDescriptor(id, new VersionRange(Version.emptyVersion, true, version, false), IUpdateDescriptor.HIGH, "desc");
	}

	public void test248468b() {
		String id = "myBundle";
		IUpdateDescriptor update = createUpdateDescriptor(id, new Version("1.0.0"));
		IInstallableUnit one = createIU(id, new Version("1.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false, update);
		IUpdateDescriptor update2 = createUpdateDescriptor(id, new Version("2.0.0"));
		IInstallableUnit two = createIU(id, new Version("2.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false, update2);

		IUpdateDescriptor update3 = createUpdateDescriptor(id, new Version("3.0.0"));
		IInstallableUnit three = createIU(id, new Version("3.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false, update3);

		Collection from = new ArrayList();
		from.add(MetadataFactory.createResolvedInstallableUnit(one, new IInstallableUnitFragment[0]));
		from.add(MetadataFactory.createResolvedInstallableUnit(two, new IInstallableUnitFragment[0]));
		from.add(MetadataFactory.createResolvedInstallableUnit(three, new IInstallableUnitFragment[0]));

		Collection to = new ArrayList();
		to.add(MetadataFactory.createResolvedInstallableUnit(three, new IInstallableUnitFragment[0]));

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);
		InstallableUnitOperand[] operands = new OperationGenerator().generateOperation(from, to);
		//We are uninstalling myBundle 1.0 and 2.0. 3.0 stays unchanged.
		for (int i = 0; i < operands.length; i++) {
			assertNotSame("3.0", three, operands[i].first());
			assertNotSame("3.0.1", three, operands[i].second());
		}
		assertEquals("3.1", one, operands[0].first());
		assertNull("3.2", operands[0].second());
		assertEquals("3.3", two, operands[1].first());
		assertNull("3.4", operands[1].second());
	}

	public void test248468d() {
		String id = "myBundle";
		IUpdateDescriptor update = createUpdateDescriptor(id, new Version("1.0.0"));
		IInstallableUnit one = createIU(id, new Version("1.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false, update);
		update = createUpdateDescriptor(id, new Version("2.0.0"));
		IInstallableUnit two = createIU(id, new Version("2.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false, update);

		IUpdateDescriptor update3 = createUpdateDescriptor(id, new Version("3.0.0"));
		IInstallableUnit three = createIU("anotherBundle", new Version("3.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false, update3);

		Collection from = new ArrayList();
		from.add(MetadataFactory.createResolvedInstallableUnit(one, new IInstallableUnitFragment[0]));
		from.add(MetadataFactory.createResolvedInstallableUnit(two, new IInstallableUnitFragment[0]));

		Collection to = new ArrayList();
		to.add(MetadataFactory.createResolvedInstallableUnit(two, new IInstallableUnitFragment[0]));
		to.add(MetadataFactory.createResolvedInstallableUnit(three, new IInstallableUnitFragment[0]));

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);

		InstallableUnitOperand[] operands = new OperationGenerator().generateOperation(from, to);
		//Two is already in the system therefore it will not be in the operands
		for (int i = 0; i < operands.length; i++) {
			assertNotSame("2.0", two, operands[i].first());
			assertNotSame("2.1", two, operands[i].second());
		}
		//three is an update of one
		assertEquals("2.2", 1, operands.length);
		assertEquals("2.4", one, operands[0].first());
		assertEquals("2.5", three, operands[0].second());
	}

	public void test248468c() {
		String id = "myBundle";
		IUpdateDescriptor update = createUpdateDescriptor(id, new Version("1.0.0"));
		IInstallableUnit one = createIU(id, new Version("1.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false, update);
		update = createUpdateDescriptor(id, new Version("2.0.0"));
		IInstallableUnit two = createIU(id, new Version("2.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false, update);

		IUpdateDescriptor update3 = MetadataFactory.createUpdateDescriptor(id, new VersionRange(new Version(2, 0, 0), true, new Version(3, 0, 0), false), IUpdateDescriptor.HIGH, "desc");
		//		IUpdateDescriptor update3 = createUpdateDescriptor(id, new Version("3.0.0"));
		IInstallableUnit three = createIU("anotherBundle", new Version("3.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false, update3);

		Collection from = new ArrayList();
		from.add(MetadataFactory.createResolvedInstallableUnit(one, new IInstallableUnitFragment[0]));
		from.add(MetadataFactory.createResolvedInstallableUnit(two, new IInstallableUnitFragment[0]));

		Collection to = new ArrayList();
		to.add(MetadataFactory.createResolvedInstallableUnit(two, new IInstallableUnitFragment[0]));
		to.add(MetadataFactory.createResolvedInstallableUnit(three, new IInstallableUnitFragment[0]));

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);

		InstallableUnitOperand[] operands = new OperationGenerator().generateOperation(from, to);
		//Two is already in the system therefore it will not be in the operands
		for (int i = 0; i < operands.length; i++) {
			assertNotSame("2.0", two, operands[i].first());
			assertNotSame("2.1", two, operands[i].second());
		}
		//We install three and uninstall one
		assertEquals("2.2", 2, operands.length);
		assertNull("2.3", operands[0].first());
		assertEquals("2.4", three, operands[0].second());
		assertEquals("2.5", one, operands[1].first());
		assertNull("2.6", operands[1].second());
	}

	public void test248468() {
		String id = "myBundle";
		IUpdateDescriptor update = createUpdateDescriptor(id, new Version("1.0.0"));
		IInstallableUnit one = createIU(id, new Version("1.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false, update);
		update = createUpdateDescriptor(id, new Version("2.0.0"));
		IInstallableUnit two = createIU(id, new Version("2.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TouchpointType.NONE, NO_TP_DATA, false, update);

		Collection from = new ArrayList();
		from.add(MetadataFactory.createResolvedInstallableUnit(one, new IInstallableUnitFragment[0]));
		from.add(MetadataFactory.createResolvedInstallableUnit(two, new IInstallableUnitFragment[0]));

		Collection to = new ArrayList();
		to.add(MetadataFactory.createResolvedInstallableUnit(two, new IInstallableUnitFragment[0]));

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);
		InstallableUnitOperand[] operands = new OperationGenerator().generateOperation(from, to);
		assertEquals("1.0", 1, operands.length);
		assertEquals("1.1", one, operands[0].first());
		assertNull("1.2", operands[0].second());
	}
}
