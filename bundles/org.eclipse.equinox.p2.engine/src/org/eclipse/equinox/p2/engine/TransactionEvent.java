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
	protected IProfile profile;
	protected PhaseSet phaseSet;
	protected InstallableUnitOperand[] deltas;

	public TransactionEvent(IProfile profile, PhaseSet phaseSet, InstallableUnitOperand[] deltas, Engine engine) {
		super(engine);
		this.profile = profile;
		this.phaseSet = phaseSet;
		this.deltas = deltas;
	}

	public IProfile getProfile() {
		return profile;
	}

	// TODO this was added as a workaround 
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=208251
	public PhaseSet getPhaseSet() {
		return phaseSet;
	}
}