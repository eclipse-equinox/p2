/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.tests.ui.actions;

import java.util.List;
import org.eclipse.equinox.internal.p2.ui.actions.InstallAction;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * @since 3.5
 *
 */
public class InstallActionTest extends ProfileModificationActionTest {

	class TestInstallAction extends InstallAction {
		TestInstallAction(Object[] sel) {
			super(InstallActionTest.this.getProvisioningUI(), InstallActionTest.this.getSelectionProvider(sel), profile.getProfileId());
		}

		public List<IInstallableUnit> getSelectedIUs() {
			return super.getSelectedIUs();
		}
	}

	public void testEmptySelection() {
		TestInstallAction action = new TestInstallAction(getEmptySelection());
		assertFalse("Should not be enabled with empty selection", action.isEnabled());
		assertEquals(0, action.getSelectedIUs().size());
	}

	public void testIUs() {
		TestInstallAction action = new TestInstallAction(getTopLevelIUs());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testElements() {
		TestInstallAction action = new TestInstallAction(getTopLevelIUElements());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testMixedIUsAndNonIUs() {
		TestInstallAction action = new TestInstallAction(getMixedIUsAndNonIUs());
		assertTrue("Mixed selections allowed", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testMixedIUsAndElements() {
		TestInstallAction action = new TestInstallAction(getMixedIUsAndElements());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().size());
	}

	public void testParentIsCategory() {
		TestInstallAction action = new TestInstallAction(getCategoryAndChildIUElements());
		assertTrue("Should be enabled", action.isEnabled());
		// Only the non-category should be considered a selection
		assertEquals(1, action.getSelectedIUs().size());
	}

	public void testParentIsNestedCategory() {
		TestInstallAction action = new TestInstallAction(getNestedCategoriesAndChildIUElements());
		assertTrue("Should be enabled", action.isEnabled());
		// Only the non-category should be considered a selection
		assertEquals(1, action.getSelectedIUs().size());
	}

	protected IIUElement element(IInstallableUnit iu) {
		return new AvailableIUElement(null, iu, profile.getProfileId(), true);
	}
}
