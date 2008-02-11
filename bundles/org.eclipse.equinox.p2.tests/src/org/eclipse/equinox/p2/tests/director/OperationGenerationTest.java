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
		new OperationGenerator().generateOperation(from, to);
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
		new OperationGenerator().generateOperation(from, to);
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
		new OperationGenerator().generateOperation(from, to);
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
		new OperationGenerator().generateOperation(from, to);
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
		new OperationGenerator().generateOperation(from, to);
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
		new OperationGenerator().generateOperation(from, to);
	}

}
