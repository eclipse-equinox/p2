/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.util.EventObject;

public abstract class TransactionEvent extends EventObject {
	private static final long serialVersionUID = 6278706971855493984L;
	protected Profile profile;
	protected PhaseSet phaseSet;
	protected Operand[] deltas;

	public TransactionEvent(Profile profile, PhaseSet phaseSet, Operand[] deltas, Engine engine) {
		super(engine);
		this.profile = profile;
		this.phaseSet = phaseSet;
		this.deltas = deltas;
	}

	public Profile getProfile() {
		return profile;
	}
}