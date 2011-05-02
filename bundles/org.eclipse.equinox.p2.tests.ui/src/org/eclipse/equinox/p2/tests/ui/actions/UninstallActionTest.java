/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.tests.ui.actions;

import java.util.List;
import org.eclipse.equinox.internal.p2.ui.actions.UninstallAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * @since 3.5
 *
 */
public class UninstallActionTest extends ProfileModificationActionTest {
	class TestUninstallAction extends UninstallAction {
		TestUninstallAction(Object[] sel) {
			super(UninstallActionTest.this.getProvisioningUI(), UninstallActionTest.this.getSelectionProvider(sel), profile.getProfileId());
		}

		public List<IInstallableUnit> getSelectedIUs() {
			return super.getSelectedIUs();
		}
	}

	public void testLockedElements() {
		TestUninstallAction action = new TestUninstallAction(getTopLevelIUElementsWithLockedIU());
		assertFalse("Should not be enabled with locked elements", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testLockedIUs() {
		TestUninstallAction action = new TestUninstallAction(getTopLevelIUsWithLockedIU());
		assertFalse("Should not be enabled with locked ius", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testEmptySelection() {
		TestUninstallAction action = new TestUninstallAction(getEmptySelection());
		assertFalse("Should not be enabled with empty selection", action.isEnabled());
		assertEquals(0, action.getSelectedIUs().size());
	}

	public void testTopLevelIUs() {
		TestUninstallAction action = new TestUninstallAction(getTopLevelIUs());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testTopLevelElements() {
		TestUninstallAction action = new TestUninstallAction(getTopLevelIUElements());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testNestedIUs() {
		TestUninstallAction action = new TestUninstallAction(getMixedIUs());
		assertFalse("Should not enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testNestedElements() {
		TestUninstallAction action = new TestUninstallAction(getMixedIUElements());
		assertFalse("Should not enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testMixedIUsAndNonIUs() {
		TestUninstallAction action = new TestUninstallAction(getMixedIUsAndNonIUs());
		assertFalse("Should not enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testMixedIUsAndElements() {
		TestUninstallAction action = new TestUninstallAction(getMixedIUsAndElements());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}
}
