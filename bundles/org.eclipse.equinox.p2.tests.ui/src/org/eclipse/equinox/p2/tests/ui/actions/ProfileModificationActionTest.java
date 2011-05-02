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
package org.eclipse.equinox.p2.tests.ui.actions;

import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * Abstract class to set up different IU selection combinations
 */
public abstract class ProfileModificationActionTest extends ActionTest {
	protected IInstallableUnit[] getMixedIUs() {
		return new IInstallableUnit[] {top1, top2, nested};
	}

	protected Object[] getCategoryAndChildIUElements() {
		CategoryElement element = new CategoryElement(null, category);
		AvailableIUElement child = new AvailableIUElement(element, uninstalled, TESTPROFILE, true);
		return new Object[] {element, child};
	}

	protected Object[] getNestedCategoriesAndChildIUElements() {
		CategoryElement element = new CategoryElement(null, category);
		CategoryElement nested = new CategoryElement(element, category);
		AvailableIUElement child = new AvailableIUElement(nested, uninstalled, TESTPROFILE, true);
		return new Object[] {element, nested, child};
	}

	protected IInstallableUnit[] getTopLevelIUs() {
		return new IInstallableUnit[] {top1, top2};
	}

	protected IInstallableUnit[] getTopLevelIUsWithLockedIU() {
		return new IInstallableUnit[] {top1, top2, locked};
	}

	protected IIUElement[] getTopLevelIUElements() {
		return new IIUElement[] {element(top1), element(top2)};
	}

	protected Object[] getMixedIUElements() {
		return new IIUElement[] {element(top1), element(top2), element(nested)};
	}

	protected Object[] getTopLevelIUElementsWithLockedIU() {
		return new IIUElement[] {element(top1), element(top2), element(locked)};
	}

	protected Object[] getMixedIUsAndElements() {
		return new Object[] {top1, element(top2)};
	}

	protected Object[] getMixedIUsAndNonIUs() {
		return new Object[] {top1, top2, new Object()};
	}

	protected Object[] getNonIUSelection() {
		return getInvalidSelection();
	}

	protected IIUElement element(IInstallableUnit iu) {
		return new InstalledIUElement(profileElement, profile.getProfileId(), iu);
	}
}
