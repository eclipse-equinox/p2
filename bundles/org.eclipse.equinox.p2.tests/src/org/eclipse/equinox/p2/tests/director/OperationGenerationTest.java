/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import java.util.ArrayList;
import java.util.Collection;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.director.OperationGenerator;
import org.eclipse.equinox.p2.metadata.IInstallableUnitConstants;
import org.eclipse.equinox.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.resolution.ResolutionHelper;
import org.osgi.framework.Version;

public class OperationGenerationTest extends TestCase {
	public void testInstallUninstall() {
		InstallableUnit a1 = new InstallableUnit();
		a1.setId("a");
		a1.setVersion(new Version(1, 0, 0));

		InstallableUnit a2 = new InstallableUnit();
		a2.setId("a");
		a2.setVersion(new Version(2, 0, 0));

		InstallableUnit a3 = new InstallableUnit();
		a3.setId("a");
		a3.setVersion(new Version(3, 0, 0));

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
		InstallableUnit a1 = new InstallableUnit();
		a1.setId("a");
		a1.setVersion(new Version(1, 0, 0));

		InstallableUnit a2 = new InstallableUnit();
		a2.setId("a");
		a2.setVersion(new Version(2, 0, 0));

		InstallableUnit a3 = new InstallableUnit();
		a3.setId("a");
		a3.setVersion(new Version(3, 0, 0));

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
		InstallableUnit a1 = new InstallableUnit();
		a1.setId("a");
		a1.setVersion(new Version(1, 0, 0));

		InstallableUnit a2 = new InstallableUnit();
		a2.setId("a");
		a2.setVersion(new Version(2, 0, 0));

		InstallableUnit a3 = new InstallableUnit();
		a3.setId("a");
		a3.setVersion(new Version(3, 0, 0));

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
		InstallableUnit a1 = new InstallableUnit();
		a1.setId("a");
		a1.setVersion(new Version(1, 0, 0));

		InstallableUnit a2 = new InstallableUnit();
		a2.setId("a");
		a2.setVersion(new Version(2, 0, 0));

		InstallableUnit b = new InstallableUnit();
		b.setId("b");
		b.setVersion(new Version(1, 0, 0));
		b.setProperty(IInstallableUnitConstants.UPDATE_FROM, "a");
		b.setProperty(IInstallableUnitConstants.UPDATE_RANGE, "[1.0.0, 2.0.0)");

		InstallableUnit c = new InstallableUnit();
		c.setId("c");
		c.setVersion(new Version(1, 0, 0));
		c.setProperty(IInstallableUnitConstants.UPDATE_FROM, "a");
		c.setProperty(IInstallableUnitConstants.UPDATE_RANGE, "[2.0.0, 2.3.0)");

		Collection from;
		from = new ArrayList();
		from.add(a1);
		from.add(a2);

		Collection to;
		to = new ArrayList();
		to.add(b);
		to.add(c);

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);
		new OperationGenerator().generateOperation(from, to);
	}

	public void testUpdate2() {
		InstallableUnit a1 = new InstallableUnit();
		a1.setId("a");
		a1.setVersion(new Version(1, 0, 0));

		InstallableUnit a2 = new InstallableUnit();
		a2.setId("a");
		a2.setVersion(new Version(2, 0, 0));

		InstallableUnit b = new InstallableUnit();
		b.setId("b");
		b.setVersion(new Version(1, 0, 0));
		b.setProperty(IInstallableUnitConstants.UPDATE_FROM, "a");
		b.setProperty(IInstallableUnitConstants.UPDATE_RANGE, "[1.0.0, 3.0.0)");

		Collection from;
		from = new ArrayList();
		from.add(a1);
		from.add(a2);

		Collection to;
		to = new ArrayList();
		to.add(b);

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);
		new OperationGenerator().generateOperation(from, to);
	}

	public void testUpdate1() {
		InstallableUnit a = new InstallableUnit();
		a.setId("a");
		a.setVersion(new Version(1, 0, 0));

		InstallableUnit b = new InstallableUnit();
		b.setId("b");
		b.setVersion(new Version(1, 0, 0));
		b.setProperty(IInstallableUnitConstants.UPDATE_FROM, "a");
		b.setProperty(IInstallableUnitConstants.UPDATE_RANGE, "[1.0.0, 2.0.0)");

		Collection from;
		from = new ArrayList();
		from.add(a);

		Collection to;
		to = new ArrayList();
		to.add(b);

		from = new ResolutionHelper(null, null).attachCUs(from);
		to = new ResolutionHelper(null, null).attachCUs(to);
		new OperationGenerator().generateOperation(from, to);
	}

}
