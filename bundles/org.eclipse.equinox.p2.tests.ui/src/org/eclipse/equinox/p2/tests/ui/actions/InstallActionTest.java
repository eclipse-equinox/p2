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

import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.InstallAction;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;

/**
 * @since 3.5
 *
 */
public class InstallActionTest extends ProfileModificationActionTest {

	class TestInstallAction extends InstallAction {
		TestInstallAction(Object[] sel) {
			super(Policy.getDefault(), InstallActionTest.this.getSelectionProvider(sel), profile.getProfileId());
		}

		public IInstallableUnit[] getSelectedIUs() {
			return super.getSelectedIUs();
		}
	}

	public void testEmptySelection() {
		TestInstallAction action = new TestInstallAction(getEmptySelection());
		assertFalse("Should not be enabled with empty selection", action.isEnabled());
		assertEquals(0, action.getSelectedIUs().length);
	}

	public void testIUs() {
		TestInstallAction action = new TestInstallAction(getTopLevelIUs());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().length);
	}

	public void testElements() {
		TestInstallAction action = new TestInstallAction(getTopLevelIUElements());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().length);
	}

	public void testMixedIUsAndNonIUs() {
		TestInstallAction action = new TestInstallAction(getMixedIUsAndNonIUs());
		assertTrue("Mixed selections allowed", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().length);
	}

	public void testMixedIUsAndElements() {
		TestInstallAction action = new TestInstallAction(getMixedIUsAndElements());
		assertTrue("Should be enabled", action.isEnabled());
		assertEquals(2, action.getSelectedIUs().length);
	}

	protected IIUElement element(IInstallableUnit iu) {
		return new AvailableIUElement(null, iu, profile.getProfileId(), true);
	}
}
