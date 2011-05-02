/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.RemoveRepositoryAction;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests for {@link org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.RemoveRepositoryAction}.
 */
public class RemoveRepositoryActionTest extends AbstractProvisioningTest {
	private static final String TEST_LOCATION = "http://eclipse.org/eclipse/updates/RemoveRepositoryActionTest";
	RemoveRepositoryAction action;
	private URI locationURI;

	/**
	 * Returns a map containing valid arguments for this action.
	 */
	private Map getValidArguments() {
		Map args = new HashMap();
		args.put(ActionConstants.PARM_AGENT, getAgent());
		args.put("location", TEST_LOCATION);
		args.put("type", Integer.toString(IRepository.TYPE_ARTIFACT));
		args.put("enabled", "true");
		return args;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		locationURI = new URI(TEST_LOCATION);
		action = new RemoveRepositoryAction();
		getArtifactRepositoryManager().addRepository(locationURI);
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
		//enablement is not relevant for remove repository action
		assertTrue("1.0", result.isOK());
		assertTrue("1.1", !getArtifactRepositoryManager().isEnabled(locationURI));
	}

	public void testInvalidLocation() {
		Map args = getValidArguments();
		args.put("location", "bogus location");
		IStatus result = action.execute(args);
		assertTrue("1.0", !result.isOK());
		assertTrue("1.1", getArtifactRepositoryManager().isEnabled(locationURI));
	}

	public void testInvalidType() {
		Map args = getValidArguments();
		args.put("type", "bogus type");
		IStatus result = action.execute(args);
		assertTrue("1.0", !result.isOK());
		assertTrue("1.1", getArtifactRepositoryManager().isEnabled(locationURI));
	}

	public void testNoArguments() {
		IStatus result = action.execute(new HashMap());
		assertTrue("1.0", !result.isOK());
		assertTrue("1.1", getArtifactRepositoryManager().isEnabled(locationURI));
	}

	public void testRemoveMetadataRepository() {
		Map args = getValidArguments();
		args.put("type", Integer.toString(IRepository.TYPE_METADATA));
		getMetadataRepositoryManager().addRepository(locationURI);
		IStatus result = action.execute(args);
		assertTrue("1.0", result.isOK());
		assertTrue("1.1", !getMetadataRepositoryManager().isEnabled(locationURI));
	}

	public void testUndo() {
		Map args = getValidArguments();
		IStatus result = action.execute(args);
		assertTrue("1.0", result.isOK());
		assertTrue("1.1", !getArtifactRepositoryManager().isEnabled(locationURI));

		result = action.undo(args);
		assertTrue("2.0", result.isOK());
	}

	public void testUndoInvalidArgument() {
		Map args = getValidArguments();
		IStatus result = action.execute(args);
		assertTrue("1.0", result.isOK());
		assertTrue("1.1", !getArtifactRepositoryManager().isEnabled(locationURI));

		args.put("type", "bogus type");
		result = action.undo(args);
		assertTrue("2.0", !result.isOK());
	}
}
