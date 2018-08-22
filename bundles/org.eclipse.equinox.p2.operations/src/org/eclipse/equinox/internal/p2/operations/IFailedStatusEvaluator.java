/*******************************************************************************
 *  Copyright (c) 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.operations;

import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;

/**
 * IFailedStatusEvaluator determines what to do (if anything)
 * when a profile change cannot be resolved successfully.
 * 
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IFailedStatusEvaluator {
	public ProvisioningContext getSecondPassProvisioningContext(IProvisioningPlan failedPlan);
}
