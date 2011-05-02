/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class UninstallEverything extends AbstractProvisioningTest {
	IInstallableUnit a;

	IDirector director;
	IProfile profile;

	protected void setUp() throws Exception {
		super.setUp();
		a = createIU("A", Version.create("1.0.0"));
		createTestMetdataRepository(new IInstallableUnit[] {a});
		profile = createProfile(Bug207319.class.getName());
		director = createDirector();

	}

	public void testEmpty() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {a});
		assertEquals(IStatus.OK, director.provision(req, null, null).getSeverity());
		assertProfileContainsAll("A is missing", profile, new IInstallableUnit[] {a});

		ProfileChangeRequest req2 = new ProfileChangeRequest(profile);
		req2.removeInstallableUnits(new IInstallableUnit[] {a});
		assertEquals(IStatus.OK, director.provision(req2, null, null).getSeverity());
		assertNotIUs(new IInstallableUnit[] {a}, profile.query(QueryUtil.createIUAnyQuery(), null).iterator());

	}
}
