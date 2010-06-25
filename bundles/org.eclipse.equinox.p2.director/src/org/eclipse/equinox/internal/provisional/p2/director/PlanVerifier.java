/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.director;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;

/**
 * Verifier is responsible for checking plan sanity
 */
public abstract class PlanVerifier {
	/**
	 * Verifies provisioning plan from P2 solver
	 */
	public abstract IStatus verify(IProvisioningPlan plan);

}
