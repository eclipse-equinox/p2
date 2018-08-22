/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;


/**
 * @since 2.0
 */
public class CommitOperationEvent extends TransactionEvent {
	private static final long serialVersionUID = -523967775426133720L;

	public CommitOperationEvent(IProfile profile, PhaseSet phaseSet, Operand[] operands, IEngine engine) {
		super(profile, phaseSet, operands, engine);
	}

}
