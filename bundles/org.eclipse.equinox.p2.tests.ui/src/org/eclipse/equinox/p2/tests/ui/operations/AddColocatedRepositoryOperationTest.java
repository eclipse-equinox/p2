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
package org.eclipse.equinox.p2.tests.ui.operations;

import java.net.URI;
import java.util.Arrays;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.AddColocatedRepositoryOperation;
import org.eclipse.equinox.p2.tests.*;

/**
 * Tests for {@link AddColocatedRepositoryOperation}.
 */
public class AddColocatedRepositoryOperationTest extends AbstractProvisioningTest {
	public void testAddSingleRepository() {
		URI repoLocation = null;
		try {
			repoLocation = TestData.getFile("artifactRepo", "").toURI();
		} catch (Exception e) {
			fail("0.99", e);
		}
		AddColocatedRepositoryOperation op = new AddColocatedRepositoryOperation("label", repoLocation);
		assertTrue("1.0", op.canExecute());
		assertTrue("1.1", !op.canUndo());
		assertTrue("1.2", op.runInBackground());
		Object[] affected = op.getAffectedObjects();
		assertEquals("1.3", 1, affected.length);
		assertEquals("1.4", repoLocation, affected[0]);

		try {
			IStatus result = op.execute(getMonitor(), null);
			assertTrue("1.9", result.isOK());
		} catch (ExecutionException e) {
			fail("1.99", e);
		}

		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
		URI[] repos = manager.getKnownRepositories(0);
		assertTrue("2.0", Arrays.asList(repos).contains(repoLocation));
		assertTrue("2.1", manager.isEnabled(repoLocation));

		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IArtifactRepositoryManager.class.getName());
		repos = artifactManager.getKnownRepositories(0);
		assertTrue("3.0", Arrays.asList(repos).contains(repoLocation));
		assertTrue("3.1", artifactManager.isEnabled(repoLocation));
	}

	public void testUndoRedo() {
		URI repoLocation = null;
		try {
			repoLocation = TestData.getFile("artifactRepo", "").toURI();
		} catch (Exception e) {
			fail("0.99", e);
		}
		AddColocatedRepositoryOperation op = new AddColocatedRepositoryOperation("label", repoLocation);

		try {
			op.execute(getMonitor(), null);
		} catch (ExecutionException e) {
			fail("1.99", e);
		}

		//should be undoable
		assertTrue("1.0", op.canUndo());
		assertTrue("1.1", !op.canExecute());

		try {
			op.undo(getMonitor(), null);
		} catch (ExecutionException e) {
			fail("2.99", e);
		}

		IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
		URI[] repos = metadataManager.getKnownRepositories(0);
		assertTrue("2.0", !Arrays.asList(repos).contains(repoLocation));
		assertTrue("2.1", !metadataManager.isEnabled(repoLocation));

		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IArtifactRepositoryManager.class.getName());
		repos = artifactManager.getKnownRepositories(0);
		assertTrue("3.0", !Arrays.asList(repos).contains(repoLocation));
		assertTrue("3.1", !artifactManager.isEnabled(repoLocation));

		//should be redoable
		assertTrue("4.0", !op.canUndo());
		assertTrue("4.1", op.canRedo());

		try {
			op.redo(getMonitor(), null);
		} catch (ExecutionException e) {
			fail("4.99", e);
		}
		repos = metadataManager.getKnownRepositories(0);
		assertTrue("2.0", Arrays.asList(repos).contains(repoLocation));
		assertTrue("2.1", metadataManager.isEnabled(repoLocation));

		repos = artifactManager.getKnownRepositories(0);
		assertTrue("3.0", Arrays.asList(repos).contains(repoLocation));
		assertTrue("3.1", artifactManager.isEnabled(repoLocation));

	}
}
