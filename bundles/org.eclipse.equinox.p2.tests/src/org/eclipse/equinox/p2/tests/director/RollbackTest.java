/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.provisional.p2.core.*;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

public class RollbackTest extends AbstractProvisioningTest {

	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnit c1;
	IInstallableUnit d1;

	private IProfile profile;
	private IDirector director;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", DEFAULT_VERSION, true);
		b1 = createIU("B", DEFAULT_VERSION, true);
		c1 = createIU("C", DEFAULT_VERSION, true);

		IRequiredCapability[] req = new IRequiredCapability[1];
		req[0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false, true);
		d1 = createIU("D", new Version("1.0.0"), req);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, c1, d1});

		profile = createProfile("TestProfile." + getName());
		director = createDirector();

		try {
			IMetadataRepository rollbackRepo = getRollbackRepository();
			if (rollbackRepo != null)
				rollbackRepo.removeAll();
		} catch (ProvisionException e) {
			return;
		}
	}

	private IMetadataRepository getRollbackRepository() throws ProvisionException {
		try {
			IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
			URL location = ((AgentLocation) ServiceHelper.getService(DirectorActivator.context, AgentLocation.class.getName())).getDataArea(DirectorActivator.PI_DIRECTOR);
			return repoMan.loadRepository(URIUtil.toURI(new URL(location, "rollback")), null);
		} catch (MalformedURLException e) {
			fail("0.02", e);
			return null;
		} catch (URISyntaxException e) {
			fail("0.03", e);
			return null;
		}
	}

	public void testRollbackProfileProperties() {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());

		assertEquals(1, profileRegistry.listProfileTimestamps(profile.getProfileId()).length);
		ProfileChangeRequest request1 = new ProfileChangeRequest(profile);
		request1.setProfileProperty("test1", "test");
		request1.setProfileProperty("test2", "test");
		IStatus status = director.provision(request1, null, new NullProgressMonitor());
		assertEquals("1.0", IStatus.OK, status.getCode());
		assertEquals("2.0", "test", profile.getProperty("test1"));
		assertEquals("3.0", "test", profile.getProperty("test2"));

		assertEquals(2, profileRegistry.listProfileTimestamps(profile.getProfileId()).length);

		ProfileChangeRequest request2 = new ProfileChangeRequest(profile);
		request2.removeProfileProperty("test1");
		request2.setProfileProperty("test2", "bad");
		request2.setProfileProperty("test3", "test");
		status = director.provision(request2, null, new NullProgressMonitor());
		assertEquals("5.0", IStatus.OK, status.getCode());
		assertEquals("6.0", null, profile.getProperty("test1"));
		assertEquals("7.0", "bad", profile.getProperty("test2"));
		assertEquals("8.0", "test", profile.getProperty("test3"));

		assertEquals(3, profileRegistry.listProfileTimestamps(profile.getProfileId()).length);
		IProfile revertProfile = profileRegistry.getProfile(profile.getProfileId(), profileRegistry.listProfileTimestamps(profile.getProfileId())[1]);

		status = director.revert(profile, revertProfile, new ProvisioningContext(), new NullProgressMonitor());
		assertEquals("10.0", IStatus.OK, status.getCode());
		assertEquals("11.0", "test", profile.getProperty("test1"));
		assertEquals("12.0", "test", profile.getProperty("test2"));
		assertEquals("13.0", null, profile.getProperty("test3"));
	}

	public void testRollbackIUs() {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());

		assertEquals(1, profileRegistry.listProfileTimestamps(profile.getProfileId()).length);
		ProfileChangeRequest request1 = new ProfileChangeRequest(profile);
		request1.addInstallableUnits(new IInstallableUnit[] {a1});
		request1.addInstallableUnits(new IInstallableUnit[] {b1});
		IStatus status = director.provision(request1, null, new NullProgressMonitor());
		assertEquals("1.0", IStatus.OK, status.getCode());

		List profileIUs = new ArrayList(profile.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection());
		assertTrue("2.0", profileIUs.contains(a1));
		assertTrue("3.0", profileIUs.contains(b1));

		assertEquals(2, profileRegistry.listProfileTimestamps(profile.getProfileId()).length);

		ProfileChangeRequest request2 = new ProfileChangeRequest(profile);
		request2.removeInstallableUnits(new IInstallableUnit[] {a1});
		request2.addInstallableUnits(new IInstallableUnit[] {c1});
		status = director.provision(request2, null, new NullProgressMonitor());
		assertEquals("5.0", IStatus.OK, status.getCode());

		profileIUs = new ArrayList(profile.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection());
		assertFalse("6.0", profileIUs.contains(a1));
		assertTrue("7.0", profileIUs.contains(b1));
		assertTrue("8.0", profileIUs.contains(c1));

		assertEquals(3, profileRegistry.listProfileTimestamps(profile.getProfileId()).length);
		IProfile revertProfile = profileRegistry.getProfile(profile.getProfileId(), profileRegistry.listProfileTimestamps(profile.getProfileId())[1]);

		status = director.revert(profile, revertProfile, new ProvisioningContext(), new NullProgressMonitor());
		assertEquals("10.0", IStatus.OK, status.getCode());

		profileIUs = new ArrayList(profile.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection());
		assertTrue("11.0", profileIUs.contains(a1));
		assertTrue("12.0", profileIUs.contains(b1));
		assertFalse("13.0", profileIUs.contains(c1));
	}

	public void testRollbackIUProfileProperties() {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());

		assertEquals(1, profileRegistry.listProfileTimestamps(profile.getProfileId()).length);
		ProfileChangeRequest request1 = new ProfileChangeRequest(profile);
		request1.addInstallableUnits(new IInstallableUnit[] {a1});
		request1.setInstallableUnitProfileProperty(a1, "test1", "test");
		request1.setInstallableUnitProfileProperty(a1, "test2", "test");
		IStatus status = director.provision(request1, null, new NullProgressMonitor());
		assertEquals("1.0", IStatus.OK, status.getCode());
		assertEquals("2.0", "test", profile.getInstallableUnitProperty(a1, "test1"));
		assertEquals("3.0", "test", profile.getInstallableUnitProperty(a1, "test2"));

		assertEquals(2, profileRegistry.listProfileTimestamps(profile.getProfileId()).length);

		ProfileChangeRequest request2 = new ProfileChangeRequest(profile);
		request2.removeInstallableUnitProfileProperty(a1, "test1");
		request2.setInstallableUnitProfileProperty(a1, "test2", "bad");
		request2.setInstallableUnitProfileProperty(a1, "test3", "test");
		status = director.provision(request2, null, new NullProgressMonitor());
		assertEquals("5.0", IStatus.OK, status.getCode());
		assertEquals("6.0", null, profile.getInstallableUnitProperty(a1, "test1"));
		assertEquals("7.0", "bad", profile.getInstallableUnitProperty(a1, "test2"));
		assertEquals("8.0", "test", profile.getInstallableUnitProperty(a1, "test3"));

		assertEquals(3, profileRegistry.listProfileTimestamps(profile.getProfileId()).length);
		IProfile revertProfile = profileRegistry.getProfile(profile.getProfileId(), profileRegistry.listProfileTimestamps(profile.getProfileId())[1]);

		status = director.revert(profile, revertProfile, new ProvisioningContext(), new NullProgressMonitor());
		assertEquals("10.0", IStatus.OK, status.getCode());
		assertEquals("11.0", "test", profile.getInstallableUnitProperty(a1, "test1"));
		assertEquals("12.0", "test", profile.getInstallableUnitProperty(a1, "test2"));
		assertEquals("13.0", null, profile.getInstallableUnitProperty(a1, "test3"));
	}

	public void testRollbackDependentIUProfileProperties() {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());

		assertEquals(1, profileRegistry.listProfileTimestamps(profile.getProfileId()).length);
		ProfileChangeRequest request1 = new ProfileChangeRequest(profile);
		request1.addInstallableUnits(new IInstallableUnit[] {d1});
		request1.setInstallableUnitProfileProperty(d1, "test1", "test");
		request1.setInstallableUnitProfileProperty(a1, "test2", "test");
		IStatus status = director.provision(request1, null, new NullProgressMonitor());
		assertEquals("1.0", IStatus.OK, status.getCode());
		assertEquals("2.0", "test", profile.getInstallableUnitProperty(d1, "test1"));
		assertEquals("3.0", "test", profile.getInstallableUnitProperty(a1, "test2"));

		assertEquals(2, profileRegistry.listProfileTimestamps(profile.getProfileId()).length);

		ProfileChangeRequest request2 = new ProfileChangeRequest(profile);
		request2.removeInstallableUnits(new IInstallableUnit[] {d1});
		request2.addInstallableUnits(new IInstallableUnit[] {b1});
		request2.setInstallableUnitProfileProperty(b1, "test3", "test");

		status = director.provision(request2, null, new NullProgressMonitor());
		assertEquals("5.0", IStatus.OK, status.getCode());
		assertEquals("6.0", null, profile.getInstallableUnitProperty(d1, "test1"));
		assertEquals("7.0", null, profile.getInstallableUnitProperty(a1, "test2"));
		assertEquals("8.0", "test", profile.getInstallableUnitProperty(b1, "test3"));

		assertEquals(3, profileRegistry.listProfileTimestamps(profile.getProfileId()).length);
		IProfile revertProfile = profileRegistry.getProfile(profile.getProfileId(), profileRegistry.listProfileTimestamps(profile.getProfileId())[1]);

		status = director.revert(profile, revertProfile, new ProvisioningContext(), new NullProgressMonitor());
		assertEquals("10.0", IStatus.OK, status.getCode());
		assertEquals("11.0", "test", profile.getInstallableUnitProperty(d1, "test1"));
		assertEquals("12.0", "test", profile.getInstallableUnitProperty(a1, "test2"));
		assertEquals("13.0", null, profile.getInstallableUnitProperty(b1, "test3"));
	}

}
