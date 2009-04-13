/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.tests.ui.actions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.model.MetadataRepositories;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * @since 3.5
 *
 */
public class ElementUtilsTest extends ProfileModificationActionTest {

	public void testEmpty() {
		assertEquals(getEmptySelection().length, ElementUtils.elementsToIUs(getEmptySelection()).length);
	}

	public void testInvalid() {
		assertTrue(ElementUtils.elementsToIUs(getInvalidSelection()).length == 0);
	}

	public void testIUs() {
		assertEquals(getTopLevelIUs().length, ElementUtils.elementsToIUs(getTopLevelIUs()).length);
	}

	public void testElements() {
		assertEquals(getTopLevelIUElements().length, ElementUtils.elementsToIUs(getTopLevelIUElements()).length);
	}

	public void testMixedIUsAndNonIUs() {
		assertTrue(getMixedIUsAndNonIUs().length != ElementUtils.elementsToIUs(getMixedIUsAndNonIUs()).length);
	}

	public void testMixedIUsAndElements() {
		assertEquals(getMixedIUsAndElements().length, ElementUtils.elementsToIUs(getMixedIUsAndElements()).length);
	}

	public void testUpdateUsingElements() throws ProvisionException, URISyntaxException {
		// Two visible repos, one is added, the other is not
		URI known1 = new URI("http://example.com/known1");
		URI known2 = new URI("http://example.com/known2");
		ProvisioningUtil.addMetadataRepository(known1, false);

		// Add system repos that should not be known or affected by ElementUtils
		// One is an enabled system repo, one is disabled system repo
		URI uri = new URI("http://example.com/1");
		URI uri2 = new URI("http://example.com/2");
		ProvisioningUtil.addMetadataRepository(uri, false);
		ProvisioningUtil.setMetadataRepositoryProperty(uri, IRepository.PROP_SYSTEM, Boolean.toString(true));
		ProvisioningUtil.addMetadataRepository(uri2, false);
		ProvisioningUtil.addArtifactRepository(uri2, false);
		ProvisioningUtil.setMetadataRepositoryProperty(uri2, IRepository.PROP_SYSTEM, Boolean.toString(true));
		ProvisioningUtil.setColocatedRepositoryEnablement(uri2, false);

		// The elements reflect all visible sites, but not system sites
		MetadataRepositories root = new MetadataRepositories(Policy.getDefault());
		List children = new ArrayList();
		children.addAll(Arrays.asList(root.getChildren(root)));
		// Add known2, this is as if a user added it in the pref page
		children.add(new MetadataRepositoryElement(null, known2, true));
		MetadataRepositoryElement[] elements = (MetadataRepositoryElement[]) children.toArray(new MetadataRepositoryElement[children.size()]);

		// Add a visible repo not known by the elements
		URI uri3 = new URI("http://example.com/3");
		ProvisioningUtil.addMetadataRepository(uri3, false);

		// Now update the repo using the elements.  
		// We expect known2 to get added because it was in the elements
		// We expect uri3 to get removed (as if it had been removed from a pref page)
		// System repos shouldn't be touched
		ElementUtils.updateRepositoryUsingElements(elements, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		final boolean[] done = new boolean[1];
		done[0] = false;
		// The repo update happens in a job, so we need to ensure it finished
		Platform.getJobManager().addJobChangeListener(new IJobChangeListener() {

			public void aboutToRun(IJobChangeEvent event) {
				// TODO Auto-generated method stub

			}

			public void awake(IJobChangeEvent event) {
				// TODO Auto-generated method stub

			}

			public void done(IJobChangeEvent event) {
				if (event.getJob().getName().equals(ProvUIMessages.ElementUtils_UpdateJobTitle))
					done[0] = true;

			}

			public void running(IJobChangeEvent event) {
				// TODO Auto-generated method stub

			}

			public void scheduled(IJobChangeEvent event) {
				if (event.getJob().getName().equals(ProvUIMessages.ElementUtils_UpdateJobTitle))
					event.getJob().setPriority(Job.INTERACTIVE);

			}

			public void sleeping(IJobChangeEvent event) {
				// TODO Auto-generated method stub

			}

		});

		// spin event loop until job is done
		Display display = PlatformUI.getWorkbench().getDisplay();
		while (!done[0]) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		URI[] enabled = ProvisioningUtil.getMetadataRepositories(IRepositoryManager.REPOSITORIES_ALL);
		URI[] disabled = ProvisioningUtil.getMetadataRepositories(IRepositoryManager.REPOSITORIES_DISABLED);

		boolean foundKnown1 = false;
		boolean foundKnown2 = false;
		boolean found1 = false;
		boolean found2 = false;
		boolean found3 = false;

		for (int i = 0; i < enabled.length; i++) {
			if (enabled[i].equals(known1))
				foundKnown1 = true;
			if (enabled[i].equals(known2))
				foundKnown2 = true;
			if (enabled[i].equals(uri))
				found1 = true;
			if (enabled[i].equals(uri2))
				found2 = true;
			if (enabled[i].equals(uri3))
				found3 = true;
		}
		for (int i = 0; i < disabled.length; i++) {
			if (disabled[i].equals(known1))
				foundKnown1 = true;
			if (disabled[i].equals(known2))
				foundKnown2 = true;
			if (disabled[i].equals(uri))
				found1 = true;
			if (disabled[i].equals(uri2))
				found2 = true;
			if (disabled[i].equals(uri3))
				found3 = true;
		}
		assertTrue("1.0", found1); // Enabled system repo still exists
		assertTrue("1.1", found2); // Disabled system repo still exists
		assertFalse("1.2", found3); // Enabled repo not known by elements was deleted
		assertTrue("1.3", foundKnown1); // Enabled visible repo still exists
		assertTrue("1.4", foundKnown2); // Enabled visible repo in elements was added

		// cleanup
		ProvisioningUtil.removeMetadataRepository(known1);
		ProvisioningUtil.removeMetadataRepository(known2);
		ProvisioningUtil.removeMetadataRepository(uri);
		ProvisioningUtil.removeMetadataRepository(uri2);
		ProvisioningUtil.removeArtifactRepository(uri2);
		ProvisioningUtil.removeMetadataRepository(uri3);
	}
}
