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

/**
 * Abstract class to set up different IU selection combinations
 */
public abstract class ProfileModificationActionTest extends ActionTest {
	protected static final String TESTPROFILE = "TestProfile";
	protected static final String TOPLEVELIU = "TopLevelIU";
	protected static final String TOPLEVELIU2 = "TopLevelIU2";
	protected static final String NESTEDIU = "NestedIU";
	protected static final String LOCKEDIU = "LockedIU";

	protected IProfile profile;
	protected ProfileElement profileElement;
	protected IInstallableUnit top1, top2, nested, locked;

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

	protected Object[] getNonIUSelection() {
		return getInvalidSelection();
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
