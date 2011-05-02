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
import org.eclipse.equinox.internal.p2.ui.actions.UpdateAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * @since 3.5
 *
 */
public class UpdateActionTest extends ProfileModificationActionTest {
	class TestUpdateAction extends UpdateAction {
		TestUpdateAction(Object[] sel) {
			super(UpdateActionTest.this.getProvisioningUI(), UpdateActionTest.this.getSelectionProvider(sel), profile.getProfileId(), true);
		}

		public List<IInstallableUnit> getSelectedIUs() {
			return super.getSelectedIUs();
		}
	}

	public void testLockedElements() {
		TestUpdateAction action = new TestUpdateAction(getTopLevelIUElementsWithLockedIU());
		assertFalse("Should not be enabled with locked elements", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testLockedIUs() {
		TestUpdateAction action = new TestUpdateAction(getTopLevelIUsWithLockedIU());
		assertFalse("Should not be enabled with locked ius", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testEmptySelection() {
		TestUpdateAction action = new TestUpdateAction(getEmptySelection());
		assertFalse("Should not be enabled with empty selection", action.isEnabled());
		assertEquals(0, action.getSelectedIUs().size());
	}

	public void testTopLevelIUs() {
		TestUpdateAction action = new TestUpdateAction(getTopLevelIUs());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testTopLevelElements() {
		TestUpdateAction action = new TestUpdateAction(getTopLevelIUElements());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testNestedIUs() {
		TestUpdateAction action = new TestUpdateAction(getMixedIUs());
		assertFalse("Should not enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testNestedElements() {
		TestUpdateAction action = new TestUpdateAction(getMixedIUElements());
		assertFalse("Should not enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testMixedIUsAndNonIUs() {
		TestUpdateAction action = new TestUpdateAction(getMixedIUsAndNonIUs());
		assertFalse("Should not enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testMixedIUsAndElements() {
		TestUpdateAction action = new TestUpdateAction(getMixedIUsAndElements());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}
}
