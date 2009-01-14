/*******************************************************************************
 * Copyright (c) 2008 Band XI International, LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Band XI - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.engine;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public interface IEngine {
	public static final String SERVICE_NAME = IEngine.class.getName();

	public IStatus perform(IProfile profile, PhaseSet phaseSet, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor);

	public IStatus validate(IProfile iprofile, PhaseSet phaseSet, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor);
}
