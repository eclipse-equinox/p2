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

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.UninstallAction;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;

/**
 * @since 3.5
 *
 */
public class UninstallActionTest extends ActionTest {
	class TestUninstallAction extends UninstallAction {
		TestUninstallAction(Object[] sel) {
			super(Policy.getDefault(), UninstallActionTest.this.getSelectionProvider(sel), profile.getProfileId());
		}

		public IInstallableUnit[] getSelectedIUs() {
			return super.getSelectedIUs();
		}
	}

	public void testLockedElements() {
		TestUninstallAction action = new TestUninstallAction(getTopLevelIUElementsWithLockedIU());
		assertFalse("Should not be enabled with locked elements", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().length);
	}

	public void testLockedIUs() {
		TestUninstallAction action = new TestUninstallAction(getTopLevelIUsWithLockedIU());
		assertFalse("Should not be enabled with locked ius", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().length);
	}

	public void testEmptySelection() {
		TestUninstallAction action = new TestUninstallAction(getEmptySelection());
		assertFalse("Should not be enabled with empty selection", action.isEnabled());
		assertEquals(0, action.getSelectedIUs().length);
	}

	public void testTopLevelIUs() {
		TestUninstallAction action = new TestUninstallAction(getTopLevelIUs());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().length);
	}

	public void testTopLevelElements() {
		TestUninstallAction action = new TestUninstallAction(getTopLevelIUElements());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().length);
	}

	public void testNestedIUs() {
		TestUninstallAction action = new TestUninstallAction(getMixedIUs());
		assertFalse("Should not enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().length);
	}

	public void testNestedElements() {
		TestUninstallAction action = new TestUninstallAction(getMixedIUElements());
		assertFalse("Should not enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().length);
	}

	public void testMixedIUsAndNonIUs() {
		TestUninstallAction action = new TestUninstallAction(getMixedIUsAndNonIUs());
		assertFalse("Should not enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().length);
	}

	public void testMixedIUsAndElements() {
		TestUninstallAction action = new TestUninstallAction(getMixedIUsAndElements());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().length);
	}
}
