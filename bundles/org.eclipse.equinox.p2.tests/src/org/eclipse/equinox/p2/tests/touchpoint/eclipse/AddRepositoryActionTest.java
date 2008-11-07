/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.AddRepositoryAction;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.Version;

/**
 * Tests for {@link org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.AddRepositoryAction}.
 */
public class AddRepositoryActionTest extends AbstractProvisioningTest {
	private static final String TEST_LOCATION = "http://eclipse.org/eclipse/updates/AddRepositoryActionTest";
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
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		getArtifactRepositoryManager().removeRepository(locationURI);
	}

	public void testInvalidEnablement() {
		Map args = getValidArguments();
		args.put("enabled", "bogus enablement");
		IStatus result = action.execute(args);
		//Any value other than "true" for enablement results in a disabled repository
		assertTrue("1.0", result.isOK());
		assertTrue("1.1", !getArtifactRepositoryManager().isEnabled(locationURI));
	}

	public void testInvalidLocation() {
		Map args = getValidArguments();
		args.put("location", "bogus location");
		IStatus result = action.execute(args);
		assertTrue("1.0", !result.isOK());
	}

	public void testInvalidType() {
		Map args = getValidArguments();
		args.put("type", "bogus type");
		IStatus result = action.execute(args);
		assertTrue("1.0", !result.isOK());
	}

	public void testMissingEnablement() {
		//note enablement is optional, defaults to true
		Map args = getValidArguments();
		args.remove("enabled");
		IStatus result = action.execute(args);
		assertTrue("1.0", result.isOK());
	}

	public void testMissingType() {
		Map args = getValidArguments();
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
		IStatus result = action.execute(args);
		assertTrue("1.0", result.isOK());

		result = action.undo(args);
		assertTrue("1.1", result.isOK());
	}

	/**
	 * Tests for install of an IU that adds a repository.
	 */
	public void testFullInstall() {
		String id = "AddRepositoryActionTest.testFullInstall";
		Version version = new Version(1, 0, 0);
		IInstallableUnit iu = createIU(id, version, null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, TOUCHPOINT_OSGI, createTouchpointData(), true, createUpdateDescriptor(id, version));
		IProfile profile = createProfile(id);
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {iu});
		IStatus result = createDirector().provision(request, new ProvisioningContext(), getMonitor());
		assertTrue("1.0", result.isOK());
		//TODO check that the repository was added

	}

	private TouchpointData createTouchpointData() {
		Map args = getValidArguments();
		Map instructions = new HashMap();
		StringBuffer addRepoInstruction = new StringBuffer("addRepository(");
		for (Iterator it = args.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			addRepoInstruction.append(entry.getKey());
			addRepoInstruction.append(':');
			addRepoInstruction.append(entry.getValue());
			if (it.hasNext())
				addRepoInstruction.append(',');
		}
		instructions.put("configure", addRepoInstruction.toString());
		return MetadataFactory.createTouchpointData(instructions);
	}
}
