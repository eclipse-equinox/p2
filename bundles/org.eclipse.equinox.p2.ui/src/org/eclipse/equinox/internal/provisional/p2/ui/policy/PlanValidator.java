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
package org.eclipse.equinox.internal.provisional.p2.ui.policy;

import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.swt.widgets.Shell;

/**
 * Abstract class for a mechanism that checks a provisioning plan to see
 * if a user should be allowed to perform a provisioning action on it.
 * 
 * @since 3.4
 * 
 * @see org.eclipse.equinox.internal.provisional.p2.ui.actions.ProfileModificationAction
 * 
 */

public abstract class PlanValidator {
	/**
	 * Return a boolean indicating whether the caller should continue working
	 * with the provisioning plan.  This method is used to give implementors a chance
	 * to validate or check a plan before continuing.  The work that the caller intends
	 * to do depends on the context in which this validator is used.  When the validator is used
	 * for an action, it may mean opening a wizard on the plan.  When the validator is used
	 * inside a wizard, it may mean attempting to perform the plan. It is up to the implementor of
	 * this method to report any errors to user or otherwise inform the user if the 
	 * outcome is <code>false</code>.
	 * 
	 * @param plan a ProvisioningPlan that the caller wishes to work with.  Never <code>null</code>.
	 * @param shell the Shell that may be used to report any errors or prompt the user.  May be <code>null</code>.
	 * @return <code>true</code> if the caller should continue working with the plan, or
	 * <code>false</code> if the caller should stop. If <code>false</code> it is expected that
	 * any error reporting has already been completed.
	 */
	public abstract boolean continueWorkingWithPlan(ProvisioningPlan plan, Shell shell);

}
