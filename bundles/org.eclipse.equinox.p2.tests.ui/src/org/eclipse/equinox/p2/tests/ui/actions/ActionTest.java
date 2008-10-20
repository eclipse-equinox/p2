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
package org.eclipse.equinox.p2.tests.ui.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.model.IUElement;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.DefaultPhaseSet;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.provisional.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.jface.viewers.*;

/**
 * Abstract class to set up the mock query provider
 */
public abstract class ActionTest extends AbstractProvisioningTest {
	protected static final String TESTPROFILE = "TestProfile";
	protected static final String TOPLEVELIU = "TopLevelIU";
	protected static final String TOPLEVELIU2 = "TopLevelIU2";
	protected static final String NESTEDIU = "NestedIU";
	protected static final String LOCKEDIU = "LockedIU";

	protected IProfile profile;
	protected ProfileElement profileElement;
	protected IInstallableUnit top1, top2, nested, locked;

	protected ISelectionProvider getSelectionProvider(final Object[] selections) {

		return new ISelectionProvider() {

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
			 */
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				// Ignore because the selection won't change 
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
			 */
			public ISelection getSelection() {
				return new StructuredSelection(selections);
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
			 */
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				// ignore because the selection is static
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
			 */
			public void setSelection(ISelection sel) {
				throw new UnsupportedOperationException("This ISelectionProvider is static, and cannot be modified."); //$NON-NLS-1$
			}
		};
	}

	protected IInstallableUnit[] getMixedIUs() {
		return new IInstallableUnit[] {top1, top2, nested};
	}

	protected IInstallableUnit[] getTopLevelIUs() {
		return new IInstallableUnit[] {top1, top2};
	}

	protected IInstallableUnit[] getTopLevelIUsWithLockedIU() {
		return new IInstallableUnit[] {top1, top2, locked};
	}

	protected IUElement[] getTopLevelIUElements() {
		return new IUElement[] {element(top1), element(top2)};
	}

	protected Object[] getMixedIUElements() {
		return new IUElement[] {element(top1), element(top2), element(nested)};
	}

	protected Object[] getTopLevelIUElementsWithLockedIU() {
		return new IUElement[] {element(top1), element(top2), element(locked)};
	}

	protected Object[] getMixedIUsAndElements() {
		return new Object[] {top1, element(top2)};
	}

	protected Object[] getMixedIUsAndNonIUs() {
		return new Object[] {top1, top2, new Object()};
	}

	protected Object[] getEmptySelection() {
		return new Object[0];
	}

	protected Object[] getNonIUSelection() {
		return new Object[] {new Object(), new Object()};
	}

	protected void setUp() throws Exception {
		super.setUp();
		profile = createProfile(TESTPROFILE);
		profileElement = new ProfileElement(null, TESTPROFILE);
		install((top1 = createIU(TOPLEVELIU)), true, false);
		install((top2 = createIU(TOPLEVELIU2)), true, false);
		install((nested = createIU(NESTEDIU)), false, false);
		install((locked = createIU(LOCKEDIU)), true, true);
	}

	protected IStatus install(IInstallableUnit iu, boolean root, boolean lock) {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {iu});
		if (root) {
			String rootProp = Policy.getDefault().getQueryContext().getVisibleInstalledIUProperty();
			if (rootProp != null)
				req.setInstallableUnitProfileProperty(iu, rootProp, Boolean.toString(true));
		}
		if (lock) {
			req.setInstallableUnitProfileProperty(iu, IInstallableUnit.PROP_PROFILE_LOCKED_IU, new Integer(IInstallableUnit.LOCK_UNINSTALL | IInstallableUnit.LOCK_UPDATE).toString());
		}
		ProvisioningPlan plan = createPlanner().getProvisioningPlan(req, null, null);
		if (plan.getStatus().getSeverity() == IStatus.ERROR || plan.getStatus().getSeverity() == IStatus.CANCEL)
			return plan.getStatus();
		return createEngine().perform(profile, new DefaultPhaseSet(), plan.getOperands(), null, null);
	}

	protected IUElement element(IInstallableUnit iu) {
		return new InstalledIUElement(profileElement, profile.getProfileId(), iu);
	}
}
