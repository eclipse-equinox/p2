/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.metadata.TouchpointInstruction;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.AddRepositoryAction;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.service.prefs.Preferences;

/**
 * Tests for {@link org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.AddRepositoryAction}.
 */
public class AddRepositoryActionTest extends AbstractProvisioningTest {
	private static final String TEST_LOCATION = "http://eclipse.org/eclipse/updates/AddRepositoryActionTest";
	private static final String KEY_URI = "uri";
	AddRepositoryAction action;
	private URI locationURI;

	/**
	 * Returns a map containing valid arguments for this action.
	 */
	private Map getValidArguments() {
		Map args = new HashMap();
		args.put("location", TEST_LOCATION);
		args.put("type", Integer.toString(IRepository.TYPE_ARTIFACT));
		args.put("enabled", "true");
		return args;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		action = new AddRepositoryAction();
		locationURI = new URI(TEST_LOCATION);
		getArtifactRepositoryManager().removeRepository(locationURI);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		getArtifactRepositoryManager().removeRepository(locationURI);
	}

	public void testInvalidEnablement() {
		Map args = getValidArguments();
		addAgent(args);
		args.put("enabled", "bogus enablement");
		IStatus result = action.execute(args);
		//Any value other than "true" for enablement results in a disabled repository
		assertTrue("1.0", result.isOK());
		assertTrue("1.1", !getArtifactRepositoryManager().isEnabled(locationURI));
	}

	private void addAgent(Map args) {
		args.put(ActionConstants.PARM_AGENT, getAgent());
	}

	public void testInvalidLocation() {
		Map args = getValidArguments();
		addAgent(args);
		args.put("location", "bogus location");
		IStatus result = action.execute(args);
		assertTrue("1.0", !result.isOK());
	}

	public void testInvalidType() {
		Map args = getValidArguments();
		addAgent(args);
		args.put("type", "bogus type");
		IStatus result = action.execute(args);
		assertTrue("1.0", !result.isOK());
	}

	public void testMissingEnablement() {
		//note enablement is optional, defaults to true
		Map args = getValidArguments();
		addAgent(args);
		args.remove("enabled");
		IStatus result = action.execute(args);
		assertTrue("1.0", result.isOK());
	}

	public void testMissingType() {
		Map args = getValidArguments();
		addAgent(args);
		args.remove("type");
		IStatus result = action.execute(args);
		assertTrue("1.0", !result.isOK());
	}

	public void testNoArguments() {
		IStatus result = action.execute(new HashMap());
		assertTrue("1.0", !result.isOK());
	}

	public void testUndo() {
		Map args = getValidArguments();
		addAgent(args);
		IStatus result = action.execute(args);
		assertTrue("1.0", result.isOK());

		result = action.undo(args);
		assertTrue("1.1", result.isOK());
	}

	public void testMultipleActionAdd() {
		Map args = getValidArguments();
		addAgent(args);
		IStatus result = action.execute(args);
		assertTrue("1.0", result.isOK());

		result = action.execute(args);
		assertTrue("1.1", result.isOK());

		result = action.undo(args);
		assertTrue("1.2", result.isOK());

		assertTrue("2.0", locationExists(null, TEST_LOCATION));
	}

	public void testUserWins() {
		try {
			getArtifactRepositoryManager().addRepository(new URI(TEST_LOCATION));
			assertTrue("0.1", locationExists(null, TEST_LOCATION));
		} catch (URISyntaxException e) {
			// Should not occur
		}

		Map args = getValidArguments();
		addAgent(args);
		IStatus result = action.execute(args);
		assertTrue("1.0", result.isOK());

		result = action.undo(args);
		assertTrue("1.1", result.isOK());

		assertTrue("2.0", locationExists(null, TEST_LOCATION));
	}

	/**
	 * Tests for install of an IU that adds a repository.
	 */
	public void testFullInstall() {
		String id = "AddRepositoryActionTest.testFullInstall";
		Version version = Version.createOSGi(1, 0, 0);
		Map instructions = new HashMap();
		instructions.put("configure", TouchpointInstruction.encodeAction("addRepository", getValidArguments()));
		ITouchpointData tpData = MetadataFactory.createTouchpointData(instructions);
		IInstallableUnit iu = createIU(id, version, null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TOUCHPOINT_OSGI, tpData, true, createUpdateDescriptor(id, version), null);
		IProfile profile = createProfile(id);
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {iu});

		assertTrue("0.1", !getArtifactRepositoryManager().contains(locationURI));

		IStatus result = createDirector().provision(request, new ProvisioningContext(getAgent()), getMonitor());
		assertTrue("1.0", result.isOK());

		//check that profile property is set
		profile = getProfile(id);
		// Get Preference node associated with the profile
		Preferences pref = new ProfileScope(getAgentLocation(), profile.getProfileId()).getNode("org.eclipse.equinox.p2.artifact.repository/repositories/" + getKey(TEST_LOCATION));
		String value = pref.get(KEY_URI, null);
		assertEquals("2.0", value, TEST_LOCATION);
	}

	/**
	 * Tests adding a repository during an update (bug 266881).
	 */
	public void testBug266881() throws ProvisionException {
		//need to install a real bundle with an artifact to check for GC bug
		URI site = getTestData("0.1", "/testData/testRepos/simple.1").toURI();
		getMetadataRepositoryManager().addRepository(site);
		getArtifactRepositoryManager().addRepository(site);

		//install the old IU
		String id = "AddRepositoryActionTest.testUpdate";
		Version version = Version.createOSGi(1, 0, 0);
		IInstallableUnit oldIU = createIU(id, version);
		IProfile profile = createProfile(id);
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		final IInstallableUnit[] oldIUs = new IInstallableUnit[] {oldIU};
		request.addInstallableUnits(oldIUs);
		IStatus result = createDirector().provision(request, new ProvisioningContext(getAgent()), getMonitor());
		assertTrue("1.0", result.isOK());

		assertTrue("1.1", !getArtifactRepositoryManager().contains(locationURI));

		//define new IU
		version = Version.createOSGi(1, 1, 0);
		Map instructions = new HashMap();
		instructions.put("configure", TouchpointInstruction.encodeAction("addRepository", getValidArguments()));
		ITouchpointData tpData = MetadataFactory.createTouchpointData(instructions);
		IInstallableUnit newIU = createIU(id, version, null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TOUCHPOINT_OSGI, tpData, true, createUpdateDescriptor(id, version), null);

		//perform the update and install an ordinary bundle
		IMetadataRepository repo = getMetadataRepositoryManager().loadRepository(site, getMonitor());
		IInstallableUnit bundle = repo.query(QueryUtil.createIUQuery("aBundle"), getMonitor()).iterator().next();
		request = new ProfileChangeRequest(profile);
		final IInstallableUnit[] newIUs = new IInstallableUnit[] {newIU, bundle};
		request.addInstallableUnits(newIUs);
		request.removeInstallableUnits(oldIUs);
		result = createDirector().provision(request, new ProvisioningContext(getAgent()), getMonitor());
		if (!result.isOK())
			LogHelper.log(result);
		assertTrue("2.0", result.isOK());

		//check that the artifact is still there
		profile = getProfile(id);
		IArtifactRepository artifacts = getArtifactRepositoryManager().loadRepository(Util.getBundlePoolLocation(getAgent(), profile), getMonitor());
		assertEquals("3.0", 1, getArtifactKeyCount(artifacts));

		//check that profile property is set
		assertProfileContains("3.1", profile, newIUs);
		// Get Preference node associated with the profile
		Preferences pref = new ProfileScope(getAgentLocation(), profile.getProfileId()).getNode("org.eclipse.equinox.p2.artifact.repository/repositories/" + getKey(TEST_LOCATION));
		String value = pref.get(KEY_URI, null);

		assertEquals("3.2", value, TEST_LOCATION);
	}

	/*
	 * Modified from AbstractRepositoryManager
	 */
	private String getKey(String location) {
		String key = location.replace('/', '_');
		//remove trailing slash
		if (key.endsWith("_")) //$NON-NLS-1$
			key = key.substring(0, key.length() - 1);
		return key;
	}

	private boolean locationExists(IProfile profile, String location) {
		final String profileId = profile != null ? profile.getProfileId() : IProfileRegistry.SELF;
		Preferences pref = new ProfileScope(getAgentLocation(), profileId).getNode("org.eclipse.equinox.p2.artifact.repository/repositories/" + getKey(location));
		if (location.equals(pref.get(KEY_URI, null)))
			return true;
		return false;
	}
}
